package net.derfruhling.serene.wasm.printer

interface Printer : Appendable {
    val indentStr: String
    var indent: Int
    val names: NameResolver

    fun appendIndented(string: String) = append(indentStr + string)
    fun appendLineIndented(string: String) = appendLine(indentStr + string)

    fun beginInlineExprBlock() {}
    fun endInlineExprBlock() {}

    fun beginExprBlock() {
        indent++
        appendLine()
    }

    fun endExprBlock() {
        indent--
        append(indentStr)
    }

    fun word(word: String)
    fun wrap(fn: Printer.() -> Unit)
    fun wrapInline(fn: Printer.() -> Unit)
    fun nest(fn: Printer.() -> Unit)
}

internal class AppendablePrinterImpl(val appendable: Appendable) : Printer, Appendable by appendable {
    private var _indentStr: String? = null

    override val indentStr: String
        get() = _indentStr ?: "  ".repeat(indent).also { _indentStr = it }
    override var indent: Int = 0
        set(value) {
            field = value
            _indentStr = null
        }
    override val names: NameResolver = NameResolver.Default()

    private var lastWhitespace: Boolean = false
    private var lastIsOpen: Boolean = false

    private fun checkChar(value: Char) {
        lastWhitespace = value.isWhitespace()
        lastIsOpen = value == '('
    }

    override fun append(value: Char): Appendable {
        checkChar(value)
        return appendable.append(value)
    }

    override fun append(value: CharSequence?): Appendable {
        val value = value ?: "null"
        value.lastOrNull()?.let { checkChar(it) }
        return appendable.append(value)
    }

    override fun append(
        value: CharSequence?,
        startIndex: Int,
        endIndex: Int
    ): Appendable {
        return append(value?.subSequence(startIndex, endIndex))
    }

    override fun wrap(fn: Printer.() -> Unit) {
        appendIndented("(")

        try {
            fn()
        } finally {
            appendLine(')')
        }
    }

    override fun wrapInline(fn: Printer.() -> Unit) {
        word("(")

        try {
            fn()
        } finally {
            append(')')
        }
    }

    override fun nest(fn: Printer.() -> Unit) {
        appendLineIndented(buildString {
            printer().fn()
        })
    }

    override fun word(word: String) {
        if(!lastIsOpen && !lastWhitespace) append(' ')
        append(word)
    }
}

fun Appendable.printer(): Printer = AppendablePrinterImpl(this)
