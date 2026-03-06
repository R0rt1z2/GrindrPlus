package com.grindrplus.utils

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.Drawable as AndroidDrawable
import android.view.View
import android.widget.Toast
import com.grindrplus.GrindrPlus.currentActivity
import com.grindrplus.GrindrPlus.loadClass
import com.grindrplus.GrindrPlus.runOnMainThread
import com.grindrplus.core.LogSource
import com.grindrplus.core.Logger
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod

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

    sealed class SnackbarType {
        object NOTIFY : SnackbarType()
        object NEUTRAL : SnackbarType()
        data class SUCCESS(val showIcon: Boolean = false) : SnackbarType()
        data class ERROR(val showIcon: Boolean = false) : SnackbarType()
    }

    const val SNACKBAR_LENGTH_SHORT = -1
    const val SNACKBAR_LENGTH_LONG = 0
    const val SNACKBAR_LENGTH_INDEFINITE = -2

    fun showSnackbar(
        message: CharSequence,
        type: SnackbarType = SnackbarType.NEUTRAL,
        duration: Int = SNACKBAR_LENGTH_SHORT,
        icon: Icon? = null,
        view: View? = null
    ) {
        val anchorView = view ?: currentActivity?.window?.decorView
        if (anchorView == null) {
            Logger.w("showSnackbar: no anchor view available", LogSource.MODULE)
            return
        }

        runOnMainThread(anchorView.context) {
            val snackbarClass = loadClass("com.google.android.material.snackbar.Snackbar")
            val snackbar = callStaticMethod(snackbarClass, "make", anchorView, message, duration)

            runCatching {
                // To find this class, search for 'R.color.snackbar_background_color_error'
                //
                // NOTIFY: R.color.snackbar_background_color_notify ("grindr_gold_star_gay": #ffcc00)
                // NEUTRAL: R.drawable.bg_snackbar ("grindr_dark_gray_3": #1f1f20)
                // SUCCESS: R.color.snackbar_background_color_success ("grindr_lime_time": #00e676)
                // ERROR: R.color.snackbar_background_color_error ("grindr_ketchup_stain": #ef5242)
                val styleHelper = loadClass("t80.k")

                when (type) {
                    SnackbarType.NOTIFY    -> callStaticMethod(styleHelper, "i", snackbar)
                    SnackbarType.NEUTRAL   -> callStaticMethod(styleHelper, "h", snackbar)
                    is SnackbarType.SUCCESS -> callStaticMethod(styleHelper, "o", snackbar, type.showIcon)
                    is SnackbarType.ERROR   -> callStaticMethod(styleHelper, "b", snackbar, type.showIcon)
                }

                // Custom icon overrides any icon set by the type (SUCCESS/ERROR)
                icon?.let {
                    when (it) {
                        is Icon.ResId  -> callStaticMethod(styleHelper, "j", snackbar, it.id)
                        is Icon.Drawable -> callStaticMethod(styleHelper, "k", snackbar, it.drawable)
                    }
                }
            }.onFailure {
                Logger.w("showSnackbar: failed to apply style, falling back to unstyled", LogSource.MODULE)
            }

            callMethod(snackbar, "show")
        }
    }
}
