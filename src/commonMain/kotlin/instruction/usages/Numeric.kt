package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsage
import net.derfruhling.serene.wasm.module.NumericType
import net.derfruhling.serene.wasm.module.VectorType

internal fun Ctx.constI32() {
    val value = args()[0].asInt()
    output { from(value.constant) }
}

internal fun Ctx.constI64() {
    val value = args()[0].asLong()
    output { from(value.constant) }
}

internal fun Ctx.constF32() {
    val value = args()[0].asFloat()
    output { from(value.constant) }
}

internal fun Ctx.constF64() {
    val value = args()[0].asDouble()
    output { from(value.constant) }
}

internal fun Ctx.constV128() {
    val value = args()[0].asVector()
    output { from(value.constant) }
}

internal fun Ctx.v128Shuffle() {
    val value = args()[0].asVector().constant
    val vector = take().asVectorValue()
    output { from(opaqueOperator(vector, value)) }
}

internal fun extractLane(output: NumericType) = OpUsage {
    val vector = take().asVectorValue()
    /*val lane =*/ args()[0].asByte()

    output { type = output; from(vector) }
}

internal fun replaceLane(input: NumericType) = OpUsage {
    val lane = take().asNumericValue()
    val vector = take().asVectorValue()
    /*val lane =*/ args()[0].asByte()

    assert(lane.type == null || lane.type == input) { "Mismatched types: expected $input, got ${lane.type}" }

    output { type = VectorType.V128; from(opaqueOperator(vector, lane)) }
}