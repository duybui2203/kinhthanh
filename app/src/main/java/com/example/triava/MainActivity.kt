package com.example.triava

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.triava.ui.bible.BibleFragment
import com.example.triava.ui.profile.ProfileFragment
import com.example.triava.ui.quiz.QuizFragment
import com.example.triava.ui.rank.RankFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // Load Default Fragment
        if (savedInstanceState == null) {
            loadFragment(BibleFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_bible -> BibleFragment()
                R.id.nav_quiz -> QuizFragment()
                R.id.nav_rank -> RankFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> BibleFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}