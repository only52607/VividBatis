package com.github.only52607.vividbatis.language

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * OGNL file type definition
 */
object OgnlFileType : LanguageFileType(OgnlLanguage) {
    
    override fun getName(): String = "OGNL"
    
    override fun getDescription(): String = "OGNL Expression Language"
    
    override fun getDefaultExtension(): String = "ognl"
    
    override fun getIcon(): Icon? = null // Can add icon later if needed
}