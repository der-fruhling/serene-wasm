package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.DeferredDecode
import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer

sealed interface Type : Encode, Printable {
    sealed class SimpleType<T: SimpleType<T>>(val id: Byte, val name: String) : Type, DeferredDecode<T> {
        override fun encode(out: WasmWriter) {
            out.writeByte(id)
        }

        override fun Printer.print() {
            word(name)
        }

        override fun finishDecoding(reader: WasmReader): T {
            @Suppress("UNCHECKED_CAST")
            return this as T
        }
    }
}

