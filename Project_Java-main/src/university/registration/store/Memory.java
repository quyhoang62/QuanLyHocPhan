package university.registration.store;

import university.registration.model.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Lớp Memory - Lưu trữ dữ liệu trong bộ nhớ (In-Memory Storage)
 * 
 * Đây là lớp quản lý toàn bộ dữ liệu của hệ thống đăng ký học phần.
 * Tất cả dữ liệu được lưu trữ trong bộ nhớ (RAM) dưới dạng các Map và List.
 * 
 * LƯU Ý QUAN TRỌNG:
 * - Đây là hệ thống demo, dữ liệu chỉ tồn tại trong bộ nhớ
 * - Khi tắt ứng dụng, tất cả dữ liệu sẽ bị mất (không lưu vào database)
 * - Trong hệ thống thực tế, cần thay thế bằng database (MySQL, PostgreSQL, v.v.)
 * 
 * Cấu trúc dữ liệu:
 * - adminPasswords: Lưu tài khoản admin (PĐT)
 * - studentsById: Lưu danh sách sinh viên (tra cứu theo MSSV)
 * - emailIndex: Index email để tra cứu sinh viên theo email (hỗ trợ đăng nhập bằng email)
 * - courses: Danh sách tất cả học phần trong hệ thống (không phụ thuộc học kỳ)
 * - terms: Danh sách các học kỳ (ví dụ: "20252", "20251")
 * - programs: Danh sách các chương trình đào tạo
 * - regs: Đăng ký học phần của sinh viên (cấu trúc: MSSV -> Học kỳ -> Danh sách RegItem)
 * - termSettings: Cấu hình học kỳ (trạng thái mở/đóng đăng ký)
 * - offerings: Cấu hình mở lớp cho từng học phần trong từng học kỳ
 * 
 * Tất cả các biến đều là static final để:
 * - Chỉ có một instance duy nhất trong toàn bộ ứng dụng (Singleton pattern)
 * - Không thể thay đổi tham chiếu (nhưng có thể thêm/sửa/xóa phần tử bên trong)
 */
public class Memory {
    /**
     * Tài khoản Phòng Đào Tạo (Admin): Map<username, password>
     * 
     * Lưu trữ thông tin đăng nhập của các tài khoản admin.
     * 
     * Ví dụ:
     *   adminPasswords.put("pdt", "pdt123");
     *   // Tài khoản admin: username="pdt", password="pdt123"
     * 
     * Lưu ý: Trong hệ thống thực tế, mật khẩu nên được hash (băm) trước khi lưu,
     * không nên lưu plain text như trong code này.
     */
    public static final Map<String, String> adminPasswords = new HashMap<>();
    
    /**
     * Danh sách sinh viên tra cứu theo MSSV: Map<MSSV, Student>
     * 
     * Đây là cấu trúc dữ liệu chính để lưu trữ thông tin sinh viên.
     * Key là MSSV (mã số sinh viên), Value là đối tượng Student.
     * 
     * Ví dụ:
     *   studentsById.put("SV001", student);
     *   Student s = studentsById.get("SV001");  // Lấy sinh viên có MSSV = "SV001"
     */
    public static final Map<String, Student> studentsById = new HashMap<>();
    
    /**
     * Index email để tra cứu sinh viên theo email: Map<email_lowercase, MSSV>
     * 
     * Hỗ trợ đăng nhập bằng email (ngoài MSSV).
     * Email được chuyển về lowercase để không phân biệt hoa/thường.
     * 
     * Cấu trúc: email (lowercase) -> MSSV
     * 
     * Ví dụ:
     *   emailIndex.put("sv001@university.edu", "SV001");
     *   String mssv = emailIndex.get("sv001@university.edu");  // Trả về "SV001"
     *   Student s = studentsById.get(mssv);  // Lấy sinh viên từ MSSV
     * 
     * Được dùng để:
     * - Kiểm tra email đã được sử dụng chưa (khi tạo tài khoản mới)
     * - Tìm sinh viên theo email (khi đăng nhập bằng email)
     */
    public static final Map<String, String> emailIndex = new HashMap<>();
    
    /**
     * Danh sách học phần master (không phụ thuộc học kỳ): Map<courseCode, Course>
     * 
     * Đây là danh sách tất cả học phần có trong hệ thống, không phụ thuộc vào học kỳ.
     * Mỗi học phần chỉ được định nghĩa một lần, sau đó có thể được mở lớp ở nhiều học kỳ khác nhau.
     * 
     * Dùng LinkedHashMap để giữ thứ tự thêm vào (khi duyệt sẽ theo thứ tự đã thêm).
     * 
     * Ví dụ:
     *   courses.put("CT101", new Course("CT101", "Lập trình cơ bản", 3));
     *   Course c = courses.get("CT101");  // Lấy học phần có mã "CT101"
     * 
     * Lưu ý: Để mở lớp cho một học phần trong một học kỳ cụ thể,
     * cần tạo Offering trong Map offerings (không phải trong courses này).
     */
    public static final Map<String, Course> courses = new LinkedHashMap<>();
    
    /**
     * Danh sách học kỳ: List<String>
     * 
     * Lưu trữ danh sách các học kỳ trong hệ thống.
     * Mỗi học kỳ được biểu diễn bằng một chuỗi (ví dụ: "20252", "20251", "20242").
     * 
     * Quy ước đặt tên học kỳ (ví dụ: "20252"):
     * - 4 chữ số đầu: Năm (2025)
     * - Chữ số cuối: Số thứ tự học kỳ trong năm (2 = học kỳ 2)
     * 
     * Ví dụ:
     *   terms.add("20252");  // Học kỳ 2 năm 2025
     *   terms.add("20251");  // Học kỳ 1 năm 2025
     */
    public static final List<String> terms = new ArrayList<>();
    
    /**
     * Danh sách chương trình đào tạo (CTĐT): List<String>
     * 
     * Lưu trữ danh sách các chương trình đào tạo có trong hệ thống.
     * 
     * Ví dụ:
     *   programs.add("Kỹ thuật Điện tử - Viễn thông 2021");
     *   programs.add("Công nghệ Thông tin 2021");
     *   programs.add("Kỹ thuật Cơ khí 2021");
     * 
     * Được dùng để:
     * - Hiển thị trong dropdown khi tạo tài khoản sinh viên mới
     * - Lọc danh sách sinh viên theo khoa/viện trong màn hình duyệt đăng ký
     * - Kiểm tra quyền đăng ký học phần (một số học phần chỉ dành cho CTĐT cụ thể)
     */
    public static final List<String> programs = new ArrayList<>();

    /**
     * Đăng ký học phần của sinh viên: Map<MSSV, Map<Học kỳ, List<RegItem>>>
     * 
     * Cấu trúc dữ liệu 3 cấp:
     * - Cấp 1: MSSV (mã số sinh viên)
     * - Cấp 2: Học kỳ (ví dụ: "20252")
     * - Cấp 3: Danh sách RegItem (các học phần đã đăng ký trong học kỳ đó)
     * 
     * Ví dụ:
     *   regs = {
     *     "SV001": {                    // Sinh viên có MSSV = "SV001"
     *       "20252": [                  // Học kỳ "20252"
     *         RegItem(course=CT101, date="2025-01-15", status="Đã duyệt"),
     *         RegItem(course=MA101, date="2025-01-15", status="Đã gửi")
     *       ],
     *       "20251": [                  // Học kỳ "20251"
     *         RegItem(course=CT102, date="2024-09-01", status="Đã duyệt")
     *       ]
     *     }
     *   }
     * 
     * Mỗi RegItem đại diện cho một học phần mà sinh viên đã đăng ký,
     * chứa thông tin: học phần, ngày đăng ký, trạng thái đăng ký.
     */
    public static final Map<String, Map<String, List<RegItem>>> regs = new HashMap<>();

    /**
     * Cấu hình học kỳ: Map<Học kỳ, TermSetting>
     * 
     * Lưu trữ cấu hình cho từng học kỳ, chủ yếu là trạng thái mở/đóng đăng ký.
     * 
     * Ví dụ:
     *   termSettings = {
     *     "20252": TermSetting(registrationOpen=true, ...),   // Học kỳ 20252 đang mở đăng ký
     *     "20251": TermSetting(registrationOpen=false, ...)    // Học kỳ 20251 đã đóng đăng ký
     *   }
     * 
     * Khi registrationOpen = true:
     * - Sinh viên có thể đăng ký học phần mới trong học kỳ này
     * 
     * Khi registrationOpen = false:
     * - Sinh viên không thể đăng ký học phần mới
     * - Các đăng ký đã gửi vẫn có thể được duyệt/từ chối bởi admin
     */
    public static final Map<String, TermSetting> termSettings = new HashMap<>();

    /**
     * Cấu hình mở lớp (Offering) theo học kỳ: Map<Học kỳ, Map<Mã học phần, Offering>>
     * 
     * Cấu trúc dữ liệu 2 cấp:
     * - Cấp 1: Học kỳ (ví dụ: "20252")
     * - Cấp 2: Mã học phần -> Offering (cấu hình mở lớp cho học phần đó)
     * 
     * Mỗi Offering chứa:
     * - open: Trạng thái mở/đóng lớp (true = mở, false = đóng)
     * - allowedProgram: Chương trình đào tạo được phép đăng ký ("Tất cả" hoặc tên CTĐT cụ thể)
     * 
     * Ví dụ:
     *   offerings = {
     *     "20252": {                    // Học kỳ "20252"
     *       "CT101": Offering(open=true, allowedProgram="Tất cả"),      // Mở cho tất cả
     *       "ET4010": Offering(open=true, allowedProgram="Kỹ thuật Điện tử - Viễn thông 2021")  // Chỉ cho CTĐT cụ thể
     *     }
     *   }
     * 
     * Lưu ý: Một học phần có thể có Offering ở nhiều học kỳ khác nhau,
     * mỗi học kỳ có cấu hình riêng (có thể mở ở kỳ này nhưng đóng ở kỳ khác).
     */
    public static final Map<String, Map<String, Offering>> offerings = new HashMap<>();

    /**
     * Hàm khởi tạo dữ liệu demo ban đầu cho hệ thống.
     * Được gọi một lần khi chương trình khởi động.
     */
    public static void init() {
        // Tài khoản PĐT mặc định: username=pdt, password=pdt123
        adminPasswords.put("pdt", "pdt123");

        // Thêm các chương trình học (CTĐT) demo
        programs.addAll(Arrays.asList(
                "Kỹ thuật Điện tử - Viễn thông 2021",
                "Công nghệ Thông tin 2021",
                "Kỹ thuật Cơ khí 2021"
        ));

        // Danh sách học kỳ demo
        terms.addAll(Arrays.asList("20252","20251","20242"));
        
        // Tạo TermSetting với thông tin chi tiết cho từng học kỳ
        // Học kỳ 20252: Học kỳ 2 Năm học 2025, 2025-2026, 08/01/2026 đến 08/04/2026, Đang hoạt động
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.JANUARY, 8, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start20252 = cal.getTime();
        cal.set(2026, Calendar.APRIL, 8, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date end20252 = cal.getTime();
        termSettings.put("20252", new TermSetting(
            true, 
            "Học kỳ 2 Năm học 2025", 
            "2025-2026", 
            start20252, 
            end20252
        ));
        
        // Học kỳ 20251: Học kỳ 1 Năm học 2025, 2025-2026, 01/09/2025 đến 01/01/2026, Đã kết thúc
        cal.set(2025, Calendar.SEPTEMBER, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start20251 = cal.getTime();
        cal.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date end20251 = cal.getTime();
        termSettings.put("20251", new TermSetting(
            false, 
            "Học kỳ 1 Năm học 2025", 
            "2025-2026", 
            start20251, 
            end20251
        ));
        
        // Học kỳ 20242: Học kỳ 2 Năm học 2024, 2024-2025, 01/02/2024 đến 01/06/2024, Đã kết thúc
        cal.set(2024, Calendar.FEBRUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start20242 = cal.getTime();
        cal.set(2024, Calendar.JUNE, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date end20242 = cal.getTime();
        termSettings.put("20242", new TermSetting(
            false, 
            "Học kỳ 2 Năm học 2024", 
            "2024-2025", 
            start20242, 
            end20242
        ));

        // Thêm các học phần (Course) vào danh sách courses
        // Cơ sở và Tự chọn
        addCourse(new Course("CT101","Lập trình cơ bản",3));
        addCourse(new Course("CT102","Cấu trúc dữ liệu",3));
        addCourse(new Course("EE201","Mạch điện 1",3));
        addCourse(new Course("MA101","Giải tích 1",4));
        addCourse(new Course("PE101","Giáo dục thể chất",2));
        
        // GDTC - Tự chọn
        addCourse(new Course("PE2101","Bóng chuyền 1",3));
        addCourse(new Course("PE2151","Erobic",3));
        addCourse(new Course("PE2201","Bóng đá 1",3));
        addCourse(new Course("PE2251","Taekwondo 1",3));
        addCourse(new Course("PE2301","Bóng rổ 1",3));
        addCourse(new Course("PE2401","Bóng bàn 1",3));
        addCourse(new Course("PE2501","Cầu lông 1",3));
        addCourse(new Course("PE2601","Chạy",0));
        addCourse(new Course("PE2701","Nhảy cao",0));
        addCourse(new Course("PE2801","Nhảy xa",0));
        addCourse(new Course("PE2901","Xà kép, xà lệch",0));
        
        // GDTC - Chuyên sâu
        addCourse(new Course("PE3101","Chuyên sâu Bóng chuyền 1",1));
        addCourse(new Course("PE3102","Chuyên sâu Bóng chuyền 2",2));
        addCourse(new Course("PE3103","Chuyên sâu Bóng chuyền 3",3));
        addCourse(new Course("PE3201","Chuyên sâu Bóng đá 1",1));
        addCourse(new Course("PE3202","Chuyên sâu Bóng đá 2",2));
        addCourse(new Course("PE3203","Chuyên sâu Bóng đá 3",3));
        addCourse(new Course("PE3301","Chuyên sâu Bóng rổ 1",1));
        addCourse(new Course("PE3302","Chuyên sâu Bóng rổ 2",2));
        addCourse(new Course("PE3303","Chuyên sâu Bóng rổ 3",3));
        
        // GDTC - Tự chọn (cấp độ 2)
        addCourse(new Course("PE2102","Bóng chuyền 2",4));
        addCourse(new Course("PE2202","Bóng đá 2",4));
        addCourse(new Course("PE2252","Taekwondo 2",4));
        addCourse(new Course("PE2302","Bóng rổ 2",4));
        addCourse(new Course("PE2402","Bóng bàn 2",4));
        addCourse(new Course("PE2502","Cầu lông 2",4));
        addCourse(new Course("PE3104","Chuyên sâu Bóng chuyền 4",4));
        addCourse(new Course("PE3204","Chuyên sâu Bóng đá 4",4));
        addCourse(new Course("PE3304","Chuyên sâu Bóng rổ 4",4));
        
        // GDTC - Tự chọn (cấp độ 3)
        addCourse(new Course("PE2103","Bóng chuyền 3",5));
        addCourse(new Course("PE2203","Bóng đá 3",5));
        addCourse(new Course("PE2253","Taekwondo 3",5));
        addCourse(new Course("PE2303","Bóng rổ 3",5));
        addCourse(new Course("PE2403","Bóng bàn 3",5));
        addCourse(new Course("PE2503","Cầu lông 3",5));
        addCourse(new Course("PE3105","Chuyên sâu Bóng chuyền 5",5));
        addCourse(new Course("PE3205","Chuyên sâu Bóng đá 5",5));
        addCourse(new Course("PE3305","Chuyên sâu Bóng rổ 5",5));
        
        // GDTC khác
        addCourse(new Course("PE1014","Lý luận TDTT",1));
        addCourse(new Course("PE1024","Bơi lội",2));
        
        // QP-AN
        addCourse(new Course("MIL1210","Đường lối quốc phòng và an ninh của ĐCSVN",1));
        addCourse(new Course("MIL1220","Công tác quốc phòng và an ninh",2));
        addCourse(new Course("MIL1230","Quân sự chung",2));
        addCourse(new Course("MIL1240","Kỹ thuật chiến đấu bộ binh và chiến thuật",2));
        
        // Ngoại ngữ
        addCourse(new Course("FL1128","Tiếng Anh tăng cường",1));
        addCourse(new Course("FL1129","Tiếng Anh cơ sở 1",1));
        addCourse(new Course("FL1130","Tiếng Anh cơ sở 2",2));
        
        // Lý luận chính trị
        addCourse(new Course("SSH1111","Triết học Mác - Lênin",1));
        addCourse(new Course("SSH1121","Kinh tế chính trị Mác - Lênin",2));
        addCourse(new Course("SSH1131","Chủ nghĩa xã hội khoa học",2));
        addCourse(new Course("SSH1141","Lịch sử Đảng cộng sản Việt Nam",2));
        addCourse(new Course("SSH1151","Tư tưởng Hồ Chí Minh",2));
        addCourse(new Course("EM1170","Pháp luật đại cương",2));
        
        // Toán-KHCB
        addCourse(new Course("MI1111","Giải tích I",4));
        addCourse(new Course("MI1121","Giải tích II",3));
        addCourse(new Course("MI1131","Giải tích III",3));
        addCourse(new Course("MI1141","Đại số",4));
        addCourse(new Course("MI2010","Phương pháp tính",2));
        addCourse(new Course("MI2020","Xác suất thống kê",3));
        addCourse(new Course("PH1111","Vật lý đại cương I",2));
        addCourse(new Course("PH1122","Vật lý đại cương II",4));
        addCourse(new Course("PH3330","Vật lý điện tử",3));
        addCourse(new Course("IT1110","Tin học đại cương",4));
        
        // Cơ sở ngành
        addCourse(new Course("ET2000","Nhập môn kỹ thuật điện tử-viễn thông",2));
        addCourse(new Course("ET2021","Thực tập cơ bản",2));
        addCourse(new Course("ET2031","Kỹ thuật lập trình C/C++",2));
        addCourse(new Course("ET2040","Cấu kiện điện tử",3));
        addCourse(new Course("ET2050","Lý thuyết mạch",3));
        addCourse(new Course("ET2060","Tín hiệu và hệ thống",3));
        addCourse(new Course("ET2072","Lý thuyết thông tin",2));
        addCourse(new Course("ET2080","Cơ sở kỹ thuật đo lường",2));
        addCourse(new Course("ET2100","Cấu trúc dữ liệu và giải thuật",2));
        addCourse(new Course("ET3210","Trường điện từ",3));
        addCourse(new Course("ET3220","Điện tử số",3));
        addCourse(new Course("ET3230","Điện tử tương tự I",3));
        addCourse(new Course("ET3241","Điện tử tương tự II",2));
        addCourse(new Course("ET3250","Thông tin số",3));
        addCourse(new Course("ET3260","Kỹ thuật phần mềm ứng dụng",2));
        addCourse(new Course("ET3270","Thực tập kỹ thuật",2));
        addCourse(new Course("ET3280","Anten và truyền sóng",2));
        addCourse(new Course("ET3290","Đồ án thiết kế I",2));
        addCourse(new Course("ET3300","Kỹ thuật vi xử lý",3));
        addCourse(new Course("ET4010","Đồ án thiết kế II",2));
        addCourse(new Course("ET4020","Xử lý tín hiệu số",3));
        addCourse(new Course("ET2022","Technical Writing and Presentation",3));
        
        // Bổ trợ
        addCourse(new Course("EM1010","Quản trị học đại cương",2));
        addCourse(new Course("EM1180","Văn hóa kinh doanh và tinh thần khởi nghiệp",2));
        addCourse(new Course("ED3280","Tâm lý học ứng dụng",2));
        addCourse(new Course("ED3220","Kỹ năng mềm",2));
        addCourse(new Course("ET3262","Tư duy công nghệ và thiết kế kỹ thuật",2));
        addCourse(new Course("CH2021","Đổi mới sáng tạo và khởi nghiệp",2));
        addCourse(new Course("ME3123","Thiết kế mỹ thuật công nghiệp",2));
        addCourse(new Course("ME3124","Thiết kế quảng bá sản phẩm",2));
        
        // Mô đun
        addCourse(new Course("ET3310","Lý thuyết mật mã",3));
        addCourse(new Course("ET4230","Mạng máy tính",3));
        addCourse(new Course("ET4250","Hệ thống viễn thông",3));
        addCourse(new Course("ET4070","Cơ sở truyền số liệu",3));
        addCourse(new Course("ET4291","Hệ điều hành",3));
        addCourse(new Course("ET3180","Thông tin vô tuyến",3));
        
        // Mô đun - Y sinh
        addCourse(new Course("ET4100","Cơ sở điện sinh học",2));
        addCourse(new Course("ET4471","Mạch xử lý tín hiệu y sinh",3));
        addCourse(new Course("ET4450","Giải phẫu và sinh lý học",2));
        addCourse(new Course("ET4110","Cảm biến và KT đo lường y sinh",3));
        addCourse(new Course("ET4480","Công nghệ chẩn đoán hình ảnh I",3));
        addCourse(new Course("ET4120","Thiết bị điện tử y sinh I",2));
        
        // Mô đun - Hàng không/Vũ trụ
        addCourse(new Course("ET4130","Truyền số liệu và chuyển tiếp điện văn",3));
        addCourse(new Course("ET4140","Định vị và dẫn đường điện tử",3));
        
        // Mô đun - Đa phương tiện
        addCourse(new Course("ET4260","Đa phương tiện",2));
        addCourse(new Course("ET4370","Kỹ thuật truyền hình",2));
        
        // Mô đun - Vi mạch
        addCourse(new Course("ET4358","Cơ sở công nghệ vi điện tử",3));
        addCourse(new Course("ET4340","Thiết kế VLSI",3));
        addCourse(new Course("ET4033","Thiết kế IC tương tự",3));
        addCourse(new Course("ET4356","Kiểm chứng và kiểm tra vi mạch",3));
        addCourse(new Course("ET4361","Hệ thống nhúng và thiết kế giao tiếp nhúng",3));
        
        // Đồ án nghiên cứu
        addCourse(new Course("ET4920","Đồ án nghiên cứu Cử nhân (KT Điện tử - Viễn thông)",8));

        // Mở offering cho học kỳ 20252 – chỉ mở các học phần được chỉ định
        String term20252 = "20252";
        // Danh sách các học phần được mở trong học kỳ 20252
        String[] openCourses20252 = {
            "CT101", "CT102", "EE201", "MA101", 
            "PE101", "PE2101", "PE2151", "PE2201"
        };
        for (String code : openCourses20252) {
            setOffering(term20252, code, true, "Tất cả");
        }

        // Tạo một sinh viên demo mặc định
        Student demo = new Student(
                "SV001","Sinh Viên Mặc Định","2004-01-01","Hà Nội",
                "sv001@university.edu","Kỹ thuật Điện tử - Viễn thông 2021"
        );
        // Thêm sinh viên demo vào hệ thống, mật khẩu là "sv123"
        addStudent(demo, "sv123");
    }

    /* ---------- helpers data ---------- */

    /** Thêm một môn học mới vào danh sách courses */
    public static void addCourse(Course c){
        courses.put(c.code,c); // key là mã học phần
    }

    /** Kiểm tra tài khoản PĐT: đúng user và password hay không */
    public static boolean verifyAdmin(String u,String p){
        return p.equals(adminPasswords.get(u));
    }

    /** Kiểm tra đăng nhập sinh viên: đúng MSSV và password hay không */
    public static boolean verifyStudent(String id,String p){
        Student s = studentsById.get(id);            // tìm sinh viên theo MSSV
        return s != null && Objects.equals(s.password,p); // so sánh mật khẩu
    }

    /**
     * Thêm sinh viên mới vào hệ thống.
     * Có kiểm tra trùng MSSV, email trống, CTĐT trống, email đã dùng chưa.
     */
    public static void addStudent(Student s,String pass){
        // Mỗi MSSV chỉ được có 1 tài khoản
        if(studentsById.containsKey(s.studentId))
            throw new RuntimeException("Mỗi MSSV chỉ có 1 tài khoản!");

        // Bắt buộc phải có email
        if(s.email == null || s.email.isBlank())
            throw new RuntimeException("Email không được để trống!");

        // Bắt buộc phải chọn chương trình học
        if(s.program == null || s.program.isBlank())
            throw new RuntimeException("Vui lòng chọn chương trình học!");

        // Dùng email lowercase làm key để tránh phân biệt hoa/thường
        String key = s.email.toLowerCase(Locale.ROOT);

        // Kiểm tra email đã được sử dụng bởi MSSV khác chưa
        if(emailIndex.containsKey(key))
            throw new RuntimeException("Email đã được dùng bởi MSSV: " + emailIndex.get(key));

        // Nếu mọi thứ hợp lệ, gán mật khẩu và lưu vào 2 map
        s.password = pass;
        studentsById.put(s.studentId, s); // lưu student
        emailIndex.put(key, s.studentId); // index email -> MSSV
    }

    /** Trả về bản copy danh sách học kỳ (để UI dùng mà không sửa list gốc) */
    public static List<String> loadTerms(){
        return new ArrayList<>(terms);
    }

    /**
     * Lấy danh sách đăng ký của một sinh viên trong một học kỳ.
     * Nếu chưa có, sẽ tự tạo list trống rồi trả về.
     */
    public static List<RegItem> loadReg(String sid,String term){
        // computeIfAbsent: nếu chưa có key thì tạo mới
        return regs
                .computeIfAbsent(sid, k -> new HashMap<>())      // map term -> list
                .computeIfAbsent(term, k -> new ArrayList<>());  // list RegItem cho term
    }

    /**
     * Thêm một RegItem (môn đăng ký) cho sinh viên trong học kỳ.
     * Trả về true nếu thêm được, false nếu đã tồn tại môn đó (tránh trùng môn).
     */
    public static boolean addReg(String sid,String term,RegItem item){
        var list = loadReg(sid,term); // lấy (hoặc tạo) list đăng ký hiện tại
        // Không cho đăng ký trùng cùng một course code
        if(list.stream().anyMatch(x -> x.course.code.equals(item.course.code)))
            return false;
        list.add(item);
        return true;
    }

    /**
     * Xóa các RegItem theo tập mã học phần (codes)
     * cho 1 sinh viên trong 1 học kỳ.
     */
    public static void deleteByCourseCodes(String sid,String term,Set<String> codes){
        var list = loadReg(sid,term);
        // removeIf: xóa các phần tử có course.code nằm trong tập codes
        list.removeIf(it -> codes.contains(it.course.code));
    }

    /* ---------- term setting ---------- */

    /** Kiểm tra học kỳ có đang mở đăng ký hay không 
     * 
     * Học kỳ được coi là mở đăng ký khi:
     * - registrationOpen = true
     * - VÀ ngày hiện tại >= startDate (nếu startDate không null)
     * - VÀ ngày hiện tại <= endDate (nếu endDate không null)
     */
    public static boolean isTermOpen(String term){
        TermSetting setting = termSettings.get(term);
        if (setting == null) {
            return false; // Nếu không có TermSetting thì đóng
        }
        
        // Kiểm tra flag registrationOpen
        if (!setting.registrationOpen) {
            return false;
        }
        
        // Kiểm tra thời gian mở đăng ký
        Date now = new Date();
        
        // Nếu có startDate, ngày hiện tại phải >= startDate
        if (setting.startDate != null && now.before(setting.startDate)) {
            return false; // Chưa đến thời gian mở đăng ký
        }
        
        // Nếu có endDate, ngày hiện tại phải <= endDate
        if (setting.endDate != null && now.after(setting.endDate)) {
            return false; // Đã qua thời gian mở đăng ký
        }
        
        return true; // Tất cả điều kiện đều thỏa mãn
    }
    
    /** Lấy thông báo lỗi chi tiết khi học kỳ không mở đăng ký */
    public static String getTermOpenErrorMessage(String term) {
        TermSetting setting = termSettings.get(term);
        if (setting == null) {
            return "Học kỳ chưa được cấu hình.";
        }
        
        if (!setting.registrationOpen) {
            return "Học kỳ đang khóa đăng ký.";
        }
        
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        if (setting.startDate != null && now.before(setting.startDate)) {
            return "Chưa đến thời gian mở đăng ký. Thời gian mở đăng ký bắt đầu từ: " + sdf.format(setting.startDate);
        }
        
        if (setting.endDate != null && now.after(setting.endDate)) {
            return "Đã qua thời gian mở đăng ký. Thời gian mở đăng ký kết thúc vào: " + sdf.format(setting.endDate);
        }
        
        return "Học kỳ đang khóa đăng ký.";
    }

    /** Đặt trạng thái mở/đóng cho một học kỳ */
    public static void setTermOpen(String term, boolean open){
        // Nếu term chưa có TermSetting thì tạo mới; sau đó cập nhật registrationOpen
        termSettings
                .computeIfAbsent(term, t -> new TermSetting(open))
                .registrationOpen = open;
    }

    /* ---------- offerings per term ---------- */

    /**
     * Lấy Offering (cấu hình mở lớp) cho một học phần trong một học kỳ.
     * Nếu term chưa có map offerings thì tạo map rỗng.
     */
    public static Offering getOffering(String term, String code){
        return offerings
                .computeIfAbsent(term, t -> new HashMap<>())
                .get(code);
    }

    /**
     * Thiết lập Offering cho một học phần trong một học kỳ.
     * Nếu chưa có map cho term thì tạo mới rồi put vào.
     */
    public static void setOffering(String term, String code, boolean open, String allowedProgram){
        offerings
                .computeIfAbsent(term, t -> new HashMap<>())
                .put(code, new Offering(open, allowedProgram));
    }

    /* ---------- Thống kê & xóa học phần ---------- */

    /**
     * Đếm số lượng đăng ký của một môn (courseCode) trong một học kỳ.
     * Duyệt qua tất cả sinh viên, đếm các RegItem trùng courseCode.
     */
    public static int countRegByCourse(String term, String courseCode){
        int cnt = 0;
        // byTerm: map term -> list RegItem của 1 sinh viên
        for (var byTerm : regs.values()) {
            var list = byTerm.get(term); // lấy list RegItem của học kỳ cần đếm
            if (list == null) continue;  // sinh viên này chưa đăng ký kỳ đó
            // Tăng biến đếm nếu RegItem có course.code khớp courseCode
            for (RegItem it : list)
                if (it.course.code.equals(courseCode)) cnt++;
        }
        return cnt;
    }

    /**
     * Kiểm tra một môn có thể xóa được không.
     * Không cho xóa nếu bất kỳ sinh viên nào đã đăng ký môn đó ở bất kỳ kỳ nào.
     */
    public static boolean canDeleteCourse(String courseCode){
        // byStudent: map term -> list RegItem cho từng sinh viên
        for (var byStudent : regs.values()) {
            for (var list : byStudent.values()) {
                for (RegItem it : list)
                    if (it.course.code.equals(courseCode)) return false; // có người đăng ký rồi
            }
        }
        return true; // không ai đăng ký, có thể xóa
    }

    /**
     * Xóa môn học khỏi hệ thống:
     *  - Xóa khỏi danh sách courses.
     *  - Xóa khỏi offerings của tất cả học kỳ.
     *  (KHÔNG đụng đến regs, nên cần đảm bảo canDeleteCourse trước khi gọi)
     */
    public static void deleteCourse(String courseCode){
        courses.remove(courseCode);         // xóa trong danh sách môn
        for (var m : offerings.values())    // m: map courseCode -> Offering
            m.remove(courseCode);           // xóa offering của môn đó trong từng kỳ
    }

    /** Trả về ngày hiện tại dạng chuỗi "yyyy-MM-dd" (ví dụ: 2025-11-19) */
    public static String today(){
        return new SimpleDateFormat("yyyy-MM-dd")
                .format(new Date());
    }
}
