package net.derfruhling.serene.wasm

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.write
import net.derfruhling.serene.wasm.instruction.InstructionVisitor
import kotlin.jvm.JvmInline

class CodeBlob(val byteString: ByteString) {
    fun visit(visitor: InstructionVisitor) {
        WasmReader(Buffer().also { it.write(byteString) }).visitExpr(visitor)
    }

    private class CodeBlobIsMultiInstruction : RuntimeException()

    val isSimpleInlineExpr: Boolean by lazy {
        try {
            var once = false
            visit(InstructionVisitor {
                if (!once) {
                    once = true
                } else {
                    throw CodeBlobIsMultiInstruction()
                }
            })

            true
        } catch (_: CodeBlobIsMultiInstruction) {
            false
        }
    }
}
