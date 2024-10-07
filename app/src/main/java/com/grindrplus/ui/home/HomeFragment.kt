package com.grindrplus.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.grindrplus.BuildConfig
import com.grindrplus.R
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import com.grindrplus.core.Utils
import com.grindrplus.databinding.FragmentHomeBinding
import com.grindrplus.utils.Config

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        updateStates(requireActivity())

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("DefaultLocale")
    private fun updateStates(activity : Activity) {
        binding.appVersion.text = String.format(
            "%s %d (%s)",
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.BUILD_TYPE
        )

        if (Utils.isPackageInstalled(activity, GRINDR_PACKAGE_NAME)) {
            binding.grindrVersion.text = String.format(
                "%s (%d)",
                Utils.getAppVersion(GRINDR_PACKAGE_NAME, activity).first,
                Utils.getAppVersion(GRINDR_PACKAGE_NAME, activity).second
            )
        }

        binding.activeHooks.text = 10.toString()

        binding.androidVersion.text = String.format(
            "%s (API %d)",
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT
        )

        binding.abiType.text = Build.SUPPORTED_ABIS.joinToString(", ")
        binding.deviceModel.text = Build.MODEL

        if (Utils.isModuleLoaded()) {
            binding.statusTitle.text = "Enabled"
            binding.statusSummary.text = String.format(
                "%s (%d)",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
            binding.warningCard.visibility = View.GONE
        } else {
            binding.statusTitle.text = "Disabled"
            binding.statusSummary.text = "Module not loaded"
            binding.statusIcon.setImageResource(R.drawable.ic_baseline_cancel_24dp)
            binding.warningCard.visibility = View.GONE
        }
    }
}