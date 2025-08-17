package com.github.only52607.vividbatis.mybatis.linemarker

import com.github.only52607.vividbatis.model.StatementPath
import com.github.only52607.vividbatis.mybatis.toolwindow.SqlPreviewWindow
import com.github.only52607.vividbatis.mybatis.util.SUPPORTED_STATEMENTS
import com.github.only52607.vividbatis.mybatis.util.findMapperNamespace
import com.github.only52607.vividbatis.mybatis.util.isInMybatisMapperFile
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag

class MybatisStatementLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is XmlTag) return null
        if (element.name !in SUPPORTED_STATEMENTS) return null
        if (!element.isInMybatisMapperFile()) return null
        val statementId = element.getAttributeValue("id") ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Actions.Preview,
            { "预览 SQL: $statementId" },
            { _, psiElement -> handleIconClick(psiElement as XmlTag) },
            GutterIconRenderer.Alignment.LEFT,
            { "预览 SQL: $statementId" }
        )
    }

    private fun handleIconClick(xmlTag: XmlTag) {
        val project = xmlTag.project
        runReadAction {
            val namespace = xmlTag.findMapperNamespace() ?: return@runReadAction
            val statementId = xmlTag.getAttributeValue("id") ?: return@runReadAction
            val statementType = xmlTag.name

            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("VividBatis") ?: return@runReadAction
            toolWindow.show {
                val content = toolWindow.contentManager.getContent(0) ?: return@show
                val previewWindow = content.getUserData(SqlPreviewWindow.PREVIEW_WINDOW_KEY)
                previewWindow?.processStatementSelection(StatementPath(namespace, statementId), statementType)
            }
        }
    }
}