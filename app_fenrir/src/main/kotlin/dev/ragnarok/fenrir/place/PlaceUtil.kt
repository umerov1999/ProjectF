package dev.ragnarok.fenrir.place

import android.app.Activity
import android.app.Dialog
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.WallEditorAttrs
import dev.ragnarok.fenrir.place.PlaceFactory.getCreatePostPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getEditPostPlace
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.spots.SpotsDialog
import java.lang.ref.WeakReference

object PlaceUtil {

    fun goToPostEditor(activity: Activity, accountId: Int, post: Post) {
        val dialog = createProgressDialog(activity)
        val dialogWeakReference = WeakReference<Dialog>(dialog)
        val reference = WeakReference(activity)
        val ownerId = post.ownerId
        val ids: MutableSet<Int> = HashSet()
        ids.add(accountId)
        ids.add(ownerId)
        val disposable = owners
            .findBaseOwnersDataAsBundle(accountId, ids, IOwnersRepository.MODE_NET)
            .fromIOToMain()
            .subscribe({ owners ->
                val attrs = WallEditorAttrs(owners.getById(ownerId), owners.getById(accountId))
                val d = dialogWeakReference.get()
                d?.dismiss()
                val a = reference.get()
                if (a != null) {
                    getEditPostPlace(accountId, post, attrs).tryOpenWith(a)
                }
            }) { throwable -> safelyShowError(reference, throwable) }
        dialog.setOnCancelListener { disposable.dispose() }
    }

    private fun safelyShowError(reference: WeakReference<Activity>, throwable: Throwable) {
        val a = reference.get()
        if (a != null) {
            MaterialAlertDialogBuilder(a)
                .setTitle(R.string.error)
                .setMessage(Utils.getCauseIfRuntime(throwable).message)
                .setPositiveButton(R.string.button_ok, null)
                .show()
        }
    }


    @JvmOverloads
    fun goToPostCreation(
        activity: Activity,
        accountId: Int,
        ownerId: Int,
        @EditingPostType editingType: Int,
        input: List<AbsModel>?,
        streams: ArrayList<Uri>? = null,
        body: String? = null,
        mime: String? = null
    ) {
        val dialog = createProgressDialog(activity)
        val dialogWeakReference = WeakReference<Dialog>(dialog)
        val reference = WeakReference(activity)
        val ids: MutableSet<Int> = HashSet()
        ids.add(accountId)
        ids.add(ownerId)
        val disposable = owners
            .findBaseOwnersDataAsBundle(accountId, ids, IOwnersRepository.MODE_NET)
            .fromIOToMain()
            .subscribe({ owners ->
                val attrs = WallEditorAttrs(owners.getById(ownerId), owners.getById(accountId))
                val d = dialogWeakReference.get()
                d?.dismiss()
                val a = reference.get()
                if (a != null) {
                    getCreatePostPlace(
                        accountId,
                        ownerId,
                        editingType,
                        input,
                        attrs,
                        streams,
                        body,
                        mime
                    ).tryOpenWith(a)
                }
            }) { throwable -> safelyShowError(reference, throwable) }
        dialog.setOnCancelListener { disposable.dispose() }
    }

    private fun createProgressDialog(activity: Activity): AlertDialog {
        val dialog = SpotsDialog.Builder().setContext(activity)
            .setMessage(activity.getString(R.string.message_obtaining_owner_information))
            .setCancelable(true).build()
        dialog.show()
        return dialog
    }
}