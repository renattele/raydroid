package ru.raydroid.plugin.impl.calculator

import ru.raydroid.plugin.api.host.Host
import ru.raydroid.plugin.api.host.service.SearchFieldSelection
import ru.raydroid.plugin.api.host.service.SearchFieldState
import ru.raydroid.plugin.api.model.UiText
import ru.raydroid.plugin.api.presentation.CommandActionScope
import ru.raydroid.plugin.api.presentation.CommandActionTarget
import ru.raydroid.plugin.api.presentation.CommandItemId
import ru.raydroid.plugin.api.presentation.CommandListScope
import ru.raydroid.plugin.api.runtime.CommandAction
import ru.raydroid.plugin.api.runtime.CommandService
import ru.raydroid.plugin.api.ui.Box
import ru.raydroid.plugin.api.ui.BoxAlignment
import ru.raydroid.plugin.api.ui.Color
import ru.raydroid.plugin.api.ui.Column
import ru.raydroid.plugin.api.ui.Detail
import ru.raydroid.plugin.api.ui.EditableText
import ru.raydroid.plugin.api.ui.EditableTextDisplayFormatter
import ru.raydroid.plugin.api.ui.FontSize
import ru.raydroid.plugin.api.ui.FontWeight
import ru.raydroid.plugin.api.ui.Grid
import ru.raydroid.plugin.api.ui.GridAspectRatio
import ru.raydroid.plugin.api.ui.GridScope
import ru.raydroid.plugin.api.ui.Icon
import ru.raydroid.plugin.api.ui.Modifier
import ru.raydroid.plugin.api.ui.RayScope
import ru.raydroid.plugin.api.ui.Row
import ru.raydroid.plugin.api.ui.ShapeToken
import ru.raydroid.plugin.api.ui.Spacing
import ru.raydroid.plugin.api.ui.Text
import ru.raydroid.plugin.api.ui.fillMaxSize
import ru.raydroid.plugin.api.ui.onClick
import ru.raydroid.plugin.api.ui.weight

class CalculatorCommand : CommandService() {
    private var expression: String = ""
    private var calculation: CalculationResult? = null
    private var fullscreenOpen = false
    private var syncedSearchFieldValues = emptySet<String>()
    private var ignoreNextEmptyFullscreenQuery = false
    private var expressionSelection = 0
    private var showCommandHelp = false

    override fun CommandListScope.content() {
        val result = calculation
        entry(
            id = CommandItemId.CommandRoot,
            title = result?.expression?.let(UiText::Plain) ?: UiText.Resource("command.calculator.title"),
            description = if (result == null) {
                UiText.Resource("command.calculator.description")
            } else {
                null
            },
            icon = if (result == null) CalculatorIcon else null,
            iconColor = if (result == null) CalculatorIconColor else null,
            trailingText = result?.formattedValue?.let(UiText::Plain)
        )
    }

    override fun RayScope.fullscreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            alignment = BoxAlignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                spacing = Spacing.Medium
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    spacing = Spacing.Small
                ) {
                    EditableText(
                        id = EXPRESSION_FIELD_ID,
                        value = expression,
                        selection = expressionSelection,
                        displayFormatter = EditableTextDisplayFormatter.CalculatorExpression,
                        placeholder = UiText.Resource("command.calculator.description"),
                        multiline = true,
                        maxLines = CalculatorExpressionMaxLines,
                        autoScrollToEnd = true
                    ) { value, selection ->
                        replaceInput(value, selection = selection, syncSearchField = true)
                    }
                }
                Detail(
                    markdown = resultMarkdown(),
                    autoScrollToEnd = true,
                    showScrollHandle = true
                )

                Grid(
                    filtering = false,
                    columns = 4,
                    aspectRatio = GridAspectRatio.ThreeToTwo
                ) {
                    InputButtons.forEachIndexed { index, button ->
                        calculatorButton(index, button)
                    }
                }

                Grid(
                    filtering = false,
                    columns = 2,
                    aspectRatio = GridAspectRatio.ThreeToTwo
                ) {
                    EraseButtons.forEachIndexed { index, button ->
                        calculatorButton(index + InputButtons.size, button)
                    }
                }
            }
            if (showCommandHelp) {
                Box(
                    modifier = Modifier.fillMaxSize().onClick {
                        showCommandHelp = false
                        renderFullscreen()
                    },
                    alignment = BoxAlignment.Center
                ) {
                    Column(
                        spacing = Spacing.Small,
                        shape = ShapeToken.Medium
                    ) {
                        commandHelp()
                    }
                }
            }
        }
    }

    override fun CommandActionScope.actions(target: CommandActionTarget) {
        action(
            title = UiText.Plain("Command help"),
            icon = Icon.Builtin("Help")
        ) {
            showCommandHelp = true
            renderFullscreen()
        }
    }

    override suspend fun execute(action: CommandAction) {
        when (action) {
            is CommandAction.OpenCommand -> {
                fullscreenOpen = true
                ignoreNextEmptyFullscreenQuery = expression.isNotBlank() || query.isNotBlank()
                if (expression.isBlank() && query.isNotBlank()) {
                    expression = query
                    expressionSelection = expression.length
                    calculation = calculateExpression(expression)
                    render()
                }
                syncSearchField()
                renderFullscreen()
            }
            is CommandAction.Enter -> {
                fullscreenOpen = true
                ignoreNextEmptyFullscreenQuery = expression.isNotBlank()
                syncSearchField()
                renderFullscreen()
            }
            is CommandAction.Focus -> Unit
            is CommandAction.CloseCommand -> {
                fullscreenOpen = false
                ignoreNextEmptyFullscreenQuery = false
                showCommandHelp = false
                expression = ""
                expressionSelection = 0
                calculation = null
            }
            is CommandAction.Type -> {
                if (consumeSyncedSearchFieldValue(action.query)) {
                    renderFullscreen()
                } else if (fullscreenOpen && action.query.isBlank() && ignoreNextEmptyFullscreenQuery) {
                    ignoreNextEmptyFullscreenQuery = false
                    renderFullscreen()
                } else {
                    ignoreNextEmptyFullscreenQuery = false
                    replaceInput(
                        value = action.query,
                        selection = inferSelectionAfterEdit(
                            previous = expression,
                            updated = action.query
                        )
                    )
                }
            }
        }
    }

    private suspend fun appendInput(button: CalculatorButton.Insert) {
        val selection = expressionSelection
            .coerceIn(0, expression.length)
            .let { currentSelection ->
                when {
                    button.value == ")" && expression.getOrNull(currentSelection) == ')' -> {
                        replaceInput(
                            value = expression,
                            selection = currentSelection + 1,
                            syncSearchField = true
                        )
                        return
                    }
                    button.value.isExpressionSeparator() -> expression.skipTechnicalClosingParentheses(currentSelection)
                    button.value.isLogBaseShortcut() -> expression.skipTechnicalClosingParentheses(currentSelection)
                    else -> currentSelection
                }
            }
        val value = button.value
        val updatedExpression = expression.replaceRange(selection, selection, value)
        replaceInput(
            value = updatedExpression,
            selection = selection + value.length + button.cursorOffset,
            syncSearchField = true
        )
    }

    private suspend fun replaceInput(
        value: String,
        selection: Int = value.length,
        syncSearchField: Boolean = false
    ) {
        expression = value
        expressionSelection = selection.coerceIn(0, expression.length)
        calculation = calculateExpression(expression)
        if (syncSearchField) {
            syncSearchField()
        }
        render()
        renderFullscreen()
    }

    private suspend fun syncSearchField() {
        syncedSearchFieldValues += expression
        Host.searchField.setState(
            SearchFieldState(
                text = expression,
                selection = SearchFieldSelection.CursorAtEnd
            )
        )
    }

    private fun consumeSyncedSearchFieldValue(value: String): Boolean {
        if (value !in syncedSearchFieldValues) return false
        syncedSearchFieldValues -= value
        return true
    }

    private fun inferSelectionAfterEdit(previous: String, updated: String): Int {
        if (previous == updated) return expressionSelection.coerceIn(0, updated.length)
        val prefixLength = previous.commonPrefixWith(updated).length
        var suffixLength = 0
        while (
            suffixLength < previous.length - prefixLength &&
            suffixLength < updated.length - prefixLength &&
            previous[previous.lastIndex - suffixLength] == updated[updated.lastIndex - suffixLength]
        ) {
            suffixLength++
        }
        return (updated.length - suffixLength).coerceIn(0, updated.length)
    }

    private fun String.skipTechnicalClosingParentheses(selection: Int): Int {
        var index = selection
        while (getOrNull(index) == ')') {
            index++
        }
        return index
    }

    private fun String.isExpressionSeparator(): Boolean =
        this in ExpressionSeparatorButtons

    private fun String.isLogBaseShortcut(): Boolean =
        this in LogBaseShortcutButtons

    private fun GridScope.calculatorButton(
        index: Int,
        button: CalculatorButton
    ) {
        item(
            id = CommandItemId("calculator-button-$index"),
            title = UiText.Plain(button.title),
            modifier = Modifier.onClick {
                when (button) {
                    is CalculatorButton.Insert -> appendInput(button)
                    CalculatorButton.Backspace -> backspaceInput()
                    CalculatorButton.Clear -> replaceInput("", syncSearchField = true)
                }
            }
        )
    }

    private fun RayScope.commandHelp() {
        Text(
            text = UiText.Plain("Commands"),
            fontSize = FontSize.Large,
            fontWeight = FontWeight.Bold
        )
        CommandHelpRows.forEach { row ->
            Row(spacing = Spacing.ExtraSmall) {
                Text(
                    text = UiText.Plain(row.command),
                    fontSize = FontSize.Small,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = UiText.Plain("- ${row.description}"),
                    fontSize = FontSize.Small,
                    color = Color.OnSurfaceVariant
                )
            }
        }
    }

    private suspend fun backspaceInput() {
        val selection = expressionSelection.coerceIn(0, expression.length)
        if (selection <= 0) {
            replaceInput(expression, selection = 0, syncSearchField = true)
            return
        }
        replaceInput(
            value = expression.removeRange(selection - 1, selection),
            selection = selection - 1,
            syncSearchField = true
        )
    }

    private fun resultMarkdown(): String {
        val result = calculation
        val valueText = result?.formattedValue
            ?: (if (expression.isInvalidCompleteExpression()) ErrorText else null)
            ?: " "
        return "# $valueText"
    }

    private fun String.isInvalidCompleteExpression(): Boolean {
        val text = trim()
        if (text.isBlank()) return false
        if (text.isPlainNumberInput()) return false
        if (text.hasUnsupportedCharacters()) return true
        if (text.hasExplicitOperatorError()) return true
        var balance = 0
        text.forEach { char ->
            when (char) {
                '(' -> balance++
                ')' -> {
                    balance--
                    if (balance < 0) return true
                }
            }
        }
        if (balance > 0) return false
        val last = text.last()
        if (last in IncompleteTrailingCharacters) return false
        val lower = text.lowercase()
        if (IncompleteFunctionTails.any { tail -> lower.endsWith(tail) }) return false
        return calculation == null && text.isClearlyClosedExpression()
    }

    private fun String.hasUnsupportedCharacters(): Boolean =
        any { char -> !char.isLetterOrDigit() && !char.isWhitespace() && char !in SupportedExpressionSymbols }

    private fun String.isPlainNumberInput(): Boolean =
        all { char -> char.isDigit() || char.isWhitespace() || char == '.' || char == ',' }

    private fun String.hasExplicitOperatorError(): Boolean =
        zipWithNext().any { (left, right) ->
            left in NonUnaryOperators && right in NonUnaryOperators
        }

    private fun String.isClearlyClosedExpression(): Boolean =
        lastOrNull()?.let { last -> last.isDigit() || last == ')' || last.isLetter() } == true

    private sealed interface CalculatorButton {
        val title: String

        data class Insert(
            override val title: String,
            val value: String,
            val cursorOffset: Int = 0
        ) : CalculatorButton

        data object Backspace : CalculatorButton {
            override val title: String = "\u232B"
        }

        data object Clear : CalculatorButton {
            override val title: String = "C"
        }
    }

    private companion object {
        val CalculatorIcon = Icon.Builtin("Calculate")
        val CalculatorIconColor = Color.OnSurfaceVariant
        const val EXPRESSION_FIELD_ID = "calculator-expression"
        const val CalculatorExpressionMaxLines = 16
        const val ErrorText = "Error"
        val IncompleteTrailingCharacters = setOf('+', '-', '*', '/', '^', '%', '(')
        val SupportedExpressionSymbols = setOf('+', '-', '*', '/', '^', '%', '!', '(', ')', '.', ',')
        val NonUnaryOperators = setOf('*', '/', '^', '%')
        val ExpressionSeparatorButtons = setOf("+", "-", "*", "/", "%")
        val LogBaseShortcutButtons = setOf("bin", "oct", "hex")
        val CommandHelpRows = listOf(
            CommandHelpRow("sqrt", "square root: sqrt(9)"),
            CommandHelpRow("^", "power: 2^(8)"),
            CommandHelpRow("log", "logarithm with a base: log(8)2"),
            CommandHelpRow("ln, lg", "natural and decimal logarithms"),
            CommandHelpRow("sin, cos, tg, ctg", "trigonometry"),
            CommandHelpRow("arcsin, arccos, arctg, arcctg", "inverse trigonometric functions"),
            CommandHelpRow("gr", "degrees inside a function: sin(30gr)"),
            CommandHelpRow("!, pi, e", "factorial and constants: 5!, pi, e"),
            CommandHelpRow("bin, oct, hex", "number in binary, octal, or hexadecimal"),
            CommandHelpRow("ns", "convert to a number system: ns2(10), ns16(1010bin)"),
            CommandHelpRow("Backspace, C", "delete one character or clear the expression")
        )
        val IncompleteFunctionTails = listOf(
            "sqrt",
            "log",
            "ln",
            "lg",
            "sin",
            "cos",
            "tan",
            "cot",
            "asin",
            "acos",
            "atan",
            "acot",
            "ns"
        )
        val InputButtons = listOf(
            CalculatorButton.Insert("7", "7"),
            CalculatorButton.Insert("8", "8"),
            CalculatorButton.Insert("9", "9"),
            CalculatorButton.Insert("\u00F7", "/"),
            CalculatorButton.Insert("4", "4"),
            CalculatorButton.Insert("5", "5"),
            CalculatorButton.Insert("6", "6"),
            CalculatorButton.Insert("\u00D7", "*"),
            CalculatorButton.Insert("1", "1"),
            CalculatorButton.Insert("2", "2"),
            CalculatorButton.Insert("3", "3"),
            CalculatorButton.Insert("\u2212", "-"),
            CalculatorButton.Insert("0", "0"),
            CalculatorButton.Insert(".", "."),
            CalculatorButton.Insert("(", "("),
            CalculatorButton.Insert(")", ")"),
            CalculatorButton.Insert("+", "+"),
            CalculatorButton.Insert("%", "%"),
            CalculatorButton.Insert("!", "!"),
            CalculatorButton.Insert("\u03C0", "pi"),
            CalculatorButton.Insert("e", "e"),
            CalculatorButton.Insert("\u221Ax", "sqrt("),
            CalculatorButton.Insert("x\u02B8", "^("),
            CalculatorButton.Insert("log\u2090", "log("),
            CalculatorButton.Insert("ln", "ln("),
            CalculatorButton.Insert("lg", "lg("),
            CalculatorButton.Insert("sin", "sin("),
            CalculatorButton.Insert("cos", "cos("),
            CalculatorButton.Insert("tg", "tan("),
            CalculatorButton.Insert("ctg", "cot("),
            CalculatorButton.Insert("arcsin", "asin("),
            CalculatorButton.Insert("arccos", "acos("),
            CalculatorButton.Insert("arctg", "atan("),
            CalculatorButton.Insert("arcctg", "acot("),
            CalculatorButton.Insert("\u00B0", "gr"),
            CalculatorButton.Insert("x\u2082", "bin"),
            CalculatorButton.Insert("x\u2088", "oct"),
            CalculatorButton.Insert("x\u2081\u2086", "hex"),
            CalculatorButton.Insert("ns\u2099", "ns(")
        )
        val EraseButtons = listOf(
            CalculatorButton.Backspace,
            CalculatorButton.Clear
        )
    }

    private data class CommandHelpRow(
        val command: String,
        val description: String
    )
}
