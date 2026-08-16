@file:Suppress("unused")

package net.derfruhling.serene.wasm.instruction

import net.derfruhling.serene.wasm.instruction.OpContract.Args
import net.derfruhling.serene.wasm.instruction.OpType.*
import net.derfruhling.serene.wasm.instruction.usages.*
import net.derfruhling.serene.wasm.module.BlockType
import net.derfruhling.serene.wasm.module.HeapType
import net.derfruhling.serene.wasm.module.NumericType.*
import net.derfruhling.serene.wasm.module.RefType
import net.derfruhling.serene.wasm.module.Type
import net.derfruhling.serene.wasm.module.VectorType.V128
import net.derfruhling.serene.wasm.instruction.OpContract.Args as OpArgs
import net.derfruhling.serene.wasm.instruction.OpContract.RequiresSpecialHandling as OpRequiresSpecialHandling

@Suppress("EnumEntryName")
enum class Op(
    val opName: String,
    val type: OpType,
    val opcode: UByte,
    val ext: Int = -1,
    val usage: OpUsage = OpUsage.NULL
) {
    // parametric instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#parametric-instructions
    @OpContract("_ -> trap")
    UNREACHABLE("unreachable", SIMPLE, 0x00u), NOP("nop", SIMPLE, 0x01u),

    @OpContract("any -> _")
    DROP("drop", SIMPLE, 0x1Au, usage = Ctx::take),

    @OpContract("any:T, any:T, i32 -> @3 ? @2 : @1")
    SELECT("select", SIMPLE, 0x1Bu, usage = Ctx::select),

    @OpContract("any:T, any:U, i32 -> @3 ? @2 : @1")
    @OpArgs(Type::class, Type::class)
    SELECT_TYPES("select", SELECT_ARGS, 0x1Cu, usage = Ctx::selectWithTypes),

    // control instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#control-instructions
    @OpContract.Block(isLoop = false)
    BLOCK("block", BLOCK_START, 0x02u, usage = Ctx::simpleBlockStart),

    @OpContract.Block(isLoop = true)
    LOOP("loop", BLOCK_START, 0x03u, usage = Ctx::loopBlockStart),

    @OpContract.Block(isLoop = false)
    @OpContract("i32, block -> if_block")
    IF("if", BLOCK_IF, 0x04u, usage = Ctx::ifBlockStart),

    @OpContract.BlockControl("else")
    ELSE("else", BLOCK_CONTROL, 0x05u, usage = Ctx::elseBlock),

    @OpContract.Block(isLoop = false)
    LEGACY_TRY("try", LEGACY_EXCEPTIONS, 0x06u, usage = Ctx::simpleBlockStart),

    @OpContract.BlockControl("catch")
    LEGACY_CATCH("catch", LEGACY_EXCEPTIONS, 0x07u, usage = Ctx::legacyCatchUsage),

    @OpContract("_ -> throw")
    @OpArgs(UInt::class)
    THROW("throw", SIMPLE_WITH_INDEX, 0x08u, usage = Ctx::throwUsage),

    @OpContract("(ref exn) -> throw; null -> trap")
    @OpArgs(UInt::class)
    LEGACY_RETHROW("rethrow", SIMPLE_WITH_INDEX, 0x09u, usage = Ctx::legacyRethrowUsage),

    @OpContract("(ref exn) -> throw; null -> trap")
    THROW_REF("throw_ref", SIMPLE, 0x0Au, usage = Ctx::throwRefUsage),

    @OpContract.BlockControl("end")
    END("end", BLOCK_CONTROL, 0x0Bu, usage = Ctx::endBlock),

    @OpContract("_ -> branch")
    @OpArgs(UInt::class)
    BRANCH("br", SIMPLE_WITH_INDEX, 0x0Cu, usage = Ctx::branchUsage),

    @OpContract("true -> branch; false -> _")
    @OpArgs(UInt::class)
    BRANCH_IF("br_if", SIMPLE_WITH_INDEX, 0x0Du, usage = Ctx::branchIfUsage),

    @OptIn(ExperimentalUnsignedTypes::class)
    @OpContract("i32 -> branch")
    @OpArgs(UIntArray::class, UInt::class)
    BRANCH_TABLE("br_table", OpType.BRANCH_TABLE, 0x0Eu, usage = Ctx::branchTableUsage),

    @OpContract("results -> return")
    RETURN("return", SIMPLE, 0x0Fu, usage = Ctx::returns),

    @OpContract("fn_args -> fn_results")
    @OpArgs(UInt::class)
    CALL("call", SIMPLE_WITH_INDEX, 0x10u, usage = Ctx::callUsage),

    @OpContract("fn_args, i32 -> fn_results")
    @OpArgs(UInt::class, UInt::class)
    CALL_INDIRECT("call_indirect", SIMPLE_WITH_INDEX_2, 0x11u, usage = Ctx::callIndirectUsage),

    @OpContract("fn_args -> return")
    @OpArgs(UInt::class)
    RETURN_CALL("return_call", SIMPLE_WITH_INDEX, 0x12u, usage = Ctx::returnCallUsage),

    @OpContract("fn_args, i32 -> return")
    @OpArgs(UInt::class, UInt::class)
    RETURN_CALL_INDIRECT(
        "return_call_indirect", SIMPLE_WITH_INDEX_2, 0x13u, usage = Ctx::returnCallIndirectUsage
    ),

    @OpContract("fn_args, (ref func) -> fn_results; null -> trap")
    @OpArgs(UInt::class)
    CALL_REF("call_ref", SIMPLE_WITH_INDEX, 0x14u, usage = Ctx::callRefUsage),

    @OpContract("fn_args, (ref func) -> return; null -> trap")
    @OpArgs(UInt::class)
    RETURN_CALL_REF("return_call_ref", SIMPLE_WITH_INDEX, 0x15u, usage = Ctx::returnCallRefUsage),

    @OpContract.BlockControl("legacy_try_delegate")
    LEGACY_TRY_DELEGATE("delegate", LEGACY_EXCEPTIONS, 0x18u, usage = Ctx::legacyTryDelegate),

    @OpContract.BlockControl("catch_all")
    LEGACY_CATCH_ALL("catch_all", LEGACY_EXCEPTIONS, 0x19u, usage = Ctx::legacyCatchAllBlock),

    @OpContract.Block(isLoop = false)
    @Args(BlockType::class, Catch::class, lastRepeating = true)
    TRY_TABLE("try_table", CATCH_TABLE, 0x1Fu, usage = Ctx::simpleBlockStart),

    @OpContract("null -> branch; !null -> _")
    @OpArgs(UInt::class)
    BRANCH_ON_NULL("br_on_null", SIMPLE_WITH_INDEX, 0xD5u, usage = Ctx::branchNullUsage),

    @OpContract("null -> branch; !null -> _")
    @OpArgs(UInt::class)
    BRANCH_ON_NON_NULL("br_on_non_null", SIMPLE_WITH_INDEX, 0xD6u, usage = Ctx::branchNonNullUsage),

    @OpContract("(ref T) :> (ref U) -> branch; else -> _")
    @OpArgs(UInt::class, RefType::class, RefType::class)
    BRANCH_ON_CAST("br_on_cast", OpType.BRANCH_ON_CAST, 0xFBu, 24, usage = Ctx::branchOnCast),

    @OpContract("(ref T) :> (ref U) -> _; else -> branch")
    @OpArgs(UInt::class, RefType::class, RefType::class)
    BRANCH_ON_CAST_FAIL(
        "br_on_cast_fail", OpType.BRANCH_ON_CAST, 0xFBu, 25, usage = Ctx::branchOnCastFail
    ),

    // variable instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#variable-instructions
    @OpContract("_ -> local")
    @OpArgs(UInt::class)
    LOCAL_GET("local.get", SIMPLE_WITH_INDEX, 0x20u, usage = Ctx::localGet),

    @OpContract("local -> _")
    @OpArgs(UInt::class)
    LOCAL_SET("local.set", SIMPLE_WITH_INDEX, 0x21u, usage = Ctx::localSet),

    @OpContract("local -> local")
    @OpArgs(UInt::class)
    LOCAL_TEE("local.tee", SIMPLE_WITH_INDEX, 0x22u, usage = Ctx::localTee),

    @OpContract("_ -> global")
    @OpArgs(UInt::class)
    GLOBAL_GET("global.get", SIMPLE_WITH_INDEX, 0x23u, usage = Ctx::globalGet),

    @OpContract("global -> _")
    @OpArgs(UInt::class)
    GLOBAL_SET("global.set", SIMPLE_WITH_INDEX, 0x24u, usage = Ctx::globalSet),

    // table instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#table-instructions
    @OpContract("i32 -> table[@1]")
    @OpArgs(UInt::class)
    TABLE_GET("table.get", SIMPLE_WITH_INDEX, 0x25u, usage = Ctx::tableGet),

    @OpContract("i32, table[@1] -> _")
    @OpArgs(UInt::class)
    TABLE_SET("table.set", SIMPLE_WITH_INDEX, 0x26u, usage = Ctx::tableSet),

    @OpContract("i32, i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    TABLE_INIT("table.init", SIMPLE_WITH_INDEX_2, 0xFCu, 12, usage = Ctx::tableInit),

    @OpContract("_ -> _")
    @OpArgs(UInt::class)
    ELEM_DROP("elem.drop", SIMPLE_WITH_INDEX, 0xFCu, 13, usage = Ctx::dropElem),

    @OpContract("i32, i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    TABLE_COPY("table.copy", SIMPLE_WITH_INDEX_2, 0xFCu, 14, usage = Ctx::tableCopy),

    @OpContract("table_elem, i32 -> _")
    @OpArgs(UInt::class)
    TABLE_GROW("table.grow", SIMPLE_WITH_INDEX, 0xFCu, 15, usage = Ctx::tableGrow),

    @OpContract("_ -> i32")
    @OpArgs(UInt::class)
    TABLE_SIZE("table.size", SIMPLE_WITH_INDEX, 0xFCu, 16, usage = Ctx::tableSize),

    @OpContract("i32, table_elem, i32 -> _")
    @OpArgs(UInt::class)
    TABLE_FILL("table.fill", SIMPLE_WITH_INDEX, 0xFCu, 17, usage = Ctx::tableFill),

    // memory instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#memory-instructions
    @Load("i32")
    I32_LOAD("i32.load", MEMARG, 0x28u, usage = load(I32, 4)),

    @Load("i64")
    I64_LOAD("i64.load", MEMARG, 0x29u, usage = load(I64, 8)),

    @Load("f32")
    F32_LOAD("f32.load", MEMARG, 0x2Au, usage = load(I32, 4)),

    @Load("f64")
    F64_LOAD("f64.load", MEMARG, 0x2Bu, usage = load(I64, 8)),

    @Load("i32")
    I32_LOAD8_S("i32.load8_s", MEMARG, 0x2Cu, usage = load(I32, 1)),

    @Load("i32")
    I32_LOAD8_U("i32.load8_u", MEMARG, 0x2Du, usage = load(I32, 1)),

    @Load("i32")
    I32_LOAD16_S("i32.load16_s", MEMARG, 0x2Eu, usage = load(I32, 2)),

    @Load("i32")
    I32_LOAD16_U("i32.load16_u", MEMARG, 0x2Fu, usage = load(I32, 2)),

    @Load("i64")
    I64_LOAD8_S("i64.load8_s", MEMARG, 0x30u, usage = load(I64, 1)),

    @Load("i64")
    I64_LOAD8_U("i64.load8_u", MEMARG, 0x31u, usage = load(I64, 1)),

    @Load("i64")
    I64_LOAD16_S("i64.load16_s", MEMARG, 0x32u, usage = load(I64, 2)),

    @Load("i64")
    I64_LOAD16_U("i64.load16_u", MEMARG, 0x33u, usage = load(I64, 2)),

    @Load("i64")
    I64_LOAD32_S("i64.load32_s", MEMARG, 0x34u, usage = load(I64, 4)),

    @Load("i64")
    I64_LOAD32_U("i64.load32_u", MEMARG, 0x35u, usage = load(I64, 4)),

    @Store("i32")
    I32_STORE("i32.store", MEMARG, 0x36u, usage = store(I32, 4)),

    @Store("i64")
    I64_STORE("i64.store", MEMARG, 0x37u, usage = store(I64, 8)),

    @Store("f32")
    F32_STORE("f32.store", MEMARG, 0x38u, usage = store(I32, 4)),

    @Store("f64")
    F64_STORE("f64.store", MEMARG, 0x39u, usage = store(I64, 8)),

    @Store("i32")
    I32_STORE8("i32.store8", MEMARG, 0x3Au, usage = store(I32, 1)),

    @Store("i32")
    I32_STORE16("i32.store16", MEMARG, 0x3Bu, usage = store(I32, 2)),

    @Store("i64")
    I64_STORE8("i64.store8", MEMARG, 0x3Cu, usage = store(I64, 1)),

    @Store("i64")
    I64_STORE16("i64.store16", MEMARG, 0x3Du, usage = store(I64, 2)),

    @Store("i64")
    I64_STORE32("i64.store32", MEMARG, 0x3Eu, usage = store(I64, 4)),

    @OpContract("_ -> i32")
    @OpArgs(UInt::class)
    MEMORY_SIZE("memory.size", SIMPLE_WITH_INDEX, 0x3Fu, usage = Ctx::memorySize),

    @OpContract("i32 -> i32")
    @OpArgs(UInt::class)
    MEMORY_GROW("memory.grow", SIMPLE_WITH_INDEX, 0x40u, usage = Ctx::memoryGrow),

    @OpContract("i32, i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    MEMORY_INIT("memory.init", SIMPLE_WITH_INDEX_2, 0xFCu, 8, usage = Ctx::memoryInit),

    @OpContract("_ -> _")
    @OpArgs(UInt::class)
    DATA_DROP("data.drop", SIMPLE_WITH_INDEX, 0xFCu, 9, usage = Ctx::dropData),

    @OpContract("i32, i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    MEMORY_COPY("memory.copy", SIMPLE_WITH_INDEX_2, 0xFCu, 10, usage = Ctx::memoryCopy),

    @OpContract("i32, i32, i32 -> _")
    @OpArgs(UInt::class)
    MEMORY_FILL("memory.fill", SIMPLE_WITH_INDEX, 0xFCu, 11, usage = Ctx::memoryFill),

    // reference instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#reference-instructions
    @OpContract("_ -> (ref null T)")
    @OpArgs(HeapType::class)
    REF_NULL("ref.null", HEAP_TYPE, 0xD0u, usage = Ctx::refNull),

    @OpContract("null -> true; !null -> false")
    REF_IS_NULL("ref.is_null", SIMPLE, 0xD1u, usage = Ctx::refIsNull),

    @OpContract("_ -> (ref func)")
    @OpArgs(UInt::class)
    REF_FUNC("ref.func", SIMPLE_WITH_INDEX, 0xD2u, usage = Ctx::refFunc),

    @BinaryBoolean("(ref null any)")
    REF_EQ("ref.eq", SIMPLE, 0xD3u, usage = Ctx::refEq),

    @OpContract("null -> trap; (ref null T) -> (ref T)")
    REF_AS_NON_NULL("ref.as_non_null", SIMPLE, 0xD4u, usage = Ctx::refAsNonNull),

    @OpContract("is (ref T) -> true; else -> false")
    @OpArgs(HeapType::class)
    REF_TEST_NON_NULL("ref.test", HEAP_TYPE, 0xFBu, 20, usage = Ctx::refTestNonNull),

    @OpContract("is (ref null T) -> true; else -> false")
    @OpArgs(HeapType::class)
    REF_TEST_NULL("ref.test", HEAP_TYPE, 0xFBu, 21, usage = Ctx::refTestNull),

    @OpContract("is (ref T) -> (ref T); else -> trap")
    @OpArgs(HeapType::class)
    REF_CAST_NON_NULL("ref.cast", HEAP_TYPE, 0xFBu, 22, usage = Ctx::refCastNonNull),

    @OpContract("is (ref T) -> (ref T); else -> trap")
    @OpArgs(HeapType::class)
    REF_CAST_NULL("ref.cast", HEAP_TYPE, 0xFBu, 23, usage = Ctx::refCastNull),

    // aggregate instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#aggregate-instructions
    @OpContract("fields -> (ref struct)")
    @OpArgs(UInt::class)
    STRUCT_NEW("struct.new", SIMPLE_WITH_INDEX, 0xFBu, 0, usage = Ctx::structNew),

    @OpContract("_ -> (ref struct)")
    @OpArgs(UInt::class)
    STRUCT_NEW_DEFAULT(
        "struct.new_default", SIMPLE_WITH_INDEX, 0xFBu, 1, usage = Ctx::structNewDefault
    ),

    @OpContract("(ref struct) -> field; null -> trap")
    @OpArgs(UInt::class, UInt::class)
    STRUCT_GET("struct.get", SIMPLE_WITH_INDEX_2, 0xFBu, 2, usage = structGet),

    @OpContract("(ref struct) -> field; null -> trap")
    @OpArgs(UInt::class, UInt::class)
    STRUCT_GET_S("struct.get_s", SIMPLE_WITH_INDEX_2, 0xFBu, 3, usage = structGet),

    @OpContract("(ref struct) -> field; null -> trap")
    @OpArgs(UInt::class, UInt::class)
    STRUCT_GET_U("struct.get_u", SIMPLE_WITH_INDEX_2, 0xFBu, 4, usage = structGet),

    @OpContract("(ref struct), field -> _; null, field -> trap")
    @OpArgs(UInt::class, UInt::class)
    STRUCT_SET("struct.set", SIMPLE_WITH_INDEX_2, 0xFBu, 5, usage = Ctx::structSet),

    @OpContract("array_value, i32 -> (ref T)")
    @OpArgs(UInt::class)
    ARRAY_NEW("array.new", SIMPLE_WITH_INDEX, 0xFBu, 6, usage = Ctx::arrayNew),

    @OpContract("i32 -> (ref T)")
    @OpArgs(UInt::class)
    ARRAY_NEW_DEFAULT(
        "array.new_default", SIMPLE_WITH_INDEX, 0xFBu, 7, usage = Ctx::arrayNewDefault
    ),

    @OpContract("array_value... -> (ref T)")
    @OpArgs(UInt::class, UInt::class)
    @OpRequiresSpecialHandling
    ARRAY_NEW_FIXED("array.new_fixed", SIMPLE_WITH_INDEX_2, 0xFBu, 8, usage = Ctx::arrayNewFixed),

    @OpContract("i32, i32 -> (ref T)")
    @OpArgs(UInt::class, UInt::class)
    ARRAY_NEW_DATA("array.new_data", SIMPLE_WITH_INDEX_2, 0xFBu, 9, usage = Ctx::arrayNewData),

    @OpContract("i32, i32 -> (ref T)")
    @OpArgs(UInt::class, UInt::class)
    ARRAY_NEW_ELEM("array.new_elem", SIMPLE_WITH_INDEX_2, 0xFBu, 10, usage = Ctx::arrayNewElem),

    @OpContract("(ref T), i32 -> array_value")
    @OpArgs(UInt::class)
    ARRAY_GET("array.get", SIMPLE_WITH_INDEX, 0xFBu, 11, usage = arrayGet),

    @OpContract("(ref T), i32 -> array_value")
    @OpArgs(UInt::class)
    ARRAY_GET_S("array.get_s", SIMPLE_WITH_INDEX, 0xFBu, 12, usage = arrayGet),

    @OpContract("(ref T), i32 -> array_value")
    @OpArgs(UInt::class)
    ARRAY_GET_U("array.get_u", SIMPLE_WITH_INDEX, 0xFBu, 13, usage = arrayGet),

    @OpContract("(ref T), i32, array_value -> _")
    @OpArgs(UInt::class)
    ARRAY_SET("array.set", SIMPLE_WITH_INDEX, 0xFBu, 14, usage = Ctx::arraySet),

    @OpContract("(ref T) -> i32")
    ARRAY_LEN("array.len", SIMPLE, 0xFBu, 15, usage = Ctx::arrayLen),

    @OpContract("(ref T), i32, array_value, i32 -> _")
    @OpArgs(UInt::class)
    ARRAY_FILL("array.fill", SIMPLE_WITH_INDEX, 0xFBu, 16, usage = Ctx::arrayFill),

    @OpContract("(ref T), i32, (ref T), i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    ARRAY_COPY("array.copy", SIMPLE_WITH_INDEX_2, 0xFBu, 17, usage = Ctx::arrayCopy),

    @OpContract("(ref T), i32, i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    ARRAY_INIT_DATA("array.init_data", SIMPLE_WITH_INDEX_2, 0xFBu, 18, usage = Ctx::arrayInitData),

    @OpContract("(ref T), i32, i32, i32 -> _")
    @OpArgs(UInt::class, UInt::class)
    ARRAY_INIT_ELEM("array.init_elem", SIMPLE_WITH_INDEX_2, 0xFBu, 19, usage = Ctx::arrayInitElem),

    @OpContract("is (ref extern) -> (ref extern); else -> trap")
    ANY_CONVERT_EXTERN("any.convert_extern", SIMPLE, 0xFBu, 26, usage = Ctx::convertExtern),

    @OpContract("is (ref extern) -> (ref any); else -> trap")
    EXTERN_CONVERT_ANY("extern.convert_any", SIMPLE, 0xFBu, 27, usage = Ctx::convertAny),

    @OpContract("i32 -> (ref i31)")
    REF_I31("ref.i31", SIMPLE, 0xFBu, 28, usage = Ctx::refI31),

    @OpContract("(ref i31) -> i32")
    I31_GET_S("i31.get_s", SIMPLE, 0xFBu, 29, usage = Ctx::i31GetS),

    @OpContract("(ref i31) -> i32")
    I31_GET_U("i31.get_u", SIMPLE, 0xFBu, 30, usage = Ctx::i31GetU),

    // numeric instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#numeric-instructions
    @OpContract("_ -> i32")
    @OpArgs(Int::class)
    I32_CONST("i32.const", CONST_I32, 0x41u, usage = Ctx::constI32),

    @OpContract("_ -> i64")
    @OpArgs(Long::class)
    I64_CONST("i64.const", CONST_I64, 0x42u, usage = Ctx::constI64),

    @OpContract("_ -> f32")
    @OpArgs(Float::class)
    F32_CONST("f32.const", CONST_F32, 0x43u, usage = Ctx::constF32),

    @OpContract("_ -> f64")
    @OpArgs(Double::class)
    F64_CONST("f64.const", CONST_F64, 0x44u, usage = Ctx::constF64),

    @UnaryBoolean("i32")
    I32_EQZ("i32.eqz", SIMPLE, 0x45u, usage = unaryBoolean),

    @BinaryBoolean("i32")
    I32_EQ("i32.eq", SIMPLE, 0x46u, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_NE("i32.ne", SIMPLE, 0x47u, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_LT_S("i32.lt_s", SIMPLE, 0x48u, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_LT_U("i32.lt_u", SIMPLE, 0x49u, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_GT_S("i32.gt_s", SIMPLE, 0x4Au, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_GT_U("i32.gt_u", SIMPLE, 0x4Bu, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_LE_S("i32.le_s", SIMPLE, 0x4Cu, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_LE_U("i32.le_u", SIMPLE, 0x4Du, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_GE_S("i32.ge_s", SIMPLE, 0x4Eu, usage = binaryBoolean),

    @BinaryBoolean("i32")
    I32_GE_U("i32.ge_u", SIMPLE, 0x4Fu, usage = binaryBoolean),

    @UnaryBoolean("i64")
    I64_EQZ("i64.eqz", SIMPLE, 0x50u, usage = unaryBoolean),

    @BinaryBoolean("i64")
    I64_EQ("i64.eq", SIMPLE, 0x51u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_NE("i64.ne", SIMPLE, 0x52u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_LT_S("i64.lt_s", SIMPLE, 0x53u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_LT_U("i64.lt_u", SIMPLE, 0x54u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_GT_S("i64.gt_s", SIMPLE, 0x55u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_GT_U("i64.gt_u", SIMPLE, 0x56u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_LE_S("i64.le_s", SIMPLE, 0x57u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_LE_U("i64.le_u", SIMPLE, 0x58u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_GE_S("i64.ge_s", SIMPLE, 0x59u, usage = binaryBoolean),

    @BinaryBoolean("i64")
    I64_GE_U("i64.ge_u", SIMPLE, 0x5Au, usage = binaryBoolean),

    @BinaryBoolean("f32")
    F32_EQ("f32.eq", SIMPLE, 0x5Bu, usage = binaryBoolean),

    @BinaryBoolean("f32")
    F32_NE("f32.ne", SIMPLE, 0x5Cu, usage = binaryBoolean),

    @BinaryBoolean("f32")
    F32_LT("f32.lt", SIMPLE, 0x5Du, usage = binaryBoolean),

    @BinaryBoolean("f32")
    F32_GT("f32.gt", SIMPLE, 0x5Eu, usage = binaryBoolean),

    @BinaryBoolean("f32")
    F32_LE("f32.le", SIMPLE, 0x5Fu, usage = binaryBoolean),

    @BinaryBoolean("f32")
    F32_GE("f32.ge", SIMPLE, 0x60u, usage = binaryBoolean),

    @BinaryBoolean("f64")
    F64_EQ("f64.eq", SIMPLE, 0x61u, usage = binaryBoolean),

    @BinaryBoolean("f64")
    F64_NE("f64.ne", SIMPLE, 0x62u, usage = binaryBoolean),

    @BinaryBoolean("f64")
    F64_LT("f64.lt", SIMPLE, 0x63u, usage = binaryBoolean),

    @BinaryBoolean("f64")
    F64_GT("f64.gt", SIMPLE, 0x64u, usage = binaryBoolean),

    @BinaryBoolean("f64")
    F64_LE("f64.le", SIMPLE, 0x65u, usage = binaryBoolean),

    @BinaryBoolean("f64")
    F64_GE("f64.ge", SIMPLE, 0x66u, usage = binaryBoolean),

    @Unary("i32")
    I32_CLZ("i32.clz", SIMPLE, 0x67u, usage = unary),

    @Unary("i32")
    I32_CTZ("i32.ctz", SIMPLE, 0x68u, usage = unary),

    @Unary("i32")
    I32_POPCNT("i32.popcnt", SIMPLE, 0x69u, usage = unary),

    @Binary("i32")
    I32_ADD("i32.add", SIMPLE, 0x6Au, usage = binary),

    @Binary("i32")
    I32_SUB("i32.sub", SIMPLE, 0x6Bu, usage = binary),

    @Binary("i32")
    I32_MUL("i32.mul", SIMPLE, 0x6Cu, usage = binary),

    @Binary("i32")
    I32_DIV_S("i32.div_s", SIMPLE, 0x6Du, usage = binary),

    @Binary("i32")
    I32_DIV_U("i32.div_u", SIMPLE, 0x6Eu, usage = binary),

    @Binary("i32")
    I32_REM_S("i32.rem_s", SIMPLE, 0x6Fu, usage = binary),

    @Binary("i32")
    I32_REM_U("i32.rem_u", SIMPLE, 0x70u, usage = binary),

    @Binary("i32")
    I32_AND("i32.and", SIMPLE, 0x71u, usage = binary),

    @Binary("i32")
    I32_OR("i32.or", SIMPLE, 0x72u, usage = binary),

    @Binary("i32")
    I32_XOR("i32.xor", SIMPLE, 0x73u, usage = binary),

    @Binary("i32")
    I32_SHL("i32.shl", SIMPLE, 0x74u, usage = binary),

    @Binary("i32")
    I32_SHR_S("i32.shr_s", SIMPLE, 0x75u, usage = binary),

    @Binary("i32")
    I32_SHR_U("i32.shr_u", SIMPLE, 0x76u, usage = binary),

    @Binary("i32")
    I32_ROTL("i32.rotl", SIMPLE, 0x77u, usage = binary),

    @Binary("i32")
    I32_ROTR("i32.rotr", SIMPLE, 0x78u, usage = binary),

    @Unary("i64")
    I64_CLZ("i64.clz", SIMPLE, 0x79u, usage = unary),

    @Unary("i64")
    I64_CTZ("i64.ctz", SIMPLE, 0x7Au, usage = unary),

    @Unary("i64")
    I64_POPCNT("i64.popcnt", SIMPLE, 0x7Bu, usage = unary),

    @Binary("i64")
    I64_ADD("i64.add", SIMPLE, 0x7Cu, usage = binary),

    @Binary("i64")
    I64_SUB("i64.sub", SIMPLE, 0x7Du, usage = binary),

    @Binary("i64")
    I64_MUL("i64.mul", SIMPLE, 0x7Eu, usage = binary),

    @Binary("i64")
    I64_DIV_S("i64.div_s", SIMPLE, 0x7Fu, usage = binary),

    @Binary("i64")
    I64_DIV_U("i64.div_u", SIMPLE, 0x80u, usage = binary),

    @Binary("i64")
    I64_REM_S("i64.rem_s", SIMPLE, 0x81u, usage = binary),

    @Binary("i64")
    I64_REM_U("i64.rem_u", SIMPLE, 0x82u, usage = binary),

    @Binary("i64")
    I64_AND("i64.and", SIMPLE, 0x83u, usage = binary),

    @Binary("i64")
    I64_OR("i64.or", SIMPLE, 0x84u, usage = binary),

    @Binary("i64")
    I64_XOR("i64.xor", SIMPLE, 0x85u, usage = binary),

    @Binary("i64")
    I64_SHL("i64.shl", SIMPLE, 0x86u, usage = binary),

    @Binary("i64")
    I64_SHR_S("i64.shr_s", SIMPLE, 0x87u, usage = binary),

    @Binary("i64")
    I64_SHR_U("i64.shr_u", SIMPLE, 0x88u, usage = binary),

    @Binary("i64")
    I64_ROTL("i64.rotl", SIMPLE, 0x89u, usage = binary),

    @Binary("i64")
    I64_ROTR("i64.rotr", SIMPLE, 0x8Au, usage = binary),

    @Unary("f32")
    F32_ABS("f32.abs", SIMPLE, 0x8Bu, usage = unary),

    @Unary("f32")
    F32_NEG("f32.neg", SIMPLE, 0x8Cu, usage = unary),

    @Unary("f32")
    F32_CEIL("f32.ceil", SIMPLE, 0x8Du, usage = unary),

    @Unary("f32")
    F32_FLOOR("f32.floor", SIMPLE, 0x8Eu, usage = unary),

    @Unary("f32")
    F32_TRUNC("f32.trunc", SIMPLE, 0x8Fu, usage = unary),

    @Unary("f32")
    F32_NEAREST("f32.nearest", SIMPLE, 0x90u, usage = unary),

    @Unary("f32")
    F32_SQRT("f32.sqrt", SIMPLE, 0x91u, usage = unary),

    @Binary("f32")
    F32_ADD("f32.add", SIMPLE, 0x92u, usage = binary),

    @Binary("f32")
    F32_SUB("f32.sub", SIMPLE, 0x93u, usage = binary),

    @Binary("f32")
    F32_MUL("f32.mul", SIMPLE, 0x94u, usage = binary),

    @Binary("f32")
    F32_DIV("f32.div", SIMPLE, 0x95u, usage = binary),

    @Binary("f32")
    F32_MIN("f32.min", SIMPLE, 0x96u, usage = binary),

    @Binary("f32")
    F32_MAX("f32.max", SIMPLE, 0x97u, usage = binary),

    @Binary("f32")
    F32_COPYSIGN("f32.copysign", SIMPLE, 0x98u, usage = binary),

    @Unary("f32")
    F64_ABS("f64.abs", SIMPLE, 0x99u, usage = unary),

    @Unary("f32")
    F64_NEG("f64.neg", SIMPLE, 0x9Au, usage = unary),

    @Unary("f32")
    F64_CEIL("f64.ceil", SIMPLE, 0x9Bu, usage = unary),

    @Unary("f32")
    F64_FLOOR("f64.floor", SIMPLE, 0x9Cu, usage = unary),

    @Unary("f32")
    F64_TRUNC("f64.trunc", SIMPLE, 0x9Du, usage = unary),

    @Unary("f32")
    F64_NEAREST("f64.nearest", SIMPLE, 0x9Eu, usage = unary),

    @Unary("f32")
    F64_SQRT("f64.sqrt", SIMPLE, 0x9Fu, usage = unary),

    @Binary("f32")
    F64_ADD("f64.add", SIMPLE, 0xA0u, usage = binary),

    @Binary("f32")
    F64_SUB("f64.sub", SIMPLE, 0xA1u, usage = binary),

    @Binary("f32")
    F64_MUL("f64.mul", SIMPLE, 0xA2u, usage = binary),

    @Binary("f32")
    F64_DIV("f64.div", SIMPLE, 0xA3u, usage = binary),

    @Binary("f32")
    F64_MIN("f64.min", SIMPLE, 0xA4u, usage = binary),

    @Binary("f32")
    F64_MAX("f64.max", SIMPLE, 0xA5u, usage = binary),

    @Binary("f32")
    F64_COPYSIGN("f64.copysign", SIMPLE, 0xA6u, usage = binary),

    @OpContract("i64 -> i32")
    I32_WRAP_I64("i32.wrap_i64", SIMPLE, 0xA7u, usage = cvt { I64 to I32 }),

    @OpContract("f32 -> i32")
    I32_TRUNC_S_F32("i32.trunc_s_f32", SIMPLE, 0xA8u, usage = cvt { F32 to I32 }),

    @OpContract("f32 -> i32")
    I32_TRUNC_U_F32("i32.trunc_u_f32", SIMPLE, 0xA9u, usage = cvt { F32 to I32 }),

    @OpContract("f64 -> i32")
    I32_TRUNC_S_F64("i32.trunc_s_f64", SIMPLE, 0xAAu, usage = cvt { F64 to I32 }),

    @OpContract("f64 -> i32")
    I32_TRUNC_U_F64("i32.trunc_u_f64", SIMPLE, 0xABu, usage = cvt { F64 to I32 }),

    @OpContract("i32 -> i64")
    I64_EXTEND_S_I32("i64.extend_s_i32", SIMPLE, 0xACu, usage = cvt { I32 to I64 }),

    @OpContract("i32 -> i64")
    I64_EXTEND_U_I32("i64.extend_u_i32", SIMPLE, 0xADu, usage = cvt { I32 to I64 }),

    @OpContract("f32 -> i64")
    I64_TRUNC_S_F32("i64.trunc_s_f32", SIMPLE, 0xAEu, usage = cvt { F32 to I64 }),

    @OpContract("f32 -> i64")
    I64_TRUNC_U_F32("i64.trunc_u_f32", SIMPLE, 0xAFu, usage = cvt { F32 to I64 }),

    @OpContract("f64 -> i64")
    I64_TRUNC_S_F64("i64.trunc_s_f64", SIMPLE, 0xB0u, usage = cvt { F64 to I64 }),

    @OpContract("f64 -> i64")
    I64_TRUNC_U_F64("i64.trunc_u_f64", SIMPLE, 0xB1u, usage = cvt { F64 to I64 }),

    @OpContract("i32 -> f32")
    F32_CONVERT_S_I32("f32.convert_s_i32", SIMPLE, 0xB2u, usage = cvt { I32 to F32 }),

    @OpContract("i32 -> f32")
    F32_CONVERT_U_I32("f32.convert_u_i32", SIMPLE, 0xB3u, usage = cvt { I32 to F32 }),

    @OpContract("i64 -> f32")
    F32_CONVERT_S_I64("f32.convert_s_i64", SIMPLE, 0xB4u, usage = cvt { I64 to F32 }),

    @OpContract("i64 -> f32")
    F32_CONVERT_U_I64("f32.convert_u_i64", SIMPLE, 0xB5u, usage = cvt { I64 to F32 }),

    @OpContract("f64 -> f32")
    F32_DEMOTE_F64("f32.demote_f64", SIMPLE, 0xB6u, usage = cvt { F64 to F32 }),

    @OpContract("i32 -> f64")
    F64_CONVERT_S_I32("f64.convert_s_i32", SIMPLE, 0xB7u, usage = cvt { I32 to F64 }),

    @OpContract("i32 -> f64")
    F64_CONVERT_U_I32("f64.convert_u_i32", SIMPLE, 0xB8u, usage = cvt { I32 to F64 }),

    @OpContract("i64 -> f64")
    F64_CONVERT_S_I64("f64.convert_s_i64", SIMPLE, 0xB9u, usage = cvt { I64 to F64 }),

    @OpContract("i64 -> f64")
    F64_CONVERT_U_I64("f64.convert_u_i64", SIMPLE, 0xBAu, usage = cvt { I64 to F64 }),

    @OpContract("f32 -> f64")
    F64_PROMOTE_F32("f64.promote_f32", SIMPLE, 0xBBu, usage = cvt { F32 to F64 }),

    @OpContract("f32 -> i32")
    I32_REINTERPRET_F32("i32.reinterpret_f32", SIMPLE, 0xBCu, usage = cvt { F32 to I32 }),

    @OpContract("f64 -> i64")
    I64_REINTERPRET_F64("i64.reinterpret_f64", SIMPLE, 0xBDu, usage = cvt { F64 to I64 }),

    @OpContract("i32 -> f32")
    F32_REINTERPRET_I32("f32.reinterpret_i32", SIMPLE, 0xBEu, usage = cvt { I32 to F32 }),

    @OpContract("i64 -> f64")
    F64_REINTERPRET_I64("f64.reinterpret_i64", SIMPLE, 0xBFu, usage = cvt { I64 to F64 }),

    @Unary("i32")
    I32_EXTEND8_S("i32.extend8_s", SIMPLE, 0xC0u, usage = unary),

    @Unary("i32")
    I32_EXTEND16_S("i32.extend16_s", SIMPLE, 0xC1u, usage = unary),

    @Unary("i64")
    I64_EXTEND8_S("i64.extend8_s", SIMPLE, 0xC2u, usage = unary),

    @Unary("i64")
    I64_EXTEND16_S("i64.extend16_s", SIMPLE, 0xC3u, usage = unary),

    @Unary("i64")
    I64_EXTEND32_S("i64.extend32_s", SIMPLE, 0xC4u, usage = unary),

    @OpContract("f32 -> i32")
    I32_TRUNC_SAT_S_F32("i32.trunc_sat_s_f32", SIMPLE, 0xFCu, 0, usage = cvt { F32 to I32 }),

    @OpContract("f32 -> i32")
    I32_TRUNC_SAT_U_F32("i32.trunc_sat_u_f32", SIMPLE, 0xFCu, 1, usage = cvt { F32 to I32 }),

    @OpContract("f64 -> i32")
    I32_TRUNC_SAT_S_F64("i32.trunc_sat_s_f64", SIMPLE, 0xFCu, 2, usage = cvt { F64 to I32 }),

    @OpContract("f64 -> i32")
    I32_TRUNC_SAT_U_F64("i32.trunc_sat_u_f64", SIMPLE, 0xFCu, 3, usage = cvt { F64 to I32 }),

    @OpContract("f32 -> i64")
    I64_TRUNC_SAT_S_F32("i64.trunc_sat_s_f32", SIMPLE, 0xFCu, 4, usage = cvt { F32 to I64 }),

    @OpContract("f32 -> i64")
    I64_TRUNC_SAT_U_F32("i64.trunc_sat_u_f32", SIMPLE, 0xFCu, 5, usage = cvt { F32 to I64 }),

    @OpContract("f64 -> i64")
    I64_TRUNC_SAT_S_F64("i64.trunc_sat_s_f64", SIMPLE, 0xFCu, 6, usage = cvt { F64 to I64 }),

    @OpContract("f64 -> i64")
    I64_TRUNC_SAT_U_F64("i64.trunc_sat_u_f64", SIMPLE, 0xFCu, 7, usage = cvt { F64 to I64 }),

    // vector instructions
    // https://webassembly.github.io/spec/core/binary/instructions.html#vector-instructions
    @Load("v128")
    V128_LOAD("v128.load", MEMARG, 0xFDu, 0, usage = load(V128, 16)),

    @Load("v128")
    V128_LOAD8x8_S("v128.load8x8_s", MEMARG, 0xFDu, 1, usage = load(V128, 8)),

    @Load("v128")
    V128_LOAD8x8_U("v128.load8x8_u", MEMARG, 0xFDu, 2, usage = load(V128, 8)),

    @Load("v128")
    V128_LOAD16x4_S("v128.load16x4_s", MEMARG, 0xFDu, 3, usage = load(V128, 8)),

    @Load("v128")
    V128_LOAD16x4_U("v128.load16x4_u", MEMARG, 0xFDu, 4, usage = load(V128, 8)),

    @Load("v128")
    V128_LOAD32x2_S("v128.load32x2_s", MEMARG, 0xFDu, 5, usage = load(V128, 8)),

    @Load("v128")
    V128_LOAD32x2_U("v128.load32x2_u", MEMARG, 0xFDu, 6, usage = load(V128, 8)),

    @Load("v128")
    V128_LOAD8_SPLAT("v128.load8_splat", MEMARG, 0xFDu, 7, usage = load(V128, 1)),

    @Load("v128")
    V128_LOAD16_SPLAT("v128.load16_splat", MEMARG, 0xFDu, 8, usage = load(V128, 2)),

    @Load("v128")
    V128_LOAD32_SPLAT("v128.load32_splat", MEMARG, 0xFDu, 9, usage = load(V128, 4)),

    @Load("v128")
    V128_LOAD64_SPLAT("v128.load64_splat", MEMARG, 0xFDu, 10, usage = load(V128, 8)),

    @Store("v128")
    V128_STORE("v128.store", MEMARG, 0xFDu, 11, usage = store(V128, 16)),

    @LoadLane("v128")
    V128_LOAD8_LANE("v128.load8_lane", MEMARG_LANE, 0xFDu, 84, usage = loadLane(V128, 1)),

    @LoadLane("v128")
    V128_LOAD16_LANE("v128.load16_lane", MEMARG_LANE, 0xFDu, 85, usage = loadLane(V128, 2)),

    @LoadLane("v128")
    V128_LOAD32_LANE("v128.load32_lane", MEMARG_LANE, 0xFDu, 86, usage = loadLane(V128, 4)),

    @LoadLane("v128")
    V128_LOAD64_LANE("v128.load64_lane", MEMARG_LANE, 0xFDu, 87, usage = loadLane(V128, 8)),

    @StoreLane("v128")
    V128_STORE8_LANE("v128.store8_lane", MEMARG_LANE, 0xFDu, 88, usage = storeLane(V128, 1)),

    @StoreLane("v128")
    V128_STORE16_LANE("v128.store16_lane", MEMARG_LANE, 0xFDu, 89, usage = storeLane(V128, 2)),

    @StoreLane("v128")
    V128_STORE32_LANE("v128.store32_lane", MEMARG_LANE, 0xFDu, 90, usage = storeLane(V128, 4)),

    @StoreLane("v128")
    V128_STORE64_LANE("v128.store64_lane", MEMARG_LANE, 0xFDu, 91, usage = storeLane(V128, 8)),

    @Load("v128")
    V128_LOAD32_ZERO("v128.load32_zero", MEMARG, 0xFDu, 92, usage = load(V128, 4)),

    @Load("v128")
    V128_LOAD64_ZERO("v128.load64_zero", MEMARG, 0xFDu, 93, usage = load(V128, 8)),

    @OpContract("_ -> v128")
    @OpArgs(VectorValue::class)
    V128_CONST("v128.const", CONST_V128, 0xFDu, 12, usage = Ctx::constV128),

    @OpContract("v128 -> v128")
    @OpArgs(VectorValue::class)
    V128_SHUFFLE("v128.shuffle", CONST_V128, 0xFDu, 13, usage = Ctx::v128Shuffle),

    @Binary("v128")
    V128_SWIZZLE("v128.swizzle", SIMPLE, 0xFDu, 14, usage = binary),

    @Binary("v128")
    V128_RELAXED_SWIZZLE("v128.relaxed_swizzle", SIMPLE, 0xFDu, 256, usage = binary),

    @ExtractLane("i32")
    I8x16_EXTRACT_LANE_S("i8x16.extract_lane_s", LANE_INDEX, 0xFDu, 21, usage = extractLane(I32)),

    @ExtractLane("i32")
    I8x16_EXTRACT_LANE_U("i8x16.extract_lane_u", LANE_INDEX, 0xFDu, 22, usage = extractLane(I32)),

    @ReplaceLane("i32")
    I8x16_REPLACE_LANE("i8x16.replace_lane", LANE_INDEX, 0xFDu, 23, usage = replaceLane(I32)),

    @ExtractLane("i32")
    I16x8_EXTRACT_LANE_S("i16x8.extract_lane_s", LANE_INDEX, 0xFDu, 24, usage = extractLane(I32)),

    @ExtractLane("i32")
    I16x8_EXTRACT_LANE_U("i16x8.extract_lane_u", LANE_INDEX, 0xFDu, 25, usage = extractLane(I32)),

    @ReplaceLane("i32")
    I16x8_REPLACE_LANE("i16x8.replace_lane", LANE_INDEX, 0xFDu, 26, usage = replaceLane(I32)),

    @ExtractLane("i32")
    I32x4_EXTRACT_LANE("i32x4.extract_lane", LANE_INDEX, 0xFDu, 27, usage = extractLane(I32)),

    @ReplaceLane("i32")
    I32x4_REPLACE_LANE("i32x4.replace_lane", LANE_INDEX, 0xFDu, 28, usage = replaceLane(I32)),

    @ExtractLane("i64")
    I64x2_EXTRACT_LANE("i64x2.extract_lane", LANE_INDEX, 0xFDu, 29, usage = extractLane(I64)),

    @ReplaceLane("i64")
    I64x2_REPLACE_LANE("i64x2.replace_lane", LANE_INDEX, 0xFDu, 30, usage = replaceLane(I64)),

    @ExtractLane("f32")
    F32x4_EXTRACT_LANE("f32x4.extract_lane", LANE_INDEX, 0xFDu, 31, usage = extractLane(F32)),

    @ReplaceLane("f32")
    F32x4_REPLACE_LANE("f32x4.replace_lane", LANE_INDEX, 0xFDu, 32, usage = replaceLane(F32)),

    @ExtractLane("f64")
    F64x2_EXTRACT_LANE("f64x2.extract_lane", LANE_INDEX, 0xFDu, 33, usage = extractLane(F64)),

    @ReplaceLane("i64")
    F64x2_REPLACE_LANE("f64x2.replace_lane", LANE_INDEX, 0xFDu, 34, usage = replaceLane(F64)),

    @OpContract("i32 -> v128")
    I8x16_SPLAT("i8x16.splat", SIMPLE, 0xFDu, 15, usage = cvt { I32 to V128 }),

    @OpContract("i32 -> v128")
    I16x8_SPLAT("i16x8.splat", SIMPLE, 0xFDu, 16, usage = cvt { I32 to V128 }),

    @OpContract("i32 -> v128")
    I32x4_SPLAT("i32x4.splat", SIMPLE, 0xFDu, 17, usage = cvt { I32 to V128 }),

    @OpContract("i64 -> v128")
    I64x2_SPLAT("i64x2.splat", SIMPLE, 0xFDu, 18, usage = cvt { I64 to V128 }),

    @OpContract("f32 -> v128")
    F32x4_SPLAT("f32x4.splat", SIMPLE, 0xFDu, 17, usage = cvt { F32 to V128 }),

    @OpContract("f64 -> v128")
    F64x2_SPLAT("f64x2.splat", SIMPLE, 0xFDu, 18, usage = cvt { F64 to V128 }),

    @BinaryBoolean("v128")
    I8x16_EQ("i8x16.eq", SIMPLE, 0xFDu, 35, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_NE("i8x16.ne", SIMPLE, 0xFDu, 36, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_LT_S("i8x16.lt_s", SIMPLE, 0xFDu, 37, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_LT_U("i8x16.lt_u", SIMPLE, 0xFDu, 38, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_GT_S("i8x16.gt_s", SIMPLE, 0xFDu, 39, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_GT_U("i8x16.gt_u", SIMPLE, 0xFDu, 40, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_LE_S("i8x16.le_s", SIMPLE, 0xFDu, 41, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_LE_U("i8x16.le_u", SIMPLE, 0xFDu, 42, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_GE_S("i8x16.ge_s", SIMPLE, 0xFDu, 43, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I8x16_GE_U("i8x16.ge_u", SIMPLE, 0xFDu, 44, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_EQ("i16x8.eq", SIMPLE, 0xFDu, 45, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_NE("i16x8.ne", SIMPLE, 0xFDu, 46, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_LT_S("i16x8.lt_s", SIMPLE, 0xFDu, 47, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_LT_U("i16x8.lt_u", SIMPLE, 0xFDu, 48, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_GT_S("i16x8.gt_s", SIMPLE, 0xFDu, 49, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_GT_U("i16x8.gt_u", SIMPLE, 0xFDu, 50, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_LE_S("i16x8.le_s", SIMPLE, 0xFDu, 51, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_LE_U("i16x8.le_u", SIMPLE, 0xFDu, 52, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_GE_S("i16x8.ge_s", SIMPLE, 0xFDu, 53, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I16x8_GE_U("i16x8.ge_u", SIMPLE, 0xFDu, 54, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_EQ("i32x4.eq", SIMPLE, 0xFDu, 55, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_NE("i32x4.ne", SIMPLE, 0xFDu, 56, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_LT_S("i32x4.lt_s", SIMPLE, 0xFDu, 57, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_LT_U("i32x4.lt_u", SIMPLE, 0xFDu, 58, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_GT_S("i32x4.gt_s", SIMPLE, 0xFDu, 59, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_GT_U("i32x4.gt_u", SIMPLE, 0xFDu, 60, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_LE_S("i32x4.le_s", SIMPLE, 0xFDu, 61, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_LE_U("i32x4.le_u", SIMPLE, 0xFDu, 62, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_GE_S("i32x4.ge_s", SIMPLE, 0xFDu, 63, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I32x4_GE_U("i32x4.ge_u", SIMPLE, 0xFDu, 64, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I64x2_EQ("i64x2.eq", SIMPLE, 0xFDu, 214, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I64x2_NE("i64x2.ne", SIMPLE, 0xFDu, 215, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I64x2_LT_S("i64x2.lt_s", SIMPLE, 0xFDu, 216, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I64x2_GT_S("i64x2.gt_s", SIMPLE, 0xFDu, 217, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I64x2_LE_S("i64x2.le_s", SIMPLE, 0xFDu, 218, usage = binaryBoolean),

    @BinaryBoolean("v128")
    I64x2_GE_S("i64x2.ge_s", SIMPLE, 0xFDu, 219, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F32x4_EQ("f32x4.eq", SIMPLE, 0xFDu, 65, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F32x4_NE("f32x4.ne", SIMPLE, 0xFDu, 66, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F32x4_LT("f32x4.lt", SIMPLE, 0xFDu, 67, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F32x4_GT("f32x4.gt", SIMPLE, 0xFDu, 68, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F32x4_LE("f32x4.le", SIMPLE, 0xFDu, 69, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F32x4_GE("f32x4.ge", SIMPLE, 0xFDu, 70, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F64x2_EQ("f64x2.eq", SIMPLE, 0xFDu, 71, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F64x2_NE("f64x2.ne", SIMPLE, 0xFDu, 72, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F64x2_LT("f64x2.lt", SIMPLE, 0xFDu, 73, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F64x2_GT("f64x2.gt", SIMPLE, 0xFDu, 74, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F64x2_LE("f64x2.le", SIMPLE, 0xFDu, 75, usage = binaryBoolean),

    @BinaryBoolean("v128")
    F64x2_GE("f64x2.ge", SIMPLE, 0xFDu, 76, usage = binaryBoolean),

    @Unary("v128")
    V128_NOT("v128.not", SIMPLE, 0xFDu, 77, usage = unary),

    @Binary("v128")
    V128_AND("v128.and", SIMPLE, 0xFDu, 78, usage = binary),

    @Binary("v128")
    V128_AND_NOT("v128.and_not", SIMPLE, 0xFDu, 79, usage = binary),

    @Binary("v128")
    V128_OR("v128.or", SIMPLE, 0xFDu, 80, usage = binary),

    @Binary("v128")
    V128_XOR("v128.xor", SIMPLE, 0xFDu, 81, usage = binary),

    @Ternery("v128")
    V128_BIT_SELECT("v128.bit_select", SIMPLE, 0xFDu, 82, usage = ternery),

    @UnaryBoolean("v128")
    V128_ANY_TRUE("v128.any_true", SIMPLE, 0xFDu, 83, usage = unaryBoolean),

    @Unary("v128")
    I8x16_ABS("i8x16.abs", SIMPLE, 0xFDu, 96, usage = unary),

    @Unary("v128")
    I8x16_NEG("i8x16.neg", SIMPLE, 0xFDu, 97, usage = unary),

    @Unary("v128")
    I8x16_POPCNT("i8x16.popcnt", SIMPLE, 0xFDu, 98, usage = unary),

    @UnaryBoolean("v128")
    I8x16_ALL_TRUE("i8x16.all_true", SIMPLE, 0xFDu, 99, usage = unaryBoolean),

    @OpContract("v128 -> i32")
    I8x16_BIT_MASK("i8x16.bit_mask", SIMPLE, 0xFDu, 100, usage = vectorBitmask),

    @Binary("v128")
    I8x16_NARROW_I16x8_S("i8x16.narrow_i16x8_s", SIMPLE, 0xFDu, 101, usage = binary),

    @Binary("v128")
    I8x16_NARROW_I16x8_U("i8x16.narrow_i16x8_u", SIMPLE, 0xFDu, 102, usage = binary),

    @OpContract("v128, i32 -> v128")
    I8x16_SHL("i8x16.shl", SIMPLE, 0xFDu, 107, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I8x16_SHR_S("i8x16.shr_s", SIMPLE, 0xFDu, 108, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I8x16_SHR_U("i8x16.shr_u", SIMPLE, 0xFDu, 109, usage = vectorShift),

    @Binary("v128")
    I8x16_ADD("i8x16.add", SIMPLE, 0xFDu, 110, usage = binary),

    @Binary("v128")
    I8x16_ADD_SAT_S("i8x16.add_sat_s", SIMPLE, 0xFDu, 111, usage = binary),

    @Binary("v128")
    I8x16_ADD_SAT_U("i8x16.add_sat_u", SIMPLE, 0xFDu, 112, usage = binary),

    @Binary("v128")
    I8x16_SUB("i8x16.sub", SIMPLE, 0xFDu, 113, usage = binary),

    @Binary("v128")
    I8x16_SUB_SAT_S("i8x16.sub_sat_s", SIMPLE, 0xFDu, 114, usage = binary),

    @Binary("v128")
    I8x16_SUB_SAT_U("i8x16.sub_sat_u", SIMPLE, 0xFDu, 115, usage = binary),

    @Binary("v128")
    I8x16_MIN_S("i8x16.min_s", SIMPLE, 0xFDu, 118, usage = binary),

    @Binary("v128")
    I8x16_MIN_U("i8x16.min_u", SIMPLE, 0xFDu, 119, usage = binary),

    @Binary("v128")
    I8x16_MAX_S("i8x16.max_s", SIMPLE, 0xFDu, 120, usage = binary),

    @Binary("v128")
    I8x16_MAX_U("i8x16.max_u", SIMPLE, 0xFDu, 121, usage = binary),

    @Binary("v128")
    I8x16_AVGR_U("i8x16.avgr_u", SIMPLE, 0xFDu, 123, usage = binary),

    @Unary("v128")
    I16x8_EXTADD_PAIRWISE_S_I8x16(
        "i16x8.extadd_pairwise_s_i8x16", SIMPLE, 0xFDu, 124, usage = unary
    ),

    @Unary("v128")
    I16x8_EXTADD_PAIRWISE_U_I8x16(
        "i16x8.extadd_pairwise_u_i8x16", SIMPLE, 0xFDu, 125, usage = unary
    ),

    @Unary("v128")
    I16x8_ABS("i16x8.abs", SIMPLE, 0xFDu, 128, usage = unary),

    @Unary("v128")
    I16x8_NEG("i16x8.neg", SIMPLE, 0xFDu, 129, usage = unary),

    @UnaryBoolean("v128")
    I16x8_ALL_TRUE("i16x8.all_true", SIMPLE, 0xFDu, 131, usage = unaryBoolean),

    @OpContract("v128 -> i32")
    I16x8_BIT_MASK("i16x8.bit_mask", SIMPLE, 0xFDu, 132, usage = vectorBitmask),

    @Binary("v128")
    I16x8_NARROW_I32x4_S("i16x8.narrow_i32x4_s", SIMPLE, 0xFDu, 133, usage = binary),

    @Binary("v128")
    I16x8_NARROW_I32x4_U("i16x8.narrow_i32x4_u", SIMPLE, 0xFDu, 134, usage = binary),

    @Unary("v128")
    I16x8_EXTEND_LOW_S_I8x16("i16x8.extend_low_s_i8x16", SIMPLE, 0xFDu, 135, usage = unary),

    @Unary("v128")
    I16x8_EXTEND_HIGH_S_I8x16("i16x8.extend_high_s_i8x16", SIMPLE, 0xFDu, 136, usage = unary),

    @Unary("v128")
    I16x8_EXTEND_LOW_U_I8x16("i16x8.extend_low_u_i8x16", SIMPLE, 0xFDu, 137, usage = unary),

    @Unary("v128")
    I16x8_EXTEND_HIGH_U_I8x16("i16x8.extend_high_u_i8x16", SIMPLE, 0xFDu, 138, usage = unary),

    @OpContract("v128, i32 -> v128")
    I16x8_SHL("i16x8.shl", SIMPLE, 0xFDu, 139, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I16x8_SHR_S("i16x8.shr_s", SIMPLE, 0xFDu, 140, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I16x8_SHR_U("i16x8.shr_u", SIMPLE, 0xFDu, 141, usage = vectorShift),

    @Binary("v128")
    I16x8_Q15MULR_SAT_S("i16x8.q15mulr_sat_s", SIMPLE, 0xFDu, 130, usage = binary),

    @Binary("v128")
    I16x8_ADD("i16x8.add", SIMPLE, 0xFDu, 142, usage = binary),

    @Binary("v128")
    I16x8_ADD_SAT_S("i16x8.add_sat_s", SIMPLE, 0xFDu, 143, usage = binary),

    @Binary("v128")
    I16x8_ADD_SAT_U("i16x8.add_sat_u", SIMPLE, 0xFDu, 144, usage = binary),

    @Binary("v128")
    I16x8_SUB("i16x8.sub", SIMPLE, 0xFDu, 145, usage = binary),

    @Binary("v128")
    I16x8_SUB_SAT_S("i16x8.sub_sat_s", SIMPLE, 0xFDu, 146, usage = binary),

    @Binary("v128")
    I16x8_SUB_SAT_U("i16x8.sub_sat_u", SIMPLE, 0xFDu, 147, usage = binary),

    @Binary("v128")
    I16x8_MUL("i16x8.mul", SIMPLE, 0xFDu, 149, usage = binary),

    @Binary("v128")
    I16x8_MIN_S("i16x8.min_s", SIMPLE, 0xFDu, 150, usage = binary),

    @Binary("v128")
    I16x8_MIN_U("i16x8.min_u", SIMPLE, 0xFDu, 151, usage = binary),

    @Binary("v128")
    I16x8_MAX_S("i16x8.max_s", SIMPLE, 0xFDu, 152, usage = binary),

    @Binary("v128")
    I16x8_MAX_U("i16x8.max_u", SIMPLE, 0xFDu, 153, usage = binary),

    @Binary("v128")
    I16x8_AVGR_U("i16x8.avgr_u", SIMPLE, 0xFDu, 155, usage = binary),

    @Binary("v128")
    I16x8_RELAXED_Q15MULR_S("i16x8.relaxed_q15mulr_s", SIMPLE, 0xFDu, 273, usage = binary),

    @Binary("v128")
    I16x8_EXTMUL_LOW_S_I8x16("i16x8.extmul_low_s_i8x16", SIMPLE, 0xFDu, 156, usage = binary),

    @Binary("v128")
    I16x8_EXTMUL_HIGH_S_I8x16("i16x8.extmul_high_s_i8x16", SIMPLE, 0xFDu, 157, usage = binary),

    @Binary("v128")
    I16x8_EXTMUL_LOW_U_I8x16("i16x8.extmul_low_u_i8x16", SIMPLE, 0xFDu, 158, usage = binary),

    @Binary("v128")
    I16x8_EXTMUL_HIGH_U_I8x16("i16x8.extmul_high_u_i8x16", SIMPLE, 0xFDu, 159, usage = binary),

    @Binary("v128")
    I16x8_RELAXED_DOT_S_I8x16("i16x8.relaxed_dot_s_i8x16", SIMPLE, 0xFDu, 274, usage = binary),

    @Unary("v128")
    I32x4_EXTADD_PAIRWISE_S_I16x8(
        "i32x4.extadd_pairwise_s_i16x8", SIMPLE, 0xFDu, 126, usage = unary
    ),

    @Unary("v128")
    I32x4_EXTADD_PAIRWISE_U_I16x8(
        "i32x4.extadd_pairwise_u_i16x8", SIMPLE, 0xFDu, 127, usage = unary
    ),

    @Unary("v128")
    I32x4_ABS("i32x4.abs", SIMPLE, 0xFDu, 160, usage = unary),

    @Unary("v128")
    I32x4_NEG("i32x4.neg", SIMPLE, 0xFDu, 161, usage = unary),

    @UnaryBoolean("v128")
    I32x4_ALL_TRUE("i32x4.all_true", SIMPLE, 0xFDu, 163, usage = unaryBoolean),

    @OpContract("v128 -> i32")
    I32x4_BIT_MASK("i32x4.bit_mask", SIMPLE, 0xFDu, 164, usage = vectorBitmask),

    @Unary("v128")
    I32x4_EXTEND_LOW_S_I16x8("i32x4.extend_low_s_i16x8", SIMPLE, 0xFDu, 167, usage = unary),

    @Unary("v128")
    I32x4_EXTEND_HIGH_S_I16x8("i32x4.extend_high_s_i16x8", SIMPLE, 0xFDu, 168, usage = unary),

    @Unary("v128")
    I32x4_EXTEND_LOW_U_I16x8("i32x4.extend_low_u_i16x8", SIMPLE, 0xFDu, 169, usage = unary),

    @Unary("v128")
    I32x4_EXTEND_HIGH_U_I16x8("i32x4.extend_high_u_i16x8", SIMPLE, 0xFDu, 170, usage = unary),

    @OpContract("v128, i32 -> v128")
    I32x4_SHL("i32x4.shl", SIMPLE, 0xFDu, 171, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I32x4_SHR_S("i32x4.shr_s", SIMPLE, 0xFDu, 172, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I32x4_SHR_U("i32x4.shr_u", SIMPLE, 0xFDu, 173, usage = vectorShift),

    @Binary("v128")
    I32x4_ADD("i32x4.add", SIMPLE, 0xFDu, 174, usage = binary),

    @Binary("v128")
    I32x4_SUB("i32x4.sub", SIMPLE, 0xFDu, 177, usage = binary),

    @Binary("v128")
    I32x4_MUL("i32x4.mul", SIMPLE, 0xFDu, 181, usage = binary),

    @Binary("v128")
    I32x4_MIN_S("i32x4.min_s", SIMPLE, 0xFDu, 182, usage = binary),

    @Binary("v128")
    I32x4_MIN_U("i32x4.min_u", SIMPLE, 0xFDu, 183, usage = binary),

    @Binary("v128")
    I32x4_MAX_S("i32x4.max_s", SIMPLE, 0xFDu, 184, usage = binary),

    @Binary("v128")
    I32x4_MAX_U("i32x4.max_u", SIMPLE, 0xFDu, 185, usage = binary),

    @Binary("v128")
    I32x4_DOT_S_I16x8("i32x4.dot_s_i16x8", SIMPLE, 0xFDu, 186, usage = binary),

    @Binary("v128")
    I32x4_EXTMUL_LOW_S_I16x8("i32x4.extmul_low_s_i16x8", SIMPLE, 0xFDu, 188, usage = binary),

    @Binary("v128")
    I32x4_EXTMUL_HIGH_S_I16x8("i32x4.extmul_high_s_i16x8", SIMPLE, 0xFDu, 189, usage = binary),

    @Binary("v128")
    I32x4_EXTMUL_LOW_U_I16x8("i32x4.extmul_low_u_i16x8", SIMPLE, 0xFDu, 190, usage = binary),

    @Binary("v128")
    I32x4_EXTMUL_HIGH_U_I16x8("i32x4.extmul_high_u_i16x8", SIMPLE, 0xFDu, 191, usage = binary),

    @Binary("v128")
    I32x4_RELAXED_DOT_ADD_S_I16x8(
        "i32x4.relaxed_dot_add_s_i16x8", SIMPLE, 0xFDu, 275, usage = binary
    ),

    @Unary("v128")
    I64x2_ABS("i64x2.abs", SIMPLE, 0xFDu, 192, usage = unary),

    @Unary("v128")
    I64x2_NEG("i64x2.neg", SIMPLE, 0xFDu, 193, usage = unary),

    @UnaryBoolean("v128")
    I64x2_ALL_TRUE("i64x2.all_true", SIMPLE, 0xFDu, 195, usage = unaryBoolean),

    @OpContract("v128 -> i32")
    I64x2_BIT_MASK("i64x2.bit_mask", SIMPLE, 0xFDu, 196, usage = vectorBitmask),

    @Unary("v128")
    I64x2_EXTEND_LOW_S_I32x4("i64x2.extend_low_s_i32x4", SIMPLE, 0xFDu, 199, usage = unary),

    @Unary("v128")
    I64x2_EXTEND_HIGH_S_I32x4("i64x2.extend_high_s_i32x4", SIMPLE, 0xFDu, 200, usage = unary),

    @Unary("v128")
    I64x2_EXTEND_LOW_U_I32x4("i64x2.extend_low_u_i32x4", SIMPLE, 0xFDu, 201, usage = unary),

    @Unary("v128")
    I64x2_EXTEND_HIGH_U_I32x4("i64x2.extend_high_u_i32x4", SIMPLE, 0xFDu, 202, usage = unary),

    @OpContract("v128, i32 -> v128")
    I64x2_SHL("i64x2.shl", SIMPLE, 0xFDu, 203, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I64x2_SHR_S("i64x2.shr_s", SIMPLE, 0xFDu, 204, usage = vectorShift),

    @OpContract("v128, i32 -> v128")
    I64x2_SHR_U("i64x2.shr_u", SIMPLE, 0xFDu, 205, usage = vectorShift),

    @Binary("v128")
    I64x2_ADD("i64x2.add", SIMPLE, 0xFDu, 206, usage = binary),

    @Binary("v128")
    I64x2_SUB("i64x2.sub", SIMPLE, 0xFDu, 209, usage = binary),

    @Binary("v128")
    I64x2_MUL("i64x2.mul", SIMPLE, 0xFDu, 213, usage = binary),

    @Binary("v128")
    I64x2_EXTMUL_LOW_S_I32x4("i64x2.extmul_low_s_i32x4", SIMPLE, 0xFDu, 220, usage = binary),

    @Binary("v128")
    I64x2_EXTMUL_HIGH_S_I32x4("i64x2.extmul_high_s_i32x4", SIMPLE, 0xFDu, 221, usage = binary),

    @Binary("v128")
    I64x2_EXTMUL_LOW_U_I32x4("i64x2.extmul_low_u_i32x4", SIMPLE, 0xFDu, 222, usage = binary),

    @Binary("v128")
    I64x2_EXTMUL_HIGH_U_I32x4("i64x2.extmul_high_u_i32x4", SIMPLE, 0xFDu, 223, usage = binary),

    @Unary("v128")
    F32x4_CEIL("f32x4.ceil", SIMPLE, 0xFDu, 103, usage = unary),

    @Unary("v128")
    F32x4_FLOOR("f32x4.floor", SIMPLE, 0xFDu, 104, usage = unary),

    @Unary("v128")
    F32x4_TRUNC("f32x4.trunc", SIMPLE, 0xFDu, 105, usage = unary),

    @Unary("v128")
    F32x4_NEAREST("f32x4.nearest", SIMPLE, 0xFDu, 106, usage = unary),

    @Unary("v128")
    F32x4_ABS("f32x4.abs", SIMPLE, 0xFDu, 224, usage = unary),

    @Unary("v128")
    F32x4_NEG("f32x4.neg", SIMPLE, 0xFDu, 225, usage = unary),

    @Unary("v128")
    F32x4_SQRT("f32x4.sqrt", SIMPLE, 0xFDu, 227, usage = unary),

    @Binary("v128")
    F32x4_ADD("f32x4.add", SIMPLE, 0xFDu, 228, usage = binary),

    @Binary("v128")
    F32x4_SUB("f32x4.sub", SIMPLE, 0xFDu, 229, usage = binary),

    @Binary("v128")
    F32x4_MUL("f32x4.mul", SIMPLE, 0xFDu, 230, usage = binary),

    @Binary("v128")
    F32x4_DIV("f32x4.div", SIMPLE, 0xFDu, 231, usage = binary),

    @Binary("v128")
    F32x4_MIN("f32x4.min", SIMPLE, 0xFDu, 232, usage = binary),

    @Binary("v128")
    F32x4_MAX("f32x4.max", SIMPLE, 0xFDu, 233, usage = binary),

    @Binary("v128")
    F32x4_PMIN("f32x4.pmin", SIMPLE, 0xFDu, 234, usage = binary),

    @Binary("v128")
    F32x4_PMAX("f32x4.pmax", SIMPLE, 0xFDu, 235, usage = binary),

    @Binary("v128")
    F32x4_RELAXED_MIN("f32x4.relaxed_min", SIMPLE, 0xFDu, 269, usage = binary),

    @Binary("v128")
    F32x4_RELAXED_MAX("f32x4.relaxed_max", SIMPLE, 0xFDu, 270, usage = binary),

    @Ternery("v128")
    F32x4_RELAXED_MADD("f32x4.relaxed_madd", SIMPLE, 0xFDu, 261, usage = ternery),

    @Ternery("v128")
    F32x4_RELAXED_NMADD("f32x4.relaxed_nmadd", SIMPLE, 0xFDu, 262, usage = ternery),

    @Unary("v128")
    F64x2_CEIL("f64x2.ceil", SIMPLE, 0xFDu, 116, usage = unary),

    @Unary("v128")
    F64x2_FLOOR("f64x2.floor", SIMPLE, 0xFDu, 117, usage = unary),

    @Unary("v128")
    F64x2_TRUNC("f64x2.trunc", SIMPLE, 0xFDu, 122, usage = unary),

    @Unary("v128")
    F64x2_NEAREST("f64x2.nearest", SIMPLE, 0xFDu, 148, usage = unary),

    @Unary("v128")
    F64x2_ABS("f64x2.abs", SIMPLE, 0xFDu, 236, usage = unary),

    @Unary("v128")
    F64x2_NEG("f64x2.neg", SIMPLE, 0xFDu, 237, usage = unary),

    @Unary("v128")
    F64x2_SQRT("f64x2.sqrt", SIMPLE, 0xFDu, 239, usage = unary),

    @Binary("v128")
    F64x2_ADD("f64x2.add", SIMPLE, 0xFDu, 240, usage = binary),

    @Binary("v128")
    F64x2_SUB("f64x2.sub", SIMPLE, 0xFDu, 241, usage = binary),

    @Binary("v128")
    F64x2_MUL("f64x2.mul", SIMPLE, 0xFDu, 242, usage = binary),

    @Binary("v128")
    F64x2_DIV("f64x2.div", SIMPLE, 0xFDu, 243, usage = binary),

    @Binary("v128")
    F64x2_MIN("f64x2.min", SIMPLE, 0xFDu, 244, usage = binary),

    @Binary("v128")
    F64x2_MAX("f64x2.max", SIMPLE, 0xFDu, 245, usage = binary),

    @Binary("v128")
    F64x2_PMIN("f64x2.pmin", SIMPLE, 0xFDu, 246, usage = binary),

    @Binary("v128")
    F64x2_PMAX("f64x2.pmax", SIMPLE, 0xFDu, 247, usage = binary),

    @Binary("v128")
    F64x2_RELAXED_MIN("f64x2.relaxed_min", SIMPLE, 0xFDu, 271, usage = binary),

    @Binary("v128")
    F64x2_RELAXED_MAX("f64x2.relaxed_max", SIMPLE, 0xFDu, 272, usage = binary),

    @Ternery("v128")
    F64x2_RELAXED_MADD("f64x2.relaxed_madd", SIMPLE, 0xFDu, 263, usage = ternery),

    @Ternery("v128")
    F64x2_RELAXED_NMADD("f64x2.relaxed_nmadd", SIMPLE, 0xFDu, 264, usage = ternery),

    @Ternery("v128")
    I8x16_RELAXED_LANE_SELECT("i8x16.relaxed_lane_select", SIMPLE, 0xFDu, 265, usage = ternery),

    @Ternery("v128")
    I16x8_RELAXED_LANE_SELECT("i16x8.relaxed_lane_select", SIMPLE, 0xFDu, 266, usage = ternery),

    @Ternery("v128")
    I32x4_RELAXED_LANE_SELECT("i32x4.relaxed_lane_select", SIMPLE, 0xFDu, 267, usage = ternery),

    @Ternery("v128")
    I64x2_RELAXED_LANE_SELECT("i64x2.relaxed_lane_select", SIMPLE, 0xFDu, 268, usage = ternery),

    @Unary("v128")
    F32x4_DEMOTE_ZERO_F64x2("f32x4.demote_zero_f64x2", SIMPLE, 0xFDu, 94, usage = unary),

    @Unary("v128")
    F64x2_PROMOTE_LOW_F32x4("f64x2.promote_low_f32x4", SIMPLE, 0xFDu, 95, usage = unary),

    @Unary("v128")
    I32x4_TRUNC_SAT_S_F32x4("i32x4.trunc_sat_s_f32x4", SIMPLE, 0xFDu, 248, usage = unary),

    @Unary("v128")
    I32x4_TRUNC_SAT_U_F32x4("i32x4.trunc_sat_u_f32x4", SIMPLE, 0xFDu, 249, usage = unary),

    @Unary("v128")
    F32x4_CONVERT_S_I32x4("f32x4.convert_s_i32x4", SIMPLE, 0xFDu, 250, usage = unary),

    @Unary("v128")
    F32x4_CONVERT_U_I32x4("f32x4.convert_u_i32x4", SIMPLE, 0xFDu, 251, usage = unary),

    @Unary("v128")
    I32x4_TRUNC_SAT_S_ZERO_F64x2("i32x4.trunc_sat_s_zero_f64x2", SIMPLE, 0xFDu, 252, usage = unary),

    @Unary("v128")
    I32x4_TRUNC_SAT_U_ZERO_F64x2("i32x4.trunc_sat_u_zero_f64x2", SIMPLE, 0xFDu, 253, usage = unary),

    @Unary("v128")
    F64x2_CONVERT_LOW_S_I32x4("f64x2.convert_low_s_i32x4", SIMPLE, 0xFDu, 254, usage = unary),

    @Unary("v128")
    F64x2_CONVERT_LOW_U_I32x4("f64x2.convert_low_u_i32x4", SIMPLE, 0xFDu, 255, usage = unary),

    @Unary("v128")
    I32x4_RELAXED_TRUNC_S_F32x4("i32x4.relaxed_trunc_s_f32x4", SIMPLE, 0xFDu, 257, usage = unary),

    @Unary("v128")
    I32x4_RELAXED_TRUNC_U_F32x4("i32x4.relaxed_trunc_u_f32x4", SIMPLE, 0xFDu, 258, usage = unary),

    @Unary("v128")
    I32x4_RELAXED_TRUNC_S_ZERO_F64x2(
        "i32x4.relaxed_trunc_s_zero_f64x2", SIMPLE, 0xFDu, 259, usage = unary
    ),

    @Unary("v128")
    I32x4_RELAXED_TRUNC_U_ZERO_F64x2(
        "i32x4.relaxed_trunc_u_zero_f64x2", SIMPLE, 0xFDu, 260, usage = unary
    ),

    ;

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("_ -> #1")
    @OpArgs(MemArg::class)
    annotation class Load(val from: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1 -> _")
    @OpArgs(MemArg::class)
    annotation class Store(val from: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("_ -> #1")
    @OpArgs(MemArg::class, Byte::class)
    annotation class LoadLane(val from: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1 -> _")
    @OpArgs(MemArg::class, Byte::class)
    annotation class StoreLane(val from: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("v128 -> #1")
    @OpArgs(Byte::class)
    annotation class ExtractLane(val type: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("v128, #1 -> v128")
    @OpArgs(Byte::class)
    annotation class ReplaceLane(val type: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1 -> #1")
    annotation class Unary(val from: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1 -> i32")
    annotation class UnaryBoolean(val type: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1, #1 -> #1")
    annotation class Binary(val type: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1, #1 -> i32")
    annotation class BinaryBoolean(val type: String)

    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.PROPERTY)
    @MustBeDocumented
    @OpContract("#1, #1, #1 -> #1")
    annotation class Ternery(val type: String)

    companion object {
        val extRange = 0xF0u..0xFFu
        val stdOps = entries.filter { it.ext == -1 }.associateBy { it.opcode }
        val extOps = entries.filter { it.ext != -1 }
            .groupBy { it.opcode }
            .mapValues { (_, values) -> values.associateBy { it.ext.toUInt() } }
    }
}
