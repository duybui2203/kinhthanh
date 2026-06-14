package com.example.triava.ui.intro

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.triava.MainActivity
import com.example.triava.R

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnAction: Button
    private lateinit var adapter: IntroAdapter
    private val slides = listOf(
        IntroAdapter.Slide(R.drawable.ic_slide_bible, R.string.intro_title_1, R.string.intro_desc_1),
        IntroAdapter.Slide(R.drawable.ic_slide_quiz, R.string.intro_title_2, R.string.intro_desc_2),
        IntroAdapter.Slide(R.drawable.ic_slide_rank, R.string.intro_title_3, R.string.intro_desc_3)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.view_pager)
        dotsContainer = findViewById(R.id.dots_container)
        btnAction = findViewById(R.id.btn_action)

        adapter = IntroAdapter(slides)
        viewPager.adapter = adapter

        setupDots()
        setActiveDot(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setActiveDot(position)
                if (position == slides.size - 1) {
                    btnAction.setText(R.string.intro_btn_start)
                } else {
                    btnAction.setText(R.string.intro_btn_next)
                }
            }
        })

        btnAction.setOnClickListener {
            val current = viewPager.currentItem
            if (current < slides.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun setupDots() {
        val dotParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 0, 8, 0)
        }

        for (i in slides.indices) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.ic_dot_inactive)
                layoutParams = dotParams
            }
            dotsContainer.addView(dot)
        }
    }

    private fun setActiveDot(position: Int) {
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as ImageView
            if (i == position) {
                dot.setImageResource(R.drawable.ic_dot_active)
            } else {
                dot.setImageResource(R.drawable.ic_dot_inactive)
            }
        }
    }

    private fun finishOnboarding() {
        val sharedPreferences = getSharedPreferences("BiblePlayPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("is_first_time", false).apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
