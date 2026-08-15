package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class TableType(val refType: RefType, val limits: Limits) : Encode, Printable {
    override fun encode(out: WasmWriter) {
        refType.encode(out)
        limits.encode(out)
    }

    override fun Printer.print() {
        word(when(limits.wasm64) {
            true -> "i64"
            false -> "i32"
        })

        word(limits.min.toString())
        limits.max?.let { word(it.toString()) }

        refType.print(this)
    }

    constructor(reader: WasmReader) : this(
        RefType.decode(reader),
        Limits.decode(reader)
    )
}