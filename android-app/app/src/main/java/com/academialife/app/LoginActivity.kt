package com.academialife.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var tabSignIn: TextView
    private lateinit var tabRegister: TextView

    private lateinit var tvAuthError: TextView
    private lateinit var tvAuthSuccess: TextView

    private lateinit var lblFullName: TextView
    private lateinit var etFullName: EditText
    private lateinit var lblCollege: TextView
    private lateinit var etCollege: EditText

    private lateinit var etEmail: EditText

    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView

    private lateinit var lblConfirmPassword: TextView
    private lateinit var layoutConfirmPassword: RelativeLayout
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivToggleConfirmPassword: ImageView

    private lateinit var btnForgotPassword: TextView
    private lateinit var btnSubmit: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnGuest: TextView

    private var isRegisterMode = false
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already authenticated
        val forceShow = intent.getBooleanExtra("FORCE_SHOW", false)
        if (!forceShow && FirebaseRepository.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        tabSignIn = findViewById(R.id.tabSignIn)
        tabRegister = findViewById(R.id.tabRegister)

        tvAuthError = findViewById(R.id.tvAuthError)
        tvAuthSuccess = findViewById(R.id.tvAuthSuccess)

        lblFullName = findViewById(R.id.lblFullName)
        etFullName = findViewById(R.id.etFullName)
        lblCollege = findViewById(R.id.lblCollege)
        etCollege = findViewById(R.id.etCollege)

        etEmail = findViewById(R.id.etEmail)

        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)

        lblConfirmPassword = findViewById(R.id.lblConfirmPassword)
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)

        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressBar = findViewById(R.id.progressBar)
        btnGuest = findViewById(R.id.btnGuest)

        setupListeners()
        updateTabState()
    }

    private fun setupListeners() {
        tabSignIn.setOnClickListener {
            if (isRegisterMode) {
                isRegisterMode = false
                updateTabState()
            }
        }

        tabRegister.setOnClickListener {
            if (!isRegisterMode) {
                isRegisterMode = true
                updateTabState()
            }
        }

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            }
            etPassword.typeface = android.graphics.Typeface.DEFAULT
            etPassword.setSelection(etPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
            } else {
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
            }
            etConfirmPassword.typeface = android.graphics.Typeface.DEFAULT
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        btnForgotPassword.setOnClickListener {
            handleForgotPassword()
        }

        btnSubmit.setOnClickListener {
            handleSubmit()
        }

        btnGuest.setOnClickListener {
            Toast.makeText(this, "Continuing as Guest Student", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun updateTabState() {
        clearBanners()

        if (isRegisterMode) {
            tabRegister.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
            tabRegister.setTextColor(resources.getColor(R.color.white, null))
            tabSignIn.setBackgroundResource(android.R.color.transparent)
            tabSignIn.setTextColor(resources.getColor(R.color.text_secondary, null))

            lblFullName.visibility = View.VISIBLE
            etFullName.visibility = View.VISIBLE
            lblCollege.visibility = View.VISIBLE
            etCollege.visibility = View.VISIBLE
            lblConfirmPassword.visibility = View.VISIBLE
            layoutConfirmPassword.visibility = View.VISIBLE

            btnForgotPassword.visibility = View.GONE
            btnSubmit.text = "Create Student Account"
        } else {
            tabSignIn.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
            tabSignIn.setTextColor(resources.getColor(R.color.white, null))
            tabRegister.setBackgroundResource(android.R.color.transparent)
            tabRegister.setTextColor(resources.getColor(R.color.text_secondary, null))

            lblFullName.visibility = View.GONE
            etFullName.visibility = View.GONE
            lblCollege.visibility = View.GONE
            etCollege.visibility = View.GONE
            lblConfirmPassword.visibility = View.GONE
            layoutConfirmPassword.visibility = View.GONE

            btnForgotPassword.visibility = View.VISIBLE
            btnSubmit.text = "Sign In to AcademiaLife"
        }
    }

    private fun clearBanners() {
        tvAuthError.visibility = View.GONE
        tvAuthSuccess.visibility = View.GONE
    }

    private fun showError(message: String) {
        tvAuthSuccess.visibility = View.GONE
        tvAuthError.text = message
        tvAuthError.visibility = View.VISIBLE
    }

    private fun showSuccess(message: String) {
        tvAuthError.visibility = View.GONE
        tvAuthSuccess.text = message
        tvAuthSuccess.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            progressBar.visibility = View.VISIBLE
            btnSubmit.text = ""
            btnSubmit.isEnabled = false
            btnGuest.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnSubmit.text = if (isRegisterMode) "Create Student Account" else "Sign In to AcademiaLife"
            btnSubmit.isEnabled = true
            btnGuest.isEnabled = true
        }
    }

    private fun handleForgotPassword() {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            showError("Please enter your student email address above to receive a password reset link.")
            etEmail.requestFocus()
            return
        }

        clearBanners()
        setLoading(true)

        lifecycleScope.launch {
            val (success, errorMsg) = FirebaseRepository.sendPasswordReset(email)
            setLoading(false)
            if (success) {
                showSuccess("Password reset email sent to $email. Please check your inbox.")
            } else {
                showError(errorMsg ?: "Unable to send password reset email. Please try again.")
            }
        }
    }

    private fun handleSubmit() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty()) {
            showError("Please enter your student email.")
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.")
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showError("Please enter your password.")
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters long.")
            etPassword.requestFocus()
            return
        }

        if (isRegisterMode) {
            val fullName = etFullName.text.toString().trim()
            val college = etCollege.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (fullName.isEmpty()) {
                showError("Please enter your full name.")
                etFullName.requestFocus()
                return
            }

            if (password != confirmPass) {
                showError("Passwords do not match. Please re-enter.")
                etConfirmPassword.requestFocus()
                return
            }

            clearBanners()
            setLoading(true)

            lifecycleScope.launch {
                val (ok, errorMsg) = FirebaseRepository.registerEmail(email, password, fullName)
                if (ok) {
                    if (college.isNotEmpty()) {
                        val currentProfile = FirebaseRepository.getProfile()
                        FirebaseRepository.saveProfile(currentProfile.copy(college = college))
                    }
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, "Welcome to AcademiaLife, $fullName!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    setLoading(false)
                    showError(formatFirebaseError(errorMsg))
                }
            }
        } else {
            clearBanners()
            setLoading(true)

            lifecycleScope.launch {
                val (ok, errorMsg) = FirebaseRepository.signInEmail(email, password)
                setLoading(false)
                if (ok) {
                    Toast.makeText(this@LoginActivity, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    showError(formatFirebaseError(errorMsg))
                }
            }
        }
    }

    private fun formatFirebaseError(raw: String?): String {
        if (raw == null) return "An unexpected error occurred. Please try again."
        val lower = raw.lowercase()
        return when {
            lower.contains("user-not-found") || lower.contains("no user record") ->
                "No student account found with this email. Tap 'Create Account' above to register!"
            lower.contains("wrong-password") || lower.contains("invalid-credential") || lower.contains("credential is incorrect") ->
                "Incorrect password or account does not exist. If this is your first time, tap 'Create Account' above!"
            lower.contains("email-already-in-use") ->
                "An account already exists with this email address. Please tap 'Sign In' or use 'Forgot Password'."
            lower.contains("weak-password") ->
                "Password is too weak. Please use at least 6 characters with letters and numbers."
            lower.contains("invalid-email") ->
                "The email address is improperly formatted. Please enter a valid email."
            lower.contains("network") ->
                "Network connection error. Please check your internet connection."
            else -> raw
        }
    }
}
