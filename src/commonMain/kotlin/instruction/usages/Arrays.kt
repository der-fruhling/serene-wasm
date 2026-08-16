package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsage
import net.derfruhling.serene.wasm.instruction.OpUsageContext
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.Default
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.RefType
import net.derfruhling.serene.wasm.module.Unpackable

internal fun Ctx.arrayNew() {
    val type = getType(args()[0].asIndex()) as CompositeType.Array
    val size = take().asNumericValue()
    val value = take()
    assert(size isGreaterEqualTo 0.constant) { "Size cannot be negative" }
    assert(value.type == null || value.type == type.type.type) { "Mismatched type: expected ${type.type.type}, got ${value.type}" }

    output {
        this.type = RefType.NonNull(HeapType.Array)
        from(opaqueOperator(size, value))
    }
}

internal fun Ctx.arrayNewDefault() {
    val type = getType(args()[0].asIndex()) as CompositeType.Array
    val size = take().asNumericValue()
    assert(size isGreaterEqualTo 0.constant) { "Size cannot be negative" }
    assert(type.type.type is Default) { "Array element type is not defaultable" }

    output {
        this.type = RefType.NonNull(HeapType.Array)
        from(size)
    }
}

internal fun Ctx.arrayNewFixed() {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Array
    val size = args[1].asUInt()
    assert(size >= 0u) { "Size cannot be negative" }

    val values = (0..<size.toInt()).map { i ->
        val value = take()
        assert(value.type == null || value.type == type.type.type) { "Mismatched type for field $i: expected ${type.type.type}, got ${value.type}" }
        value
    }

    output {
        this.type = RefType.NonNull(HeapType.Array)
        from(opaqueOperator(values))
    }
}

internal fun Ctx.arrayNewData() {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Array
    val data = getData(args[1].asIndex())
    val size = take().asNumericValue()
    val offset = take().asNumericValue()
    assert(size isGreaterEqualTo 0.constant) { "Size cannot be negative" }
    val unpackType = type.type.type as Unpackable

    val values = mutableListOf<OpUsageContext.Value>()
    val unpackSize = unpackType.size.constant
    forEach(offset..<(offset + (size * unpackSize)) step unpackType.size) {
        values.add(data.read(it, unpackSize))
    }

    output {
        this.type = RefType.NonNull(HeapType.Array)
        from(opaqueOperator(values))
    }
}

internal fun Ctx.arrayNewElem() {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Array
    val elem = getElement(args[1].asIndex())
    val size = take().asNumericValue()
    val offset = take().asNumericValue()
    assert(size isGreaterEqualTo 0.constant) { "Size cannot be negative" }
    assert(type.type.type == elem.type) { "Mismatched types: expected ${type.type.type}, got ${elem.type}" }

    val values = mutableListOf<OpUsageContext.Value>()
    forEach(offset..(offset + size)) {
        values.add(elem[it])
    }

    output {
        this.type = RefType.NonNull(HeapType.Array)
        from(opaqueOperator(values))
    }
}

internal val arrayGet = OpUsage {
    val type = getType(args()[0].asIndex()) as CompositeType.Array

    val index = take().asNumericValue()
    val ref = take()
    suggest({ !ref.isNull }) { "Reference is always null" }
    suggest({ index isGreaterEqualTo 0.constant }) { "Index is always negative" }

    output {
        this.type = type.type.type.valueType
        from(opaqueOperator(ref, index))
    }
}

internal fun Ctx.arraySet() {
    val type = getType(args()[0].asIndex()) as CompositeType.Array

    val value = take()
    val index = take().asNumericValue()
    val ref = take()
    assert(type.type.isMutable) { "Array is not mutable" }
    suggest({ !ref.isNull }) { "Reference is always null" }
    suggest({ index isGreaterEqualTo 0.constant }) { "Index is always negative" }
    assert(value.type == type.type.type.valueType) { "Mismatched types: expected ${type.type.type.valueType}, got ${value.type}" }
}

internal fun Ctx.arrayLen() {
    val ref = take().asArray()
    output { from(ref.size) }
}

internal fun Ctx.arrayFill() {
    val type = getType(args()[0].asIndex()) as CompositeType.Array

    val count = take().asNumericValue()
    val value = take()
    val offset = take().asNumericValue()
    val ref = take()
    assert(type.type.isMutable) { "Array is not mutable" }
    suggest({ !ref.isNull }) { "Reference is always null" }
    suggest({ offset isGreaterEqualTo 0.constant }) { "Index is always negative" }
    assert(value.type == type.type.type.valueType) { "Mismatched types: expected ${type.type.type.valueType}, got ${value.type}" }
}

internal fun Ctx.arrayCopy() {
    val sourceType = getType(args()[0].asIndex()) as CompositeType.Array
    val targetType = getType(args()[1].asIndex()) as CompositeType.Array

    val count = take().asNumericValue()
    val sourceOffset = take().asNumericValue()
    val sourceRef = take().asArray()
    val targetOffset = take().asNumericValue()
    val targetRef = take().asArray()
    assert(targetType.type.isMutable) { "Array is not mutable" }
    suggest({ !sourceRef.isNull }) { "Source reference is always null" }
    suggest({ !targetRef.isNull }) { "Target reference is always null" }
    suggest({ sourceOffset isGreaterEqualTo 0.constant }) { "Source offset is always negative" }
    suggest({ targetOffset isGreaterEqualTo 0.constant }) { "Target offset is always negative" }
    suggest({ count isGreaterEqualTo 0.constant }) { "Count is always negative" }
    assert(sourceType.type.type.valueType == targetType.type.type.valueType) { "Mismatched types: expected ${targetType.type.type.valueType}, got ${sourceType.type.type.valueType}" }
}

internal fun Ctx.arrayInitData() {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Array
    val data = getData(args[1].asIndex())
    val size = take().asNumericValue()
    var dataOffset = take().asNumericValue()
    val arrayOffset = take().asNumericValue()
    val array = take().asArray()
    assert(size isGreaterEqualTo 0.constant) { "Size cannot be negative" }
    val unpackType = type.type.type as Unpackable

    val unpackSize = unpackType.size.constant
    forEach(0.constant..size) {
        array[arrayOffset + it] = data.read(dataOffset, unpackSize)
        dataOffset += unpackSize
    }
}

internal fun Ctx.arrayInitElem() {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Array
    val elem = getElement(args[1].asIndex())
    val size = take().asNumericValue()
    val elemOffset = take().asNumericValue()
    val arrayOffset = take().asNumericValue()
    val array = take().asArray()
    assert(size isGreaterEqualTo 0.constant) { "Size cannot be negative" }
    assert(type.type.isMutable) { "Array is not mutable" }
    assert(type.type.type == elem.type) { "Mismatched types: expected ${type.type.type}, got ${elem.type}" }

    forEach(0.constant..size) {
        array[arrayOffset + it] = elem[elemOffset + it]
    }
}