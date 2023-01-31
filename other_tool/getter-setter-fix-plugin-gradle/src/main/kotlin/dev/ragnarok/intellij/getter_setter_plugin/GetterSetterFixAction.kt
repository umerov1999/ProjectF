package dev.ragnarok.intellij.getter_setter_plugin

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import java.util.regex.Pattern

open class GetterSetterFixAction : AnAction() {
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
            var selected = primaryCaret.selectedText ?: return
            val pattern = Pattern.compile(
                "(\\w*) = ([a-zA-Z0-9_\\t!@&?,+|><\"\\-=/*;:.()\\[\\]{} \\r\\n]*)",
                Pattern.MULTILINE
            )
            val ll = pattern.matcher(selected)
            if (!ll.find()) {
                selected = if (selected[0] == 'i' && selected[1] == 's') {
                    "$selected()"
                } else {
                    "get" + selected[0].uppercaseChar() + selected.substring(1) + "()"
                }
            } else {
                var v1 = ll.group(1)
                val v2 = ll.group(2)
                v1 = if (v1[0] == 'i' && v1[1] == 's') {
                    "set" + v1.substring(2) + "("
                } else {
                    "set" + v1[0].uppercaseChar() + v1.substring(1) + "("
                }
                selected = "$v1$v2)"
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
