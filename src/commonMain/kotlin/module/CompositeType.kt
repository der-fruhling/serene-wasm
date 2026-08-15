package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.Decode
import net.derfruhling.serene.wasm.DeferredDecode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.identWord
import net.derfruhling.serene.wasm.printer.print

sealed interface CompositeType : Type, RecursiveType.SubType {
    data class Array(val type: FieldType) : CompositeType {
        override fun encode(out: WasmWriter) {
            out.writeByte(Constants.COMP_TYPE_ARRAY)
            type.encode(out)
        }

        override fun Printer.print() {
            word("array")
            type.print(this)
        }
    }

    data class Struct(val fields: List<FieldType>) : CompositeType {
        override fun encode(out: WasmWriter) {
            out.writeByte(Constants.COMP_TYPE_STRUCT)
            out.writeList(fields)
        }

        override fun Printer.print() {
            word("struct")

            for((i, field) in fields.withIndex()) wrapInline {
                word("field")
                names.resolveFieldName(names.currentType, i)?.let {
                    identWord(it)
                }

                field.print(this)
            }
        }
    }

    data class Func(val args: ResultType, val result: ResultType) : CompositeType {
        override fun encode(out: WasmWriter) {
            out.writeByte(Constants.COMP_TYPE_FUNC)
            args.encode(out)
            result.encode(out)
        }

        override fun Printer.print() {
            word("func")

            printParams()
        }

        fun Printer.printParams() {
            for((i, param) in args.types.withIndex()) wrapInline {
                word("param")

                names.resolveParamName(names.currentFunction, i)?.let {
                    identWord(it)
                }

                param.print(this)
            }

            for(type in result.types) wrapInline {
                word("result")

                type.print(this)
            }
        }
    }

    companion object : Decode<CompositeType> {
        override fun deferredDecode(reader: WasmReader): DeferredDecode<CompositeType>? {
            return when(reader.readByte()) {
                Constants.COMP_TYPE_ARRAY -> DeferredDecode {
                    Array(FieldType(it))
                }

                Constants.COMP_TYPE_STRUCT -> DeferredDecode {
                    Struct(it.readList(::FieldType))
                }

                Constants.COMP_TYPE_FUNC -> DeferredDecode {
                    Func(ResultType(it), ResultType(it))
                }

                else -> null
            }
        }
    }
}

fun CompositeType.Func.printParams(printer: Printer) {
    with(printer) { printParams() }
}
