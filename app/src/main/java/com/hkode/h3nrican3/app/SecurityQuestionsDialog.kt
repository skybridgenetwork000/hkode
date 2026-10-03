package com.hkode.h3nrican3.app

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

object SecurityQuestionsDialog {

    fun showSetup(
        context: Context,
        mandatory: Boolean = true,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        val dialog = Dialog(context, R.style.AppTheme_FullScreenDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_security_questions_setup, null)
        dialog.setContentView(view)

        setupDialogWindow(dialog, context)

        val scrollView = view.findViewById<NestedScrollView>(R.id.scroll_questions_setup)
        val header = view.findViewById<LinearLayout>(R.id.header_questions_setup)
        val origTopPadding = header.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            header.updatePadding(top = origTopPadding + statusBarInsets.top)
            val bottomInset = if (isImeVisible) imeInsets.bottom else navBarInsets.bottom
            view.updatePadding(bottom = bottomInset)

            if (isImeVisible) {
                scrollView.postDelayed({
                    val focused = view.findFocus()
                    if (focused != null) {
                        val rect = android.graphics.Rect()
                        focused.getDrawingRect(rect)
                        scrollView.offsetDescendantRectToMyCoords(focused, rect)
                        scrollView.smoothScrollTo(0, rect.bottom + 80)
                    }
                }, 100)
            }
            insets
        }

        val btnClose = view.findViewById<FrameLayout>(R.id.btn_questions_setup_close)
        val btnSelectQ1 = view.findViewById<LinearLayout>(R.id.btn_select_q1)
        val tvSelectedQ1 = view.findViewById<TextView>(R.id.tv_selected_q1)
        val etAnswer1 = view.findViewById<EditText>(R.id.et_answer_1)

        val btnSelectQ2 = view.findViewById<LinearLayout>(R.id.btn_select_q2)
        val tvSelectedQ2 = view.findViewById<TextView>(R.id.tv_selected_q2)
        val etAnswer2 = view.findViewById<EditText>(R.id.et_answer_2)

        val tvError = view.findViewById<TextView>(R.id.tv_questions_error)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_questions)

        var selectedQ1: String? = null
        var selectedQ2: String? = null

        // Auto-scroll when input fields gain focus so keyboard never covers them
        etAnswer1.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    val rect = android.graphics.Rect()
                    etAnswer1.getDrawingRect(rect)
                    scrollView.offsetDescendantRectToMyCoords(etAnswer1, rect)
                    scrollView.smoothScrollTo(0, rect.top - 40)
                }, 150)
            }
        }

        etAnswer2.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    val rect = android.graphics.Rect()
                    etAnswer2.getDrawingRect(rect)
                    scrollView.offsetDescendantRectToMyCoords(etAnswer2, rect)
                    scrollView.smoothScrollTo(0, rect.bottom + 120)
                }, 150)
            }
        }

        // Pre-fill existing questions if available
        AppSecurityManager.getSecurityQuestions(context)?.let { (existingQ1, existingQ2) ->
            selectedQ1 = existingQ1
            tvSelectedQ1.text = existingQ1
            selectedQ2 = existingQ2
            tvSelectedQ2.text = existingQ2
        }

        btnSelectQ1.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            showQuestionSelectionBottomSheet(
                context = context,
                title = "Select Security Question 1",
                currentSelected = selectedQ1,
                otherSelected = selectedQ2
            ) { chosen ->
                if (chosen == selectedQ2) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Please choose two different questions."
                } else {
                    selectedQ1 = chosen
                    tvSelectedQ1.text = chosen
                    tvError.visibility = View.GONE
                }
            }
        }

        btnSelectQ2.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            showQuestionSelectionBottomSheet(
                context = context,
                title = "Select Security Question 2",
                currentSelected = selectedQ2,
                otherSelected = selectedQ1
            ) { chosen ->
                if (chosen == selectedQ1) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Please choose two different questions."
                } else {
                    selectedQ2 = chosen
                    tvSelectedQ2.text = chosen
                    tvError.visibility = View.GONE
                }
            }
        }

        btnSave.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            val q1 = selectedQ1
            val q2 = selectedQ2
            val a1 = etAnswer1.text.toString().trim()
            val a2 = etAnswer2.text.toString().trim()

            if (q1.isNullOrEmpty() || q2.isNullOrEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Please select both security questions."
                return@setOnClickListener
            }
            if (q1 == q2) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Question 1 and Question 2 must be different."
                return@setOnClickListener
            }
            if (a1.isEmpty() || a2.isEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Please provide answers for both security questions."
                return@setOnClickListener
            }

            val saved = AppSecurityManager.saveSecurityQuestions(context, q1, a1, q2, a2)
            if (saved) {
                if (context is Activity) {
                    CustomToast.showSuccess(context, "Questions Configured", "Security recovery questions saved successfully")
                }
                dialog.dismiss()
                onSuccess()
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = "Failed to save security questions. Please try again."
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        dialog.setOnCancelListener {
            onCancel()
        }

        dialog.show()
    }

    fun showVerify(
        context: Context,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        val existingQuestions = AppSecurityManager.getSecurityQuestions(context)
        if (existingQuestions == null) {
            if (context is Activity) {
                CustomToast.showError(context, "No Questions Set", "No recovery security questions have been set up yet.")
            }
            onCancel()
            return
        }

        val dialog = Dialog(context, R.style.AppTheme_FullScreenDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_forgot_passcode, null)
        dialog.setContentView(view)

        setupDialogWindow(dialog, context)

        val scrollView = view.findViewById<NestedScrollView>(R.id.scroll_forgot_passcode)
        val header = view.findViewById<LinearLayout>(R.id.header_forgot_passcode)
        val origTopPadding = header.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            header.updatePadding(top = origTopPadding + statusBarInsets.top)
            val bottomInset = if (isImeVisible) imeInsets.bottom else navBarInsets.bottom
            view.updatePadding(bottom = bottomInset)

            if (isImeVisible) {
                scrollView.postDelayed({
                    val focused = view.findFocus()
                    if (focused != null) {
                        val rect = android.graphics.Rect()
                        focused.getDrawingRect(rect)
                        scrollView.offsetDescendantRectToMyCoords(focused, rect)
                        scrollView.smoothScrollTo(0, rect.bottom + 80)
                    }
                }, 100)
            }
            insets
        }

        val btnClose = view.findViewById<FrameLayout>(R.id.btn_forgot_close)
        val tvQ1 = view.findViewById<TextView>(R.id.tv_verify_q1)
        val etA1 = view.findViewById<EditText>(R.id.et_verify_a1)
        val tvQ2 = view.findViewById<TextView>(R.id.tv_verify_q2)
        val etA2 = view.findViewById<EditText>(R.id.et_verify_a2)
        val tvError = view.findViewById<TextView>(R.id.tv_verify_error)
        val btnVerify = view.findViewById<MaterialButton>(R.id.btn_verify_answers)

        tvQ1.text = existingQuestions.first
        tvQ2.text = existingQuestions.second

        etA1.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    val rect = android.graphics.Rect()
                    etA1.getDrawingRect(rect)
                    scrollView.offsetDescendantRectToMyCoords(etA1, rect)
                    scrollView.smoothScrollTo(0, rect.top - 40)
                }, 150)
            }
        }

        etA2.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    val rect = android.graphics.Rect()
                    etA2.getDrawingRect(rect)
                    scrollView.offsetDescendantRectToMyCoords(etA2, rect)
                    scrollView.smoothScrollTo(0, rect.bottom + 120)
                }, 150)
            }
        }

        btnVerify.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            val a1 = etA1.text.toString().trim()
            val a2 = etA2.text.toString().trim()

            if (a1.isEmpty() || a2.isEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Please answer both security questions."
                return@setOnClickListener
            }

            val isCorrect = AppSecurityManager.verifySecurityAnswers(context, a1, a2)
            if (isCorrect) {
                if (context is Activity) {
                    CustomToast.showSuccess(context, "Verified", "Identity verified successfully!")
                }
                dialog.dismiss()
                onSuccess()
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = "Incorrect answers. Please try again."
                val shake = AnimationUtils.loadAnimation(context, R.anim.shake_pin)
                tvError.startAnimation(shake)
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        dialog.setOnCancelListener {
            onCancel()
        }

        dialog.show()
    }

    private fun showQuestionSelectionBottomSheet(
        context: Context,
        title: String,
        currentSelected: String?,
        otherSelected: String?,
        onSelected: (String) -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog)
        val sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_select_question, null)
        bottomSheet.setContentView(sheetView)

        bottomSheet.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val internalSheet = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            internalSheet?.let { sheet ->
                sheet.background = ContextCompat.getDrawable(context, R.drawable.bg_bottom_sheet_gradient_top)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val tvTitle = sheetView.findViewById<TextView>(R.id.tv_sheet_question_title)
        val btnClose = sheetView.findViewById<FrameLayout>(R.id.btn_sheet_close)
        val rv = sheetView.findViewById<RecyclerView>(R.id.rv_sheet_questions)

        tvTitle.text = title
        btnClose.setOnClickListener {
            bottomSheet.dismiss()
        }

        rv.layoutManager = LinearLayoutManager(context)
        val questions = AppSecurityManager.DEFAULT_SECURITY_QUESTIONS
        rv.adapter = SecurityQuestionAdapter(
            context = context,
            questions = questions,
            currentSelected = currentSelected,
            otherSelected = otherSelected,
            onQuestionChosen = { chosen ->
                onSelected(chosen)
                bottomSheet.dismiss()
            }
        )

        bottomSheet.show()
    }

    private class SecurityQuestionAdapter(
        private val context: Context,
        private val questions: List<String>,
        private val currentSelected: String?,
        private val otherSelected: String?,
        private val onQuestionChosen: (String) -> Unit
    ) : RecyclerView.Adapter<SecurityQuestionAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val rootLayout: LinearLayout = view.findViewById(R.id.layout_item_question_root)
            val tvText: TextView = view.findViewById(R.id.tv_question_text)
            val tvStatus: TextView = view.findViewById(R.id.tv_question_status)
            val layoutCheck: FrameLayout = view.findViewById(R.id.layout_question_check)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_security_question, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = questions.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val q = questions[position]
            holder.tvText.text = q

            val isCurrent = (q == currentSelected)
            val isOther = (q == otherSelected)

            if (isCurrent) {
                holder.rootLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_question_item_selected)
                holder.layoutCheck.visibility = View.VISIBLE
                holder.tvStatus.visibility = View.GONE
                holder.rootLayout.alpha = 1.0f
            } else if (isOther) {
                holder.rootLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_question_item)
                holder.layoutCheck.visibility = View.GONE
                holder.tvStatus.visibility = View.VISIBLE
                holder.tvStatus.text = "Selected for the other question"
                holder.rootLayout.alpha = 0.85f
            } else {
                holder.rootLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_question_item)
                holder.layoutCheck.visibility = View.GONE
                holder.tvStatus.visibility = View.GONE
                holder.rootLayout.alpha = 1.0f
            }

            holder.rootLayout.setOnClickListener {
                AppAnimationUtil.bouncePress(it)
                onQuestionChosen(q)
            }
        }
    }

    private fun setupDialogWindow(dialog: Dialog, context: Context) {
        dialog.window?.let { win ->
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            val bgColor = ContextCompat.getColor(context, R.color.app_background)
            win.statusBarColor = bgColor
            win.navigationBarColor = bgColor

            val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                win.insetsController?.let { controller ->
                    val flags = if (!isDarkMode) {
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    } else {
                        0
                    }
                    controller.setSystemBarsAppearance(flags, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                }
            } else {
                @Suppress("DEPRECATION")
                win.decorView.systemUiVisibility = if (!isDarkMode) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    0
                }
            }
        }
    }
}
