package net.derfruhling.serene.wasm.module

import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString
import net.derfruhling.serene.wasm.*
import net.derfruhling.serene.wasm.printer.InlineExpressionFormatter
import net.derfruhling.serene.wasm.printer.Namespace
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.string

class DataSegment private constructor(
    val memoryIndex: UInt,
    val offsetExpr: CodeBlob?,
    val bytes: ByteString
) : Encode, Printable {
    override fun encode(out: WasmWriter) {
        out.writeByte(
            if (offsetExpr == null) {
                1
            } else {
                if (memoryIndex == 0u) {
                    0
                } else {
                    2
                }
            }
        )

        if (memoryIndex != 0u) out.writeUInt(memoryIndex)
        if (offsetExpr != null) out.writeBytes(offsetExpr.byteString)
        out.writeUInt(bytes.size.toUInt())
        out.writeBytes(bytes)
    }

    override fun Printer.print() {
        if(offsetExpr != null) {
            word(names.resolveName(Namespace.MEMORY, memoryIndex))
            offsetExpr.visit(InlineExpressionFormatter(this))
        }

        string(bytes)
    }

    companion object : Decode<DataSegment> {
        private fun WasmReader.readByteList(): ByteString {
            val size = readUInt()
            return readBytes(size).readByteString()
        }

        override fun deferredDecode(reader: WasmReader): DeferredDecode<DataSegment>? {
            return when (reader.readByte().toInt()) {
                0 -> DeferredDecode {
                    active(it.readExpr(), it.readByteList())
                }

                1 -> DeferredDecode {
                    passive(it.readByteList())
                }

                2 -> DeferredDecode {
                    active(
                        it.readUInt(),
                        it.readExpr(),
                        it.readByteList()
                    )
                }

                else -> null
            }
        }

        fun passive(bytes: ByteString) =
            DataSegment(0u, null, bytes)

        fun active(offsetExpr: CodeBlob, bytes: ByteString) =
            DataSegment(0u, offsetExpr, bytes)

        fun active(memoryIndex: UInt, offsetExpr: CodeBlob, bytes: ByteString) =
            DataSegment(memoryIndex, offsetExpr, bytes)
    }
}