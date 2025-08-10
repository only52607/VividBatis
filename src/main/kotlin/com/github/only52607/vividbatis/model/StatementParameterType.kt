package com.github.only52607.vividbatis.model

import com.github.only52607.vividbatis.util.*
import com.google.gson.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil

sealed class StatementParameterType {

    abstract fun generateTemplate(): JsonElement

    abstract fun createRootObject(jsonElement: JsonElement): Any?

    class Map(private val valueType: PsiType? = null) : StatementParameterType() {
        override fun generateTemplate(): JsonElement {
            return JsonObject()
        }
        
        override fun createRootObject(jsonElement: JsonElement): Any? {
            if (jsonElement.isJsonObject) {
                return JsonMapRootObject(jsonElement.asJsonObject, valueType)
            }
            return null
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
            val parameterMap = mutableMapOf<String, Any>()
            
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                declarations.forEachIndexed { idx, param ->
                    val paramName = param.name ?: "param${idx}"
                    val jsonValue = jsonObject.get(paramName)
                    if (jsonValue != null) {
                        val convertedValue = convertJsonToJavaObject(jsonValue, param.type)
                        parameterMap[paramName] = convertedValue
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
        
        override fun createRootObject(jsonElement: JsonElement): Any {
            val convertedObject = convertJsonToJavaObject(jsonElement, psiClass?.qualifiedName)
            val parameterMap = mutableMapOf<String, Any>()
            
            if (convertedObject is kotlin.collections.Map<*, *>) {
                convertedObject.forEach { (key, value) ->
                    if (key is String && value != null) {
                        parameterMap[key] = value
                    }
                }
            } else {
                parameterMap["root"] = convertedObject
            }
            
            return parameterMap
        }
    }

    companion object {
        fun convertJsonToJavaObject(jsonElement: JsonElement, expectedType: String? = null): Any {
            return when {
                jsonElement.isJsonNull -> ""
                jsonElement.isJsonPrimitive -> {
                    val primitive = jsonElement.asJsonPrimitive
                    when {
                        primitive.isString -> primitive.asString
                        primitive.isNumber -> {
                            when (expectedType) {
                                "int", "java.lang.Integer" -> primitive.asInt
                                "long", "java.lang.Long" -> primitive.asLong
                                "float", "java.lang.Float" -> primitive.asFloat
                                "double", "java.lang.Double" -> primitive.asDouble
                                "byte", "java.lang.Byte" -> primitive.asByte
                                "short", "java.lang.Short" -> primitive.asShort
                                else -> primitive.asNumber
                            }
                        }
                        primitive.isBoolean -> primitive.asBoolean
                        else -> primitive.asString
                    }
                }
                jsonElement.isJsonArray -> {
                    val list = mutableListOf<Any>()
                    jsonElement.asJsonArray.forEach { element ->
                        list.add(convertJsonToJavaObject(element))
                    }
                    list
                }
                jsonElement.isJsonObject -> {
                    val map = mutableMapOf<String, Any>()
                    jsonElement.asJsonObject.entrySet().forEach { (key, value) ->
                        map[key] = convertJsonToJavaObject(value)
                    }
                    map
                }
                else -> jsonElement.toString()
            }
        }
    }
}

