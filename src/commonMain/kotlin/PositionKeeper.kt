package net.derfruhling.serene.wasm

import kotlin.reflect.KProperty

class PositionKeeper private constructor(private var position: Long) {
    constructor() : this(0)

    fun inherit() = PositionKeeper(position)

    operator fun getValue(self: Any?, property: KProperty<*>): Long {
        return position
    }

    operator fun setValue(self: Any?, property: KProperty<*>, value: Long) {
        require(value >= position)
        position = value
    }
}
