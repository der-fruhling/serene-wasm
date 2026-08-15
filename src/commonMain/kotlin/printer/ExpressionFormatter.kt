package net.derfruhling.serene.wasm.printer

import net.derfruhling.serene.wasm.instruction.*
import net.derfruhling.serene.wasm.instruction.Op.*
import net.derfruhling.serene.wasm.module.BlockType
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.RefType
import net.derfruhling.serene.wasm.module.ResultType
import net.derfruhling.serene.wasm.module.ValueType
import net.derfruhling.serene.wasm.module.printParams
import net.derfruhling.serene.wasm.pow
import net.derfruhling.serene.wasm.printer.Namespace.*
import kotlin.math.hypot

abstract class AbstractExpressionFormatter(val out: Printer) : InstructionVisitor,
    LegacyExceptionsVisitor {
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
            THROW,
            LEGACY_RETHROW ->
                TAG

            BRANCH,
            BRANCH_IF,
            BRANCH_ON_NULL,
            BRANCH_ON_NON_NULL ->
                null // label

            CALL,
            RETURN_CALL,
            REF_FUNC ->
                FUNC

            CALL_REF,
            RETURN_CALL_REF,
            STRUCT_NEW,
            STRUCT_NEW_DEFAULT,
            ARRAY_NEW,
            ARRAY_NEW_DEFAULT,
            ARRAY_GET,
            ARRAY_GET_S,
            ARRAY_GET_U,
            ARRAY_SET,
            ARRAY_FILL ->
                TYPE

            LOCAL_GET,
            LOCAL_SET,
            LOCAL_TEE ->
                LOCAL

            GLOBAL_GET,
            GLOBAL_SET ->
                GLOBAL

            TABLE_GET,
            TABLE_SET,
            TABLE_GROW,
            TABLE_SIZE,
            TABLE_FILL ->
                TABLE

            ELEM_DROP ->
                ELEM

            MEMORY_SIZE,
            MEMORY_GROW,
            MEMORY_FILL ->
                MEMORY

            else -> opFallback(op, OpType.SIMPLE_WITH_INDEX)
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
            CALL_INDIRECT, RETURN_CALL_INDIRECT ->
                TYPE to TABLE

            TABLE_INIT -> ELEM to TABLE
            TABLE_COPY -> TABLE to TABLE
            MEMORY_INIT -> DATA to MEMORY
            MEMORY_COPY -> MEMORY to MEMORY

            STRUCT_GET, STRUCT_GET_U, STRUCT_GET_S, STRUCT_SET ->
                TYPE to null /* field */

            ARRAY_NEW_FIXED -> TYPE to null /* length */
            ARRAY_NEW_DATA, ARRAY_INIT_DATA -> TYPE to DATA
            ARRAY_NEW_ELEM, ARRAY_INIT_ELEM -> TYPE to ELEM
            ARRAY_COPY -> TYPE to TYPE

            else -> opFallback(op, OpType.SIMPLE_WITH_INDEX_2)
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

            is ValueType ->
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
                is Catch.All -> {
                    word("catch_all")
                    word(catch.labelIndex.toString())
                }

                is Catch.AllRef -> {
                    word("catch_all_ref")
                    word(catch.labelIndex.toString())
                }

                is Catch.Ref -> {
                    word("catch_ref")
                    word(names.resolveName(TAG, catch.tagIndex))
                    word(catch.labelIndex.toString())
                }

                is Catch.Tag -> {
                    word("catch")
                    word(names.resolveName(TAG, catch.tagIndex))
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
            I32_LOAD8_S, I32_LOAD8_U,
            I64_LOAD8_S, I64_LOAD8_U,
            I32_STORE8,
            I64_STORE8,
            V128_LOAD8_SPLAT -> 1

            I32_LOAD16_S, I32_LOAD16_U,
            I64_LOAD16_S, I64_LOAD16_U,
            I32_STORE16,
            I64_STORE16,
            V128_LOAD16_SPLAT -> 2

            I32_LOAD,
            F32_LOAD,
            I32_STORE,
            F32_STORE,
            I64_LOAD32_S, I64_LOAD32_U,
            V128_LOAD32_SPLAT,
            V128_LOAD32_ZERO -> 4

            I64_LOAD,
            F64_LOAD,
            I64_STORE,
            F64_STORE,
            V128_LOAD64_SPLAT,
            V128_LOAD64_ZERO,
            V128_LOAD8x8_S, V128_LOAD8x8_U,
            V128_LOAD16x4_S, V128_LOAD16x4_U,
            V128_LOAD32x2_S, V128_LOAD32x2_U -> 8

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
            V128_LOAD8_LANE, V128_STORE8_LANE -> 1
            V128_LOAD16_LANE, V128_STORE16_LANE -> 2
            V128_LOAD32_LANE, V128_STORE32_LANE -> 4
            V128_LOAD64_LANE, V128_STORE64_LANE -> 8
            else -> opFallback(op, OpType.MEMARG_LANE)
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
        if (memArg.memoryIndex != 0u) out.word(out.names.resolveName(MEMORY, memArg.memoryIndex))
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

    override fun visitLegacyTry(op: Op, blockType: BlockType): LegacyTryBlockVisitor {
        out.appendIndented(op.opName)
        blockType.fixAndPrint(out)
        out.beginExprBlock()
        return LegacyTryBlock()
    }

    inner class NestedBlock : InstructionVisitor by this {
        override fun visitEnd() {
            out.endExprBlock()
            out.appendLine("end")
        }
    }

    inner class IfBlock : InstructionVisitor by this, IfBlockVisitor {
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

    inner class LegacyTryBlock : InstructionVisitor by this, LegacyTryBlockVisitor {
        override fun visitCatch(tagIndex: UInt): LegacyTryBlockVisitor {
            out.endExprBlock()
            out.word("catch")
            out.word(out.names.resolveName(TAG, tagIndex))
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

class ExpressionFormatter(out: Printer) : AbstractExpressionFormatter(out) {
    init {
        out.beginExprBlock()
    }

    override fun Printer.appendLineCond(string: String) {
        appendLineIndented(string)
    }

    override fun Printer.appendLineCond() {
        appendLine()
    }

    override fun visitEnd() {
        out.endExprBlock()
    }
}

class InlineExpressionFormatter(out: Printer) : AbstractExpressionFormatter(out) {
    init {
        out.beginInlineExprBlock()
    }

    override fun Printer.appendLineCond(string: String) {
        word(string)
    }

    override fun visitEnd() {
        out.endInlineExprBlock()
    }
}

class FunctionCodeFormatter(out: Printer) : AbstractExpressionFormatter(out) {
    init {
        out.beginInlineExprBlock()
    }

    override fun Printer.appendLineCond(string: String) {
        appendLineIndented(string)
    }

    override fun Printer.appendLineCond() {
        appendLine()
    }

    override fun visitEnd() {
        out.endInlineExprBlock()
    }
}
