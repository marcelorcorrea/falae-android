package org.falaeapp.falae.fragment

import android.app.ProgressDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.android.volley.AuthFailureError
import org.falaeapp.falae.R
import org.falaeapp.falae.exception.UserNotFoundException
import org.falaeapp.falae.util.Util
import org.falaeapp.falae.viewmodel.UserViewModel
import java.util.regex.Pattern

class SyncUserFragment : Fragment() {
    private lateinit var mListener: SyncUserFragmentListener

    private lateinit var mEmailView: EditText
    private lateinit var mPasswordView: EditText
    private var pDialog: ProgressDialog? = null
    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
        userViewModel.reposResult.observe(this, Observer { event ->
            event?.getContentIfNotHandled()?.let { result ->
                result.second?.let { error ->
                    onError(error)
                }
            }
            pDialog?.dismiss()
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sync_user, container, false)
        mEmailView = view.findViewById(R.id.email) as EditText
        mPasswordView = view.findViewById(R.id.password) as EditText
        mPasswordView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mPasswordView.windowToken, 0)
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        pDialog = ProgressDialog(context)
        pDialog?.apply {
            setMessage(context?.getString(R.string.authenticate_message))
            isIndeterminate = false
            setCancelable(false)
        }
        val mEmailSignInButton = view.findViewById(R.id.email_sign_in_button) as Button
        mEmailSignInButton.setOnClickListener { attemptLogin() }
        setHasOptionsMenu(true)
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is SyncUserFragmentListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement SyncUserFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        showSoftwareKeyboard(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        pDialog?.dismiss()
    }

    private fun showSoftwareKeyboard(showKeyboard: Boolean) {
        val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken,
                if (showKeyboard) InputMethodManager.SHOW_FORCED else InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun attemptLogin() {
        mEmailView.error = null
        mPasswordView.error = null

        val email = mEmailView.text.toString()
        val password = mPasswordView.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(email)) {
            mEmailView.error = getString(R.string.error_field_required)
            focusView = mEmailView
            cancel = true
        } else if (!isEmailValid(email)) {
            mEmailView.error = getString(R.string.error_invalid_email)
            focusView = mEmailView
            cancel = true
        } else if (TextUtils.isEmpty(password)) {
            mPasswordView.error = getString(R.string.error_field_required)
            focusView = mPasswordView
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            userViewModel.login(email, password)
            pDialog?.show()
        }
    }

    private fun isEmailValid(email: String): Boolean {
        val m = VALID_EMAIL_REGEX.matcher(email)
        return m.matches()
    }

    private fun onError(error: Exception) {
        if (error is AuthFailureError) {
            handleError(error)
        } else {
            Toast.makeText(context, getString(R.string.error_internet_access), Toast.LENGTH_LONG).show()
            error.printStackTrace()
        }
    }

    private fun handleError(error: AuthFailureError) {
        context?.let { context ->
            if (error is UserNotFoundException) {
                Util.createDialog(
                        context = context,
                        positiveText = getString(R.string.ok),
                        message = getString(R.string.create_accout_msg))
                        .show()
            } else {
                mPasswordView.error = getString(R.string.error_incorrect_password)
                mPasswordView.requestFocus()
            }
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
