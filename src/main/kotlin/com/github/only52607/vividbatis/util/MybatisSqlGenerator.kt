package com.github.only52607.vividbatis.util

import com.intellij.psi.xml.XmlTag
import ognl.Ognl
import ognl.OgnlContext
import ognl.OgnlException

class MybatisSqlGenerator {

    fun generateSql(template: SqlTemplate, parameters: Map<String, Any>): String {
        val statementTag = findStatementTag(template)
        val sql = processXmlTag(statementTag, parameters, template)
        return formatSql(sql)
    }
    
    private fun findStatementTag(template: SqlTemplate): XmlTag {
        return MybatisXmlUtils.findStatementTag(template.mapperFile, template.statementId)
            ?: throw RuntimeException("Statement tag not found: ${template.statementId}")
    }
    
    private fun processXmlTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        return when (tag.name) {
            "include" -> processIncludeTag(tag, parameters, template)
            "bind" -> "" // Processed in child tags
            "if" -> processConditionalTag(tag, parameters, template)
            "foreach" -> processForeachTag(tag, parameters, template)
            "trim" -> processTrimTag(tag, parameters, template)
            "where" -> processWhereTag(tag, parameters, template)
            "set" -> processSetTag(tag, parameters, template)
            "choose" -> processChooseTag(tag, parameters, template)
            "when" -> processConditionalTag(tag, parameters, template)
            "otherwise" -> processChildTags(tag, parameters, template)
            else -> processChildTags(tag, parameters, template)
        }
    }
    
    private fun processIncludeTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val refId = tag.getAttributeValue("refid") ?: return ""
        val includeParameters = mutableMapOf<String, String>()
        
        tag.findSubTags("property").forEach { property ->
            val name = property.getAttributeValue("name")
            val value = property.getAttributeValue("value")
            if (name != null && value != null) {
                includeParameters[name] = value
            }
        }
        
        val resolvedRefId = resolveVariables(refId, parameters, includeParameters)
        val sqlFragment = MybatisXmlUtils.findSqlFragmentByRefId(template.project, template.mapperFile, resolvedRefId)
        
        return if (sqlFragment != null) {
            val mergedParameters = parameters.toMutableMap()
            includeParameters.forEach { (key, value) -> mergedParameters[key] = value }
            processXmlTag(sqlFragment, mergedParameters, template)
        } else ""
    }
    
    private fun processConditionalTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val test = tag.getAttributeValue("test") ?: return ""
        return if (evaluateOgnlExpressionForBoolean(test, parameters)) {
            processChildTags(tag, parameters, template)
        } else ""
    }
    
    private fun processForeachTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val collection = tag.getAttributeValue("collection") ?: return ""
        val item = tag.getAttributeValue("item") ?: "item"
        val index = tag.getAttributeValue("index") ?: "index"
        val open = tag.getAttributeValue("open") ?: ""
        val close = tag.getAttributeValue("close") ?: ""
        val separator = tag.getAttributeValue("separator") ?: ","
        
        val collectionValue = parameters[collection] as? List<*> ?: return ""
        val items = collectionValue.mapIndexed { idx, value ->
            val itemParams = parameters.toMutableMap()
            itemParams[item] = value ?: ""
            itemParams[index] = idx
            processChildTags(tag, itemParams, template)
        }
        
        return open + items.joinToString(separator) + close
    }

    private fun processTrimTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val content = processChildTags(tag, parameters, template).trim()
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

    private fun processWhereTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val content = processChildTags(tag, parameters, template).trim()
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
    
    private fun processSetTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val content = processChildTags(tag, parameters, template).trim()
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
    
    private fun processChooseTag(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        tag.subTags.forEach { child ->
            if (child.name == "when") {
                val test = child.getAttributeValue("test")
                if (test != null && evaluateOgnlExpressionForBoolean(test, parameters)) {
                    return processChildTags(child, parameters, template)
                }
            }
        }
        
        return tag.subTags.find { it.name == "otherwise" }?.let { 
            processChildTags(it, parameters, template) 
        } ?: ""
    }
    
    private fun processChildTags(tag: XmlTag, parameters: Map<String, Any>, template: SqlTemplate): String {
        val builder = StringBuilder()
        val mutableParams = parameters.toMutableMap()
        
        tag.value.children.forEach { child ->
            when {
                child is XmlTag -> {
                    if (child.name == "bind") {
                        val name = child.getAttributeValue("name")
                        val value = child.getAttributeValue("value")
                        if (name != null && value != null) {
                            val bindValue = evaluateOgnlExpressionForValue(value, mutableParams)
                            mutableParams[name] = bindValue
                        }
                    } else {
                        builder.append(processXmlTag(child, mutableParams, template))
                    }
                }
                else -> {
                    val text = child.text
                    if (text.isNotBlank()) {
                        builder.append(replaceParameters(text, mutableParams))
                    }
                }
            }
        }
        
        return builder.toString()
    }
    
    private fun resolveVariables(text: String, parameters: Map<String, Any>, includeParameters: Map<String, String>): String {
        var result = text
        val pattern = "\\$\\{([^}]+)}".toRegex()
        
        pattern.findAll(text).forEach { match ->
            val variableName = match.groupValues[1]
            val value = includeParameters[variableName] 
                ?: parameters[variableName]?.toString() 
                ?: match.value
            result = result.replace(match.value, value)
        }
        
        return result
    }
    
    private fun evaluateOgnlExpressionForBoolean(expression: String, parameters: Map<String, Any>): Boolean {
        return try {
            val context = OgnlContext(null, null, null)
            val root = HashMap<String, Any>()
            parameters.forEach { (key, value) -> root[key] = value }
            
            val result = Ognl.getValue(expression, context, root)
            
            when (result) {
                is Boolean -> result
                is Number -> result.toDouble() != 0.0
                is String -> result.isNotEmpty()
                null -> false
                else -> true
            }
        } catch (e: OgnlException) {
            false
        }
    }
    
    private fun evaluateOgnlExpressionForValue(expression: String, parameters: Map<String, Any>): Any {
        return try {
            val context = OgnlContext(null, null, null)
            val root = HashMap<String, Any>()
            parameters.forEach { (key, value) -> root[key] = value }
            
            Ognl.getValue(expression, context, root) ?: ""
        } catch (e: OgnlException) {
            throw RuntimeException("Error evaluating OGNL expression: $expression", e)
        }
    }
    
    private fun replaceParameters(text: String, parameters: Map<String, Any>): String {
        var result = text
        
        result = "#\\{([^}]+)}".toRegex().replace(result) { match ->
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
        
        result = "\\$\\{([^}]+)}".toRegex().replace(result) { match ->
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