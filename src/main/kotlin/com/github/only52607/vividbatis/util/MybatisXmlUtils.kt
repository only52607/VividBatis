package com.github.only52607.vividbatis.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag

// Top-level MyBatis XML constants
val SUPPORTED_STATEMENTS = setOf("select", "insert", "update", "delete")

// Project-level extensions
fun Project.findMybatisMapperXml(namespace: String): XmlFile? {
    val psiManager = PsiManager.getInstance(this)
    val xmlFiles = FilenameIndex.getAllFilesByExt(this, "xml", GlobalSearchScope.projectScope(this))
    for (virtualFile in xmlFiles) {
        val psiFile = psiManager.findFile(virtualFile) as? XmlFile ?: continue
        val rootTag = psiFile.rootTag ?: continue
        if (rootTag.name == "mapper" && rootTag.getAttributeValue("namespace") == namespace) {
            return psiFile
        }
    }
    return null
}

// XmlFile-level extensions
fun XmlFile.findMybatisStatementById(statementId: String): XmlTag? {
    val rootTag = this.rootTag ?: return null
    return rootTag.subTags.find { it.name in SUPPORTED_STATEMENTS && it.getAttributeValue("id") == statementId }
}

fun XmlFile.findSqlFragmentById(fragmentId: String): XmlTag? {
    val rootTag = this.rootTag ?: return null
    return rootTag.subTags.find { it.name == "sql" && it.getAttributeValue("id") == fragmentId }
}

fun XmlFile.findSqlFragmentByRefId(refId: String): XmlTag? {
    val dotIndex = refId.lastIndexOf('.')
    return if (dotIndex > 0) {
        val namespace = refId.substring(0, dotIndex)
        val fragmentId = refId.substring(dotIndex + 1)
        val targetXmlFile = this.project.findMybatisMapperXml(namespace)
        if (targetXmlFile != null) {
            targetXmlFile.findSqlFragmentById(fragmentId)
        } else null
    } else {
        this.findSqlFragmentById(refId)
    }
}

// XmlTag-level extensions
fun XmlTag.findMapperNamespace(): String? {
    var current = this.parent
    while (current != null) {
        if (current is XmlTag && current.name == "mapper") {
            return current.getAttributeValue("namespace")
        }
        current = current.parent
    }
    return null
}

fun XmlTag.isInMybatisMapperFile(): Boolean {
    var current: XmlTag = this
    while (current.parentTag != null) {
        current = current.parentTag!!
    }
    return current.name == "mapper" && current.getAttributeValue("namespace") != null
}