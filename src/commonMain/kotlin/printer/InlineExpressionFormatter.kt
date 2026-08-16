package net.derfruhling.serene.wasm.printer

class InlineExpressionFormatter(out: Printer) : AbstractExpressionFormatter(out) {
    init {
        out.beginInlineExprBlock()
    }

    override fun Printer.appendLineCond(string: String) {
        word(string)
    }

    override fun visitEnd() {
        out.endInlineExprBlock()
    }
}
