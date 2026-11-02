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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
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
    private lateinit var btnGoogleSignIn: View
    private lateinit var btnPhoneSignIn: View
    private lateinit var btnGuest: TextView

    private var isRegisterMode = false
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleGoogleAccount(account)
        } catch (e: ApiException) {
            setLoading(false)
            if (e.statusCode == 12501) {
                // User cancelled dialog
                return@registerForActivityResult
            }
            val msg = when (e.statusCode) {
                7 -> "Unable to reach Google services. Please check your internet connection."
                else -> "Unable to sign in with Google right now. Please enter your email or continue as Guest."
            }
            showError(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Check if user is already authenticated
        val forceShow = intent.getBooleanExtra("FORCE_SHOW", false)
        if (!forceShow && FirebaseRepository.isUserLoggedIn()) {
            checkOnboardingAndProceed()
            return
        }

        setContentView(R.layout.activity_login)

        // Initialize Google Sign-In Client
        setupGoogleSignIn()

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
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnPhoneSignIn = findViewById(R.id.btnPhoneSignIn)
        btnGuest = findViewById(R.id.btnGuest)

        setupListeners()
        updateTabState()
    }

    private fun setupGoogleSignIn() {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        val resId = try {
            resources.getIdentifier("default_web_client_id", "string", packageName)
        } catch (_: Exception) { 0 }

        if (resId != 0) {
            val clientId = try { getString(resId) } catch (_: Exception) { "" }
            if (clientId.isNotEmpty()) {
                try {
                    gsoBuilder.requestIdToken(clientId)
                } catch (_: Exception) {}
            }
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())
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

        btnGoogleSignIn.setOnClickListener {
            clearBanners()
            setLoading(true)
            // Sign out of previous Google session to allow account chooser
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        btnPhoneSignIn.setOnClickListener {
            showPhoneAuthDialog()
        }

        btnGuest.setOnClickListener {
            clearBanners()
            setLoading(true)
            lifecycleScope.launch {
                val (ok, errorMsg) = FirebaseRepository.signInGuest()
                setLoading(false)
                if (ok) {
                    Toast.makeText(this@LoginActivity, "Continuing as Guest Student", Toast.LENGTH_SHORT).show()
                    checkOnboardingAndProceed()
                } else {
                    showError(errorMsg ?: "Guest sign-in failed. Please try again.")
                }
            }
        }
    }

    private fun handleGoogleAccount(account: GoogleSignInAccount) {
        val idToken = account.idToken
        val displayName = account.displayName ?: "Student"

        setLoading(true)
        lifecycleScope.launch {
            if (!idToken.isNullOrEmpty()) {
                val (ok, errorMsg) = FirebaseRepository.signInWithGoogleCredential(idToken)
                setLoading(false)
                if (ok) {
                    Toast.makeText(this@LoginActivity, "Welcome to MyBuddy, $displayName!", Toast.LENGTH_SHORT).show()
                    checkOnboardingAndProceed()
                    return@launch
                } else {
                    showError(formatFirebaseError(errorMsg))
                }
            } else {
                setLoading(false)
                showError("Unable to obtain Google sign-in token. Please sign in with email or continue as Guest.")
            }
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
            btnSubmit.text = "Sign In to MyBuddy"
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
            btnGoogleSignIn.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnSubmit.text = if (isRegisterMode) "Create Student Account" else "Sign In to MyBuddy"
            btnSubmit.isEnabled = true
            btnGuest.isEnabled = true
            btnGoogleSignIn.isEnabled = true
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
                    Toast.makeText(this@LoginActivity, "Welcome to MyBuddy, $fullName!", Toast.LENGTH_SHORT).show()
                    checkOnboardingAndProceed()
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
                    checkOnboardingAndProceed()
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
                "Password should be at least 6 characters."
            lower.contains("invalid-email") ->
                "The email address is improperly formatted. Please enter a valid email."
            lower.contains("network") ->
                "Network connection error. Please check your internet connection."
            else -> "Sign-in could not be completed. Please check your credentials or continue as Guest."
        }
    }

    private fun showPhoneAuthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_phone_auth, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvBanner = dialogView.findViewById<TextView>(R.id.tvPhoneAuthBanner)
        val etName = dialogView.findViewById<EditText>(R.id.etPhoneName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhoneNumber)
        val layoutOtp = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutOtpSection)
        val etOtp = dialogView.findViewById<EditText>(R.id.etPhoneOtp)
        val btnAction = dialogView.findViewById<TextView>(R.id.btnPhoneAction)
        val pbAuth = dialogView.findViewById<ProgressBar>(R.id.pbPhoneAuth)

        var otpSent = false
        var storedVerificationId: String? = null
        var resendToken: PhoneAuthProvider.ForceResendingToken? = null

        btnAction.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val name = etName.text.toString().trim()

            if (!otpSent) {
                if (phone.length < 10) {
                    tvBanner.text = "Please enter a valid 10-digit mobile number."
                    tvBanner.setBackgroundResource(R.drawable.bg_pill_coral)
                    return@setOnClickListener
                }

                pbAuth.visibility = View.VISIBLE
                btnAction.visibility = View.INVISIBLE
                tvBanner.text = "Sending SMS verification code to +91 $phone..."
                tvBanner.setBackgroundResource(R.drawable.bg_pill_lime)

                val fullPhone = "+91$phone"
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        pbAuth.visibility = View.GONE
                        btnAction.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            val (ok, err) = FirebaseRepository.signInWithPhoneCredential(credential, name)
                            if (ok) {
                                dialog.dismiss()
                                Toast.makeText(this@LoginActivity, "Mobile verified successfully!", Toast.LENGTH_SHORT).show()
                                checkOnboardingAndProceed()
                            } else {
                                tvBanner.text = err ?: "Phone verification failed."
                                tvBanner.setBackgroundResource(R.drawable.bg_pill_coral)
                            }
                        }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        pbAuth.visibility = View.GONE
                        btnAction.visibility = View.VISIBLE
                        val msg = when {
                            e.message?.contains("BILLING", ignoreCase = true) == true ||
                            e.message?.contains("quota", ignoreCase = true) == true ->
                                "SMS quota limit reached. Please sign in with email or continue as Guest."
                            e.message?.contains("invalid", ignoreCase = true) == true ->
                                "Invalid mobile number format."
                            else -> "Verification failed: ${e.localizedMessage ?: "Please try again."}"
                        }
                        tvBanner.text = msg
                        tvBanner.setBackgroundResource(R.drawable.bg_pill_coral)
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        pbAuth.visibility = View.GONE
                        btnAction.visibility = View.VISIBLE
                        storedVerificationId = verificationId
                        resendToken = token
                        otpSent = true
                        layoutOtp.visibility = View.VISIBLE
                        btnAction.text = "Verify OTP & Continue"
                        tvBanner.text = "Code sent to +91 $phone! Enter the 6-digit OTP below."
                        tvBanner.setBackgroundResource(R.drawable.bg_pill_lime)
                        etOtp.requestFocus()
                    }
                }

                val options = PhoneAuthOptions.newBuilder(FirebaseRepository.auth)
                    .setPhoneNumber(fullPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this@LoginActivity)
                    .setCallbacks(callbacks)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            } else {
                val otp = etOtp.text.toString().trim()
                if (otp.length < 6) {
                    tvBanner.text = "Please enter the complete 6-digit OTP code."
                    tvBanner.setBackgroundResource(R.drawable.bg_pill_coral)
                    return@setOnClickListener
                }

                if (storedVerificationId == null) {
                    tvBanner.text = "Verification session expired. Please request a new code."
                    tvBanner.setBackgroundResource(R.drawable.bg_pill_coral)
                    return@setOnClickListener
                }

                pbAuth.visibility = View.VISIBLE
                btnAction.visibility = View.INVISIBLE

                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
                lifecycleScope.launch {
                    val (success, errorMsg) = FirebaseRepository.signInWithPhoneCredential(credential, name)
                    pbAuth.visibility = View.GONE
                    btnAction.visibility = View.VISIBLE

                    if (success) {
                        dialog.dismiss()
                        Toast.makeText(this@LoginActivity, "Welcome to MyBuddy, ${if (name.isNotEmpty()) name else "Student"}!", Toast.LENGTH_SHORT).show()
                        checkOnboardingAndProceed()
                    } else {
                        tvBanner.text = errorMsg ?: "Invalid verification code. Please check and try again."
                        tvBanner.setBackgroundResource(R.drawable.bg_pill_coral)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun checkOnboardingAndProceed() {
        lifecycleScope.launch {
            val profile = FirebaseRepository.getProfile()
            if (!profile.isOnboardingCompleted && profile.college.isBlank() && profile.course.isBlank()) {
                startActivity(Intent(this@LoginActivity, OnboardingActivity::class.java))
            } else {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            }
            finish()
        }
    }
}
