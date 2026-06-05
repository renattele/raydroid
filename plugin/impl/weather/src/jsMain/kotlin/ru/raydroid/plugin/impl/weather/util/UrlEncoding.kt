package ru.raydroid.plugin.impl.weather.util

internal fun String.urlEncode(): String {
    val bytes = encodeToByteArray()
    return buildString {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xff
            if (
                value in 'A'.code..'Z'.code ||
                value in 'a'.code..'z'.code ||
                value in '0'.code..'9'.code ||
                value == '-'.code ||
                value == '_'.code ||
                value == '.'.code ||
                value == '~'.code
            ) {
                append(value.toChar())
            } else {
                append('%')
                append(Hex[value ushr 4])
                append(Hex[value and 0x0f])
            }
        }
    }
}

private const val Hex = "0123456789ABCDEF"
