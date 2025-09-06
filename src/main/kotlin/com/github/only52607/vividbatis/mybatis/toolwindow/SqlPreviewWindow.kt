package com.github.only52607.vividbatis.mybatis.toolwindow


import com.github.only52607.vividbatis.common.MyBundle
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
import javax.swing.JCheckBox
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
            val content = ContentFactory.getInstance().createContent(sqlPreviewWindow.contentPanel, MyBundle.message("vividbatis.toolwindow.title.preview"), false)
            content.putUserData(PREVIEW_WINDOW_KEY, sqlPreviewWindow)
            Disposer.register(content, sqlPreviewWindow)
            toolWindow.contentManager.addContent(content)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    private val statementInfoLabel = JBLabel(MyBundle.message("vividbatis.toolwindow.select.statement.default"))
    private val parameterEditor = createEditor(
        project,
        Language.findLanguageByID("JSON")?.associatedFileType ?: PlainTextFileType.INSTANCE,
        false,
        MyBundle.message("vividbatis.toolwindow.json.editor.placeholder")
    )
    private val sqlEditor = createEditor(
        project,
        Language.findLanguageByID("SQL")?.associatedFileType ?: PlainTextFileType.INSTANCE,
        true,
        MyBundle.message("vividbatis.toolwindow.sql.editor.placeholder")
    )
    private val errorTextArea = createErrorTextArea()
    private val bottomCardLayout = CardLayout()
    private val bottomPanel = JBPanel<JBPanel<*>>(bottomCardLayout)
    private val generateButton = createGenerateButton()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private var currentStatement: StatementPath? = null
    private var warnings: List<String> = emptyList()

    private val warningAction = object : AnAction(
        MyBundle.message("vividbatis.action.show.warnings.text"),
        MyBundle.message("vividbatis.action.show.warnings.description"),
        AllIcons.General.Warning
    ) {
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
        statementInfoLabel.text = MyBundle.message("vividbatis.toolwindow.statement.info.format", statementPath, statementType)
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
                setParameterText(MyBundle.message("vividbatis.error.analyze.parameter.short", e.message ?: ""))
                setGenerateButtonEnabled(false)
                val stackTrace = e.stackTraceToString()
                val errorMessage = MyBundle.message("vividbatis.error.analyze.parameter.detailed", stackTrace)
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
                    val stackTrace = e.stackTraceToString()
                    val errorMessage = MyBundle.message("vividbatis.error.generate.sql.detailed", stackTrace)
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
            isUseSoftWraps = false
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

    private fun createGenerateButton() = JButton(MyBundle.message("vividbatis.toolwindow.button.preview")).apply {
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

        val parameterPanel = createLabeledPanel(
            MyBundle.message("vividbatis.toolwindow.parameters.label"),
            parameterEditor.component,
            null,
            createWordWrapCheckbox(parameterEditor)
        )
        val sqlPanel = createLabeledPanel(
            MyBundle.message("vividbatis.toolwindow.generated.sql.label"),
            sqlEditor.component,
            warningButton,
            createWordWrapCheckbox(sqlEditor)
        )
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

    private fun createWordWrapCheckbox(editor: EditorEx) =
        JCheckBox(MyBundle.message("vividbatis.toolwindow.toggle.wordwrap")).apply {
            isSelected = editor.settings.isUseSoftWraps
            addActionListener {
                editor.settings.isUseSoftWraps = this.isSelected
            }
        }

    private fun createLabeledPanel(
        labelText: String,
        component: Component,
        leftComponent: Component?,
        rightComponent: Component?
    ): JPanel {
        val labelPanel = JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.empty(5, 8)
            val gbc = GridBagConstraints()
            gbc.anchor = GridBagConstraints.WEST

            leftComponent?.let {
                it.isVisible = false
                gbc.gridx = 0
                gbc.weightx = 0.0
                add(it, gbc)
            }

            gbc.gridx = 1
            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            add(JBLabel(labelText), gbc)

            rightComponent?.let {
                gbc.gridx = 2
                gbc.weightx = 0.0
                gbc.anchor = GridBagConstraints.EAST
                gbc.fill = GridBagConstraints.NONE
                add(it, gbc)
            }
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
                .createHtmlTextBalloonBuilder(MyBundle.message("vividbatis.warning.generation.title"), MessageType.WARNING, null)
                .setFadeoutTime(5000)
                .createBalloon()
            EventQueue.invokeLater {
                balloon.show(RelativePoint.getSouthOf(warningButton), Balloon.Position.below)
            }
        }
    }

    private fun showWarningsPopup() {
        if (warnings.isEmpty()) return

        val listModel = JBList(warnings)
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(JBScrollPane(listModel), listModel)
            .setTitle(MyBundle.message("vividbatis.warning.popup.title"))
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
