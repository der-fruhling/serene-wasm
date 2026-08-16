package net.derfruhling.serene.wasm.instruction.usages

internal fun Ctx.throwUsage() {
    val arg = args()[0].asIndex()
    throws(getTag(arg))
}

internal fun Ctx.throwRefUsage() {
    val tag = take().asTagRef()
    throws(tag)
}

internal fun Ctx.legacyRethrowUsage() {
    val arg = args()[0].asLabel()
    val value = take().asTagRef()
    legacyRethrows(arg, value)
}
