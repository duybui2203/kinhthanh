package com.example.triava.data.model

data class Verse(
    val id: Int,
    val bookId: Int,
    val chapter: Int,
    val verseNumber: Int,
    val textVi: String,
    val textEn: String,
    val heading: String? = null,
    var isBookmarked: Boolean = false
)
