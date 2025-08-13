package com.github.only52607.vividbatis.model

import PsiTypeToJsonConverter
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.psi.PsiType

sealed class StatementParameterType {

    abstract fun generateTemplate(): JsonElement

    abstract fun createRootObject(jsonElement: JsonElement): Any?

    class Map(private val valueType: PsiType? = null) : StatementParameterType() {
        override fun generateTemplate(): JsonElement {
            return JsonObject()
        }

        override fun createRootObject(jsonElement: JsonElement): Any {
            return OgnlObjectMapper.convertToOgnlMap(jsonElement, valueType)
        }
    }

    data class Multiple(val declarations: List<StatementParameterDeclaration>) : StatementParameterType() {
        constructor(vararg declarations: StatementParameterDeclaration) : this(declarations.toList())

        override fun generateTemplate(): JsonElement {
            val jsonObject = JsonObject()
            declarations.forEachIndexed { idx, param ->
                val paramName = param.name ?: "param${idx}"
                jsonObject.add(paramName, PsiTypeToJsonConverter.convert(param.psiParameter.type))
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
                        parameterMap[paramName] = OgnlObjectMapper.convertToOgnlObject(jsonElement, param.psiParameter.type)
                    }
                }
            }

            return parameterMap
        }
    }

    data class JavaBean(
        val psiType: PsiType?
    ) : StatementParameterType() {
        override fun generateTemplate(): JsonElement {
            if (psiType == null) return JsonObject()
            return PsiTypeToJsonConverter.convert(psiType)
        }

        override fun createRootObject(jsonElement: JsonElement): Any? {
            return OgnlObjectMapper.convertToOgnlObject(jsonElement, psiType)
        }
    }
}

