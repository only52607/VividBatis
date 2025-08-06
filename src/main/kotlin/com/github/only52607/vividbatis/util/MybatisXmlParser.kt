package com.github.only52607.vividbatis.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

class MybatisXmlParser {
    
    fun getParameterType(project: Project, namespace: String, statementId: String): String? {
        val xmlFile = findMapperXmlFile(project, namespace) ?: return null
        val statementTag = findStatementTag(xmlFile, statementId) ?: return null
        return statementTag.getAttributeValue("parameterType")
    }
    
    fun getSqlTemplate(project: Project, namespace: String, statementId: String): SqlTemplate? {
        val xmlFile = findMapperXmlFile(project, namespace) ?: return null
        val statementTag = findStatementTag(xmlFile, statementId) ?: return null
        
        return SqlTemplate(
            namespace = namespace,
            statementId = statementId,
            statementType = statementTag.name,
            sqlContent = extractSqlContent(statementTag),
            includes = extractIncludes(statementTag),
            mapperFile = xmlFile
        )
    }
    
    private fun findMapperXmlFile(project: Project, namespace: String): XmlFile? {
        val psiManager = PsiManager.getInstance(project)
        val xmlFiles = FilenameIndex.getAllFilesByExt(project, "xml", GlobalSearchScope.projectScope(project))
        
        for (virtualFile in xmlFiles) {
            val psiFile = psiManager.findFile(virtualFile) as? XmlFile ?: continue
            val rootTag = psiFile.rootTag ?: continue
            
            if (rootTag.name == "mapper" && rootTag.getAttributeValue("namespace") == namespace) {
                return psiFile
            }
        }
        return null
    }
    
    private fun findStatementTag(xmlFile: XmlFile, statementId: String): XmlTag? {
        val rootTag = xmlFile.rootTag ?: return null
        
        for (child in rootTag.subTags) {
            if (child.name in setOf("select", "insert", "update", "delete") &&
                child.getAttributeValue("id") == statementId) {
                return child
            }
        }
        return null
    }
    
    private fun extractSqlContent(statementTag: XmlTag): String {
        return statementTag.value.text.trim()
    }
    
    private fun extractIncludes(statementTag: XmlTag): List<String> {
        val includes = mutableListOf<String>()
        val includeElements = statementTag.findSubTags("include")
        
        for (includeElement in includeElements) {
            val refId = includeElement.getAttributeValue("refid")
            if (refId != null) {
                includes.add(refId)
            }
        }
        return includes
    }
    
    fun getSqlFragment(project: Project, namespace: String, fragmentId: String): String? {
        val xmlFile = findMapperXmlFile(project, namespace) ?: return null
        val rootTag = xmlFile.rootTag ?: return null
        
        for (child in rootTag.subTags) {
            if (child.name == "sql" && child.getAttributeValue("id") == fragmentId) {
                return child.value.text.trim()
            }
        }
        return null
    }
}

data class SqlTemplate(
    val namespace: String,
    val statementId: String,
    val statementType: String,
    val sqlContent: String,
    val includes: List<String>,
    val mapperFile: XmlFile
) 