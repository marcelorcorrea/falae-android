package org.falaeapp.falae.activity

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
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
        // Importante: requestWindowFeature deve ser chamado antes de super.onCreate()
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY)
        super.onCreate(savedInstanceState)

        try {
            // Configuração moderna para Android 15 Edge-to-Edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Aplicar modo imersivo
            applyImmersiveMode()
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao configurar fullscreen: ${e.message}")
        }

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar Window Insets para evitar sobreposição com system bars
        setupWindowInsets()

        mDrawer = findViewById(R.id.drawer_layout)
        val toggle = object : ActionBarDrawerToggle(
            this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {
            override fun onOptionsItemSelected(item: MenuItem): Boolean {
                // Quando o ícone da casinha (hamburger) é clicado
                if (item.itemId == android.R.id.home) {
                    // Se o drawer está aberto, fechamos normalmente
                    if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                        mDrawer.closeDrawer(GravityCompat.START)
                        return true
                    }

                    // Se o drawer está fechado, carregamos o último usuário conectado
                    try {
                        Log.d("MainActivity", "Home icon clicked, loading last connected user")
                        userViewModel.loadLastConnectedUser()
                        return true
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error loading last connected user: ${e.message}", e)
                        // Em caso de erro, abrimos o drawer normalmente
                        mDrawer.openDrawer(GravityCompat.START)
                        return true
                    }
                }
                return super.onOptionsItemSelected(item)
            }
        }
        mDrawer.addDrawerListener(toggle)
        toggle.syncState()
        mDrawer.openDrawer(GravityCompat.START)
        mNavigationView = findViewById(R.id.nav_view)
        mNavigationView.setNavigationItemSelectedListener(this)

        // Registrando o callback para o botão voltar (substituindo onBackPressed depreciado)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                    mDrawer.closeDrawer(GravityCompat.START)
                } else {
                    if (doubleBackToExitPressedOnce) {
                        // Desabilitar o callback antes de chamar onBackPressed para evitar loops
                        isEnabled = false
                        // Finalizar a atividade diretamente em vez de chamar onBackPressed
                        finish()
                    } else {
                        doubleBackToExitPressedOnce = true
                        Toast.makeText(this@MainActivity, R.string.exit_app_msg, Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                    }
                }
            }
        })

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        userViewModel.handleNewVersion(BuildConfig.VERSION_CODE)
        observeUsers()
        observeLastConnectedUser()
    }

    override fun onResume() {
        super.onResume()
        // Reaplicar modo imersivo quando a atividade volta ao foco
        applyImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Reaplicar modo imersivo quando a janela ganha foco
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        try {
            // Para Android 15 e versões mais recentes, usar flags legados para esconder completamente a barra de navegação
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Erro ao aplicar modo imersivo: ${e.message}")
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
            users.reversed().forEach {
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
        
        // Reaplicar modo imersivo após trocar o fragment
        applyImmersiveMode()
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

        // Usando o método tradicional já que estamos com compileSdk 34
        startActivity(intent)
        @Suppress("DEPRECATION")
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

    /**
     * Configura Window Insets para evitar sobreposição com system bars
     */
    private fun setupWindowInsets() {
        val rootView = findViewById<DrawerLayout>(R.id.drawer_layout)
        
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // Aplicar padding apenas na parte inferior para evitar sobreposição com navigation bar
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navigationBarsInsets.bottom
            )
            
            Log.d("MainActivity", "Window Insets aplicados - Bottom: ${navigationBarsInsets.bottom}")
            
            insets
        }
    }

    companion object {
        private const val ERROR_DIALOG_REQUEST_CODE = 1
        private const val PROVIDER_INSTALLED = "provider_installed"
    }
}
