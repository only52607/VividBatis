package com.github.only52607.vividbatis.util

import com.google.gson.JsonElement
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType

object OgnlObjectMapper {
    fun convertToOgnlObject(jsonElement: JsonElement, expectedType: PsiType?): Any? {
        return jsonElement.asOgnlObject(expectedType)
    }

    fun convertToOgnlMap(jsonElement: JsonElement, expectedType: PsiType?): Any {
        return jsonElement.asOgnlMap(expectedType)
    }

    fun JsonElement.asOgnlObject(expectedType: PsiType? = null): Any? {
        if (expectedType == null) {
            return when {
                isJsonNull -> null
                isJsonArray -> asOgnlList()
                isJsonObject -> asOgnlMap()
                isJsonPrimitive -> {
                    val primitive = asJsonPrimitive
                    when {
                        primitive.isString -> primitive.asString
                        primitive.isNumber -> primitive.asNumber
                        primitive.isBoolean -> primitive.asBoolean
                        else -> primitive.asString
                    }
                }

                else -> toString()
            }
        }
        when (expectedType) {
            is PsiPrimitiveType -> {
                return when (expectedType.canonicalText) {
                    "int" -> asInt
                    "char" -> return asString.toCharArray()[0]
                    "long" -> asLong
                    "float" -> asFloat
                    "double" -> asDouble
                    "byte" -> asByte
                    "short" -> asShort
                    "boolean" -> asBoolean
                    else -> null // Unsupported primitive type
                }
            }

            is PsiArrayType -> {
                return asOgnlArray(expectedType.componentType)
            }

            is PsiClassType -> {
                when(expectedType.className) {
                    CommonClassNames.JAVA_LANG_STRING -> return asString
                    CommonClassNames.JAVA_LANG_CHARACTER -> return asString.toCharArray()[0]
                    CommonClassNames.JAVA_LANG_INTEGER -> return asInt
                    CommonClassNames.JAVA_LANG_LONG -> return asLong
                    CommonClassNames.JAVA_LANG_FLOAT -> return asFloat
                    CommonClassNames.JAVA_LANG_DOUBLE -> return asDouble
                    CommonClassNames.JAVA_LANG_BYTE -> return asByte
                    CommonClassNames.JAVA_LANG_SHORT -> return asShort
                    CommonClassNames.JAVA_LANG_BOOLEAN -> return asBoolean
                    CommonClassNames.JAVA_UTIL_LIST, CommonClassNames.JAVA_UTIL_ARRAY_LIST -> return asOgnlList(expectedType.parameters.firstOrNull())
                    CommonClassNames.JAVA_UTIL_SET, CommonClassNames.JAVA_UTIL_HASH_SET -> return asOgnlSet(expectedType.parameters.firstOrNull())
                    CommonClassNames.JAVA_UTIL_MAP, CommonClassNames.JAVA_UTIL_HASH_MAP -> return asOgnlMap(expectedType.parameters.getOrNull(1))
                }
            }
        }

        return asOgnlObject()
    }

    fun JsonElement.asOgnlList(expectedType: PsiType? = null) =
        asJsonArray.map { it.asOgnlObject(expectedType) }

    fun JsonElement.asOgnlArray(expectedType: PsiType? = null) =
        asOgnlList(expectedType).toTypedArray()

    fun JsonElement.asOgnlSet(expectedType: PsiType? = null) =
        asOgnlList(expectedType).toSet()

    fun JsonElement.asOgnlMap(expectedType: PsiType? = null) =
        asJsonObject.entrySet().associate { entry -> entry.key to entry.value.asOgnlObject(expectedType) }
}