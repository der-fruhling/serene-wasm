package net.derfruhling.serene.wasm.printer

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations

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

private fun Appendable.commonString(text: ByteArray) {
    for(b in text) {
        when (b) {
            0x22.toByte() -> append("\\\"")
            0x5C.toByte() -> append("\\\\")
            in 0x20..<0x7F -> append(b.toInt().toChar())
            0x09.toByte() -> append("\\t")
            0x0A.toByte() -> append("\\n")
            0x0D.toByte() -> append("\\r")
            else -> append("\\${Hex.format(b)}")
        }
    }

    append('"')
}

fun Printer.string(text: String) {
    word("\"")
    commonString(text.encodeToByteArray())
}

@OptIn(UnsafeByteStringApi::class)
fun Printer.string(text: ByteString) {
    word("\"")
    UnsafeByteStringOperations.withByteArrayUnsafe(text) {
        commonString(it)
    }
}

fun Printer.string(text: ByteArray) {
    word("\"")
    commonString(text)
}

private fun Appendable.string(text: String) {
    append('"')
    commonString(text.encodeToByteArray())
}

private val identRegex = Regex("^[0-9A-Za-z!#$%&'*+\\-./:<=>?@\\\\^_`|~]+$")

fun Printer.identWord(text: String) {
    if(text.matches(identRegex)) {
        word($$"$$$text")
    } else {
        word("$")
        string(text)
    }
}

fun String.toIdentWord(): String {
    return if(matches(identRegex)) {
        "$$this"
    } else {
        "$${buildString { string(this@toIdentWord) }}"
    }
}

private class AppendablePrinterImpl(val appendable: Appendable) : Printer, Appendable by appendable {
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
