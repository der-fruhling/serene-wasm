package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsageContext
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.RefType

internal fun Ctx.refNull() {
    val type = args()[0].asType() as HeapType
    output { from(OpUsageContext.Value.Null.ofType(type)) }
}

internal fun Ctx.refIsNull() {
    val input = take()
    output { from(input.isNull) }
}

internal fun Ctx.refFunc() {
    val func = getFunction(args()[0].asIndex())
    output { from(func.asRef()) }
}

internal fun Ctx.refEq() {
    val b = take()
    val a = take()
    output { from(a isEqualTo b) }
}

internal fun Ctx.refAsNonNull() {
    val input = take()
    assert(input.type is RefType) { "Input type is not a ref type" }
    suggest({ input.isNull }) { "Input type is always null" }
    suggest({ (input.type !is RefType.Nullable).constant }) { "Input type is not nullable" }

    output {
        val refType = input.type
        if (refType is RefType.Nullable) {
            val value = input.withType(RefType.NonNull(refType.heapType))
            from(value)
        } else {
            from(input)
        }
    }
}

internal fun Ctx.refTestNonNull() {
    val input = take()
    val type = RefType.NonNull(args()[0].asType() as HeapType)
    assert(input.canDowncast(type)) { "${input.type} cannot possibly contain a value of ${type}" }
    output { from((input.type == type).constant) }
}

internal fun Ctx.refTestNull() {
    val input = take()
    val type = RefType.Nullable(args()[0].asType() as HeapType)
    assert(input.canDowncast(type)) { "${input.type} cannot possibly contain a value of ${type}" }
    output { from((input.type == type).constant) }
}

internal fun Ctx.refCastNonNull() {
    val input = take()
    val type = RefType.NonNull(args()[0].asType() as HeapType)
    assert(input.canDowncast(type)) { "${input.type} cannot possibly contain a value of ${type}" }
    output { from(input.withType(type)) }
}

internal fun Ctx.refCastNull() {
    val input = take()
    val type = RefType.Nullable(args()[0].asType() as HeapType)
    assert(input.canDowncast(type)) { "${input.type} cannot possibly contain a value of ${type}" }
    output { from(input.withType(type)) }
}

internal fun Ctx.convertExtern() {
    val value = take()
    output { from(value.wrapExtern()) }
}

internal fun Ctx.convertAny() {
    val value = take()
    output { from(value.unwrapExtern()) }
}

internal fun Ctx.refI31() {
    val value = take().asNumericValue()
    output { from(value.wrapI31()) }
}

internal fun Ctx.i31GetS() {
    val value = take()
    output { from(value.unwrapI31(signed = true)) }
}

internal fun Ctx.i31GetU() {
    val value = take()
    output { from(value.unwrapI31(signed = false)) }
}
