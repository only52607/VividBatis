package com.github.only52607.vividbatis.toolWindow

import com.github.only52607.vividbatis.message.SqlStatementSelectedEvent
import com.github.only52607.vividbatis.message.SqlStatementSelectedListener
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
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JPanel

private const val SQL_PANEL = "SQL_PANEL"
private const val ERROR_PANEL = "ERROR_PANEL"

internal interface SqlPreviewContract {
    interface View {
        fun setStatementId(namespace: String, statementId: String, statementType: String)
        fun setParameters(json: String)
        fun showSql(sql: String)
        fun showError(message: String)
        fun setGenerateButtonEnabled(enabled: Boolean)
        fun setParameterError(message: String)
        fun clearSql()
        fun activateWindow()
    }

    interface Presenter : SqlStatementSelectedListener {
        fun onGenerateSqlClicked(parameters: String)
    }
}

internal class SqlPreviewPresenter(
    private val project: Project,
    private val view: SqlPreviewContract.View
) : SqlPreviewContract.Presenter {
    private var currentStatement: StatementQualifyId? = null
    private val gson = GsonBuilder().setPrettyPrinting().create()

    override fun onStatementSelected(event: SqlStatementSelectedEvent) {
        view.activateWindow()
        currentStatement = StatementQualifyId(event.namespace, event.statementId)
        view.setStatementId(event.namespace, event.statementId, event.statementType)
        view.clearSql()

        try {
            val parameterInfo = ParameterAnalyzer.getStatementParameterInfo(project, currentStatement!!)
            val template = parameterInfo.generateTemplate()
            view.setParameters(gson.toJson(template))
            view.setGenerateButtonEnabled(true)
            onGenerateSqlClicked(gson.toJson(template))
        } catch (e: Exception) {
            view.setParameterError("// 无法分析参数类型: ${e.message}")
            view.setGenerateButtonEnabled(false)
            val errorMessage = "参数分析失败:\n\n${e.stackTraceToString()}"
            view.showError(errorMessage)
        }
    }

    override fun onGenerateSqlClicked(parameters: String) {
        currentStatement?.let {
            try {
                val generatedSql = SqlGenerator(project, it).generate(parameters)
                view.showSql(generatedSql)
            } catch (e: Exception) {
                val errorMessage = "生成 SQL 失败:\n\n${e.stackTraceToString()}"
                view.showError(errorMessage)
            }
        }
    }
}

class SqlPreviewWindow(private val project: Project) : SqlPreviewContract.View {
    class Factory : ToolWindowFactory, DumbAware {
        override fun shouldBeAvailable(project: Project) = true
        override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
            val sqlPreviewWindow = SqlPreviewWindow(project)
            val content = ContentFactory.getInstance().createContent(sqlPreviewWindow.contentPanel, "SQL 预览", false)
            toolWindow.contentManager.addContent(content)
        }
    }

    private val presenter: SqlPreviewContract.Presenter = SqlPreviewPresenter(project, this)
    private val statementInfoLabel = JBLabel("请选择一个 SQL 语句标签")
    private val parameterEditor = createEditorTextField(languageId = "JSON", placeholder = "请输入JSON格式的参数")
    private val sqlEditor = createEditorTextField(languageId = "SQL", placeholder = "生成的SQL将显示在这里")
    private val errorTextArea = createErrorTextArea()
    private val bottomCardLayout = CardLayout()
    private val bottomPanel = JBPanel<JBPanel<*>>(bottomCardLayout)
    private val generateButton = createGenerateButton()

    val contentPanel: JPanel = buildMainPanel()

    init {
        project.messageBus.connect().subscribe(SqlStatementSelectedListener.TOPIC, presenter)
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
        addActionListener { presenter.onGenerateSqlClicked(parameterEditor.text) }
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
        val sqlPanel = createLabeledPanel("SQL预览", sqlEditor)
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
                border = JBUI.Borders.empty(0, 8)
                add(component, BorderLayout.CENTER)
            }
            add(label, BorderLayout.NORTH)
            add(wrapper, BorderLayout.CENTER)
        }
    }

    override fun setStatementId(namespace: String, statementId: String, statementType: String) {
        statementInfoLabel.text = "$namespace.$statementId [$statementType]"
    }

    override fun setParameters(json: String) {
        parameterEditor.text = json
    }

    override fun showSql(sql: String) {
        sqlEditor.text = sql
        bottomCardLayout.show(bottomPanel, SQL_PANEL)
    }

    override fun showError(message: String) {
        errorTextArea.text = message
        bottomCardLayout.show(bottomPanel, ERROR_PANEL)
        EventQueue.invokeLater {
            (bottomPanel.components.find { it.isVisible } as? JBScrollPane)?.verticalScrollBar?.value = 0
        }
    }

    override fun setGenerateButtonEnabled(enabled: Boolean) {
        generateButton.isEnabled = enabled
    }

    override fun setParameterError(message: String) {
        parameterEditor.text = message
    }

    override fun clearSql() {
        sqlEditor.text = ""
    }

    override fun activateWindow() {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("VividBatis")
        toolWindow?.let {
            if (!it.isVisible) it.show()
            it.activate(null)
        }
    }
}