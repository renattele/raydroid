package ru.raydroid.plugin.host.impl.presentation

import androidx.compose.foundation.text.input.OutputTransformation

internal fun calculatorExpressionOutputTransformation(): OutputTransformation =
    OutputTransformation {
        val rawValue = toString()
        val displayValue = rawValue.toCalculatorDisplayExpression()
        if (displayValue != rawValue) {
            replaceWithCommonAffixes(
                rawValue = rawValue,
                displayValue = displayValue
            )
        }
    }

internal fun staticOutputTransformation(rawValue: String, displayValue: String): OutputTransformation =
    OutputTransformation {
        if (toString() == rawValue) {
            replaceWithCommonAffixes(
                rawValue = rawValue,
                displayValue = displayValue
            )
        }
    }

private fun androidx.compose.foundation.text.input.TextFieldBuffer.replaceWithCommonAffixes(
    rawValue: String,
    displayValue: String
) {
    val prefixLength = rawValue.commonPrefixWith(displayValue).length
    var suffixLength = 0
    while (
        suffixLength < rawValue.length - prefixLength &&
        suffixLength < displayValue.length - prefixLength &&
        rawValue[rawValue.lastIndex - suffixLength] == displayValue[displayValue.lastIndex - suffixLength]
    ) {
        suffixLength++
    }
    replace(
        start = prefixLength,
        end = rawValue.length - suffixLength,
        text = displayValue.substring(prefixLength, displayValue.length - suffixLength)
    )
}

private fun String.toCalculatorDisplayExpression(): String =
    formatPowers().formatSquareRoots().formatLogarithms().formatNumberSystemLiterals().formatDegrees()

private fun String.formatPowers(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (this[index] == '^') {
            val exponent = exponentAfter(index)
            if (exponent != null) {
                val exponentText = substring(exponent.range)
                val superscript = exponentText.toSuperscript()
                if (superscript != null && exponent.openParenthesisUnclosed) {
                    result.append(SuperscriptOpenParenthesis).append(superscript)
                    index = exponent.nextIndex
                    continue
                } else if (superscript != null) {
                    result.append(superscript)
                    index = exponent.nextIndex
                    continue
                } else if (exponentText.isEmpty() && getOrNull(index + 1) == '(') {
                    result.append(SuperscriptOpenParenthesis)
                    index = exponent.nextIndex
                    continue
                }
            }
        }
        result.append(this[index])
        index++
    }
    return result.toString()
}

private fun String.exponentAfter(powerIndex: Int): FormattedRange? {
    val start = powerIndex + 1
    return if (getOrNull(start) == '(') {
        val end = findMatchingParenthesis(start) ?: length
        FormattedRange(
            range = (start + 1) until end,
            nextIndex = if (end < length) end + 1 else end,
            openParenthesisUnclosed = end == length
        )
    } else {
        var index = start
        if (getOrNull(index) == '+' || getOrNull(index) == '-') {
            index++
        }
        val digitStart = index
        while (getOrNull(index)?.isDigit() == true) {
            index++
        }
        if (index == digitStart) return null
        FormattedRange(
            range = start until index,
            nextIndex = index
        )
    }
}

private fun String.formatSquareRoots(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (startsWith(SqrtFunction, startIndex = index, ignoreCase = true) && getOrNull(index + SqrtFunction.length) == '(') {
            val openIndex = index + SqrtFunction.length
            val end = findMatchingParenthesis(openIndex)
            val radicandEnd = end ?: length
            val radicand = substring(openIndex + 1, radicandEnd).toCalculatorDisplayExpression()
            result.append(RootSymbol)
            if (end == null) {
                result.append('(').append(radicand)
            } else if (radicand.isEmpty()) {
                result.append('(')
            } else if (radicand.isSimpleRadicand()) {
                result.append(radicand)
            } else {
                result.append('(').append(radicand).append(')')
            }
            index = if (end != null) end + 1 else length
            continue
        }
        result.append(this[index])
        index++
    }
    return result.toString()
}

private fun String.formatLogarithms(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (startsWith(LogFunction, startIndex = index, ignoreCase = true) && getOrNull(index + LogFunction.length) == '(') {
            val openIndex = index + LogFunction.length
            val end = findMatchingParenthesis(openIndex)
            val argumentEnd = end ?: length
            val argument = substring(openIndex + 1, argumentEnd).toCalculatorDisplayExpression()
            val base = if (end != null) baseAfter(end) else null
            val subscript = base?.let { substring(it.range).toSubscript() }
            result.append(LogFunction)
            if (subscript != null) {
                result.append(subscript)
            }
            result.append('(').append(argument)
            if (end != null) {
                result.append(')')
            }
            index = base?.takeIf { subscript != null }?.nextIndex
                ?: if (end != null) end + 1 else length
            continue
        }
        result.append(this[index])
        index++
    }
    return result.toString()
}

private fun String.formatDegrees(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (index > 0 && startsWith(DegreesSuffix, startIndex = index, ignoreCase = true) && this[index - 1].isDigit()) {
            result.append(DegreeSymbol)
            index += DegreesSuffix.length
        } else {
            result.append(this[index])
            index++
        }
    }
    return result.toString()
}

private fun String.formatNumberSystemLiterals(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        val literal = numberSystemLiteralAt(index)
        if (literal != null) {
            result.append(literal.digits)
                .append(literal.base.toString().toSubscript().orEmpty())
            index = literal.nextIndex
        } else {
            result.append(this[index])
            index++
        }
    }
    return result.toString()
}

private fun String.numberSystemLiteralAt(index: Int): NumberSystemLiteral? {
    if (getOrNull(index)?.isLetterOrDigit() != true) return null
    var digitEnd = index + 1
    while (digitEnd <= length && getOrNull(digitEnd - 1)?.isLetterOrDigit() == true) {
        val suffix = numberSystemSuffixAt(digitEnd)
        if (suffix != null) {
            return NumberSystemLiteral(
                digits = substring(index, digitEnd),
                base = suffix.base,
                nextIndex = suffix.nextIndex
            )
        }
        digitEnd++
    }
    return null
}

private fun String.numberSystemSuffixAt(index: Int): NumberSystemSuffix? = when {
    startsWith(BinFunction, startIndex = index, ignoreCase = true) -> NumberSystemSuffix(
        base = BinaryBase,
        nextIndex = index + BinFunction.length
    )
    startsWith(OctFunction, startIndex = index, ignoreCase = true) -> NumberSystemSuffix(
        base = OctalBase,
        nextIndex = index + OctFunction.length
    )
    startsWith(HexFunction, startIndex = index, ignoreCase = true) -> NumberSystemSuffix(
        base = HexBase,
        nextIndex = index + HexFunction.length
    )
    startsWith(NumberSystemFunction, startIndex = index, ignoreCase = true) -> {
        val baseStart = index + NumberSystemFunction.length
        if (getOrNull(baseStart) == '(') {
            val closeIndex = findMatchingParenthesis(baseStart) ?: return null
            val base = substring(baseStart + 1, closeIndex).toIntOrNull()
                ?.takeIf { it in MinBase..MaxBase }
                ?: return null
            NumberSystemSuffix(base = base, nextIndex = closeIndex + 1)
        } else {
            var baseEnd = baseStart
            while (getOrNull(baseEnd)?.isDigit() == true) {
                baseEnd++
            }
            if (baseEnd == baseStart) return null
            val base = substring(baseStart, baseEnd).toIntOrNull()
                ?.takeIf { it in MinBase..MaxBase }
                ?: return null
            NumberSystemSuffix(base = base, nextIndex = baseEnd)
        }
    }
    else -> null
}

private fun String.baseAfter(closeIndex: Int): FormattedRange? {
    val start = closeIndex + 1
    var index = start
    if (getOrNull(index) == '+') {
        index++
    }
    val digitStart = index
    while (getOrNull(index)?.isDigit() == true) {
        index++
    }
    if (getOrNull(index) == '.') {
        index++
        while (getOrNull(index)?.isDigit() == true) {
            index++
        }
    }
    if (index == digitStart) return null
    return FormattedRange(
        range = start until index,
        nextIndex = index
    )
}

private fun String.findMatchingParenthesis(openIndex: Int): Int? {
    var depth = 0
    for (index in openIndex until length) {
        when (this[index]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

private fun String.toSuperscript(): String? {
    if (isBlank()) return null
    return buildString(length) {
        this@toSuperscript.forEach { char ->
            append(SuperscriptChars[char] ?: return null)
        }
    }
}

private fun String.toSubscript(): String? {
    if (isBlank()) return null
    return buildString(length) {
        this@toSubscript.forEach { char ->
            append(SubscriptChars[char] ?: return null)
        }
    }
}

private fun String.isSimpleRadicand(): Boolean =
    all { char -> char.isLetterOrDigit() || char == '.' || char in SuperscriptChars.values || char in SubscriptChars.values }

private data class FormattedRange(
    val range: IntRange,
    val nextIndex: Int,
    val openParenthesisUnclosed: Boolean = false
)

private data class NumberSystemLiteral(
    val digits: String,
    val base: Int,
    val nextIndex: Int
)

private data class NumberSystemSuffix(
    val base: Int,
    val nextIndex: Int
)

private const val SqrtFunction = "sqrt"
private const val LogFunction = "log"
private const val DegreesSuffix = "gr"
private const val BinFunction = "bin"
private const val OctFunction = "oct"
private const val HexFunction = "hex"
private const val NumberSystemFunction = "ns"
private const val BinaryBase = 2
private const val OctalBase = 8
private const val HexBase = 16
private const val MinBase = 2
private const val MaxBase = 36
private const val RootSymbol = '\u221A'
private const val DegreeSymbol = '\u00B0'
private const val SuperscriptOpenParenthesis = '\u207D'
private val SuperscriptChars = mapOf(
    '0' to '\u2070',
    '1' to '\u00B9',
    '2' to '\u00B2',
    '3' to '\u00B3',
    '4' to '\u2074',
    '5' to '\u2075',
    '6' to '\u2076',
    '7' to '\u2077',
    '8' to '\u2078',
    '9' to '\u2079',
    '+' to '\u207A',
    '-' to '\u207B',
    '(' to '\u207D',
    ')' to '\u207E'
)
private val SubscriptChars = mapOf(
    '0' to '\u2080',
    '1' to '\u2081',
    '2' to '\u2082',
    '3' to '\u2083',
    '4' to '\u2084',
    '5' to '\u2085',
    '6' to '\u2086',
    '7' to '\u2087',
    '8' to '\u2088',
    '9' to '\u2089',
    '+' to '\u208A',
    '-' to '\u208B'
)
