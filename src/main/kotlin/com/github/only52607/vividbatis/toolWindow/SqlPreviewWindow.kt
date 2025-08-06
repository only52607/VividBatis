package com.github.only52607.vividbatis.toolWindow

import com.github.only52607.vividbatis.message.SqlStatementSelectedEvent
import com.github.only52607.vividbatis.message.SqlStatementSelectedListener
import com.github.only52607.vividbatis.services.ParameterAnalysisService
import com.github.only52607.vividbatis.services.SqlGenerationService
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JPanel

/**
 * SQL 预览窗口
 */
class SqlPreviewWindow(private val project: Project) : SqlStatementSelectedListener {
    
    private val parameterAnalysisService = ParameterAnalysisService.getInstance(project)
    private val sqlGenerationService = SqlGenerationService.getInstance(project)
    
    private val statementInfoLabel = JBLabel("请选择一个 SQL 语句标签")
    private val parameterEditor = EditorTextField("", project, PlainTextFileType.INSTANCE)
    private val sqlEditor = EditorTextField("", project, PlainTextFileType.INSTANCE)
    private val generateButton = JButton("生成 SQL")
    
    private var currentEvent: SqlStatementSelectedEvent? = null
    
    init {
        setupUI()
        setupEventHandlers()
        
        // 订阅事件
        project.messageBus.connect().subscribe(SqlStatementSelectedListener.TOPIC, this)
    }
    
    private fun setupUI() {
        parameterEditor.preferredSize = Dimension(400, 200)
        parameterEditor.isViewer = false
        
        sqlEditor.preferredSize = Dimension(400, 200)
        sqlEditor.isViewer = true
        
        generateButton.isEnabled = false
    }
    
    private fun setupEventHandlers() {
        generateButton.addActionListener {
            currentEvent?.let { event ->
                try {
                    val parameterJson = parameterEditor.text
                    val generatedSql = sqlGenerationService.generateSql(
                        event.namespace,
                        event.statementId,
                        parameterJson
                    )
                    sqlEditor.text = generatedSql
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        project,
                        "生成 SQL 失败: ${e.message}",
                        "错误"
                    )
                }
            }
        }
    }
    
    fun getContent(): JPanel {
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // 顶部信息面板
        val infoPanel = createInfoPanel()
        mainPanel.add(infoPanel, BorderLayout.NORTH)
        
        // 中间分割面板
        val splitter = JBSplitter(true, 0.5f)
        
        // 参数编辑器面板
        val parameterPanel = createParameterPanel()
        splitter.firstComponent = parameterPanel
        
        // SQL 结果面板
        val sqlPanel = createSqlPanel()
        splitter.secondComponent = sqlPanel
        
        mainPanel.add(splitter, BorderLayout.CENTER)
        
        return mainPanel
    }
    
    private fun createInfoPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints()
        
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = Insets(10, 10, 10, 10)
        panel.add(statementInfoLabel, gbc)
        
        gbc.gridx = 1
        gbc.anchor = GridBagConstraints.EAST
        gbc.weightx = 1.0
        panel.add(generateButton, gbc)
        
        return panel
    }
    
    private fun createParameterPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(JBLabel("参数 (JSON 格式)"), BorderLayout.NORTH)
        panel.add(JBScrollPane(parameterEditor), BorderLayout.CENTER)
        return panel
    }
    
    private fun createSqlPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(JBLabel("生成的 SQL"), BorderLayout.NORTH)
        panel.add(JBScrollPane(sqlEditor), BorderLayout.CENTER)
        return panel
    }
    
    override fun onStatementSelected(event: SqlStatementSelectedEvent) {
        // 激活工具窗口（避免通过消息总线再次发布事件）
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("VividBatis")
        toolWindow?.activate(null)
        
        currentEvent = event
        
        // 更新界面信息
        statementInfoLabel.text = "命名空间: ${event.namespace}, 语句ID: ${event.statementId}, 类型: ${event.statementType}"
        
        // 分析参数类型并生成默认 JSON
        try {
            val defaultJson = parameterAnalysisService.generateDefaultParameterJson(
                event.namespace,
                event.statementId
            )
            parameterEditor.text = defaultJson
            generateButton.isEnabled = true
        } catch (e: Exception) {
            parameterEditor.text = "// 无法分析参数类型: ${e.message}"
            generateButton.isEnabled = false
        }
        
        // 清空 SQL 结果
        sqlEditor.text = ""
    }
} 