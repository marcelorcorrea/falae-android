package org.falaeapp.falae.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.android.volley.AuthFailureError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.falaeapp.falae.R
import org.falaeapp.falae.exception.NoNetworkConnectionException
import org.falaeapp.falae.exception.UserNotFoundException
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.UserViewModel
import java.io.IOException
import java.util.regex.Pattern

class SyncUserFragment : Fragment() {
    private lateinit var mListener: SyncUserFragmentListener

    private lateinit var mEmailView: EditText
    private lateinit var mPasswordView: EditText
    private var progressDialog: androidx.appcompat.app.AlertDialog? = null
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var userViewModel: UserViewModel

    // Flag para evitar múltiplas tentativas de login
    private var isLoggingIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SyncUserFragment", "onCreate")

        // Inicializar o ViewModel no onCreate, mas configurar o observer no onViewCreated
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        Log.d("SyncUserFragment", "ViewModel initialized")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SyncUserFragment", "onViewCreated")

        // Configurar o observer no onViewCreated para garantir que viewLifecycleOwner esteja pronto
        setupObservers()
    }

    private fun setupObservers() {
        try {
            Log.d("SyncUserFragment", "Setting up observers")

            userViewModel.syncAccountEvent.observe(viewLifecycleOwner) { event ->
                Log.d("SyncUserFragment", "Received syncAccountEvent")
                isLoggingIn = false

                try {
                    // Esconder o diálogo de progresso primeiro para evitar problemas de UI
                    try {
                        progressDialog?.dismiss()
                    } catch (e: Exception) {
                        Log.e("SyncUserFragment", "Error dismissing progress dialog: ${e.message}")
                    }

                    event?.getContentIfNotHandled()?.let { pair ->
                        val user = pair.first
                        val error = pair.second

                        if (user != null) {
                            // Sucesso
                            Log.d("SyncUserFragment", "Sync successful for user: ${user.name}")

                            if (isAdded && context != null) {
                                try {
                                    Toast.makeText(requireContext(), getString(R.string.success_user_added), Toast.LENGTH_SHORT).show()

                                    // Navegar de volta usando a nova API de navegação
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        try {
                                            if (isAdded && activity != null) {
                                                Log.d("SyncUserFragment", "Navigating back")
                                                activity?.onBackPressed()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("SyncUserFragment", "Error navigating back: ${e.message}", e)
                                        }
                                    }, 500) // Pequeno atraso para garantir que o Toast seja exibido
                                } catch (e: Exception) {
                                    Log.e("SyncUserFragment", "Error showing success toast: ${e.message}", e)
                                }
                            }
                        } else if (error != null) {
                            // Erro
                            Log.e("SyncUserFragment", "Error during sync: ${error.message}", error)
                            if (isAdded && context != null) {
                                try {
                                    onError(error)
                                } catch (e: Exception) {
                                    Log.e("SyncUserFragment", "Error in onError: ${e.message}", e)
                                    try {
                                        Toast.makeText(requireContext(), getString(R.string.error_generic, error.message), Toast.LENGTH_LONG).show()
                                    } catch (e2: Exception) {
                                        Log.e("SyncUserFragment", "Error showing error toast: ${e2.message}", e2)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "Error in observer: ${e.message}", e)
                    try {
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), getString(R.string.error_processing_response, e.message), Toast.LENGTH_LONG).show()
                        }
                    } catch (e2: Exception) {
                        Log.e("SyncUserFragment", "Error showing error toast: ${e2.message}", e2)
                    }
                }
            }

            Log.d("SyncUserFragment", "Observers setup completed")
        } catch (e: Exception) {
            Log.e("SyncUserFragment", "Error setting up observers: ${e.message}", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        try {
            Log.d("SyncUserFragment", "onCreateView - Starting")

            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_sync_user, container, false)

            try {
                // Inicializar views com tratamento de erro
                try {
                    mEmailView = view.findViewById(R.id.email) as EditText
                    mPasswordView = view.findViewById(R.id.password) as EditText
                    Log.d("SyncUserFragment", "onCreateView - Views initialized")
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "onCreateView - Error initializing views: ${e.message}", e)
                }

                // Configurar listener do teclado com tratamento de erro
                try {
                    mPasswordView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                        try {
                            if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                                try {
                                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.hideSoftInputFromWindow(mPasswordView.windowToken, 0)
                                } catch (e: Exception) {
                                    Log.e("SyncUserFragment", "onCreateView - Error hiding keyboard: ${e.message}", e)
                                }

                                // Usar Handler para evitar problemas de UI thread
                                Handler(Looper.getMainLooper()).post {
                                    try {
                                        attemptLogin()
                                    } catch (e: Exception) {
                                        Log.e("SyncUserFragment", "onCreateView - Error in attemptLogin from keyboard: ${e.message}", e)
                                    }
                                }
                                return@OnEditorActionListener true
                            }
                            false
                        } catch (e: Exception) {
                            Log.e("SyncUserFragment", "onCreateView - Error in keyboard listener: ${e.message}", e)
                            false
                        }
                    })
                    Log.d("SyncUserFragment", "onCreateView - Keyboard listener set")
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "onCreateView - Error setting keyboard listener: ${e.message}", e)
                }

                // Criar o indicador de progresso circular com tratamento de erro
                try {
                    progressIndicator = CircularProgressIndicator(requireContext()).apply {
                        isIndeterminate = true
                        visibility = View.VISIBLE
                    }
                    Log.d("SyncUserFragment", "onCreateView - Progress indicator created")
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "onCreateView - Error creating progress indicator: ${e.message}", e)
                    // Criar um indicador padrão em caso de erro
                    progressIndicator = CircularProgressIndicator(requireContext())
                }

                // Criar o diálogo de progresso com tratamento de erro
                try {
                    progressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.synchronizing)
                        .setMessage(getString(R.string.synchronize_message))
                        .setView(progressIndicator)
                        .setCancelable(false)
                        .create()
                    Log.d("SyncUserFragment", "onCreateView - Progress dialog created")
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "onCreateView - Error creating progress dialog: ${e.message}", e)
                }

                // Configurar botão de login com tratamento de erro
                try {
                    val mEmailSignInButton = view.findViewById(R.id.email_sign_in_button) as Button
                    mEmailSignInButton.setOnClickListener {
                        try {
                            // Usar Handler para evitar problemas de UI thread
                            Handler(Looper.getMainLooper()).post {
                                try {
                                    attemptLogin()
                                } catch (e: Exception) {
                                    Log.e("SyncUserFragment", "onCreateView - Error in attemptLogin from button: ${e.message}", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SyncUserFragment", "onCreateView - Error in button click: ${e.message}", e)
                        }
                    }
                    Log.d("SyncUserFragment", "onCreateView - Sign in button configured")
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "onCreateView - Error configuring sign in button: ${e.message}", e)
                }

                // Configurar menu com tratamento de erro
                try {
                    requireActivity().addMenuProvider(object : MenuProvider {
                        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                            // Adicionar itens de menu se necessário
                        }

                        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                            return false
                        }
                    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
                    Log.d("SyncUserFragment", "onCreateView - Menu provider added")
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "onCreateView - Error adding menu provider: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e("SyncUserFragment", "onCreateView - Error in view setup: ${e.message}", e)
            }

            Log.d("SyncUserFragment", "onCreateView - Completed successfully")
            return view
        } catch (e: Exception) {
            Log.e("SyncUserFragment", "onCreateView - Critical error: ${e.message}", e)
            // Em caso de erro crítico, retornar uma view vazia para evitar crash
            return inflater.inflate(R.layout.fragment_sync_user, container, false)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SyncUserFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement SyncUserFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        showSoftwareKeyboard(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }

    private fun showSoftwareKeyboard(showKeyboard: Boolean) {
        val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        activity?.currentFocus?.windowToken?.let { token ->
            if (showKeyboard) {
                // SHOW_FORCED está depreciado, usar showSoftInput em vez disso
                inputManager.showSoftInput(activity?.currentFocus, InputMethodManager.SHOW_IMPLICIT)
            } else {
                inputManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }

    private fun attemptLogin() {
        // Evitar múltiplas tentativas de login simultâneas
        if (isLoggingIn) {
            Log.d("SyncUserFragment", "attemptLogin - Already logging in, ignoring request")
            return
        }

        try {
            isLoggingIn = true
            Log.d("SyncUserFragment", "attemptLogin - Starting login attempt")

            if (!isAdded || context == null) {
                Log.e("SyncUserFragment", "attemptLogin - Fragment not attached to context")
                isLoggingIn = false
                return
            }

            // Limpar erros anteriores
            mEmailView.error = null
            mPasswordView.error = null

            val email = mEmailView.text.toString().trim()
            val password = mPasswordView.text.toString().trim()

            // Validação básica
            if (email.isEmpty()) {
                mEmailView.error = getString(R.string.error_field_required)
                mEmailView.requestFocus()
                isLoggingIn = false
                return
            }

            if (password.isEmpty()) {
                mPasswordView.error = getString(R.string.error_field_required)
                mPasswordView.requestFocus()
                isLoggingIn = false
                return
            }

            // Esconder teclado
            try {
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view?.windowToken, 0)
            } catch (e: Exception) {
                Log.e("SyncUserFragment", "Error hiding keyboard: ${e.message}")
            }

            // Mostrar diálogo de progresso
            try {
                progressDialog?.show()
            } catch (e: Exception) {
                Log.e("SyncUserFragment", "Error showing progress dialog: ${e.message}")
            }

            // Iniciar sincronização em um handler separado para evitar problemas de UI thread
            Handler(Looper.getMainLooper()).post {
                try {
                    Log.d("SyncUserFragment", "Calling synchronizeUser with email: $email")
                    userViewModel.synchronizeUser(email, password)
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "Error in synchronizeUser: ${e.message}", e)
                    try {
                        progressDialog?.dismiss()
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                        }
                    } catch (e2: Exception) {
                        Log.e("SyncUserFragment", "Error handling exception: ${e2.message}", e2)
                    }
                    isLoggingIn = false
                }
            }
        } catch (e: Exception) {
            Log.e("SyncUserFragment", "Critical error in attemptLogin: ${e.message}", e)
            isLoggingIn = false
            try {
                progressDialog?.dismiss()
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), getString(R.string.error_critical, e.message), Toast.LENGTH_LONG).show()
                }
            } catch (e2: Exception) {
                Log.e("SyncUserFragment", "Error handling critical exception: ${e2.message}", e2)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    private fun isEmailValid(email: String): Boolean {
        val m = VALID_EMAIL_REGEX.matcher(email)
        return m.matches()
    }

    private fun onError(error: Exception) {
        try {
            Log.e("SyncUserFragment", "onError: ${error.message}", error)

            if (!isAdded || context == null) {
                Log.e("SyncUserFragment", "onError - Fragment not attached to context")
                return
            }

            when (error) {
                is AuthFailureError -> {
                    handleAuthError(error)
                }
                is NoNetworkConnectionException -> {
                    Toast.makeText(requireContext(), getString(R.string.error_no_network), Toast.LENGTH_LONG).show()
                }
                is IOException -> {
                    Toast.makeText(requireContext(), getString(R.string.error_io_exception), Toast.LENGTH_LONG).show()
                }
                else -> {
                    val errorMessage = error.message ?: "Erro desconhecido"
                    Log.e("SyncUserFragment", "Unknown error: $errorMessage")
                    Toast.makeText(requireContext(), getString(R.string.error_internet_access), Toast.LENGTH_LONG).show()
                    error.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncUserFragment", "Error in onError handler: ${e.message}", e)
        }
    }

    private fun handleAuthError(error: AuthFailureError) {
        try {
            Log.d("SyncUserFragment", "handleAuthError: ${error.javaClass.simpleName}")

            if (!isAdded || context == null) {
                Log.e("SyncUserFragment", "handleAuthError - Fragment not attached to context")
                return
            }

            if (error is UserNotFoundException) {
                Log.d("SyncUserFragment", "User not found, showing create account dialog")
                try {
                    Util.createDialog(
                            context = requireContext(),
                            positiveText = getString(R.string.ok),
                            message = getString(R.string.create_accout_msg))
                            .show()
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "Error showing dialog: ${e.message}", e)
                    Toast.makeText(requireContext(), getString(R.string.create_accout_msg), Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("SyncUserFragment", "Authentication failed, showing incorrect password error")
                try {
                    mPasswordView.error = getString(R.string.error_incorrect_password)
                    mPasswordView.requestFocus()
                } catch (e: Exception) {
                    Log.e("SyncUserFragment", "Error setting password error: ${e.message}", e)
                    Toast.makeText(requireContext(), getString(R.string.error_incorrect_password), Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("SyncUserFragment", "Error in handleAuthError: ${e.message}", e)
        }
    }

    interface SyncUserFragmentListener {
    }

    companion object {

        private const val LOGIN_ENDPOINT = "/login.json"
        private const val EMAIL_CREDENTIAL_FIELD = "email"
        private const val PASSWORD_CREDENTIAL_FIELD = "password"
        private const val USER_CREDENTIAL_FIELD = "user"
        private val VALID_EMAIL_REGEX = Pattern.compile("\\A[\\w+\\-.]+@[a-z\\d\\-.]+\\.[a-z]+\\z", Pattern.CASE_INSENSITIVE)

        fun newInstance(): SyncUserFragment = SyncUserFragment()
    }
}
