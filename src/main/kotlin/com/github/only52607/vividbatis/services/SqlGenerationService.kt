package com.github.only52607.vividbatis.services

import com.github.only52607.vividbatis.model.ExtendedRootObject
import com.github.only52607.vividbatis.model.StatementQualifyId
import com.github.only52607.vividbatis.util.OgnlRootObject
import com.github.only52607.vividbatis.util.SqlTemplate
import com.github.only52607.vividbatis.util.findMybatisMapperXml
import com.github.only52607.vividbatis.util.findMybatisStatementById
import com.github.only52607.vividbatis.util.findSqlFragmentByRefId
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlTag
import ognl.Ognl
import ognl.OgnlContext

@Service
class SqlGenerationService(private val project: Project) {
    private val parameterService = project.getService(ParameterAnalysisService::class.java)
    private val gson = Gson()

    fun generateSql(statementQualifyId: StatementQualifyId, parameterJson: String): String {
        val parameterInfo = parameterService.getStatementParameterInfo(statementQualifyId)
        val rootObject = parameterInfo.createRootObject(gson.fromJson(parameterJson, JsonElement::class.java), gson)
        return generateSql(statementQualifyId, rootObject)
    }

    fun generateSql(statementQualifyId: StatementQualifyId, rootObject: OgnlRootObject): String {
        val template = buildSqlTemplate(statementQualifyId.namespace, statementQualifyId.statementId)
            ?: throw RuntimeException("未找到语句: $statementQualifyId")
        val sql = processXmlTag(
            findStatementTag(template),
            Ognl.createDefaultContext(rootObject) as OgnlContext,
            template
        )
        return formatSql(sql)
    }

    private fun buildSqlTemplate(namespace: String, statementId: String): SqlTemplate? {
        val xmlFile = project.findMybatisMapperXml(namespace) ?: return null
        val statementTag = xmlFile.findMybatisStatementById(statementId) ?: return null
        return SqlTemplate(
            namespace = namespace,
            statementId = statementId,
            statementType = statementTag.name,
            mapperFile = xmlFile,
            project = project
        )
    }

    private fun findStatementTag(template: SqlTemplate): XmlTag {
        return template.mapperFile.findMybatisStatementById(template.statementId)
            ?: throw RuntimeException("Statement tag not found: ${template.statementId}")
    }

    private fun processXmlTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
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

    private fun processIncludeTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
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

    private fun processConditionalTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
        val test = tag.getAttributeValue("test") ?: return ""
        return if (Ognl.getValue(test, context, context.root).asBoolean()) {
            processChildTags(tag, context, template)
        } else ""
    }

    private fun processForeachTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
        val collection = tag.getAttributeValue("collection") ?: return ""
        val item = tag.getAttributeValue("item") ?: "item"
        val index = tag.getAttributeValue("index") ?: "index"
        val open = tag.getAttributeValue("open") ?: ""
        val close = tag.getAttributeValue("close") ?: ""
        val separator = tag.getAttributeValue("separator") ?: ","

        val collectionValue = Ognl.getValue(collection, context, context.root)

        val collectionList = when (collectionValue) {
            is List<*> -> collectionValue
            is Array<*> -> collectionValue.toList()
            is Set<*> -> collectionValue.toList()
            is Collection<*> -> collectionValue.toList()
            else -> return ""
        }

        val items = collectionList.mapIndexed { idx, value ->
            val originRoot = context.root
            context.root = ExtendedRootObject(context.root, mapOf(item to value, index to idx))
            processChildTags(tag, context, template)
            context.root = originRoot
        }

        return open + items.joinToString(separator) + close
    }

    private fun processTrimTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
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
            suffixOverrides = suffixOverrides
        )
    }

    private fun processWhereTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
        val content = processChildTags(tag, context, template).trim()
        if (content.isBlank()) return ""

        return applyTrim(
            content = content,
            prefix = "WHERE",
            suffix = "",
            prefixOverrides = listOf("AND ", "OR "),
            suffixOverrides = emptyList()
        )
    }

    private fun processSetTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
        val content = processChildTags(tag, context, template).trim()
        if (content.isBlank()) return ""

        return applyTrim(
            content = content,
            prefix = "SET",
            suffix = "",
            prefixOverrides = emptyList(),
            suffixOverrides = listOf(",")
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
        suffixOverrides: List<String>
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
            "$prefix $working"
        } else working
        val withSuffix = if (suffix.isNotEmpty()) withPrefix + suffix else withPrefix
        return withSuffix
    }

    private fun processChooseTag(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
        tag.subTags.forEach { child ->
            if (child.name == "when") {
                val test = child.getAttributeValue("test")
                if (test != null && Ognl.getValue(test, context, context.root).asBoolean()) {
                    return processChildTags(child, context, template)
                }
            }
        }

        return tag.subTags.find { it.name == "otherwise" }?.let {
            processChildTags(it, context, template)
        } ?: ""
    }

    private fun processChildTags(tag: XmlTag, context: OgnlContext, template: SqlTemplate): String {
        val builder = StringBuilder()
        var currentContext = context
        var currentRoot = context.root

        tag.value.children.forEach { child ->
            when {
                child is XmlTag -> {
                    if (child.name == "bind") {
                        val name = child.getAttributeValue("name")
                        val value = child.getAttributeValue("value")
                        if (name != null && value != null) {
                            val bindValue = Ognl.getValue(value, context, context.root)
                            currentRoot = ExtendedRootObject(currentRoot, mapOf(name to bindValue))
                            currentContext = Ognl.createDefaultContext(currentRoot) as OgnlContext
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

    private fun resolveVariables(text: String, context: OgnlContext, includeParameters: Map<String, String>): String {
        var result = text
        val pattern = "\\$\\{([^}]+)}".toRegex()

        pattern.findAll(text).forEach { match ->
            val variableName = match.groupValues[1]
            val value = includeParameters[variableName]
                ?: Ognl.getValue(variableName, context, context.root).toString()
            result = result.replace(match.value, value)
        }

        return result
    }

    private fun replaceParameters(text: String, context: OgnlContext): String {
        var result = text

        result = "#\\{([^}]+)}".toRegex().replace(result) { match ->
            val parts = match.groupValues[1].trim().split(",").map(String::trim)
            val paramName = parts.first()
            val properties = parts.drop(1).associate {
                val keyValue = it.split("=")
                keyValue[0].trim() to keyValue.getOrNull(1)?.trim()
            }
            val value = Ognl.getValue(paramName, context, context.root).toString()
            when (value) {
                else -> "'$value'"
            }
        }

        result = "\\$\\{([^}]+)}".toRegex().replace(result) { match ->
            val paramName = match.groupValues[1]
            Ognl.getValue(paramName, context, context.root).toString()
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

    fun Any?.asBoolean(): Boolean {
        return when (this) {
            is Boolean -> this
            is Number -> this.toDouble() != 0.0
            is String -> this.isNotEmpty()
            is Collection<*> -> this.isNotEmpty()
            is Array<*> -> this.isNotEmpty()
            null -> false
            else -> true
        }
    }
}