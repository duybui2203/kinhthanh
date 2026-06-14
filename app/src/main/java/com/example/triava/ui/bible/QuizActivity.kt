package com.example.triava.ui.bible

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.triava.R
import com.example.triava.data.database.BibleDatabaseHelper
import com.example.triava.data.model.Question
import com.example.triava.data.repository.QuizRepository
import com.google.android.material.card.MaterialCardView
import java.util.Calendar

class QuizActivity : AppCompatActivity() {

    private lateinit var tvQuizTitle: TextView
    private lateinit var tvQuizProgress: TextView
    private lateinit var pbQuiz: ProgressBar
    private lateinit var tvQuestionText: TextView
    private lateinit var btnSubmitNext: Button

    private val cardOptions = mutableListOf<MaterialCardView>()
    private val tvOptions = mutableListOf<TextView>()

    private lateinit var dbHelper: BibleDatabaseHelper
    
    // Quiz Config
    private var quizMode: String = "BOOK" // BOOK, DAILY, QUICK, CATEGORY
    private var bookId: Int = 1
    private var bookNameVi: String = ""
    private var categoryName: String = "OT"
    private var alreadyDoneToday: Boolean = false
    
    private var questions: List<Question> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var isAnswered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        dbHelper = BibleDatabaseHelper(this)
        
        // Extract Intent Parameters
        quizMode = intent.getStringExtra("QUIZ_MODE") ?: "BOOK"
        alreadyDoneToday = intent.getBooleanExtra("ALREADY_DONE_TODAY", false)
        
        setupQuizMode()

        if (questions.isEmpty()) {
            Toast.makeText(this, "Không có câu hỏi nào cho phần chơi này!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Bind Views
        tvQuizTitle = findViewById(R.id.tv_quiz_title)
        tvQuizProgress = findViewById(R.id.tv_quiz_progress)
        pbQuiz = findViewById(R.id.pb_quiz)
        tvQuestionText = findViewById(R.id.tv_question_text)
        btnSubmitNext = findViewById(R.id.btn_submit_next)

        cardOptions.add(findViewById(R.id.card_option_0))
        cardOptions.add(findViewById(R.id.card_option_1))
        cardOptions.add(findViewById(R.id.card_option_2))
        cardOptions.add(findViewById(R.id.card_option_3))

        tvOptions.add(findViewById(R.id.tv_option_0))
        tvOptions.add(findViewById(R.id.tv_option_1))
        tvOptions.add(findViewById(R.id.tv_option_2))
        tvOptions.add(findViewById(R.id.tv_option_3))

        // Update title and progress bar
        updateTitleText()
        pbQuiz.max = questions.size

        for (i in cardOptions.indices) {
            cardOptions[i].setOnClickListener {
                onOptionSelected(i)
            }
        }

        btnSubmitNext.setOnClickListener {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                loadQuestion()
            } else {
                showResultDialog()
            }
        }

        loadQuestion()
    }

    private fun setupQuizMode() {
        when (quizMode) {
            "DAILY" -> {
                questions = QuizRepository.getDailyQuestions(this)
            }
            "QUICK" -> {
                questions = QuizRepository.getRandomQuestions(this, 10)
            }
            "CATEGORY" -> {
                categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "OT"
                questions = QuizRepository.getQuestionsByCategory(this, categoryName, 10)
            }
            else -> { // BOOK
                bookId = intent.getIntExtra("BOOK_ID", 1)
                bookNameVi = intent.getStringExtra("BOOK_NAME_VI") ?: ""
                questions = QuizRepository.getQuestionsForBook(this, bookId)
            }
        }
    }

    private fun updateTitleText() {
        when (quizMode) {
            "DAILY" -> tvQuizTitle.text = "Thử Thách Hàng Ngày"
            "QUICK" -> tvQuizTitle.text = "Chơi Nhanh"
            "CATEGORY" -> tvQuizTitle.text = if (categoryName == "OT") "Luyện Tập: Cựu Ước" else "Luyện Tập: Tân Ước"
            else -> tvQuizTitle.text = "Bài đọc: $bookNameVi"
        }
    }

    private fun loadQuestion() {
        isAnswered = false
        btnSubmitNext.visibility = View.GONE
        
        val question = questions[currentQuestionIndex]
        tvQuizProgress.text = "${currentQuestionIndex + 1} / ${questions.size}"
        pbQuiz.progress = currentQuestionIndex + 1
        tvQuestionText.text = question.text

        for (i in cardOptions.indices) {
            tvOptions[i].text = question.options[i]
            cardOptions[i].isEnabled = true
            cardOptions[i].setCardBackgroundColor(Color.WHITE)
            cardOptions[i].strokeColor = Color.parseColor("#E1E6E2")
            tvOptions[i].setTextColor(Color.parseColor("#162E24")) // text_dark
        }
    }

    private fun onOptionSelected(selectedIndex: Int) {
        if (isAnswered) return
        isAnswered = true

        val question = questions[currentQuestionIndex]
        val correctIndex = question.correctAnswerIndex

        if (selectedIndex == correctIndex) {
            score++
            cardOptions[selectedIndex].setCardBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green bg
            cardOptions[selectedIndex].strokeColor = Color.parseColor("#4CAF50")
            tvOptions[selectedIndex].setTextColor(Color.parseColor("#1B5E20")) // Dark Green text
        } else {
            cardOptions[selectedIndex].setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Light Red bg
            cardOptions[selectedIndex].strokeColor = Color.parseColor("#F44336")
            tvOptions[selectedIndex].setTextColor(Color.parseColor("#B71C1C")) // Dark Red text
            
            // Highlight correct
            cardOptions[correctIndex].setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            cardOptions[correctIndex].strokeColor = Color.parseColor("#4CAF50")
            tvOptions[correctIndex].setTextColor(Color.parseColor("#1B5E20"))
        }

        for (card in cardOptions) {
            card.isEnabled = false
        }

        if (currentQuestionIndex < questions.size - 1) {
            btnSubmitNext.text = "Câu tiếp theo"
        } else {
            btnSubmitNext.text = "Xem kết quả"
        }
        btnSubmitNext.visibility = View.VISIBLE
    }

    private fun showResultDialog() {
        val totalQuestions = questions.size
        
        when (quizMode) {
            "DAILY" -> {
                val passed = score >= 4 // 80% to pass
                if (passed) {
                    if (alreadyDoneToday) {
                        showCustomVictoryDialog(
                            title = "Đạt yêu cầu! 🌟",
                            message = "Bạn đã trả lời đúng $score/$totalQuestions câu.\n(Bạn đã nhận phần thưởng hôm nay rồi, kết quả này được lưu làm thành tích luyện tập!)",
                            coins = 0,
                            xp = 0,
                            levelUp = false
                        )
                    } else {
                        // Reward: 20 coins, 100 XP, and increase/preserve streak
                        handleDailyStreak()
                        dbHelper.addCoins(20)
                        val levelUp = dbHelper.addXp(100)
                        
                        showCustomVictoryDialog(
                            title = "Vượt qua thử thách! 🎉",
                            message = "Bạn đã trả lời đúng $score/$totalQuestions câu.\nChúc mừng bạn giữ vững ngọn lửa học hỏi!",
                            coins = 20,
                            xp = 100,
                            levelUp = levelUp
                        )
                    }
                } else {
                    showFailureDialog("Bạn trả lời đúng $score/$totalQuestions câu. Cần đúng từ 4 câu trở lên để vượt qua Thử Thách Hàng Ngày. Hãy ôn lại bài và thử lại nhé!")
                }
            }
            "QUICK", "CATEGORY" -> {
                // Earn 2 coins and 10 XP per correct answer
                val coinsEarned = score * 2
                val xpEarned = score * 10
                
                dbHelper.addCoins(coinsEarned)
                val levelUp = dbHelper.addXp(xpEarned)
                
                showCustomVictoryDialog(
                    title = "Hoàn thành bài luyện tập! 📖",
                    message = "Bạn đã trả lời đúng $score/$totalQuestions câu.",
                    coins = coinsEarned,
                    xp = xpEarned,
                    levelUp = levelUp
                )
            }
            else -> { // BOOK
                val passed = score >= 7 // 70% to pass
                if (passed) {
                    val totalChapters = dbHelper.getChaptersCount(bookId)
                    val rewardAmount = totalChapters * 10
                    val claimed = dbHelper.markChapterCompletedAndClaimReward(bookId, 9999, rewardAmount)
                    
                    if (claimed) {
                        val levelUp = dbHelper.addXp(100)
                        showCustomVictoryDialog(
                            title = "Hoàn thành sách! 🎉",
                            message = "Bạn đã trả lời đúng $score/$totalQuestions câu.\nChúc mừng bạn đã chinh phục trọn vẹn sách $bookNameVi!",
                            coins = rewardAmount,
                            xp = 100,
                            levelUp = levelUp
                        )
                    } else {
                        showCustomVictoryDialog(
                            title = "Hoàn thành sách! 📖",
                            message = "Bạn đã trả lời đúng $score/$totalQuestions câu.\n(Sách này đã được hoàn thành trước đó rồi.)",
                            coins = 0,
                            xp = 0,
                            levelUp = false
                        )
                    }
                } else {
                    showFailureDialog("Bạn trả lời đúng $score/$totalQuestions câu. Cần đúng từ 7 câu trở lên để hoàn thành sách. Hãy đọc lại kỹ và thử sức lại nhé!")
                }
            }
        }
    }

    private fun handleDailyStreak() {
        val lastActive = dbHelper.getLastActive()
        val streak = dbHelper.getUserStreak()
        val today = System.currentTimeMillis()
        
        val calLast = Calendar.getInstance().apply { timeInMillis = lastActive }
        val calToday = Calendar.getInstance().apply { timeInMillis = today }
        
        val sameYear = calLast.get(Calendar.YEAR) == calToday.get(Calendar.YEAR)
        val lastDayOfYear = calLast.get(Calendar.DAY_OF_YEAR)
        val todayDayOfYear = calToday.get(Calendar.DAY_OF_YEAR)
        
        var newStreak = streak
        if (lastActive == 0L) {
            newStreak = 1
        } else {
            if (sameYear) {
                val diff = todayDayOfYear - lastDayOfYear
                if (diff == 1) {
                    newStreak = streak + 1
                } else if (diff > 1) {
                    newStreak = 1
                }
                // if diff == 0, keep same streak
            } else {
                val diffYears = calToday.get(Calendar.YEAR) - calLast.get(Calendar.YEAR)
                if (diffYears == 1 && lastDayOfYear >= 365 && todayDayOfYear == 1) {
                    newStreak = streak + 1
                } else {
                    newStreak = 1
                }
            }
        }
        
        dbHelper.updateStreak(newStreak)
        dbHelper.updateLastActive(today)
    }

    private fun showCustomVictoryDialog(
        title: String,
        message: String,
        coins: Int,
        xp: Int,
        levelUp: Boolean
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_victory, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_victory_title)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_victory_message)
        val tvCoins = dialogView.findViewById<TextView>(R.id.tv_coins_earned)
        val tvXp = dialogView.findViewById<TextView>(R.id.tv_xp_earned)
        
        val layoutCoins = dialogView.findViewById<View>(R.id.layout_coins_reward)
        val layoutXp = dialogView.findViewById<View>(R.id.layout_xp_reward)
        
        tvTitle.text = title
        
        var finalMessage = message
        if (levelUp) {
            finalMessage += "\n\n🌟 THĂNG CẤP! Bạn đạt Cấp độ ${dbHelper.getUserLevel()}! 🌟"
        }
        tvMessage.text = finalMessage
        
        if (coins > 0) {
            tvCoins.text = "+$coins Vàng"
            layoutCoins.visibility = View.VISIBLE
        } else {
            layoutCoins.visibility = View.GONE
        }
        
        if (xp > 0) {
            tvXp.text = "+$xp XP"
            layoutXp.visibility = View.VISIBLE
        } else {
            layoutXp.visibility = View.GONE
        }
        
        // Adjust layout margins if only one is shown
        if (coins > 0 && xp <= 0) {
            val params = layoutCoins.layoutParams as android.widget.LinearLayout.LayoutParams
            params.marginEnd = 0
            layoutCoins.layoutParams = params
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<View>(R.id.btn_continue).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showFailureDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Chưa đạt yêu cầu 😢")
            .setMessage(message)
            .setPositiveButton("Đồng ý") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
