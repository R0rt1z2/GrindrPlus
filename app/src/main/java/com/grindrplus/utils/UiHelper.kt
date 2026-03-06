package com.grindrplus.utils

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.Drawable as AndroidDrawable
import android.view.View
import android.widget.Toast
import com.grindrplus.GrindrPlus.currentActivity
import com.grindrplus.GrindrPlus.runOnMainThread
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger

object UiHelper {
    fun showToast(message: CharSequence, duration: Int) {
        runOnMainThread { ctx ->
            runCatching {
                Toast.makeText(ctx, message, duration).show()
            }.onFailure { error ->
                Logger.e("Failed to show toast: ${error.message}", LogSource.MODULE)
            }
        }
    }

    sealed class Icon {
        data class ResId(val id: Int) : Icon()
        data class Drawable(val drawable: AndroidDrawable) : Icon()
    }

    data class DialogButton(
        val text: CharSequence,
        val onClick: (() -> Unit)? = null,
        val onLongClick: ((AlertDialog) -> Unit)? = null,
        val onPreventDismiss: ((AlertDialog) -> Unit)? = null
    )

    class AlertDialogConfig {
        var title: CharSequence? = null
        var message: CharSequence? = null
        var positiveButton: DialogButton? = DialogButton("OK")
        var negativeButton: DialogButton? = null
        var neutralButton: DialogButton? = null
        var onDismiss: (() -> Unit)? = null
        var onCancel: (() -> Unit)? = null
        var onShow: ((AlertDialog) -> Unit)? = null
        var cancellable: Boolean = true
        var icon: Icon? = null
        var view: View? = null
        var activity: Activity? = null
    }

    fun showAlertDialog(block: AlertDialogConfig.() -> Unit) {
        val config = AlertDialogConfig().apply(block)
        val targetActivity = config.activity ?: currentActivity ?: return

        runOnMainThread(targetActivity) {
            runCatching {
                AlertDialog.Builder(targetActivity).apply {
                    config.title?.let { setTitle(it) }
                    config.message?.let { setMessage(it) }
                    config.negativeButton?.let { btn -> setNegativeButton(btn.text) { _, _ -> btn.onClick?.invoke() } }
                    config.neutralButton?.let { btn -> setNeutralButton(btn.text) { _, _ -> btn.onClick?.invoke() } }
                    config.onDismiss?.let { dismiss -> setOnDismissListener { dismiss() } }
                    config.onCancel?.let { cancel -> setOnCancelListener { cancel() } }
                    setCancelable(config.cancellable)

                    config.icon?.let {
                        when (it) {
                            is Icon.ResId    -> setIcon(it.id)
                            is Icon.Drawable -> setIcon(it.drawable)
                        }
                    }

                    config.view?.let { setView(it) }

                    config.positiveButton?.let { btn ->
                        if (btn.onPreventDismiss != null) {
                            setPositiveButton(btn.text, null)
                        } else {
                            setPositiveButton(btn.text) { _, _ -> btn.onClick?.invoke() }
                        }
                    }

                    val dialog = show()

                    config.onShow?.invoke(dialog)

                    config.positiveButton?.let { btn ->
                        btn.onPreventDismiss?.let { block ->
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                block.invoke(dialog)
                            }
                        }
                        btn.onLongClick?.let { block ->
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener {
                                block.invoke(dialog)
                                true
                            }
                        }
                    }

                    config.negativeButton?.onLongClick?.let { block ->
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnLongClickListener {
                            block.invoke(dialog)
                            true
                        }
                    }

                    config.neutralButton?.onLongClick?.let { block ->
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnLongClickListener {
                            block.invoke(dialog)
                            true
                        }
                    }
                }
            }.onFailure { e ->
                Logger.e("Failed to show alert dialog: ${e.message}", LogSource.MODULE)
            }
        }
    }
}
