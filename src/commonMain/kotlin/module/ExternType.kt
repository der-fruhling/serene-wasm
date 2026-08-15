package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Decode
import net.derfruhling.serene.wasm.DeferredDecode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.map
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.identWord
import net.derfruhling.serene.wasm.printer.print

sealed interface ExternType : Type {
    fun Printer.print(name: String)

    data class Func(val typeIdx: UInt) : ExternType {
        override fun encode(out: WasmWriter) {
            out.writeByte(0)
            out.writeUInt(typeIdx)
        }

        override fun Printer.print() {
            word("func")

            wrapInline {
                word("type")
                word(names.resolveNameInfer(Namespace.TYPE, typeIdx))
            }
        }

        override fun Printer.print(name: String) {
            word("func")
            word(name)

            wrapInline {
                word("type")
                word(names.resolveNameInfer(Namespace.TYPE, typeIdx))
            }
        }
    }

    data class Table(val tableType: TableType) : ExternType {
        override fun encode(out: WasmWriter) {
            out.writeByte(1)
            tableType.encode(out)
        }

        override fun Printer.print() {
            word("table")
            tableType.print(this)
        }

        override fun Printer.print(name: String) {
            word("table")
            word(name)
            tableType.print(this)
        }
    }

    data class Memory(val memType: MemoryType) : ExternType {
        override fun encode(out: WasmWriter) {
            out.writeByte(2)
            memType.encode(out)
        }

        override fun Printer.print() {
            word("memory")
            memType.print(this)
        }

        override fun Printer.print(name: String) {
            word("memory")
            word(name)
            memType.print(this)
        }
    }

    data class Global(val globalType: GlobalType) : ExternType {
        override fun encode(out: WasmWriter) {
            out.writeByte(3)
            globalType.encode(out)
        }

        override fun Printer.print() {
            word("global")
            globalType.print(this)
        }

        override fun Printer.print(name: String) {
            word("global")
            word(name)
            globalType.print(this)
        }
    }

    data class Tag(val tagType: TagType) : ExternType {
        override fun encode(out: WasmWriter) {
            out.writeByte(4)
            tagType.encode(out)
        }

        override fun Printer.print() {
            word("tag")
            word(names.resolveName(Namespace.TYPE, tagType.typeIdx))
        }

        override fun Printer.print(name: String) {
            word("tag")
            word(name)
            word(names.resolveName(Namespace.TYPE, tagType.typeIdx))
        }
    }

    companion object : Decode<ExternType> {
        override fun deferredDecode(reader: WasmReader): DeferredDecode<ExternType>? {
            return when(reader.readByte().toInt()) {
                0 -> DeferredDecode { Func(it.readUInt()) }
                1 -> DeferredDecode { Table(TableType(it)) }
                2 -> MemoryType.nestedDecode(reader)?.map(::Memory)
                3 -> DeferredDecode { Global(GlobalType(it)) }
                4 -> TagType.nestedDecode(reader)?.map(::Tag)
                else -> null
            }
        }
    }
}

fun ExternType.print(printer: Printer, name: String) {
    with(printer) { print(name) }
}
