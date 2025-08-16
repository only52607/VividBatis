package com.github.only52607.vividbatis.toolWindow


import com.github.only52607.vividbatis.model.StatementQualifyId
import com.github.only52607.vividbatis.util.ParameterAnalyzer
import com.github.only52607.vividbatis.util.SqlGenerator
import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JPanel

private const val SQL_PANEL = "SQL_PANEL"
private const val ERROR_PANEL = "ERROR_PANEL"

class SqlPreviewWindow(private val project: Project) {
    companion object {
        val PREVIEW_WINDOW_KEY = Key.create<SqlPreviewWindow>("vividbatis.sql.preview.window")
    }

    class Factory : ToolWindowFactory, DumbAware {
        override fun shouldBeAvailable(project: Project) = true
        override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
            val sqlPreviewWindow = SqlPreviewWindow(project)
            val content = ContentFactory.getInstance().createContent(sqlPreviewWindow.contentPanel, "SQL 预览", false)
            content.putUserData(PREVIEW_WINDOW_KEY, sqlPreviewWindow)
            toolWindow.contentManager.addContent(content)
        }
    }

    private val statementInfoLabel = JBLabel("请选择一个 SQL 语句标签")
    private val parameterEditor = createEditorTextField(languageId = "JSON", placeholder = "请输入JSON格式的参数")
    private val sqlEditor = createEditorTextField(languageId = "SQL", placeholder = "生成的SQL将显示在这里")
    private val errorTextArea = createErrorTextArea()
    private val bottomCardLayout = CardLayout()
    private val bottomPanel = JBPanel<JBPanel<*>>(bottomCardLayout)
    private val generateButton = createGenerateButton()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private var currentStatement: StatementQualifyId? = null

    val contentPanel: JPanel = buildMainPanel()

    fun processStatementSelection(namespace: String, statementId: String, statementType: String) {
        currentStatement = StatementQualifyId(namespace, statementId)
        statementInfoLabel.text = "${namespace}.${statementId} [${statementType}]"
        sqlEditor.text = ""

        try {
            val parameterInfo = ParameterAnalyzer.getStatementParameterInfo(project, currentStatement!!)
            val template = parameterInfo.generateTemplate()
            parameterEditor.text = gson.toJson(template)
            setGenerateButtonEnabled(true)
            generateSql()
        } catch (e: Exception) {
            parameterEditor.text = "// 无法分析参数类型: ${e.message}"
            setGenerateButtonEnabled(false)
            val errorMessage = "参数分析失败:\n\n${e.stackTraceToString()}"
            showError(errorMessage)
        }
    }

    private fun generateSql() {
        currentStatement?.let {
            try {
                val generatedSql = SqlGenerator(project, it).generate(parameterEditor.text)
                showSql(generatedSql)
            } catch (e: Exception) {
                val errorMessage = "生成 SQL 失败:\n\n${e.stackTraceToString()}"
                showError(errorMessage)
            }
        }
    }

    private fun createEditorTextField(languageId: String, placeholder: String): EditorTextField {
        val language = Language.findLanguageByID(languageId)
        val fileType: FileType? = language?.associatedFileType
        return EditorTextField("", project, fileType ?: PlainTextFileType.INSTANCE).apply {
            setOneLineMode(false)
            isViewer = false
            setPlaceholder(placeholder)
            addSettingsProvider { editor ->
                editor.settings.isUseSoftWraps = true
                editor.settings.setTabSize(2)
            }
        }
    }

    private fun createErrorTextArea() = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        foreground = JBColor.RED
        font = parameterEditor.font
        border = JBUI.Borders.empty(8)
    }

    private fun createGenerateButton() = JButton("预览 SQL").apply {
        isEnabled = false
        icon = AllIcons.Actions.Execute
        addActionListener { generateSql() }
    }

    private fun buildMainPanel(): JPanel {
        val topPanel = JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.empty(5, 8)
            add(statementInfoLabel, GridBagConstraints().apply {
                gridx = 0; weightx = 1.0; anchor = GridBagConstraints.WEST; fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insetsRight(10)
            })
            add(generateButton, GridBagConstraints().apply {
                gridx = 1; weightx = 0.0; anchor = GridBagConstraints.EAST
            })
        }

        val parameterPanel = createLabeledPanel("参数 (JSON 格式)", parameterEditor)
        val sqlPanel = createLabeledPanel("生成的 SQL", sqlEditor)
        val errorScrollPane = JBScrollPane(errorTextArea).apply { border = JBUI.Borders.empty() }

        bottomPanel.add(sqlPanel, SQL_PANEL)
        bottomPanel.add(errorScrollPane, ERROR_PANEL)

        val mainSplitter = JBSplitter(false, 0.4f).apply {
            firstComponent = parameterPanel
            secondComponent = bottomPanel
        }

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty(8)
            add(topPanel, BorderLayout.NORTH)
            add(mainSplitter, BorderLayout.CENTER)
            addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    mainSplitter.orientation = e.component.height > e.component.width
                }
            })
        }
    }

    private fun createLabeledPanel(labelText: String, component: Component): JPanel {
        return JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(5))).apply {
            val label = JBLabel(labelText).apply { border = JBUI.Borders.empty(5, 8) }
            val wrapper = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(0, 8, 0, 8)
                add(component, BorderLayout.CENTER)
            }
            add(label, BorderLayout.NORTH)
            add(wrapper, BorderLayout.CENTER)
        }
    }

    private fun showSql(sql: String) {
        sqlEditor.text = sql
        bottomCardLayout.show(bottomPanel, SQL_PANEL)
    }

    private fun showError(message: String) {
        errorTextArea.text = message
        bottomCardLayout.show(bottomPanel, ERROR_PANEL)
        EventQueue.invokeLater {
            (bottomPanel.components.find { it.isVisible } as? JBScrollPane)?.verticalScrollBar?.value = 0
        }
    }

    private fun setGenerateButtonEnabled(enabled: Boolean) {
        generateButton.isEnabled = enabled
    }
}