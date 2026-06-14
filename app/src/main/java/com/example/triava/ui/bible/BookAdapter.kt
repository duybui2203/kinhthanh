package com.example.triava.ui.bible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triava.R
import com.example.triava.data.model.Book

class BookAdapter(
    private var books: List<Book>,
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAbbrev: TextView = view.findViewById(R.id.tv_abbrev)
        val tvBookNameVi: TextView = view.findViewById(R.id.tv_book_name_vi)
        val tvBookNameEn: TextView = view.findViewById(R.id.tv_book_name_en)
        val tvBookReward: TextView = view.findViewById(R.id.tv_book_reward)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.tvAbbrev.text = book.abbreviation.uppercase()
        holder.tvBookNameVi.text = "${position + 1}. ${book.nameVi}"
        
        val rewardAmount = book.chaptersCount * 10
        holder.tvBookReward.text = "+$rewardAmount 🪙"
        if (book.nameEn.isNullOrEmpty()) {
            holder.tvBookNameEn.visibility = View.GONE
        } else {
            holder.tvBookNameEn.visibility = View.VISIBLE
            holder.tvBookNameEn.text = book.nameEn
        }
        
        // Ensure translationX is reset in case of recycling
        holder.itemView.translationX = 0f

        holder.itemView.setOnClickListener { onBookClick(book) }
    }

    override fun getItemCount(): Int = books.size

    fun updateData(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }
}
