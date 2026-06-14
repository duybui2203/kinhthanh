package com.example.triava.data.repository

import android.content.Context
import com.example.triava.data.model.Question
import org.json.JSONObject
import java.util.Calendar
import java.util.Random

object QuizRepository {
    
    // In-memory cache: BookId -> Map of ChapterNumber -> List of Questions
    private val quizCache = mutableMapOf<Int, Map<Int, List<Question>>>()

    private fun loadBookQuiz(context: Context, bookId: Int): Map<Int, List<Question>> {
        val cached = quizCache[bookId]
        if (cached != null) return cached

        val map = mutableMapOf<Int, List<Question>>()
        try {
            val jsonString = context.assets.open("quiz/vn/$bookId.json").bufferedReader().use { it.readText() }
            val rootObj = JSONObject(jsonString)
            val chaptersArray = rootObj.getJSONArray("chapters")
            for (i in 0 until chaptersArray.length()) {
                val chapterObj = chaptersArray.getJSONObject(i)
                val chapterNum = chapterObj.getInt("chapter")
                val questionsArray = chapterObj.getJSONArray("questions")
                val questions = mutableListOf<Question>()
                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)
                    val text = qObj.getString("text")
                    val optionsArray = qObj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (k in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(k))
                    }
                    val correctIndex = qObj.getInt("correctAnswerIndex")
                    questions.add(Question(text, options, correctIndex))
                }
                map[chapterNum] = questions
            }
            quizCache[bookId] = map
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun getQuestionsForChapter(context: Context, bookId: Int, chapter: Int): List<Question> {
        val bookQuiz = loadBookQuiz(context, bookId)
        val staticQuestions = bookQuiz[chapter]
        if (staticQuestions != null && staticQuestions.isNotEmpty()) {
            return staticQuestions
        }
        return generateDynamicQuestions(context, bookId, chapter)
    }

    fun getQuestionsForBook(context: Context, bookId: Int): List<Question> {
        val bookQuiz = loadBookQuiz(context, bookId)
        val staticQuestions = bookQuiz.values.flatten()
        if (staticQuestions.isNotEmpty()) {
            return staticQuestions
        }

        // Dynamically generate book-level questions (20 questions from random chapters)
        val dbHelper = com.example.triava.data.database.BibleDatabaseHelper(context)
        val totalChapters = dbHelper.getChaptersCount(bookId)
        val questions = mutableListOf<Question>()
        if (totalChapters <= 0) return emptyList()

        val chaptersToQuery = (1..totalChapters).shuffled().take(minOf(5, totalChapters))
        for (ch in chaptersToQuery) {
            questions.addAll(generateDynamicQuestions(context, bookId, ch))
        }
        return questions.shuffled().take(20)
    }

    private fun generateDynamicQuestions(context: Context, bookId: Int, chapter: Int): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            val dbHelper = com.example.triava.data.database.BibleDatabaseHelper(context)
            val verses = dbHelper.getVerses(bookId, chapter)
            if (verses.isEmpty()) return emptyList()

            val book = dbHelper.getBook(bookId)
            val bookName = book?.nameVi ?: "Kinh Thánh"

            // Shuffling verses so we get different questions if they play again
            val shuffledVerses = verses.shuffled()
            val limit = minOf(10, shuffledVerses.size)

            val fallbackDistractors = listOf(
                "Thiên Chúa", "ĐỨC CHÚA", "sứ thần", "Giao ước", "lễ tế", "bàn thờ",
                "Áp-ra-ham", "Gia-cóp", "Giô-sép", "Mô-sê", "Ai Cập", "Ca-na-an",
                "lúa mì", "đất đai", "súc vật", "con chiên", "bánh mì", "rượu nho",
                "hoang địa", "chúc phúc", "đức tin", "công chính", "sự sống", "đền thờ"
            )

            for (i in 0 until limit) {
                val verse = shuffledVerses[i]
                val originalText = verse.textVi
                if (originalText.isBlank()) continue

                // Find candidate words to blank out
                val words = originalText.split(Regex("\\s+"))
                val candidates = words.map { word ->
                    word.trim { it in listOf(',', '.', ';', ':', '?', '!', '"', '\'', '(', ')', '[', ']', '“', '”', '‘', '’') }
                }.filter { cleanWord ->
                    cleanWord.length >= 3 && (
                        cleanWord.contains("-") || // proper name hyphenated (e.g. Gia-cóp)
                        cleanWord.firstOrNull()?.isUpperCase() == true || // capitalized word
                        cleanWord.length >= 5 // longer word
                    )
                }

                if (candidates.isEmpty()) continue

                // Pick a random candidate word
                val targetWord = candidates.random()

                // Replace the target word in the text with [...]
                var blankedText = originalText
                val wordIndex = originalText.indexOf(targetWord)
                if (wordIndex != -1) {
                    blankedText = originalText.substring(0, wordIndex) + "[...]" + originalText.substring(wordIndex + targetWord.length)
                } else {
                    continue
                }

                // Prepare options (correct answer + 3 distractors)
                val options = mutableSetOf<String>()
                options.add(targetWord)

                // Try to get other words from the same chapter as distractors
                val otherWords = verses.flatMap { v ->
                    v.textVi.split(Regex("\\s+")).map { w ->
                        w.trim { it in listOf(',', '.', ';', ':', '?', '!', '"', '\'', '(', ')', '[', ']', '“', '”', '‘', '’') }
                    }
                }.filter { cleanWord ->
                    cleanWord.length >= 3 && 
                    cleanWord != targetWord && 
                    cleanWord.firstOrNull()?.isUpperCase() == targetWord.firstOrNull()?.isUpperCase()
                }.shuffled()

                for (w in otherWords) {
                    if (options.size < 4) {
                        options.add(w)
                    } else {
                        break
                    }
                }

                // If still not enough options, use fallbacks
                val shuffledFallbacks = fallbackDistractors.shuffled()
                for (w in shuffledFallbacks) {
                    if (options.size < 4 && w != targetWord) {
                        options.add(w)
                    }
                }

                // Fallback placeholders
                val placeholders = listOf("Đức tin", "Hy vọng", "Yêu thương", "Bình an")
                for (w in placeholders) {
                    if (options.size < 4 && w != targetWord) {
                        options.add(w)
                    }
                }

                val optionsList = options.toList().shuffled()
                val correctIndex = optionsList.indexOf(targetWord)

                questions.add(
                    Question(
                        text = "Điền từ còn thiếu vào chỗ trống:\n\n\"$blankedText\"\n\n($bookName ${verse.chapter}:${verse.verseNumber})",
                        options = optionsList,
                        correctAnswerIndex = correctIndex
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return questions
    }

    fun getRandomQuestions(context: Context, count: Int): List<Question> {
        val allQuestions = mutableListOf<Question>()
        try {
            val files = context.assets.list("quiz/vn") ?: emptyArray()
            for (filename in files) {
                if (filename.endsWith(".json")) {
                    val bookId = filename.substringBefore(".json").toIntOrNull() ?: continue
                    allQuestions.addAll(getQuestionsForBook(context, bookId))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (allQuestions.isEmpty()) return emptyList()
        return allQuestions.shuffled().take(count)
    }

    fun getQuestionsByCategory(context: Context, testament: String, count: Int): List<Question> {
        val bookIds = if (testament == "OT") 1..46 else 47..73
        val categoryQuestions = mutableListOf<Question>()
        try {
            val files = context.assets.list("quiz/vn") ?: emptyArray()
            for (filename in files) {
                if (filename.endsWith(".json")) {
                    val bookId = filename.substringBefore(".json").toIntOrNull() ?: continue
                    if (bookId in bookIds) {
                        categoryQuestions.addAll(getQuestionsForBook(context, bookId))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (categoryQuestions.isEmpty()) {
            return getRandomQuestions(context, count)
        }
        return categoryQuestions.shuffled().take(count)
    }

    fun getDailyQuestions(context: Context): List<Question> {
        val allQuestions = mutableListOf<Question>()
        try {
            val files = context.assets.list("quiz/vn") ?: emptyArray()
            for (filename in files) {
                if (filename.endsWith(".json")) {
                    val bookId = filename.substringBefore(".json").toIntOrNull() ?: continue
                    allQuestions.addAll(getQuestionsForBook(context, bookId))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (allQuestions.isEmpty()) return emptyList()

        // Seed based on current date
        val calendar = Calendar.getInstance()
        val seed = (calendar.get(Calendar.YEAR) * 10000 + 
                    (calendar.get(Calendar.MONTH) + 1) * 100 + 
                    calendar.get(Calendar.DAY_OF_MONTH)).toLong()
        
        val random = Random(seed)
        val shuffled = allQuestions.toMutableList()
        
        // Custom shuffle using the seeded random
        for (i in shuffled.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }
        
        return shuffled.take(5)
    }
}
