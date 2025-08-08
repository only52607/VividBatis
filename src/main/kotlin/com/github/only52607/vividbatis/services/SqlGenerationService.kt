package com.github.only52607.vividbatis.services

import com.github.only52607.vividbatis.util.MybatisSqlGenerator
import com.github.only52607.vividbatis.util.SqlTemplate
import com.github.only52607.vividbatis.util.findMybatisMapperXml
import com.github.only52607.vividbatis.util.findMybatisStatementById
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.google.gson.Gson
import com.google.gson.JsonElement

@Service
class SqlGenerationService(private val project: Project) {
    
    companion object {
        fun getInstance(project: Project): SqlGenerationService {
            return project.getService(SqlGenerationService::class.java)
        }
    }
    
    private val mybatisSqlGenerator = MybatisSqlGenerator()
    private val gson = Gson()
    
    fun generateSql(namespace: String, statementId: String, parameterJson: String): String {
        val parameters = parseParameterJson(parameterJson)
        val sqlTemplate = buildSqlTemplate(namespace, statementId)
            ?: throw RuntimeException("未找到语句: $namespace.$statementId")
        return mybatisSqlGenerator.generateSql(sqlTemplate, parameters)
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
    
    private fun parseParameterJson(json: String): Map<String, Any> {
        if (json.isBlank()) return emptyMap()
        
        return try {
            val jsonElement = gson.fromJson(json, JsonElement::class.java)
            flattenJsonObject(jsonElement)
        } catch (e: Exception) {
            throw RuntimeException("JSON 解析失败: ${e.message}", e)
        }
    }
    
    private fun flattenJsonObject(element: JsonElement): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        when {
            element.isJsonObject -> {
                val jsonObject = element.asJsonObject
                jsonObject.entrySet().forEach { (key, value) ->
                    when {
                        value.isJsonPrimitive -> {
                            val primitive = value.asJsonPrimitive
                            result[key] = when {
                                primitive.isString -> primitive.asString
                                primitive.isNumber -> primitive.asNumber
                                primitive.isBoolean -> primitive.asBoolean
                                else -> primitive.asString
                            }
                        }
                        value.isJsonObject -> {
                            val nested = flattenJsonObject(value)
                            nested.forEach { (nestedKey, nestedValue) ->
                                result["$key.$nestedKey"] = nestedValue
                            }
                        }
                        value.isJsonArray -> {
                            result[key] = value.asJsonArray.map { flattenJsonObject(it) }
                        }
                    }
                }
            }
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                result["value"] = when {
                    primitive.isString -> primitive.asString
                    primitive.isNumber -> primitive.asNumber
                    primitive.isBoolean -> primitive.asBoolean
                    else -> primitive.asString
                }
            }
        }
        
        return result
    }
} 