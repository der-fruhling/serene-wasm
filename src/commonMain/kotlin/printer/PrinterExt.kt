package net.derfruhling.serene.wasm.printer

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlin.experimental.and

object Hex {
    private val digits = arrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    )

    fun format(byte: Byte): String {
        val msb = (byte and 0xF0.toByte()).toUByte().toInt() ushr 4
        val lsb = (byte and 0x0F.toByte()).toUByte().toInt()
        return charArrayOf(digits[msb], digits[lsb]).concatToString()
    }
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
