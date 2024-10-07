package com.grindrplus.ui.hooks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.materialswitch.MaterialSwitch
import com.grindrplus.R
import com.grindrplus.databinding.FragmentHooksBinding
import com.grindrplus.utils.Config
import com.grindrplus.utils.HookConfig

class HooksFragment : Fragment(), MenuProvider {
    private var _binding: FragmentHooksBinding? = null
    private lateinit var hookLayout: LinearLayout
    private val binding
        get() = _binding!!

    private val hookList = mutableListOf<HookConfig>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHooksBinding.inflate(inflater, container, false)
        hookLayout = binding.nestedScrollView.findViewById(R.id.hookLayout)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val root: View = binding.root

        hookList.addAll(
            Config.getConfig().getJSONObject("hooks").keys().asSequence().map { hookName ->
                HookConfig(
                    name = hookName,
                    description =
                    Config.getConfig()
                        .getJSONObject("hooks")
                        .getJSONObject(hookName)
                        .getString("description"),
                    enabled = Config.isHookEnabled(hookName)
                )
            }
        )

        populateHookList(hookList)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun populateHookList(hookList: List<HookConfig>) {
        hookLayout.removeAllViews()

        for (hook in hookList) {
            val hookItemView = layoutInflater.inflate(R.layout.hook_item, hookLayout, false)

            hookItemView.findViewById<TextView>(R.id.hookName).text = hook.name
            hookItemView.findViewById<TextView>(R.id.hookDescription).text = hook.description

            val hookSwitch = hookItemView.findViewById<MaterialSwitch>(R.id.hookSwitch)
            hookSwitch.isChecked = hook.enabled

            hookSwitch.setOnCheckedChangeListener { _, isChecked ->
                Config.setHookState(hook.name, isChecked)
            }

            hookLayout.addView(hookItemView)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_modules, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterHooks(newText ?: "")
                    return true
                }
            }
        )
    }

    private fun filterHooks(query: String) {
        val filteredHooks =
            hookList.filter { hook ->
                hook.name.contains(query, ignoreCase = true) ||
                        hook.description.contains(query, ignoreCase = true)
            }
        populateHookList(filteredHooks)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_search -> true
            else -> false
        }
    }
}
