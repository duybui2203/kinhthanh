package com.example.triava.ui.quiz

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.triava.R
import com.example.triava.data.database.BibleDatabaseHelper
import com.example.triava.ui.bible.QuizActivity
import com.google.android.material.card.MaterialCardView
import java.util.Calendar

class QuizFragment : Fragment() {

    private lateinit var dbHelper: BibleDatabaseHelper
    private lateinit var tvGreeting: TextView
    private lateinit var tvUserRank: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvLevel: TextView
    private lateinit var tvXp: TextView
    private lateinit var progressXp: ProgressBar
    
    private lateinit var btnPlayDaily: Button
    private lateinit var cardQuickPlay: MaterialCardView
    private lateinit var cardCategories: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quiz, container, false)
        
        dbHelper = BibleDatabaseHelper(requireContext())
        
        // Initialize views
        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvUserRank = view.findViewById(R.id.tv_user_rank)
        tvStreak = view.findViewById(R.id.tv_streak)
        tvLevel = view.findViewById(R.id.tv_level)
        tvXp = view.findViewById(R.id.tv_xp)
        progressXp = view.findViewById(R.id.progress_xp)
        
        btnPlayDaily = view.findViewById(R.id.btn_play_daily)
        cardQuickPlay = view.findViewById(R.id.card_quick_play)
        cardCategories = view.findViewById(R.id.card_categories)
        
        setupListeners()
        loadUserData()
        
        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val coins = dbHelper.getUserCoins()
        val xp = dbHelper.getUserXp()
        val level = dbHelper.getUserLevel()
        val streak = dbHelper.getUserStreak()
        
        tvGreeting.text = "Chào bạn! 🪙 $coins"
        tvLevel.text = "Cấp độ $level"
        tvXp.text = "$xp / 1000 XP"
        progressXp.progress = (xp * 100) / 1000
        tvStreak.text = "🔥 $streak Ngày"
        tvUserRank.text = getRankName(level)
        
        // Update daily button state if already completed today
        val lastActive = dbHelper.getLastActive()
        if (isSameDay(lastActive, System.currentTimeMillis()) && streak > 0) {
            btnPlayDaily.text = "ĐÃ HOÀN THÀNH HÔM NAY"
            btnPlayDaily.isEnabled = true // Still allow playing for fun, but notify them
        } else {
            btnPlayDaily.text = "BẮT ĐẦU QUIZ HÔM NAY"
            btnPlayDaily.isEnabled = true
        }
    }

    private fun getRankName(level: Int): String {
        return when {
            level <= 5 -> "Tập Sự Kinh Thánh"
            level <= 10 -> "Môn Đồ Kinh Thánh"
            level <= 15 -> "Giảng Viên Giáo Lý"
            level <= 20 -> "Tiến Sĩ Thần Học"
            else -> "Hiền Sĩ Kinh Thánh 🌟"
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        if (time1 == 0L || time2 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun setupListeners() {
        btnPlayDaily.setOnClickListener {
            val lastActive = dbHelper.getLastActive()
            val alreadyDone = isSameDay(lastActive, System.currentTimeMillis())
            
            val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                putExtra("QUIZ_MODE", "DAILY")
                putExtra("ALREADY_DONE_TODAY", alreadyDone)
            }
            startActivity(intent)
        }

        cardQuickPlay.setOnClickListener {
            val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                putExtra("QUIZ_MODE", "QUICK")
            }
            startActivity(intent)
        }

        cardCategories.setOnClickListener {
            showCategorySelectionDialog()
        }
    }

    private fun showCategorySelectionDialog() {
        val categories = arrayOf("Cựu Ước (Old Testament)", "Tân Ước (New Testament)")
        AlertDialog.Builder(requireContext())
            .setTitle("Chọn chủ đề Quiz")
            .setItems(categories) { _, which ->
                val categoryName = if (which == 0) "OT" else "NT"
                val intent = Intent(requireContext(), QuizActivity::class.java).apply {
                    putExtra("QUIZ_MODE", "CATEGORY")
                    putExtra("CATEGORY_NAME", categoryName)
                }
                startActivity(intent)
            }
            .show()
    }
}
