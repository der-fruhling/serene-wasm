package net.derfruhling.serene.wasm.tests

import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual object Env {
    actual val dir: Path by lazy {
        SystemFileSystem.resolve(Path(System.getenv("TEST_CASE_PATH")))
    }
}
