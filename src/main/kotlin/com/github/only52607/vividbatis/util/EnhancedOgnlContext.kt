package com.github.only52607.vividbatis.util

import ognl.Ognl
import ognl.OgnlException

class EnhancedOgnlContext(private val rootObject: OgnlRootObject) {

    fun evaluateBoolean(expression: String): Boolean {
        return try {
            val result = Ognl.getValue(expression, rootObject)
            
            when (result) {
                is Boolean -> result
                is Number -> result.toDouble() != 0.0
                is String -> result.isNotEmpty()
                is Collection<*> -> result.isNotEmpty()
                is Array<*> -> result.isNotEmpty()
                null -> false
                else -> true
            }
        } catch (e: OgnlException) {
            false
        }
    }
    
    fun evaluateValue(expression: String): Any {
        return try {
            Ognl.getValue(expression, rootObject) ?: ""
        } catch (e: OgnlException) {
            throw RuntimeException("Error evaluating OGNL expression: $expression", e)
        }
    }
    
    fun getParameterValue(paramName: String): Any? {
        return rootObject.getProperty(paramName)
    }
    
    fun getAllParameters(): Map<String, Any> {
        return rootObject
    }
    
    fun addParameter(name: String, value: Any): EnhancedOgnlContext {
        val newRootObject = rootObject.addParameter(name, value)
        return EnhancedOgnlContext(newRootObject)
    }
    
}