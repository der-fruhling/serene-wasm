package net.derfruhling.serene.wasm.printer

import net.derfruhling.serene.wasm.UnstablePublicApi
import net.derfruhling.serene.wasm.instruction.Catch
import net.derfruhling.serene.wasm.instruction.CompleteInstructionVisitor
import net.derfruhling.serene.wasm.instruction.IfBlockVisitor
import net.derfruhling.serene.wasm.instruction.InstructionVisitor
import net.derfruhling.serene.wasm.instruction.LegacyExceptionsVisitor
import net.derfruhling.serene.wasm.instruction.LegacyTryBlockVisitor
import net.derfruhling.serene.wasm.instruction.MemArg
import net.derfruhling.serene.wasm.instruction.Op
import net.derfruhling.serene.wasm.instruction.OpType
import net.derfruhling.serene.wasm.instruction.VectorValue
import net.derfruhling.serene.wasm.module.BlockType
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.RefType
import net.derfruhling.serene.wasm.module.ResultType
import net.derfruhling.serene.wasm.module.ValueType
import net.derfruhling.serene.wasm.module.printParams
import net.derfruhling.serene.wasm.pow

@OptIn(UnstablePublicApi::class)
abstract class AbstractExpressionFormatter(val out: Printer) : CompleteInstructionVisitor {
    protected abstract fun Printer.appendLineCond(string: String)
    protected open fun Printer.appendLineCond() {}

    override fun visitSimple(op: Op) {
        out.appendLineCond(op.opName)
    }

    override fun visitSelect(
        op: Op,
        types: List<ValueType>
    ) {
        out.appendIndented(op.opName)
        out.wrapInline {
            word("result")
            for (type in types) {
                type.print(this)
            }
        }
    }

    private fun opFallback(op: Op, type: OpType): Nothing {
        if (op.type != type) {
            throw IllegalArgumentException("Op $op has type ${op.type}, but this function should only be called with type $type")
        } else {
            throw IllegalStateException("serene-wasm: missed op $op in $type printer")
        }
    }

    override fun visitIndex(op: Op, index: UInt) {
        val ns = when (op) {
            Op.THROW,
            Op.LEGACY_RETHROW
                         ->
                Namespace.TAG

            Op.BRANCH,
            Op.BRANCH_IF,
            Op.BRANCH_ON_NULL,
            Op.BRANCH_ON_NON_NULL
                         ->
                null // label

            Op.CALL,
            Op.RETURN_CALL,
            Op.REF_FUNC
                         ->
                Namespace.FUNC

            Op.CALL_REF,
            Op.RETURN_CALL_REF,
            Op.STRUCT_NEW,
            Op.STRUCT_NEW_DEFAULT,
            Op.ARRAY_NEW,
            Op.ARRAY_NEW_DEFAULT,
            Op.ARRAY_GET,
            Op.ARRAY_GET_S,
            Op.ARRAY_GET_U,
            Op.ARRAY_SET,
            Op.ARRAY_FILL
                         ->
                Namespace.TYPE

            Op.LOCAL_GET,
            Op.LOCAL_SET,
            Op.LOCAL_TEE
                         ->
                Namespace.LOCAL

            Op.GLOBAL_GET,
            Op.GLOBAL_SET
                         ->
                Namespace.GLOBAL

            Op.TABLE_GET,
            Op.TABLE_SET,
            Op.TABLE_GROW,
            Op.TABLE_SIZE,
            Op.TABLE_FILL
                         ->
                Namespace.TABLE

            Op.ELEM_DROP ->
                Namespace.ELEM

            Op.MEMORY_SIZE,
            Op.MEMORY_GROW,
            Op.MEMORY_FILL
                         ->
                Namespace.MEMORY

            else         -> opFallback(op, OpType.SIMPLE_WITH_INDEX)
        }

        out.appendLineCond(
            "${op.opName} ${
                ns?.let {
                    out.names.resolveName(
                        it,
                        index
                    )
                } ?: index.toString()
            }")
    }

    override fun visitIndices(
        op: Op,
        firstIndex: UInt,
        secondIndex: UInt
    ) {
        val (ns1: Namespace?, ns2: Namespace?) = when (op) {
            Op.CALL_INDIRECT, Op.RETURN_CALL_INDIRECT                      ->
                Namespace.TYPE to Namespace.TABLE

            Op.TABLE_INIT                                                  -> Namespace.ELEM to Namespace.TABLE
            Op.TABLE_COPY                                                  -> Namespace.TABLE to Namespace.TABLE
            Op.MEMORY_INIT                                                 -> Namespace.DATA to Namespace.MEMORY
            Op.MEMORY_COPY                                                 -> Namespace.MEMORY to Namespace.MEMORY

            Op.STRUCT_GET, Op.STRUCT_GET_U, Op.STRUCT_GET_S, Op.STRUCT_SET ->
                Namespace.TYPE to null /* field */

            Op.ARRAY_NEW_FIXED                                             -> Namespace.TYPE to null /* length */
            Op.ARRAY_NEW_DATA, Op.ARRAY_INIT_DATA                          -> Namespace.TYPE to Namespace.DATA
            Op.ARRAY_NEW_ELEM, Op.ARRAY_INIT_ELEM                          -> Namespace.TYPE to Namespace.ELEM
            Op.ARRAY_COPY                                                  -> Namespace.TYPE to Namespace.TYPE

            else                                                           -> opFallback(op, OpType.SIMPLE_WITH_INDEX_2)
        }

        val d1 = ns1?.let { out.names.resolveName(it, firstIndex) } ?: firstIndex.toString()
        val d2 = ns2?.let { out.names.resolveName(it, secondIndex) } ?: secondIndex.toString()

        out.appendLineCond("${op.opName} $d1 $d2")
    }

    override fun visitBranchTable(
        op: Op,
        branches: List<UInt>,
        fallback: UInt
    ) {
        out.appendIndented(op.opName)

        for (b in branches) {
            out.word(b.toString())
        }

        out.word(fallback.toString())
        out.appendLineCond()
    }

    override fun visitBranchOnCast(
        op: Op,
        labelIndex: UInt,
        from: RefType,
        to: RefType
    ) {
        out.appendIndented(op.opName)
        out.word(labelIndex.toString())
        from.print(out)
        to.print(out)
        out.appendLineCond()
    }

    private fun BlockType.fixAndPrint(printer: Printer) {
        return when(this) {
            is BlockType.Void, is BlockType.ByIndex ->
                print(printer)

            is ValueType                            ->
                CompositeType.Func(
                    ResultType(emptyList()),
                    ResultType(listOf(this))
                ).printParams(printer)
        }
    }

    override fun visitBlock(
        op: Op,
        blockType: BlockType
    ): InstructionVisitor {
        out.appendIndented(op.opName)
        blockType.fixAndPrint(out)
        out.beginExprBlock()
        return NestedBlock()
    }

    override fun visitIf(
        op: Op,
        blockType: BlockType
    ): IfBlockVisitor {
        out.appendIndented(op.opName)
        blockType.fixAndPrint(out)
        out.beginExprBlock()
        return IfBlock()
    }

    override fun visitCatchTable(
        op: Op,
        blockType: BlockType,
        catches: List<Catch>
    ): InstructionVisitor {
        out.appendIndented(op.opName)
        blockType.fixAndPrint(out)

        for (catch in catches) out.wrapInline {
            when (catch) {
                is Catch.All    -> {
                    word("catch_all")
                    word(catch.labelIndex.toString())
                }

                is Catch.AllRef -> {
                    word("catch_all_ref")
                    word(catch.labelIndex.toString())
                }

                is Catch.Ref    -> {
                    word("catch_ref")
                    word(names.resolveName(Namespace.TAG, catch.tagIndex))
                    word(catch.labelIndex.toString())
                }

                is Catch.Tag    -> {
                    word("catch")
                    word(names.resolveName(Namespace.TAG, catch.tagIndex))
                    word(catch.labelIndex.toString())
                }
            }
        }

        out.beginExprBlock()
        return NestedBlock()
    }

    override fun visitMemoryOp(
        op: Op,
        memArg: MemArg
    ) {
        val defaultAlign = when (op) {
            Op.I32_LOAD8_S, Op.I32_LOAD8_U,
            Op.I64_LOAD8_S, Op.I64_LOAD8_U,
            Op.I32_STORE8,
            Op.I64_STORE8,
            Op.V128_LOAD8_SPLAT
                 -> 1

            Op.I32_LOAD16_S, Op.I32_LOAD16_U,
            Op.I64_LOAD16_S, Op.I64_LOAD16_U,
            Op.I32_STORE16,
            Op.I64_STORE16,
            Op.V128_LOAD16_SPLAT
                 -> 2

            Op.I32_LOAD,
            Op.F32_LOAD,
            Op.I32_STORE,
            Op.F32_STORE,
            Op.I64_LOAD32_S, Op.I64_LOAD32_U,
            Op.V128_LOAD32_SPLAT,
            Op.V128_LOAD32_ZERO
                 -> 4

            Op.I64_LOAD,
            Op.F64_LOAD,
            Op.I64_STORE,
            Op.F64_STORE,
            Op.V128_LOAD64_SPLAT,
            Op.V128_LOAD64_ZERO,
            Op.V128_LOAD8x8_S, Op.V128_LOAD8x8_U,
            Op.V128_LOAD16x4_S, Op.V128_LOAD16x4_U,
            Op.V128_LOAD32x2_S, Op.V128_LOAD32x2_U
                 -> 8

            else -> opFallback(op, OpType.MEMARG)
        }

        commonMemoryOp(memArg, op, defaultAlign)
        out.appendLineCond()
    }

    override fun visitMemoryOpWithLane(
        op: Op,
        memArg: MemArg,
        laneIndex: Byte
    ) {
        val defaultAlign = when(op) {
            Op.V128_LOAD8_LANE, Op.V128_STORE8_LANE   -> 1
            Op.V128_LOAD16_LANE, Op.V128_STORE16_LANE -> 2
            Op.V128_LOAD32_LANE, Op.V128_STORE32_LANE -> 4
            Op.V128_LOAD64_LANE, Op.V128_STORE64_LANE -> 8
            else                                      -> opFallback(op, OpType.MEMARG_LANE)
        }

        commonMemoryOp(memArg, op, defaultAlign)
        out.word(laneIndex.toString())
        out.appendLineCond()
    }

    private fun commonMemoryOp(
        memArg: MemArg,
        op: Op,
        defaultAlign: Int
    ) {
        val actualAlign = 2 pow memArg.alignment.toInt()

        out.appendIndented(op.opName)
        if (memArg.memoryIndex != 0u) out.word(out.names.resolveName(Namespace.MEMORY, memArg.memoryIndex))
        if (memArg.offset > 0uL) out.word("offset=${memArg.offset}")
        if (actualAlign != defaultAlign) out.word("align=${actualAlign}")
        out.appendLineCond()
    }

    override fun visitWithHeapType(
        op: Op,
        type: HeapType
    ) {
        out.appendIndented(op.opName)
        type.print(out)
        out.appendLineCond()
    }

    override fun visitConstI32(op: Op, value: Int) {
        out.appendLineCond("${op.opName} $value")
    }

    override fun visitConstI64(op: Op, value: Long) {
        out.appendLineCond("${op.opName} $value")
    }

    override fun visitConstF32(
        op: Op,
        value: Float
    ) {
        out.appendLineCond("${op.opName} ${
            when {
                value.isInfinite() -> if(value < 0) "-inf" else "inf"
                value.isNaN() -> "nan"
                else -> value.toString()
            }
        }")
    }

    override fun visitConstF64(
        op: Op,
        value: Double
    ) {
        out.appendLineCond("${op.opName} ${
            when {
                value.isInfinite() -> if (value < 0) "-inf" else "inf"
                value.isNaN() -> "nan"
                else -> value.toString()
            }
        }")
    }

    override fun visitConstV128(
        op: Op,
        value: VectorValue
    ) {
        out.appendIndented(op.opName)
        out.word("i8x16")

        for(byte in value.bytes.toByteArray()) {
            out.word(byte.toString())
        }

        out.appendLineCond()
    }

    override fun visitWithLane(
        op: Op,
        laneIndex: Byte
    ) {
        out.appendLineCond("${op.opName} $laneIndex")
    }

    @UnstablePublicApi
    override fun visitLegacyTry(op: Op, blockType: BlockType): LegacyTryBlockVisitor {
        out.appendIndented(op.opName)
        blockType.fixAndPrint(out)
        out.beginExprBlock()
        return LegacyTryBlock()
    }

    @UnstablePublicApi
    protected inner class NestedBlock : InstructionVisitor by this {
        override fun visitEnd() {
            out.endExprBlock()
            out.appendLine("end")
        }
    }

    @UnstablePublicApi
    protected inner class IfBlock : InstructionVisitor by this, IfBlockVisitor {
        override fun visitElse(): InstructionVisitor {
            out.endExprBlock()
            out.append("else")
            out.beginExprBlock()
            return this
        }

        override fun visitEnd() {
            out.endExprBlock()
            out.appendLine("end")
        }
    }

    @UnstablePublicApi
    protected inner class LegacyTryBlock : InstructionVisitor by this, LegacyTryBlockVisitor {
        override fun visitCatch(tagIndex: UInt): LegacyTryBlockVisitor {
            out.endExprBlock()
            out.word("catch")
            out.word(out.names.resolveName(Namespace.TAG, tagIndex))
            out.beginExprBlock()
            return LegacyTryBlock()
        }

        override fun visitCatchAll(): InstructionVisitor {
            out.endExprBlock()
            out.word("catch_all")
            out.beginExprBlock()
            return NestedBlock()
        }

        override fun visitDelegate(label: UInt) {
            out.indent--
            out.appendIndented("delegate")
            out.word(label.toString())
        }

        override fun visitEnd() {
            out.endExprBlock()
            out.appendLine("end")
        }
    }
}