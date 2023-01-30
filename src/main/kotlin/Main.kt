/**
 * @author Kacper Piasta, 249105
 *
 */

import java.io.Closeable
import java.util.Scanner

class ArithmeticExpressionSyntaxAnalyser(expression: String) : Closeable {
    private companion object {
        val digits = (0..9).joinToString().toCharArray()
        val operators = "+-:*^".toCharArray()

        const val epsilon = 'ε'
        const val dot = '.'
        const val semicolon = ';'
        const val leftParenthesis = '('
        const val rightParenthesis = ')'

        private fun Char.isEpsilon() = this == epsilon
        private fun Char.isDot() = this == dot
        private fun Char.isSemicolon() = this == semicolon
        private fun Char.isLeftParenthesis() = this == leftParenthesis
        private fun Char.isRightParenthesis() = this == rightParenthesis
        private fun CharArray.current() = last()
        private fun CharIterator.nextSymbol() = takeIf { it.hasNext() }?.nextChar() ?: epsilon
    }

    private val _iterator = expression.iterator()
    private var _readChars = charArrayOf()
    private var _valid = false

    /**
     * Wyświetla rezultat analizy składniowej
     */
    override fun close() {
        fun Boolean.status() = if (this) "valid" else "invalid"
        println("Read expression: ${_readChars.concatToString()}")
        println("Arithmetic expression is ${_valid.status()}")
    }

    /**
     * Bootloader
     */
    fun read() {
        readNextChar()
        _valid = readRoot()
    }

    /**
     * Analizuje korzeń pod kątem poprawności składni
     * Wczytuje wnętrze korzenia, a nastepnie środnik, opcjonalnie rekuruje
     */
    private fun readRoot(): Boolean {
        return readRootInternal() and when (_readChars.current().isSemicolon()) {
            true -> when (!readNextChar().isEpsilon()) {
                true -> readRoot()
                else -> true
            }
            else -> throwError(semicolon)
        }
    }

    /**
     * Dokonuje wewnętrznej analizy korzenia
     */
    private fun readRootInternal() = when (_readChars.current()) {
        in digits + leftParenthesis -> readLevel1ChildLeft()
        in operators -> readLevel1ChildRight()
        else -> throwError(operators + digits + leftParenthesis)
    }

    /**
     * Wczytuje lewą stronę pierwszego poziomu (cyfra i lewy nawias)
     */
    private fun readLevel1ChildLeft(): Boolean = with(_readChars.current()) {
        return when {
            isLeftParenthesis() -> readLevel2ChildLeft()
            this in digits -> readLevel2ChildRight()
            else -> throwError(digits + leftParenthesis)
        }
    }

    /**
     * Wczytuje prawą stronę pierwszego poziomu (operatory)
     */
    private fun readLevel1ChildRight() = when (_readChars.current()) {
        in operators -> readOperators()
        else -> throwError(operators)
    }

    /**
     * Wczytuje lewą stronę drugiego poziomu (nawiasy i korzeń)
     * W przypadku gdy ostatni znak poziomu nie jest prawnym nawiasem, wraca do poziomu nadrzędnego
     */
    private fun readLevel2ChildLeft() = when (_readChars.current().isLeftParenthesis()) {
        true -> when (!readNextChar().isRightParenthesis()) {
            true -> readRoot()
            false -> true
        }
        else -> throwError(leftParenthesis)
    }

    /**
     * Wczytuje prawą stronę drugiego poziomu (cyfry i opcjonalna kropka)
     */
    private fun readLevel2ChildRight() = when (_readChars.current()) {
        in digits -> readDigits() and readDot()
        else -> throwError(digits)
    }

    /**
     * Wczytuje operatory
     */
    private fun readOperators() = when (_readChars.current()) {
        in operators -> {
            readNextChar()
            true
        }
        else -> throwError(operators)
    }

    /**
     * Wczytuje cyfry
     * W przypadku napotkania kolejnego znaku będącego ponownie cyfrą, rekuruje
     */
    private fun readDigits(): Boolean {
        return when (_readChars.current()) {
            in digits -> when (readNextChar()) {
                in digits -> readDigits()
                else -> true
            }
            else -> throwError(digits)
        }
    }

    /**
     * Wczytuje kropkę
     * @param optional czy opcjonalnie, domyślnie true
     */
    private fun readDot(optional: Boolean = true) = when (_readChars.current().isDot()) {
        true -> {
            readNextChar()
            true
        }
        else -> optional.takeIf { !it }
            ?.let { throwError(dot) }
            ?: true
    }

    /**
     * Wczytuje kolejny znak i dodaje go do tablicy
     */
    private fun readNextChar() = _iterator.nextSymbol().apply {
        _readChars += this
    }

    private fun throwError(expected: Char): Nothing = throw SyntaxException(_readChars.current(), charArrayOf(expected))
    private fun throwError(expected: CharArray): Nothing = throw SyntaxException(_readChars.current(), expected)

    private class SyntaxException(provided: Char, expected: CharArray) :
        Exception("Error: provided $provided, expected one of [${expected.joinToString()}]")
}

/**
 * Wczytuje wyrażenie arytmetyczne od użytkownika i przekazuje je do analizy składniowej
 * W przypadku błędu wyświetla komunikat o niepoprawnym symbolu i oczekiwanych znakach
 */
fun main() = Scanner(System.`in`).use { it ->
    while (true) {
        print("Enter a string to check if it is a valid arithmetic expression: ")
        val input = it.nextLine()
        ArithmeticExpressionSyntaxAnalyser(input).use { analyser ->
            runCatching {
                analyser.read()
            }.onFailure {
                println(it.message)
            }
        }
    }
}
