package com.academialife.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LifestyleFragment : Fragment() {

    private lateinit var tabWallet: TextView
    private lateinit var tabWellness: TextView
    private lateinit var tabRewards: TextView

    private lateinit var sectionWallet: LinearLayout
    private lateinit var sectionWellness: LinearLayout
    private lateinit var sectionRewards: LinearLayout

    // Wallet Views
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var tvBudgetDetail: TextView
    private lateinit var pbBudget: ProgressBar
    private lateinit var btnAddExpense: TextView
    private lateinit var layoutExpensesList: LinearLayout

    // Emergency Wallet Views
    private lateinit var tvWalletLockBadge: TextView
    private lateinit var layoutWalletLocked: LinearLayout
    private lateinit var layoutWalletUnlocked: LinearLayout
    private lateinit var btnUnlockEmergencyWallet: TextView
    private lateinit var layoutCredentialsList: LinearLayout
    private lateinit var btnAddEmergencyId: TextView
    private lateinit var btnLockEmergencyWallet: TextView

    // Wellness Views
    private lateinit var tvCurrentMoodStatus: TextView
    private lateinit var moodExcited: TextView
    private lateinit var moodMotivated: TextView
    private lateinit var moodCalm: TextView
    private lateinit var moodTired: TextView
    private lateinit var moodStressed: TextView
    private lateinit var tvWellnessWaterCount: TextView
    private lateinit var pbWellnessWater: ProgressBar
    private lateinit var btnDrinkGlass: TextView
    private lateinit var tvSleepDisplay: TextView
    private lateinit var etJournalReflection: EditText
    private lateinit var btnSaveReflection: TextView

    // Gamification Views
    private lateinit var tvGamificationTitle: TextView
    private lateinit var tvGamificationPoints: TextView
    private lateinit var pbXpProgress: ProgressBar
    private lateinit var layoutBadgesList: LinearLayout

    private var activeTab: String = "wallet"

    companion object {
        private const val ARG_TAB = "arg_initial_tab"

        fun newInstance(initialTab: String = "wallet"): LifestyleFragment {
            val fragment = LifestyleFragment()
            val args = Bundle()
            args.putString(ARG_TAB, initialTab)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeTab = arguments?.getString(ARG_TAB) ?: "wallet"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_lifestyle, container, false)

        tabWallet = root.findViewById(R.id.tabWallet)
        tabWellness = root.findViewById(R.id.tabWellness)
        tabRewards = root.findViewById(R.id.tabRewards)

        sectionWallet = root.findViewById(R.id.sectionWallet)
        sectionWellness = root.findViewById(R.id.sectionWellness)
        sectionRewards = root.findViewById(R.id.sectionRewards)

        // Wallet
        tvBudgetRemaining = root.findViewById(R.id.tvBudgetRemaining)
        tvBudgetDetail = root.findViewById(R.id.tvBudgetDetail)
        pbBudget = root.findViewById(R.id.pbBudget)
        btnAddExpense = root.findViewById(R.id.btnAddExpense)
        layoutExpensesList = root.findViewById(R.id.layoutExpensesList)

        // Emergency Wallet
        tvWalletLockBadge = root.findViewById(R.id.tvWalletLockBadge)
        layoutWalletLocked = root.findViewById(R.id.layoutWalletLocked)
        layoutWalletUnlocked = root.findViewById(R.id.layoutWalletUnlocked)
        btnUnlockEmergencyWallet = root.findViewById(R.id.btnUnlockEmergencyWallet)
        layoutCredentialsList = root.findViewById(R.id.layoutCredentialsList)
        btnAddEmergencyId = root.findViewById(R.id.btnAddEmergencyId)
        btnLockEmergencyWallet = root.findViewById(R.id.btnLockEmergencyWallet)

        // Wellness
        tvCurrentMoodStatus = root.findViewById(R.id.tvCurrentMoodStatus)
        moodExcited = root.findViewById(R.id.moodExcited)
        moodMotivated = root.findViewById(R.id.moodMotivated)
        moodCalm = root.findViewById(R.id.moodCalm)
        moodTired = root.findViewById(R.id.moodTired)
        moodStressed = root.findViewById(R.id.moodStressed)
        tvWellnessWaterCount = root.findViewById(R.id.tvWellnessWaterCount)
        pbWellnessWater = root.findViewById(R.id.pbWellnessWater)
        btnDrinkGlass = root.findViewById(R.id.btnDrinkGlass)
        tvSleepDisplay = root.findViewById(R.id.tvSleepDisplay)
        etJournalReflection = root.findViewById(R.id.etJournalReflection)
        btnSaveReflection = root.findViewById(R.id.btnSaveReflection)

        // Gamification
        tvGamificationTitle = root.findViewById(R.id.tvGamificationTitle)
        tvGamificationPoints = root.findViewById(R.id.tvGamificationPoints)
        pbXpProgress = root.findViewById(R.id.pbXpProgress)
        layoutBadgesList = root.findViewById(R.id.layoutBadgesList)

        setupTabNavigation()
        setupWalletActions()
        setupWellnessActions()

        switchTab(activeTab)
        return root
    }

    override fun onResume() {
        super.onResume()
        refreshActiveData()
    }

    private fun setupTabNavigation() {
        tabWallet.setOnClickListener { switchTab("wallet") }
        tabWellness.setOnClickListener { switchTab("wellness") }
        tabRewards.setOnClickListener { switchTab("rewards") }
    }

    fun switchTab(tab: String) {
        activeTab = tab
        val context = requireContext()

        // Reset tab styles
        tabWallet.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        tabWallet.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        tabWellness.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        tabWellness.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        tabRewards.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        tabRewards.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

        sectionWallet.visibility = View.GONE
        sectionWellness.visibility = View.GONE
        sectionRewards.visibility = View.GONE

        when (tab) {
            "wellness" -> {
                tabWellness.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                tabWellness.setTextColor(ContextCompat.getColor(context, R.color.white))
                sectionWellness.visibility = View.VISIBLE
                loadWellnessData()
            }
            "rewards" -> {
                tabRewards.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                tabRewards.setTextColor(ContextCompat.getColor(context, R.color.white))
                sectionRewards.visibility = View.VISIBLE
                loadRewardsData()
            }
            else -> {
                tabWallet.setBackgroundResource(R.drawable.bg_neumorphic_btn_primary_idle)
                tabWallet.setTextColor(ContextCompat.getColor(context, R.color.white))
                sectionWallet.visibility = View.VISIBLE
                loadWalletData()
            }
        }
    }

    private fun refreshActiveData() {
        when (activeTab) {
            "wellness" -> loadWellnessData()
            "rewards" -> loadRewardsData()
            else -> loadWalletData()
        }
    }

    // ==========================================
    // 1. WALLET & EXPENSES
    // ==========================================
    private fun setupWalletActions() {
        btnAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
        setupEmergencyWalletActions()
    }

    private fun setupEmergencyWalletActions() {
        btnUnlockEmergencyWallet.setOnClickListener {
            val act = activity as? AppCompatActivity ?: return@setOnClickListener
            SecurityWalletManager.authenticate(
                activity = act,
                title = "Unlock Emergency ID Wallet",
                subtitle = "Authenticate to access encrypted student IDs",
                onSuccess = {
                    Toast.makeText(context, "Wallet Unlocked (60s session)", Toast.LENGTH_SHORT).show()
                    updateEmergencyWalletUI()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }

        btnLockEmergencyWallet.setOnClickListener {
            SecurityWalletManager.lockWallet()
            Toast.makeText(context, "Emergency Wallet Locked", Toast.LENGTH_SHORT).show()
            updateEmergencyWalletUI()
        }

        btnAddEmergencyId.setOnClickListener {
            showAddEmergencyIdDialog()
        }
    }

    private fun updateEmergencyWalletUI() {
        if (SecurityWalletManager.isUnlocked()) {
            tvWalletLockBadge.text = "🔓 Active"
            tvWalletLockBadge.setBackgroundResource(R.drawable.bg_pill_lime)
            layoutWalletLocked.visibility = View.GONE
            layoutWalletUnlocked.visibility = View.VISIBLE
            loadDecryptedCredentials()
        } else {
            tvWalletLockBadge.text = "🔒 Locked"
            tvWalletLockBadge.setBackgroundResource(R.drawable.bg_pill_coral)
            layoutWalletLocked.visibility = View.VISIBLE
            layoutWalletUnlocked.visibility = View.GONE
        }
    }

    private fun loadDecryptedCredentials() {
        layoutCredentialsList.removeAllViews()
        viewLifecycleOwner.lifecycleScope.launch {
            val docs = FirebaseRepository.getEmergencyDocuments()
            if (docs.isEmpty()) {
                val tv = TextView(context).apply {
                    text = "No emergency credentials saved yet. Tap '+ Add ID' below."
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    setPadding(12, 16, 12, 16)
                    textSize = 13f
                }
                layoutCredentialsList.addView(tv)
            } else {
                docs.forEach { doc ->
                    val card = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.bg_neumorphic_sunken)
                        setPadding(16, 14, 16, 14)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 8, 0, 8) }
                        layoutParams = params
                    }

                    val titleRow = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    val tvTitle = TextView(context).apply {
                        text = "🪪 " + doc.title
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                        textSize = 14f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val btnDelete = TextView(context).apply {
                        text = "🗑️"
                        textSize = 14f
                        setPadding(8, 4, 8, 4)
                        setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                FirebaseRepository.deleteEmergencyDocument(doc.id)
                                loadDecryptedCredentials()
                            }
                        }
                    }

                    titleRow.addView(tvTitle)
                    titleRow.addView(btnDelete)

                    val tvNumber = TextView(context).apply {
                        text = "ID: " + doc.idNumber
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
                        textSize = 13f
                        typeface = android.graphics.Typeface.MONOSPACE
                        setPadding(0, 6, 0, 2)
                    }

                    val tvHolder = TextView(context).apply {
                        text = if (doc.holderName.isNotEmpty()) "Holder: " + doc.holderName else "Protected Credential"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                        textSize = 12f
                    }

                    card.addView(titleRow)
                    card.addView(tvNumber)
                    card.addView(tvHolder)
                    layoutCredentialsList.addView(card)
                }
            }
        }
    }

    private fun showAddEmergencyIdDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_emergency_id, null, false)
        val etTitle = view.findViewById<EditText>(R.id.etEmergencyIdTitle)
        val etNumber = view.findViewById<EditText>(R.id.etEmergencyIdNumber)
        val etHolder = view.findViewById<EditText>(R.id.etEmergencyIdHolder)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Encrypted Credential")
            .setView(view)
            .setPositiveButton("Save Securely") { _, _ ->
                val title = etTitle.text.toString().trim()
                val number = etNumber.text.toString().trim()
                val holder = etHolder.text.toString().trim()

                if (title.isNotEmpty() && number.isNotEmpty()) {
                    val doc = EmergencyDocument(
                        id = "eid_" + System.currentTimeMillis(),
                        title = title,
                        idNumber = number,
                        holderName = holder,
                        isEncrypted = true
                    )
                    SecurityWalletManager.saveEncryptedDocument(requireContext(), doc)
                    viewLifecycleOwner.lifecycleScope.launch {
                        FirebaseRepository.saveEmergencyDocument(doc)
                        Toast.makeText(context, "ID securely saved and encrypted!", Toast.LENGTH_SHORT).show()
                        loadDecryptedCredentials()
                    }
                } else {
                    Toast.makeText(context, "Title and ID Number are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadWalletData() {
        updateEmergencyWalletUI()
        val budget = FirebaseRepository.getBudget()
        val expenses = FirebaseRepository.getExpenses()

        val remaining = (budget.monthlyBudget - budget.spentThisMonth).coerceAtLeast(0.0)
        tvBudgetRemaining.text = "₹${remaining.toInt()} Remaining"
        tvBudgetDetail.text = "Spent ₹${budget.spentThisMonth.toInt()} of ₹${budget.monthlyBudget.toInt()} monthly allowance"

        pbBudget.max = budget.monthlyBudget.toInt()
        pbBudget.progress = budget.spentThisMonth.toInt().coerceAtMost(budget.monthlyBudget.toInt())

        layoutExpensesList.removeAllViews()
        if (expenses.isEmpty()) {
            val emptyTv = TextView(context).apply {
                text = "No expenses logged yet. Tap '+ Log New Expense' to start tracking."
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                setPadding(16, 24, 16, 24)
                textSize = 13f
            }
            layoutExpensesList.addView(emptyTv)
        } else {
            expenses.forEach { exp ->
                val itemView = layoutInflater.inflate(R.layout.item_lifestyle_expense, layoutExpensesList, false)
                val tvIcon = itemView.findViewById<TextView>(R.id.tvExpenseIcon)
                val tvTitle = itemView.findViewById<TextView>(R.id.tvExpenseTitle)
                val tvSub = itemView.findViewById<TextView>(R.id.tvExpenseSubtitle)
                val tvAmount = itemView.findViewById<TextView>(R.id.tvExpenseAmount)
                val btnDelete = itemView.findViewById<ImageView>(R.id.btnDeleteExpense)

                tvIcon.text = when (exp.category) {
                    "Food" -> "🍔"
                    "Travel" -> "🚇"
                    "Books" -> "📚"
                    "Shopping" -> "🛍️"
                    else -> "💡"
                }
                tvTitle.text = exp.title
                tvSub.text = "${exp.category} • ${exp.date}"
                tvAmount.text = "- ₹${exp.amount.toInt()}"

                btnDelete.setOnClickListener {
                    FirebaseRepository.deleteExpense(exp.id)
                    Toast.makeText(context, "Expense removed", Toast.LENGTH_SHORT).show()
                    loadWalletData()
                }

                layoutExpensesList.addView(itemView)
            }
        }
    }

    private fun showAddExpenseDialog() {
        val dialogView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etTitle = EditText(requireContext()).apply {
            hint = "Expense description (e.g. Canteen Lunch)"
        }
        val etAmount = EditText(requireContext()).apply {
            hint = "Amount in ₹ (e.g. 150)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val spCategory = Spinner(requireContext())
        val categories = arrayOf("Food", "Travel", "Books", "Shopping", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spCategory.adapter = adapter

        dialogView.addView(etTitle)
        dialogView.addView(etAmount)
        dialogView.addView(spCategory)

        AlertDialog.Builder(requireContext())
            .setTitle("Log Student Expense")
            .setView(dialogView)
            .setPositiveButton("Add (+20 XP)") { _, _ ->
                val title = etTitle.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                val cat = spCategory.selectedItem.toString()

                if (title.isNotEmpty() && amountStr.isNotEmpty()) {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    FirebaseRepository.addExpense(title, amount, cat)
                    Toast.makeText(context, "🎉 Logged! +20 XP awarded to your streak.", Toast.LENGTH_SHORT).show()
                    loadWalletData()
                } else {
                    Toast.makeText(context, "Please enter title and amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // 2. WELLNESS & HABITS
    // ==========================================
    private fun setupWellnessActions() {
        moodExcited.setOnClickListener { setMood("Excited", "🤩") }
        moodMotivated.setOnClickListener { setMood("Motivated", "🚀") }
        moodCalm.setOnClickListener { setMood("Calm", "😌") }
        moodTired.setOnClickListener { setMood("Tired", "😴") }
        moodStressed.setOnClickListener { setMood("Stressed", "😰") }

        btnDrinkGlass.setOnClickListener {
            val newGlasses = FirebaseRepository.incrementWater()
            tvWellnessWaterCount.text = "$newGlasses / 8"
            pbWellnessWater.progress = newGlasses
            Toast.makeText(context, "💧 Stay hydrated! +15 XP added ($newGlasses/8 glasses)", Toast.LENGTH_SHORT).show()
        }

        btnSaveReflection.setOnClickListener {
            val note = etJournalReflection.text.toString().trim()
            val current = FirebaseRepository.getTodayWellness()
            FirebaseRepository.saveWellness(current.mood, current.waterGlasses, current.sleepHours, note)
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val log = DailyLog(
                userId = FirebaseRepository.getCurrentUserId(),
                date = todayStr,
                whatStudied = "Academic Day Reflection",
                whatCompleted = note,
                whatLearned = note,
                tomorrowPlan = "Continue study plan",
                focusMinutes = (current.sleepHours * 60).toInt(),
                moodScore = when (current.mood) {
                    "Excited" -> 5
                    "Motivated" -> 4
                    "Calm" -> 3
                    "Tired" -> 2
                    "Stressed" -> 1
                    else -> 3
                }
            )
            viewLifecycleOwner.lifecycleScope.launch {
                FirebaseRepository.saveDailyLog(log)
            }
            Toast.makeText(context, "✨ Daily reflection saved to Journal! +30 XP awarded.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setMood(mood: String, emoji: String) {
        val current = FirebaseRepository.getTodayWellness()
        FirebaseRepository.saveWellness(mood, current.waterGlasses, current.sleepHours, current.journalNote)
        tvCurrentMoodStatus.text = "Current state: $emoji $mood"
        Toast.makeText(context, "Mood recorded as $mood $emoji! +10 XP", Toast.LENGTH_SHORT).show()
    }

    private fun loadWellnessData() {
        val wellness = FirebaseRepository.getTodayWellness()
        val emoji = when (wellness.mood) {
            "Excited" -> "🤩"
            "Calm" -> "😌"
            "Tired" -> "😴"
            "Stressed" -> "😰"
            else -> "🚀"
        }
        tvCurrentMoodStatus.text = "Current state: $emoji ${wellness.mood}"

        tvWellnessWaterCount.text = "${wellness.waterGlasses} / 8"
        pbWellnessWater.max = 8
        pbWellnessWater.progress = wellness.waterGlasses

        tvSleepDisplay.text = "Sleep: ${wellness.sleepHours} hours logged"
        if (etJournalReflection.text.isEmpty() && wellness.journalNote.isNotEmpty()) {
            etJournalReflection.setText(wellness.journalNote)
        }
    }

    // ==========================================
    // 3. GAMIFICATION & BADGES
    // ==========================================
    private fun loadRewardsData() {
        val g = FirebaseRepository.getGamification()
        tvGamificationTitle.text = "Level ${g.level} • ${g.levelTitle} 🚀"

        val nextRankXP = g.level * 400
        tvGamificationPoints.text = "${g.xpPoints} XP earned • Next rank at $nextRankXP XP"

        pbXpProgress.max = nextRankXP
        pbXpProgress.progress = g.xpPoints.coerceAtMost(nextRankXP)

        val badges = FirebaseRepository.getAvailableBadges()
        layoutBadgesList.removeAllViews()

        badges.forEach { badge ->
            val itemView = layoutInflater.inflate(R.layout.item_lifestyle_badge, layoutBadgesList, false)
            val ivGraphic = itemView.findViewById<android.widget.ImageView>(R.id.ivBadgeGraphic)
            val tvIcon = itemView.findViewById<TextView>(R.id.tvBadgeIcon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvBadgeTitle)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvBadgeDesc)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvBadgeStatus)
            val tvXP = itemView.findViewById<TextView>(R.id.tvBadgeXP)

            // Select MyBuddy brand achievement graphics
            val graphicRes = when {
                badge.id.contains("academic", true) || badge.id.contains("attendance", true) || badge.title.contains("Exam", true) -> R.drawable.mybuddy_grad_cap
                badge.id.contains("streak", true) || badge.id.contains("focus", true) || badge.title.contains("Master", true) -> R.drawable.mybuddy_sparkle
                else -> R.drawable.mybuddy_badge_sticker
            }
            ivGraphic?.setImageResource(graphicRes)

            tvIcon.text = badge.iconEmoji
            tvTitle.text = badge.title
            tvDesc.text = badge.description
            tvXP.text = "+${badge.xpReward} XP"

            if (badge.isUnlocked) {
                tvStatus.text = "UNLOCKED 🏆"
                tvStatus.setBackgroundResource(R.drawable.bg_pill_lime)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            } else {
                tvStatus.text = "LOCKED 🔒"
                tvStatus.setBackgroundResource(R.drawable.bg_pill_neutral)
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            layoutBadgesList.addView(itemView)
        }
    }
}

