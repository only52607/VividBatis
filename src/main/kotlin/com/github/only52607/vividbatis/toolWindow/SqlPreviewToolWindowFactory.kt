package com.github.only52607.vividbatis.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class SqlPreviewToolWindowFactory : ToolWindowFactory, DumbAware {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val sqlPreviewWindow = SqlPreviewWindow(project)
        val content = ContentFactory.getInstance().createContent(
            sqlPreviewWindow.getContent(), 
            "SQL 预览", 
            false
        )
        toolWindow.contentManager.addContent(content)
        
        // 确保工具窗口在创建后立即可用
        toolWindow.setToHideOnEmptyContent(false)
    }
    
    override fun shouldBeAvailable(project: Project) = true
} 