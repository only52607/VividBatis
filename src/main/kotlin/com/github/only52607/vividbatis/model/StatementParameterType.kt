package com.github.only52607.vividbatis.model

import com.github.only52607.vividbatis.util.JavaClassAnalyzer
import com.github.only52607.vividbatis.util.TypeUtils
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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

        override fun createRootObject(jsonElement: JsonElement): Any {
            return OgnlObjectMapper.asOgnlMap(jsonElement, valueType)
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
                        parameterMap[paramName] = OgnlObjectMapper.asOgnlObject(jsonElement, param.psiParameter.type)
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
            return OgnlObjectMapper.asOgnlObject(jsonElement, psiType)
        }
    }
}

