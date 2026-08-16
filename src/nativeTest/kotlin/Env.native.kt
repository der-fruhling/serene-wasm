package net.derfruhling.serene.wasm.tests

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.posix.getenv

actual object Env {
    @OptIn(ExperimentalForeignApi::class)
    actual val dir: Path by lazy {
        SystemFileSystem.resolve(Path(getenv("TEST_CASE_PATH")!!.toKString()))
    }
}