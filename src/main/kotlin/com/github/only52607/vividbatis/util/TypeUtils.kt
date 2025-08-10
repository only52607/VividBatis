package com.github.only52607.vividbatis.util

import com.google.gson.JsonPrimitive

object TypeUtils {
    private val PRIMITIVE_TYPES = setOf(
        "int", "java.lang.Integer", "long", "java.lang.Long",
        "double", "java.lang.Double", "float", "java.lang.Float",
        "boolean", "java.lang.Boolean", "byte", "java.lang.Byte",
        "short", "java.lang.Short", "char", "java.lang.Character",
        "string", "java.lang.String"
    )
    
    private val COLLECTION_TYPES = setOf(
        "java.util.List", "java.util.ArrayList", "java.util.LinkedList",
        "java.util.Set", "java.util.HashSet", "java.util.LinkedHashSet",
        "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap",
        "java.util.Collection"
    )

    fun isPrimitive(type: String): Boolean = type in PRIMITIVE_TYPES
    
    fun isCollectionType(type: String): Boolean {
        return type in COLLECTION_TYPES || 
               COLLECTION_TYPES.any { collectionType -> type.startsWith("$collectionType<") }
    }

    fun isMap(type: String): Boolean {
        return type == "java.util.Map" || type == "java.util.HashMap" ||
               type.startsWith("java.util.Map<") || type.startsWith("java.util.HashMap<")
    }

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
    
    fun getJavaClass(typeName: String): Class<*>? {
        return try {
            when (typeName) {
                "int" -> Int::class.java
                "java.lang.Integer" -> Integer::class.java
                "long" -> Long::class.java
                "java.lang.Long" -> java.lang.Long::class.java
                "double" -> Double::class.java
                "java.lang.Double" -> java.lang.Double::class.java
                "float" -> Float::class.java
                "java.lang.Float" -> java.lang.Float::class.java
                "boolean" -> Boolean::class.java
                "java.lang.Boolean" -> java.lang.Boolean::class.java
                "byte" -> Byte::class.java
                "java.lang.Byte" -> java.lang.Byte::class.java
                "short" -> Short::class.java
                "java.lang.Short" -> java.lang.Short::class.java
                "char" -> Char::class.java
                "java.lang.Character" -> Character::class.java
                "java.lang.String" -> String::class.java
                "java.util.List" -> List::class.java
                "java.util.ArrayList" -> ArrayList::class.java
                "java.util.Set" -> Set::class.java
                "java.util.HashSet" -> HashSet::class.java
                "java.util.Map" -> Map::class.java
                "java.util.HashMap" -> HashMap::class.java
                else -> Class.forName(typeName)
            }
        } catch (e: ClassNotFoundException) {
            null
        }
    }
} 