package com.github.only52607.vividbatis.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * SQL 预览工具窗口工厂
 */
class SqlPreviewToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val sqlPreviewWindow = SqlPreviewWindow(project)
        val content = ContentFactory.getInstance().createContent(
            sqlPreviewWindow.getContent(), 
            "SQL 预览", 
            false
        )
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project) = true
} 