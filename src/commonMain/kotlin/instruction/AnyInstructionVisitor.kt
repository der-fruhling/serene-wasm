package net.derfruhling.serene.wasm.instruction

import net.derfruhling.serene.wasm.UnstablePublicApi
import net.derfruhling.serene.wasm.module.BlockType
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.RefType
import net.derfruhling.serene.wasm.module.ValueType

@OptIn(UnstablePublicApi::class)
abstract class AnyInstructionVisitor : CompleteInstructionVisitor {
    abstract fun visit(op: Op)

    override fun visitSimple(op: Op) = visit(op)
    override fun visitSelect(op: Op, types: List<ValueType>) = visit(op)
    override fun visitIndex(op: Op, index: UInt) = visit(op)
    override fun visitIndices(op: Op, firstIndex: UInt, secondIndex: UInt) = visit(op)
    override fun visitBranchTable(op: Op, branches: List<UInt>, fallback: UInt) = visit(op)
    override fun visitBranchOnCast(op: Op, labelIndex: UInt, from: RefType, to: RefType) = visit(op)
    override fun visitBlock(op: Op, blockType: BlockType): InstructionVisitor = visit(op).let { NestedBlock() }
    override fun visitIf(op: Op, blockType: BlockType): IfBlockVisitor = visit(op).let { IfBlock() }
    override fun visitCatchTable(op: Op, blockType: BlockType, catches: List<Catch>): InstructionVisitor = visit(op).let { NestedBlock() }
    override fun visitMemoryOp(op: Op, memArg: MemArg) = visit(op)
    override fun visitMemoryOpWithLane(op: Op, memArg: MemArg, laneIndex: Byte) = visit(op)
    override fun visitWithHeapType(op: Op, type: HeapType) = visit(op)
    override fun visitConstI32(op: Op, value: Int) = visit(op)
    override fun visitConstI64(op: Op, value: Long) = visit(op)
    override fun visitConstF32(op: Op, value: Float) = visit(op)
    override fun visitConstF64(op: Op, value: Double) = visit(op)
    override fun visitConstV128(op: Op, value: VectorValue) = visit(op)
    override fun visitWithLane(op: Op, laneIndex: Byte) = visit(op)
    override fun visitEnd() {}

    @UnstablePublicApi
    override fun visitLegacyTry(op: Op, blockType: BlockType): LegacyTryBlockVisitor =
        visit(op).let { TryBlock() }

    private inner class NestedBlock : CompleteInstructionVisitor by this {
        override fun visitEnd() {
            visit(Op.END)
        }
    }

    private inner class IfBlock : CompleteInstructionVisitor by this, IfBlockVisitor {
        override fun visitElse(): InstructionVisitor {
            visit(Op.ELSE)
            return NestedBlock()
        }

        override fun visitEnd() {
            visit(Op.END)
        }
    }

    private inner class TryBlock : CompleteInstructionVisitor by this, LegacyTryBlockVisitor {
        override fun visitCatch(tagIndex: UInt): LegacyTryBlockVisitor {
            visit(Op.LEGACY_CATCH)
            return TryBlock()
        }

        override fun visitCatchAll(): InstructionVisitor {
            visit(Op.LEGACY_CATCH_ALL)
            return NestedBlock()
        }

        override fun visitDelegate(label: UInt) {
            visit(Op.LEGACY_TRY_DELEGATE)
        }

        override fun visitEnd() {
            visit(Op.END)
        }
    }
}

private class LambdaInstructionVisitor(val lambda: (Op) -> Unit) : AnyInstructionVisitor() {
    override fun visit(op: Op) {
        lambda(op)
    }
}

fun InstructionVisitor(fn: (Op) -> Unit): InstructionVisitor {
    return LambdaInstructionVisitor(fn)
}
