package com.github.only52607.vividbatis.model

import com.github.only52607.vividbatis.util.*
import com.google.gson.*
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil

sealed class StatementParameterType {

    abstract fun generateTemplate(): JsonElement

    abstract fun createRootObject(jsonElement: JsonElement): Any?

    class Map(private val valueType: PsiType? = null) : StatementParameterType() {
        override fun generateTemplate(): JsonElement {
            return JsonObject()
        }

        override fun createRootObject(jsonElement: JsonElement): Any {
            return jsonElement.asOgnlMap(valueType)
        }
    }

    data class Multiple(val declarations: List<StatementParameterDeclaration>) : StatementParameterType() {
        constructor(vararg declarations: StatementParameterDeclaration) : this(declarations.toList())

        override fun generateTemplate(): JsonElement {
            val jsonObject = JsonObject()
            declarations.forEachIndexed { idx, param ->
                val paramName = param.name ?: "param${idx}"
                val defaultValue = TypeUtils.generateDefaultValue(param.type)
                jsonObject.add(paramName, defaultValue)
            }
            return jsonObject
        }

        override fun createRootObject(jsonElement: JsonElement): Any {
            val parameterMap = mutableMapOf<String, Any?>()

            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                declarations.forEachIndexed { idx, param ->
                    val paramName = param.name ?: "param${idx}"
                    val jsonValue = jsonObject.get(paramName)
                    if (jsonValue != null) {
                        parameterMap[paramName] = jsonValue.asOgnlObject(param.psiParameter.type)
                    }
                }
            }

            return parameterMap
        }
    }

    data class JavaBean(
        val psiType: PsiType?
    ) : StatementParameterType() {
        val psiClass: PsiClass? = psiType?.let { PsiTypesUtil.getPsiClass(it) }

        override fun generateTemplate(): JsonElement {
            if (psiClass == null) return JsonObject()
            return JavaClassAnalyzer().analyzeClass(psiClass)
        }

        override fun createRootObject(jsonElement: JsonElement): Any? {
            return jsonElement.asOgnlObject(psiType)
        }
    }
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
                "java.lang.String" -> return asString
                "java.lang.Character" -> return asString.toCharArray()[0]
                "java.lang.Integer" -> return asInt
                "java.lang.Long" -> return asLong
                "java.lang.Float" -> return asFloat
                "java.lang.Double" -> return asDouble
                "java.lang.Byte" -> return asByte
                "java.lang.Short" -> return asShort
                "java.lang.Boolean" -> return asBoolean
                "java.lang.List", "java.util.ArrayList" -> return asOgnlList(expectedType.parameters.firstOrNull())
                "java.lang.Set", "java.util.HashSet" -> return asOgnlSet(expectedType.parameters.firstOrNull())
                "java.lang.Map", "java.util.HashMap" -> return asOgnlMap(expectedType.parameters.getOrNull(1))
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
