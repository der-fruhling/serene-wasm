package net.derfruhling.serene.wasm.printer

class FunctionCodeFormatter(out: Printer) : AbstractExpressionFormatter(out) {
    init {
        out.beginInlineExprBlock()
    }

    override fun Printer.appendLineCond(string: String) {
        appendLineIndented(string)
    }

    override fun Printer.appendLineCond() {
        appendLine()
    }

    override fun visitEnd() {
        out.endInlineExprBlock()
    }
}
