package com.github.only52607.vividbatis.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.google.gson.JsonArray
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
        
        if (className in processedClasses) return jsonObject
        processedClasses.add(className)
        
        psiClass.allFields.forEach { field ->
            if (!field.hasModifierProperty("static") && 
                !field.hasModifierProperty("final") &&
                !field.name.startsWith("this$")) {
                
                val fieldName = field.name
                val fieldType = field.type
                val defaultValue = generateFieldValue(fieldType)
                
                jsonObject.add(fieldName, defaultValue)
            }
        }
        
        return jsonObject
    }
    
    private fun generateFieldValue(fieldType: PsiType): com.google.gson.JsonElement {
        val canonicalText = fieldType.canonicalText
        
        return when {
            canonicalText.startsWith("java.util.List<") || canonicalText.startsWith("java.util.ArrayList<") -> {
                val jsonArray = JsonArray()
                val elementType = extractGenericType(canonicalText)
                if (elementType != null) {
                    val elementValue = TypeUtils.generateDefaultValue(elementType)
                    jsonArray.add(elementValue)
                }
                jsonArray
            }
            canonicalText.startsWith("java.util.Set<") || canonicalText.startsWith("java.util.HashSet<") -> {
                val jsonArray = JsonArray()
                val elementType = extractGenericType(canonicalText)
                if (elementType != null) {
                    val elementValue = TypeUtils.generateDefaultValue(elementType)
                    jsonArray.add(elementValue)
                }
                jsonArray
            }
            canonicalText.startsWith("java.util.Map<") || canonicalText.startsWith("java.util.HashMap<") -> {
                val jsonObject = JsonObject()
                val keyValueTypes = extractMapGenericTypes(canonicalText)
                if (keyValueTypes != null) {
                    val keyValue = TypeUtils.generateDefaultValue(keyValueTypes.first)
                    val valueValue = TypeUtils.generateDefaultValue(keyValueTypes.second)
                    jsonObject.add(keyValue.asString, valueValue)
                }
                jsonObject
            }
            canonicalText == "java.util.List" || canonicalText == "java.util.ArrayList" -> {
                val jsonArray = JsonArray()
                jsonArray.add(JsonPrimitive("示例元素"))
                jsonArray
            }
            canonicalText == "java.util.Set" || canonicalText == "java.util.HashSet" -> {
                val jsonArray = JsonArray()
                jsonArray.add(JsonPrimitive("示例元素"))
                jsonArray
            }
            canonicalText == "java.util.Map" || canonicalText == "java.util.HashMap" -> {
                val jsonObject = JsonObject()
                jsonObject.add("key", JsonPrimitive("value"))
                jsonObject
            }
            else -> TypeUtils.generateDefaultValue(canonicalText)
        }
    }
    
    private fun extractGenericType(typeString: String): String? {
        val startIndex = typeString.indexOf('<')
        val endIndex = typeString.lastIndexOf('>')
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return typeString.substring(startIndex + 1, endIndex).trim()
        }
        return null
    }
    
    private fun extractMapGenericTypes(typeString: String): Pair<String, String>? {
        val genericPart = extractGenericType(typeString) ?: return null
        val commaIndex = genericPart.indexOf(',')
        if (commaIndex != -1) {
            val keyType = genericPart.substring(0, commaIndex).trim()
            val valueType = genericPart.substring(commaIndex + 1).trim()
            return Pair(keyType, valueType)
        }
        return null
    }
} 