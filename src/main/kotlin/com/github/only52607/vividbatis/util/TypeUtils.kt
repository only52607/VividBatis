package com.github.only52607.vividbatis.util

import com.google.gson.JsonPrimitive

object TypeUtils {
    fun generateDefaultValue(typeName: String): JsonPrimitive {
        return when {
            typeName == "string" || typeName == "java.lang.String" -> JsonPrimitive("示例字符串")
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
            typeName.startsWith("java.math.BigInteger") -> JsonPrimitive(100)
            typeName.startsWith("java.sql.Timestamp") -> JsonPrimitive("2024-01-01 12:00:00.000")
            typeName.startsWith("java.sql.Date") -> JsonPrimitive("2024-01-01")
            typeName.startsWith("java.sql.Time") -> JsonPrimitive("12:00:00")
            else -> JsonPrimitive("示例值")
        }
    }

}