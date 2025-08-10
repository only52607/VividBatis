package com.github.only52607.vividbatis.toolWindow

import com.github.only52607.vividbatis.message.SqlStatementSelectedEvent
import com.github.only52607.vividbatis.message.SqlStatementSelectedListener
import com.github.only52607.vividbatis.model.StatementQualifyId
import com.github.only52607.vividbatis.services.ParameterAnalysisService
import com.github.only52607.vividbatis.services.SqlGenerationService
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextArea
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane

class SqlPreviewWindow(private val project: Project) : SqlStatementSelectedListener {

    private val parameterAnalysisService = project.getService(ParameterAnalysisService::class.java)
    private val sqlGenerationService = project.getService(SqlGenerationService::class.java)

    private val statementInfoLabel = JBLabel("请选择一个 SQL 语句标签")
    private val parameterEditor = createJsonEditor()
    private val sqlEditor = createSqlEditor()
    private val generateButton = JButton("预览 SQL")
    private val errorTextArea = JBTextArea()
    private val errorScrollPane = JScrollPane(errorTextArea)

    private var mainSplitter: JBSplitter? = null
    private var parameterPanel: JPanel? = null
    private var sqlPanel: JPanel? = null
    private var bottomPanel: JPanel? = null
    private var contentPanel: JPanel? = null

    private var currentEvent: SqlStatementSelectedEvent? = null

    init {
        setupUI()
        setupEventHandlers()
        project.messageBus.connect().subscribe(SqlStatementSelectedListener.TOPIC, this)
    }

    private fun createJsonEditor(): EditorTextField {
        val jsonLanguage = Language.findLanguageByID("JSON")
        val editor = if (jsonLanguage != null) {
            EditorTextField("", project, jsonLanguage.associatedFileType ?: PlainTextFileType.INSTANCE)
        } else {
            EditorTextField("", project, PlainTextFileType.INSTANCE)
        }

        editor.setOneLineMode(false)
        return editor
    }

    private fun createSqlEditor(): EditorTextField {
        val sqlLanguage = Language.findLanguageByID("SQL")
        val editor = if (sqlLanguage != null) {
            EditorTextField("", project, sqlLanguage.associatedFileType ?: PlainTextFileType.INSTANCE)
        } else {
            EditorTextField("", project, PlainTextFileType.INSTANCE)
        }

        editor.setOneLineMode(false)
        return editor
    }

    private fun setupUI() {
        parameterEditor.preferredSize = Dimension(400, 200)
        parameterEditor.isViewer = false
        parameterEditor.setPlaceholder("请输入JSON格式的参数")
        parameterEditor.addSettingsProvider { editor ->
            editor.settings.isUseSoftWraps = true
            editor.settings.setTabSize(2)
        }

        sqlEditor.preferredSize = Dimension(400, 200)
        sqlEditor.isViewer = false
        sqlEditor.setPlaceholder("生成的SQL将显示在这里")

        generateButton.isEnabled = false
        generateButton.icon = AllIcons.Actions.Execute
        generateButton.putClientProperty("JButton.buttonType", "primary")
        generateButton.background = JBColor.namedColor("Button.default.focusedBorderColor", JBColor.BLUE)

        errorTextArea.isEditable = false
        errorTextArea.lineWrap = true
        errorTextArea.wrapStyleWord = true
        errorTextArea.foreground = JBColor.RED  // 文字红色
        errorTextArea.background = JBColor.namedColor("Panel.background", JBColor.WHITE)  // 默认背景
        errorTextArea.font = parameterEditor.font  // 使用与编辑器相同的字体
        errorTextArea.border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
        errorScrollPane.preferredSize = Dimension(400, 200)
        errorScrollPane.minimumSize = Dimension(200, 100)
        errorScrollPane.border = javax.swing.BorderFactory.createEmptyBorder()
    }

    private fun setupEventHandlers() {
        generateButton.addActionListener {
            generateSql()
        }
    }

    private fun generateSql() {
        currentEvent?.let { event ->
            try {
                hideError()
                showSqlEditor()
                val parameterJson = parameterEditor.text
                val generatedSql = sqlGenerationService.generateSql(
                    StatementQualifyId(event.namespace, event.statementId),
                    parameterJson
                )
                sqlEditor.text = generatedSql
            } catch (e: Exception) {
                hideSqlEditor()
                showError("生成 SQL 失败:\n\n${e.stackTraceToString()}")
            }
        }
    }

    private fun showError(message: String) {
        errorTextArea.text = message
        errorTextArea.isVisible = true

        bottomPanel?.removeAll()
        bottomPanel?.add(errorScrollPane, BorderLayout.CENTER)

        errorScrollPane.isVisible = true

        bottomPanel?.invalidate()
        bottomPanel?.revalidate()
        bottomPanel?.repaint()

        javax.swing.SwingUtilities.invokeLater {
            errorScrollPane.verticalScrollBar.value = 0
            errorScrollPane.horizontalScrollBar.value = 0
            errorTextArea.caretPosition = 0

            contentPanel?.invalidate()
            contentPanel?.revalidate()
            contentPanel?.repaint()
        }
    }

    private fun hideError() {
        errorScrollPane.isVisible = false
        errorTextArea.isVisible = false

        bottomPanel?.removeAll()
        bottomPanel?.add(sqlPanel!!, BorderLayout.CENTER)

        bottomPanel?.invalidate()
        bottomPanel?.revalidate()
        bottomPanel?.repaint()

        javax.swing.SwingUtilities.invokeLater {
            contentPanel?.invalidate()
            contentPanel?.revalidate()
            contentPanel?.repaint()
        }
    }

    private fun showSqlEditor() {
        if (bottomPanel?.components?.contains(sqlPanel) != true) {
            bottomPanel?.removeAll()
            bottomPanel?.add(sqlPanel!!, BorderLayout.CENTER)
            bottomPanel?.revalidate()
            bottomPanel?.repaint()
        }
    }

    private fun hideSqlEditor() {
    }

    fun getContent(): JPanel {
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
        mainPanel.border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)

        val infoPanel = createInfoPanel()
        mainPanel.add(infoPanel, BorderLayout.NORTH)

        contentPanel = JBPanel<JBPanel<*>>(BorderLayout())

        parameterPanel = createParameterPanel()
        sqlPanel = createSqlPanel()

        bottomPanel = JBPanel<JBPanel<*>>(BorderLayout())
        bottomPanel!!.add(sqlPanel!!, BorderLayout.CENTER)

        updateLayout(mainPanel.width, mainPanel.height)

        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                val component = e?.component
                if (component != null) {
                    updateLayout(component.width, component.height)
                }
            }
        })

        mainPanel.add(contentPanel!!, BorderLayout.CENTER)
        return mainPanel
    }

    private fun updateLayout(width: Int, height: Int) {
        if (contentPanel == null || parameterPanel == null || bottomPanel == null) return

        contentPanel!!.removeAll()

        val useVerticalLayout = height > width

        if (mainSplitter != null) {
            mainSplitter!!.firstComponent = null
            mainSplitter!!.secondComponent = null
        }

        mainSplitter = JBSplitter(useVerticalLayout, 0.4f)
        mainSplitter!!.firstComponent = parameterPanel
        mainSplitter!!.secondComponent = bottomPanel

        contentPanel!!.add(mainSplitter!!, BorderLayout.CENTER)
        contentPanel!!.revalidate()
        contentPanel!!.repaint()
    }

    private fun createInfoPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        panel.border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)

        val gbc = GridBagConstraints()

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = Insets(0, 0, 0, 10)
        panel.add(statementInfoLabel, gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.EAST
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        gbc.insets = Insets(0, 0, 0, 0)
        panel.add(generateButton, gbc)

        return panel
    }

    private fun createParameterPanel(): JPanel {
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 0, 5, 0)
            add(JBLabel("参数 (JSON 格式)").apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
            }, BorderLayout.NORTH)
            add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8)
                add(parameterEditor, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }
    }

    private fun createSqlPanel(): JPanel {
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 0, 5, 0)
            add(JBLabel("生成的 SQL").apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
            }, BorderLayout.NORTH)
            add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8)
                add(sqlEditor, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }
    }

    override fun onStatementSelected(event: SqlStatementSelectedEvent) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("VividBatis")

        if (toolWindow != null) {
            if (!toolWindow.isVisible) {
                toolWindow.show(null)
            }
            toolWindow.activate(null)
        } else {
            ApplicationManager.getApplication().invokeLater {
                val laterToolWindow = toolWindowManager.getToolWindow("VividBatis")
                laterToolWindow?.let {
                    if (!it.isVisible) {
                        it.show(null)
                    }
                    it.activate(null)
                }
            }
        }

        currentEvent = event
        statementInfoLabel.text = "${event.namespace}.${event.statementId} [${event.statementType}]"

        try {
            hideError()
            parameterEditor.text = parameterAnalysisService.getStatementParameterInfo(
                StatementQualifyId(event.namespace, event.statementId),
            ).generateTemplate().let {
                Gson().toJson(it)
            } ?: "{}"
            generateButton.isEnabled = true

            generateSql()
        } catch (e: Exception) {
            parameterEditor.text = "// 无法分析参数类型: ${e.message}"
            generateButton.isEnabled = false
            sqlEditor.text = ""
            showError("参数分析失败:\n\n${e.stackTraceToString()}")
        }
    }
} 