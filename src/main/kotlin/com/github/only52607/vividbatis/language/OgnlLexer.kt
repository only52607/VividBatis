package com.github.only52607.vividbatis.language

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Simple OGNL lexer for basic syntax highlighting
 */
class OgnlLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset: Int = 0
    private var endOffset: Int = 0
    private var currentOffset: Int = 0
    private var currentTokenType: IElementType? = null
    private var currentTokenStart: Int = 0
    
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentOffset = startOffset
        this.currentTokenStart = startOffset
        advance()
    }
    
    override fun getState(): Int = 0
    
    override fun getTokenType(): IElementType? = currentTokenType
    
    override fun getTokenStart(): Int = currentTokenStart
    
    override fun getTokenEnd(): Int = currentOffset
    
    override fun advance() {
        currentTokenStart = currentOffset
        
        if (currentOffset >= endOffset) {
            currentTokenType = null
            return
        }
        
        val ch = buffer[currentOffset]
        
        when {
            Character.isWhitespace(ch) -> {
                while (currentOffset < endOffset && Character.isWhitespace(buffer[currentOffset])) {
                    currentOffset++
                }
                currentTokenType = OgnlTokenTypes.WHITE_SPACE
            }
            Character.isLetter(ch) || ch == '_' -> {
                while (currentOffset < endOffset) {
                    val c = buffer[currentOffset]
                    if (!Character.isLetterOrDigit(c) && c != '_') break
                    currentOffset++
                }
                val tokenText = buffer.subSequence(currentTokenStart, currentOffset).toString()
                currentTokenType = when (tokenText) {
                    "true", "false" -> OgnlTokenTypes.BOOLEAN_LITERAL
                    "null" -> OgnlTokenTypes.NULL_LITERAL
                    "and", "or", "not", "in", "instanceof" -> OgnlTokenTypes.KEYWORD
                    else -> OgnlTokenTypes.IDENTIFIER
                }
            }
            Character.isDigit(ch) -> {
                while (currentOffset < endOffset && (Character.isDigit(buffer[currentOffset]) || buffer[currentOffset] == '.')) {
                    currentOffset++
                }
                currentTokenType = OgnlTokenTypes.NUMBER_LITERAL
            }
            ch == '"' || ch == '\'' -> {
                val quote = ch
                currentOffset++
                while (currentOffset < endOffset && buffer[currentOffset] != quote) {
                    if (buffer[currentOffset] == '\\') {
                        currentOffset++ // Skip escape character
                    }
                    currentOffset++
                }
                if (currentOffset < endOffset) currentOffset++ // Skip closing quote
                currentTokenType = OgnlTokenTypes.STRING_LITERAL
            }
            ch == '(' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.LEFT_PAREN
            }
            ch == ')' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.RIGHT_PAREN
            }
            ch == '[' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.LEFT_BRACKET
            }
            ch == ']' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.RIGHT_BRACKET
            }
            ch == '{' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.LEFT_BRACE
            }
            ch == '}' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.RIGHT_BRACE
            }
            ch == '.' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.DOT
            }
            ch == ',' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.COMMA
            }
            ch == '=' -> {
                currentOffset++
                if (currentOffset < endOffset && buffer[currentOffset] == '=') {
                    currentOffset++
                    currentTokenType = OgnlTokenTypes.EQ_OP
                } else {
                    currentTokenType = OgnlTokenTypes.ASSIGN_OP
                }
            }
            ch == '!' -> {
                currentOffset++
                if (currentOffset < endOffset && buffer[currentOffset] == '=') {
                    currentOffset++
                    currentTokenType = OgnlTokenTypes.NE_OP
                } else {
                    currentOffset--
                    currentOffset++
                    currentTokenType = OgnlTokenTypes.NOT_OP
                }
            }
            ch == '<' -> {
                currentOffset++
                if (currentOffset < endOffset && buffer[currentOffset] == '=') {
                    currentOffset++
                    currentTokenType = OgnlTokenTypes.LE_OP
                } else {
                    currentTokenType = OgnlTokenTypes.LT_OP
                }
            }
            ch == '>' -> {
                currentOffset++
                if (currentOffset < endOffset && buffer[currentOffset] == '=') {
                    currentOffset++
                    currentTokenType = OgnlTokenTypes.GE_OP
                } else {
                    currentTokenType = OgnlTokenTypes.GT_OP
                }
            }
            ch == '+' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.PLUS_OP
            }
            ch == '-' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.MINUS_OP
            }
            ch == '*' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.MULTIPLY_OP
            }
            ch == '/' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.DIVIDE_OP
            }
            ch == '%' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.MODULO_OP
            }
            ch == '?' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.QUESTION_OP
            }
            ch == ':' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.COLON_OP
            }
            ch == '@' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.AT_OP
            }
            ch == '#' -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.HASH_OP
            }
            else -> {
                currentOffset++
                currentTokenType = OgnlTokenTypes.BAD_CHARACTER
            }
        }
    }
    
    override fun getBufferSequence(): CharSequence = buffer
    
    override fun getBufferEnd(): Int = endOffset
}