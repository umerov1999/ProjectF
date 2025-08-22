package dev.ragnarok.intellij.getter_setter_plugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

open class CaseAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(event: AnActionEvent) {
        try {
            val editor = event.getData(CommonDataKeys.EDITOR) ?: return
            val project = event.getData(CommonDataKeys.PROJECT) ?: return
            val document = editor.document

            // Work off of the primary caret to get the selection info
            val primaryCaret = editor.caretModel.primaryCaret
            var selected: String = primaryCaret.selectedText ?: return
            if (selected.isNotEmpty()) {
                var uppers = 0
                var lowers = 0
                for (i in selected) {
                    if (!i.isLetter()) {
                        continue
                    }
                    if (i.isLowerCase()) {
                        lowers++
                    } else {
                        uppers++
                    }
                }
                selected = if (lowers > 0 && uppers > 0) {
                    if (uppers >= lowers) {
                        selected.uppercase()
                    } else {
                        selected.lowercase()
                    }
                } else {
                    if (selected[0].isLowerCase()) {
                        selected.uppercase()
                    } else {
                        selected.lowercase()
                    }
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
        } catch (_: Throwable) {
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val caretModel = editor.caretModel
        e.presentation.isEnabledAndVisible = caretModel.currentCaret.hasSelection()
    }
}