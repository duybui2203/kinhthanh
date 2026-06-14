package com.example.triava.ui.bible

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.triava.R
import com.example.triava.data.database.BibleDatabaseHelper
import com.example.triava.data.model.Book
import com.google.android.material.tabs.TabLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class BibleFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: BibleDatabaseHelper
    private lateinit var adapter: BookAdapter
    private var allBooks = listOf<Book>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bible, container, false)

        tabLayout = view.findViewById(R.id.tab_layout)
        recyclerView = view.findViewById(R.id.recycler_books)

        dbHelper = BibleDatabaseHelper(requireContext())
        
        // Setup TabLayout
        tabLayout.addTab(tabLayout.newTab().setText(R.string.testament_ot))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.testament_nt))

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookAdapter(emptyList()) { book ->
            showChapterSelectionDialog(book)
        }
        recyclerView.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val testament = if (tab.position == 0) "OT" else "NT"
                adapter.updateData(filterBooks(testament))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        loadBooks()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Update user coins when returning to the lobby
        view?.findViewById<TextView>(R.id.tv_user_coins)?.text = dbHelper.getUserCoins().toString()
    }

    private fun loadBooks() {
        allBooks = dbHelper.getBooks()
        if (allBooks.isEmpty()) {
            downloadBooksMetadata()
        } else {
            updateAdapter()
        }
    }

    private fun downloadBooksMetadata() {
        viewLifecycleOwner.lifecycleScope.launch {
            val metadataUrl = "https://huggingface.co/datasets/v-bible/catholic-resources/resolve/main/books/bible/versions/ktcgkpv.org/kt2011/metadata.json"
            val jsonStr = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val url = java.net.URL(metadataUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.inputStream.bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    null
                }
            }

            if (jsonStr != null) {
                val booksList = mutableListOf<Book>()
                try {
                    val booksArray = org.json.JSONArray(jsonStr)
                    for (i in 0 until booksArray.length()) {
                        val bObj = booksArray.getJSONObject(i)
                        val bookId = bObj.optInt("bookOrder", i + 1)
                        val name = bObj.optString("name")
                        val code = bObj.optString("code")
                        val testament = bObj.optString("testament").uppercase()
                        booksList.add(Book(bookId, name, "", code, testament))
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        dbHelper.insertBooks(booksList)
                    }
                    allBooks = dbHelper.getBooks()
                    updateAdapter()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateAdapter() {
        val testament = if (tabLayout.selectedTabPosition == 0) "OT" else "NT"
        adapter.updateData(filterBooks(testament))
    }

    private fun filterBooks(testament: String): List<Book> {
        return allBooks.filter { it.testament == testament }
    }

    private fun showChapterSelectionDialog(book: Book) {
        val chaptersCount = dbHelper.getChaptersCount(book.id)
        val chapters = (1..chaptersCount).toList()

        val context = requireContext()
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_chapter_selection, null)

        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvSubtitle = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_subtitle)
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.recycler_chapters)

        tvTitle.text = book.nameVi
        tvSubtitle.text = "${book.nameEn} • $chaptersCount chương"

        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 5)
        recyclerView.adapter = ChapterAdapter(chapters) { selectedChapter ->
            dialog.dismiss()
            val intent = Intent(context, ReaderActivity::class.java).apply {
                putExtra("BOOK_ID", book.id)
                putExtra("BOOK_NAME_VI", book.nameVi)
                putExtra("BOOK_NAME_EN", book.nameEn)
                putExtra("CHAPTER", selectedChapter)
            }
            startActivity(intent)
        }

        dialog.setContentView(bottomSheetView)
        dialog.show()
    }
}

class ChapterAdapter(
    private val chapters: List<Int>,
    private val onChapterClick: (Int) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChapterNumber: TextView = view.findViewById(R.id.tv_chapter_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.tvChapterNumber.text = chapter.toString()
        holder.tvChapterNumber.setOnClickListener { onChapterClick(chapter) }
    }

    override fun getItemCount(): Int = chapters.size
}
