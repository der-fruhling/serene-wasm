package net.derfruhling.serene.wasm.module

import net.derfruhling.serene.wasm.CodeBlob
import net.derfruhling.serene.wasm.Encode
import net.derfruhling.serene.wasm.WasmReader
import net.derfruhling.serene.wasm.WasmWriter
import net.derfruhling.serene.wasm.printer.ExpressionFormatter
import net.derfruhling.serene.wasm.printer.InlineExpressionFormatter
import net.derfruhling.serene.wasm.printer.Printable
import net.derfruhling.serene.wasm.printer.Printer
import net.derfruhling.serene.wasm.printer.print

data class Global(val type: GlobalType, val expr: CodeBlob) : Encode, Printable {
    override fun encode(out: WasmWriter) {
        type.encode(out)
        out.writeBytes(expr.byteString)
    }

    override fun Printer.print() {
        type.print(this)
        if(expr.isSimpleInlineExpr()) {
            expr.visit(InlineExpressionFormatter(this))
        } else {
            expr.visit(ExpressionFormatter(this))
        }
    }

    constructor(reader: WasmReader) : this(GlobalType(reader), reader.readExpr())
}