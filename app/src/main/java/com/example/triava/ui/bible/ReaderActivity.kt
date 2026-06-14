package com.example.triava.ui.bible

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.SuperscriptSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.triava.R
import com.example.triava.data.database.BibleDatabaseHelper
import com.example.triava.data.model.Verse
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ReaderActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var dbHelper: BibleDatabaseHelper

    private lateinit var layoutDownload: LinearLayout
    private lateinit var progressDownload: ProgressBar
    private lateinit var tvDownloadMessage: TextView

    // Header Controls
    private lateinit var btnHeaderPrev: ImageButton
    private lateinit var btnHeaderNext: ImageButton
    private lateinit var tvHeaderChapter: TextView

    // Content Views
    private lateinit var scrollContent: ScrollView
    private lateinit var tvVersesContentVi: TextView

    // Media Player Views
    private lateinit var seekbarMedia: SeekBar
    private lateinit var tvMediaCurrent: TextView
    private lateinit var tvMediaTotal: TextView
    private lateinit var btnMediaRewind: ImageButton
    private lateinit var btnMediaForward: ImageButton
    private lateinit var btnTtsPlay: FloatingActionButton
    private lateinit var btnTtsSpeed: TextView

    private lateinit var btnFooterPrev: ImageButton
    private lateinit var btnFooterNext: ImageButton

    // Speech speed control
    private val speechRates = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var currentSpeedIndex = 1

    // State Variables
    private var bookId: Int = 1
    private var bookNameVi: String = ""
    private var bookNameEn: String = ""
    private var currentChapter: Int = 1
    private var isDownloadFailed = false

    private var currentVerses: List<Verse> = emptyList()
    private var currentPlayingVerseIndex = 0

    // TTS Engine
    private var tts: TextToSpeech? = null
    private var isPlayingTts = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        // Extract Intent Parameters
        bookId = intent.getIntExtra("BOOK_ID", 1)
        bookNameVi = intent.getStringExtra("BOOK_NAME_VI") ?: ""
        bookNameEn = intent.getStringExtra("BOOK_NAME_EN") ?: ""
        currentChapter = intent.getIntExtra("CHAPTER", 1)

        dbHelper = BibleDatabaseHelper(this)

        // Initialize Views
        toolbar = findViewById(R.id.reader_toolbar)
        layoutDownload = findViewById(R.id.layout_download)
        progressDownload = findViewById(R.id.progress_download)
        tvDownloadMessage = findViewById(R.id.tv_download_message)

        btnHeaderPrev = findViewById(R.id.btn_header_prev)
        btnHeaderNext = findViewById(R.id.btn_header_next)
        tvHeaderChapter = findViewById(R.id.tv_header_chapter)

        scrollContent = findViewById(R.id.scroll_content)
        tvVersesContentVi = findViewById(R.id.tv_verses_content_vi)

        seekbarMedia = findViewById(R.id.seekbar_media)
        tvMediaCurrent = findViewById(R.id.tv_media_current)
        tvMediaTotal = findViewById(R.id.tv_media_total)
        btnMediaRewind = findViewById(R.id.btn_media_rewind)
        btnMediaForward = findViewById(R.id.btn_media_forward)
        btnTtsPlay = findViewById(R.id.btn_tts_play)
        btnTtsSpeed = findViewById(R.id.btn_tts_speed)

        btnFooterPrev = findViewById(R.id.btn_footer_prev)
        btnFooterNext = findViewById(R.id.btn_footer_next)

        // Setup Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup Header Chapter Selector
        tvHeaderChapter.setOnClickListener {
            showChapterSelectionBottomSheet()
        }

        // Setup Prev/Next Navigation Clicks
        val prevClickListener = View.OnClickListener {
            if (currentChapter > 1) {
                stopTts()
                currentChapter--
                updateTitle()
                checkContent()
            }
        }
        btnHeaderPrev.setOnClickListener(prevClickListener)
        btnFooterPrev.setOnClickListener(prevClickListener)

        val nextClickListener = View.OnClickListener {
            val totalChapters = dbHelper.getChaptersCount(bookId)
            if (currentChapter < totalChapters) {
                stopTts()
                playNextChapter()
            }
        }
        btnHeaderNext.setOnClickListener(nextClickListener)
        btnFooterNext.setOnClickListener(nextClickListener)

        // Setup Quiz Button
        val btnTakeQuiz = findViewById<android.widget.Button>(R.id.btn_take_quiz)
        btnTakeQuiz.setOnClickListener {
            showQuizDialog()
        }

        // Setup Media Controls
        btnTtsPlay.setOnClickListener {
            toggleTts()
        }
        btnMediaRewind.setOnClickListener {
            rewindVerse()
        }
        btnMediaForward.setOnClickListener {
            forwardVerse()
        }
        btnTtsSpeed.setOnClickListener {
            currentSpeedIndex = (currentSpeedIndex + 1) % speechRates.size
            val rate = speechRates[currentSpeedIndex]
            tts?.setSpeechRate(rate)
            btnTtsSpeed.text = "${rate}x"
            if (isPlayingTts) {
                playVerse(currentPlayingVerseIndex)
            }
        }

        // Setup Seekbar Dragging Listener
        seekbarMedia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentPlayingVerseIndex = progress
                    tvMediaCurrent.text = "Câu ${progress + 1}"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isPlayingTts) {
                    playVerse(currentPlayingVerseIndex)
                } else {
                    updateProgressUi()
                }
            }
        })

        // Setup retry click listener on the download container
        layoutDownload.setOnClickListener {
            if (isDownloadFailed) {
                downloadChapter()
            }
        }

        // Initialize TTS
        initTts()

        // Sync & check content
        updateTitle()
        checkContent()
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.forLanguageTag("vi"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("ReaderActivity", "Vietnamese TTS not supported on this device.")
                } else {
                    tts?.setSpeechRate(speechRates[currentSpeedIndex])
                }
                
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId != null && utteranceId.startsWith("verse_")) {
                            val completedIndex = utteranceId.substringAfter("verse_").toIntOrNull()
                            if (completedIndex == currentPlayingVerseIndex) {
                                runOnUiThread {
                                    playNextVerse()
                                }
                            }
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        runOnUiThread { stopTts() }
                    }
                })
            } else {
                Log.e("ReaderActivity", "TTS Initialization failed.")
            }
        }
    }

    private fun toggleTts() {
        if (currentVerses.isEmpty()) return

        if (isPlayingTts) {
            tts?.stop()
            isPlayingTts = false
            btnTtsPlay.setImageResource(R.drawable.ic_play)
        } else {
            playVerse(currentPlayingVerseIndex)
        }
    }

    private fun playVerse(index: Int) {
        if (index < 0 || index >= currentVerses.size) return
        currentPlayingVerseIndex = index
        updateProgressUi()

        val verse = currentVerses[index]
        val textToSpeak = if (!verse.heading.isNullOrBlank()) {
            "${verse.heading}\n\n${verse.textVi}"
        } else {
            verse.textVi
        }
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "verse_$index")
        }
        val result = tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "verse_$index")
        if (result == TextToSpeech.SUCCESS) {
            isPlayingTts = true
            btnTtsPlay.setImageResource(R.drawable.ic_pause)
        } else {
            Toast.makeText(this, "Không thể phát âm thanh", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playNextVerse() {
        val nextIndex = currentPlayingVerseIndex + 1
        if (nextIndex < currentVerses.size) {
            playVerse(nextIndex)
        } else {
            // Chapter complete! Transition to next chapter.
            stopTts()
            playNextChapter(autoPlay = true)
        }
    }

    private fun showVictoryDialog(coinsEarned: Int, autoPlay: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_victory, null)
        val tvCoins = dialogView.findViewById<TextView>(R.id.tv_coins_earned)
        tvCoins.text = "+$coinsEarned Vàng"
        dialogView.findViewById<View>(R.id.layout_xp_reward).visibility = View.GONE

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<View>(R.id.btn_continue).setOnClickListener {
            dialog.dismiss()
            playNextChapter(autoPlay)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun playNextChapter(autoPlay: Boolean = false) {
        val totalChapters = dbHelper.getChaptersCount(bookId)
        if (currentChapter < totalChapters) {
            currentChapter++
            currentPlayingVerseIndex = 0
            updateTitle()
            checkContent(autoPlay = autoPlay)
        }
    }

    private fun rewindVerse() {
        if (currentPlayingVerseIndex > 0) {
            val target = currentPlayingVerseIndex - 1
            if (isPlayingTts) {
                playVerse(target)
            } else {
                currentPlayingVerseIndex = target
                updateProgressUi()
            }
        }
    }

    private fun forwardVerse() {
        if (currentPlayingVerseIndex < currentVerses.size - 1) {
            val target = currentPlayingVerseIndex + 1
            if (isPlayingTts) {
                playVerse(target)
            } else {
                currentPlayingVerseIndex = target
                updateProgressUi()
            }
        }
    }

    private fun stopTts() {
        tts?.stop()
        isPlayingTts = false
        currentPlayingVerseIndex = 0
        updateProgressUi()
        btnTtsPlay.setImageResource(R.drawable.ic_play)
    }

    private fun updateProgressUi() {
        if (currentVerses.isEmpty()) {
            seekbarMedia.max = 0
            seekbarMedia.progress = 0
            tvMediaCurrent.text = "Câu 0"
            tvMediaTotal.text = "0 câu"
            return
        }
        
        seekbarMedia.max = currentVerses.size - 1
        seekbarMedia.progress = currentPlayingVerseIndex
        tvMediaCurrent.text = "Câu ${currentPlayingVerseIndex + 1}"
        tvMediaTotal.text = "${currentVerses.size} câu"
    }

    private fun updateTitle() {
        title = bookNameVi
        supportActionBar?.title = bookNameVi
        tvHeaderChapter.text = "Chương $currentChapter"

        // Update Prev/Next Buttons Enabled State
        val totalChapters = dbHelper.getChaptersCount(bookId)
        val hasPrev = currentChapter > 1
        val hasNext = currentChapter < totalChapters

        btnHeaderPrev.isEnabled = hasPrev
        btnHeaderPrev.alpha = if (hasPrev) 1.0f else 0.4f
        btnFooterPrev.isEnabled = hasPrev
        btnFooterPrev.alpha = if (hasPrev) 1.0f else 0.4f

        btnHeaderNext.isEnabled = hasNext
        btnHeaderNext.alpha = if (hasNext) 1.0f else 0.4f
        btnFooterNext.isEnabled = hasNext
        btnFooterNext.alpha = if (hasNext) 1.0f else 0.4f
        
        // Show Quiz button on EVERY chapter
        val btnTakeQuiz = findViewById<android.widget.Button>(R.id.btn_take_quiz)
        btnTakeQuiz.visibility = View.VISIBLE
        btnTakeQuiz.text = "Làm Quiz Chương $currentChapter"
    }

    private fun showQuizDialog() {
        val questions = com.example.triava.data.repository.QuizRepository.getQuestionsForChapter(this, bookId, currentChapter)
        
        if (questions.isNotEmpty()) {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("QUIZ_MODE", "BOOK")
                putExtra("BOOK_ID", bookId)
                putExtra("BOOK_NAME_VI", bookNameVi)
                putExtra("CHAPTER", currentChapter)
            }
            startActivity(intent)
        } else {
            val bookQuestions = com.example.triava.data.repository.QuizRepository.getQuestionsForBook(this, bookId)
            if (bookQuestions.isNotEmpty()) {
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("QUIZ_MODE", "BOOK")
                    putExtra("BOOK_ID", bookId)
                    putExtra("BOOK_NAME_VI", bookNameVi)
                }
                startActivity(intent)
            } else {
                // For books without a quiz yet, grant reward directly
                val totalChapters = dbHelper.getChaptersCount(bookId)
                val rewardAmount = totalChapters * 10
                
                val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Hoàn thành sách! 📖")
                    .setMessage("Bạn đã hoàn thành sách $bookNameVi!\n(Bài kiểm tra cho sách này đang được cập nhật, bạn được nhận thưởng trực tiếp: $rewardAmount 🪙)")
                    .setPositiveButton("Nhận Thưởng!") { _, _ ->
                        val claimed = dbHelper.markChapterCompletedAndClaimReward(bookId, 9999, rewardAmount)
                        if (claimed) {
                            showVictoryDialog(rewardAmount, autoPlay = false)
                        } else {
                            Toast.makeText(this, "Bạn đã nhận phần thưởng cho sách này rồi!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setCancelable(false)
                    .create()
                dialog.show()
            }
        }
    }

    private fun checkContent(autoPlay: Boolean = false) {
        val verses = dbHelper.getVerses(bookId, currentChapter)
        if (verses.isEmpty()) {
            scrollContent.visibility = View.GONE
            layoutDownload.visibility = View.VISIBLE
            progressDownload.visibility = View.VISIBLE
            tvDownloadMessage.text = "Đang tải Lời Chúa..."
            downloadChapter(autoPlay)
        } else {
            scrollContent.visibility = View.VISIBLE
            layoutDownload.visibility = View.GONE
            currentVerses = verses
            displayContinuousContent(verses)
            updateProgressUi()
            if (autoPlay) {
                playVerse(0)
            }
        }
    }

    private fun displayContinuousContent(verses: List<Verse>) {
        val viBuilder = SpannableStringBuilder()

        // Thêm "Chương X" nếu câu đầu tiên không có heading từ API
        val firstVerseHasHeading = verses.firstOrNull()?.heading?.isNotBlank() == true
        if (!firstVerseHasHeading) {
            val chapterTitle = if (bookNameVi.equals("Thánh vịnh", ignoreCase = true)) {
                "Thánh vịnh $currentChapter\n\n"
            } else {
                "Chương $currentChapter\n\n"
            }
            val chapterStart = viBuilder.length
            viBuilder.append(chapterTitle)
            viBuilder.setSpan(StyleSpan(Typeface.BOLD), chapterStart, viBuilder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            viBuilder.setSpan(RelativeSizeSpan(1.5f), chapterStart, viBuilder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        for (verse in verses) {
            // Heading
            if (!verse.heading.isNullOrBlank()) {
                val headingStart = viBuilder.length
                viBuilder.append("${verse.heading}\n\n")
                viBuilder.setSpan(StyleSpan(Typeface.BOLD), headingStart, viBuilder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                viBuilder.setSpan(RelativeSizeSpan(1.1f), headingStart, viBuilder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Vietnamese
            val startVi = viBuilder.length
            viBuilder.append("${verse.verseNumber} ")
            viBuilder.setSpan(
                SuperscriptSpan(),
                startVi,
                viBuilder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            viBuilder.setSpan(
                RelativeSizeSpan(0.65f),
                startVi,
                viBuilder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            viBuilder.setSpan(
                ForegroundColorSpan(Color.parseColor("#C9A054")), // gold
                startVi,
                viBuilder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            viBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                startVi,
                viBuilder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            viBuilder.append("${verse.textVi}  ")
        }

        tvVersesContentVi.text = viBuilder
    }

    private fun downloadChapter(autoPlay: Boolean = false) {
        isDownloadFailed = false
        progressDownload.visibility = View.VISIBLE
        tvDownloadMessage.text = "Đang tải Lời Chúa..."

        val book = dbHelper.getBook(bookId)
        val bookAbbrev = book?.abbreviation?.lowercase() ?: "st"
        val encodedAbbrev = android.net.Uri.encode(bookAbbrev)
        val hfUrl = "https://huggingface.co/datasets/v-bible/catholic-resources/resolve/main/books/bible/versions/ktcgkpv.org/kt2011/$encodedAbbrev/$currentChapter/$currentChapter.json"

        lifecycleScope.launch {
            val viJsonStr = withContext(Dispatchers.IO) {
                downloadUrl(hfUrl)
            }

            if (viJsonStr != null) {
                try {
                    val viObj = JSONObject(viJsonStr)
                    val viVersesArray = viObj.getJSONArray("verses")
                    val headingsArray = viObj.optJSONArray("headings")
                    val verseIdToHeading = mutableMapOf<String, String>()

                    if (headingsArray != null) {
                        for (i in 0 until headingsArray.length()) {
                            val hObj = headingsArray.getJSONObject(i)
                            val verseId = hObj.optString("verseId")
                            val text = hObj.optString("text")
                            if (verseId.isNotEmpty() && text.isNotEmpty()) {
                                val current = verseIdToHeading[verseId]
                                if (current != null) {
                                    verseIdToHeading[verseId] = "$current\n$text"
                                } else {
                                    verseIdToHeading[verseId] = text
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.IO) {
                        for (i in 0 until viVersesArray.length()) {
                            val verseObj = viVersesArray.getJSONObject(i)
                            val numberStr = verseObj.get("number").toString()
                            val match = Regex("\\d+").find(numberStr)
                            val verseNum = match?.value?.toInt() ?: continue
                            val textVi = verseObj.optString("content", verseObj.optString("text", "")).trim()
                            val verseId = verseObj.optString("id")
                            val heading = verseIdToHeading[verseId]

                            dbHelper.insertDownloadedVerse(bookId, currentChapter, verseNum, textVi, "", heading)
                        }
                    }
                    checkContent(autoPlay)
                } catch (e: Exception) {
                    Log.e("ReaderActivity", "JSON parse error: ${e.message}")
                    isDownloadFailed = true
                    progressDownload.visibility = View.GONE
                    tvDownloadMessage.text = "Lỗi định dạng dữ liệu. Chạm để thử lại."
                }
            } else {
                isDownloadFailed = true
                progressDownload.visibility = View.GONE
                tvDownloadMessage.text = "Tải xuống thất bại. Chạm để thử lại."
            }
        }
    }

    private fun downloadUrl(urlString: String): String? {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doInput = true

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return response.toString()
            } else {
                Log.e("ReaderActivity", "HTTP error code $responseCode for $urlString")
                return null
            }
        } catch (e: Exception) {
            Log.e("ReaderActivity", "Download error for $urlString: ${e.message}")
            return null
        }
    }

    private fun showChapterSelectionBottomSheet() {
        val totalChapters = dbHelper.getChaptersCount(bookId)
        val chapters = (1..totalChapters).toList()

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_chapter_selection, null)

        val tvTitle = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvSubtitle = bottomSheetView.findViewById<TextView>(R.id.tv_dialog_subtitle)
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.recycler_chapters)

        tvTitle.text = bookNameVi
        tvSubtitle.text = "$bookNameEn • $totalChapters chương"

        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)
        recyclerView.adapter = ChapterAdapter(chapters) { selectedChapter ->
            dialog.dismiss()
            stopTts()
            currentChapter = selectedChapter
            updateTitle()
            checkContent()
        }

        dialog.setContentView(bottomSheetView)
        dialog.show()
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}
