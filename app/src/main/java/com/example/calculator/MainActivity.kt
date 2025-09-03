package com.example.calculator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calculator.databinding.ActivityMainBinding
import java.util.LinkedList
import java.util.Stack

class MainActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityMainBinding

    // State variables
    private val currentExpression =
        StringBuilder("0") // Builds the expression with spaces, shown in text_result
    private var isResultDisplayed = false // Flag to handle behavior after pressing "="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set click listeners for digits
        val digits = listOf(
            binding.buttonDigit0, binding.buttonDigit1, binding.buttonDigit2, binding.buttonDigit3,
            binding.buttonDigit4, binding.buttonDigit5, binding.buttonDigit6, binding.buttonDigit7,
            binding.buttonDigit8, binding.buttonDigit9
        )
        digits.forEach { button ->
            button.setOnClickListener {
                appendDigit(button.text.toString())
            }
        }

        // Set click listeners for basic operators (+, -, x, /)
        val operators = listOf(
            binding.buttonOperationAddition, binding.buttonOperationSubtraction,
            binding.buttonOperationMultiplication, binding.buttonOperationDivision
        )
        operators.forEach { button ->
            button.setOnClickListener {
                appendOperator(button.text.toString())
            }
        }

        // Special buttons
        binding.buttonClear.setOnClickListener { clear() }
        binding.buttonDot.setOnClickListener { appendDot() }
        binding.buttonPositiveNegative.setOnClickListener { toggleSign() }
        binding.buttonHundredPercent.setOnClickListener { applyPercent() }
        binding.buttonEqual.setOnClickListener { calculate() }

        // Initial update
        updateDisplay()
    }

    /**
     * Appends a digit to the current expression.
     * If a result is displayed, starts a new expression.
     * Handles replacing initial "0" or appending after operators.
     */
    private fun appendDigit(digit: String) {
        if (isResultDisplayed) {
            currentExpression.setLength(0)
            currentExpression.append(digit)
            isResultDisplayed = false
        } else {
            if (currentExpression.toString() == "0") {
                currentExpression.setLength(0)
                currentExpression.append(digit)
            } else {
                currentExpression.append(digit)
            }
        }
        updateDisplay()
    }

    /**
     * Appends an operator with spaces (" + ").
     * If after a result, uses the result as the starting number.
     * Replaces the last operator if pressed consecutively.
     */
    private fun appendOperator(op: String) {
        if (isResultDisplayed) {
            isResultDisplayed = false
        }
        if (currentExpression.isEmpty()) return

        val current = currentExpression.toString()
        if (current.endsWith(" ")) {
            // Replace last operator (remove last 3 chars: space + op + space)
            currentExpression.setLength(currentExpression.length - 3)
            currentExpression.append(" $op ")
        } else {
            currentExpression.append(" $op ")
        }
        updateDisplay()
    }

    /**
     * Appends a decimal point if not already in the current number.
     * Starts with "0." if after operator or initial.
     */
    private fun appendDot() {
        if (isResultDisplayed) {
            currentExpression.setLength(0)
            currentExpression.append("0.")
            isResultDisplayed = false
            updateDisplay()
            return
        }

        val current = currentExpression.toString()
        val lastToken = if (current.contains(" ")) current.substringAfterLast(" ") else current
        if (lastToken.contains(".")) return

        if (current.endsWith(" ") || current.isEmpty()) {
            currentExpression.append("0.")
        } else if (current == "0") {
            currentExpression.setLength(0)
            currentExpression.append("0.")
        } else {
            currentExpression.append(".")
        }
        updateDisplay()
    }

    /**
     * Toggles the sign of the current number (last token).
     * If after an operator, starts a negative number.
     */
    private fun toggleSign() {
        if (currentExpression.isEmpty()) {
            currentExpression.append("-")
            updateDisplay()
            return
        }

        val current = currentExpression.toString()
        if (current.endsWith(" ")) {
            currentExpression.append("-")
            updateDisplay()
            return
        }

        val lastSpace = current.lastIndexOf(" ")
        val lastTokenStart = if (lastSpace == -1) 0 else lastSpace + 1
        val lastToken = current.substring(lastTokenStart)
        val newToken = if (lastToken.startsWith("-")) lastToken.substring(1) else "-$lastToken"

        currentExpression.setLength(lastTokenStart)
        currentExpression.append(newToken)
        updateDisplay()
    }

    /**
     * Applies % to the current number (divides by 100).
     * Ignores if no current number.
     */
    private fun applyPercent() {
        val current = currentExpression.toString()
        if (current.isEmpty() || current.endsWith(" ")) return

        val lastSpace = current.lastIndexOf(" ")
        val lastTokenStart = if (lastSpace == -1) 0 else lastSpace + 1
        val lastToken = current.substring(lastTokenStart)

        try {
            val num = lastToken.toDouble() / 100.0
            currentExpression.setLength(lastTokenStart)
            currentExpression.append(num.toString())
            updateDisplay()
        } catch (e: NumberFormatException) {
            // Ignore invalid number
        }
    }

    /**
     * Evaluates the expression on "=".
     * Moves expression to text_last_operation, shows result in text_result.
     * Uses a custom parser to handle operator precedence and errors.
     */
    private fun calculate() {
        val current = currentExpression.toString()
        if (current.isEmpty() || current.endsWith(" ") || current == "0") {
            Toast.makeText(this, "Incomplete expression", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val result = evaluateExpression(current)
            if (result.isInfinite() || result.isNaN()) {
                Toast.makeText(this, "Cannot divide by zero", Toast.LENGTH_SHORT).show()
                return
            }

            // Format result: integer if no decimal, else float
            val resultStr = if (result % 1 == 0.0) {
                result.toLong().toString()
            } else {
                result.toString()
            }

            binding.textLastOperation.text = current
            currentExpression.setLength(0)
            currentExpression.append(resultStr)
            isResultDisplayed = true
            updateDisplay()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid expression", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Evaluates the expression using Shunting Yard algorithm to convert infix to postfix (RPN) and evaluate.
     * Handles +, -, x, / with correct precedence, supports decimals and negatives.
     */
    private fun evaluateExpression(expression: String): Double {
        // Tokenize: split by spaces, replace 'x' with '*' and '÷' with '/' (adjust based on strings.xml)
        val tokens = expression.split(" ").map {
            it.replace("x", "*").replace("×", "*").replace("÷", "/")
        }

        // Operator precedence: higher number means higher precedence
        val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)
        val outputQueue = LinkedList<String>() // Postfix output (e.g., "5 10 2 * +")
        val operatorStack = Stack<String>() // Temporary stack for operators

        // Shunting Yard: Convert infix to postfix
        for (token in tokens) {
            when {
                // Number (including negative numbers)
                token.toDoubleOrNull() != null || token.startsWith("-") && token.substring(1).toDoubleOrNull() != null -> {
                    outputQueue.add(token)
                }
                // Operator
                token in precedence -> {
                    while (operatorStack.isNotEmpty() && precedence[operatorStack.peek()] ?: 0 >= precedence[token]!!) {
                        outputQueue.add(operatorStack.pop())
                    }
                    operatorStack.push(token)
                }
                else -> throw IllegalArgumentException("Invalid token: $token")
            }
        }
        // Add remaining operators to output
        while (operatorStack.isNotEmpty()) {
            outputQueue.add(operatorStack.pop())
        }

        // Evaluate postfix (RPN) using a stack
        val evalStack = Stack<Double>()
        for (token in outputQueue) {
            when (token) {
                "+", "-", "*", "/" -> {
                    if (evalStack.size < 2) throw IllegalArgumentException("Invalid expression: not enough operands")
                    val b = evalStack.pop() // Second operand
                    val a = evalStack.pop() // First operand
                    when (token) {
                        "+" -> evalStack.push(a + b)
                        "-" -> evalStack.push(a - b)
                        "*" -> evalStack.push(a * b)
                        "/" -> {
                            if (b == 0.0) throw ArithmeticException("Division by zero")
                            evalStack.push(a / b)
                        }
                    }
                }
                else -> {
                    try {
                        evalStack.push(token.toDouble())
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException("Invalid number: $token")
                    }
                }
            }
        }

        if (evalStack.size != 1) throw IllegalArgumentException("Invalid expression: too many operands")
        return evalStack.pop()
    }

    /**
     * Clears everything to initial state.
     */
    private fun clear() {
        currentExpression.setLength(0)
        currentExpression.append("0")
        binding.textLastOperation.text = ""
        isResultDisplayed = false
        updateDisplay()
    }

    /**
     * Updates the text_result with the current expression.
     */
    private fun updateDisplay() {
        binding.textResult.text = currentExpression.toString()
    }
}