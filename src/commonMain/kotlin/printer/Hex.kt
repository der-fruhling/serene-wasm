package net.derfruhling.serene.wasm.printer

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