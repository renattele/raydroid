package ru.raydroid.plugin.impl.calculator

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal data class CalculationResult(
    val expression: String,
    val formattedValue: String,
)

internal fun calculateExpression(input: String): CalculationResult? {
    val expression = input.trim()
    if (!expression.looksLikeExpression()) return null

    expression.calculateNumberSystemConversion()?.let { result ->
        return result
    }

    return runCatching {
        val value = ExpressionParser(expression.normalizedExpression()).parse()
        if (!value.isFinite()) return null
        CalculationResult(
            expression = expression.toDisplayExpression(),
            formattedValue = value.formatCalculationValue(),
        )
    }.getOrNull()
}

private fun String.calculateNumberSystemConversion(): CalculationResult? =
    parseNumberSystemConversion()?.let { conversion ->
        runCatching {
            val value = ExpressionParser(conversion.valueExpression.normalizedExpression()).parse()
            if (!value.isFinite()) return null
            val integer = value.toIntegerOrNull() ?: return null
            CalculationResult(
                expression = conversion.displayExpression,
                formattedValue = integer.toBaseString(conversion.base).withBaseIndex(conversion.base),
            )
        }.getOrNull()
    }

private fun String.parseNumberSystemConversion(): NumberSystemConversion? {
    parseShortcutNumberSystem(BIN_FUNCTION, BINARY_BASE)?.let { return it }
    parseShortcutNumberSystem(OCT_FUNCTION, OCTAL_BASE)?.let { return it }
    parseShortcutNumberSystem(HEX_FUNCTION, HEX_BASE)?.let { return it }
    return parseNumberSystem()
}

private fun String.hasNumberSystemLiteral(): Boolean {
    indices.forEach { index ->
        if (numberSystemLiteralAt(index) != null) return true
    }
    return false
}

private fun String.parseShortcutNumberSystem(
    name: String,
    base: Int,
): NumberSystemConversion? {
    if (!startsWith(name, ignoreCase = true)) return null
    val openIndex = name.length
    if (getOrNull(openIndex) != '(') return null
    val closeIndex = findMatchingParenthesis(openIndex) ?: return null
    if (closeIndex != lastIndex) return null
    return NumberSystemConversion(
        base = base,
        valueExpression = substring(openIndex + 1, closeIndex),
        displayExpression = "${name.lowercase()}(${substring(openIndex + 1, closeIndex).toDisplayExpression()})",
    )
}

private fun String.parseNumberSystem(): NumberSystemConversion? {
    if (!startsWith(NUMBER_SYSTEM_FUNCTION, ignoreCase = true)) return null
    val baseStart = NUMBER_SYSTEM_FUNCTION.length
    val baseEnd: Int
    val baseText: String
    if (getOrNull(baseStart) == '(') {
        val closeIndex = findMatchingParenthesis(baseStart) ?: return null
        baseText = substring(baseStart + 1, closeIndex)
        baseEnd = closeIndex + 1
    } else {
        var index = baseStart
        while (getOrNull(index)?.isDigit() == true) {
            index++
        }
        if (index == baseStart) return null
        baseText = substring(baseStart, index)
        baseEnd = index
    }
    val base = baseText.toIntOrNull()?.takeIf { it in MIN_BASE..MAX_BASE } ?: return null
    if (getOrNull(baseEnd) != '(') return null
    val valueEnd = findMatchingParenthesis(baseEnd) ?: return null
    if (valueEnd != lastIndex) return null
    return NumberSystemConversion(
        base = base,
        valueExpression = substring(baseEnd + 1, valueEnd),
        displayExpression = "ns${base.toString().toSubscript().orEmpty()}(${substring(baseEnd + 1, valueEnd).toDisplayExpression()})",
    )
}

private data class NumberSystemConversion(
    val base: Int,
    val valueExpression: String,
    val displayExpression: String,
)

private fun String.looksLikeExpression(): Boolean {
    if (none { it.isDigit() } && !hasConstant()) return false
    val normalized = normalizedExpression()
    return normalized.any { it in "+*/^%" } ||
        normalized.drop(1).any { it == '-' } ||
        '!' in normalized ||
        '(' in normalized ||
        ')' in normalized ||
        normalized.hasNumberSystemLiteral() ||
        normalized.hasConstant()
}

private fun String.hasConstant(): Boolean = contains(PI_CONSTANT, ignoreCase = true) || contains(E_CONSTANT, ignoreCase = true)

private fun Double.toIntegerOrNull(): Long? {
    val integer = roundToLong()
    return if (abs(this - integer.toDouble()) < INTEGER_EPSILON) {
        integer
    } else {
        null
    }
}

private fun Long.toBaseString(base: Int): String {
    if (this == 0L) return "0"
    val negative = this < 0
    var value = if (negative) -this else this
    val result = StringBuilder()
    while (value > 0) {
        val digit = (value % base).toInt()
        result.append(BaseDigits[digit])
        value /= base
    }
    if (negative) {
        result.append('-')
    }
    return result.reverse().toString()
}

private fun Double.factorial(): Double {
    val integer = toIntegerOrNull() ?: error("Factorial requires integer")
    if (integer < 0) error("Factorial requires non-negative integer")
    if (integer > MAX_FACTORIAL) error("Factorial is too large")
    var result = 1.0
    for (value in 2..integer) {
        result *= value
    }
    return result
}

private fun String.parseBaseInteger(base: Int): Long {
    var result = 0L
    forEach { char ->
        val digit = BaseDigits.indexOf(char.uppercaseChar())
        if (digit !in 0 until base) error("Invalid digit")
        result = result * base + digit
    }
    return result
}

private fun String.numberSystemLiteralAt(start: Int): NumberSystemLiteral? {
    if (getOrNull(start)?.isLetterOrDigit() != true) return null
    var split = start + 1
    while (split <= length && substring(start, split).all { it.isLetterOrDigit() }) {
        val suffix = parseDisplayNumberSystemSuffix(split)
        if (suffix != null) {
            val digits = substring(start, split)
            val value = runCatching { digits.parseBaseInteger(suffix.base) }.getOrNull()
            if (value != null) {
                return NumberSystemLiteral(
                    value = value,
                    base = suffix.base,
                    digits = digits,
                    nextIndex = suffix.nextIndex,
                )
            }
        }
        split++
    }
    return null
}

private data class NumberSystemLiteral(
    val value: Long,
    val base: Int,
    val digits: String,
    val nextIndex: Int,
)

private fun String.withBaseIndex(base: Int): String = this + (base.toString().toSubscript() ?: base.toString())

private fun String.normalizedExpression(): String =
    replace('×', '*')
        .replace('÷', '/')
        .replace(',', '.')

private class ExpressionParser(
    private val source: String,
) {
    private var index = 0

    fun parse(): Double {
        val value = parseExpression()
        skipSpaces()
        if (index != source.length) {
            error("Unexpected token")
        }
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipSpaces()
            value =
                when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
        }
    }

    private fun parseTerm(): Double {
        var value = parsePower()
        while (true) {
            skipSpaces()
            value =
                when {
                    consume('*') -> value * parsePower()
                    consume('/') -> value / parsePower()
                    consume('%') -> value % parsePower()
                    startsImplicitMultiplier() -> value * parsePower()
                    else -> return value
                }
        }
    }

    private fun parsePower(): Double {
        val value = parsePostfix()
        skipSpaces()
        return if (consume('^')) {
            value.pow(parsePower())
        } else {
            value
        }
    }

    private fun parseUnary(): Double {
        skipSpaces()
        return when {
            consume('+') -> parseUnary()
            consume('-') -> -parseUnary()
            else -> parsePrimary()
        }
    }

    private fun parsePostfix(): Double {
        var value = parseUnary()
        while (true) {
            skipSpaces()
            value =
                when {
                    consume('!') -> value.factorial()
                    else -> return value
                }
        }
    }

    private fun parsePrimary(): Double {
        skipSpaces()
        if (consumeIdentifier(SQRT_FUNCTION)) {
            skipSpaces()
            if (!consume('(')) error("Opening parenthesis expected")
            val value = parseExpression()
            skipSpaces()
            if (!consume(')')) error("Missing closing parenthesis")
            return sqrt(value)
        }
        if (consumeIdentifier(LOG_FUNCTION)) {
            skipSpaces()
            if (!consume('(')) error("Opening parenthesis expected")
            val value = parseExpression()
            skipSpaces()
            if (!consume(')')) error("Missing closing parenthesis")
            val base = parseUnary()
            return ln(value) / ln(base)
        }
        if (consumeIdentifier(LN_FUNCTION)) {
            skipSpaces()
            if (!consume('(')) error("Opening parenthesis expected")
            val value = parseExpression()
            skipSpaces()
            if (!consume(')')) error("Missing closing parenthesis")
            return ln(value)
        }
        if (consumeIdentifier(LG_FUNCTION)) {
            skipSpaces()
            if (!consume('(')) error("Opening parenthesis expected")
            val value = parseExpression()
            skipSpaces()
            if (!consume(')')) error("Missing closing parenthesis")
            return ln(value) / ln(10.0)
        }
        parseSingleArgumentFunction()?.let { return it }
        if (consume('(')) {
            val value = parseExpression()
            skipSpaces()
            if (!consume(')')) error("Missing closing parenthesis")
            return value
        }
        parseConstant()?.let { return it }
        parseNumberSystemLiteral()?.let { return it.toDouble() }
        return parseNumber()
    }

    private fun parseConstant(): Double? {
        skipSpaces()
        return when {
            consumeIdentifier(PI_CONSTANT) -> PI
            consumeIdentifier(E_CONSTANT) -> E
            else -> null
        }
    }

    private fun parseNumberSystemLiteral(): Long? {
        skipSpaces()
        val literal = source.numberSystemLiteralAt(index)
        if (literal != null) {
            index = literal.nextIndex
            return literal.value
        }
        return null
    }

    private fun parseNumber(): Double {
        skipSpaces()
        val start = index
        var hasDigits = false
        var hasDot = false
        while (index < source.length) {
            val char = source[index]
            when {
                char.isDigit() -> {
                    hasDigits = true
                    index++
                }

                char == '.' && !hasDot -> {
                    hasDot = true
                    index++
                }

                else -> {
                    break
                }
            }
        }
        if (!hasDigits) error("Number expected")
        return source.substring(start, index).toDouble()
    }

    private fun consumeIdentifier(identifier: String): Boolean {
        if (!source.startsWith(identifier, startIndex = index, ignoreCase = true)) return false
        index += identifier.length
        return true
    }

    private fun parseSingleArgumentFunction(): Double? {
        val function =
            TrigonometricFunctions.firstOrNull { name ->
                source.startsWith(name, startIndex = index, ignoreCase = true)
            } ?: return null
        index += function.length
        skipSpaces()
        if (!consume('(')) error("Opening parenthesis expected")
        val value = parseExpression()
        val argument =
            if (function in DirectTrigonometricFunctions && consumeIdentifier(DEGREES_SUFFIX)) {
                value * PI / 180.0
            } else {
                value
            }
        skipSpaces()
        if (!consume(')')) error("Missing closing parenthesis")
        return when (function) {
            SIN_FUNCTION -> sin(argument)
            COS_FUNCTION -> cos(argument)
            TAN_FUNCTION -> tan(argument)
            COT_FUNCTION -> 1.0 / tan(argument)
            ASIN_FUNCTION -> asin(value)
            ACOS_FUNCTION -> acos(value)
            ATAN_FUNCTION -> atan(value)
            ACOT_FUNCTION -> PI / 2.0 - atan(value)
            else -> error("Unknown function")
        }
    }

    private fun startsImplicitMultiplier(): Boolean =
        source.getOrNull(index) == '(' ||
            source.startsWith(PI_CONSTANT, startIndex = index, ignoreCase = true) ||
            source.startsWith(E_CONSTANT, startIndex = index, ignoreCase = true) ||
            source.startsWith(SQRT_FUNCTION, startIndex = index, ignoreCase = true) ||
            source.startsWith(LOG_FUNCTION, startIndex = index, ignoreCase = true) ||
            source.startsWith(LN_FUNCTION, startIndex = index, ignoreCase = true) ||
            source.startsWith(LG_FUNCTION, startIndex = index, ignoreCase = true) ||
            TrigonometricFunctions.any { function ->
                source.startsWith(function, startIndex = index, ignoreCase = true)
            }

    private fun consume(char: Char): Boolean {
        if (index >= source.length || source[index] != char) return false
        index++
        return true
    }

    private fun skipSpaces() {
        while (index < source.length && source[index].isWhitespace()) {
            index++
        }
    }
}

private fun String.toDisplayExpression(): String =
    formatPowers()
        .formatSquareRoots()
        .formatLogarithms()
        .formatNumberSystemLiterals()
        .formatConstants()
        .formatDegrees()

private fun String.formatPowers(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (this[index] == '^') {
            val exponent = exponentAfter(index)
            if (exponent != null) {
                val exponentText = substring(exponent.range)
                val superscript = exponentText.toSuperscript()
                if (superscript != null) {
                    result.append(superscript)
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

private fun String.exponentAfter(powerIndex: Int): FormattedExponent? {
    val start = powerIndex + 1
    return if (getOrNull(start) == '(') {
        val end = findMatchingParenthesis(start) ?: return null
        FormattedExponent(
            range = (start + 1) until end,
            nextIndex = end + 1,
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
        FormattedExponent(
            range = start until index,
            nextIndex = index,
        )
    }
}

private data class FormattedExponent(
    val range: IntRange,
    val nextIndex: Int,
)

private fun String.formatLogarithms(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (startsWith(LOG_FUNCTION, startIndex = index, ignoreCase = true) && getOrNull(index + LOG_FUNCTION.length) == '(') {
            val openIndex = index + LOG_FUNCTION.length
            val end = findMatchingParenthesis(openIndex)
            if (end != null) {
                val base = baseAfter(end)
                if (base != null) {
                    val value = substring(openIndex + 1, end).toDisplayExpression()
                    val baseText = substring(base.range)
                    val subscript = baseText.toSubscript()
                    if (subscript != null) {
                        result
                            .append(LOG_FUNCTION)
                            .append(subscript)
                            .append('(')
                            .append(value)
                            .append(')')
                        index = base.nextIndex
                        continue
                    }
                }
            }
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
        if (index > 0 && startsWith(DEGREES_SUFFIX, startIndex = index, ignoreCase = true) && this[index - 1].isDigit()) {
            result.append(DegreeSymbol)
            index += DEGREES_SUFFIX.length
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
            result
                .append(literal.digits)
                .append(
                    literal.base
                        .toString()
                        .toSubscript()
                        .orEmpty(),
                )
            index = literal.nextIndex
        } else {
            result.append(this[index])
            index++
        }
    }
    return result.toString()
}

private fun String.formatConstants(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (startsWith(PI_CONSTANT, startIndex = index, ignoreCase = true)) {
            result.append(PI_SYMBOL)
            index += PI_CONSTANT.length
        } else {
            result.append(this[index])
            index++
        }
    }
    return result.toString()
}

private fun String.parseDisplayNumberSystemSuffix(numberEnd: Int): NumberSystemSuffix? {
    val oldIndex = numberEnd
    return when {
        startsWith(BIN_FUNCTION, startIndex = oldIndex, ignoreCase = true) -> {
            NumberSystemSuffix(
                base = BINARY_BASE,
                nextIndex = oldIndex + BIN_FUNCTION.length,
            )
        }

        startsWith(OCT_FUNCTION, startIndex = oldIndex, ignoreCase = true) -> {
            NumberSystemSuffix(
                base = OCTAL_BASE,
                nextIndex = oldIndex + OCT_FUNCTION.length,
            )
        }

        startsWith(HEX_FUNCTION, startIndex = oldIndex, ignoreCase = true) -> {
            NumberSystemSuffix(
                base = HEX_BASE,
                nextIndex = oldIndex + HEX_FUNCTION.length,
            )
        }

        startsWith(NUMBER_SYSTEM_FUNCTION, startIndex = oldIndex, ignoreCase = true) -> {
            val baseStart = oldIndex + NUMBER_SYSTEM_FUNCTION.length
            if (getOrNull(baseStart) == '(') {
                val closeIndex = findMatchingParenthesis(baseStart) ?: return null
                val base =
                    substring(baseStart + 1, closeIndex)
                        .toIntOrNull()
                        ?.takeIf { it in MIN_BASE..MAX_BASE }
                        ?: return null
                NumberSystemSuffix(base = base, nextIndex = closeIndex + 1)
            } else {
                var index = baseStart
                while (getOrNull(index)?.isDigit() == true) {
                    index++
                }
                if (index == baseStart) return null
                val base =
                    substring(baseStart, index)
                        .toIntOrNull()
                        ?.takeIf { it in MIN_BASE..MAX_BASE }
                        ?: return null
                NumberSystemSuffix(base = base, nextIndex = index)
            }
        }

        else -> {
            null
        }
    }
}

private data class NumberSystemSuffix(
    val base: Int,
    val nextIndex: Int,
)

private fun String.baseAfter(closeIndex: Int): FormattedExponent? {
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
    return FormattedExponent(
        range = start until index,
        nextIndex = index,
    )
}

private fun String.formatSquareRoots(): String {
    val result = StringBuilder(length)
    var index = 0
    while (index < length) {
        if (startsWith(SQRT_FUNCTION, startIndex = index, ignoreCase = true) && getOrNull(index + SQRT_FUNCTION.length) == '(') {
            val openIndex = index + SQRT_FUNCTION.length
            val end = findMatchingParenthesis(openIndex)
            if (end != null) {
                val radicand = substring(openIndex + 1, end)
                val formattedRadicand = radicand.toDisplayExpression()
                result.append(RootSymbol)
                if (formattedRadicand.isSimpleRadicand()) {
                    result.append(formattedRadicand)
                } else {
                    result.append('(').append(formattedRadicand).append(')')
                }
                index = end + 1
                continue
            }
        }
        result.append(this[index])
        index++
    }
    return result.toString()
}

private fun String.findMatchingParenthesis(openIndex: Int): Int? {
    var depth = 0
    for (index in openIndex until length) {
        when (this[index]) {
            '(' -> {
                depth++
            }

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

private fun String.isSimpleRadicand(): Boolean = all { char -> char.isDigit() || char == '.' || char == ',' }

private fun Double.formatCalculationValue(): String {
    val integer = roundToLong()
    if (abs(this - integer.toDouble()) < INTEGER_EPSILON) {
        return integer.toString()
    }

    val rounded = round(this * FRACTION_SCALE) / FRACTION_SCALE
    if (abs(rounded) < INTEGER_EPSILON) return "0"

    val text = rounded.toString()
    if ('E' in text || 'e' in text) return text
    return text.trimEnd('0').trimEnd('.')
}

private const val SQRT_FUNCTION = "sqrt"
private const val LOG_FUNCTION = "log"
private const val LN_FUNCTION = "ln"
private const val LG_FUNCTION = "lg"
private const val NUMBER_SYSTEM_FUNCTION = "ns"
private const val BIN_FUNCTION = "bin"
private const val OCT_FUNCTION = "oct"
private const val HEX_FUNCTION = "hex"
private const val SIN_FUNCTION = "sin"
private const val COS_FUNCTION = "cos"
private const val TAN_FUNCTION = "tan"
private const val COT_FUNCTION = "cot"
private const val ASIN_FUNCTION = "asin"
private const val ACOS_FUNCTION = "acos"
private const val ATAN_FUNCTION = "atan"
private const val ACOT_FUNCTION = "acot"
private const val DEGREES_SUFFIX = "gr"
private const val PI_CONSTANT = "pi"
private const val E_CONSTANT = "e"
private const val RootSymbol = '√'
private const val DegreeSymbol = '°'
private const val BINARY_BASE = 2
private const val PI_SYMBOL = '\u03C0'
private const val OCTAL_BASE = 8
private const val HEX_BASE = 16
private const val MIN_BASE = 2
private const val MAX_BASE = 36
private const val INTEGER_EPSILON = 1e-10
private const val FRACTION_SCALE = 1_000_000_000_000.0
private const val MAX_FACTORIAL = 170L
private const val BaseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private val SuperscriptChars =
    mapOf(
        '0' to '⁰',
        '1' to '¹',
        '2' to '²',
        '3' to '³',
        '4' to '⁴',
        '5' to '⁵',
        '6' to '⁶',
        '7' to '⁷',
        '8' to '⁸',
        '9' to '⁹',
        '+' to '⁺',
        '-' to '⁻',
    )
private val SubscriptChars =
    mapOf(
        '0' to '₀',
        '1' to '₁',
        '2' to '₂',
        '3' to '₃',
        '4' to '₄',
        '5' to '₅',
        '6' to '₆',
        '7' to '₇',
        '8' to '₈',
        '9' to '₉',
        '+' to '₊',
        '-' to '₋',
    )
private val TrigonometricFunctions =
    listOf(
        ASIN_FUNCTION,
        ACOS_FUNCTION,
        ATAN_FUNCTION,
        ACOT_FUNCTION,
        SIN_FUNCTION,
        COS_FUNCTION,
        TAN_FUNCTION,
        COT_FUNCTION,
    )
private val DirectTrigonometricFunctions =
    listOf(
        SIN_FUNCTION,
        COS_FUNCTION,
        TAN_FUNCTION,
        COT_FUNCTION,
    )
