package net.derfruhling.serene.wasm.printer

interface Printable {
    fun Printer.print()
}

fun Printable.print(printer: Printer) = with(printer) { print() }
