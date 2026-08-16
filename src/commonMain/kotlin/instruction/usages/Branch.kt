package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.hasBit
import net.derfruhling.serene.wasm.instruction.OpUsageContext
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.RefType

internal fun Ctx.branchUsage() {
    branchTo(args()[0].asLabel())
}

internal fun Ctx.branchIfUsage() {
    val label = args()[0].asLabel()
    ifThen(take().asCondition()) {
        branchTo(label)
    }
}

internal fun Ctx.branchTableUsage() {
    val args = args()
    assert(args.isNotEmpty()) { "Must have at least one label" }

    val conditionalBranches = args.sliceArray(0..<args.size - 1)
    val condition = take().asNumericValue()

    for ((i, arg) in conditionalBranches.withIndex()) {
        val label = arg.asLabel()
        ifThen(condition.isEqualTo(i.constant)) {
            branchTo(label)
        }
    }

    branchTo(args.last().asLabel())
}

internal fun Ctx.branchNullUsage() {
    val input = take()
    val label = args()[0].asLabel()

    ifThen(input.isNull) {
        branchTo(label)
    }
}

internal fun Ctx.branchNonNullUsage() {
    val input = take()
    val label = args()[0].asLabel()

    ifThen(!input.isNull) {
        branchTo(label)
    }
}

internal fun Ctx.branchOnCast() {
    val (canDowncast, label) = castBranch()

    ifThen(canDowncast) {
        branchTo(label)
    }
}

internal fun Ctx.branchOnCastFail() {
    val (canDowncast, label) = castBranch()

    ifThen(!canDowncast) {
        branchTo(label)
    }
}

internal fun Ctx.castBranch(): Pair<OpUsageContext.Condition, OpUsageContext.Label> {
    val castOp = args()[0].asByte()
    val label = args()[1].asLabel()
    val from = args()[2].asType() as HeapType
    val to = args()[3].asType() as HeapType

    val fromRef = if (castOp.hasBit(0)) RefType.Nullable(from) else RefType.NonNull(from)
    val toRef = if (castOp.hasBit(1)) RefType.Nullable(to) else RefType.NonNull(to)

    val value = take()
    assert(value.type == null || value.type == fromRef) { "Incorrect type: expected $fromRef, got ${value.type}" }
    return value.canDowncast(toRef) to label
}
