package com.github.only52607.vividbatis.language

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Parser definition for OGNL language
 */
class OgnlParserDefinition : ParserDefinition {
    
    companion object {
        val FILE = IFileElementType(OgnlLanguage)
        val WHITE_SPACES = TokenSet.create(OgnlTokenTypes.WHITE_SPACE)
        val COMMENTS = TokenSet.EMPTY
        val STRING_LITERALS = TokenSet.create(OgnlTokenTypes.STRING_LITERAL)
    }
    
    override fun createLexer(project: Project?): Lexer = OgnlLexer()
    
    override fun createParser(project: Project?): PsiParser = OgnlParser()
    
    override fun getFileNodeType(): IFileElementType = FILE
    
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    
    override fun getCommentTokens(): TokenSet = COMMENTS
    
    override fun getStringLiteralElements(): TokenSet = STRING_LITERALS
    
    override fun createElement(node: ASTNode): PsiElement = OgnlPsiElement(node)
    
    override fun createFile(viewProvider: FileViewProvider): PsiFile = OgnlFile(viewProvider)
}