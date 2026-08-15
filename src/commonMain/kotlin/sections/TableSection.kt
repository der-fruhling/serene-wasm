package net.derfruhling.serene.wasm.sections

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.Table
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class TableSection(val tables: List<Table>) : Section, Printable {
    override val id: Byte
        get() = Constants.TABLE_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(tables)
    }

    override fun Printer.print() {
        for(table in tables) wrap {
            word("table")
            word(names.resolveNameDecl(Namespace.TABLE, names.currentTable))
            table.print(this)
            names.currentTable++
        }
    }

    constructor(reader: WasmReader) : this(reader.readList { Table.decode(it) })

    companion object {
        val EMPTY = TableSection(emptyList())
    }
}
