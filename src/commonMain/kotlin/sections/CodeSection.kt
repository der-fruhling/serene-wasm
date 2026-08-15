package net.derfruhling.serene.wasm.sections

import kotlinx.io.Buffer
import net.derfruhling.serene.wasm.Constants
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.module.FunctionCode
import net.derfruhling.serene.wasm.module.InvalidModuleDataException
import net.derfruhling.serene.wasm.printer.ExpressionFormatter
import net.derfruhling.serene.wasm.printer.FunctionCodeFormatter
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.identWord
import net.derfruhling.serene.wasm.printer.print

data class CodeSection(val codes: List<FunctionCode>) : Section, Printable {
    override val id: Byte
        get() = Constants.CODE_SECTION

    override fun encode(out: WasmWriter) {
        out.writeList(codes) { code ->
            val buffer = Buffer()
            code.encode(WasmWriter(buffer))
            out.writeUInt(buffer.size.toUInt())
            out.writeBytes(buffer)
        }
    }

    override fun Printer.print() {
        for ((locals, code) in codes) wrap {
            val func = names.currentFuncType
            val funcName = names.resolveNameDecl(Namespace.FUNC, names.currentFunction)

            word("func")
            word(funcName)

            wrapInline {
                word("type")
                word(names.resolveNameInfer(Namespace.TYPE, func))
            }

            appendLine()
            indent++

            if (locals.isNotEmpty()) {
                var local = 0
                for ((count, type) in locals) {
                    repeat(count.toInt()) {
                        wrap {
                            word("local")
                            names.resolveLocalName(names.currentFunction, local++)?.let { identWord(it) }
                            type.print(this)
                        }
                    }
                }

                appendLine()
            }

            code.visit(FunctionCodeFormatter(this))

            indent--
            append(indentStr)
            names.currentFunction++
        }
    }

    companion object {
        val EMPTY = CodeSection(emptyList())

        private fun decodeList(reader: WasmReader): List<FunctionCode> {
            return reader.readList {
                val size = reader.readUInt()
                val keeper = reader.keeper.inherit()
                val buffer = reader.readBytes(size)
                val code = FunctionCode(WasmReader(buffer, keeper))
                if (!buffer.exhausted())
                    throw InvalidModuleDataException("Function code not exhausted by parsing")
                code
            }
        }
    }

    constructor(reader: WasmReader) : this(decodeList(reader))
}
