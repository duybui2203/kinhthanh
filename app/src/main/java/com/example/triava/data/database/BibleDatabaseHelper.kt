package com.example.triava.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.triava.data.model.Book
import com.example.triava.data.model.Verse

class BibleDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "BibleDatabaseHelper"
        private const val DATABASE_NAME = "bibleplay.db"
        private const val DATABASE_VERSION = 12 // Upgraded for Level/XP Schema

        // Table Names
        private const val TABLE_BOOKS = "books"
        private const val TABLE_VERSES = "verses"
        private const val TABLE_BOOKMARKS = "bookmarks"

        // Books Table Columns
        private const val KEY_BOOK_ID = "id"
        private const val KEY_BOOK_NAME_VI = "name_vi"
        private const val KEY_BOOK_NAME_EN = "name_en"
        private const val KEY_BOOK_ABBREV = "abbreviation"
        private const val KEY_BOOK_TESTAMENT = "testament" // "OT" or "NT"

        // Verses Table Columns
        private const val KEY_VERSE_ID = "id"
        private const val KEY_VERSE_BOOK_ID = "book_id"
        private const val KEY_VERSE_CHAPTER = "chapter"
        private const val KEY_VERSE_NUMBER = "verse_number"
        private const val KEY_VERSE_TEXT_VI = "text_vi"
        private const val KEY_VERSE_TEXT_EN = "text_en"
        private const val KEY_VERSE_HEADING = "heading"

        // Bookmarks Table Columns
        private const val KEY_BOOKMARK_ID = "id"
        private const val KEY_BOOKMARK_BOOK_ID = "book_id"
        private const val KEY_BOOKMARK_CHAPTER = "chapter"
        private const val KEY_BOOKMARK_VERSE = "verse_number"
        private const val KEY_BOOKMARK_DATE = "created_at"

        // Gamification Tables
        private const val TABLE_USERS = "users"
        private const val TABLE_CHAPTER_PROGRESS = "chapter_progress"

        // Users Columns
        private const val KEY_USER_ID = "id"
        private const val KEY_USER_COINS = "coins"
        private const val KEY_USER_STREAK = "streak_days"
        private const val KEY_USER_LAST_ACTIVE = "last_active"
        private const val KEY_USER_XP = "xp"
        private const val KEY_USER_LEVEL = "level"

        // Chapter Progress Columns
        private const val KEY_PROG_ID = "id"
        private const val KEY_PROG_BOOK_ID = "book_id"
        private const val KEY_PROG_CHAPTER = "chapter"
        private const val KEY_PROG_COMPLETED = "is_completed"
        private const val KEY_PROG_REWARD_CLAIMED = "reward_claimed"
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate: Creating tables...")
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAPTER_PROGRESS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VERSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKS")

        val createBooksTable = ("CREATE TABLE $TABLE_BOOKS (" +
                "$KEY_BOOK_ID INTEGER PRIMARY KEY," +
                "$KEY_BOOK_NAME_VI TEXT," +
                "$KEY_BOOK_NAME_EN TEXT," +
                "$KEY_BOOK_ABBREV TEXT," +
                "$KEY_BOOK_TESTAMENT TEXT)")
        db.execSQL(createBooksTable)

        val createVersesTable = ("CREATE TABLE $TABLE_VERSES (" +
                "$KEY_VERSE_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$KEY_VERSE_BOOK_ID INTEGER," +
                "$KEY_VERSE_CHAPTER INTEGER," +
                "$KEY_VERSE_NUMBER INTEGER," +
                "$KEY_VERSE_TEXT_VI TEXT," +
                "$KEY_VERSE_TEXT_EN TEXT," +
                "$KEY_VERSE_HEADING TEXT," +
                "FOREIGN KEY($KEY_VERSE_BOOK_ID) REFERENCES $TABLE_BOOKS($KEY_BOOK_ID))")
        db.execSQL(createVersesTable)

        val createBookmarksTable = ("CREATE TABLE $TABLE_BOOKMARKS (" +
                "$KEY_BOOKMARK_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$KEY_BOOKMARK_BOOK_ID INTEGER," +
                "$KEY_BOOKMARK_CHAPTER INTEGER," +
                "$KEY_BOOKMARK_VERSE INTEGER," +
                "$KEY_BOOKMARK_DATE INTEGER)")
        db.execSQL(createBookmarksTable)

        val createUsersTable = ("CREATE TABLE $TABLE_USERS (" +
                "$KEY_USER_ID INTEGER PRIMARY KEY DEFAULT 1," +
                "$KEY_USER_COINS INTEGER DEFAULT 0," +
                "$KEY_USER_STREAK INTEGER DEFAULT 0," +
                "$KEY_USER_LAST_ACTIVE INTEGER DEFAULT 0," +
                "$KEY_USER_XP INTEGER DEFAULT 0," +
                "$KEY_USER_LEVEL INTEGER DEFAULT 1)")
        db.execSQL(createUsersTable)

        // Khởi tạo User mặc định
        db.execSQL("INSERT INTO $TABLE_USERS ($KEY_USER_ID, $KEY_USER_COINS, $KEY_USER_STREAK, $KEY_USER_XP, $KEY_USER_LEVEL) VALUES (1, 0, 0, 0, 1)")

        val createChapterProgressTable = ("CREATE TABLE $TABLE_CHAPTER_PROGRESS (" +
                "$KEY_PROG_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$KEY_PROG_BOOK_ID INTEGER," +
                "$KEY_PROG_CHAPTER INTEGER," +
                "$KEY_PROG_COMPLETED INTEGER DEFAULT 0," +
                "$KEY_PROG_REWARD_CLAIMED INTEGER DEFAULT 0," +
                "UNIQUE($KEY_PROG_BOOK_ID, $KEY_PROG_CHAPTER))")
        db.execSQL(createChapterProgressTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade: Upgrading database from version $oldVersion to $newVersion...")
        createTables(db)
    }

    fun insertBooks(books: List<Book>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            for (book in books) {
                val values = ContentValues().apply {
                    put(KEY_BOOK_ID, book.id)
                    put(KEY_BOOK_NAME_VI, book.nameVi)
                    put(KEY_BOOK_NAME_EN, book.nameEn)
                    put(KEY_BOOK_ABBREV, book.abbreviation)
                    put(KEY_BOOK_TESTAMENT, book.testament)
                }
                db.insertWithOnConflict(TABLE_BOOKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertVerse(db: SQLiteDatabase, bookId: Int, chapter: Int, verseNumber: Int, textVi: String, textEn: String) {
        val values = ContentValues().apply {
            put(KEY_VERSE_BOOK_ID, bookId)
            put(KEY_VERSE_CHAPTER, chapter)
            put(KEY_VERSE_NUMBER, verseNumber)
            put(KEY_VERSE_TEXT_VI, textVi)
            put(KEY_VERSE_TEXT_EN, textEn)
        }
        db.insertWithOnConflict(TABLE_VERSES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Query Wrapper ---
    fun getBooks(testament: String? = null): List<Book> {
        return queryBooksFromDb(testament)
    }

    private fun queryBooksFromDb(testament: String?): List<Book> {
        val books = mutableListOf<Book>()
        val db = this.readableDatabase
        try {
            val query = if (testament != null) {
                "SELECT * FROM $TABLE_BOOKS WHERE $KEY_BOOK_TESTAMENT = ? ORDER BY $KEY_BOOK_ID ASC"
            } else {
                "SELECT * FROM $TABLE_BOOKS ORDER BY $KEY_BOOK_ID ASC"
            }
            val args = if (testament != null) arrayOf(testament) else null
            val cursor = db.rawQuery(query, args)

            if (cursor.moveToFirst()) {
                do {
                    val bookId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BOOK_ID))
                    val book = Book(
                        id = bookId,
                        nameVi = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_NAME_VI)),
                        nameEn = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_NAME_EN)),
                        abbreviation = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_ABBREV)),
                        testament = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_TESTAMENT)),
                        chaptersCount = getChaptersCount(bookId)
                    )
                    books.add(book)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "queryBooksFromDb error: ${e.message}")
        }
        return books
    }

    fun getBook(bookId: Int): Book? {
        val db = this.readableDatabase
        var book: Book? = null
        try {
            val cursor = db.rawQuery("SELECT * FROM $TABLE_BOOKS WHERE $KEY_BOOK_ID = ?", arrayOf(bookId.toString()))
            if (cursor.moveToFirst()) {
                book = Book(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_BOOK_ID)),
                    nameVi = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_NAME_VI)),
                    nameEn = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_NAME_EN)),
                    abbreviation = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_ABBREV)),
                    testament = cursor.getString(cursor.getColumnIndexOrThrow(KEY_BOOK_TESTAMENT))
                )
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "getBook error: ${e.message}")
        }
        return book
    }

    fun getBookEnglishNameForApi(bookId: Int): String {
        val book = getBook(bookId)
        return book?.nameEn ?: "Genesis"
    }

    // Return the actual liturgical chapter counts for Catholic Bible books
    fun getChaptersCount(bookId: Int): Int {
        return when (bookId) {
            1 -> 50  // Sáng Thế
            2 -> 40  // Xuất Hành
            3 -> 27  // Lê-vi
            4 -> 36  // Dân Số
            5 -> 34  // Đệ Nhị Luật
            6 -> 24  // Giô-suê
            7 -> 21  // Thủ Lãnh
            8 -> 4   // Rút
            9 -> 31  // 1 Sa-mu-en
            10 -> 24 // 2 Sa-mu-en
            11 -> 22 // 1 Các Vua
            12 -> 25 // 2 Các Vua
            13 -> 29 // 1 Sử Biên Niên
            14 -> 36 // 2 Sử Biên Niên
            15 -> 10 // Ét-ra
            16 -> 13 // Nơ-khe-mi-a
            17 -> 14 // Tô-bi-a
            18 -> 16 // Giu-đi-tha
            19 -> 10 // Ét-te
            20 -> 16 // 1 Ma-ca-bê
            21 -> 15 // 2 Ma-ca-bê
            22 -> 42 // Gióp
            23 -> 150 // Thánh Vịnh
            24 -> 31 // Châm Ngôn
            25 -> 12 // Giảng Viên
            26 -> 8  // Diễm Ca
            27 -> 19 // Khôn Ngoan
            28 -> 51 // Huấn Ca
            29 -> 66 // I-sai-a
            30 -> 52 // Giê-rê-mi-a
            31 -> 5  // Ca Thương
            32 -> 6  // Ba-rúc
            33 -> 48 // Ê-dê-ki-en
            34 -> 14 // Đa-ni-en
            35 -> 14 // Hô-sê
            36 -> 4  // Giô-ên
            37 -> 9  // A-mốt
            38 -> 1  // Ô-vát-đi-a
            39 -> 4  // Giô-na
            40 -> 7  // Mi-kha
            41 -> 3  // Na-khum
            42 -> 3  // Kha-ba-cúc
            43 -> 3  // Xô-phô-ni-a
            44 -> 2  // Khác-gai
            45 -> 14 // Xa-ca-ri-a
            46 -> 3  // Ma-la-khi

            // Tân Ước (NT)
            47 -> 28 // Mát-thêu
            48 -> 16 // Mác-cô
            49 -> 24 // Lu-ca
            50 -> 21 // Gio-an
            51 -> 28 // Tông Đồ Công Vụ
            52 -> 16 // Rô-ma
            53 -> 16 // 1 Cô-rin-tô
            54 -> 13 // 2 Cô-rin-tô
            55 -> 6  // Ga-lát
            56 -> 6  // Ê-phê-sô
            57 -> 4  // Phi-líp-phê
            58 -> 4  // Cô-lô-sê
            59 -> 5  // 1 Tê-sa-lô-ni-ca
            60 -> 3  // 2 Tê-sa-lô-ni-ca
            61 -> 6  // 1 Ti-mô-thê
            62 -> 4  // 2 Ti-mô-thê
            63 -> 3  // Ti-tô
            64 -> 1  // Phi-lê-môn
            65 -> 13 // Híp-ri
            66 -> 5  // Gia-cô-bê
            67 -> 5  // 1 Phê-rô
            68 -> 3  // 2 Phê-rô
            69 -> 5  // 1 Gio-an
            70 -> 1  // 2 Gio-an
            71 -> 1  // 3 Gio-an
            72 -> 1  // Giu-đa
            73 -> 22 // Khải Huyền
            else -> 1
        }
    }

    fun insertDownloadedVerse(bookId: Int, chapter: Int, verseNumber: Int, textVi: String, textEn: String, heading: String? = null) {
        val db = this.writableDatabase
        try {
            val values = ContentValues().apply {
                put(KEY_VERSE_BOOK_ID, bookId)
                put(KEY_VERSE_CHAPTER, chapter)
                put(KEY_VERSE_NUMBER, verseNumber)
                put(KEY_VERSE_TEXT_VI, textVi)
                put(KEY_VERSE_TEXT_EN, textEn)
                put(KEY_VERSE_HEADING, heading)
            }
            db.insertWithOnConflict(TABLE_VERSES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            Log.e(TAG, "insertDownloadedVerse error: ${e.message}")
        }
    }

    // Returns verses, returns empty list if not downloaded offline yet
    fun getVerses(bookId: Int, chapter: Int): List<Verse> {
        return queryVersesFromDb(bookId, chapter)
    }

    private fun queryVersesFromDb(bookId: Int, chapter: Int): List<Verse> {
        val verses = mutableListOf<Verse>()
        val db = this.readableDatabase
        try {
            val query = "SELECT v.*, (SELECT 1 FROM $TABLE_BOOKMARKS b WHERE b.$KEY_BOOKMARK_BOOK_ID = v.$KEY_VERSE_BOOK_ID AND b.$KEY_BOOKMARK_CHAPTER = v.$KEY_VERSE_CHAPTER AND b.$KEY_BOOKMARK_VERSE = v.$KEY_VERSE_NUMBER) as bookmarked FROM $TABLE_VERSES v WHERE v.$KEY_VERSE_BOOK_ID = ? AND v.$KEY_VERSE_CHAPTER = ? ORDER BY v.$KEY_VERSE_ID ASC"
            val cursor = db.rawQuery(query, arrayOf(bookId.toString(), chapter.toString()))

            if (cursor.moveToFirst()) {
                do {
                    val verse = Verse(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_VERSE_ID)),
                        bookId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_VERSE_BOOK_ID)),
                        chapter = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_VERSE_CHAPTER)),
                        verseNumber = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_VERSE_NUMBER)),
                        textVi = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VERSE_TEXT_VI)),
                        textEn = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VERSE_TEXT_EN)),
                        heading = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VERSE_HEADING)),
                        isBookmarked = cursor.getInt(cursor.getColumnIndexOrThrow("bookmarked")) == 1
                    )
                    verses.add(verse)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "queryVersesFromDb error: ${e.message}")
        }
        return verses
    }

    fun toggleBookmark(bookId: Int, chapter: Int, verseNumber: Int): Boolean {
        val db = this.writableDatabase
        try {
            val query = "SELECT * FROM $TABLE_BOOKMARKS WHERE $KEY_BOOKMARK_BOOK_ID = ? AND $KEY_BOOKMARK_CHAPTER = ? AND $KEY_BOOKMARK_VERSE = ?"
            val cursor = db.rawQuery(query, arrayOf(bookId.toString(), chapter.toString(), verseNumber.toString()))
            val exists = cursor.count > 0
            cursor.close()

            return if (exists) {
                db.delete(TABLE_BOOKMARKS, "$KEY_BOOKMARK_BOOK_ID = ? AND $KEY_BOOKMARK_CHAPTER = ? AND $KEY_BOOKMARK_VERSE = ?", arrayOf(bookId.toString(), chapter.toString(), verseNumber.toString()))
                false
            } else {
                val values = ContentValues().apply {
                    put(KEY_BOOKMARK_BOOK_ID, bookId)
                    put(KEY_BOOKMARK_CHAPTER, chapter)
                    put(KEY_BOOKMARK_VERSE, verseNumber)
                    put(KEY_BOOKMARK_DATE, System.currentTimeMillis())
                }
                db.insert(TABLE_BOOKMARKS, null, values)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "toggleBookmark error: ${e.message}")
            return false
        }
    }

    // --- Gamification Helper Methods ---
    fun getUserCoins(): Int {
        val db = this.readableDatabase
        var coins = 0
        try {
            val cursor = db.rawQuery("SELECT $KEY_USER_COINS FROM $TABLE_USERS WHERE $KEY_USER_ID = 1", null)
            if (cursor.moveToFirst()) {
                coins = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return coins
    }

    fun addCoins(amount: Int) {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TABLE_USERS SET $KEY_USER_COINS = $KEY_USER_COINS + $amount WHERE $KEY_USER_ID = 1")
    }

    fun isChapterCompleted(bookId: Int, chapter: Int): Boolean {
        val db = this.readableDatabase
        var completed = false
        try {
            val cursor = db.rawQuery("SELECT $KEY_PROG_COMPLETED FROM $TABLE_CHAPTER_PROGRESS WHERE $KEY_PROG_BOOK_ID = ? AND $KEY_PROG_CHAPTER = ?", arrayOf(bookId.toString(), chapter.toString()))
            if (cursor.moveToFirst()) {
                completed = cursor.getInt(0) == 1
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return completed
    }

    fun markChapterCompletedAndClaimReward(bookId: Int, chapter: Int, rewardAmount: Int): Boolean {
        val db = this.writableDatabase
        var rewardClaimed = false
        
        db.beginTransaction()
        try {
            // Kiểm tra xem đã claim chưa
            var alreadyClaimed = false
            val cursor = db.rawQuery("SELECT $KEY_PROG_REWARD_CLAIMED FROM $TABLE_CHAPTER_PROGRESS WHERE $KEY_PROG_BOOK_ID = ? AND $KEY_PROG_CHAPTER = ?", arrayOf(bookId.toString(), chapter.toString()))
            if (cursor.moveToFirst()) {
                alreadyClaimed = cursor.getInt(0) == 1
            }
            cursor.close()

            if (!alreadyClaimed) {
                // Đánh dấu hoàn thành và đã claim thưởng
                val values = ContentValues().apply {
                    put(KEY_PROG_BOOK_ID, bookId)
                    put(KEY_PROG_CHAPTER, chapter)
                    put(KEY_PROG_COMPLETED, 1)
                    put(KEY_PROG_REWARD_CLAIMED, 1)
                }
                db.insertWithOnConflict(TABLE_CHAPTER_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                
                // Cộng tiền
                db.execSQL("UPDATE $TABLE_USERS SET $KEY_USER_COINS = $KEY_USER_COINS + $rewardAmount WHERE $KEY_USER_ID = 1")
                rewardClaimed = true
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return rewardClaimed
    }

    fun getUserXp(): Int {
        val db = this.readableDatabase
        var xp = 0
        try {
            val cursor = db.rawQuery("SELECT xp FROM $TABLE_USERS WHERE $KEY_USER_ID = 1", null)
            if (cursor.moveToFirst()) {
                xp = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return xp
    }

    fun getUserLevel(): Int {
        val db = this.readableDatabase
        var level = 1
        try {
            val cursor = db.rawQuery("SELECT level FROM $TABLE_USERS WHERE $KEY_USER_ID = 1", null)
            if (cursor.moveToFirst()) {
                level = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return level
    }

    fun getUserStreak(): Int {
        val db = this.readableDatabase
        var streak = 0
        try {
            val cursor = db.rawQuery("SELECT $KEY_USER_STREAK FROM $TABLE_USERS WHERE $KEY_USER_ID = 1", null)
            if (cursor.moveToFirst()) {
                streak = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return streak
    }

    fun addXp(amount: Int): Boolean {
        val db = this.writableDatabase
        var levelUp = false
        db.beginTransaction()
        try {
            var currentXp = 0
            var currentLevel = 1
            val cursor = db.rawQuery("SELECT xp, level FROM $TABLE_USERS WHERE $KEY_USER_ID = 1", null)
            if (cursor.moveToFirst()) {
                currentXp = cursor.getInt(0)
                currentLevel = cursor.getInt(1)
            }
            cursor.close()

            var newXp = currentXp + amount
            var newLevel = currentLevel

            while (newXp >= 1000) {
                newXp -= 1000
                newLevel++
                levelUp = true
            }

            db.execSQL("UPDATE $TABLE_USERS SET xp = $newXp, level = $newLevel WHERE $KEY_USER_ID = 1")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return levelUp
    }

    fun updateStreak(streak: Int) {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TABLE_USERS SET $KEY_USER_STREAK = $streak WHERE $KEY_USER_ID = 1")
    }

    fun updateLastActive(timestamp: Long) {
        val db = this.writableDatabase
        db.execSQL("UPDATE $TABLE_USERS SET $KEY_USER_LAST_ACTIVE = $timestamp WHERE $KEY_USER_ID = 1")
    }

    fun getLastActive(): Long {
        val db = this.readableDatabase
        var lastActive = 0L
        try {
            val cursor = db.rawQuery("SELECT $KEY_USER_LAST_ACTIVE FROM $TABLE_USERS WHERE $KEY_USER_ID = 1", null)
            if (cursor.moveToFirst()) {
                lastActive = cursor.getLong(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return lastActive
    }
}
