package net.derfruhling.serene.wasm.printer

import net.derfruhling.serene.wasm.module.CompositeType
import net.derfruhling.serene.wasm.module.Type

interface NameResolver {
    fun resolveNameOptional(ns: Namespace, index: UInt): String? = null

    fun resolveName(ns: Namespace, index: UInt): String {
        return resolveNameOptional(ns, index)?.toIdentWord()
            ?: "$index (; ${ns.name.lowercase()} ;)"
    }

    fun resolveNameInfer(ns: Namespace, index: UInt): String {
        return resolveNameOptional(ns, index)?.toIdentWord()
            ?: index.toString()
    }

    fun resolveNameDecl(ns: Namespace, index: UInt): String {
        return resolveNameOptional(ns, index)?.toIdentWord()
            ?: "(; $index ;)"
    }

    fun resolveNameComment(ns: Namespace, index: UInt): String {
        return resolveNameOptional(ns, index)?.let { "(; $it ;)" }
            ?: "(; $index ;)"
    }

    var currentType: UInt
    var currentMemory: UInt
    var currentData: UInt
    var currentTable: UInt
    var currentElement: UInt
    var currentFunction: UInt
    var currentGlobal: UInt
    var currentTag: UInt

    fun defineFunction(func: UInt)
    val currentFuncType: UInt

    fun resolveFieldName(type: UInt, field: Int): String? = null
    fun resolveParamName(func: UInt, param: Int): String? = null
    fun resolveLocalName(func: UInt, local: Int): String? = null
    fun resolveType(type: UInt): Type
    fun defineType(type: Type)
    fun resolveTypeName(type: UInt): String? = null

    class Default : NameResolver {
        override var currentType: UInt = 0u
        override var currentMemory: UInt = 0u
        override var currentData: UInt = 0u
        override var currentTable: UInt = 0u
        override var currentElement: UInt = 0u
        override var currentFunction: UInt = 0u
        override var currentGlobal: UInt = 0u
        override var currentTag: UInt = 0u

        private val functions = ArrayList<UInt>()
        private val types = ArrayList<Type>()

        override val currentFuncType: UInt
            get() = functions[currentFunction.toInt()]

        override fun defineFunction(func: UInt) {
            val index = currentFunction.toInt()
            if(index != functions.size) {
                error("Function definition missed")
            }

            functions.add(func)
        }

        override fun resolveType(type: UInt): Type {
            return types[type.toInt()]
        }

        override fun defineType(type: Type) {
            val index = currentType.toInt()
            if(index != types.size) {
                error("Type definition missed")
            }
            types.add(type)
        }
    }
}
