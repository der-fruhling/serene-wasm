package net.derfruhling.serene.wasm.instruction.usages

import net.derfruhling.serene.wasm.instruction.OpUsage
import net.derfruhling.serene.wasm.module.NumericType
import net.derfruhling.serene.wasm.module.VectorType

internal val unary = OpUsage {
    val input = take()

    output { from(opaqueOperator(input)) }
}

internal val unaryBoolean = OpUsage {
    val input = take()

    output { type = NumericType.I32; from(opaqueOperator(input)) }
}

internal val binary = OpUsage {
    val b = take()
    val a = take()

    output { from(opaqueOperator(a, b)) }
}

internal val vectorShift = OpUsage {
    val b = take().asNumericValue()
    val a = take().asVectorValue()

    output { type = VectorType.V128; from(opaqueOperator(a, b)) }
}

internal val vectorBitmask = OpUsage {
    val vector = take().asVectorValue()

    output { type = NumericType.I32; from(opaqueOperator(vector)) }
}

internal val binaryBoolean = OpUsage {
    val b = take()
    val a = take()

    output { type = NumericType.I32; from(opaqueOperator(a, b)) }
}

internal val ternery = OpUsage {
    val c = take()
    val b = take()
    val a = take()

    output { from(opaqueOperator(a, b, c)) }
}