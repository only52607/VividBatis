package com.github.only52607.vividbatis.util

import com.google.gson.JsonObject
import com.intellij.psi.PsiType

class OgnlRootObject(parameters: Map<String, Any>) : HashMap<String, Any>() {
    
    init {
        putAll(parameters)
    }
    
    override fun get(key: String): Any? {
        val value = super.get(key)
        return when (value) {
            is Map<*, *> -> EnhancedMap(value.filterKeys { it is String }.mapKeys { it.key as String }.mapValues { it.value ?: "" })
            is List<*> -> EnhancedList(value)
            else -> value
        }
    }
    
    fun getProperty(name: String): Any? {
        return get(name)
    }
    
    fun hasProperty(name: String): Boolean {
        return containsKey(name)
    }
    
    override fun isEmpty(): Boolean {
        return super.isEmpty()
    }
    
    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }
    
    override fun toString(): String {
        return super.toString()
    }
    
    fun addParameter(name: String, value: Any): OgnlRootObject {
        val newParameters = this.toMutableMap()
        newParameters[name] = value
        return OgnlRootObject(newParameters)
    }
    
    class EnhancedMap(private val originalMap: Map<String, Any>) : HashMap<String, Any>(originalMap) {
        
        override fun get(key: String): Any? {
            return originalMap[key]
        }
        
        override fun containsKey(key: String): Boolean {
            return originalMap.containsKey(key)
        }
        
        override fun isEmpty(): Boolean {
            return originalMap.isEmpty()
        }
        
        fun isNotEmpty(): Boolean {
            return originalMap.isNotEmpty()
        }
        
        override fun toString(): String {
            return originalMap.toString()
        }
    }
    
    class EnhancedList(private val originalList: List<*>) : ArrayList<Any?>(originalList) {
        
        override fun isEmpty(): Boolean {
            return originalList.isEmpty()
        }
        
        fun isNotEmpty(): Boolean {
            return originalList.isNotEmpty()
        }
        
        override fun get(index: Int): Any? {
            return if (index >= 0 && index < originalList.size) {
                originalList[index]
            } else null
        }
        
        override fun contains(element: Any?): Boolean {
            return originalList.contains(element)
        }
        
        override fun toString(): String {
            return originalList.toString()
        }
    }
}

class JsonMapRootObject(
    val json: JsonObject,
    val type: PsiType?
): Map<String, Any> {
    override val entries: Set<Map.Entry<String, Any>>
        get() = json.entrySet()

    override val keys: Set<String>
        get() = json.keySet()

    override val size: Int
        get() = json.entrySet().size

    override val values: Collection<Any>
        get() = json.entrySet().map { entry ->
            when {
                entry.value.isJsonPrimitive -> entry.value.asJsonPrimitive
                entry.value.isJsonObject -> JsonMapRootObject(entry.value.asJsonObject, type)
                entry.value.isJsonArray -> entry.value.asJsonArray.map { it.asJsonPrimitive }
                else -> null
            } as Any
        }

    override fun containsKey(key: String): Boolean {
        return json.has(key)
    }

    override fun containsValue(value: Any): Boolean {
        return json.entrySet().any { it.value == value }
    }

    override fun get(key: String): Any? {
        return json.get(key)?.let {
            when {
                it.isJsonPrimitive -> it.asJsonPrimitive
                it.isJsonObject -> JsonMapRootObject(it.asJsonObject, type)
                it.isJsonArray -> it.asJsonArray.map { item -> item.asJsonPrimitive }
                else -> null
            }
        }
    }

    override fun isEmpty(): Boolean {
        return json.entrySet().isEmpty()
    }
}