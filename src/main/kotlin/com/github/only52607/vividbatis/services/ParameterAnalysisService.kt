package com.github.only52607.vividbatis.services

import com.github.only52607.vividbatis.model.*
import com.github.only52607.vividbatis.util.*
import com.google.gson.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import java.lang.reflect.Type

@Service
class ParameterAnalysisService(private val project: Project) {
    
    companion object {
        const val TYPE_IBATIS_PARAM = "org.apache.ibatis.annotations.Param"
    }
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(List::class.java, ListTypeAdapter())
        .registerTypeAdapter(Map::class.java, MapTypeAdapter())
        .create()
    
    fun generateDefaultParameterJson(namespace: String, statementId: String): String {
        val parameterInfo = getStatementParameterInfo(namespace, statementId)
        
        if (parameterInfo != null) {
            return parameterInfo.generateTemplate(gson)
        }

        val parameterType = project.findMybatisMapperXml(namespace)
            ?.findMybatisStatementById(statementId)
            ?.getAttributeValue("parameterType")

        return when (parameterType) {
            null -> "{}"
            "java.util.Map" -> "{\n  \"key1\": \"value1\",\n  \"key2\": \"value2\"\n}"
            else -> "{}"
        }
    }
    
    fun getStatementParameterInfo(namespace: String, statementId: String): ParameterInfo? {
        val parameterTypeInXml = project.findMybatisMapperXml(namespace)
            ?.findMybatisStatementById(statementId)
            ?.getAttributeValue("parameterType")
        if (parameterTypeInXml?.isNotEmpty() == true) {
            val paramClass = findPsiClass(parameterTypeInXml)
            return ParameterInfo.JavaBeanParameter(paramClass, parameterTypeInXml)
        }
        val method = findPsiMethod(namespace, statementId) ?: return null
        val parameters = method.parameterList.parameters
        if (parameters.isEmpty()) {
            return ParameterInfo.MapParameter
        }
        if (parameters.size == 1) {
            val param = parameters[0]
            val paramType = param.type.canonicalText
            when {
                paramType == "java.util.Map" || paramType.startsWith("java.util.Map<") -> {
                    return ParameterInfo.MapParameter
                }
                TypeUtils.isPrimitiveOrWrapper(paramType) || paramType == "java.lang.String" -> {
                    return ParameterInfo.SinglePrimitiveParameter(paramType)
                }
                else -> {
                    val paramClass = findPsiClass(paramType)
                    return ParameterInfo.JavaBeanParameter(paramClass, paramType)
                }
            }
        }
        val hasParamAnnotations = parameters.any { hasParamAnnotation(it) }
        val methodParams = parameters.mapIndexed { index, param ->
            MethodParameter(
                name = getParameterName(param),
                type = param.type.canonicalText,
                paramAnnotation = getParamAnnotationValue(param),
                position = index,
                psiParameter = param
            )
        }
        return if (hasParamAnnotations) {
            val hasMixedTypes = methodParams.any { param ->
                !TypeUtils.isPrimitiveOrWrapper(param.type) && param.type != "java.lang.String"
            }
            if (hasMixedTypes) {
                ParameterInfo.MixedParameter(methodParams, ::findPsiClass)
            } else {
                ParameterInfo.AnnotationParameter(methodParams)
            }
        } else {
            ParameterInfo.PositionalParameter(methodParams)
        }
    }
    
    fun parseParameterJson(json: String, parameterInfo: ParameterInfo): OgnlRootObject {
        val jsonElement = gson.fromJson(json, JsonElement::class.java)
        return parameterInfo.parseJson(jsonElement, gson)
    }

    private fun findPsiClass(className: String): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
    }

    private fun findPsiMethod(psiClass: PsiClass, methodName: String): PsiMethod? {
        return psiClass.findMethodsByName(methodName, false).firstOrNull()
    }

    private fun findPsiMethod(className: String, methodName: String): PsiMethod? {
        val mapperInterface = findPsiClass(className) ?: return null
        return findPsiMethod(mapperInterface, methodName)
    }

    private fun hasParamAnnotation(parameter: PsiParameter): Boolean {
        return parameter.annotations.any { it.qualifiedName == TYPE_IBATIS_PARAM }
    }
    
    private fun getParamAnnotationValue(parameter: PsiParameter): String? {
        val paramAnnotation = parameter.annotations.find { it.qualifiedName == TYPE_IBATIS_PARAM }
        return paramAnnotation?.findAttributeValue("value")?.text?.removeSurrounding("\"")
    }
    
    private fun getParameterName(parameter: PsiParameter): String {
        return getParamAnnotationValue(parameter) ?: parameter.name ?: "param${parameter.parent.children.indexOf(parameter)}"
    }
    
    private class ListTypeAdapter : JsonSerializer<List<*>>, JsonDeserializer<List<*>> {
        override fun serialize(src: List<*>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            val jsonArray = JsonArray()
            src?.forEach { item -> jsonArray.add(context?.serialize(item)) }
            return jsonArray
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): List<*> {
            val list = mutableListOf<Any>()
            if (json?.isJsonArray == true) {
                json.asJsonArray.forEach { element ->
                    when {
                        element.isJsonPrimitive -> {
                            val primitive = element.asJsonPrimitive
                            when {
                                primitive.isString -> list.add(primitive.asString)
                                primitive.isNumber -> list.add(primitive.asNumber)
                                primitive.isBoolean -> list.add(primitive.asBoolean)
                            }
                        }
                        element.isJsonObject -> list.add(context?.deserialize(element, Map::class.java) ?: emptyMap<String, Any>())
                        element.isJsonArray -> list.add(context?.deserialize(element, List::class.java) ?: emptyList<Any>())
                    }
                }
            }
            return list
        }
    }
    
    private class MapTypeAdapter : JsonSerializer<Map<*, *>>, JsonDeserializer<Map<*, *>> {
        override fun serialize(src: Map<*, *>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            val jsonObject = JsonObject()
            src?.forEach { (key, value) -> 
                if (key is String) {
                    jsonObject.add(key, context?.serialize(value))
                }
            }
            return jsonObject
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Map<*, *> {
            val map = mutableMapOf<String, Any>()
            if (json?.isJsonObject == true) {
                json.asJsonObject.entrySet().forEach { (key, value) ->
                    when {
                        value.isJsonPrimitive -> {
                            val primitive = value.asJsonPrimitive
                            when {
                                primitive.isString -> map[key] = primitive.asString
                                primitive.isNumber -> map[key] = primitive.asNumber
                                primitive.isBoolean -> map[key] = primitive.asBoolean
                            }
                        }
                        value.isJsonObject -> map[key] = context?.deserialize(value, Map::class.java) ?: emptyMap<String, Any>()
                        value.isJsonArray -> map[key] = context?.deserialize(value, List::class.java) ?: emptyList<Any>()
                    }
                }
            }
            return map
        }
    }
} 