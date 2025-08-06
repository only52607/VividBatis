package com.github.only52607.vividbatis.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

class JavaClassAnalyzer {
    
    private val processedClasses = mutableSetOf<String>()
    
    fun analyzeClass(psiClass: PsiClass): JsonObject {
        processedClasses.clear()
        return analyzeClassInternal(psiClass)
    }
    
    private fun analyzeClassInternal(psiClass: PsiClass): JsonObject {
        val jsonObject = JsonObject()
        val className = psiClass.qualifiedName ?: psiClass.name ?: return jsonObject
        
        if (className in processedClasses) {
            return jsonObject
        }
        processedClasses.add(className)
        
        val allFields = psiClass.allFields
        for (field in allFields) {
            if (field.hasModifierProperty("static") || 
                field.hasModifierProperty("final") ||
                field.name.startsWith("this$")) {
                continue
            }
            
            val fieldName = field.name
            val fieldType = field.type
            val defaultValue = generateDefaultValue(fieldType)
            
            jsonObject.add(fieldName, defaultValue)
        }
        
        return jsonObject
    }
    
    private fun generateDefaultValue(type: PsiType): JsonPrimitive {
        val typeName = type.canonicalText
        
        return when {
            typeName == "java.lang.String" -> JsonPrimitive("示例字符串")
            typeName == "int" || typeName == "java.lang.Integer" -> JsonPrimitive(1)
            typeName == "long" || typeName == "java.lang.Long" -> JsonPrimitive(1L)
            typeName == "double" || typeName == "java.lang.Double" -> JsonPrimitive(1.0)
            typeName == "float" || typeName == "java.lang.Float" -> JsonPrimitive(1.0f)
            typeName == "boolean" || typeName == "java.lang.Boolean" -> JsonPrimitive(true)
            typeName == "byte" || typeName == "java.lang.Byte" -> JsonPrimitive(1)
            typeName == "short" || typeName == "java.lang.Short" -> JsonPrimitive(1)
            typeName == "char" || typeName == "java.lang.Character" -> JsonPrimitive("A")
            typeName.startsWith("java.util.Date") -> JsonPrimitive("2024-01-01 12:00:00")
            typeName.startsWith("java.time.LocalDateTime") -> JsonPrimitive("2024-01-01T12:00:00")
            typeName.startsWith("java.time.LocalDate") -> JsonPrimitive("2024-01-01")
            typeName.startsWith("java.time.LocalTime") -> JsonPrimitive("12:00:00")
            typeName.startsWith("java.math.BigDecimal") -> JsonPrimitive(100.00)
            else -> JsonPrimitive("示例值")
        }
    }
} 