package com.github.only52607.vividbatis.util

import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class MybatisXmlParser {
    
    fun getParameterType(project: Project, namespace: String, statementId: String): String? {
        val xmlFile = project.findMybatisMapperXml(namespace) ?: return null
        val statementTag = xmlFile.findMybatisStatementById(statementId) ?: return null
        return statementTag.getAttributeValue("parameterType")
    }
    
    fun getSqlTemplate(project: Project, namespace: String, statementId: String): SqlTemplate? {
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
}

data class SqlTemplate(
    val namespace: String,
    val statementId: String,
    val statementType: String,
    val mapperFile: XmlFile,
    val project: Project
) 