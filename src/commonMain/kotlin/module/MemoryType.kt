package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Decode
import net.derfruhling.serene.wasm.DeferredDecode
import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.map
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer

data class MemoryType(val limits: Limits) : Encode by limits, Printable {
    override fun Printer.print() {
        word(when(limits.wasm64) {
            true -> "i64"
            false -> "i32"
        })

        word(limits.min.toString())
        limits.max?.let { word(it.toString()) }
    }

    companion object : Decode<MemoryType> {
        override fun deferredDecode(reader: WasmReader): DeferredDecode<MemoryType>? {
            return Limits.deferredDecode(reader)?.map(::MemoryType)
        }
    }
}