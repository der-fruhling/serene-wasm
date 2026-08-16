package net.derfruhling.serene.wasm

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import net.derfruhling.serene.wasm.instruction.InstructionVisitor
import net.derfruhling.serene.wasm.instruction.Op
import net.derfruhling.serene.wasm.instruction.UnknownInstructionException
import net.derfruhling.serene.wasm.instruction.WasmWriterInstructionVisitor
import net.derfruhling.serene.wasm.module.InvalidModuleDataException
import kotlin.experimental.and

class WasmReader(private val source: Source, internal val keeper: PositionKeeper = PositionKeeper()) {
    constructor(rawSource: RawSource) : this(rawSource.buffered())
    constructor(bytes: ByteArray) : this(Buffer().also { it.write(bytes) })

    var position by keeper
    val isExhausted
        get() = source.exhausted()

    fun advanceTo(other: WasmReader) {
        require(other.position >= this.position)
        source.skip(other.position - this.position)
        this.position = other.position
    }

    fun peek() = WasmReader(source.peek(), keeper.inherit())

    @Throws(EOFException::class)
    fun readByte(): Byte {
        position++
        return source.readByte()
    }

    fun consume(): ByteString {
        return source.readByteString().also { position += it.size }
    }

    @Throws(EOFException::class)
    fun readBytes(length: Long): Buffer {
        val buffer = Buffer()
        source.readTo(buffer, length)
        position += length
        return buffer
    }

    @Throws(EOFException::class)
    fun readBytes(length: Int) = readBytes(length.toLong())

    @Throws(EOFException::class)
    fun readBytes(length: UInt) = readBytes(length.toLong())

    @Throws(EOFException::class)
    inline fun readUntil(condition: (Byte) -> Boolean): Buffer {
        val buffer = Buffer()

        do {
            val byte = readByte()
            buffer.writeByte(byte)
        } while(!condition(byte))

        return buffer
    }

    @PublishedApi
    @Throws(EOFException::class)
    internal inline fun <T> commonGenericIntRead(initial: T, accum: (T, Byte, Int) -> T): Triple<T, Int, Byte> {
        var value = initial
        val bytes = readUntil { !it.hasBit(7) }

        var offset = 0
        var byte: Byte
        do {
            byte = bytes.readByte()
            value = accum(value, byte and 0x7F, offset)
            offset += 7
        } while (!bytes.exhausted())
        return Triple(value, offset, byte)
    }

    @Throws(EOFException::class)
    inline fun <T> readGenericInt(initial: T, accum: (T, Byte, Int) -> T): T {
        val (value) = commonGenericIntRead(initial, accum)
        return value
    }

    @Throws(EOFException::class)
    inline fun <T> readGenericSignedInt(initial: T, signExtend: (T, Int) -> T, accum: (T, Byte, Int) -> T): T {
        val (value, offset, byte) = commonGenericIntRead(initial, accum)
        return if (byte.hasBit(6)) {
            signExtend(value, offset)
        } else {
            value
        }
    }

    @Throws(EOFException::class)
    fun readUInt() = readGenericInt(0u) { acc, byte, off ->
        if(off > 32) throw InvalidModuleDataException("ULEB128 integer too long")
        acc or (byte.toUInt() shl off)
    }

    @Throws(EOFException::class)
    fun readULong() = readGenericInt(0uL) { acc, byte, off ->
        if(off > 64) throw InvalidModuleDataException("ULEB128 integer too long")
        acc or (byte.toULong() shl off)
    }

    @Throws(EOFException::class)
    fun readInt() = readGenericSignedInt(0, { v, off ->
        // sign extension
        if(off < 32) v or (0.inv() shl off) else v
    }) { acc, byte, off ->
        if(off > 32) throw InvalidModuleDataException("SLEB128 integer too long")
        acc or (byte.toInt() shl off)
    }

    @Throws(EOFException::class)
    fun readLong() = readGenericSignedInt(0L, { v, off ->
        // sign extension
        if(off < 64) v or (0L.inv() shl off) else v
    }) { acc, byte, off ->
        if(off > 64) throw InvalidModuleDataException("SLEB128 integer too long")
        acc or (byte.toLong() shl off)
    }

    @Throws(EOFException::class)
    fun readStaticUInt(): UInt {
        position += 4
        return source.readUIntLe()
    }

    @Throws(EOFException::class)
    fun readFloat(): Float {
        position += 4
        return source.readFloatLe()
    }

    @Throws(EOFException::class)
    fun readDouble(): Double {
        position += 8
        return source.readDoubleLe()
    }

    @Throws(EOFException::class)
    fun readString(): String {
        val byteCount = readUInt()
        val bytes = readBytes(byteCount)

        return bytes.readString()
    }

    @Throws(EOFException::class)
    fun <T> readList(fn: (WasmReader) -> T): List<T> {
        return List(readUInt().toInt()) { fn(this) }
    }

    @Throws(EOFException::class, UnknownInstructionException::class)
    fun readExpr(): CodeBlob {
        val buffer = Buffer()
        val writerVisitor = WasmWriterInstructionVisitor(WasmWriter(buffer))
        visitExpr(writerVisitor)
        return CodeBlob(buffer.readByteString())
    }

    @Throws(EOFException::class, UnknownInstructionException::class)
    fun visitExpr(writerVisitor: InstructionVisitor) {
        try {
            var currentVisitor: InstructionVisitor = writerVisitor
            val blockStack = arrayListOf<InstructionVisitor>()

            while (true) {
                val op = readOp()
                val newVisitor = op.type.visit(this, op, blockStack.lastOrNull(), currentVisitor)
                    ?: break

                if (newVisitor !== currentVisitor) {
                    when (op) {
                        Op.END -> blockStack.removeLast()
                        Op.ELSE -> {}
                        else -> {
                            if (op.type.shouldPushCurrentBlock(op))
                                blockStack.add(currentVisitor)
                        }
                    }

                    currentVisitor = newVisitor
                }
            }
        } catch (e: EOFException) {
            throw InvalidModuleDataException("Expression did not end correctly", e)
        }
    }

    @Throws(EOFException::class, UnknownInstructionException::class)
    fun readOp(): Op {
        val opByte = readByte().toUByte()
        return if(opByte in Op.extRange) {
            val extOps = Op.extOps[opByte]
                ?: throw UnknownInstructionException("Extension instruction 0x${opByte.toString(16)} is not known")
            val ext = readUInt()
            extOps[ext]
                ?: throw UnknownInstructionException("Instruction $ext in extension block ${opByte.toString(16)} is not known")
        } else {
            Op.stdOps[opByte]
                ?: throw UnknownInstructionException("Instruction ${opByte.toString(16)} is not known")
        }
    }

    @Throws(EOFException::class)
    fun readMagicUInt(): UInt = source.readUInt()
}
