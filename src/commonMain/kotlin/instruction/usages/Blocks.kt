package net.derfruhling.serene.wasm.instruction.usages

internal fun Ctx.simpleBlockStart() {
    startBlock(isLoop = false)
}

internal fun Ctx.loopBlockStart() {
    startBlock(isLoop = true)
}

internal fun Ctx.ifBlockStart() {
    ifBlock(take().asCondition())
}

internal fun Ctx.legacyCatchUsage() {
    legacyCatchBlock(args()[0].asIndex())
}

internal fun Ctx.legacyTryDelegate() {
    val label = args()[0].asLabel()
    legacyTryDelegate(label)
}
