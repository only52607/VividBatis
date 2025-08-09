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
        fun getInstance(project: Project): ParameterAnalysisService {
            return project.getService(ParameterAnalysisService::class.java)
        }
    }
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(List::class.java, ListTypeAdapter())
        .registerTypeAdapter(Map::class.java, MapTypeAdapter())
        .create()
    
    fun generateDefaultParameterJson(namespace: String, statementId: String): String {
        val parameterInfo = analyzeMapperMethod(namespace, statementId)
        
        if (parameterInfo != null) {
            return generateParameterTemplate(parameterInfo)
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
    
    fun analyzeMapperMethod(namespace: String, statementId: String): ParameterInfo? {
        val mapperInterface = findMapperInterface(namespace) ?: return null
        val method = findMapperMethod(mapperInterface, statementId) ?: return null
        
        return analyzeMethodParameters(method)
    }
    
    fun generateParameterTemplate(parameterInfo: ParameterInfo): String {
        return when (parameterInfo.type) {
            ParameterType.MAP -> generateMapTemplate()
            ParameterType.ANNOTATION -> generateAnnotationTemplate(parameterInfo.parameters)
            ParameterType.JAVA_BEAN -> generateBeanTemplate(parameterInfo.parameterClass)
            ParameterType.MIXED -> generateMixedTemplate(parameterInfo.parameters)
            ParameterType.POSITIONAL -> generatePositionalTemplate(parameterInfo.parameters)
            ParameterType.SINGLE_PRIMITIVE -> generateSinglePrimitiveTemplate(parameterInfo.parameterTypeString)
        }
    }
    
    fun parseParameterJson(json: String, parameterInfo: ParameterInfo): OgnlRootObject {
        val jsonElement = gson.fromJson(json, JsonElement::class.java)
        
        return when (parameterInfo.type) {
            ParameterType.MAP -> parseMapParameters(jsonElement)
            ParameterType.ANNOTATION -> parseAnnotationParameters(jsonElement, parameterInfo.parameters)
            ParameterType.JAVA_BEAN -> parseBeanParameters(jsonElement, parameterInfo.parameterClass)
            ParameterType.MIXED -> parseMixedParameters(jsonElement, parameterInfo.parameters)
            ParameterType.POSITIONAL -> parsePositionalParameters(jsonElement, parameterInfo.parameters)
            ParameterType.SINGLE_PRIMITIVE -> parseSinglePrimitiveParameters(jsonElement, parameterInfo.parameterTypeString)
        }
    }
    
    private fun analyzeMethodParameters(method: PsiMethod): ParameterInfo {
        val parameters = method.parameterList.parameters
        
        if (parameters.isEmpty()) {
            return ParameterInfo(ParameterType.MAP)
        }
        
        if (parameters.size == 1) {
            val param = parameters[0]
            val paramType = param.type.canonicalText
            
            when {
                paramType == "java.util.Map" || paramType.startsWith("java.util.Map<") -> {
                    return ParameterInfo(ParameterType.MAP)
                }
                TypeUtils.isPrimitiveOrWrapper(paramType) || paramType == "java.lang.String" -> {
                    return ParameterInfo(
                        type = ParameterType.SINGLE_PRIMITIVE,
                        parameterTypeString = paramType
                    )
                }
                else -> {
                    val paramClass = findPsiClass(paramType)
                    return ParameterInfo(
                        type = ParameterType.JAVA_BEAN,
                        parameterClass = paramClass,
                        parameterTypeString = paramType
                    )
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
                ParameterInfo(ParameterType.MIXED, parameters = methodParams)
            } else {
                ParameterInfo(ParameterType.ANNOTATION, parameters = methodParams)
            }
        } else {
            ParameterInfo(ParameterType.POSITIONAL, parameters = methodParams)
        }
    }
    
    private fun generateMapTemplate(): String {
        return """
        {
          "key1": "value1",
          "key2": "value2"
        }
        """.trimIndent()
    }
    
    private fun generateAnnotationTemplate(parameters: List<MethodParameter>): String {
        val jsonObject = JsonObject()
        parameters.forEach { param ->
            val paramName = param.paramAnnotation ?: param.name
            val defaultValue = TypeUtils.generateDefaultValue(param.type)
            jsonObject.add(paramName, defaultValue)
        }
        return gson.toJson(jsonObject)
    }
    
    private fun generateBeanTemplate(parameterClass: PsiClass?): String {
        if (parameterClass == null) return "{}"
        val analyzer = JavaClassAnalyzer()
        val jsonObject = analyzer.analyzeClass(parameterClass)
        return gson.toJson(jsonObject)
    }
    
    private fun generateMixedTemplate(parameters: List<MethodParameter>): String {
        val jsonObject = JsonObject()
        parameters.forEach { param ->
            val paramName = param.paramAnnotation ?: param.name
            when {
                TypeUtils.isPrimitiveOrWrapper(param.type) || param.type == "java.lang.String" -> {
                    val defaultValue = TypeUtils.generateDefaultValue(param.type)
                    jsonObject.add(paramName, defaultValue)
                }
                else -> {
                    val paramClass = findPsiClass(param.type)
                    if (paramClass != null) {
                        val analyzer = JavaClassAnalyzer()
                        val beanObject = analyzer.analyzeClass(paramClass)
                        jsonObject.add(paramName, beanObject)
                    } else {
                        jsonObject.add(paramName, JsonObject())
                    }
                }
            }
        }
        return gson.toJson(jsonObject)
    }
    
    private fun generatePositionalTemplate(parameters: List<MethodParameter>): String {
        val jsonArray = JsonArray()
        parameters.forEach { param ->
            val defaultValue = TypeUtils.generateDefaultValue(param.type)
            jsonArray.add(defaultValue)
        }
        return gson.toJson(jsonArray)
    }
    
    private fun generateSinglePrimitiveTemplate(parameterType: String?): String {
        if (parameterType == null) return "{}"
        val defaultValue = TypeUtils.generateDefaultValue(parameterType)
        return gson.toJson(defaultValue)
    }
    
    private fun parseMapParameters(jsonElement: JsonElement): OgnlRootObject {
        val parameterMap = mutableMapOf<String, Any>()
        
        if (jsonElement.isJsonObject) {
            jsonElement.asJsonObject.entrySet().forEach { (key, value) ->
                val convertedValue = convertJsonToJavaObject(value)
                parameterMap[key] = convertedValue
            }
        }
        
        return OgnlRootObject(parameterMap)
    }
    
    private fun parseAnnotationParameters(jsonElement: JsonElement, parameters: List<MethodParameter>): OgnlRootObject {
        val parameterMap = mutableMapOf<String, Any>()
        
        if (jsonElement.isJsonObject) {
            val jsonObject = jsonElement.asJsonObject
            parameters.forEach { param ->
                val paramName = param.paramAnnotation ?: param.name
                val jsonValue = jsonObject.get(paramName)
                if (jsonValue != null) {
                    val convertedValue = convertJsonToJavaObject(jsonValue, param.type)
                    parameterMap[paramName] = convertedValue
                }
            }
        }
        
        return OgnlRootObject(parameterMap)
    }
    
    private fun parseBeanParameters(jsonElement: JsonElement, parameterClass: PsiClass?): OgnlRootObject {
        val convertedObject = convertJsonToJavaObject(jsonElement, parameterClass?.qualifiedName)
        val parameterMap = mutableMapOf<String, Any>()
        
        if (convertedObject is Map<*, *>) {
            convertedObject.forEach { (key, value) ->
                if (key is String && value != null) {
                    parameterMap[key] = value
                }
            }
        } else {
            parameterMap["root"] = convertedObject
        }
        
        return OgnlRootObject(parameterMap)
    }
    
    private fun parseMixedParameters(jsonElement: JsonElement, parameters: List<MethodParameter>): OgnlRootObject {
        val parameterMap = mutableMapOf<String, Any>()
        
        if (jsonElement.isJsonObject) {
            val jsonObject = jsonElement.asJsonObject
            parameters.forEach { param ->
                val paramName = param.paramAnnotation ?: param.name
                val jsonValue = jsonObject.get(paramName)
                if (jsonValue != null) {
                    val convertedValue = convertJsonToJavaObject(jsonValue, param.type)
                    parameterMap[paramName] = convertedValue
                }
            }
        }
        
        return OgnlRootObject(parameterMap)
    }
    
    private fun parsePositionalParameters(jsonElement: JsonElement, parameters: List<MethodParameter>): OgnlRootObject {
        val parameterMap = mutableMapOf<String, Any>()
        
        if (jsonElement.isJsonArray) {
            val jsonArray = jsonElement.asJsonArray
            parameters.forEachIndexed { index, param ->
                if (index < jsonArray.size()) {
                    val jsonValue = jsonArray.get(index)
                    val convertedValue = convertJsonToJavaObject(jsonValue, param.type)
                    parameterMap[index.toString()] = convertedValue
                    parameterMap[param.name] = convertedValue
                }
            }
        }
        
        return OgnlRootObject(parameterMap)
    }
    
    private fun parseSinglePrimitiveParameters(jsonElement: JsonElement, parameterType: String?): OgnlRootObject {
        val convertedValue = convertJsonToJavaObject(jsonElement, parameterType)
        val parameterMap = mapOf("value" to convertedValue)
        
        return OgnlRootObject(parameterMap)
    }
    
    private fun convertJsonToJavaObject(jsonElement: JsonElement, expectedType: String? = null): Any {
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
    
    private fun findMapperInterface(namespace: String): PsiClass? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        return javaPsiFacade.findClass(namespace, GlobalSearchScope.allScope(project))
    }
    
    private fun findMapperMethod(mapperInterface: PsiClass, methodName: String): PsiMethod? {
        return mapperInterface.findMethodsByName(methodName, false).firstOrNull()
    }
    
    private fun findPsiClass(className: String): PsiClass? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        return javaPsiFacade.findClass(className, GlobalSearchScope.allScope(project))
    }
    
    private fun hasParamAnnotation(parameter: PsiParameter): Boolean {
        return parameter.annotations.any { it.qualifiedName == "org.apache.ibatis.annotations.Param" }
    }
    
    private fun getParamAnnotationValue(parameter: PsiParameter): String? {
        val paramAnnotation = parameter.annotations.find { it.qualifiedName == "org.apache.ibatis.annotations.Param" }
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