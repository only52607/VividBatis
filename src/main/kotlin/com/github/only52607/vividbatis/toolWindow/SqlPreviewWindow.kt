package com.github.only52607.vividbatis.toolWindow

import com.github.only52607.vividbatis.message.SqlStatementSelectedEvent
import com.github.only52607.vividbatis.message.SqlStatementSelectedListener
import com.github.only52607.vividbatis.model.StatementQualifyId
import com.github.only52607.vividbatis.util.ParameterAnalyzer
import com.github.only52607.vividbatis.util.SqlGenerator
import com.google.gson.Gson
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JPanel

class SqlPreviewWindow(private val project: Project) : SqlStatementSelectedListener {
    class Factory : ToolWindowFactory, DumbAware {
        override fun shouldBeAvailable(project: Project) = true

        override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
            toolWindow.apply {
                contentManager.addContent(
                    ContentFactory.getInstance().createContent(
                        SqlPreviewWindow(project).getContent(),
                        "SQL 预览",
                        false
                    )
                )
                toolWindow.setToHideOnEmptyContent(false)
            }
        }
    }

    private val statementInfoLabel = JBLabel("请选择一个 SQL 语句标签")
    private val parameterEditor = createJsonEditor().apply {
        preferredSize = Dimension(400, 200)
        isViewer = false
        setPlaceholder("请输入JSON格式的参数")
        addSettingsProvider { editor ->
            editor.settings.isUseSoftWraps = true
            editor.settings.setTabSize(2)
        }
    }

    private val sqlEditor = createSqlEditor().apply {
        preferredSize = Dimension(400, 200)
        isViewer = false
        setPlaceholder("生成的SQL将显示在这里")
    }

    private val generateButton = JButton("预览 SQL").apply {
        isEnabled = false
        icon = AllIcons.Actions.Execute
        putClientProperty("JButton.buttonType", "primary")
        background = JBColor.namedColor("Button.default.focusedBorderColor", JBColor.BLUE)
        addActionListener {
            generateSql()
        }
    }

    private val errorTextArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        foreground = JBColor.RED  // 文字红色
        background = JBColor.namedColor("Panel.background", JBColor.WHITE)  // 默认背景
        font = parameterEditor.font  // 使用与编辑器相同的字体
        border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }

    private val errorScrollPane = JBScrollPane(errorTextArea).apply {
        preferredSize = Dimension(400, 200)
        minimumSize = Dimension(200, 100)
        border = javax.swing.BorderFactory.createEmptyBorder()
    }

    private var mainSplitter: JBSplitter? = null
    private var parameterPanel: JPanel? = null
    private var sqlPanel: JPanel? = null
    private var bottomPanel: JPanel? = null
    private var contentPanel: JPanel? = null

    private var currentEvent: SqlStatementSelectedEvent? = null

    init {
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

    private fun generateSql() {
        currentEvent?.let { event ->
            try {
                hideError()
                showSqlEditor()
                sqlEditor.text = SqlGenerator(
                    project,
                    StatementQualifyId(event.namespace, event.statementId)
                ).generate(
                    parameterEditor.text
                )
            } catch (e: Exception) {
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

    fun getContent(): JPanel {
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JBPanel<JBPanel<*>>(GridBagLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
                add(statementInfoLabel, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    anchor = GridBagConstraints.WEST
                    fill = GridBagConstraints.HORIZONTAL
                    weightx = 1.0
                    insets = JBUI.insetsRight(10)
                })
                add(generateButton, GridBagConstraints().apply {
                    gridx = 1
                    gridy = 0
                    anchor = GridBagConstraints.EAST
                    fill = GridBagConstraints.NONE
                    weightx = 0.0
                    insets = JBUI.emptyInsets()
                })
            }, BorderLayout.NORTH)
        }

        contentPanel = JBPanel<JBPanel<*>>(BorderLayout())

        parameterPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 0, 5, 0)
            add(JBLabel("参数 (JSON 格式)").apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
            }, BorderLayout.NORTH)
            add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8)
                add(parameterEditor, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }

        sqlPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = javax.swing.BorderFactory.createEmptyBorder(5, 0, 5, 0)
            add(JBLabel("生成的 SQL").apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8)
            }, BorderLayout.NORTH)
            add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8)
                add(sqlEditor, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }

        bottomPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(sqlPanel!!, BorderLayout.CENTER)
        }

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
        contentPanel?.removeAll()
        mainSplitter?.apply {
            firstComponent = null
            secondComponent = null
        }
        mainSplitter = JBSplitter(height > width, 0.4f).apply {
            firstComponent = parameterPanel
            secondComponent = bottomPanel
        }

        contentPanel!!.add(mainSplitter!!, BorderLayout.CENTER)
        contentPanel!!.revalidate()
        contentPanel!!.repaint()
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
            parameterEditor.text = ParameterAnalyzer.getStatementParameterInfo(
                project,
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