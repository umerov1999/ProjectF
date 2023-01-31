package dev.ragnarok.intellij.getter_setter_plugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

open class CaseActionAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(event: AnActionEvent) {
        try {
            val editor = event.getRequiredData(CommonDataKeys.EDITOR)
            val project = event.getRequiredData(CommonDataKeys.PROJECT)
            val document = editor.document

            // Work off of the primary caret to get the selection info
            val primaryCaret = editor.caretModel.primaryCaret
            var selected: String = primaryCaret.selectedText ?: return
            if (selected.isNotEmpty()) {
                selected = if (selected[0].isLowerCase()) {
                    selected.uppercase()
                } else {
                    selected.lowercase()
                }
            }

            val start = primaryCaret.selectionStart
            val end = primaryCaret.selectionEnd

            // Replace the selection with a fixed string.
            // Must do this document change in a write action context.
            WriteCommandAction.runWriteCommandAction(
                project
            ) { document.replaceString(start, end, selected) }

            // De-select the text range that was just replaced
            primaryCaret.removeSelection()
        } catch (ignored: Throwable) {
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val caretModel = editor.caretModel
        e.presentation.isEnabledAndVisible = caretModel.currentCaret.hasSelection()
    }
}
