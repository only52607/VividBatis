package com.github.only52607.vividbatis.mybatis.toolwindow


import com.github.only52607.vividbatis.model.StatementPath
import com.github.only52607.vividbatis.mybatis.util.ParameterAnalyzer
import com.github.only52607.vividbatis.mybatis.util.SqlGenerator
import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JButton
import javax.swing.JPanel

private const val SQL_PANEL = "SQL_PANEL"
private const val ERROR_PANEL = "ERROR_PANEL"

class SqlPreviewWindow(private val project: Project) : Disposable {
    companion object {
        val PREVIEW_WINDOW_KEY = Key.create<SqlPreviewWindow>("vividbatis.sql.preview.window")
    }

    class Factory : ToolWindowFactory, DumbAware {
        override fun shouldBeAvailable(project: Project) = true
        override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
            val sqlPreviewWindow = SqlPreviewWindow(project)
            val content = ContentFactory.getInstance().createContent(sqlPreviewWindow.contentPanel, "SQL 预览", false)
            content.putUserData(PREVIEW_WINDOW_KEY, sqlPreviewWindow)
            Disposer.register(content, sqlPreviewWindow)
            toolWindow.contentManager.addContent(content)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    private val statementInfoLabel = JBLabel("请选择一个 SQL 语句标签")
    private val parameterEditor = createEditor(
        project,
        Language.findLanguageByID("JSON")?.associatedFileType ?: PlainTextFileType.INSTANCE,
        false,
        "请输入JSON格式的参数"
    )
    private val sqlEditor = createEditor(
        project,
        Language.findLanguageByID("SQL")?.associatedFileType ?: PlainTextFileType.INSTANCE,
        false,
        "生成的SQL将显示在这里"
    )
    private val errorTextArea = createErrorTextArea()
    private val bottomCardLayout = CardLayout()
    private val bottomPanel = JBPanel<JBPanel<*>>(bottomCardLayout)
    private val generateButton = createGenerateButton()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private var currentStatement: StatementPath? = null
    private var warnings: List<String> = emptyList()

    private val warningAction = object : AnAction("Show Warnings", "Show SQL generation warnings", AllIcons.General.Warning) {
        override fun actionPerformed(e: AnActionEvent) {
            showWarningsPopup()
        }
    }

    private val warningButton = ActionButton(
        warningAction,
        warningAction.templatePresentation,
        ActionPlaces.UNKNOWN,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )

    val contentPanel: JPanel = buildMainPanel()

    fun processStatementSelection(statementPath: StatementPath, statementType: String) {
        currentStatement = statementPath
        statementInfoLabel.text = "$statementPath [${statementType}]"
        setSqlText("")

        scope.launch {
            try {
                val template = readAction {
                    ParameterAnalyzer.getStatementParameterInfo(project, currentStatement!!).generateTemplate()
                }
                setParameterText(gson.toJson(template))
                setGenerateButtonEnabled(true)
                generateSql()
            } catch (e: Exception) {
                setParameterText("// 无法分析参数类型: ${e.message}")
                setGenerateButtonEnabled(false)
                val errorMessage = "参数分析失败:\n\n${e.stackTraceToString()}"
                showError(errorMessage)
            }
        }
    }

    private fun generateSql() {
        currentStatement?.let {
            scope.launch {
                try {
                    val (generatedSql, newWarnings) = readAction {
                        SqlGenerator(project, it).generate(parameterEditor.document.text)
                    }
                    showSql(generatedSql, newWarnings)
                } catch (e: Exception) {
                    val errorMessage = "生成 SQL 失败:\n\n${e.stackTraceToString()}"
                    showError(errorMessage)
                }
            }
        }
    }

    private fun createEditor(project: Project, fileType: FileType, isViewer: Boolean, placeholder: String): EditorEx {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("")
        val editor = editorFactory.createEditor(document, project, fileType, isViewer) as EditorEx
        editor.settings.apply {
            isLineNumbersShown = true
            isUseSoftWraps = true
            setTabSize(2)
            isFoldingOutlineShown = true
            isIndentGuidesShown = true
            isBlinkCaret = true
        }
        editor.setPlaceholder(placeholder)
        return editor
    }

    private fun createErrorTextArea() = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        foreground = JBColor.RED
        font = Font("monospaced", Font.PLAIN, 12)
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

        val parameterPanel = createLabeledPanel("参数 (JSON 格式)", parameterEditor.component, null)
        val sqlPanel = createLabeledPanel("生成的 SQL", sqlEditor.component, warningButton)
        val errorScrollPane = JBScrollPane(errorTextArea).apply { border = JBUI.Borders.empty() }

        bottomPanel.add(sqlPanel, SQL_PANEL)
        bottomPanel.add(errorScrollPane, ERROR_PANEL)

        val mainSplitter = JBSplitter(true, 0.4f).apply {
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

    private fun createLabeledPanel(labelText: String, component: Component, leftComponent: Component?): JPanel {
        val labelPanel = JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.empty(5, 8)
            val gbc = GridBagConstraints()
            gbc.anchor = GridBagConstraints.WEST

            leftComponent?.let {
                it.isVisible = false
                gbc.weightx = 0.0
                add(it, gbc)
            }

            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            add(JBLabel(labelText), gbc)
        }
        return JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(5))).apply {
            val wrapper = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(0, 8)
                add(component, BorderLayout.CENTER)
            }
            add(labelPanel, BorderLayout.NORTH)
            add(wrapper, BorderLayout.CENTER)
        }
    }

    private fun showSql(sql: String, newWarnings: List<String>) {
        this.warnings = newWarnings
        setSqlText(sql)
        bottomCardLayout.show(bottomPanel, SQL_PANEL)
        updateWarningsUI()
    }

    private fun updateWarningsUI() {
        warningButton.isVisible = warnings.isNotEmpty()
        if (warnings.isNotEmpty()) {
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("SQL generation produced warnings", MessageType.WARNING, null)
                .setFadeoutTime(5000)
                .createBalloon()
            balloon.show(RelativePoint.getNorthWestOf(warningButton), Balloon.Position.atRight)
        }
    }

    private fun showWarningsPopup() {
        if (warnings.isEmpty()) return

        val listModel = JBList(warnings)
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(JBScrollPane(listModel), listModel)
            .setTitle("Generation Warnings")
            .setMovable(true)
            .setResizable(true)
            .createPopup()
        popup.showInCenterOf(contentPanel)
    }

    private fun setSqlText(sql: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            sqlEditor.document.setText(sql)
        }
    }

    private fun setParameterText(text: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            parameterEditor.document.setText(text)
        }
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

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(parameterEditor)
        EditorFactory.getInstance().releaseEditor(sqlEditor)
    }
}