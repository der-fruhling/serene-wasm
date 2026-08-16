package net.derfruhling.serene.wasm.instruction

import net.derfruhling.serene.wasm.UnstablePublicApi
import net.derfruhling.serene.wasm.module.BlockType

@SubclassOptInRequired(UnstablePublicApi::class)
interface CompleteInstructionVisitor : InstructionVisitor, LegacyExceptionsVisitor {
    @UnstablePublicApi
    override fun visitLegacyTry(op: Op, blockType: BlockType): LegacyTryBlockVisitor
}
