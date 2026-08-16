package net.derfruhling.serene.wasm.tests

import kotlinx.io.buffered
import net.derfruhling.serene.wasm.WasmModule
import kotlin.test.Test

class ModuleTest {
    @Test
    fun `parse simple`() {
        Env.open("test.wasm").buffered().use {
            WasmModule.parse(it)
        }
    }
}
