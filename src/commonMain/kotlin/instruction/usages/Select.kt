package net.derfruhling.serene.wasm.instruction.usages

internal fun Ctx.select() {
    val condition = take().asCondition()
    val b = take()
    val a = take()

    output { from(ternery(condition, b, a)) }
}

internal fun Ctx.selectWithTypes() {
    val args = args()
    val condition = take().asCondition()
    val b = take()
    val a = take()

    if (args.isNotEmpty()) assert(a.type == args[0].asType()) { "Mismatched type of first argument: ${a.type} != ${args[0].asType()}" }
    if (args.size >= 2) assert(b.type == args[1].asType()) { "Mismatched type of first argument: ${b.type} != ${args[1].asType()}" }

    output { from(ternery(condition, b, a)) }
}
