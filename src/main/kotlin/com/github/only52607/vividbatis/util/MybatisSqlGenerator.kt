package com.github.only52607.vividbatis.util

import java.util.regex.Pattern

class MybatisSqlGenerator {
    
    companion object {
        private val PARAMETER_PATTERN = Pattern.compile("#\\{([^}]+)}")
        private val DOLLAR_PARAMETER_PATTERN = Pattern.compile("\\$\\{([^}]+)}")
    }
    
    fun generateSql(template: SqlTemplate, parameters: Map<String, Any>): String {
        var sql = template.sqlContent
        sql = processIncludes(sql, template, parameters)
        sql = processDynamicTags(sql, parameters)
        sql = replaceParameters(sql, parameters)
        return formatSql(sql)
    }
    
    private fun processIncludes(sql: String, template: SqlTemplate, parameters: Map<String, Any>): String {
        var processedSql = sql
        
        for (includeId in template.includes) {
            val mybatisXmlParser = MybatisXmlParser()
            val fragmentContent = mybatisXmlParser.getSqlFragment(
                template.mapperFile.project,
                template.namespace,
                includeId
            )
            
            if (fragmentContent != null) {
                val includePattern = Pattern.compile("<include\\s+refid\\s*=\\s*[\"']$includeId[\"']\\s*/>")
                processedSql = includePattern.matcher(processedSql).replaceAll(fragmentContent)
            }
        }
        
        return processedSql
    }
    
    private fun processDynamicTags(sql: String, parameters: Map<String, Any>): String {
        var processedSql = sql
        processedSql = processIfTags(processedSql, parameters)
        processedSql = processForeachTags(processedSql, parameters)
        processedSql = processWhereTags(processedSql)
        processedSql = processSetTags(processedSql)
        processedSql = processChooseTags(processedSql, parameters)
        return processedSql
    }
    
    private fun processIfTags(sql: String, parameters: Map<String, Any>): String {
        val ifPattern = Pattern.compile("<if\\s+test\\s*=\\s*[\"']([^\"']+)[\"']\\s*>(.*?)</if>", Pattern.DOTALL)
        val matcher = ifPattern.matcher(sql)
        val result = StringBuffer()
        
        while (matcher.find()) {
            val condition = matcher.group(1)
            val content = matcher.group(2)
            
            val shouldInclude = evaluateCondition(condition, parameters)
            val replacement = if (shouldInclude) content else ""
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
    
    private fun processForeachTags(sql: String, parameters: Map<String, Any>): String {
        val foreachPattern = Pattern.compile(
            "<foreach\\s+collection\\s*=\\s*[\"']([^\"']+)[\"']\\s+" +
            "item\\s*=\\s*[\"']([^\"']+)[\"']\\s*" +
            "(?:index\\s*=\\s*[\"']([^\"']+)[\"']\\s*)?" +
            "(?:open\\s*=\\s*[\"']([^\"']*)[\"']\\s*)?" +
            "(?:close\\s*=\\s*[\"']([^\"']*)[\"']\\s*)?" +
            "(?:separator\\s*=\\s*[\"']([^\"']*)[\"']\\s*)?>" +
            "(.*?)</foreach>", 
            Pattern.DOTALL
        )
        
        val matcher = foreachPattern.matcher(sql)
        val result = StringBuffer()
        
        while (matcher.find()) {
            val collection = matcher.group(1)
            val item = matcher.group(2)
            val index = matcher.group(3) ?: "index"
            val open = matcher.group(4) ?: ""
            val close = matcher.group(5) ?: ""
            val separator = matcher.group(6) ?: ","
            val content = matcher.group(7)
            
            val collectionValue = parameters[collection]
            val replacement = if (collectionValue is List<*>) {
                val items = mutableListOf<String>()
                collectionValue.forEachIndexed { idx, value ->
                    val itemParams = parameters.toMutableMap()
                    itemParams[item] = value ?: ""
                    itemParams[index] = idx
                    items.add(replaceParameters(content, itemParams))
                }
                open + items.joinToString(separator) + close
            } else {
                ""
            }
            
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
    
    private fun processWhereTags(sql: String): String {
        val wherePattern = Pattern.compile("<where\\s*>(.*?)</where>", Pattern.DOTALL)
        val matcher = wherePattern.matcher(sql)
        val result = StringBuffer()
        
        while (matcher.find()) {
            val content = matcher.group(1).trim()
            val processedContent = content
                .replaceFirst("^\\s*AND\\s+".toRegex(RegexOption.IGNORE_CASE), "")
                .replaceFirst("^\\s*OR\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            
            val replacement = if (processedContent.isNotBlank()) {
                "WHERE $processedContent"
            } else {
                ""
            }
            
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
    
    private fun processSetTags(sql: String): String {
        val setPattern = Pattern.compile("<set\\s*>(.*?)</set>", Pattern.DOTALL)
        val matcher = setPattern.matcher(sql)
        val result = StringBuffer()
        
        while (matcher.find()) {
            val content = matcher.group(1).trim()
            val processedContent = content.removeSuffix(",")
            
            val replacement = if (processedContent.isNotBlank()) {
                "SET $processedContent"
            } else {
                ""
            }
            
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
    
    private fun processChooseTags(sql: String, parameters: Map<String, Any>): String {
        val choosePattern = Pattern.compile(
            "<choose\\s*>(.*?)</choose>", 
            Pattern.DOTALL
        )
        
        val matcher = choosePattern.matcher(sql)
        val result = StringBuffer()
        
        while (matcher.find()) {
            val chooseContent = matcher.group(1)
            val replacement = processChooseContent(chooseContent, parameters)
            matcher.appendReplacement(result, replacement)
        }
        matcher.appendTail(result)
        
        return result.toString()
    }
    
    private fun processChooseContent(content: String, parameters: Map<String, Any>): String {
        val whenPattern = Pattern.compile("<when\\s+test\\s*=\\s*[\"']([^\"']+)[\"']\\s*>(.*?)</when>", Pattern.DOTALL)
        val otherwisePattern = Pattern.compile("<otherwise\\s*>(.*?)</otherwise>", Pattern.DOTALL)
        
        val whenMatcher = whenPattern.matcher(content)
        
        while (whenMatcher.find()) {
            val condition = whenMatcher.group(1)
            val whenContent = whenMatcher.group(2)
            
            if (evaluateCondition(condition, parameters)) {
                return whenContent
            }
        }
        
        val otherwiseMatcher = otherwisePattern.matcher(content)
        if (otherwiseMatcher.find()) {
            return otherwiseMatcher.group(1)
        }
        
        return ""
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
    
    private fun replaceParameters(sql: String, parameters: Map<String, Any>): String {
        var result = sql
        
        val paramMatcher = PARAMETER_PATTERN.matcher(result)
        val paramResult = StringBuffer()
        
        while (paramMatcher.find()) {
            val paramName = paramMatcher.group(1)
            val value = parameters[paramName]
            val replacement = when (value) {
                is String -> "'$value'"
                is Number -> value.toString()
                is Boolean -> value.toString()
                null -> "NULL"
                else -> "'$value'"
            }
            paramMatcher.appendReplacement(paramResult, replacement)
        }
        paramMatcher.appendTail(paramResult)
        result = paramResult.toString()
        
        val dollarMatcher = DOLLAR_PARAMETER_PATTERN.matcher(result)
        val dollarResult = StringBuffer()
        
        while (dollarMatcher.find()) {
            val paramName = dollarMatcher.group(1)
            val value = parameters[paramName]?.toString() ?: ""
            dollarMatcher.appendReplacement(dollarResult, value)
        }
        dollarMatcher.appendTail(dollarResult)
        
        return dollarResult.toString()
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