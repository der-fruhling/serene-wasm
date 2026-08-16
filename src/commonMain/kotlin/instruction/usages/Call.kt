package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsageContext
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.HeapType

internal fun Ctx.callUsage() {
    call(getFunction(args()[0].asIndex()))
}

internal fun Ctx.callIndirectUsage() {
    call(indirectCall())
}

internal fun Ctx.returnCallUsage() {
    tailCall(getFunction(args()[0].asIndex()))
}

internal fun Ctx.returnCallIndirectUsage() {
    tailCall(indirectCall())
}

internal fun Ctx.callRefUsage() {
    val type = getType(args()[0].asIndex())
    assert(type is CompositeType.Func) { "Called type is not Func" }
    call(take().asFunctionFromRef())
}

internal fun Ctx.returnCallRefUsage() {
    val type = getType(args()[0].asIndex())
    assert(type is CompositeType.Func) { "Called type is not Func" }
    tailCall(take().asFunctionFromRef())
}

internal fun Ctx.indirectCall(): OpUsageContext.Function {
    val args = args()
    val type = getType(args[0].asIndex())
    val table = getTable(args[1].asIndex())
    assert(type is CompositeType.Func) { "Called type is not Func" }
    assert(table.type.refType.heapType == HeapType.Func) { "Referenced table is not Func" }

    val index = take().asNumericValue()
    return table[index].asFunctionFromRef()
}
