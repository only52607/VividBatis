package com.github.only52607.vividbatis.language

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * PSI file for OGNL
 */
class OgnlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, OgnlLanguage) {
    
    override fun getFileType(): FileType = OgnlFileType
    
    override fun toString(): String = "OGNL File"
}