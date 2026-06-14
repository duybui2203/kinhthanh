package com.example.triava.data.model

data class Book(
    val id: Int,
    val nameVi: String,
    val nameEn: String,
    val abbreviation: String,
    val testament: String, // "OT" or "NT"
    val chaptersCount: Int = 0
)
