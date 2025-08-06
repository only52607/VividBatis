package com.github.only52607.vividbatis.language

import com.intellij.psi.tree.IElementType

/**
 * OGNL token types for syntax highlighting
 */
object OgnlTokenTypes {
    // Literals
    @JvmField val STRING_LITERAL = IElementType("OGNL_STRING_LITERAL", OgnlLanguage)
    @JvmField val NUMBER_LITERAL = IElementType("OGNL_NUMBER_LITERAL", OgnlLanguage)
    @JvmField val BOOLEAN_LITERAL = IElementType("OGNL_BOOLEAN_LITERAL", OgnlLanguage)
    @JvmField val NULL_LITERAL = IElementType("OGNL_NULL_LITERAL", OgnlLanguage)
    
    // Identifiers
    @JvmField val IDENTIFIER = IElementType("OGNL_IDENTIFIER", OgnlLanguage)
    
    // Keywords
    @JvmField val KEYWORD = IElementType("OGNL_KEYWORD", OgnlLanguage)
    
    // Operators
    @JvmField val EQ_OP = IElementType("OGNL_EQ_OP", OgnlLanguage)
    @JvmField val NE_OP = IElementType("OGNL_NE_OP", OgnlLanguage)
    @JvmField val LT_OP = IElementType("OGNL_LT_OP", OgnlLanguage)
    @JvmField val LE_OP = IElementType("OGNL_LE_OP", OgnlLanguage)
    @JvmField val GT_OP = IElementType("OGNL_GT_OP", OgnlLanguage)
    @JvmField val GE_OP = IElementType("OGNL_GE_OP", OgnlLanguage)
    @JvmField val PLUS_OP = IElementType("OGNL_PLUS_OP", OgnlLanguage)
    @JvmField val MINUS_OP = IElementType("OGNL_MINUS_OP", OgnlLanguage)
    @JvmField val MULTIPLY_OP = IElementType("OGNL_MULTIPLY_OP", OgnlLanguage)
    @JvmField val DIVIDE_OP = IElementType("OGNL_DIVIDE_OP", OgnlLanguage)
    @JvmField val MODULO_OP = IElementType("OGNL_MODULO_OP", OgnlLanguage)
    @JvmField val ASSIGN_OP = IElementType("OGNL_ASSIGN_OP", OgnlLanguage)
    @JvmField val NOT_OP = IElementType("OGNL_NOT_OP", OgnlLanguage)
    @JvmField val QUESTION_OP = IElementType("OGNL_QUESTION_OP", OgnlLanguage)
    @JvmField val COLON_OP = IElementType("OGNL_COLON_OP", OgnlLanguage)
    @JvmField val AT_OP = IElementType("OGNL_AT_OP", OgnlLanguage)
    @JvmField val HASH_OP = IElementType("OGNL_HASH_OP", OgnlLanguage)
    
    // Punctuation
    @JvmField val LEFT_PAREN = IElementType("OGNL_LEFT_PAREN", OgnlLanguage)
    @JvmField val RIGHT_PAREN = IElementType("OGNL_RIGHT_PAREN", OgnlLanguage)
    @JvmField val LEFT_BRACKET = IElementType("OGNL_LEFT_BRACKET", OgnlLanguage)
    @JvmField val RIGHT_BRACKET = IElementType("OGNL_RIGHT_BRACKET", OgnlLanguage)
    @JvmField val LEFT_BRACE = IElementType("OGNL_LEFT_BRACE", OgnlLanguage)
    @JvmField val RIGHT_BRACE = IElementType("OGNL_RIGHT_BRACE", OgnlLanguage)
    @JvmField val DOT = IElementType("OGNL_DOT", OgnlLanguage)
    @JvmField val COMMA = IElementType("OGNL_COMMA", OgnlLanguage)
    
    // Special
    @JvmField val WHITE_SPACE = IElementType("OGNL_WHITE_SPACE", OgnlLanguage)
    @JvmField val BAD_CHARACTER = IElementType("OGNL_BAD_CHARACTER", OgnlLanguage)
}