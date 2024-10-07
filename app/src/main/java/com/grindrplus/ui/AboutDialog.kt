package com.grindrplus.ui

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.grindrplus.BuildConfig
import com.grindrplus.R
import com.grindrplus.databinding.DialogAboutBinding

class AboutDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding: DialogAboutBinding =
            DialogAboutBinding.inflate(layoutInflater, null, false)
        binding.designAboutTitle.setText(R.string.app_name)
        binding.designAboutInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.designAboutInfo.text = HtmlCompat.fromHtml(
            getString(
                R.string.about_view_source_code,
                "<b><a href=\"https://github.com/R0rt1z2/GrindrPlus\">GitHub</a></b>",
            ), HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.designAboutVersion.text = String.format("%s (%s)",
            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE
        )
        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }
}