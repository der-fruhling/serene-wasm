package net.derfruhling.serene.wasm.instruction.usages

internal fun Ctx.tableGet() {
    val table = getTable(args()[0].asIndex())
    val index = take().asNumericValue()

    output { from(table[index]) }
}

internal fun Ctx.tableSet() {
    val table = getTable(args()[0].asIndex())
    val value = take()
    val index = take().asNumericValue()

    table[index] = value
}

internal fun Ctx.tableInit() {
    val args = args()
    val elem = getElement(args[0].asIndex())
    val table = getTable(args[1].asIndex())

    val count = take().asNumericValue()
    val elemOffset = take().asNumericValue()
    val tableOffset = take().asNumericValue()

    assert((tableOffset + count) isLessEqualTo table.size) { "Cannot copy beyond the table boundaries" }
    assert((elemOffset + count) isLessEqualTo elem.size) { "Cannot copy beyond the element boundaries" }
}

internal fun Ctx.dropElem() {
    getElement(args()[0].asIndex()).drop()
}

internal fun Ctx.tableCopy() {
    val args = args()
    val sourceTable = getTable(args[0].asIndex())
    val targetTable = getTable(args[1].asIndex())

    val count = take().asNumericValue()
    val targetOffset = take().asNumericValue()
    val sourceOffset = take().asNumericValue()

    assert((sourceOffset + count) isLessEqualTo sourceTable.size) { "Cannot copy beyond the source table boundaries" }
    assert((targetOffset + count) isLessEqualTo targetTable.size) { "Cannot copy beyond the target table boundaries" }
}

internal fun Ctx.tableGrow() {
    val table = getTable(args()[0].asIndex())
    val growBy = take().asNumericValue()
    val refValue = take()

    assert(refValue.type == table.type.refType) { "Incorrect type: expected ${table.type.refType}, got ${refValue.type}" }
    suggest({ table.canGrow(growBy) }) { "Cannot grow table by ${resolve(growBy)}" }
}

internal fun Ctx.tableSize() {
    val table = getTable(args()[0].asIndex())
    output { from(table.size) }
}

internal fun Ctx.tableFill() {
    val table = getTable(args()[0].asIndex())
    val count = take().asNumericValue()
    val refValue = take()
    val offset = take().asNumericValue()

    assert(refValue.type == table.type.refType) { "Incorrect type: expected ${table.type.refType}, got ${refValue.type}" }
    assert((offset + count) isLessEqualTo table.size) { "Cannot fill the table beyond it's boundaries" }
}