package net.derfruhling.serene.wasm.instruction

import net.derfruhling.serene.wasm.module.BlockType

interface LegacyExceptionsVisitor {
    fun visitLegacyTry(op: Op, blockType: BlockType): LegacyTryBlockVisitor
}
