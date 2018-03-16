package org.falaeapp.falae.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import org.falaeapp.falae.R
import org.falaeapp.falae.database.DownloadCacheDbHelper
import org.falaeapp.falae.database.UserDbHelper
import org.falaeapp.falae.fragment.SettingsFragment
import org.falaeapp.falae.fragment.SyncUserFragment
import org.falaeapp.falae.fragment.TabPagerFragment
import org.falaeapp.falae.loadUser
import org.falaeapp.falae.model.SpreadSheet
import org.falaeapp.falae.model.User
import org.falaeapp.falae.storage.FileHandler
import org.falaeapp.falae.storage.SharedPreferencesUtils
import org.falaeapp.falae.task.DownloadTask
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        TabPagerFragment.TabPagerFragmentListener,
        SyncUserFragment.SyncUserFragmentListener,
        ProviderInstaller.ProviderInstallListener {

    private lateinit var mDrawer: DrawerLayout
    private lateinit var mNavigationView: NavigationView
    private lateinit var dbHelper: UserDbHelper
    private lateinit var downloadCacheDbHelper: DownloadCacheDbHelper
    private var mCurrentUser: User? = null
    private var doubleBackToExitPressedOnce: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        mCurrentUser = savedInstanceState?.let {
            savedInstanceState.classLoader = classLoader
            it.getParcelable(USER_PARAM)
        }
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        mDrawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mDrawer.addDrawerListener(toggle)
        toggle.syncState()
        mDrawer.openDrawer(GravityCompat.START)
        mNavigationView = findViewById(R.id.nav_view) as NavigationView
        mNavigationView.setNavigationItemSelectedListener(this)
        dbHelper = UserDbHelper(this)
        downloadCacheDbHelper = DownloadCacheDbHelper(this)
        val users = dbHelper.read()
        for (user in users) {
            addUserToMenu(user)
        }
        loadDemoUser()
        getLastConnectedUser()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            ProviderInstaller.installIfNeededAsync(this, this)
        }
    }

    private fun getLastConnectedUser() {
        val email = SharedPreferencesUtils.getString(USER_EMAIL, this)
        if (email.isNotEmpty()) {
            openUserMenuItem(email)
        } else {
            onNavigationItemSelected(mNavigationView.menu.findItem(R.id.add_user))
            mDrawer.openDrawer(GravityCompat.START)
        }
    }

    private fun openUserMenuItem(email: String) {
        mCurrentUser = dbHelper.findByEmail(email)
        val item = mCurrentUser?.id?.let { mNavigationView.menu.findItem(it) }
        item?.let { onNavigationItemSelected(it) }
    }

    private fun loadDemoUser() {
        val demoUser = resources.loadUser(getString(R.string.sampleboard))
        addUserToMenu(demoUser, R.id.settings_group, 1) { null }
    }

    private fun addUserToMenu(user: User, groupId: Int = R.id.users_group, order: Int = 0, findUser: (User) -> User? = this::findUser) {
        val userItem = mNavigationView.menu.add(groupId, user.id, order, user.name)
        userItem.setIcon(R.drawable.ic_person_black_24dp)
        userItem.setOnMenuItemClickListener { item ->
            mCurrentUser = findUser(user) ?: user
            onNavigationItemSelected(item)
            true
        }
    }

    private fun findUser(user: User) = dbHelper.findByEmail(user.email)

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
        val id = item.itemId
        when (id) {
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
                fragment = TabPagerFragment.newInstance(mCurrentUser)
                tag = TabPagerFragment::class.java.simpleName
            }
        }
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left,
                        R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.container, fragment, tag)
                .commit()

        item.isChecked = true
        title = item.title
        mDrawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onDestroy() {
        dbHelper.close()
        downloadCacheDbHelper.close()
        mCurrentUser?.let {
            SharedPreferencesUtils.storeString(USER_EMAIL, it.email, this)
        }
        super.onDestroy()
    }

    override fun onUserAuthenticated(user: User?) {
        DownloadTask(WeakReference(this), downloadCacheDbHelper, { u ->
            if (!dbHelper.doesUserExist(u)) {
                val id = dbHelper.insert(u)
                addUserToMenu(u.copy(id = id.toInt()))
            } else {
                dbHelper.update(u)
            }
            Toast.makeText(this@MainActivity, R.string.success_user_added, Toast.LENGTH_SHORT).show()
            openUserMenuItem(u.email)
        }).execute(user)
    }

    private fun openTTSLanguageSettings() {
        val installTts = Intent()
        installTts.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
        startActivity(installTts)
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
                    ERROR_DIALOG_REQUEST_CODE,
                    DialogInterface.OnCancelListener {
                        onProviderInstallerNotAvailable()
                    }
            )
        } else {
            onProviderInstallerNotAvailable()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED)
                onProviderInstallerNotAvailable()
        }
    }

    private fun onProviderInstallerNotAvailable() {
        Toast.makeText(this, getString(R.string.provider_not_available), Toast.LENGTH_LONG).show()
    }

    override fun removeUser(user: User) {
        dbHelper.remove(user.id)
        downloadCacheDbHelper.remove(user.email)
        SharedPreferencesUtils.remove(USER_EMAIL, this)
        FileHandler.deleteUserFolder(this, user.email)
        mCurrentUser = null
        recreate()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mCurrentUser?.let { outState?.putParcelable(USER_PARAM, mCurrentUser) }
    }

    companion object {
        private const val USER_EMAIL = "email"
        private const val USER_PARAM = "UserParam"
        private const val ERROR_DIALOG_REQUEST_CODE = 1
        private const val PROVIDER_INSTALLED = "provider_installed"
    }
}
