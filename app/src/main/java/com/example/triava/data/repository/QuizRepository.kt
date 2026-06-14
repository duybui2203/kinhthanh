package com.example.triava.data.repository

import android.content.Context
import com.example.triava.data.model.Question
import org.json.JSONObject
import java.util.Calendar
import java.util.Random

object QuizRepository {
    
    private var quizMap: Map<Int, List<Question>>? = null

    @Synchronized
    fun init(context: Context) {
        if (quizMap != null) return
        val map = mutableMapOf<Int, List<Question>>()
        try {
            val jsonString = context.assets.open("bible_quiz.json").bufferedReader().use { it.readText() }
            val rootObj = JSONObject(jsonString)
            val quizzesArray = rootObj.getJSONArray("quizzes")
            for (i in 0 until quizzesArray.length()) {
                val quizObj = quizzesArray.getJSONObject(i)
                val bookId = quizObj.getInt("bookId")
                val questionsArray = quizObj.getJSONArray("questions")
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
                map[bookId] = questions
            }
            quizMap = map
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: load hardcoded if file is missing/corrupt
            quizMap = getFallbackQuizzes()
        }
    }

    private fun getFallbackQuizzes(): Map<Int, List<Question>> {
        return mapOf(
            1 to getGenesisQuestions(),
            2 to getExodusQuestions(),
            47 to getMatthewQuestions(),
            50 to getJohnQuestions()
        )
    }

    fun getQuestionsForBook(context: Context, bookId: Int): List<Question> {
        init(context)
        return quizMap?.get(bookId) ?: emptyList()
    }

    fun getRandomQuestions(context: Context, count: Int): List<Question> {
        init(context)
        val allQuestions = quizMap?.values?.flatten() ?: emptyList()
        if (allQuestions.isEmpty()) return emptyList()
        return allQuestions.shuffled().take(count)
    }

    fun getQuestionsByCategory(context: Context, testament: String, count: Int): List<Question> {
        init(context)
        val bookIds = if (testament == "OT") 1..46 else 47..73
        val categoryQuestions = quizMap?.filterKeys { it in bookIds }?.values?.flatten() ?: emptyList()
        if (categoryQuestions.isEmpty()) {
            return getRandomQuestions(context, count)
        }
        return categoryQuestions.shuffled().take(count)
    }

    fun getDailyQuestions(context: Context): List<Question> {
        init(context)
        val allQuestions = quizMap?.values?.flatten() ?: emptyList()
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

    private fun getGenesisQuestions(): List<Question> {
        return listOf(
            Question("Thiên Chúa đã tạo dựng vũ trụ trong mấy ngày?", listOf("5 ngày", "6 ngày", "7 ngày", "8 ngày"), 1),
            Question("Người đàn ông đầu tiên được Thiên Chúa tạo ra tên là gì?", listOf("Áp-ra-ham", "Mô-sê", "A-đam", "Nô-ê"), 2),
            Question("Tại sao Thiên Chúa lại cho trận đại hồng thủy xảy ra?", listOf("Vì con người quá đông đúc", "Vì con người tội lỗi và gian ác", "Vì muốn thanh lọc trái đất", "Vì một tai nạn tự nhiên"), 1),
            Question("Ai là người đã đóng tàu để cứu gia đình và các loài động vật khỏi hồng thủy?", listOf("Lót", "Gióp", "Áp-ra-ham", "Nô-ê"), 3),
            Question("Tháp nào mà con người xây dựng để cao đến tận trời nhưng bị Thiên Chúa làm lộn xộn ngôn ngữ?", listOf("Tháp Si-on", "Tháp Ba-bên", "Tháp Đa-vít", "Tháp Giê-ru-sa-lem"), 1),
            Question("Thiên Chúa đã gọi ai rời bỏ quê hương để đi đến một vùng đất mới và hứa sẽ làm cho ông thành một dân tộc lớn?", listOf("I-xa-ác", "Gia-cóp", "Áp-ra-ham", "Mô-sê"), 2),
            Question("Vợ của Áp-ra-ham, người đã sinh cho ông một người con trai khi bà đã già yếu, tên là gì?", listOf("Ra-khen", "Xa-ra", "Rê-bê-ca", "Lê-a"), 1),
            Question("Con trai duy nhất của Áp-ra-ham và Xa-ra tên là gì?", listOf("I-xa-ác", "Ích-ma-ên", "Ê-sau", "Gia-cóp"), 0),
            Question("Ai là người đã bán quyền trưởng nam của mình chỉ vì một bát cháo đậu đỏ?", listOf("Ê-sau", "Gia-cóp", "Giô-sép", "Giu-đa"), 0),
            Question("Ai đã bị các anh mình bán sang Ai Cập nhưng sau đó trở thành tể tướng?", listOf("Bên-gia-min", "Giu-đa", "Ru-bên", "Giô-sép"), 3)
        )
    }

    private fun getExodusQuestions(): List<Question> {
        return listOf(
            Question("Ai là người được Thiên Chúa chọn để dẫn dắt dân Israel ra khỏi Ai Cập?", listOf("Áp-ra-ham", "Gia-cóp", "Mô-sê", "A-ha-ron"), 2),
            Question("Thiên Chúa đã hiện ra với Mô-sê trong hình ảnh nào trên núi Hô-rếp?", listOf("Một đám mây", "Một bụi gai bốc cháy nhưng không tàn", "Một cột lửa", "Một thiên thần"), 1),
            Question("Có bao nhiêu tai ương đã giáng xuống Ai Cập trước khi Pha-ra-ôn chịu để dân Israel đi?", listOf("7", "10", "12", "40"), 1),
            Question("Tai ương cuối cùng giáng xuống Ai Cập là gì?", listOf("Nước hóa máu", "Dịch ếch nhái", "Bóng tối bao trùm", "Các con đầu lòng bị giết"), 3),
            Question("Biển nào đã rẽ nước ra để dân Israel đi qua như trên đất liền?", listOf("Biển Chết", "Biển Đỏ", "Biển Ga-li-lê", "Biển Địa Trung Hải"), 1),
            Question("Thiên Chúa đã ban thức ăn gì từ trời xuống cho dân Israel trong sa mạc?", listOf("Man-na", "Bánh mì", "Trái cây", "Thịt cừu"), 0),
            Question("Mô-sê đã nhận được Hai Mươi Điều Răn trên ngọn núi nào?", listOf("Núi Ca-rê-bê", "Núi Xi-nai", "Núi Các-mên", "Núi Ô-liu"), 1),
            Question("Trong khi Mô-sê ở trên núi, dân Israel đã đúc tượng con vật gì để thờ lạy?", listOf("Con cừu vàng", "Con rắn đồng", "Con bê vàng", "Con chim câu"), 2),
            Question("Ai là anh trai của Mô-sê và được chọn làm tư tế đầu tiên?", listOf("Giu-đa", "Lê-vi", "A-ha-ron", "Mi-ri-am"), 2),
            Question("Khám Giao Ước được đặt ở đâu trong Lều Hội Ngộ?", listOf("Nơi Thánh", "Nơi Cực Thánh", "Bàn Thờ", "Sân Lều"), 1)
        )
    }

    private fun getMatthewQuestions(): List<Question> {
        return listOf(
            Question("Sách Phúc Âm Mát-thêu bắt đầu bằng việc gì?", listOf("Gia phả của Chúa Giêsu", "Sự giáng sinh của Chúa Giêsu", "Gioan Tẩy Giả rao giảng", "Chúa Giêsu chịu phép rửa"), 0),
            Question("Chúa Giêsu đã sinh ra tại thành phố nào?", listOf("Na-da-rét", "Bê-lem", "Giê-ru-sa-lem", "Ca-phác-na-um"), 1),
            Question("Ai đã báo mộng cho Thánh Giuse đưa Hài Nhi và Mẹ Người trốn sang Ai Cập?", listOf("Thiên thần Gáp-ri-en", "Một vị đạo sĩ", "Sứ thần của Chúa", "Bà Ê-li-xa-bét"), 2),
            Question("Chúa Giêsu đã ăn chay trong hoang địa bao nhiêu ngày đêm?", listOf("7 ngày", "12 ngày", "40 ngày", "50 ngày"), 2),
            Question("Bài giảng nổi tiếng nhất của Chúa Giêsu trong Mát-thêu chương 5-7 gọi là gì?", listOf("Bài giảng trên Núi", "Bài giảng tại Đền Thờ", "Bài giảng bên Biển Hồ", "Bài giảng ở Hội Đường"), 0),
            Question("Trong kinh Lạy Cha, câu nào đi sau 'Xin tha nợ chúng con'?", listOf("Như chúng con cũng tha kẻ có nợ chúng con", "Và chớ để chúng con sa chước cám dỗ", "Nhưng cứu chúng con cho khỏi sự dữ", "Ý Cha thể hiện dưới đất cũng như trên trời"), 0),
            Question("Chúa Giêsu đã dùng mấy chiếc bánh và mấy con cá để nuôi 5000 người ăn no?", listOf("7 bánh, 2 cá", "5 bánh, 2 cá", "5 bánh, 5 cá", "7 bánh, 5 cá"), 1),
            Question("Môn đệ nào đã đi trên mặt nước để đến với Chúa Giêsu nhưng lại sợ hãi và chìm xuống?", listOf("Gio-an", "Gia-cô-bê", "Phê-rô", "An-rê"), 2),
            Question("Môn đệ nào đã phản bội và nộp Chúa Giêsu với giá 30 đồng bạc?", listOf("Tô-ma", "Giu-đa Ít-ca-ri-ốt", "Phê-rô", "Si-môn"), 1),
            Question("Lời cuối cùng Chúa Giêsu dặn dò các môn đệ trước khi về trời là gì?", listOf("Hãy đi giảng dạy muôn dân", "Hãy xây dựng đền thờ", "Hãy tránh xa người Pha-ri-sêu", "Hãy đi tìm chiên lạc"), 0)
        )
    }

    private fun getJohnQuestions(): List<Question> {
        return listOf(
            Question("Sách Phúc Âm Gio-an bắt đầu bằng câu nào?", listOf("Gia phả của Đức Giêsu Kitô", "Lúc khởi đầu đã có Ngôi Lời", "Có một người tên là Gioan", "Khởi đầu Tin Mừng Đức Giêsu Kitô"), 1),
            Question("Phép lạ đầu tiên Chúa Giêsu làm tại tiệc cưới Ca-na là gì?", listOf("Chữa người mù", "Hóa bánh ra nhiều", "Biến nước thành rượu", "Đi trên mặt nước"), 2),
            Question("Chúa Giêsu nói chuyện với người phụ nữ ngoại giáo nào bên bờ giếng Gia-cóp?", listOf("Người Samari", "Người Rôma", "Người Hylạp", "Người Cana-an"), 0),
            Question("Ai là người đã được Chúa Giêsu gọi ra khỏi mồ sau khi chết 4 ngày?", listOf("Da-kêu", "La-da-rô", "Nai-in", "Giai-rô"), 1),
            Question("Chúa Giêsu đã làm hành động gì cho các môn đệ trong Bữa Tiệc Ly?", listOf("Rửa mặt", "Rửa tay", "Rửa chân", "Xức dầu"), 2),
            Question("Chúa Giêsu tự xưng mình là gì trong Phúc Âm Gio-an?", listOf("Ta là bánh hằng sống", "Ta là sự sáng thế gian", "Ta là mục tử nhân lành", "Tất cả các ý trên"), 3),
            Question("Môn đệ nào đã không tin Chúa Giêsu phục sinh cho đến khi chạm vào vết thương của Người?", listOf("Phê-rô", "Tô-ma", "Giu-đa", "Mat-thêu"), 1),
            Question("Trên thập giá, Chúa Giêsu đã trao phó Đức Mẹ cho môn đệ nào?", listOf("Phê-rô", "Gio-an", "Gia-cô-bê", "Tô-ma"), 1),
            Question("Sau khi phục sinh, Chúa Giêsu đã hỏi Phê-rô câu gì 3 lần?", listOf("Con có yêu mến Thầy không?", "Con có tin Thầy không?", "Con có theo Thầy không?", "Con có phản Thầy không?"), 0),
            Question("Theo chương 21, người môn đệ 'được Chúa Giêsu yêu mến' là ai?", listOf("Phê-rô", "Chính tác giả sách Phúc Âm Gio-an", "Phao-lô", "Gia-cô-bê"), 1)
        )
    }
}
