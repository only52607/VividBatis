package com.github.only52607.vividbatis.language

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Simple parser for OGNL expressions
 */
class OgnlParser : PsiParser {
    
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val marker = builder.mark()
        
        // Simple parsing - just consume all tokens as one expression
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        
        marker.done(root)
        return builder.treeBuilt
    }
}