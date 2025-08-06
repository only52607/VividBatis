package com.github.only52607.vividbatis.util

import com.google.gson.JsonPrimitive

object TypeUtils {
    private val PRIMITIVE_TYPES = setOf(
        "int", "java.lang.Integer", "long", "java.lang.Long",
        "double", "java.lang.Double", "float", "java.lang.Float",
        "boolean", "java.lang.Boolean", "byte", "java.lang.Byte",
        "short", "java.lang.Short", "char", "java.lang.Character",
        "java.lang.String"
    )

    fun isPrimitiveOrWrapper(type: String): Boolean = type in PRIMITIVE_TYPES

    fun generateDefaultValue(typeName: String): JsonPrimitive {
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

    fun generatePrimitiveJsonValue(type: String): String {
        return when (type) {
            "int", "java.lang.Integer" -> "1"
            "long", "java.lang.Long" -> "1"
            "double", "java.lang.Double" -> "1.0"
            "float", "java.lang.Float" -> "1.0"
            "boolean", "java.lang.Boolean" -> "true"
            "byte", "java.lang.Byte" -> "1"
            "short", "java.lang.Short" -> "1"
            "char", "java.lang.Character" -> "\"A\""
            "java.lang.String" -> "\"示例值\""
            else -> "\"示例值\""
        }
    }
} 