package net.derfruhling.serene.wasm

fun interface DeferredDecode<out T> {
    fun finishDecoding(reader: WasmReader): T
}

inline fun <T, R> DeferredDecode<T>.map(crossinline fn: (T) -> R): DeferredDecode<R> {
    return DeferredDecode { fn(this.finishDecoding(it)) }
}

