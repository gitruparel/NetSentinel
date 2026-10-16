package com.netsentinel.app.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import android.media.MediaPlayer
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.netsentinel.app.R
import com.netsentinel.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var mediaPlayer: MediaPlayer? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("netsentinel_settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", true)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dashboard,
                R.id.nav_hunt,
                R.id.nav_map,
                R.id.nav_logs,
                R.id.nav_settings
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        requestRequiredPermissions()
    }

    fun playThreatAlarm(onCompletion: (() -> Unit)? = null): Boolean {
        return try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.threat_alarm).apply {
                    setOnCompletionListener {
                        releaseMediaPlayer()
                        onCompletion?.invoke()
                    }
                }
            }

            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                    Toast.makeText(this, "🚨 ACOUSTIC SIREN DISPATCHED — INTRUSION DETECTED", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Toast.makeText(this, "Siren Error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun stopThreatAlarm() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
            releaseMediaPlayer()
            Toast.makeText(this, "🔇 Acoustic Threat Siren Silenced", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            releaseMediaPlayer()
        }
    }

    fun isThreatAlarmPlaying(): Boolean = mediaPlayer?.isPlaying == true

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer?.isPlaying == true) {
            stopThreatAlarm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
    }

    fun selectBottomNavItem(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_simulation -> {
                navController.navigate(R.id.nav_simulation)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
