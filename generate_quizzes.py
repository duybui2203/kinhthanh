import os
import sys
import json
import time
import urllib.request
import urllib.error

# Path configurations
METADATA_PATH = "metadata.json"
QUIZ_OUTPUT_PATH = os.path.join("app", "src", "main", "assets", "bible_quiz.json")

def get_api_key():
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        print("Không tìm thấy biến môi trường GEMINI_API_KEY.")
        api_key = input("Nhập Gemini API Key của bạn: ").strip()
    if not api_key:
        print("Lỗi: Cần có API Key để chạy script.")
        sys.exit(1)
    return api_key

def call_gemini_api(prompt, api_key):
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={api_key}"
    headers = {"Content-Type": "application/json"}
    data = {
        "contents": [{
            "parts": [{"text": prompt}]
        }],
        "generationConfig": {
            "responseMimeType": "application/json"
        }
    }
    
    req = urllib.request.Request(
        url, 
        data=json.dumps(data).encode("utf-8"), 
        headers=headers, 
        method="POST"
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode("utf-8"))
            text_response = result['candidates'][0]['content']['parts'][0]['text']
            return json.loads(text_response.strip())
    except urllib.error.HTTPError as e:
        print(f"\nLỗi HTTP từ Gemini API: {e.code} - {e.reason}")
        print(e.read().decode("utf-8"))
        return None
    except Exception as e:
        print(f"\nLỗi kết nối hoặc phân tích kết quả: {e}")
        return None

def load_existing_quizzes():
    if os.path.exists(QUIZ_OUTPUT_PATH):
        try:
            with open(QUIZ_OUTPUT_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
                if isinstance(data, dict) and "quizzes" in data:
                    return data
        except Exception as e:
            print(f"Lỗi khi đọc file quiz hiện tại: {e}. Sẽ ghi đè file mới.")
    return {"quizzes": []}

def save_quizzes(quizzes_data):
    # Tạo thư mục cha nếu chưa có
    os.makedirs(os.path.dirname(QUIZ_OUTPUT_PATH), exist_ok=True)
    with open(QUIZ_OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(quizzes_data, f, ensure_ascii=False, indent=2)

def main():
    print("=== BẮT ĐẦU TẠO BỘ CÂU HỎI KINH THÁNH ===")
    
    api_key = get_api_key()
    
    # Đọc danh sách sách từ metadata.json
    if not os.path.exists(METADATA_PATH):
        print(f"Lỗi: Không tìm thấy file {METADATA_PATH}")
        sys.exit(1)
        
    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        books = json.load(f)
        
    print(f"Đã tìm thấy {len(books)} sách trong {METADATA_PATH}.")
    
    # Tải dữ liệu đã có để tiếp tục (resume) nếu cần
    existing_data = load_existing_quizzes()
    quiz_list = existing_data["quizzes"]
    
    # Tạo tập hợp các bookId đã có câu hỏi
    completed_book_ids = {q["bookId"] for q in quiz_list}
    print(f"Đã có bộ câu hỏi cho {len(completed_book_ids)}/{len(books)} sách.")
    
    for index, book in enumerate(books):
        book_id = book.get("bookOrder") or (index + 1)
        book_name = book.get("name")
        book_code = book.get("code")
        
        if book_id in completed_book_ids:
            # Bỏ qua nếu sách này đã có câu hỏi
            continue
            
        print(f"\nĐang sinh câu hỏi cho Sách {book_id}: {book_name} ({book_code})...", end="", flush=True)
        
        prompt = (
            f"Bạn là một chuyên gia thần học và Kinh Thánh Công Giáo. "
            f"Hãy tạo đúng 10 câu hỏi trắc nghiệm tiếng Việt chất lượng cao về nội dung cuốn sách '{book_name}' "
            f"trong Kinh Thánh Công Giáo (sách số {book_id} trong quy điển Công giáo, ký hiệu '{book_code}').\n"
            f"Yêu cầu:\n"
            f"1. Câu hỏi phải bao quát các sự kiện, nhân vật, bài học hoặc nội dung quan trọng nhất của sách này.\n"
            f"2. Mỗi câu hỏi gồm có câu hỏi ('text'), danh sách 4 lựa chọn ('options') và chỉ số của đáp án đúng ('correctAnswerIndex' từ 0 đến 3).\n"
            f"3. Câu hỏi và câu trả lời phải bám sát bản dịch Kinh Thánh Công Giáo (KTCGKPV).\n"
            f"4. Trả về kết quả trực tiếp dưới dạng một mảng JSON các đối tượng câu hỏi, không kèm thêm giải thích, markdown ```json hay ký tự nào ngoài JSON.\n\n"
            f"Định dạng JSON yêu cầu:\n"
            f"[\n"
            f"  {{\n"
            f"    \"text\": \"Thiên Chúa đã tạo dựng vũ trụ trong mấy ngày?\",\n"
            f"    \"options\": [\"5 ngày\", \"6 ngày\", \"7 ngày\", \"8 ngày\"],\n"
            f"    \"correctAnswerIndex\": 1\n"
            f"  }}\n"
            f"]"
        )
        
        # Thử sinh câu hỏi (tối đa 3 lần nếu lỗi)
        questions = None
        for attempt in range(3):
            questions = call_gemini_api(prompt, api_key)
            if questions and isinstance(questions, list) and len(questions) == 10:
                break
            print(".", end="", flush=True)
            time.sleep(2)
            
        if not questions:
            print(" THẤT BẠI (Bỏ qua hoặc dừng lại)")
            continue
            
        # Thêm vào danh sách và lưu ngay lập tức
        quiz_list.append({
            "bookId": int(book_id),
            "bookName": book_name,
            "questions": questions
        })
        
        # Sắp xếp lại danh sách theo bookId để file ngăn nắp
        quiz_list.sort(key=lambda x: x["bookId"])
        existing_data["quizzes"] = quiz_list
        save_quizzes(existing_data)
        
        print(" THÀNH CÔNG!")
        
        # Tránh bị giới hạn RPM của API miễn phí
        time.sleep(2.5)
        
    print("\n=== HOÀN THÀNH QUÁ TRÌNH SINH CÂU HỎI KINH THÁNH ===")

if __name__ == "__main__":
    main()
