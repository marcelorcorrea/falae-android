package org.falaeapp.falae.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.navigation.NavigationView
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.R
import org.falaeapp.falae.fragment.SettingsFragment
import org.falaeapp.falae.fragment.SyncUserFragment
import org.falaeapp.falae.fragment.TabPagerFragment
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.model.User
import org.falaeapp.falae.viewmodel.UserViewModel

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    TabPagerFragment.TabPagerFragmentListener,
    SyncUserFragment.SyncUserFragmentListener,
    ProviderInstaller.ProviderInstallListener {

    private lateinit var mDrawer: DrawerLayout
    private lateinit var mNavigationView: NavigationView
    private var doubleBackToExitPressedOnce: Boolean = false

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mDrawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        mDrawer.addDrawerListener(toggle)
        toggle.syncState()
        mDrawer.openDrawer(GravityCompat.START)
        mNavigationView = findViewById(R.id.nav_view)
        mNavigationView.setNavigationItemSelectedListener(this)

        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)

        userViewModel.handleNewVersion(BuildConfig.VERSION_CODE)
        observeUsers()
        observeLastConnectedUser()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            ProviderInstaller.installIfNeededAsync(this, this)
        }
    }

    private fun observeLastConnectedUser() {
        userViewModel.lastConnectedUserId.observe(this, Observer {
            it?.let { lastConnectedUserId ->
                openUserItem(lastConnectedUserId)
            }
        })
    }

    private fun observeUsers() {
        userViewModel.users.observe(this, Observer<List<User>> { users ->
            mNavigationView.menu.removeGroup(R.id.users_group)
            users?.reversed()?.forEach {
                addUserToMenu(it)
            }
            userViewModel.loadLastConnectedUser()
        })
    }

    private fun openUserItem(userId: Long) {
        val item = mNavigationView.menu.findItem(userId.toInt())
        item?.let { onNavigationItemSelected(it) }
    }

    private fun addUserToMenu(user: User, groupId: Int = R.id.users_group, order: Int = 1) {
        val userItem = mNavigationView.menu.add(groupId, user.id, order, user.name)
        userItem.setIcon(R.drawable.ic_person_black_24dp)
        userItem.setOnMenuItemClickListener { item ->
            onNavigationItemSelected(item)
            true
        }
    }

    override fun onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START)
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
            } else {
                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, R.string.exit_app_msg, Toast.LENGTH_SHORT).show()
                Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment: Fragment
        val tag: String
        item.isChecked = true
        title = item.title
        mDrawer.closeDrawer(GravityCompat.START)

        when (val id = item.itemId) {
            R.id.add_user -> {
                fragment = SyncUserFragment.newInstance()
                tag = SyncUserFragment::class.java.simpleName
            }
            R.id.voice_item -> {
                openTTSLanguageSettings()
                return false
            }
            R.id.settings -> {
                fragment = SettingsFragment.newInstance()
                tag = SettingsFragment::class.java.simpleName
            }
            else -> {
                userViewModel.loadUser(id.toLong())
                fragment = TabPagerFragment.newInstance()
                tag = TabPagerFragment::class.java.simpleName
            }
        }
        changeFragment(fragment, tag)
        return true
    }

    private fun changeFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right, R.anim.exit_to_left,
                R.anim.enter_from_left, R.anim.exit_to_right
            )
            .replace(R.id.container, fragment, tag)
            .commit()
    }

    private fun openTTSLanguageSettings() {
        try {
            val installTts = Intent()
            installTts.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            startActivity(installTts)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.language_settings_not_available), Toast.LENGTH_LONG).show()
        }
    }

    override fun displayActivity(spreadSheet: SpreadSheet) {
        val intent = Intent(this, DisplayActivity::class.java)
        intent.putExtra(DisplayActivity.SPREADSHEET, spreadSheet)
        startActivity(intent)
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun onProviderInstalled() {
        Log.d(javaClass.name, "Provider installed or up to date.")
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
        if (GoogleApiAvailability.getInstance().isUserResolvableError(errorCode)) {
            GoogleApiAvailability.getInstance().showErrorDialogFragment(
                this,
                errorCode,
                ERROR_DIALOG_REQUEST_CODE
            ) {
                onProviderInstallerNotAvailable()
            }
        } else {
            onProviderInstallerNotAvailable()
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED)
                onProviderInstallerNotAvailable()
        }
    }

    private fun onProviderInstallerNotAvailable() {
        Toast.makeText(this, getString(R.string.provider_not_available), Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val ERROR_DIALOG_REQUEST_CODE = 1
        private const val PROVIDER_INSTALLED = "provider_installed"
    }
}
