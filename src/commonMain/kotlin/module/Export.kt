package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.string

data class Export(val name: String, val kind: ExportType, val externIdx: UInt) : Encode, Printable {
    override fun encode(out: WasmWriter) {
        out.writeString(name)
        out.writeByte(kind.ordinal.toByte())
        out.writeUInt(externIdx)
    }

    override fun Printer.print() {
        val (ns, exportType) = when(kind) {
            ExportType.FUNCTION -> Namespace.FUNC to "func"
            ExportType.TABLE -> Namespace.TABLE to "table"
            ExportType.MEMORY -> Namespace.MEMORY to "memory"
            ExportType.GLOBAL -> Namespace.GLOBAL to "global"
            ExportType.TAG -> Namespace.TAG to "tag"
        }

        string(name)

        wrapInline {
            word(exportType)
            word(names.resolveNameInfer(ns, externIdx))
        }
    }

    constructor(reader: WasmReader) : this(
        reader.readString(),
        ExportType.entries[reader.readByte().toInt()],
        reader.readUInt()
    )
}
