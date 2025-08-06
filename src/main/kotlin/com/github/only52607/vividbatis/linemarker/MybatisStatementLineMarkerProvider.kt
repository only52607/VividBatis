package com.github.only52607.vividbatis.linemarker

import com.github.only52607.vividbatis.message.SqlStatementSelectedEvent
import com.github.only52607.vividbatis.message.SqlStatementSelectedListener
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import javax.swing.Icon

class MybatisStatementLineMarkerProvider : LineMarkerProvider {
    
    companion object {
        private val SUPPORTED_STATEMENTS = setOf("select", "insert", "update", "delete")
        private val SQL_ICON: Icon = AllIcons.Nodes.DataTables
    }
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is XmlTag) return null
        val tagName = element.name
        if (tagName !in SUPPORTED_STATEMENTS) return null
        if (!isMybatisMapperFile(element)) return null
        val statementId = element.getAttributeValue("id") ?: return null
        
        return LineMarkerInfo(
            element,
            element.textRange,
            SQL_ICON,
            { "预览 SQL: $statementId" },
            { _, psiElement -> handleIconClick(psiElement as XmlTag) },
            GutterIconRenderer.Alignment.LEFT,
            { "预览 SQL: $statementId" }
        )
    }
    
    private fun isMybatisMapperFile(element: XmlTag): Boolean {
        val rootTag = element.parentTag
        if (rootTag is XmlTag) {
            return rootTag.name == "mapper" && rootTag.getAttributeValue("namespace") != null
        }
        return false
    }
    
    private fun handleIconClick(xmlTag: XmlTag) {
        val namespace = getMapperNamespace(xmlTag) ?: return
        val statementId = xmlTag.getAttributeValue("id") ?: return
        val statementType = xmlTag.name
        val xmlFilePath = xmlTag.containingFile.virtualFile?.path ?: return
        
        val event = SqlStatementSelectedEvent(namespace, statementId, statementType, xmlFilePath)
        val project = xmlTag.project
        project.messageBus.syncPublisher(SqlStatementSelectedListener.TOPIC).onStatementSelected(event)
    }
    
    private fun getMapperNamespace(xmlTag: XmlTag): String? {
        var current = xmlTag.parent
        while (current != null) {
            if (current is XmlTag && current.name == "mapper") {
                return current.getAttributeValue("namespace")
            }
            current = current.parent
        }
        return null
    }
} 