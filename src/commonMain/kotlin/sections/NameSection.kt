package net.derfruhling.serene.wasm.sections

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.io.Buffer
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import kotlin.jvm.JvmInline

open class NameSection @PublishedApi internal constructor(builder: Builder) : CustomSection("name") {
    open class Builder {
        internal var moduleName: String? = null
        internal val functionNames = mutableMapOf<UInt, String>()
        internal val localNames = mutableMapOf<UInt, Map<UInt, String>>()
        internal val typeNames = mutableMapOf<UInt, String>()
        internal val fieldNames = mutableMapOf<UInt, Map<UInt, String>>()
        internal val tagNames = mutableMapOf<UInt, String>()

        fun module(name: String) {
            moduleName = name
        }

        fun function(index: UInt, name: String) {
            functionNames[index] = name
        }

        @JvmInline
        value class LocalBuilder internal constructor(private val map: MutableMap<UInt, String>) {
            fun param(index: UInt, name: String) {
                map[index] = name
            }

            internal fun toMap() = map.toMap()
        }

        fun functionLocals(index: UInt, fn: LocalBuilder.() -> Unit) {
            localNames[index] = LocalBuilder(mutableMapOf()).apply(fn).toMap()
        }

        fun type(index: UInt, name: String) {
            typeNames[index] = name
        }

        @JvmInline
        value class FieldBuilder internal constructor(private val map: MutableMap<UInt, String>) {
            fun field(index: UInt, name: String) {
                map[index] = name
            }

            internal fun toMap() = map.toMap()
        }

        fun fields(index: UInt, fn: FieldBuilder.() -> Unit) {
            fieldNames[index] = FieldBuilder(mutableMapOf()).apply(fn).toMap()
        }

        fun tag(index: UInt, name: String) {
            tagNames[index] = name
        }
    }

    val moduleName = builder.moduleName
    val functionNames = builder.functionNames.toMap()
    val localNames = builder.localNames.toMap()
    val typeNames = builder.typeNames.toMap()
    val fieldNames = builder.fieldNames.toMap()
    val tagNames = builder.tagNames.toMap()

    override fun encodeCustom(out: WasmWriter) {
        fun WasmWriter.sub(id: Byte, fn: (WasmWriter) -> Unit) {
            writeByte(id)
            val buffer = Buffer()
            val writer = WasmWriter(buffer)
            fn(writer)
            writeUInt(buffer.size.toUInt())
            writeBytes(buffer)
        }

        fun WasmWriter.nameMap(map: Map<UInt, String>) {
            writeList(map.entries) { (index, name) ->
                writeUInt(index)
                writeString(name)
            }
        }

        fun WasmWriter.indirectNameMap(map: Map<UInt, Map<UInt, String>>) {
            writeList(map.entries) { (index, nameMap) ->
                writeUInt(index)
                nameMap(nameMap)
            }
        }

        if (moduleName != null) out.sub(0) { it.writeString(moduleName) }
        if (functionNames.isNotEmpty()) out.sub(1) { it.nameMap(functionNames) }
        if (localNames.isNotEmpty()) out.sub(2) { it.indirectNameMap(localNames) }
        if (typeNames.isNotEmpty()) out.sub(4) { it.nameMap(typeNames) }
        if (fieldNames.isNotEmpty()) out.sub(10) { it.indirectNameMap(fieldNames) }
        if (tagNames.isNotEmpty()) out.sub(11) { it.nameMap(tagNames) }
    }

    companion object : Factory<NameSection> {
        val logger = KotlinLogging.logger {}

        override fun parseFrom(reader: WasmReader): NameSection = build {
            while (!reader.isExhausted) {
                val byte = reader.readByte().toInt()
                val size = reader.readUInt()
                val keeper = reader.keeper.inherit()
                val buffer = WasmReader(reader.readBytes(size), keeper)

                fun WasmReader.readNameMap(fn: (UInt, String) -> Unit) {
                    readList {
                        val index = it.readUInt()
                        val name = it.readString()
                        fn(index, name)
                    }
                }

                fun <F> WasmReader.readIndirectNameMap(
                    fn: (UInt, F.() -> Unit) -> Unit,
                    each: F.(WasmReader) -> Unit
                ) {
                    readList {
                        val index = it.readUInt()
                        fn(index) { each(it) }
                    }
                }

                when (byte) {
                    0 -> module(buffer.readString())
                    1 -> buffer.readNameMap(::function)
                    2 -> buffer.readIndirectNameMap(::functionLocals) { it.readNameMap(::param) }
                    4 -> buffer.readNameMap(::type)
                    10 -> buffer.readIndirectNameMap(::fields) { it.readNameMap(::field) }
                    11 -> buffer.readNameMap(::tag)
                    else -> logger.warn { "Ignoring unknown subsection $byte" }
                }
            }
        }

        inline fun build(fn: Builder.() -> Unit): NameSection {
            return NameSection(Builder().apply(fn))
        }
    }
}