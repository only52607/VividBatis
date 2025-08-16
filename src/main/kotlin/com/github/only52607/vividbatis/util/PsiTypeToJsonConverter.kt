package com.github.only52607.vividbatis.util

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import java.util.UUID

object PsiTypeToJsonConverter {

    fun convert(psiType: PsiType?): JsonElement {
        return convert(psiType, mutableSetOf())
    }

    private fun convert(psiType: PsiType?, processedTypes: MutableSet<String>): JsonElement {
        if (psiType == null) return JsonNull.INSTANCE

        val qualifiedName = psiType.getCanonicalText(false)
        if (processedTypes.contains(qualifiedName)) {
            return JsonObject() // Prevent circular references
        }

        return when {
            psiType is PsiArrayType -> convertArray(psiType, processedTypes)
            isInheritor(psiType, CommonClassNames.JAVA_UTIL_COLLECTION) -> convertCollection(psiType, processedTypes)
            isInheritor(psiType, CommonClassNames.JAVA_UTIL_MAP) -> convertMap(psiType, processedTypes)
            else -> convertSimpleOrObject(psiType, processedTypes)
        }
    }

    private fun convertSimpleOrObject(psiType: PsiType, processedTypes: MutableSet<String>): JsonElement {
        return when (psiType.getCanonicalText(true)) {
            "boolean", CommonClassNames.JAVA_LANG_BOOLEAN -> JsonPrimitive(true)
            "byte", CommonClassNames.JAVA_LANG_BYTE -> JsonPrimitive(1)
            "short", CommonClassNames.JAVA_LANG_SHORT -> JsonPrimitive(1)
            "int", CommonClassNames.JAVA_LANG_INTEGER -> JsonPrimitive(1)
            "long", CommonClassNames.JAVA_LANG_LONG -> JsonPrimitive(1L)
            "float", CommonClassNames.JAVA_LANG_FLOAT -> JsonPrimitive(1.0f)
            "double", CommonClassNames.JAVA_LANG_DOUBLE -> JsonPrimitive(1.0)
            "char", CommonClassNames.JAVA_LANG_CHARACTER -> JsonPrimitive("c")
            CommonClassNames.JAVA_LANG_STRING -> JsonPrimitive("string")
            CommonClassNames.JAVA_UTIL_DATE -> JsonPrimitive("2025-08-16T12:00:00Z")
            ParamClassNames.JAVA_TIME_INSTANT -> JsonPrimitive("2025-08-16T12:00:00Z")
            ParamClassNames.JAVA_TIME_LOCAL_DATE -> JsonPrimitive("2025-08-16")
            ParamClassNames.JAVA_TIME_LOCAL_DATE_TIME -> JsonPrimitive("2025-08-16T12:00:00")
            ParamClassNames.JAVA_MATH_BIG_DECIMAL -> JsonPrimitive("123.45")
            ParamClassNames.JAVA_MATH_BIG_INTEGER -> JsonPrimitive("123")
            ParamClassNames.JAVA_UTIL_UUID -> JsonPrimitive(UUID.randomUUID().toString())
            else -> convertObject(psiType, processedTypes)
        }
    }

    private fun convertArray(arrayType: PsiArrayType, processedTypes: MutableSet<String>): JsonArray {
        val componentType = arrayType.componentType
        return JsonArray().apply { add(convert(componentType, processedTypes)) }
    }

    private fun convertCollection(collectionType: PsiType, processedTypes: MutableSet<String>): JsonArray {
        val elementType = PsiUtil.substituteTypeParameter(collectionType, CommonClassNames.JAVA_UTIL_COLLECTION, 0, false)
        return JsonArray().apply { add(convert(elementType, processedTypes)) }
    }

    private fun convertMap(mapType: PsiType, processedTypes: MutableSet<String>): JsonObject {
        val keyType = PsiUtil.substituteTypeParameter(mapType, CommonClassNames.JAVA_UTIL_MAP, 0, false)
        val valueType = PsiUtil.substituteTypeParameter(mapType, CommonClassNames.JAVA_UTIL_MAP, 1, false)

        val keyString = when {
            keyType == null -> "key"
            isInheritor(keyType, CommonClassNames.JAVA_LANG_STRING) -> "key"
            isInheritor(keyType, CommonClassNames.JAVA_LANG_NUMBER) -> "1"
            else -> "key"
        }

        return JsonObject().apply { add(keyString, convert(valueType, processedTypes)) }
    }

    private fun convertObject(psiType: PsiType, processedTypes: MutableSet<String>): JsonElement {
        val psiClass = PsiTypesUtil.getPsiClass(psiType) ?: return JsonNull.INSTANCE

        if (isInheritor(psiType, CommonClassNames.JAVA_LANG_ENUM)) {
            val firstEnumConstant = psiClass.fields.firstOrNull { it is com.intellij.psi.PsiEnumConstant }
            return JsonPrimitive(firstEnumConstant?.name ?: "")
        }

        val typeName = psiClass.qualifiedName ?: return JsonNull.INSTANCE
        processedTypes.add(typeName)

        return try {
            JsonObject().apply {
                psiClass.allFields
                    .filterNot { it.hasModifierProperty(PsiModifier.STATIC) || it.name.startsWith("$") }
                    .forEach { field ->
                        add(field.name, convert(field.type, processedTypes))
                    }
            }
        } finally {
            processedTypes.remove(typeName)
        }
    }

    private fun isInheritor(psiType: PsiType, baseClass: String): Boolean {
        val psiClass = PsiTypesUtil.getPsiClass(psiType) ?: return false
        return InheritanceUtil.isInheritor(psiClass, false, baseClass)
    }
}