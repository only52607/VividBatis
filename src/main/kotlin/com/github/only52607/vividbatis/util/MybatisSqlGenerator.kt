package com.github.only52607.vividbatis.util

import com.intellij.psi.xml.XmlTag

class MybatisSqlGenerator {

    fun generateSql(template: SqlTemplate, rootObject: OgnlRootObject): String {
        val statementTag = findStatementTag(template)
        val enhancedContext = EnhancedOgnlContext(rootObject)
        val sql = processXmlTag(statementTag, enhancedContext, template)
        return formatSql(sql)
    }
    
    private fun findStatementTag(template: SqlTemplate): XmlTag {
        return template.mapperFile.findMybatisStatementById(template.statementId)
            ?: throw RuntimeException("Statement tag not found: ${template.statementId}")
    }
    
    private fun processXmlTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        return when (tag.name) {
            "include" -> processIncludeTag(tag, context, template)
            "bind" -> ""
            "if" -> processConditionalTag(tag, context, template)
            "foreach" -> processForeachTag(tag, context, template)
            "trim" -> processTrimTag(tag, context, template)
            "where" -> processWhereTag(tag, context, template)
            "set" -> processSetTag(tag, context, template)
            "choose" -> processChooseTag(tag, context, template)
            "when" -> processConditionalTag(tag, context, template)
            "otherwise" -> processChildTags(tag, context, template)
            else -> processChildTags(tag, context, template)
        }
    }
    
    private fun processIncludeTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val refId = tag.getAttributeValue("refid") ?: return ""
        val includeParameters = mutableMapOf<String, String>()
        
        tag.findSubTags("property").forEach { property ->
            val name = property.getAttributeValue("name")
            val value = property.getAttributeValue("value")
            if (name != null && value != null) {
                includeParameters[name] = value
            }
        }
        
        val resolvedRefId = resolveVariables(refId, context, includeParameters)
        val sqlFragment = template.mapperFile.findSqlFragmentByRefId(resolvedRefId)
        
        return if (sqlFragment != null) {
            processXmlTag(sqlFragment, context, template)
        } else ""
    }
    
    private fun processConditionalTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val test = tag.getAttributeValue("test") ?: return ""
        return if (context.evaluateBoolean(test)) {
            processChildTags(tag, context, template)
        } else ""
    }
    
    private fun processForeachTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val collection = tag.getAttributeValue("collection") ?: return ""
        val item = tag.getAttributeValue("item") ?: "item"
        val index = tag.getAttributeValue("index") ?: "index"
        val open = tag.getAttributeValue("open") ?: ""
        val close = tag.getAttributeValue("close") ?: ""
        val separator = tag.getAttributeValue("separator") ?: ","
        
        val collectionValue = context.evaluateValue(collection)
        
        val collectionList = when (collectionValue) {
            is List<*> -> collectionValue
            is Array<*> -> collectionValue.toList()
            is Set<*> -> collectionValue.toList()
            is Collection<*> -> collectionValue.toList()
            else -> return ""
        }
        
        val items = collectionList.mapIndexed { idx, value ->
            val itemContext = createItemContext(context, item, index, value, idx)
            processChildTags(tag, itemContext, template)
        }
        
        return open + items.joinToString(separator) + close
    }

    private fun processTrimTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val content = processChildTags(tag, context, template).trim()
        if (content.isBlank()) return ""

        val prefix = tag.getAttributeValue("prefix") ?: ""
        val suffix = tag.getAttributeValue("suffix") ?: ""
        val prefixOverrides = parseOverrides(tag.getAttributeValue("prefixOverrides"))
        val suffixOverrides = parseOverrides(tag.getAttributeValue("suffixOverrides"))

        return applyTrim(
            content = content,
            prefix = prefix,
            suffix = suffix,
            prefixOverrides = prefixOverrides,
            suffixOverrides = suffixOverrides,
            addSpaceAfterPrefix = true
        )
    }

    private fun processWhereTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val content = processChildTags(tag, context, template).trim()
        if (content.isBlank()) return ""

        return applyTrim(
            content = content,
            prefix = "WHERE",
            suffix = "",
            prefixOverrides = listOf("AND ", "OR "),
            suffixOverrides = emptyList(),
            addSpaceAfterPrefix = true
        )
    }
    
    private fun processSetTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val content = processChildTags(tag, context, template).trim()
        if (content.isBlank()) return ""

        return applyTrim(
            content = content,
            prefix = "SET",
            suffix = "",
            prefixOverrides = emptyList(),
            suffixOverrides = listOf(","),
            addSpaceAfterPrefix = true
        )
    }

    private fun parseOverrides(attr: String?): List<String> {
        return attr?.split('|')?.map { it }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun applyTrim(
        content: String,
        prefix: String,
        suffix: String,
        prefixOverrides: List<String>,
        suffixOverrides: List<String>,
        addSpaceAfterPrefix: Boolean
    ): String {
        var working = content.trim()

        if (working.isBlank()) return ""

        if (prefixOverrides.isNotEmpty()) {
            var changed: Boolean
            do {
                changed = false
                for (token in prefixOverrides) {
                    val pattern = ("^\\s*" + Regex.escape(token)).toRegex(RegexOption.IGNORE_CASE)
                    val newContent = working.replaceFirst(pattern, "").trimStart()
                    if (newContent != working) {
                        working = newContent
                        changed = true
                        break
                    }
                }
            } while (changed && working.isNotBlank())
        }

        if (suffixOverrides.isNotEmpty()) {
            var changed: Boolean
            do {
                changed = false
                for (token in suffixOverrides) {
                    val pattern = (Regex.escape(token) + "\\s*$").toRegex(RegexOption.IGNORE_CASE)
                    val newContent = working.replaceFirst(pattern, "").trimEnd()
                    if (newContent != working) {
                        working = newContent
                        changed = true
                        break
                    }
                }
            } while (changed && working.isNotBlank())
        }

        if (working.isBlank()) return ""

        val withPrefix = if (prefix.isNotEmpty()) {
            if (addSpaceAfterPrefix) "$prefix $working" else prefix + working
        } else working
        val withSuffix = if (suffix.isNotEmpty()) withPrefix + suffix else withPrefix
        return withSuffix
    }
    
    private fun processChooseTag(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        tag.subTags.forEach { child ->
            if (child.name == "when") {
                val test = child.getAttributeValue("test")
                if (test != null && context.evaluateBoolean(test)) {
                    return processChildTags(child, context, template)
                }
            }
        }
        
        return tag.subTags.find { it.name == "otherwise" }?.let { 
            processChildTags(it, context, template) 
        } ?: ""
    }
    
    private fun processChildTags(tag: XmlTag, context: EnhancedOgnlContext, template: SqlTemplate): String {
        val builder = StringBuilder()
        var currentContext = context
        
        tag.value.children.forEach { child ->
            when {
                child is XmlTag -> {
                    if (child.name == "bind") {
                        val name = child.getAttributeValue("name")
                        val value = child.getAttributeValue("value")
                        if (name != null && value != null) {
                            val bindValue = currentContext.evaluateValue(value)
                            currentContext = currentContext.addParameter(name, bindValue)
                        }
                    } else {
                        builder.append(processXmlTag(child, currentContext, template))
                    }
                }
                else -> {
                    val text = child.text
                    if (text.isNotBlank()) {
                        builder.append(replaceParameters(text, currentContext))
                    }
                }
            }
        }
        
        return builder.toString()
    }
    
    private fun resolveVariables(text: String, context: EnhancedOgnlContext, includeParameters: Map<String, String>): String {
        var result = text
        val pattern = "\\$\\{([^}]+)}".toRegex()
        
        pattern.findAll(text).forEach { match ->
            val variableName = match.groupValues[1]
            val value = includeParameters[variableName] 
                ?: context.getParameterValue(variableName)?.toString() 
                ?: match.value
            result = result.replace(match.value, value)
        }
        
        return result
    }
    
    private fun replaceParameters(text: String, context: EnhancedOgnlContext): String {
        var result = text
        
        result = "#\\{([^}]+)}".toRegex().replace(result) { match ->
            val paramName = match.groupValues[1]
            val value = context.getParameterValue(paramName)
            when (value) {
                is String -> "'$value'"
                is Number -> value.toString()
                is Boolean -> value.toString()
                null -> "NULL"
                else -> "'$value'"
            }
        }
        
        result = "\\$\\{([^}]+)}".toRegex().replace(result) { match ->
            val paramName = match.groupValues[1]
            context.getParameterValue(paramName)?.toString() ?: ""
        }
        
        return result
    }
    
    private fun createItemContext(originalContext: EnhancedOgnlContext, item: String, index: String, value: Any?, idx: Int): EnhancedOgnlContext {
        val newParameterMap = originalContext.getAllParameters().toMutableMap()
        newParameterMap[item] = value ?: ""
        newParameterMap[index] = idx
        
        val newRootObject = OgnlRootObject(newParameterMap)
        return EnhancedOgnlContext(newRootObject)
    }
    
    private fun formatSql(sql: String): String {
        return sql
            .replace("\\s+".toRegex(), " ")
            .replace("\\( ", "(")
            .replace(" \\)", ")")
            .replace(" ,", ",")
            .trim()
    }
}