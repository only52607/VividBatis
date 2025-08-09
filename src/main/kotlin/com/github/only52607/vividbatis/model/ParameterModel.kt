package com.github.only52607.vividbatis.model

import com.github.only52607.vividbatis.util.*
import com.google.gson.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiParameter

/**
 * 参数信息密封类，封装了不同类型参数的处理逻辑
 */
sealed class ParameterInfo {
    
    /**
     * 生成默认参数模板
     */
    abstract fun generateTemplate(gson: Gson): String
    
    /**
     * 解析JSON参数并构建OgnlRootObject
     */
    abstract fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject
    
    /**
     * Map类型参数
     */
    object MapParameter : ParameterInfo() {
        override fun generateTemplate(gson: Gson): String {
            return """
            {
              "key1": "value1",
              "key2": "value2"
            }
            """.trimIndent()
        }
        
        override fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject {
            val parameterMap = mutableMapOf<String, Any>()
            
            if (jsonElement.isJsonObject) {
                jsonElement.asJsonObject.entrySet().forEach { (key, value) ->
                    val convertedValue = convertJsonToJavaObject(value)
                    parameterMap[key] = convertedValue
                }
            }
            
            return OgnlRootObject(parameterMap)
        }
    }
    
    /**
     * 注解参数类型
     */
    data class AnnotationParameter(val parameters: List<MethodParameter>) : ParameterInfo() {
        override fun generateTemplate(gson: Gson): String {
            val jsonObject = JsonObject()
            parameters.forEach { param ->
                val paramName = param.paramAnnotation ?: param.name
                val defaultValue = TypeUtils.generateDefaultValue(param.type)
                jsonObject.add(paramName, defaultValue)
            }
            return gson.toJson(jsonObject)
        }
        
        override fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject {
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
    }
    
    /**
     * Java Bean参数类型
     */
    data class JavaBeanParameter(
        val parameterClass: PsiClass?,
        val parameterTypeString: String?
    ) : ParameterInfo() {
        override fun generateTemplate(gson: Gson): String {
            if (parameterClass == null) return "{}"
            val analyzer = JavaClassAnalyzer()
            val jsonObject = analyzer.analyzeClass(parameterClass)
            return gson.toJson(jsonObject)
        }
        
        override fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject {
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
    }
    
    /**
     * 混合参数类型
     */
    data class MixedParameter(
        val parameters: List<MethodParameter>,
        val psiClassFinder: ((String) -> PsiClass?)? = null
    ) : ParameterInfo() {
        override fun generateTemplate(gson: Gson): String {
            val jsonObject = JsonObject()
            parameters.forEach { param ->
                val paramName = param.paramAnnotation ?: param.name
                when {
                    TypeUtils.isPrimitiveOrWrapper(param.type) || param.type == "java.lang.String" -> {
                        val defaultValue = TypeUtils.generateDefaultValue(param.type)
                        jsonObject.add(paramName, defaultValue)
                    }
                    else -> {
                        val paramClass = psiClassFinder?.invoke(param.type)
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
        
        override fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject {
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
    }
    
    /**
     * 位置参数类型
     */
    data class PositionalParameter(val parameters: List<MethodParameter>) : ParameterInfo() {
        override fun generateTemplate(gson: Gson): String {
            val jsonArray = JsonArray()
            parameters.forEach { param ->
                val defaultValue = TypeUtils.generateDefaultValue(param.type)
                jsonArray.add(defaultValue)
            }
            return gson.toJson(jsonArray)
        }
        
        override fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject {
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
    }
    
    /**
     * 单一原始类型参数
     */
    data class SinglePrimitiveParameter(val parameterTypeString: String?) : ParameterInfo() {
        override fun generateTemplate(gson: Gson): String {
            if (parameterTypeString == null) return "{}"
            val defaultValue = TypeUtils.generateDefaultValue(parameterTypeString)
            return gson.toJson(defaultValue)
        }
        
        override fun parseJson(jsonElement: JsonElement, gson: Gson): OgnlRootObject {
            val convertedValue = convertJsonToJavaObject(jsonElement, parameterTypeString)
            val parameterMap = mapOf("value" to convertedValue)
            
            return OgnlRootObject(parameterMap)
        }
    }
    
    companion object {
        /**
         * 将JSON转换为Java对象的通用方法
         */
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

/**
 * 方法参数数据类
 */
data class MethodParameter(
    val name: String,
    val type: String,
    val paramAnnotation: String? = null,
    val position: Int,
    val psiParameter: PsiParameter? = null
)
