package com.github.only52607.vividbatis.model

import ognl.ObjectPropertyAccessor
import ognl.OgnlRuntime

class ExtendedRootObject(
    val root: Any,
    val properties: Map<Any, Any?>,
)

class ExtendedObjectPropertyAccessor : ObjectPropertyAccessor() {
    companion object {
        init {
            OgnlRuntime.setPropertyAccessor(ExtendedRootObject::class.java, ExtendedObjectPropertyAccessor())
        }
    }

    override fun getProperty(context: MutableMap<*, *>?, target: Any?, name: Any?): Any? {
        if (target is ExtendedRootObject) {
            if (name != null && target.properties.containsKey(name)) {
                return target.properties[name]
            }
            return getProperty(context, target.root, name)
        }
        return super.getProperty(context, target, name)
    }
}