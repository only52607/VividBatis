package com.github.only52607.vividbatis.util

import com.intellij.psi.xml.XmlTag

class MybatisSqlGenerator {

    fun generateSql(template: SqlTemplate, parameters: Map<String, Any>): String {
        val statementTag = findStatementTag(template)
        val sql = processXmlTag(statementTag, parameters, template)
        return formatSql(sql)
    }
    
    private fun findStatementTag(template: SqlTemplate): XmlTag {
        val rootTag = template.mapperFile.rootTag!!
        for (child in rootTag.subTags) {
            if (child.name in setOf("select", "insert", "update", "delete") &&
                child.getAttributeValue("id") == template.statementId) {
                return child
            }
        }
        throw RuntimeException("Statement tag not found: ${template.statementId}")
    }
    
    private fun processXmlTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        return when (tag.name) {
            "include" -> processIncludeTag(tag, parameters, template)
            "if" -> processIfTag(tag, parameters, template)
            "foreach" -> processForeachTag(tag, parameters, template)
            "where" -> processWhereTag(tag, parameters, template)
            "set" -> processSetTag(tag, parameters, template)
            "choose" -> processChooseTag(tag, parameters, template)
            "when" -> processWhenTag(tag, parameters, template)
            "otherwise" -> processOtherwiseTag(tag, parameters, template)
            "sql" -> processRegularTag(tag, parameters, template)
            else -> processRegularTag(tag, parameters, template)
        }
    }
    
    private fun processIncludeTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val refid = tag.getAttributeValue("refid") ?: return ""
        
        val includeParameters = mutableMapOf<String, String>()
        for (property in tag.findSubTags("property")) {
            val name = property.getAttributeValue("name")
            val value = property.getAttributeValue("value")
            if (name != null && value != null) {
                includeParameters[name] = value
            }
        }
        
        val resolvedRefid = resolveVariables(refid, parameters, includeParameters)
        val sqlFragment = findSqlFragment(template, resolvedRefid)
        
        if (sqlFragment != null) {
            val mergedParameters = parameters.toMutableMap()
            for ((key, value) in includeParameters) {
                mergedParameters[key] = value
            }
            return processXmlTag(sqlFragment, mergedParameters, template)
        }
        
        return ""
    }
    
    private fun processIfTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val test = tag.getAttributeValue("test") ?: return ""
        if (evaluateCondition(test, parameters)) {
            return processChildTags(tag, parameters, template)
        }
        return ""
    }
    
    private fun processForeachTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val collection = tag.getAttributeValue("collection") ?: return ""
        val item = tag.getAttributeValue("item") ?: "item"
        val index = tag.getAttributeValue("index") ?: "index"
        val open = tag.getAttributeValue("open") ?: ""
        val close = tag.getAttributeValue("close") ?: ""
        val separator = tag.getAttributeValue("separator") ?: ","
        
        val collectionValue = parameters[collection]
        if (collectionValue is List<*>) {
            val items = mutableListOf<String>()
            collectionValue.forEachIndexed { idx, value ->
                val itemParams = parameters.toMutableMap()
                itemParams[item] = value ?: ""
                itemParams[index] = idx
                items.add(processChildTags(tag, itemParams, template))
            }
            return open + items.joinToString(separator) + close
        }
        
        return ""
    }
    
    private fun processWhereTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val content = processChildTags(tag, parameters, template).trim()
        if (content.isBlank()) return ""
        
        val processedContent = content
            .replaceFirst("^\\s*AND\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            .replaceFirst("^\\s*OR\\s+".toRegex(RegexOption.IGNORE_CASE), "")
        
        return if (processedContent.isNotBlank()) "WHERE $processedContent" else ""
    }
    
    private fun processSetTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val content = processChildTags(tag, parameters, template).trim()
        if (content.isBlank()) return ""
        
        val processedContent = content.removeSuffix(",").trim()
        return if (processedContent.isNotBlank()) "SET $processedContent" else ""
    }
    
    private fun processChooseTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        for (child in tag.subTags) {
            when (child.name) {
                "when" -> {
                    val test = child.getAttributeValue("test")
                    if (test != null && evaluateCondition(test, parameters)) {
                        return processChildTags(child, parameters, template)
                    }
                }
            }
        }
        
        for (child in tag.subTags) {
            if (child.name == "otherwise") {
                return processChildTags(child, parameters, template)
            }
        }
        
        return ""
    }
    
    private fun processWhenTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val test = tag.getAttributeValue("test") ?: return ""
        if (evaluateCondition(test, parameters)) {
            return processChildTags(tag, parameters, template)
        }
        return ""
    }
    
    private fun processOtherwiseTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        return processChildTags(tag, parameters, template)
    }
    
    private fun processRegularTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val builder = StringBuilder()
        
        // 处理XML值内容（包含文本和子标签，保持原始顺序）
        for (child in tag.value.children) {
            when {
                child is XmlTag -> {
                    builder.append(processXmlTag(child, parameters, template))
                }
                else -> {
                    val text = child.text
                    if (text.isNotBlank()) {
                        builder.append(replaceParameters(text, parameters))
                    }
                }
            }
        }
        
        return builder.toString()
    }
    
    private fun processChildTags(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val builder = StringBuilder()
        
        // 处理XML值内容（包含文本和子标签，保持原始顺序）
        for (child in tag.value.children) {
            when {
                child is XmlTag -> {
                    builder.append(processXmlTag(child, parameters, template))
                }
                else -> {
                    val text = child.text
                    if (text.isNotBlank()) {
                        builder.append(replaceParameters(text, parameters))
                    }
                }
            }
        }
        
        return builder.toString()
    }
    
    private fun findSqlFragment(template: SqlTemplate, fragmentId: String): XmlTag? {
        val rootTag = template.mapperFile.rootTag ?: return null
        
        for (child in rootTag.subTags) {
            if (child.name == "sql" && child.getAttributeValue("id") == fragmentId) {
                return child
            }
        }
        
        return null
    }
    
    private fun resolveVariables(text: String, parameters: Map<String, Any>, includeParameters: Map<String, String>): String {
        var result = text
        
        val pattern = "\\$\\{([^}]+)}".toRegex()
        val matches = pattern.findAll(text).toList()
        
        for (match in matches) {
            val variableName = match.groupValues[1]
            val value = includeParameters[variableName] 
                ?: parameters[variableName]?.toString() 
                ?: match.value
            result = result.replace(match.value, value)
        }
        
        return result
    }
    
    private fun evaluateCondition(condition: String, parameters: Map<String, Any>): Boolean {
        when {
            condition.contains("!=") -> {
                val parts = condition.split("!=").map { it.trim() }
                if (parts.size == 2) {
                    val value = getParameterValue(parts[0], parameters)
                    val expected = parseValue(parts[1])
                    return value != expected
                }
            }
            condition.contains("==") -> {
                val parts = condition.split("==").map { it.trim() }
                if (parts.size == 2) {
                    val value = getParameterValue(parts[0], parameters)
                    val expected = parseValue(parts[1])
                    return value == expected
                }
            }
            condition.contains(" != null") -> {
                val paramName = condition.replace(" != null", "").trim()
                return parameters[paramName] != null
            }
            condition.contains(" == null") -> {
                val paramName = condition.replace(" == null", "").trim()
                return parameters[paramName] == null
            }
            else -> {
                val paramName = condition.trim()
                val value = parameters[paramName]
                return value != null && value.toString().isNotBlank()
            }
        }
        
        return false
    }
    
    private fun getParameterValue(expression: String, parameters: Map<String, Any>): Any? {
        return when {
            expression.startsWith("'") && expression.endsWith("'") -> {
                expression.substring(1, expression.length - 1)
            }
            expression.startsWith("\"") && expression.endsWith("\"") -> {
                expression.substring(1, expression.length - 1)
            }
            else -> parameters[expression]
        }
    }
    
    private fun parseValue(value: String): Any? {
        return when {
            value == "null" -> null
            value.startsWith("'") && value.endsWith("'") -> value.substring(1, value.length - 1)
            value.startsWith("\"") && value.endsWith("\"") -> value.substring(1, value.length - 1)
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            value == "true" -> true
            value == "false" -> false
            else -> value
        }
    }
    
    private fun replaceParameters(text: String, parameters: Map<String, Any>): String {
        var result = text
        
        val paramPattern = "#\\{([^}]+)}".toRegex()
        result = paramPattern.replace(result) { match ->
            val paramName = match.groupValues[1]
            val value = parameters[paramName]
            when (value) {
                is String -> "'$value'"
                is Number -> value.toString()
                is Boolean -> value.toString()
                null -> "NULL"
                else -> "'$value'"
            }
        }
        
        val dollarPattern = "\\$\\{([^}]+)}".toRegex()
        result = dollarPattern.replace(result) { match ->
            val paramName = match.groupValues[1]
            parameters[paramName]?.toString() ?: ""
        }
        
        return result
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