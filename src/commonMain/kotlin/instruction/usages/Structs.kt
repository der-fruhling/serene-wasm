package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsage
import net.derfruhling.serene.wasm.instruction.OpUsageContext
import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.Default
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.NumericType
import net.derfruhling.serene.wasm.module.RefType
import net.derfruhling.serene.wasm.module.VectorType

internal fun Ctx.structNew() {
    val type = getType(args()[0].asIndex()) as CompositeType.Struct

    val fields = arrayOfNulls<OpUsageContext.Input>(type.fields.size)
    for ((i, t) in type.fields.withIndex().reversed()) {
        val value = take()
        fields[i] = value
        assert(value.type == null || value.type == t.type) { "value[$i] expects type ${t.type}, but actually got ${value.type}" }
    }

    output {
        this.type = RefType.NonNull(HeapType.Struct)
        from(opaqueOperator(*fields.requireNoNulls()))
    }
}

internal fun Ctx.structNewDefault() {
    val type = getType(args()[0].asIndex()) as CompositeType.Struct

    val fields = arrayOfNulls<OpUsageContext.Value>(type.fields.size)
    for ((i, t) in type.fields.withIndex().reversed()) {
        assert(t.type is Default) { "Field $i is not defaultable" }
        val value = when (t.type as Default) {
            NumericType.F32     -> 0f.constant
            NumericType.F64     -> 0.0.constant
            NumericType.I32     -> 0.constant
            NumericType.I64     -> 0L.constant
            is RefType.Nullable -> OpUsageContext.Value.Null.ofType((t.type as RefType).heapType)
            VectorType.V128     -> zeroVector
        }
        fields[i] = value
    }

    output {
        this.type = RefType.NonNull(HeapType.Struct)
        from(opaqueOperator(*fields.requireNoNulls()))
    }
}

internal val structGet = OpUsage {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Struct
    val field = type.fields[args[1].asIndex().toInt()]

    val ref = take()
    suggest({ !ref.isNull }) { "Reference is always null" }

    output {
        this.type = field.type.valueType
        from(ref)
    }
}

internal fun Ctx.structSet() {
    val args = args()
    val type = getType(args[0].asIndex()) as CompositeType.Struct
    val fieldIndex = args[1].asIndex().toInt()
    val field = type.fields[fieldIndex]
    assert(field.isMutable) { "Field $fieldIndex is not mutable" }

    val value = take()
    val ref = take()
    suggest({ !ref.isNull }) { "Reference is always null" }
    assert(value.type == null || value.type == field.type) { "Mismatched type: expected ${field.type}, got ${value.type}" }
}