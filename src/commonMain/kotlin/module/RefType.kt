package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.*
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

sealed interface RefType : ValueType {
    val heapType: HeapType

    data class Nullable(override val heapType: HeapType) : RefType, Default {
        override fun encode(out: WasmWriter) {
            if(heapType !is HeapType.Abstract) {
                out.writeByte(Constants.REF_TYPE_NULLABLE)
            }
            heapType.encode(out)
        }

        override fun Printer.print() = wrapInline {
            word("ref")
            word("null")
            heapType.print(this)
        }
    }

    data class NonNull(override val heapType: HeapType) : RefType {
        override fun encode(out: WasmWriter) {
            out.writeByte(Constants.REF_TYPE_NOT_NULL)
            heapType.encode(out)
        }

        override fun Printer.print() = wrapInline {
            word("ref")
            heapType.print(this)
        }
    }

    companion object : Decode<RefType> {
        override fun deferredDecode(reader: WasmReader): DeferredDecode<RefType>? {
            HeapType.Abstract.nestedDecode(reader)?.let { return it.map(::Nullable) }
            return when(val byte = reader.readByte()) {
                Constants.REF_TYPE_NULLABLE -> HeapType.deferredDecode(reader)?.map(::Nullable)
                Constants.REF_TYPE_NOT_NULL -> HeapType.deferredDecode(reader)?.map(::NonNull)
                else -> null
            }
        }
    }
}