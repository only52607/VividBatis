package com.github.only52607.vividbatis.language

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

/**
 * OGNL syntax highlighter
 */
class OgnlSyntaxHighlighter : SyntaxHighlighterBase() {
    
    companion object {
        // Define text attribute keys for different token types
        @JvmField val KEYWORD = TextAttributesKey.createTextAttributesKey("OGNL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmField val STRING = TextAttributesKey.createTextAttributesKey("OGNL_STRING", DefaultLanguageHighlighterColors.STRING)
        @JvmField val NUMBER = TextAttributesKey.createTextAttributesKey("OGNL_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        @JvmField val BOOLEAN = TextAttributesKey.createTextAttributesKey("OGNL_BOOLEAN", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmField val NULL = TextAttributesKey.createTextAttributesKey("OGNL_NULL", DefaultLanguageHighlighterColors.KEYWORD)
        @JvmField val IDENTIFIER = TextAttributesKey.createTextAttributesKey("OGNL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        @JvmField val OPERATOR = TextAttributesKey.createTextAttributesKey("OGNL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        @JvmField val PARENTHESES = TextAttributesKey.createTextAttributesKey("OGNL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
        @JvmField val BRACKETS = TextAttributesKey.createTextAttributesKey("OGNL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        @JvmField val BRACES = TextAttributesKey.createTextAttributesKey("OGNL_BRACES", DefaultLanguageHighlighterColors.BRACES)
        @JvmField val DOT = TextAttributesKey.createTextAttributesKey("OGNL_DOT", DefaultLanguageHighlighterColors.DOT)
        @JvmField val COMMA = TextAttributesKey.createTextAttributesKey("OGNL_COMMA", DefaultLanguageHighlighterColors.COMMA)
        @JvmField val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("OGNL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
        
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
    
    override fun getHighlightingLexer(): Lexer = OgnlLexer()
    
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            OgnlTokenTypes.KEYWORD -> arrayOf(KEYWORD)
            OgnlTokenTypes.STRING_LITERAL -> arrayOf(STRING)
            OgnlTokenTypes.NUMBER_LITERAL -> arrayOf(NUMBER)
            OgnlTokenTypes.BOOLEAN_LITERAL -> arrayOf(BOOLEAN)
            OgnlTokenTypes.NULL_LITERAL -> arrayOf(NULL)
            OgnlTokenTypes.IDENTIFIER -> arrayOf(IDENTIFIER)
            
            OgnlTokenTypes.EQ_OP, OgnlTokenTypes.NE_OP, OgnlTokenTypes.LT_OP, OgnlTokenTypes.LE_OP,
            OgnlTokenTypes.GT_OP, OgnlTokenTypes.GE_OP, OgnlTokenTypes.PLUS_OP, OgnlTokenTypes.MINUS_OP,
            OgnlTokenTypes.MULTIPLY_OP, OgnlTokenTypes.DIVIDE_OP, OgnlTokenTypes.MODULO_OP,
            OgnlTokenTypes.ASSIGN_OP, OgnlTokenTypes.NOT_OP, OgnlTokenTypes.QUESTION_OP,
            OgnlTokenTypes.COLON_OP, OgnlTokenTypes.AT_OP, OgnlTokenTypes.HASH_OP -> arrayOf(OPERATOR)
            
            OgnlTokenTypes.LEFT_PAREN, OgnlTokenTypes.RIGHT_PAREN -> arrayOf(PARENTHESES)
            OgnlTokenTypes.LEFT_BRACKET, OgnlTokenTypes.RIGHT_BRACKET -> arrayOf(BRACKETS)
            OgnlTokenTypes.LEFT_BRACE, OgnlTokenTypes.RIGHT_BRACE -> arrayOf(BRACES)
            OgnlTokenTypes.DOT -> arrayOf(DOT)
            OgnlTokenTypes.COMMA -> arrayOf(COMMA)
            OgnlTokenTypes.BAD_CHARACTER -> arrayOf(BAD_CHARACTER)
            
            else -> EMPTY_KEYS
        }
    }
}