package com.grindrplus

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.grindrplus.databinding.ActivityMainBinding
import com.grindrplus.ui.AboutDialog
import com.grindrplus.utils.Config
import kotlin.Boolean
import kotlin.apply

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize the config before anything else so all the fragments
        // can access the config values without any issues (e.g. hooks).
        Config.initialize(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_settings, R.id.navigation_hooks
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_about -> {
                AboutDialog().show(supportFragmentManager, "AboutDialog")
                true
            }
            R.id.action_comments_help -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(
                        "https://github.com/R0rt1z2/GrindrPlus/issues")
                    startActivity(this)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}