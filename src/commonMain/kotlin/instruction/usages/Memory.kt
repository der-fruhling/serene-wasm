package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsage
import net.derfruhling.serene.wasm.module.NumericType
import net.derfruhling.serene.wasm.module.Unpackable

internal fun Ctx.memorySize() {
    val memory = getMemory(args()[0].asIndex())
    output { from(memory.size) }
}

internal fun Ctx.memoryGrow() {
    val memory = getMemory(args()[0].asIndex())
    val growBy = take().asNumericValue()

    suggest({ memory.canGrow(growBy) }) { "Cannot grow memory by ${resolve(growBy)}" }

    val originalSize = memory.size.copy()
    memory.grow(growBy)
    output { from(originalSize) }
}

internal fun Ctx.memoryInit() {
    val args = args()
    val data = getData(args[0].asIndex())
    val memory = getMemory(args[1].asIndex())

    val count = take().asNumericValue()
    val dataOffset = take().asNumericValue()
    val memoryOffset = take().asNumericValue()

    assert((memoryOffset + count) isLessEqualTo memory.sizeBytes) { "Cannot copy beyond the memory boundaries" }
    assert((dataOffset + count) isLessEqualTo data.sizeBytes) { "Cannot copy beyond the data boundaries" }

    memory.write(memoryOffset, count, data)
}

internal fun Ctx.dropData() {
    getData(args()[0].asIndex()).drop()
}

internal fun Ctx.memoryCopy() {
    val args = args()
    val sourceMemory = getMemory(args[0].asIndex())
    val targetMemory = getMemory(args[1].asIndex())

    val count = take().asNumericValue()
    val targetOffset = take().asNumericValue()
    val sourceOffset = take().asNumericValue()

    assert((sourceOffset + count) isLessEqualTo sourceMemory.sizeBytes) { "Cannot copy beyond the source memory boundaries" }
    assert((targetOffset + count) isLessEqualTo targetMemory.sizeBytes) { "Cannot copy beyond the target memory boundaries" }
}

internal fun Ctx.memoryFill() {
    val table = getMemory(args()[0].asIndex())
    val count = take().asNumericValue()
    val value = take().asNumericValue()
    val offset = take().asNumericValue()

    assert(value.type == NumericType.I32) { "Incorrect type: expected i32, got ${value.type}" }
    assert((offset + count) isLessEqualTo table.sizeBytes) { "Cannot fill the memory beyond it's boundaries" }
}

internal fun load(type: Unpackable, bytes: Int) = OpUsage {
    val arg = args()[0].asMemArg()
    val address = take().asNumericValue()
    val memory = getMemory(arg.memoryIndex)

    val bytes = bytes.constant
    val offset = arg.offset.toLong().constant
    val memoryAddress = address + offset

    assert((memoryAddress + bytes) isLessEqualTo memory.sizeBytes) {
        "Cannot read data from outside of memory"
    }

    output { this.type = type; from(memory.read(memoryAddress, bytes)) }
}

internal fun loadLane(type: Unpackable, bytes: Int) = OpUsage {
    val args = args()
    val arg = args[0].asMemArg()
    /*val lane =*/ args[1].asByte()
    val address = take().asNumericValue()
    val memory = getMemory(arg.memoryIndex)

    val bytes = bytes.constant
    val offset = arg.offset.toLong().constant
    val memoryAddress = address + offset

    assert((memoryAddress + bytes) isLessEqualTo memory.sizeBytes) {
        "Cannot read data from outside of memory"
    }

    output { this.type = type; from(memory.read(memoryAddress, bytes)) }
}

internal fun store(type: Unpackable, bytes: Int) = OpUsage {
    val arg = args()[0].asMemArg()
    val value = take().asNumericValue()
    val address = take().asNumericValue()
    val memory = getMemory(arg.memoryIndex)

    val bytes = bytes.constant
    val offset = arg.offset.toLong().constant
    val memoryAddress = address + offset

    assert(type == value.type) { "Incorrect type: expected ${type}, got ${value.type}" }

    assert((memoryAddress + bytes) isLessEqualTo memory.sizeBytes) {
        "Cannot write data to outside of memory"
    }

    memory.write(memoryAddress, bytes, value)
}

internal fun storeLane(type: Unpackable, bytes: Int) = OpUsage {
    val args = args()
    val arg = args[0].asMemArg()
    /*val lane =*/ args[1].asByte()
    val value = take().asNumericValue()
    val address = take().asNumericValue()
    val memory = getMemory(arg.memoryIndex)

    val bytes = bytes.constant
    val offset = arg.offset.toLong().constant
    val memoryAddress = address + offset

    assert(type == value.type) { "Incorrect type: expected ${type}, got ${value.type}" }

    assert((memoryAddress + bytes) isLessEqualTo memory.sizeBytes) {
        "Cannot write data to outside of memory"
    }

    memory.write(memoryAddress, bytes, value)
}