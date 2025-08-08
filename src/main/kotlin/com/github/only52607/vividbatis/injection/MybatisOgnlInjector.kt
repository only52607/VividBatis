package com.github.only52607.vividbatis.injection

import com.github.only52607.vividbatis.language.OgnlLanguage
import com.github.only52607.vividbatis.util.isInMybatisMapperFile
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag

/**
 * Injector for OGNL language into MyBatis XML attributes
 */
class MybatisOgnlInjector : MultiHostInjector {
    
    companion object {
        // MyBatis attributes that contain OGNL expressions
        private val OGNL_ATTRIBUTES = setOf(
            "test",      // if, when tags
            "collection", // foreach tag
            "value",     // bind tag
            "name"       // bind tag (when it's an expression)
        )
        
        // MyBatis tags that support OGNL expressions
        private val OGNL_SUPPORTING_TAGS = setOf(
            "if", "when", "foreach", "bind"
        )
    }
    
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is XmlAttributeValue) return
        
        // XmlAttributeValue implements PsiLanguageInjectionHost
        val injectionHost = context as? PsiLanguageInjectionHost ?: return
        
        val xmlAttribute = context.parent as? XmlAttribute ?: return
        val attributeName = xmlAttribute.name
        val xmlTag = xmlAttribute.parent as? XmlTag ?: return
        
        // Check if this is a MyBatis mapper file
        if (!xmlTag.isInMybatisMapperFile()) return
        
        // Check if the tag supports OGNL and the attribute is an OGNL attribute
        if (xmlTag.name in OGNL_SUPPORTING_TAGS && attributeName in OGNL_ATTRIBUTES) {
            val valueText = context.value
            if (valueText.isNotBlank()) {
                // Get the text range inside the attribute value, excluding quotes
                val textRange = getValueTextRange(context)
                if (textRange != null) {
                    registrar.startInjecting(OgnlLanguage)
                        .addPlace(null, null, injectionHost, textRange)
                        .doneInjecting()
                }
            }
        }
    }
    
    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(XmlAttributeValue::class.java)
    }
    
    /**
     * Get the text range for the value content, excluding quotes
     */
    private fun getValueTextRange(attributeValue: XmlAttributeValue): TextRange? {
        val text = attributeValue.text
        if (text.length < 2) return null
        
        // Check if the value is quoted
        val startChar = text.first()
        val endChar = text.last()
        
        return if ((startChar == '"' && endChar == '"') || (startChar == '\'' && endChar == '\'')) {
            // Exclude quotes: start from 1, end at length-1
            TextRange.create(1, text.length - 1)
        } else {
            // No quotes, use entire range
            TextRange.create(0, text.length)
        }
    }
}