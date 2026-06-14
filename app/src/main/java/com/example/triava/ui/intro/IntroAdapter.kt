package com.example.triava.ui.intro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.triava.R

class IntroAdapter(private val slides: List<Slide>) : RecyclerView.Adapter<IntroAdapter.IntroViewHolder>() {

    data class Slide(val imageRes: Int, val titleRes: Int, val descRes: Int)

    class IntroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val slideImage: ImageView = view.findViewById(R.id.slide_image)
        val slideTitle: TextView = view.findViewById(R.id.slide_title)
        val slideDesc: TextView = view.findViewById(R.id.slide_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_intro_slide, parent, false)
        return IntroViewHolder(view)
    }

    override fun onBindViewHolder(holder: IntroViewHolder, position: Int) {
        val slide = slides[position]
        holder.slideImage.setImageResource(slide.imageRes)
        holder.slideTitle.setText(slide.titleRes)
        holder.slideDesc.setText(slide.descRes)
    }

    override fun getItemCount(): Int = slides.size
}
