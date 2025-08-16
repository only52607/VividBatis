package com.github.only52607.vividbatis.util

import com.google.gson.JsonElement
import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object OgnlObjectMapper {
    fun convertToOgnlObject(jsonElement: JsonElement, expectedType: PsiType?): Any? {
        return jsonElement.asOgnlObject(expectedType)
    }

    fun convertToOgnlMap(jsonElement: JsonElement, expectedType: PsiType?): Any {
        return jsonElement.asOgnlMap(expectedType)
    }

    private fun JsonElement.asOgnlObject(expectedType: PsiType? = null): Any? {
        if (this.isJsonNull) return null

        if (expectedType == null) {
            return when {
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

        return when (expectedType) {
            is PsiPrimitiveType -> toPsiPrimitive(expectedType)
            is PsiArrayType -> toPsiArray(expectedType)
            is PsiClassType -> toPsiClass(expectedType)
            else -> asOgnlObject()
        }
    }

    private fun JsonElement.toPsiPrimitive(type: PsiPrimitiveType): Any? {
        return when (type.canonicalText) {
            "int" -> asInt
            "long" -> asLong
            "float" -> asFloat
            "double" -> asDouble
            "boolean" -> asBoolean
            "char" -> asString.firstOrNull()
            "byte" -> asByte
            "short" -> asShort
            else -> null
        }
    }

    private fun JsonElement.toPsiArray(type: PsiArrayType): Any? {
        val componentType = type.componentType
        if (componentType is PsiPrimitiveType) {
            return when (componentType.canonicalText) {
                "int" -> asJsonArray.map { it.asInt }.toIntArray()
                "long" -> asJsonArray.map { it.asLong }.toLongArray()
                "float" -> asJsonArray.map { it.asFloat }.toFloatArray()
                "double" -> asJsonArray.map { it.asDouble }.toDoubleArray()
                "boolean" -> asJsonArray.map { it.asBoolean }.toBooleanArray()
                "char" -> asJsonArray.joinToString("") { it.asString }.toCharArray()
                "byte" -> asJsonArray.map { it.asByte }.toByteArray()
                "short" -> asJsonArray.map { it.asShort }.toShortArray()
                else -> asOgnlList(componentType).toTypedArray()
            }
        }
        return asOgnlList(componentType).toTypedArray()
    }

    private fun JsonElement.toPsiClass(type: PsiClassType): Any? {
        val firstParam = type.parameters.firstOrNull()
        when (type.getQualifiedName()) {
            CommonClassNames.JAVA_LANG_STRING -> return asString
            CommonClassNames.JAVA_LANG_CHARACTER -> return asString.firstOrNull()
            CommonClassNames.JAVA_LANG_INTEGER -> return asInt
            CommonClassNames.JAVA_LANG_LONG -> return asLong
            CommonClassNames.JAVA_LANG_FLOAT -> return asFloat
            CommonClassNames.JAVA_LANG_DOUBLE -> return asDouble
            CommonClassNames.JAVA_LANG_BOOLEAN -> return asBoolean
            CommonClassNames.JAVA_LANG_BYTE -> return asByte
            CommonClassNames.JAVA_LANG_SHORT -> return asShort
            ParamClassNames.JAVA_MATH_BIG_DECIMAL -> return BigDecimal(asString)
            ParamClassNames.JAVA_MATH_BIG_INTEGER -> return BigInteger(asString)
            CommonClassNames.JAVA_UTIL_DATE -> return Date.from(Instant.parse(asString))
            ParamClassNames.JAVA_TIME_LOCAL_DATE -> return LocalDate.parse(asString, DateTimeFormatter.ISO_LOCAL_DATE)
            ParamClassNames.JAVA_TIME_LOCAL_DATE_TIME -> return LocalDateTime.parse(asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ParamClassNames.JAVA_TIME_INSTANT -> return Instant.parse(asString)
            ParamClassNames.JAVA_UTIL_UUID -> return UUID.fromString(asString)

            CommonClassNames.JAVA_UTIL_LIST,
            CommonClassNames.JAVA_UTIL_ARRAY_LIST,
            ParamClassNames.JAVA_UTIL_LINKED_LIST,
            ParamClassNames.JAVA_UTIL_QUEUE -> return asOgnlList(firstParam)

            CommonClassNames.JAVA_UTIL_SET,
            CommonClassNames.JAVA_UTIL_HASH_SET,
            ParamClassNames.JAVA_UTIL_LINKED_HASH_SET,
            ParamClassNames.JAVA_UTIL_SORTED_SET,
            ParamClassNames.JAVA_UTIL_TREE_SET -> return asOgnlSet(firstParam)

            CommonClassNames.JAVA_UTIL_MAP,
            CommonClassNames.JAVA_UTIL_HASH_MAP,
            ParamClassNames.JAVA_UTIL_LINKED_HASH_MAP -> return asOgnlMap(type.parameters.getOrNull(1))
        }

        if (InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_LANG_ENUM)) {
            val enumClass = try {
                Class.forName(type.getQualifiedName())
            } catch (e: ClassNotFoundException) {
                return null
            }
            if (enumClass.isEnum) {
                return enumClass.enumConstants.firstOrNull { (it as Enum<*>).name == asString }
            }
        }

        return asOgnlMap(null) // Fallback for custom POJOs
    }

    private fun JsonElement.asOgnlList(expectedType: PsiType? = null): List<Any?> = asJsonArray.map { it.asOgnlObject(expectedType) }

    private fun JsonElement.asOgnlSet(expectedType: PsiType? = null): Set<Any?> = asOgnlList(expectedType).toSet()

    private fun JsonElement.asOgnlMap(expectedType: PsiType? = null): Map<String, Any?> {
        val valueType = if (expectedType is PsiClassType) expectedType.parameters.getOrNull(1) else null
        return asJsonObject.entrySet().associate { (key, value) ->
            val fieldType = (expectedType as? PsiClassType)?.resolve()?.findFieldByName(key, true)?.type
            key to value.asOgnlObject(fieldType ?: valueType)
        }
    }

    private fun PsiClassType.getQualifiedName(): String? = resolve()?.qualifiedName
}