package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.Element
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class ElementSection(val elements: List<Element>) : Section, Printable {
    override val id: Byte
        get() = Constants.ELEMENT_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(elements)
    }

    override fun Printer.print() {
        for(element in elements) wrap {
            word("elem")
            word(names.resolveNameDecl(Namespace.ELEM, names.currentElement))
            element.print(this)
            names.currentElement++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList { Element.decode(it) })

    companion object {
        val EMPTY = ElementSection(emptyList())
    }
}
