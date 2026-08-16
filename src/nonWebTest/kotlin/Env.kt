package net.derfruhling.serene.wasm.tests

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

expect object Env {
    val dir: Path
}

fun Env.open(file: String) = SystemFileSystem.source(Path(Env.dir, *file.split('/').toTypedArray()))
