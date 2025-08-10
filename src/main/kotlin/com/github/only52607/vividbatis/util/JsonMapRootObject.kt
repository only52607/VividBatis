package com.github.only52607.vividbatis.util

import com.google.gson.JsonObject
import com.intellij.psi.PsiType

class JsonMapRootObject(
    val json: JsonObject,
    val type: PsiType? = null
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