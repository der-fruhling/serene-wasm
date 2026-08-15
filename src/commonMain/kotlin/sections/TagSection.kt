package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.TagType
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer

data class TagSection(val tags: List<TagType>) : Section, Printable {
    override val id: Byte
        get() = Constants.TAG_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(tags)
    }

    override fun Printer.print() {
        for((tag) in tags) wrap {
            word("tag")
            word(names.resolveNameDecl(Namespace.TAG, names.currentTag))

            wrapInline {
                word("type")
                word(names.resolveNameInfer(Namespace.TYPE, tag))
            }

            names.currentTag++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList { TagType.decode(it) })

    companion object {
        val EMPTY = TagSection(emptyList())
    }
}
