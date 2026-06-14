package com.example.triava.ui.bible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triava.R
import com.example.triava.data.model.Verse

class VerseAdapter(
    private var verses: List<Verse>,
    private val onBookmarkClick: (Verse) -> Unit
) : RecyclerView.Adapter<VerseAdapter.VerseViewHolder>() {

    class VerseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVerseNumber: TextView = view.findViewById(R.id.tv_verse_number)
        val tvTextVi: TextView = view.findViewById(R.id.tv_text_vi)
        val tvTextEn: TextView = view.findViewById(R.id.tv_text_en)
        val btnBookmark: ImageButton = view.findViewById(R.id.btn_bookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
        return VerseViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerseViewHolder, position: Int) {
        val verse = verses[position]
        holder.tvVerseNumber.text = verse.verseNumber.toString()
        holder.tvTextVi.text = verse.textVi
        holder.tvTextEn.text = verse.textEn

        val bookmarkIcon = if (verse.isBookmarked) {
            R.drawable.ic_bookmark
        } else {
            R.drawable.ic_bookmark_border
        }
        holder.btnBookmark.setImageResource(bookmarkIcon)

        holder.btnBookmark.setOnClickListener {
            onBookmarkClick(verse)
            // Local state toggle and refresh item
            verse.isBookmarked = !verse.isBookmarked
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = verses.size

    fun updateData(newVerses: List<Verse>) {
        verses = newVerses
        notifyDataSetChanged()
    }
}
