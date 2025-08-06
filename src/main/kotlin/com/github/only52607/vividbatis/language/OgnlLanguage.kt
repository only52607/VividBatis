package com.github.only52607.vividbatis.language

import com.intellij.lang.Language

/**
 * OGNL (Object-Graph Navigation Language) language definition for MyBatis
 */
object OgnlLanguage : Language("OGNL", "application/x-ognl") {
    override fun getDisplayName(): String = "OGNL"
    
    override fun isCaseSensitive(): Boolean = true
}