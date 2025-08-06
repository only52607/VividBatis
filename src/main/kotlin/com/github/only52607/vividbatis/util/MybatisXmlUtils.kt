package com.github.only52607.vividbatis.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

object MybatisXmlUtils {
    val SUPPORTED_STATEMENTS = setOf("select", "insert", "update", "delete")

    fun findMapperXmlFile(project: Project, namespace: String): XmlFile? {
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

    fun findStatementTag(xmlFile: XmlFile, statementId: String): XmlTag? {
        val rootTag = xmlFile.rootTag ?: return null
        return rootTag.subTags.find { 
            it.name in SUPPORTED_STATEMENTS && it.getAttributeValue("id") == statementId 
        }
    }

    fun findSqlFragment(xmlFile: XmlFile, fragmentId: String): XmlTag? {
        val rootTag = xmlFile.rootTag ?: return null
        return rootTag.subTags.find { 
            it.name == "sql" && it.getAttributeValue("id") == fragmentId 
        }
    }

    fun getMapperNamespace(xmlTag: XmlTag): String? {
        var current = xmlTag.parent
        while (current != null) {
            if (current is XmlTag && current.name == "mapper") {
                return current.getAttributeValue("namespace")
            }
            current = current.parent
        }
        return null
    }

    fun isMybatisMapperFile(element: XmlTag): Boolean {
        val rootTag = element.parentTag
        return rootTag is XmlTag && rootTag.name == "mapper" && rootTag.getAttributeValue("namespace") != null
    }
} 