package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsage
import net.derfruhling.serene.wasm.instruction.OpUsageContext
import net.derfruhling.serene.wasm.module.ValueType

internal typealias Ctx = OpUsageContext

internal inline fun cvt(crossinline fn: () -> Pair<ValueType, ValueType>) = OpUsage {
    val (from, to) = fn()
    val input = take().asNumericValue()
    assert(input.type == null || input.type == from) { "Mismatched types: expected $from, got ${input.type}" }

    output { type = to; from(opaqueOperator(input)) }
}
