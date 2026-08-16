package net.derfruhling.serene.wasm.instruction.usages

internal fun Ctx.localGet() {
    val local = getLocal(args()[0].asIndex())
    output { from(local.value) }
}

internal fun Ctx.localSet() {
    val local = getLocal(args()[0].asIndex())
    local.value = take()
}

internal fun Ctx.localTee() {
    val local = getLocal(args()[0].asIndex())
    val value = take()
    local.value = value
    output { from(value) }
}

internal fun Ctx.globalGet() {
    val local = getGlobal(args()[0].asIndex())
    output { from(local.value) }
}

internal fun Ctx.globalSet() {
    val local = getGlobal(args()[0].asIndex())
    val value = take()

    local.value = value
}
