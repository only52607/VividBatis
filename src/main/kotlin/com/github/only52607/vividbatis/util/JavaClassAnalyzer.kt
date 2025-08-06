package com.github.only52607.vividbatis.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.google.gson.JsonObject

class JavaClassAnalyzer {
    
    private val processedClasses = mutableSetOf<String>()
    
    fun analyzeClass(psiClass: PsiClass): JsonObject {
        processedClasses.clear()
        return analyzeClassInternal(psiClass)
    }
    
    private fun analyzeClassInternal(psiClass: PsiClass): JsonObject {
        val jsonObject = JsonObject()
        val className = psiClass.qualifiedName ?: psiClass.name ?: return jsonObject
        
        if (className in processedClasses) return jsonObject
        processedClasses.add(className)
        
        psiClass.allFields.forEach { field ->
            if (!field.hasModifierProperty("static") && 
                !field.hasModifierProperty("final") &&
                !field.name.startsWith("this$")) {
                
                val fieldName = field.name
                val fieldType = field.type
                val defaultValue = TypeUtils.generateDefaultValue(fieldType.canonicalText)
                
                jsonObject.add(fieldName, defaultValue)
            }
        }
        
        return jsonObject
    }
} 