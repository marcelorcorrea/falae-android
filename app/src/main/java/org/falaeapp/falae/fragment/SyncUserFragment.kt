package org.falaeapp.falae.fragment

import android.app.ProgressDialog
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
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import org.falaeapp.falae.BuildConfig
import org.falaeapp.falae.R
import org.falaeapp.falae.model.User
import org.falaeapp.falae.task.GsonRequest
import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

class SyncUserFragment : Fragment(), Response.Listener<User>, Response.ErrorListener {
    private lateinit var mListener: SyncUserFragmentListener

    private lateinit var mEmailView: EditText
    private lateinit var mPasswordView: EditText
    private lateinit var pDialog: ProgressDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_sync_user, container, false)
        mEmailView = view.findViewById(R.id.email) as EditText
        mPasswordView = view.findViewById(R.id.password) as EditText
        mPasswordView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(mPasswordView.windowToken, 0)
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        pDialog = ProgressDialog(context)
        pDialog.setMessage(context.getString(R.string.authenticate_message))
        pDialog.isIndeterminate = false
        pDialog.setCancelable(false)
        val mEmailSignInButton = view.findViewById(R.id.email_sign_in_button) as Button
        mEmailSignInButton.setOnClickListener { attemptLogin() }
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

    private fun showSoftwareKeyboard(showKeyboard: Boolean) {
        val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken,
                if (showKeyboard) InputMethodManager.SHOW_FORCED else InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun attemptLogin() {
        mEmailView.error = null
        mPasswordView.error = null

        val email = mEmailView.text.toString()
        val password = mPasswordView.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.error = getString(R.string.error_invalid_password)
            focusView = mPasswordView
            cancel = true
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.error = getString(R.string.error_field_required)
            focusView = mEmailView
            cancel = true
        } else if (!isEmailValid(email)) {
            mEmailView.error = getString(R.string.error_invalid_email)
            focusView = mEmailView
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            loginIn(email, password)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        val m = VALID_EMAIL_REGEX.matcher(email)
        return m.matches()
    }

    private fun isPasswordValid(password: String): Boolean = password.length > 4

    private fun loginIn(email: String, password: String) {
        try {
            val credentials = JSONObject()
            credentials.put(EMAIL_CREDENTIAL_FIELD, email)
            credentials.put(PASSWORD_CREDENTIAL_FIELD, password)

            val jsonRequest = JSONObject()
            jsonRequest.put(USER_CREDENTIAL_FIELD, credentials)

            val url = BuildConfig.BASE_URL + LOGIN_ENDPOINT
            val gsonRequest = GsonRequest(url = url,
                    clazz = User::class.java,
                    jsonRequest = jsonRequest,
                    listener = this,
                    errorListener = this)

            Volley.newRequestQueue(context).add(gsonRequest)
            pDialog.show()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onResponse(response: User) {
        mListener.onUserAuthenticated(response)
        if (pDialog.isShowing) {
            pDialog.dismiss()
        }
    }

    override fun onErrorResponse(error: VolleyError) {
        if (pDialog.isShowing) {
            pDialog.dismiss()
        }
        if (error is AuthFailureError) {
            mPasswordView.error = getString(R.string.error_incorrect_password)
            mPasswordView.requestFocus()
        } else {
            Toast.makeText(context, getString(R.string.error_internet_access), Toast.LENGTH_LONG).show()
            error.printStackTrace()
        }
    }

    interface SyncUserFragmentListener {
        fun onUserAuthenticated(user: User?)
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