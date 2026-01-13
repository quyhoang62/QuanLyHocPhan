package university.registration.ui;

import university.registration.model.Course;
import university.registration.model.RegItem;
import university.registration.model.Student;
import university.registration.store.Memory;
import university.registration.ui.components.CardPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Màn hình đăng ký học phần dành cho Sinh viên
 * 
 * Đây là màn hình chính mà sinh viên sử dụng để:
 * - Xem danh sách học phần có thể đăng ký trong học kỳ
 * - Thêm học phần vào giỏ đăng ký
 * - Xem tổng số tín chỉ đã đăng ký (tối thiểu 12 TC, tối đa 24 TC)
 * - Gửi đăng ký để PĐT duyệt
 * - Xem lịch sử đăng ký các học kỳ trước
 * 
 * Cấu trúc màn hình:
 * - Header: Tiêu đề + thông tin sinh viên + nút đăng xuất
 * - TabbedPane với 2 tab:
 *   1. Tab "Đăng ký học phần": 
 *      - Bên trái: Danh sách học phần có thể đăng ký (có pagination)
 *      - Bên phải: Giỏ đăng ký + tổng số tín chỉ + nút gửi đăng ký
 *   2. Tab "Lịch sử đăng ký": Bảng hiển thị tất cả đăng ký của sinh viên qua các học kỳ
 * 
 * Tính năng chính:
 * - Tìm kiếm học phần theo mã hoặc tên
 * - Lọc học phần theo học kỳ
 * - Phân trang danh sách học phần (5 môn/trang)
 * - Kiểm tra giới hạn tín chỉ (12-24 TC)
 * - Hiển thị trạng thái đăng ký (Tạm, Đã gửi, Đã duyệt, Đã từ chối)
 * - Tự động cập nhật UI khi thêm/xóa học phần
 */
public class StudentRegistrationFrame extends JFrame {

    /**
     * Sinh viên đang đăng nhập và sử dụng màn hình này
     * 
     * Thuộc tính final: không thể thay đổi sau khi khởi tạo.
     * Đảm bảo màn hình luôn gắn với một sinh viên cụ thể.
     */
    final Student student;

    /**
     * ComboBox để chọn học kỳ đăng ký
     * 
     * Sinh viên chọn học kỳ từ danh sách các học kỳ có trong hệ thống.
     * Khi thay đổi học kỳ, danh sách học phần và giỏ đăng ký sẽ được refresh.
     */
    JComboBox<String> cbTerm = new JComboBox<>();
    
    /**
     * TextField để tìm kiếm học phần
     * 
     * Sinh viên có thể nhập mã học phần hoặc tên học phần để tìm kiếm.
     * Kết quả tìm kiếm được cập nhật real-time khi gõ (sử dụng DocumentListener).
     */
    JTextField searchField = new JTextField();
    
    /**
     * Bảng hiển thị danh sách học phần có thể đăng ký
     * 
     * Các cột: Mã HP, Tên học phần, TC (tín chỉ), Loại, Hành động
     * Cột "Hành động" chứa nút "THÊM" hoặc "ĐÃ CHỌN" để thêm học phần vào giỏ.
     */
    JTable courseTable;
    
    /**
     * Model dữ liệu cho bảng học phần (courseTable)
     * 
     * Chứa dữ liệu hiển thị trong bảng, được cập nhật khi:
     * - Thay đổi học kỳ
     * - Tìm kiếm học phần
     * - Chuyển trang (pagination)
     */
    DefaultTableModel courseModel;
    
    /**
     * Panel chứa danh sách học phần đã thêm vào giỏ đăng ký
     * 
     * Mỗi học phần trong giỏ được hiển thị dưới dạng một item với:
     * - Mã HP, tên học phần, số tín chỉ
     * - Trạng thái đăng ký (nếu đã gửi)
     * - Nút xóa (nếu chưa gửi hoặc trạng thái là "Tạm")
     */
    JPanel cartPanel;
    
    /**
     * Label hiển thị tổng số tín chỉ (không dùng nữa, thay bằng lbCreditRange)
     */
    JLabel lbTotalCredits = new JLabel("0");
    
    /**
     * Label hiển thị tổng số tín chỉ đã đăng ký / 24 TC
     * 
     * Ví dụ: "15/24 TC" nghĩa là đã đăng ký 15 tín chỉ trên tổng tối đa 24 tín chỉ.
     */
    JLabel lbCreditRange = new JLabel("0/24 TC");
    
    /**
     * Progress bar hiển thị tiến độ đăng ký tín chỉ
     * 
     * Range: 0-24 (tối đa 24 tín chỉ)
     * Màu xanh khi trong khoảng 12-24 TC, cảnh báo khi < 12 hoặc > 24 TC.
     */
    JProgressBar creditProgressBar = new JProgressBar(0, 24);
    
    /**
     * Label hiển thị cảnh báo về số tín chỉ
     * 
     * Hiển thị cảnh báo khi:
     * - Tổng tín chỉ < 12: "Cần đăng ký tối thiểu 12 tín chỉ để hoàn tất."
     * - Tổng tín chỉ > 24: "Tổng số tín chỉ vượt quá 24. Vui lòng xóa bớt học phần."
     */
    JLabel lbWarning = new JLabel();
    
    /**
     * Nút "Gửi đăng ký" - gửi tất cả học phần trong giỏ để PĐT duyệt
     * 
     * Khi nhấn, tất cả học phần trong giỏ sẽ được chuyển sang trạng thái "Đã gửi".
     */
    JButton btnSubmit = new JButton("Gửi Đăng Ký");
    
    /**
     * Nút "Xóa hết" - xóa tất cả học phần khỏi giỏ đăng ký
     * 
     * Có xác nhận trước khi xóa để tránh xóa nhầm.
     */
    JButton btnClearAll = new JButton("XÓA HẾT");
    
    /**
     * Set chứa mã các học phần đã được thêm vào giỏ đăng ký
     * 
     * Dùng Set để đảm bảo không trùng lặp (mỗi học phần chỉ có thể thêm 1 lần).
     * Khi sinh viên thêm học phần vào giỏ, mã học phần được thêm vào Set này.
     * Khi xóa khỏi giỏ, mã học phần được xóa khỏi Set.
     */
    Set<String> selectedCourseCodes = new HashSet<>();
    
    /**
     * TabbedPane để chuyển đổi giữa 2 tab: "Đăng ký học phần" và "Lịch sử đăng ký"
     * 
     * Tab 1: Đăng ký học phần mới
     * Tab 2: Xem lịch sử đăng ký các học kỳ trước
     */
    JTabbedPane tabbedPane;
    
    /**
     * Model dữ liệu cho bảng lịch sử đăng ký
     * 
     * Chứa tất cả đăng ký của sinh viên qua các học kỳ.
     * Các cột: Học kỳ, Mã HP, Tên học phần, Số TC, Ngày đăng ký, Trạng thái
     */
    DefaultTableModel historyModel;
    
    /**
     * Bảng hiển thị lịch sử đăng ký học phần
     * 
     * Hiển thị tất cả học phần mà sinh viên đã đăng ký ở tất cả các học kỳ,
     * kèm theo trạng thái (Tạm, Đã gửi, Đã duyệt, Đã từ chối).
     */
    JTable historyTable;
    
    /**
     * Trang hiện tại trong pagination (bắt đầu từ 1)
     * 
     * Dùng để phân trang danh sách học phần (5 môn/trang).
     */
    int currentPage = 1;
    
    /**
     * Số lượng học phần hiển thị trên mỗi trang
     * 
     * Mặc định: 5 học phần/trang để danh sách không quá dài.
     */
    int pageSize = 5;
    
    /**
     * Label hiển thị thông tin pagination
     * 
     * Ví dụ: "Hiển thị 1 đến 5 trên 25 kết quả"
     */
    JLabel lbPaginationInfo = new JLabel();
    
    /**
     * Nút "Trang trước" và "Trang sau" trong pagination
     */
    JButton btnPrevPage, btnNextPage;
    
    /**
     * Panel chứa pagination info và các nút pagination
     */
    JPanel paginationPanel, paginationButtons;
    
    /**
     * Helper method: Xác định loại học phần dựa trên mã học phần
     * 
     * Phân loại học phần theo quy tắc đặt mã:
     * - PE*: Giáo dục thể chất (GDTC)
     * - MIL*: Quốc phòng An ninh (QP-AN)
     * - FL*: Ngoại ngữ
     * - SSH*: Lý luận chính trị
     * - EM1170: Pháp luật
     * - MI*, PH*, IT1110: Toán-KHCB
     * - ET2*, ET3*, ET4*: Cơ sở ngành (có thể là mô đun chuyên ngành)
     * - EM101*, EM118*, ED*, ET3262, CH202, ME312*: Bổ trợ
     * - CT*: Tự chọn
     * - MA*: Cơ sở
     * 
     * @param code Mã học phần (ví dụ: "CT101", "PE2101", "ET4010")
     * @return Loại học phần (ví dụ: "Tự chọn", "GDTC - Tự chọn", "Cơ sở ngành")
     */
    private String getCourseType(String code) {
        if (code == null || code.isEmpty()) return "Tự chọn";
        
        // GDTC - Giáo dục thể chất
        if (code.startsWith("PE")) {
            if (code.startsWith("PE31") || code.startsWith("PE32") || code.startsWith("PE33")) {
                return "GDTC - Chuyên sâu";
            }
            if (code.startsWith("PE21") || code.startsWith("PE22") || code.startsWith("PE23") || 
                code.startsWith("PE24") || code.startsWith("PE25") || code.startsWith("PE26") ||
                code.startsWith("PE27") || code.startsWith("PE28") || code.startsWith("PE29")) {
                return "GDTC - Tự chọn";
            }
            if (code.startsWith("PE10")) {
                return "GDTC";
            }
            return "Bắt buộc";
        }
        
        // QP-AN - Quốc phòng An ninh
        if (code.startsWith("MIL")) return "QP-AN";
        
        // Ngoại ngữ
        if (code.startsWith("FL")) return "Ngoại ngữ";
        
        // Lý luận chính trị
        if (code.startsWith("SSH")) return "Lý luận chính trị";
        
        // Pháp luật
        if (code.startsWith("EM1170")) return "Pháp luật";
        
        // Toán-KHCB
        if (code.startsWith("MI") || code.startsWith("PH") || code.startsWith("IT1110")) {
            return "Toán-KHCB";
        }
        
        // Cơ sở ngành
        if (code.startsWith("ET2") || code.startsWith("ET3") || code.startsWith("ET4")) {
            if (code.startsWith("ET410") || code.startsWith("ET447") || code.startsWith("ET445") || 
                code.startsWith("ET411") || code.startsWith("ET448") || code.startsWith("ET412")) {
                return "Mô đun - Y sinh";
            }
            if (code.startsWith("ET413") || code.startsWith("ET414")) {
                return "Mô đun - Hàng không/Vũ trụ";
            }
            if (code.startsWith("ET426") || code.startsWith("ET437")) {
                return "Mô đun - Đa phương tiện";
            }
            if (code.startsWith("ET435") || code.startsWith("ET434") || code.startsWith("ET4033") || 
                code.startsWith("ET436")) {
                return "Mô đun - Vi mạch";
            }
            if (code.startsWith("ET331") || code.startsWith("ET423") || code.startsWith("ET425") || 
                code.startsWith("ET407") || code.startsWith("ET429") || code.startsWith("ET318")) {
                return "Mô đun";
            }
            if (code.startsWith("ET327")) {
                return "Thực tập";
            }
            if (code.startsWith("ET329") || code.startsWith("ET401") || code.startsWith("ET492")) {
                return code.startsWith("ET492") ? "Đồ án nghiên cứu" : "Cơ sở ngành";
            }
            if (code.startsWith("ET2022")) {
                return "Cơ sở ngành";
            }
            return "Cơ sở ngành";
        }
        
        // Bổ trợ
        if (code.startsWith("EM101") || code.startsWith("EM118") || code.startsWith("ED") || 
            code.startsWith("ET3262") || code.startsWith("CH202") || code.startsWith("ME312")) {
            return "Bổ trợ";
        }
        
        // Mặc định
        if (code.startsWith("CT")) return "Tự chọn";
        if (code.startsWith("MA")) return "Cơ sở";
        
        return "Tự chọn";
    }

    /**
     * Constructor: Tạo màn hình đăng ký học phần cho sinh viên
     * 
     * Khởi tạo toàn bộ UI components, load dữ liệu ban đầu và hiển thị màn hình.
     * 
     * @param owner Frame cha (thường là LoginFrame) - dùng để đặt vị trí cửa sổ
     * @param s Sinh viên đang đăng nhập - chứa thông tin sinh viên (MSSV, họ tên, CTĐT, v.v.)
     * 
     * Quy trình khởi tạo:
     * 1. Thiết lập thuộc tính cửa sổ (title, size, close operation)
     * 2. Tạo header bar với thông tin sinh viên và nút đăng xuất
     * 3. Tạo TabbedPane với 2 tab: Đăng ký học phần và Lịch sử đăng ký
     * 4. Load danh sách học kỳ và chọn học kỳ đầu tiên
     * 5. Gắn các event listener (thay đổi học kỳ, tìm kiếm, gửi đăng ký, v.v.)
     * 6. Refresh dữ liệu ban đầu (danh sách học phần, giỏ đăng ký)
     * 7. Hiển thị màn hình
     */
    public StudentRegistrationFrame(JFrame owner, Student s){
        // Lưu thông tin sinh viên đang đăng nhập
        this.student = s;

        // Thiết lập tiêu đề cửa sổ: hiển thị tên và MSSV của sinh viên
        setTitle("Sinh viên – Đăng ký học phần | " +
                s.fullName + " (" + s.studentId + ")");
        
        // Khi đóng cửa sổ, thoát hoàn toàn ứng dụng
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Mở cửa sổ ở chế độ fullscreen (maximized)
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Thiết lập màu nền và layout chính
        setBackground(new Color(249, 250, 251)); // Màu xám nhạt
        setLayout(new BorderLayout()); // Layout chia thành 5 vùng: NORTH, SOUTH, EAST, WEST, CENTER

        // ========== TOP HEADER BAR ==========
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Color.WHITE);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
                new EmptyBorder(16, 24, 16, 24)
        ));
        
        JLabel headerTitle = new JLabel("Đăng ký Học phần");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerTitle.setForeground(new Color(31, 41, 55));
        
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);
        
        JLabel lbUser = new JLabel(student.fullName + " (" + student.studentId + ")");
        lbUser.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lbUser.setForeground(new Color(107, 114, 128));
        
        JButton btnLogoutTop = new JButton("Đăng xuất");
        btnLogoutTop.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogoutTop.setBackground(new Color(59, 130, 246));
        btnLogoutTop.setForeground(Color.WHITE);
        btnLogoutTop.setBorderPainted(false);
        btnLogoutTop.setPreferredSize(new Dimension(120, 36));
        btnLogoutTop.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogoutTop.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        headerRight.add(lbUser);
        headerRight.add(btnLogoutTop);
        
        headerBar.add(headerTitle, BorderLayout.WEST);
        headerBar.add(headerRight, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // ========== MAIN CONTENT ==========
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(249, 250, 251));
        mainContent.setBorder(new EmptyBorder(24, 24, 24, 24));

        // ========== TABBED PANE ==========
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tabbedPane.setBackground(new Color(249, 250, 251));
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Tab 1: Đăng ký học phần
        JPanel registrationPanel = createRegistrationPanel();
        
        // Tab 2: Lịch sử đăng ký
        JPanel historyPanel = createHistoryPanel();
        
        tabbedPane.addTab("Đăng ký học phần", registrationPanel);
        tabbedPane.addTab("Lịch sử đăng ký", historyPanel);
        
        // Customize tab appearance
        tabbedPane.setBackgroundAt(0, new Color(243, 244, 246));
        tabbedPane.setBackgroundAt(1, new Color(243, 244, 246));
        tabbedPane.setForegroundAt(0, new Color(107, 114, 128));
        tabbedPane.setForegroundAt(1, new Color(107, 114, 128));
        
        // Initialize first tab as selected
        tabbedPane.setBackgroundAt(0, Color.WHITE);
        tabbedPane.setForegroundAt(0, new Color(59, 130, 246));
        
        // Thêm listener để refresh lịch sử khi chuyển tab và cập nhật style
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            
            // Cập nhật style cho các tab
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (i == selectedIndex) {
                    tabbedPane.setBackgroundAt(i, Color.WHITE);
                    tabbedPane.setForegroundAt(i, new Color(59, 130, 246));
                } else {
                    tabbedPane.setBackgroundAt(i, new Color(243, 244, 246));
                    tabbedPane.setForegroundAt(i, new Color(107, 114, 128));
                }
            }
            
            // Refresh lịch sử khi chuyển sang tab lịch sử
            if (selectedIndex == 1) {
                refreshHistory();
            }
        });

        mainContent.add(tabbedPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);
        
        // Footer
        JPanel footer = new JPanel();
        footer.setBackground(new Color(249, 250, 251));
        footer.setBorder(new EmptyBorder(16, 0, 16, 0));
        JLabel footerText = new JLabel("© 2025 University Portal. All rights reserved.");
        footerText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerText.setForeground(new Color(107, 114, 128));
        footer.add(footerText);
        add(footer, BorderLayout.SOUTH);

        // ====== GẮN ACTION ======
        cbTerm.addActionListener(e -> refreshAll());
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshCourseTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshCourseTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshCourseTable(); }
        });
        btnSubmit.addActionListener(e -> submit());
        btnClearAll.addActionListener(e -> clearCart());

        // Chọn kỳ đầu tiên, sau đó load dữ liệu
        cbTerm.removeAllItems();
        for (String t : Memory.loadTerms()) cbTerm.addItem(t);
        if (cbTerm.getItemCount() > 0) {
            cbTerm.setSelectedIndex(0);
            refreshAll();
        }

        setVisible(true);
    }

    /**
     * Tạo panel đăng ký học phần với layout 2 cột (trái: danh sách học phần, phải: giỏ đăng ký)
     * 
     * Panel này được thêm vào Tab 1 "Đăng ký học phần" trong TabbedPane.
     * 
     * Layout:
     * - Bên trái (CENTER): Panel danh sách học phần có thể đăng ký
     *   + Filter: Chọn học kỳ, tìm kiếm học phần
     *   + Bảng học phần với pagination
     * - Bên phải (EAST): Panel giỏ đăng ký
     *   + Danh sách học phần đã chọn
     *   + Tổng số tín chỉ + progress bar
     *   + Nút gửi đăng ký
     * 
     * @return Panel chứa layout 2 cột cho màn hình đăng ký học phần
     */
    JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);
        panel.setBackground(new Color(249, 250, 251));
        
        // Panel trái: Danh sách học phần
        JPanel leftPanel = createCourseListPanel();
        
        // Panel phải: Giỏ đăng ký
        JPanel rightPanel = createCartPanel();
        
        panel.add(leftPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Tạo panel danh sách học phần có thể đăng ký (bên trái)
     * 
     * Panel này chứa:
     * - Filter section: Chọn học kỳ và tìm kiếm học phần
     * - Bảng học phần: Hiển thị danh sách học phần có thể đăng ký
     *   + Các cột: Mã HP, Tên học phần, TC, Loại, Hành động
     *   + Cột "Hành động" có nút "THÊM" hoặc "ĐÃ CHỌN"
     * - Pagination: Phân trang danh sách (5 môn/trang)
     * 
     * Học phần được lọc theo:
     * - Học kỳ đã chọn
     * - Từ khóa tìm kiếm (mã hoặc tên)
     * - Offering phải đang mở (open = true)
     * - CTĐT của sinh viên phải được phép đăng ký
     * 
     * @return Panel chứa danh sách học phần có thể đăng ký
     */
    JPanel createCourseListPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBackground(Color.WHITE);
        
        // Header với filters
        JPanel filterPanel = new JPanel(new BorderLayout(0, 12));
        filterPanel.setOpaque(false);
        
        // Hàng trên: Học kỳ
        JPanel termRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        termRow.setOpaque(false);
        JLabel lbTerm = new JLabel("HỌC KỲ");
        lbTerm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbTerm.setForeground(new Color(107, 114, 128));
        cbTerm.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbTerm.setPreferredSize(new Dimension(150, 36));
        termRow.add(lbTerm);
        termRow.add(cbTerm);
        
        // Hàng dưới: Tìm kiếm
        JPanel searchRow = new JPanel(new BorderLayout(0, 0));
        searchRow.setOpaque(false);
        JLabel lbSearch = new JLabel("TÌM KIẾM HỌC PHẦN");
        lbSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbSearch.setForeground(new Color(107, 114, 128));
        searchRow.add(lbSearch, BorderLayout.NORTH);
        
        JPanel searchFieldPanel = new JPanel(new BorderLayout());
        searchFieldPanel.setOpaque(false);
        searchField.setPreferredSize(new Dimension(0, 40));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        searchField.setToolTipText("Nhập mã hoặc tên học phần...");
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        searchRow.add(searchFieldPanel, BorderLayout.CENTER);
        
        filterPanel.add(termRow, BorderLayout.NORTH);
        filterPanel.add(searchRow, BorderLayout.CENTER);
        
        card.add(filterPanel, BorderLayout.NORTH);
        
        // Bảng học phần
        courseModel = new DefaultTableModel(
                new Object[]{"MÃ HP", "TÊN HỌC PHẦN", "TC", "LOẠI", "HÀNH ĐỘNG"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 4; // Chỉ cột HÀNH ĐỘNG có thể tương tác
            }
        };
        
        courseTable = new JTable(courseModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return c;
            }
        };
        courseTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        courseTable.setRowHeight(50);
        courseTable.setShowGrid(true);
        courseTable.setGridColor(new Color(229, 231, 235));
        
        // Custom header
        JTableHeader header = courseTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(243, 244, 246));
        header.setForeground(new Color(31, 41, 55));
        header.setPreferredSize(new Dimension(0, 45));
        header.setReorderingAllowed(false);
        
        // Custom renderer cho cột HÀNH ĐỘNG
        courseTable.getColumn("HÀNH ĐỘNG").setCellRenderer(new ButtonCellRenderer());
        
        // Custom cell editor cho cột HÀNH ĐỘNG
        courseTable.getColumn("HÀNH ĐỘNG").setCellEditor(new ButtonCellEditor());
        
        // Độ rộng cột
        courseTable.getColumnModel().getColumn(0).setPreferredWidth(100);  // MÃ HP
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(350);  // TÊN HỌC PHẦN
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(60);   // TC
        courseTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // LOẠI
        courseTable.getColumnModel().getColumn(4).setPreferredWidth(120);   // HÀNH ĐỘNG
        
        // Cho phép click vào cột HÀNH ĐỘNG để kích hoạt editor
        courseTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = courseTable.rowAtPoint(e.getPoint());
                int col = courseTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 4) { // Cột HÀNH ĐỘNG
                    courseTable.editCellAt(row, col);
                }
            }
        });
        
        JScrollPane sp = new JScrollPane(courseTable);
        sp.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        sp.setViewportBorder(null);
        card.add(sp, BorderLayout.CENTER);
        
        // Pagination
        paginationPanel = new JPanel(new BorderLayout());
        paginationPanel.setOpaque(false);
        paginationPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        
        lbPaginationInfo = new JLabel();
        lbPaginationInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbPaginationInfo.setForeground(new Color(107, 114, 128));
        
        paginationButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        paginationButtons.setOpaque(false);
        btnPrevPage = new JButton("<");
        btnPrevPage.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPrevPage.setPreferredSize(new Dimension(36, 36));
        btnPrevPage.setBackground(Color.WHITE);
        btnPrevPage.setForeground(new Color(17, 24, 39)); // Màu đậm để dễ nhìn
        btnPrevPage.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        btnPrevPage.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrevPage.setOpaque(true);
        btnPrevPage.setFocusPainted(false);
        btnPrevPage.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshCourseTable();
            }
        });
        
        btnNextPage = new JButton(">");
        btnNextPage.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNextPage.setPreferredSize(new Dimension(36, 36));
        btnNextPage.setBackground(Color.WHITE);
        btnNextPage.setForeground(new Color(17, 24, 39)); // Màu đậm để dễ nhìn
        btnNextPage.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        btnNextPage.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNextPage.setOpaque(true);
        btnNextPage.setFocusPainted(false);
        btnNextPage.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) getFilteredCourses().size() / pageSize);
            if (currentPage < totalPages) {
                currentPage++;
                refreshCourseTable();
            }
        });
        
        paginationButtons.add(btnPrevPage);
        // Page numbers will be added dynamically in refreshCourseTable
        paginationButtons.add(btnNextPage);
        
        paginationPanel.add(lbPaginationInfo, BorderLayout.WEST);
        paginationPanel.add(paginationButtons, BorderLayout.EAST);
        
        card.add(paginationPanel, BorderLayout.SOUTH);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        
        return wrapper;
    }
    
    /**
     * Tạo panel giỏ đăng ký (bên phải)
     * 
     * Panel này hiển thị:
     * - Header: Tiêu đề "Đăng ký" + nút "Xóa hết"
     * - Danh sách học phần đã thêm vào giỏ:
     *   + Mỗi item hiển thị: Mã HP, tên học phần, số tín chỉ
     *   + Nếu đã gửi đăng ký: hiển thị thêm trạng thái (Đã gửi, Đã duyệt, v.v.)
     *   + Nút xóa (chỉ enable nếu chưa gửi hoặc trạng thái là "Tạm")
     * - Summary section:
     *   + Label hiển thị tổng số tín chỉ (ví dụ: "15/24 TC")
     *   + Progress bar hiển thị tiến độ
     *   + Label cảnh báo nếu < 12 TC hoặc > 24 TC
     * - Nút "Gửi đăng ký": Gửi tất cả học phần trong giỏ để PĐT duyệt
     * 
     * Kích thước panel được cố định (width = 380px) để không thay đổi khi thêm nhiều môn.
     * 
     * @return Panel chứa giỏ đăng ký và các controls liên quan
     */
    JPanel createCartPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(380, 0));
        card.setBackground(Color.WHITE);
        
        // Header - đơn giản, hiện đại
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        JLabel cartTitle = new JLabel("Đăng ký");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cartTitle.setForeground(new Color(17, 24, 39));
        
        btnClearAll = new JButton("Xóa hết");
        btnClearAll.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClearAll.setForeground(new Color(156, 163, 175));
        btnClearAll.setBackground(Color.WHITE);
        btnClearAll.setBorder(null);
        btnClearAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearAll.setFocusPainted(false);
        btnClearAll.setContentAreaFilled(false);
        
        // Hover effect cho nút xóa hết
        btnClearAll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnClearAll.setForeground(new Color(239, 68, 68));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnClearAll.setForeground(new Color(156, 163, 175));
            }
        });
        
        headerPanel.add(cartTitle, BorderLayout.WEST);
        headerPanel.add(btnClearAll, BorderLayout.EAST);
        
        card.add(headerPanel, BorderLayout.NORTH);
        
        // Danh sách học phần đã chọn với scroll pane cố định
        cartPanel = new JPanel();
        cartPanel.setLayout(new BoxLayout(cartPanel, BoxLayout.Y_AXIS));
        cartPanel.setOpaque(false);
        cartPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        JScrollPane cartScroll = new JScrollPane(cartPanel);
        cartScroll.setBorder(null);
        cartScroll.setOpaque(false);
        cartScroll.getViewport().setOpaque(false);
        cartScroll.setBackground(Color.WHITE);
        // Cố định kích thước để không thay đổi khi thêm nhiều môn
        cartScroll.setPreferredSize(new Dimension(0, 400));
        cartScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));
        cartScroll.setMinimumSize(new Dimension(0, 200));
        cartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        cartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Customize scrollbar - mỏng và đơn giản
        cartScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        cartScroll.getVerticalScrollBar().setUnitIncrement(16);
        cartScroll.getVerticalScrollBar().setBackground(Color.WHITE);
        
        card.add(cartScroll, BorderLayout.CENTER);
        
        // Summary panel - đơn giản, hiện đại
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        lbCreditRange = new JLabel("0/24 TC");
        lbCreditRange.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lbCreditRange.setForeground(new Color(17, 24, 39));
        
        creditProgressBar = new JProgressBar(0, 24);
        creditProgressBar.setStringPainted(false);
        creditProgressBar.setPreferredSize(new Dimension(0, 6));
        creditProgressBar.setBackground(new Color(243, 244, 246));
        creditProgressBar.setForeground(new Color(59, 130, 246));
        creditProgressBar.setBorderPainted(false);
        
        JPanel minMaxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        minMaxPanel.setOpaque(false);
        JLabel lbMin = new JLabel("Tối thiểu: 12 TC");
        lbMin.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbMin.setForeground(new Color(156, 163, 175));
        JLabel lbMax = new JLabel("Tối đa: 24 TC");
        lbMax.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbMax.setForeground(new Color(156, 163, 175));
        minMaxPanel.add(lbMin);
        minMaxPanel.add(lbMax);
        
        lbWarning = new JLabel("Cần đăng ký tối thiểu 12 tín chỉ để hoàn tất.");
        lbWarning.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbWarning.setForeground(new Color(239, 68, 68));
        lbWarning.setVisible(false);
        
        summaryPanel.add(lbCreditRange);
        summaryPanel.add(Box.createVerticalStrut(12));
        summaryPanel.add(creditProgressBar);
        summaryPanel.add(Box.createVerticalStrut(12));
        summaryPanel.add(minMaxPanel);
        summaryPanel.add(Box.createVerticalStrut(8));
        summaryPanel.add(lbWarning);
        
        // Nút gửi đăng ký - đơn giản, hiện đại
        btnSubmit = new JButton("Gửi đăng ký");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnSubmit.setBackground(new Color(59, 130, 246));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setPreferredSize(new Dimension(0, 44));
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setFocusPainted(false);
        
        // Hover effect
        btnSubmit.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnSubmit.setBackground(new Color(37, 99, 235));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnSubmit.setBackground(new Color(59, 130, 246));
            }
        });
        
        // Kết hợp summary và submit button
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 16));
        bottomPanel.setOpaque(false);
        bottomPanel.add(summaryPanel, BorderLayout.CENTER);
        bottomPanel.add(btnSubmit, BorderLayout.SOUTH);
        
        card.add(bottomPanel, BorderLayout.SOUTH);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        
        return wrapper;
    }
    
    /**
     * Tạo panel lịch sử đăng ký học phần
     * 
     * Panel này được thêm vào Tab 2 "Lịch sử đăng ký" trong TabbedPane.
     * 
     * Hiển thị bảng chứa tất cả đăng ký của sinh viên qua các học kỳ:
     * - Các cột: Học kỳ, Mã HP, Tên học phần, Số TC, Ngày đăng ký, Trạng thái
     * - Màu nền cột "Trạng thái" thay đổi theo trạng thái:
     *   + Xanh lá: Đã duyệt
     *   + Đỏ: Đã từ chối
     *   + Vàng: Đã gửi / Tạm (chờ xử lý)
     * 
     * Dữ liệu được load từ Memory.regs theo MSSV của sinh viên.
     * 
     * @return Panel chứa bảng lịch sử đăng ký
     */
    JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(249, 250, 251));
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        CardPanel historyCard = new CardPanel();
        historyCard.setLayout(new BorderLayout());
        historyCard.setBackground(Color.WHITE);

        JLabel title = new JLabel("Lịch sử đăng ký học phần");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(31, 41, 55));
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        historyCard.add(title, BorderLayout.NORTH);

        historyModel = new DefaultTableModel(
                new Object[]{"Học kỳ", "Mã HP", "Tên học phần", "Số TC", 
                        "Ngày đăng ký", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        historyTable = new JTable(historyModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                // Cột "Trạng thái" là cột cuối cùng (index 5)
                if (column == 5) {
                    String status = (String) getValueAt(row, column);
                    if (status != null) {
                        if ("Đã duyệt".equals(status) || "Thành công".equals(status)) {
                            c.setBackground(new Color(220, 255, 220));
                        } else if ("Từ chối".equals(status)) {
                            c.setBackground(new Color(255, 220, 220));
                        } else if ("Đã gửi".equals(status) || "Tạm".equals(status)) {
                            c.setBackground(new Color(255, 255, 220));
                        }
                    }
                }
                return c;
            }
        };
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        historyTable.setRowHeight(48);
        
        JTableHeader historyHeader = historyTable.getTableHeader();
        historyHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        historyHeader.setBackground(new Color(243, 244, 246));
        historyHeader.setForeground(new Color(31, 41, 55));
        historyHeader.setPreferredSize(new Dimension(0, 45));
        historyHeader.setReorderingAllowed(false);
        
        historyTable.setSelectionBackground(new Color(239, 246, 255));
        historyTable.setSelectionForeground(Color.BLACK);
        historyTable.setShowGrid(true);
        historyTable.setGridColor(new Color(229, 231, 235));
        
        // Load lịch sử ban đầu
        refreshHistory();
        
        JScrollPane historySp = new JScrollPane(historyTable);
        historySp.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        historyCard.add(historySp, BorderLayout.CENTER);
        
        panel.add(historyCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Refresh (làm mới) dữ liệu lịch sử đăng ký
     * 
     * Phương thức này:
     * 1. Xóa tất cả dữ liệu cũ trong bảng
     * 2. Load lại tất cả đăng ký của sinh viên từ Memory.regs
     * 3. Thêm vào bảng theo thứ tự: học kỳ -> danh sách RegItem
     * 
     * Được gọi khi:
     * - Chuyển sang tab "Lịch sử đăng ký"
     * - Sau khi gửi đăng ký mới
     * - Sau khi xóa học phần khỏi giỏ (nếu đã đăng ký)
     */
    void refreshHistory() {
        if (historyModel == null) return;
        
        // Xóa dữ liệu cũ
        historyModel.setRowCount(0);
        
        // Load lịch sử
        var allRegs = Memory.regs.get(student.studentId);
        if (allRegs != null) {
            for (var entry : allRegs.entrySet()) {
                String term = entry.getKey();
                for (RegItem item : entry.getValue()) {
                    historyModel.addRow(new Object[]{
                            term,
                            item.course.code,
                            item.course.name,
                            String.valueOf(item.course.credits),
                            item.date,
                            item.status
                    });
                }
            }
        }
    }
    
    /**
     * Tạo nút số trang cho pagination
     * 
     * Tạo một JButton tùy chỉnh để hiển thị số trang trong pagination.
     * Nút có các đặc điểm:
     * - Custom paintComponent để đảm bảo text được render đúng (đặc biệt là màu trắng)
     * - Màu nền và chữ thay đổi tùy theo trạng thái:
     *   + Trang được chọn: nền xanh (#3B82F6), chữ trắng, border xanh đậm
     *   + Trang không được chọn: nền trắng, chữ đen
     * - Hover effect: đổi màu nền khi hover (chỉ cho trang không được chọn)
     * - Click: chuyển sang trang đó và refresh bảng học phần
     * 
     * @param pageNum Số trang (bắt đầu từ 1)
     * @return JButton tùy chỉnh để hiển thị số trang
     */
    JButton createPageButton(int pageNum) {
        // Tạo custom button để đảm bảo text được render đúng
        JButton btn = new JButton(String.valueOf(pageNum)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Vẽ nền
                if (isOpaque()) {
                    g2.setColor(getBackground());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                
                // Vẽ text với màu foreground - đảm bảo màu trắng hiển thị rõ
                Color textColor = getForeground();
                g2.setColor(textColor);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + textHeight) / 2 - fm.getDescent();
                g2.drawString(text, x, y);
                
                g2.dispose();
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // Vẽ border
                super.paintBorder(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(36, 36));
        
        // Màu mặc định cho trang không được chọn
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(17, 24, 39)); // Màu đậm để dễ nhìn
        btn.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true); // Đảm bảo nền được vẽ
        btn.setContentAreaFilled(true); // Đảm bảo nền được fill
        btn.setFocusPainted(false);
        
        // Hover effect để làm rõ hơn (chỉ cho trang không được chọn)
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (pageNum != currentPage) {
                    btn.setBackground(new Color(249, 250, 251));
                    btn.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (pageNum != currentPage) {
                    btn.setBackground(Color.WHITE);
                    btn.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
                }
            }
        });
        
        final int page = pageNum;
        btn.addActionListener(e -> {
            currentPage = page;
            refreshCourseTable();
        });
        
        return btn;
    }
    
    /**
     * Lấy danh sách học phần đã được lọc theo các tiêu chí
     * 
     * Lọc học phần theo:
     * 1. Học kỳ đã chọn (cbTerm)
     * 2. Từ khóa tìm kiếm (searchField) - tìm trong mã HP và tên học phần
     * 3. Offering phải đang mở (offering.open = true)
     * 4. CTĐT của sinh viên phải được phép đăng ký:
     *    - Nếu offering.allowedProgram = "Tất cả" → cho phép tất cả
     *    - Nếu offering.allowedProgram = tên CTĐT cụ thể → chỉ cho phép sinh viên có program trùng khớp
     * 
     * @return Danh sách Course đã được lọc, sẵn sàng để hiển thị trong bảng
     */
    List<Course> getFilteredCourses() {
        List<Course> filtered = new ArrayList<>();
        String term = (String) cbTerm.getSelectedItem();
        if (term == null) return filtered;
        
        // Kiểm tra học kỳ có đang hoạt động không (không phải "Đã kết thúc")
        // Nếu học kỳ đã kết thúc, không hiển thị học phần nào
        if (!Memory.isTermOpen(term)) {
            return filtered; // Trả về danh sách rỗng nếu học kỳ đã kết thúc
        }
        
        String searchText = searchField.getText().toLowerCase().trim();
        
        for (Course c : Memory.courses.values()) {
            var off = Memory.getOffering(term, c.code);
            if (off != null && off.open &&
                    ("Tất cả".equals(off.allowedProgram) || student.program.equals(off.allowedProgram))) {
                
                if (searchText.isEmpty() || 
                    c.code.toLowerCase().contains(searchText) ||
                    c.name.toLowerCase().contains(searchText)) {
                    filtered.add(c);
                }
            }
        }
        
        return filtered;
    }
    
    /**
     * Refresh (làm mới) bảng danh sách học phần
     * 
     * Phương thức này:
     * 1. Xóa tất cả dữ liệu cũ trong bảng
     * 2. Lấy danh sách học phần đã được lọc (getFilteredCourses)
     * 3. Tính toán pagination (trang hiện tại, tổng số trang)
     * 4. Cập nhật thông tin pagination (label + nút Previous/Next)
     * 5. Tạo các nút số trang (hiển thị tối đa 5 số trang xung quanh trang hiện tại)
     * 6. Thêm các dòng học phần của trang hiện tại vào bảng
     * 7. Refresh UI để hiển thị thay đổi
     * 
     * Được gọi khi:
     * - Thay đổi học kỳ
     * - Thay đổi từ khóa tìm kiếm
     * - Chuyển trang (pagination)
     * - Thêm/xóa học phần khỏi giỏ (để cập nhật trạng thái "ĐÃ CHỌN" / "THÊM")
     */
    void refreshCourseTable() {
        courseModel.setRowCount(0);
        List<Course> filtered = getFilteredCourses();
        
        // Pagination
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filtered.size());
        int totalPages = (int) Math.ceil((double) filtered.size() / pageSize);
        
        // Cập nhật pagination info
        if (filtered.size() > 0) {
            lbPaginationInfo.setText(String.format("Hiển thị %d đến %d trên %d kết quả", 
                    start + 1, end, filtered.size()));
        } else {
            lbPaginationInfo.setText("Không có kết quả");
        }
        
        // Cập nhật style cho nút Previous/Next khi disabled
        boolean prevEnabled = currentPage > 1;
        boolean nextEnabled = currentPage < totalPages;
        
        btnPrevPage.setEnabled(prevEnabled);
        if (prevEnabled) {
            btnPrevPage.setBackground(Color.WHITE);
            btnPrevPage.setForeground(new Color(17, 24, 39));
        } else {
            btnPrevPage.setBackground(new Color(249, 250, 251));
            btnPrevPage.setForeground(new Color(156, 163, 175));
        }
        
        btnNextPage.setEnabled(nextEnabled);
        if (nextEnabled) {
            btnNextPage.setBackground(Color.WHITE);
            btnNextPage.setForeground(new Color(17, 24, 39));
        } else {
            btnNextPage.setBackground(new Color(249, 250, 251));
            btnNextPage.setForeground(new Color(156, 163, 175));
        }
        
        // Cập nhật page number buttons
        paginationButtons.removeAll();
        paginationButtons.add(btnPrevPage);
        
        // Hiển thị tối đa 5 số trang xung quanh trang hiện tại
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);
        
        if (startPage > 1) {
            JButton btnFirst = createPageButton(1);
            if (currentPage == 1) {
                // Trang đầu tiên được chọn
                btnFirst.setBackground(new Color(59, 130, 246));
                btnFirst.setForeground(Color.WHITE);
                btnFirst.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 2));
                btnFirst.setOpaque(true);
                btnFirst.setContentAreaFilled(true);
                // Đảm bảo text được hiển thị với màu trắng
                btnFirst.setText("1"); // Set lại text để đảm bảo render đúng
                btnFirst.setFont(new Font("Segoe UI", Font.BOLD, 14)); // Đảm bảo font được set lại
                btnFirst.repaint();
            }
            paginationButtons.add(btnFirst);
            if (startPage > 2) {
                JLabel ellipsis = new JLabel("...");
                ellipsis.setForeground(new Color(75, 85, 99)); // Màu đậm hơn để dễ nhìn
                ellipsis.setFont(new Font("Segoe UI", Font.BOLD, 14));
                paginationButtons.add(ellipsis);
            }
        }
        
        for (int i = startPage; i <= endPage; i++) {
            JButton btn = createPageButton(i);
            if (i == currentPage) {
                // Trang được chọn: nền xanh, chữ trắng đậm
                btn.setBackground(new Color(59, 130, 246));
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 2)); // Border xanh đậm hơn
                btn.setOpaque(true);
                btn.setContentAreaFilled(true);
                // Đảm bảo text được hiển thị với màu trắng
                btn.setText(String.valueOf(i)); // Set lại text để đảm bảo render đúng
                btn.setFont(new Font("Segoe UI", Font.BOLD, 14)); // Đảm bảo font được set lại
                btn.repaint(); // Đảm bảo nút được vẽ lại với màu mới
            }
            paginationButtons.add(btn);
        }
        
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                JLabel ellipsis = new JLabel("...");
                ellipsis.setForeground(new Color(75, 85, 99)); // Màu đậm hơn để dễ nhìn
                ellipsis.setFont(new Font("Segoe UI", Font.BOLD, 14));
                paginationButtons.add(ellipsis);
            }
            JButton btnLast = createPageButton(totalPages);
            if (currentPage == totalPages) {
                // Trang cuối cùng được chọn
                btnLast.setBackground(new Color(59, 130, 246));
                btnLast.setForeground(Color.WHITE);
                btnLast.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 2));
                btnLast.setOpaque(true);
                btnLast.setContentAreaFilled(true);
                // Đảm bảo text được hiển thị với màu trắng
                btnLast.setText(String.valueOf(totalPages)); // Set lại text để đảm bảo render đúng
                btnLast.setFont(new Font("Segoe UI", Font.BOLD, 14)); // Đảm bảo font được set lại
                btnLast.repaint();
            }
            paginationButtons.add(btnLast);
        }
        
        paginationButtons.add(btnNextPage);
        paginationButtons.revalidate();
        paginationButtons.repaint();
        
        // Thêm các dòng cho trang hiện tại
        for (int i = start; i < end; i++) {
            Course c = filtered.get(i);
            String type = getCourseType(c.code);
            boolean isInCart = selectedCourseCodes.contains(c.code);
            
            courseModel.addRow(new Object[]{
                    c.code,
                    c.name,
                    String.valueOf(c.credits),
                    type,
                    isInCart ? "ĐÃ CHỌN" : "THÊM"
            });
        }
        
        // Đảm bảo renderer được áp dụng
        courseTable.revalidate();
        courseTable.repaint();
    }
    
    /**
     * Tính tổng số tín chỉ đã đăng ký trong một học kỳ
     * 
     * Lấy tất cả RegItem của sinh viên trong học kỳ đó từ Memory,
     * sau đó tính tổng số tín chỉ của tất cả học phần đã đăng ký.
     * 
     * @param term Học kỳ cần tính (ví dụ: "20252")
     * @return Tổng số tín chỉ đã đăng ký trong học kỳ đó (0 nếu term = null hoặc chưa đăng ký gì)
     */
    int getTotalRegisteredCredits(String term) {
        if (term == null) return 0;
        var existingRegs = Memory.loadReg(student.studentId, term);
        return existingRegs.stream()
                .mapToInt(r -> r.course.credits)
                .sum();
    }
    
    /**
     * Tính tổng số tín chỉ của các học phần trong giỏ đăng ký hiện tại
     * 
     * Duyệt qua selectedCourseCodes (Set chứa mã các học phần trong giỏ),
     * lấy Course tương ứng từ Memory.courses và tính tổng số tín chỉ.
     * 
     * @return Tổng số tín chỉ của tất cả học phần trong giỏ (0 nếu giỏ trống)
     */
    int getTotalCartCredits() {
        return selectedCourseCodes.stream()
                .mapToInt(code -> {
                    Course c = Memory.courses.get(code);
                    return c != null ? c.credits : 0;
                })
                .sum();
    }
    
    /**
     * Thêm học phần vào giỏ đăng ký
     * 
     * Quy trình:
     * 1. Kiểm tra học phần có tồn tại không
     * 2. Kiểm tra học kỳ đã được chọn chưa
     * 3. Kiểm tra học phần đã được đăng ký chưa (tránh trùng lặp)
     * 4. Kiểm tra tổng số tín chỉ:
     *    - Tính: đã đăng ký + trong giỏ + môn mới
     *    - Nếu > 24 TC: hiển thị cảnh báo và không cho thêm
     * 5. Thêm mã học phần vào selectedCourseCodes
     * 6. Refresh giỏ đăng ký và bảng học phần
     * 
     * @param courseCode Mã học phần cần thêm vào giỏ (ví dụ: "CT101")
     */
    void addToCart(String courseCode) {
        Course course = Memory.courses.get(courseCode);
        if (course == null) return;
        
        String term = (String) cbTerm.getSelectedItem();
        if (term == null) return;
        
        // Kiểm tra đã đăng ký chưa
        var existingRegs = Memory.loadReg(student.studentId, term);
        boolean alreadyRegistered = existingRegs.stream()
                .anyMatch(r -> r.course.code.equals(courseCode));
        
        if (alreadyRegistered) {
            JOptionPane.showMessageDialog(this, "Bạn đã đăng ký học phần này.");
            return;
        }
        
        // Kiểm tra tổng số tín chỉ: đã đăng ký + trong giỏ + môn mới
        int registeredCredits = getTotalRegisteredCredits(term);
        int cartCredits = getTotalCartCredits();
        int newTotal = registeredCredits + cartCredits + course.credits;
        
        if (newTotal > 24) {
            JOptionPane.showMessageDialog(this, 
                    "Tổng số tín chỉ không được vượt quá 24. " +
                    "Hiện tại: " + (registeredCredits + cartCredits) + "/24 TC. " +
                    "Môn này: " + course.credits + " TC.");
            return;
        }
        
        // Thêm vào giỏ
        selectedCourseCodes.add(courseCode);
        refreshCart();
        refreshCourseTable();
    }
    
    /**
     * Refresh (làm mới) giỏ đăng ký
     * 
     * Phương thức này:
     * 1. Xóa tất cả items cũ trong giỏ
     * 2. Duyệt qua selectedCourseCodes (các học phần trong giỏ)
     * 3. Với mỗi học phần:
     *    - Kiểm tra xem đã đăng ký chưa (có RegItem trong Memory không)
     *    - Tạo item panel hiển thị: mã HP, tên, số TC, trạng thái (nếu đã đăng ký)
     *    - Tạo nút xóa (disable nếu đã đăng ký và trạng thái không phải "Tạm")
     * 4. Tính tổng số tín chỉ: đã đăng ký + chưa đăng ký trong giỏ
     * 5. Cập nhật label tổng tín chỉ và progress bar
     * 6. Hiển thị cảnh báo nếu < 12 TC hoặc > 24 TC
     * 7. Nếu giỏ trống, hiển thị thông báo "Chưa có học phần nào"
     * 
     * Được gọi khi:
     * - Thêm học phần vào giỏ
     * - Xóa học phần khỏi giỏ
     * - Thay đổi học kỳ
     * - Sau khi gửi đăng ký (để cập nhật trạng thái)
     */
    void refreshCart() {
        cartPanel.removeAll();
        
        String term = (String) cbTerm.getSelectedItem();
        if (term == null) return;
        
        // Lấy danh sách các môn đã đăng ký để kiểm tra trạng thái
        var existingRegs = Memory.loadReg(student.studentId, term);
        Map<String, RegItem> registeredMap = new HashMap<>();
        for (RegItem item : existingRegs) {
            registeredMap.put(item.course.code, item);
        }
        
        // Tính tổng số tín chỉ: chỉ tính các môn CHƯA đăng ký trong giỏ + đã đăng ký
        int registeredCredits = getTotalRegisteredCredits(term);
        int unregisteredCartCredits = 0; // Chỉ tính các môn chưa đăng ký trong giỏ
        
        for (String code : selectedCourseCodes) {
            Course course = Memory.courses.get(code);
            if (course == null) continue;
            
            // Kiểm tra xem môn này đã được đăng ký chưa
            RegItem regItem = registeredMap.get(code);
            boolean isRegistered = regItem != null;
            String status = isRegistered ? regItem.status : null;
            
            // Chỉ tính tín chỉ của các môn chưa đăng ký vào tổng
            if (!isRegistered) {
                unregisteredCartCredits += course.credits;
            }
            
            // Tạo item trong giỏ - đơn giản, hiện đại
            JPanel itemPanel = new JPanel(new BorderLayout(12, 0));
            itemPanel.setOpaque(false);
            itemPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
            
            // Tạo label với trạng thái nếu đã đăng ký
            String courseInfoText;
            if (isRegistered && status != null) {
                courseInfoText = "<html><div style='line-height: 1.5;'>" +
                        "<span style='font-size: 14px; color: #111827; font-weight: 600;'>" + code + "</span> " +
                        "<span style='font-size: 13px; color: #9CA3AF;'>" + course.credits + " TC</span><br/>" +
                        "<span style='font-size: 13px; color: #374151;'>" + course.name + "</span><br/>" +
                        "<span style='color: #10B981; font-size: 12px;'>" + status + "</span>" +
                        "</div></html>";
            } else {
                courseInfoText = "<html><div style='line-height: 1.5;'>" +
                        "<span style='font-size: 14px; color: #111827; font-weight: 600;'>" + code + "</span> " +
                        "<span style='font-size: 13px; color: #9CA3AF;'>" + course.credits + " TC</span><br/>" +
                        "<span style='font-size: 13px; color: #374151;'>" + course.name + "</span>" +
                        "</div></html>";
            }
            
            JLabel courseInfo = new JLabel(courseInfoText);
            courseInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            
            // Nút xóa - đơn giản, không border
            JButton btnRemove = new JButton("x");
            btnRemove.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btnRemove.setForeground(new Color(156, 163, 175));
            btnRemove.setBackground(Color.WHITE);
            btnRemove.setBorder(null);
            btnRemove.setPreferredSize(new Dimension(28, 28));
            btnRemove.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnRemove.setFocusPainted(false);
            btnRemove.setContentAreaFilled(false);
            
            // Hover effect - đơn giản
            btnRemove.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (btnRemove.isEnabled()) {
                        btnRemove.setForeground(new Color(239, 68, 68));
                    }
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (btnRemove.isEnabled()) {
                        btnRemove.setForeground(new Color(156, 163, 175));
                    }
                }
            });
            
            // Nếu đã đăng ký, disable nút xóa hoặc chỉ cho phép xóa nếu trạng thái là "Tạm"
            if (isRegistered && !"Tạm".equals(status)) {
                btnRemove.setEnabled(false);
                btnRemove.setForeground(new Color(200, 200, 200));
            }
            
            btnRemove.addActionListener(e -> {
                // Xóa khỏi giỏ
                selectedCourseCodes.remove(code);
                
                // Nếu đã đăng ký, xóa khỏi đăng ký
                if (term != null) {
                    Set<String> codesToDelete = new HashSet<>();
                    codesToDelete.add(code);
                    Memory.deleteByCourseCodes(student.studentId, term, codesToDelete);
                }
                
                refreshCart();
                refreshCourseTable();
                refreshHistory();
            });
            
            itemPanel.add(courseInfo, BorderLayout.CENTER);
            itemPanel.add(btnRemove, BorderLayout.EAST);
            
            // Thêm khoảng cách giữa các item
            cartPanel.add(itemPanel);
            cartPanel.add(Box.createVerticalStrut(8));
        }
        
        // Nếu giỏ trống, hiển thị thông báo - đơn giản
        if (selectedCourseCodes.isEmpty()) {
            JLabel emptyLabel = new JLabel("<html><div style='text-align: center; color: #9CA3AF; padding: 60px 20px;'>" +
                    "<div style='font-size: 14px; line-height: 1.6;'>Chưa có học phần nào<br/>" +
                    "<span style='font-size: 12px; color: #D1D5DB;'>Thêm học phần từ danh sách bên trái</span></div>" +
                    "</div></html>", SwingConstants.CENTER);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            cartPanel.add(emptyLabel);
        }
        
        // Tính tổng số tín chỉ: đã đăng ký + chưa đăng ký trong giỏ
        int totalCredits = registeredCredits + unregisteredCartCredits;
        
        // Cập nhật tổng số tín chỉ (bao gồm cả đã đăng ký)
        lbCreditRange.setText(totalCredits + "/24 TC");
        creditProgressBar.setValue(Math.min(totalCredits, 24));
        
        // Hiển thị cảnh báo nếu < 12 TC hoặc > 24 TC
        if (totalCredits < 12) {
            lbWarning.setText("Cần đăng ký tối thiểu 12 tín chỉ để hoàn tất.");
            lbWarning.setVisible(true);
        } else if (totalCredits > 24) {
            lbWarning.setText("Tổng số tín chỉ vượt quá 24. Vui lòng xóa bớt học phần.");
            lbWarning.setVisible(true);
        } else {
            lbWarning.setVisible(false);
        }
        
        cartPanel.revalidate();
        cartPanel.repaint();
    }
    
    /**
     * Xóa tất cả học phần khỏi giỏ đăng ký
     * 
     * Quy trình:
     * 1. Hiển thị dialog xác nhận (YES/NO)
     * 2. Nếu người dùng chọn YES:
     *    - Xóa tất cả đăng ký trong Memory (nếu đã đăng ký)
     *    - Xóa tất cả mã học phần khỏi selectedCourseCodes
     *    - Refresh giỏ đăng ký và bảng học phần
     * 
     * Lưu ý: Chỉ xóa các đăng ký có trạng thái "Tạm" hoặc chưa đăng ký.
     * Các đăng ký đã gửi/đã duyệt sẽ không bị xóa (nút xóa đã bị disable).
     */
    void clearCart() {
        int result = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa tất cả học phần trong giỏ?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            String term = (String) cbTerm.getSelectedItem();
            if (term != null) {
                // Xóa tất cả đăng ký
                Memory.deleteByCourseCodes(student.studentId, term, new HashSet<>(selectedCourseCodes));
            }
            selectedCourseCodes.clear();
            refreshCart();
            refreshCourseTable();
        }
    }
    
    /**
     * Refresh (làm mới) toàn bộ UI
     * 
     * Phương thức này refresh cả bảng học phần và giỏ đăng ký.
     * Được gọi khi thay đổi học kỳ.
     * 
     * Lưu ý: Không tự động load các môn đã đăng ký vào selectedCourseCodes.
     * Chỉ giữ lại các môn đã được thêm vào giỏ trong phiên hiện tại.
     * Điều này giúp sinh viên có thể chọn lại các môn đã đăng ký nếu muốn.
     */
    void refreshAll() {
        // Không tự động load các môn đã đăng ký vào selectedCourseCodes
        // Chỉ giữ lại các môn đã được thêm vào giỏ trong phiên hiện tại
        currentPage = 1;
        
        refreshCourseTable();
        refreshCart();
    }
    
    /**
     * Gửi đăng ký học phần để PĐT duyệt
     * 
     * Quy trình xử lý:
     * 1. Kiểm tra học kỳ đã được chọn chưa
     * 2. Kiểm tra giỏ đăng ký có rỗng không
     * 3. Tính tổng số tín chỉ:
     *    - Đã đăng ký + mới submit (chỉ tính các môn chưa đăng ký)
     *    - Kiểm tra: >= 12 TC và <= 24 TC
     * 4. Kiểm tra học kỳ có đang mở đăng ký không
     * 5. Với mỗi học phần trong giỏ:
     *    - Nếu đã đăng ký và trạng thái là "Tạm": cập nhật thành "Đã gửi"
     *    - Nếu chưa đăng ký: thêm mới RegItem với trạng thái "Đã gửi"
     * 6. Hiển thị thông báo thành công và refresh UI
     * 
     * Sau khi gửi:
     * - Trạng thái các học phần chuyển thành "Đã gửi"
     * - PĐT có thể xem và duyệt/từ chối trong màn hình "Duyệt đăng ký học phần"
     * - Sinh viên vẫn có thể xem trong giỏ nhưng không thể xóa (nút xóa bị disable)
     */
    void submit() {
        String term = (String) cbTerm.getSelectedItem();
        if (term == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn học kỳ.");
            return;
        }
        
        if (selectedCourseCodes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ đăng ký trống.");
            return;
        }
        
        // Lấy danh sách đăng ký hiện tại (dùng chung cho cả kiểm tra và submit)
        var existingRegs = Memory.loadReg(student.studentId, term);
        Set<String> registeredCodes = new HashSet<>();
        Map<String, RegItem> existingMap = new HashMap<>();
        for (RegItem item : existingRegs) {
            registeredCodes.add(item.course.code);
            existingMap.put(item.course.code, item);
        }
        
        // Tính tổng số tín chỉ: đã đăng ký + mới submit (chỉ tính các môn chưa đăng ký)
        int registeredCredits = getTotalRegisteredCredits(term);
        
        // Chỉ tính các môn chưa đăng ký trong selectedCourseCodes
        int newCredits = selectedCourseCodes.stream()
                .filter(code -> !registeredCodes.contains(code))
                .mapToInt(code -> {
                    Course c = Memory.courses.get(code);
                    return c != null ? c.credits : 0;
                })
                .sum();
        int totalCredits = registeredCredits + newCredits;
        
        if (totalCredits < 12) {
            JOptionPane.showMessageDialog(this, 
                    "Cần đăng ký tối thiểu 12 tín chỉ để hoàn tất. " +
                    "Hiện tại: " + totalCredits + "/24 TC.");
            return;
        }
        
        if (totalCredits > 24) {
            JOptionPane.showMessageDialog(this, 
                    "Tổng số tín chỉ không được vượt quá 24. " +
                    "Đã đăng ký: " + registeredCredits + " TC. " +
                    "Mới thêm: " + newCredits + " TC. " +
                    "Tổng: " + totalCredits + " TC.");
            return;
        }
        
        // Kiểm tra học kỳ có mở không
        if (!Memory.isTermOpen(term)) {
            String errorMsg = Memory.getTermOpenErrorMessage(term);
            JOptionPane.showMessageDialog(this, errorMsg, "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Thêm các học phần vào đăng ký hoặc cập nhật trạng thái
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        int successCount = 0;
        
        for (String code : selectedCourseCodes) {
            Course course = Memory.courses.get(code);
            if (course == null) continue;
            
            RegItem existing = existingMap.get(code);
            if (existing != null) {
                // Nếu đã đăng ký, cập nhật trạng thái thành "Đã gửi" nếu đang là "Tạm"
                if ("Tạm".equals(existing.status)) {
                    existing.status = "Đã gửi";
                    existing.date = today; // Cập nhật ngày đăng ký
                    successCount++;
                }
                // Nếu đã có trạng thái khác (Đã gửi, Đã duyệt, v.v.), không làm gì
            } else {
                // Nếu chưa đăng ký, thêm mới với trạng thái "Đã gửi"
                boolean ok = Memory.addReg(student.studentId, term, 
                        new RegItem(course, today, "Đã gửi"));
                if (ok) successCount++;
            }
        }
        
        if (successCount > 0) {
            JOptionPane.showMessageDialog(this, 
                    "Đã gửi đăng ký " + successCount + " học phần thành công!", 
                    "Thành công", 
                    JOptionPane.INFORMATION_MESSAGE);
            // Không xóa giỏ, chỉ refresh để hiển thị trạng thái "Đã gửi"
            refreshCart();
            refreshCourseTable();
            refreshHistory(); // Refresh lịch sử để hiển thị ngay
        } else {
            JOptionPane.showMessageDialog(this, 
                    "Không thể đăng ký. Có thể bạn đã đăng ký một số học phần này.");
        }
    }
    
    /**
     * Custom cell renderer cho cột "HÀNH ĐỘNG" trong bảng học phần
     * 
     * Renderer này tạo một JButton trong mỗi ô của cột "HÀNH ĐỘNG":
     * - Nếu học phần đã trong giỏ: hiển thị nút "ĐÃ CHỌN" (màu xám, disabled)
     * - Nếu học phần chưa trong giỏ:
     *   + Nếu tổng tín chỉ + tín chỉ môn này <= 24: hiển thị nút "THÊM" (màu xanh, enabled)
     *   + Nếu tổng tín chỉ + tín chỉ môn này > 24: hiển thị nút "THÊM" (màu xám, disabled)
     * 
     * Renderer chỉ chịu trách nhiệm hiển thị (render), không xử lý sự kiện click.
     * Sự kiện click được xử lý bởi ButtonCellEditor.
     */
    class ButtonCellRenderer implements javax.swing.table.TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // Tạo panel để chứa button
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            panel.setOpaque(true);
            panel.setBackground(isSelected ? table.getSelectionBackground() : 
                    (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            
            if (row >= 0 && row < table.getRowCount()) {
                String courseCode = (String) table.getValueAt(row, 0);
                if (courseCode != null) {
                    boolean isInCart = selectedCourseCodes.contains(courseCode);
                    
                    // Kiểm tra tổng số tín chỉ để quyết định có cho phép thêm không
                    String term = (String) cbTerm.getSelectedItem();
                    int registeredCredits = term != null ? getTotalRegisteredCredits(term) : 0;
                    int cartCredits = getTotalCartCredits();
                    Course course = Memory.courses.get(courseCode);
                    int currentTotal = registeredCredits + cartCredits;
                    boolean canAdd = !isInCart && course != null && (currentTotal + course.credits) <= 24;
                    
                    JButton btn = new JButton(isInCart ? "ĐÃ CHỌN" : "THÊM");
                    btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    
                    if (isInCart) {
                        // Nút "ĐÃ CHỌN" - màu xám nhạt, không click được
                        btn.setEnabled(false);
                        btn.setBackground(new Color(229, 231, 235));
                        btn.setForeground(new Color(107, 114, 128));
                        btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    } else if (!canAdd) {
                        // Nút "THÊM" - disable nếu đã đạt 24 tín chỉ
                        btn.setEnabled(false);
                        btn.setBackground(new Color(243, 244, 246));
                        btn.setForeground(new Color(156, 163, 175));
                        btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    } else {
                        // Nút "THÊM" - màu xanh, có thể click
                        btn.setEnabled(true);
                        btn.setBackground(new Color(59, 130, 246));
                        btn.setForeground(Color.WHITE);
                        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    }
                    
                    btn.setBorderPainted(false);
                    btn.setFocusPainted(false);
                    btn.setPreferredSize(new Dimension(100, 32));
                    btn.setOpaque(true);
                    
                    panel.add(btn);
                }
            }
            
            return panel;
        }
    }
    
    /**
     * Custom cell editor cho cột "HÀNH ĐỘNG" trong bảng học phần
     * 
     * Editor này xử lý sự kiện click vào nút trong cột "HÀNH ĐỘNG":
     * - Khi click vào nút "THÊM" (enabled): gọi addToCart() để thêm học phần vào giỏ
     * - Sau khi thêm, dừng editing và refresh bảng
     * 
     * Editor tạo JButton tương tự như Renderer, nhưng có thêm ActionListener
     * để xử lý sự kiện click.
     * 
     * Lưu ý: Editor chỉ được kích hoạt khi click vào ô (courseTable.editCellAt()).
     */
    class ButtonCellEditor extends javax.swing.AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private JButton btn;
        private String currentCode;
        private JTable table;
        
        @Override
        public Object getCellEditorValue() {
            return currentCode;
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.table = table;
            currentCode = (String) table.getValueAt(row, 0);
            boolean isInCart = selectedCourseCodes.contains(currentCode);
            
            // Kiểm tra tổng số tín chỉ để quyết định có cho phép thêm không
            String term = (String) cbTerm.getSelectedItem();
            int registeredCredits = term != null ? getTotalRegisteredCredits(term) : 0;
            int cartCredits = getTotalCartCredits();
            Course course = Memory.courses.get(currentCode);
            int currentTotal = registeredCredits + cartCredits;
            boolean canAdd = !isInCart && course != null && (currentTotal + course.credits) <= 24;
            
            btn = new JButton(isInCart ? "ĐÃ CHỌN" : "THÊM");
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            
            if (isInCart) {
                // Nút "ĐÃ CHỌN" - màu xám nhạt, không click được
                btn.setEnabled(false);
                btn.setBackground(new Color(229, 231, 235));
                btn.setForeground(new Color(107, 114, 128));
                btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            } else if (!canAdd) {
                // Nút "THÊM" - disable nếu đã đạt 24 tín chỉ
                btn.setEnabled(false);
                btn.setBackground(new Color(243, 244, 246));
                btn.setForeground(new Color(156, 163, 175));
                btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            } else {
                // Nút "THÊM" - màu xanh, có thể click
                btn.setEnabled(true);
                btn.setBackground(new Color(59, 130, 246));
                btn.setForeground(Color.WHITE);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(100, 32));
            
            btn.addActionListener(e -> {
                if (!isInCart && canAdd) {
                    addToCart(currentCode);
                    stopCellEditing();
                }
            });
            
            return btn;
        }
    }
}
