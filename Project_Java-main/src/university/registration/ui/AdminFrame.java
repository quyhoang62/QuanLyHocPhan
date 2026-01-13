package university.registration.ui;

import university.registration.model.Course;
import university.registration.model.Offering;
import university.registration.model.RegItem;
import university.registration.model.Student;
import university.registration.model.TermSetting;
import university.registration.store.Memory;
import university.registration.ui.components.CardPanel;
import university.registration.ui.components.PlaceholderTextField;
import java.text.SimpleDateFormat;

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
 * Màn hình quản trị dành cho Phòng Đào Tạo (PĐT) - Admin
 * 
 * Đây là màn hình chính mà admin (PĐT) sử dụng để quản lý toàn bộ hệ thống đăng ký học phần.
 * 
 * Cấu trúc màn hình:
 * - Header bar: Tiêu đề "PHÒNG ĐÀO TẠO" ở trên cùng
 * - Sidebar navigation menu (bên trái): Menu điều hướng với 4 mục chính
 * - Content area (bên phải): Hiển thị nội dung tương ứng với menu được chọn
 * - Footer: Thông tin bản quyền
 * 
 * 4 màn hình chính (sử dụng CardLayout để chuyển đổi):
 * 1. "Quản lý học phần" (COURSE_MANAGEMENT):
 *    - Quản lý học phần mở trong từng học kỳ
 *    - Xem danh sách học phần, số lượng đăng ký
 *    - Tạo học phần mới, cập nhật Offering (mở/đóng lớp)
 * 
 * 2. "Duyệt đăng ký học phần" (APPROVAL):
 *    - Xem tất cả đăng ký của sinh viên
 *    - Lọc theo học kỳ, trạng thái, khoa/viện
 *    - Duyệt/từ chối đăng ký học phần
 * 
 * 3. "Cài đặt học phần" (COURSE_SETTINGS):
 *    - Quản lý danh sách học phần master (không phụ thuộc học kỳ)
 *    - Thêm/sửa/xóa học phần
 *    - Xem thống kê số lượng sinh viên đã đăng ký
 * 
 * 4. "Cài đặt kỳ học" (TERM_SETTINGS):
 *    - Quản lý học kỳ (thêm/sửa/xóa)
 *    - Mở/đóng đăng ký cho từng học kỳ
 *    - Xem thống kê học kỳ
 * 
 * Thiết kế:
 * - Giao diện hiện đại, đơn giản, đẹp
 * - Sidebar menu với highlight cho mục được chọn
 * - CardLayout để chuyển đổi giữa các màn hình (không cần tạo nhiều frame)
 * - Responsive và dễ sử dụng
 */
public class AdminFrame extends JFrame {

    /**
     * CardLayout để chuyển đổi giữa các panel (màn hình) khác nhau
     * 
     * CardLayout cho phép chuyển đổi giữa các panel mà không cần tạo nhiều frame.
     * Khi click vào menu item, chỉ cần gọi cardLayout.show() để hiển thị panel tương ứng.
     */
    CardLayout cardLayout;
    
    /**
     * Panel chứa tất cả các màn hình con (sử dụng CardLayout)
     * 
     * Chứa 4 panel:
     * - "COURSE_MANAGEMENT": Quản lý học phần
     * - "APPROVAL": Duyệt đăng ký
     * - "COURSE_SETTINGS": Cài đặt học phần
     * - "TERM_SETTINGS": Cài đặt kỳ học
     */
    JPanel contentPanel;
    
    /**
     * Các nút menu điều hướng trong sidebar
     * 
     * - btnMenuCourseManagement: Nút "Quản lý học phần"
     * - btnMenuApproval: Nút "Duyệt đăng ký học phần"
     * - btnMenuCourseSettings: Nút "Cài đặt học phần"
     * - btnMenuTermSettings: Nút "Cài đặt kỳ học"
     * - btnMenuLogout: Nút "Đăng xuất" (ở cuối sidebar)
     */
    JButton btnMenuCourseManagement, btnMenuApproval, btnMenuCourseSettings, btnMenuTermSettings, btnMenuLogout;
    
    // ========== TAB 1: QUẢN LÝ HỌC PHẦN MỞ TRONG KỲ ==========
    
    /**
     * ComboBox chọn học kỳ để quản lý học phần
     * 
     * Admin chọn học kỳ từ danh sách, sau đó xem/cập nhật các học phần mở trong kỳ đó.
     */
    JComboBox<String> cbTermCourse = new JComboBox<>();
    
    /**
     * Spinner chọn ngày bắt đầu đăng ký (filter)
     * 
     * Dùng để lọc học phần theo thời gian mở đăng ký (chức năng filter, có thể chưa được sử dụng đầy đủ).
     */
    JSpinner spRegStartDate = new JSpinner(new SpinnerDateModel());
    
    /**
     * Spinner chọn ngày kết thúc đăng ký (filter)
     * 
     * Dùng để lọc học phần theo thời gian mở đăng ký (chức năng filter, có thể chưa được sử dụng đầy đủ).
     */
    JSpinner spRegEndDate = new JSpinner(new SpinnerDateModel());
    
    /**
     * Bảng hiển thị danh sách học phần trong học kỳ đã chọn
     * 
     * Các cột: Mã học phần, Tên học phần, Loại học phần, Thời gian mở,
     * Trạng thái, Số lượng đăng ký, Hành động
     */
    JTable courseTable;
    
    /**
     * Model dữ liệu cho bảng học phần (courseTable)
     */
    DefaultTableModel courseModel;
    
    /**
     * TextField tìm kiếm học phần (theo mã hoặc tên)
     */
    JTextField searchCourseField = new JTextField();
    
    /**
     * ComboBox lọc học phần theo trạng thái mở/đóng
     * 
     * Các giá trị: "Tất cả", "Đang mở", "Đóng"
     */
    JComboBox<String> cbCourseStatus = new JComboBox<>(new String[]{"Tất cả", "Đang mở", "Đóng"});
    
    /**
     * Form tạo học phần mới
     * 
     * Các trường:
     * - tfCode: Mã học phần
     * - tfName: Tên học phần
     * - spCredits: Số tín chỉ (1-10)
     * - cbType: Loại học phần (Bắt buộc, Tự chọn, Cơ sở)
     * - spCourseStartDate: Ngày bắt đầu (có thể chưa được sử dụng)
     * - spCourseEndDate: Ngày kết thúc (có thể chưa được sử dụng)
     */
    JTextField tfCode = new JTextField();
    JTextField tfName = new JTextField();
    JSpinner spCredits = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    JComboBox<String> cbType = new JComboBox<>(new String[]{"Bắt buộc", "Tự chọn", "Cơ sở"});
    JSpinner spCourseStartDate = new JSpinner(new SpinnerDateModel());
    JSpinner spCourseEndDate = new JSpinner(new SpinnerDateModel());
    
    // ========== TAB 2: DUYỆT ĐĂNG KÝ HỌC PHẦN ==========
    
    /**
     * Bảng hiển thị danh sách đăng ký học phần của sinh viên
     * 
     * Các cột: Mã ĐK, Tên sinh viên, Mã SV, Tên học phần, Tín chỉ,
     * Thời gian ĐK, Trạng thái, Học kỳ, Hành động
     * 
     * Cột "Hành động" chứa các nút: Duyệt (✓), Từ chối (✗), Sửa (✎)
     */
    JTable approvalTable;
    
    /**
     * Model dữ liệu cho bảng duyệt đăng ký (approvalTable)
     */
    DefaultTableModel approvalModel;
    
    /**
     * TextField tìm kiếm đăng ký (theo tên/MSSV sinh viên, mã/tên học phần)
     */
    JTextField searchApprovalField = new JTextField();
    
    /**
     * ComboBox lọc đăng ký theo trạng thái
     * 
     * Các giá trị: "Tất cả", "Chờ duyệt", "Đã duyệt", "Từ chối"
     */
    JComboBox<String> cbApprovalStatus = new JComboBox<>(new String[]{"Tất cả", "Chờ duyệt", "Đã duyệt", "Từ chối"});
    
    /**
     * ComboBox lọc đăng ký theo học kỳ
     * 
     * Các giá trị: "Tất cả" + danh sách các học kỳ
     */
    JComboBox<String> cbTermApproval = new JComboBox<>();
    
    /**
     * ComboBox lọc đăng ký theo khoa/viện (chương trình đào tạo)
     * 
     * Các giá trị: "Tất cả" + danh sách các chương trình đào tạo
     */
    JComboBox<String> cbDeptApproval = new JComboBox<>(new String[]{"Tất cả"});
    
    // ========== TAB 3: CÀI ĐẶT HỌC PHẦN ==========
    
    /**
     * Bảng hiển thị danh sách tất cả học phần master (không phụ thuộc học kỳ)
     * 
     * Admin có thể xem, thêm, sửa, xóa học phần trong bảng này.
     */
    JTable settingsCourseTable;
    
    /**
     * Model dữ liệu cho bảng cài đặt học phần (settingsCourseTable)
     */
    DefaultTableModel settingsCourseModel;
    
    /**
     * TextField tìm kiếm học phần (theo mã hoặc tên)
     */
    JTextField searchSettingsField = new JTextField();
    
    /**
     * ComboBox lọc học phần theo loại
     * 
     * Các giá trị: "Tất cả", "Bắt buộc", "Tự chọn", "Cơ sở"
     */
    JComboBox<String> cbSettingsType = new JComboBox<>(new String[]{"Tất cả", "Bắt buộc", "Tự chọn", "Cơ sở"});
    
    /**
     * Form cài đặt học phần (thêm/sửa học phần master)
     * 
     * Các trường:
     * - tfSettingsCode: Mã học phần
     * - tfSettingsName: Tên học phần
     * - spSettingsCredits: Số tín chỉ (1-10)
     * - cbSettingsTypeForm: Loại học phần (Bắt buộc, Tự chọn, Cơ sở)
     */
    JTextField tfSettingsCode = new JTextField();
    JTextField tfSettingsName = new JTextField();
    JSpinner spSettingsCredits = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
    JComboBox<String> cbSettingsTypeForm = new JComboBox<>(new String[]{"Bắt buộc", "Tự chọn", "Cơ sở"});
    
    // ========== TAB 4: CÀI ĐẶT KỲ HỌC ==========
    
    /**
     * Bảng hiển thị danh sách các học kỳ và cấu hình của chúng
     * 
     * Admin có thể xem, thêm, sửa, xóa học kỳ, mở/đóng đăng ký cho từng học kỳ.
     */
    JTable termSettingsTable;
    
    /**
     * Model dữ liệu cho bảng cài đặt kỳ học (termSettingsTable)
     */
    DefaultTableModel termSettingsModel;
    
    /**
     * TextField tìm kiếm học kỳ (theo mã học kỳ hoặc tên)
     */
    JTextField searchTermField = new JTextField();
    
    /**
     * ComboBox lọc học kỳ theo năm học
     * 
     * Các giá trị: "Tất cả năm học" + danh sách các năm học
     */
    JComboBox<String> cbTermAcademicYear = new JComboBox<>(new String[]{"Tất cả năm học"});
    
    /**
     * ComboBox lọc học kỳ theo trạng thái hoạt động
     * 
     * Các giá trị: "Tất cả trạng thái", "Đang hoạt động", "Đã kết thúc"
     */
    JComboBox<String> cbTermStatus = new JComboBox<>(new String[]{"Tất cả trạng thái", "Đang hoạt động", "Đã kết thúc"});
    

    /**
     * Constructor: Tạo màn hình quản trị cho Phòng Đào Tạo
     * 
     * Khởi tạo toàn bộ UI components, tạo 4 màn hình chính và hiển thị màn hình đầu tiên.
     * 
     * @param owner Frame cha (thường là LoginFrame) - dùng để đặt vị trí cửa sổ
     * 
     * Quy trình khởi tạo:
     * 1. Thiết lập thuộc tính cửa sổ (title, size, close operation)
     * 2. Tạo header bar với tiêu đề "PHÒNG ĐÀO TẠO"
     * 3. Tạo sidebar navigation menu với 4 mục chính
     * 4. Tạo content panel với CardLayout
     * 5. Tạo 4 màn hình con:
     *    - Quản lý học phần
     *    - Duyệt đăng ký học phần
     *    - Cài đặt học phần
     *    - Cài đặt kỳ học
     * 6. Load dữ liệu ban đầu cho tất cả các màn hình
     * 7. Hiển thị màn hình đầu tiên ("Quản lý học phần")
     */
    public AdminFrame(JFrame owner){
        setTitle("Phòng Đào Tạo – Quản lý đăng ký học phần");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        setBackground(new Color(249, 250, 251));
        setLayout(new BorderLayout());

        // ========== TOP HEADER BAR ==========
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Color.WHITE);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
                new EmptyBorder(16, 24, 16, 24)
        ));
        
        JLabel headerTitle = new JLabel("PHÒNG ĐÀO TẠO");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerTitle.setForeground(new Color(31, 41, 55));
        
        headerBar.add(headerTitle, BorderLayout.WEST);
        add(headerBar, BorderLayout.NORTH);

        // ========== SIDEBAR NAVIGATION MENU ==========
        JPanel sidebar = createSidebarNavigation();
        
        // ========== MAIN CONTENT WITH CARD LAYOUT ==========
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(new Color(249, 250, 251));
        contentPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        
        // Tạo các panel
        JPanel courseManagementPanel = createCourseManagementPanel();
        JPanel approvalPanel = createApprovalPanel();
        JPanel courseSettingsPanel = createCourseSettingsPanel();
        JPanel termSettingsPanel = createTermSettingsPanel();
        
        // Thêm các panel vào CardLayout
        contentPanel.add(courseManagementPanel, "COURSE_MANAGEMENT");
        contentPanel.add(approvalPanel, "APPROVAL");
        contentPanel.add(courseSettingsPanel, "COURSE_SETTINGS");
        contentPanel.add(termSettingsPanel, "TERM_SETTINGS");
        
        // Hiển thị panel đầu tiên
        cardLayout.show(contentPanel, "COURSE_MANAGEMENT");
        selectMenuButton(btnMenuCourseManagement);
        
        // Footer
        JPanel footer = new JPanel();
        footer.setBackground(new Color(249, 250, 251));
        footer.setBorder(new EmptyBorder(16, 0, 16, 0));
        JLabel footerText = new JLabel("© 2025 University Portal. All rights reserved.");
        footerText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerText.setForeground(new Color(107, 114, 128));
        footer.add(footerText);
        
        // Panel chứa content và footer (bên phải sidebar)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(249, 250, 251));
        rightPanel.add(contentPanel, BorderLayout.CENTER);
        rightPanel.add(footer, BorderLayout.SOUTH);
        
        // Container chứa sidebar và rightPanel, sidebar sẽ kéo dài xuống footer
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(249, 250, 251));
        centerContainer.add(sidebar, BorderLayout.WEST);
        centerContainer.add(rightPanel, BorderLayout.CENTER);
        
        add(centerContainer, BorderLayout.CENTER);

        // Load dữ liệu ban đầu
        loadTerms();
        refreshCourseTable();
        filterApprovalTable();
        refreshSettingsCourseTable();
        refreshTermSettingsTable();

        setVisible(true);
    }

    /**
     * Tạo sidebar navigation menu (menu điều hướng bên trái)
     * 
     * Sidebar chứa:
     * - 4 nút menu chính: Quản lý học phần, Duyệt đăng ký, Cài đặt học phần, Cài đặt kỳ học
     * - Nút đăng xuất ở cuối sidebar
     * 
     * Khi click vào menu item:
     * - Highlight menu item được chọn (màu xanh)
     * - Chuyển sang màn hình tương ứng (sử dụng CardLayout)
     * 
     * @return Panel chứa sidebar navigation menu
     */
    JPanel createSidebarNavigation() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(229, 231, 235)));
        sidebar.setMinimumSize(new Dimension(280, 0));
        
        // Menu items panel
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(new EmptyBorder(0, 12, 12, 12));
        
        // Menu items
        btnMenuCourseManagement = createMenuButton("Quản lý học phần", true);
        btnMenuApproval = createMenuButton("Duyệt đăng ký học phần", false);
        btnMenuCourseSettings = createMenuButton("Cài đặt học phần", false);
        btnMenuTermSettings = createMenuButton("Cài đặt kỳ học", false);
        
        // Add action listeners
        btnMenuCourseManagement.addActionListener(e -> {
            cardLayout.show(contentPanel, "COURSE_MANAGEMENT");
            selectMenuButton(btnMenuCourseManagement);
        });
        
        btnMenuApproval.addActionListener(e -> {
            cardLayout.show(contentPanel, "APPROVAL");
            selectMenuButton(btnMenuApproval);
        });
        
        btnMenuCourseSettings.addActionListener(e -> {
            cardLayout.show(contentPanel, "COURSE_SETTINGS");
            selectMenuButton(btnMenuCourseSettings);
        });
        
        btnMenuTermSettings.addActionListener(e -> {
            cardLayout.show(contentPanel, "TERM_SETTINGS");
            selectMenuButton(btnMenuTermSettings);
        });
        
        menuPanel.add(btnMenuCourseManagement);
        menuPanel.add(Box.createVerticalStrut(4));
        menuPanel.add(btnMenuApproval);
        menuPanel.add(Box.createVerticalStrut(4));
        menuPanel.add(btnMenuCourseSettings);
        menuPanel.add(Box.createVerticalStrut(4));
        menuPanel.add(btnMenuTermSettings);
        
        // Scroll pane cho menu items
        JScrollPane menuScrollPane = new JScrollPane(menuPanel);
        menuScrollPane.setBorder(BorderFactory.createEmptyBorder());
        menuScrollPane.setOpaque(false);
        menuScrollPane.getViewport().setOpaque(false);
        menuScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        menuScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Logout button at bottom
        JPanel logoutPanel = new JPanel(new BorderLayout());
        logoutPanel.setBackground(Color.WHITE);
        logoutPanel.setBorder(new EmptyBorder(12, 12, 20, 12));
        
        btnMenuLogout = createLogoutButton("Đăng xuất");
        btnMenuLogout.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        
        logoutPanel.add(btnMenuLogout, BorderLayout.SOUTH);
        
        sidebar.add(menuScrollPane, BorderLayout.CENTER);
        sidebar.add(logoutPanel, BorderLayout.SOUTH);
        
        return sidebar;
    }
    
    /**
     * Tạo nút menu
     * 
     * Nút menu có các đặc điểm:
     * - Font: Segoe UI, size 15
     * - Căn trái text
     * - Kích thước: 256x44px
     * - Màu sắc thay đổi theo trạng thái:
     *   + Được chọn: nền xanh nhạt (#EFF6FF), chữ xanh (#3B82F6)
     *   + Không được chọn: nền trắng, chữ xám (#4B5563)
     * - Hover effect: đổi màu khi hover (chỉ cho nút không được chọn)
     * 
     * @param text Text hiển thị trên nút (ví dụ: "Quản lý học phần")
     * @param selected true nếu nút được chọn (highlight), false nếu không
     * @return JButton với style menu đã được tùy chỉnh
     */
    JButton createMenuButton(String text, boolean selected) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setPreferredSize(new Dimension(256, 44));
        btn.setMaximumSize(new Dimension(256, 44));
        btn.setMinimumSize(new Dimension(256, 44));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));
        btn.setOpaque(true);
        
        if (selected) {
            btn.setBackground(new Color(239, 246, 255));
            btn.setForeground(new Color(59, 130, 246));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(75, 85, 99));
        }
        
        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(new Color(239, 246, 255))) {
                    btn.setBackground(new Color(249, 250, 251));
                    btn.setForeground(new Color(31, 41, 55));
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (selected) {
                    btn.setBackground(new Color(239, 246, 255));
                    btn.setForeground(new Color(59, 130, 246));
                } else {
                    btn.setBackground(Color.WHITE);
                    btn.setForeground(new Color(75, 85, 99));
                }
            }
        });
        
        return btn;
    }
    
    /**
     * Tạo nút đăng xuất
     * 
     * Nút đăng xuất có style khác với nút menu thông thường:
     * - Màu nền: đỏ (#EF4444)
     * - Màu chữ: trắng
     * - Hover effect: đổi sang màu đỏ đậm hơn (#DC2626)
     * 
     * @param text Text hiển thị trên nút (thường là "Đăng xuất")
     * @return JButton với style đăng xuất (màu đỏ)
     */
    JButton createLogoutButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setPreferredSize(new Dimension(256, 44));
        btn.setMaximumSize(new Dimension(256, 44));
        btn.setMinimumSize(new Dimension(256, 44));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));
        btn.setOpaque(true);
        btn.setBackground(new Color(239, 68, 68));
        btn.setForeground(Color.WHITE);
        
        // Hover effect for logout
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(220, 38, 38));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(239, 68, 68));
            }
        });
        
        return btn;
    }
    
    /**
     * Chọn menu button và bỏ chọn các button khác
     * 
     * Phương thức này:
     * 1. Reset tất cả menu buttons về trạng thái không được chọn (nền trắng, chữ xám)
     * 2. Highlight menu button được chọn (nền xanh nhạt, chữ xanh)
     * 
     * Được gọi khi:
     * - Click vào một menu item
     * - Khởi tạo màn hình (chọn menu đầu tiên)
     * 
     * @param selectedBtn Menu button được chọn (null nếu không có)
     */
    void selectMenuButton(JButton selectedBtn) {
        // Reset all menu buttons
        btnMenuCourseManagement.setBackground(Color.WHITE);
        btnMenuCourseManagement.setForeground(new Color(75, 85, 99));
        
        btnMenuApproval.setBackground(Color.WHITE);
        btnMenuApproval.setForeground(new Color(75, 85, 99));
        
        btnMenuCourseSettings.setBackground(Color.WHITE);
        btnMenuCourseSettings.setForeground(new Color(75, 85, 99));
        
        btnMenuTermSettings.setBackground(Color.WHITE);
        btnMenuTermSettings.setForeground(new Color(75, 85, 99));
        
        // Highlight selected button
        if (selectedBtn != null && !selectedBtn.equals(btnMenuLogout)) {
            selectedBtn.setBackground(new Color(239, 246, 255));
            selectedBtn.setForeground(new Color(59, 130, 246));
        }
    }

    /**
     * Tạo panel "Quản lý học phần mở trong kỳ" (Tab 1)
     * 
     * Panel này cho phép admin:
     * - Xem danh sách học phần mở trong một học kỳ cụ thể
     * - Lọc học phần theo: học kỳ, thời gian, trạng thái mở/đóng, từ khóa tìm kiếm
     * - Xem số lượng sinh viên đã đăng ký mỗi học phần
     * - Tạo học phần mới và mở lớp cho học kỳ
     * - Cập nhật Offering (mở/đóng lớp, CTĐT được phép)
     * - Xóa học phần (nếu chưa có sinh viên đăng ký)
     * 
     * Cấu trúc:
     * - Filter section: Chọn học kỳ, thời gian, tìm kiếm, trạng thái, nút tạo học phần
     * - Table section: Bảng danh sách học phần với các cột thông tin
     * 
     * @return Panel chứa giao diện quản lý học phần mở trong kỳ
     */
    JPanel createCourseManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBackground(new Color(249, 250, 251));
        
        // ========== FILTER SECTION ==========
        CardPanel filterCard = new CardPanel();
        filterCard.setLayout(new BorderLayout(0, 16));
        filterCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        filterPanel.setOpaque(false);
        
        // Chọn kỳ học
        JLabel lbTerm = new JLabel("Học kỳ:");
        lbTerm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbTerm.setForeground(new Color(55, 65, 81));
        cbTermCourse.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbTermCourse.setPreferredSize(new Dimension(150, 36));
        cbTermCourse.addActionListener(e -> refreshCourseTable());
        
        // Tìm kiếm
        JLabel lbSearch = new JLabel("Tìm kiếm:");
        lbSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbSearch.setForeground(new Color(55, 65, 81));
        searchCourseField.setPreferredSize(new Dimension(300, 36));
        searchCourseField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchCourseField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchCourseField.setToolTipText("Tìm theo mã hoặc tên học phần...");
        searchCourseField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshCourseTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshCourseTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshCourseTable(); }
        });
        
        // Trạng thái
        JLabel lbStatus = new JLabel("Trạng thái:");
        lbStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbStatus.setForeground(new Color(55, 65, 81));
        cbCourseStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbCourseStatus.setPreferredSize(new Dimension(140, 36));
        cbCourseStatus.addActionListener(e -> refreshCourseTable());
        
        filterPanel.add(lbTerm);
        filterPanel.add(cbTermCourse);
        filterPanel.add(lbSearch);
        filterPanel.add(searchCourseField);
        filterPanel.add(lbStatus);
        filterPanel.add(cbCourseStatus);
        
        // Panel cho các action buttons (bên phải)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setOpaque(false);
        
        // Nút Mở tất cả
        JButton btnOpenAll = new JButton("Mở tất cả");
        btnOpenAll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnOpenAll.setBackground(new Color(34, 197, 94));
        btnOpenAll.setForeground(Color.WHITE);
        btnOpenAll.setBorderPainted(false);
        btnOpenAll.setPreferredSize(new Dimension(130, 36));
        btnOpenAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpenAll.addActionListener(e -> openAllCourses());
        
        // Hover effect cho nút Mở tất cả
        btnOpenAll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnOpenAll.setBackground(new Color(22, 163, 74));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnOpenAll.setBackground(new Color(34, 197, 94));
            }
        });
        
        // Nút Đóng tất cả
        JButton btnCloseAll = new JButton("Đóng tất cả");
        btnCloseAll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCloseAll.setBackground(new Color(239, 68, 68));
        btnCloseAll.setForeground(Color.WHITE);
        btnCloseAll.setBorderPainted(false);
        btnCloseAll.setPreferredSize(new Dimension(130, 36));
        btnCloseAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCloseAll.addActionListener(e -> closeAllCourses());
        
        // Hover effect cho nút Đóng tất cả
        btnCloseAll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnCloseAll.setBackground(new Color(220, 38, 38));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnCloseAll.setBackground(new Color(239, 68, 68));
            }
        });
        
        buttonPanel.add(btnOpenAll);
        buttonPanel.add(btnCloseAll);
        
        JPanel mainFilterPanel = new JPanel(new BorderLayout());
        mainFilterPanel.setOpaque(false);
        mainFilterPanel.add(filterPanel, BorderLayout.CENTER);
        mainFilterPanel.add(buttonPanel, BorderLayout.EAST);
        
        filterCard.add(mainFilterPanel, BorderLayout.CENTER);
        
        // ========== TABLE SECTION ==========
        courseModel = new DefaultTableModel(new Object[]{
                "Mã học phần", "Tên học phần", "Loại học phần", "Thời gian mở",
                "Trạng thái", "Số lượng đăng ký", "Hành động"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
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
        
        courseTable.setSelectionBackground(new Color(239, 246, 255));
        courseTable.setSelectionForeground(Color.BLACK);
        
        // Column widths
        courseTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        courseTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        courseTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        courseTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        courseTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        
        // Custom renderer cho cột HÀNH ĐỘNG
        courseTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : 
                    (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));

                String status = (String) table.getValueAt(row, 4);
                boolean isOpen = "Đang mở".equals(status);
                
                JButton btnToggle = new JButton(isOpen ? "Đóng" : "Mở");
                btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnToggle.setBackground(isOpen ? new Color(239, 68, 68) : new Color(34, 197, 94));
                btnToggle.setForeground(Color.WHITE);
                btnToggle.setBorderPainted(false);
                btnToggle.setPreferredSize(new Dimension(80, 32));
                btnToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnToggle.setToolTipText(isOpen ? "Đóng học phần" : "Mở học phần");

                panel.add(btnToggle);
                return panel;
            }
        });
        
        // MouseListener để detect click vào buttons
        courseTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = courseTable.rowAtPoint(e.getPoint());
                int col = courseTable.columnAtPoint(e.getPoint());
                
                if (row >= 0 && col == 6) {
                    toggleCourseStatus(row);
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(courseTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        tableScrollPane.setViewportBorder(null);

        CardPanel tableCard = new CardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel tableTitle = new JLabel("Danh sách học phần mở trong kỳ");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(new Color(31, 41, 55));
        tableTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(tableScrollPane, BorderLayout.CENTER);
        
        panel.add(filterCard, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Tạo panel "Duyệt đăng ký học phần" (Tab 2)
     * 
     * Panel này cho phép admin:
     * - Xem tất cả đăng ký học phần của sinh viên
     * - Lọc đăng ký theo: học kỳ, trạng thái, khoa/viện, từ khóa tìm kiếm
     * - Duyệt đăng ký (chuyển trạng thái thành "Đã duyệt")
     * - Từ chối đăng ký (chuyển trạng thái thành "Đã từ chối")
     * - Duyệt/từ chối hàng loạt (tất cả đăng ký đang chờ duyệt)
     * 
     * Cấu trúc:
     * - Filter section: Tìm kiếm, lọc theo trạng thái, học kỳ, khoa/viện
     * - Action buttons: "Duyệt tất cả", "Từ chối tất cả"
     * - Table section: Bảng danh sách đăng ký với cột "Hành động" chứa nút Duyệt/Từ chối/Sửa
     * 
     * Màu sắc cột "Trạng thái":
     * - Xanh lá: Đã duyệt
     * - Đỏ: Đã từ chối
     * - Vàng: Chờ duyệt (Tạm, Đã gửi)
     * 
     * @return Panel chứa giao diện duyệt đăng ký học phần
     */
    JPanel createApprovalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBackground(new Color(249, 250, 251));
        
        // ========== FILTER SECTION ==========
        CardPanel filterCard = new CardPanel();
        filterCard.setLayout(new BorderLayout());
        filterCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Panel chính chứa filters và buttons
        JPanel mainFilterPanel = new JPanel(new BorderLayout());
        mainFilterPanel.setOpaque(false);
        
        // Panel cho các filter controls
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        filterPanel.setOpaque(false);
        
        // Tìm kiếm
        JLabel lbSearch = new JLabel("Tìm kiếm:");
        lbSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbSearch.setForeground(new Color(55, 65, 81));
        searchApprovalField.setPreferredSize(new Dimension(300, 36));
        searchApprovalField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchApprovalField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchApprovalField.setToolTipText("Tìm theo Mã/Tên Sinh viên, Mã/Tên Học phần...");
        searchApprovalField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterApprovalTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterApprovalTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterApprovalTable(); }
        });
        
        // Trạng thái
        JLabel lbStatus = new JLabel("Trạng thái:");
        lbStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbStatus.setForeground(new Color(55, 65, 81));
        cbApprovalStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbApprovalStatus.setPreferredSize(new Dimension(150, 36));
        cbApprovalStatus.addActionListener(e -> filterApprovalTable());
        
        // Học kỳ
        JLabel lbTerm = new JLabel("Học kỳ:");
        lbTerm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbTerm.setForeground(new Color(55, 65, 81));
        cbTermApproval.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbTermApproval.setPreferredSize(new Dimension(140, 36));
        updateApprovalTermComboBox();
        cbTermApproval.addActionListener(e -> filterApprovalTable());
        
        // Khoa/Viện
        JLabel lbDept = new JLabel("Khoa/Viện:");
        lbDept.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbDept.setForeground(new Color(55, 65, 81));
        cbDeptApproval.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbDeptApproval.setPreferredSize(new Dimension(200, 36));
        for (String p : Memory.programs) cbDeptApproval.addItem(p);
        cbDeptApproval.addActionListener(e -> filterApprovalTable());
        
        filterPanel.add(lbSearch);
        filterPanel.add(searchApprovalField);
        filterPanel.add(lbStatus);
        filterPanel.add(cbApprovalStatus);
        filterPanel.add(lbTerm);
        filterPanel.add(cbTermApproval);
        filterPanel.add(lbDept);
        filterPanel.add(cbDeptApproval);
        
        // Panel cho các action buttons (luôn hiển thị bên phải)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setOpaque(false);
        
        // Nút Duyệt tất cả
        JButton btnApproveAll = new JButton("Duyệt tất cả");
        btnApproveAll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnApproveAll.setBackground(new Color(34, 197, 94));
        btnApproveAll.setForeground(Color.WHITE);
        btnApproveAll.setBorderPainted(false);
        btnApproveAll.setPreferredSize(new Dimension(150, 36));
        btnApproveAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnApproveAll.addActionListener(e -> approveAll());
        
        // Hover effect cho nút Duyệt tất cả
        btnApproveAll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnApproveAll.setBackground(new Color(22, 163, 74));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnApproveAll.setBackground(new Color(34, 197, 94));
            }
        });
        
        // Nút Từ chối tất cả
        JButton btnRejectAll = new JButton("Từ chối tất cả");
        btnRejectAll.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRejectAll.setBackground(new Color(239, 68, 68));
        btnRejectAll.setForeground(Color.WHITE);
        btnRejectAll.setBorderPainted(false);
        btnRejectAll.setPreferredSize(new Dimension(150, 36));
        btnRejectAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRejectAll.addActionListener(e -> rejectAll());
        
        // Hover effect cho nút Từ chối tất cả
        btnRejectAll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnRejectAll.setBackground(new Color(220, 38, 38));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnRejectAll.setBackground(new Color(239, 68, 68));
            }
        });
        
        buttonPanel.add(btnApproveAll);
        buttonPanel.add(btnRejectAll);
        
        mainFilterPanel.add(filterPanel, BorderLayout.CENTER);
        mainFilterPanel.add(buttonPanel, BorderLayout.EAST);
        
        filterCard.add(mainFilterPanel, BorderLayout.CENTER);

        // ========== TABLE SECTION ==========
        approvalModel = new DefaultTableModel(new Object[]{
                "Mã đăng ký", "Tên sinh viên", "Mã SV", "Khoa/Viện", "Mã học phần", "Tên học phần",
                "Trạng thái", "Hành động", "Học kỳ"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        approvalTable = new JTable(approvalModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                if (column == 6) { // Trạng thái ở cột 6
                    String status = (String) getValueAt(row, column);
                    if ("Đã duyệt".equals(status)) {
                        c.setBackground(new Color(220, 255, 220));
                    } else if ("Từ chối".equals(status) || "Đã từ chối".equals(status)) {
                        c.setBackground(new Color(255, 220, 220));
                    } else if ("Chờ duyệt".equals(status) || "Chờ xử lý".equals(status) || 
                               "Đã gửi".equals(status) || "Tạm".equals(status)) {
                        c.setBackground(new Color(255, 255, 220));
                    }
                }
                return c;
            }
        };
        approvalTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        approvalTable.setRowHeight(50);
        approvalTable.setShowGrid(true);
        approvalTable.setGridColor(new Color(229, 231, 235));
        
        // Custom header
        JTableHeader header = approvalTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(243, 244, 246));
        header.setForeground(new Color(31, 41, 55));
        header.setPreferredSize(new Dimension(0, 45));
        header.setReorderingAllowed(false);
        
        approvalTable.setSelectionBackground(new Color(239, 246, 255));
        approvalTable.setSelectionForeground(Color.BLACK);
        
        // Column widths
        approvalTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        approvalTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        approvalTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        approvalTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Khoa/Viện
        approvalTable.getColumnModel().getColumn(4).setPreferredWidth(120); // Mã học phần
        approvalTable.getColumnModel().getColumn(5).setPreferredWidth(300); // Tên học phần
        approvalTable.getColumnModel().getColumn(6).setPreferredWidth(120); // Trạng thái
        approvalTable.getColumnModel().getColumn(7).setPreferredWidth(200); // Hành động
        approvalTable.getColumnModel().getColumn(8).setPreferredWidth(0); // Học kỳ (ẩn)
        approvalTable.getColumnModel().getColumn(8).setMinWidth(0);
        approvalTable.getColumnModel().getColumn(8).setMaxWidth(0);
        
        // Custom renderer cho cột HÀNH ĐỘNG
        approvalTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                    boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : 
                    (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));

                String status = (String) table.getValueAt(row, 6); // Trạng thái ở cột 6
                boolean canApprove = "Chờ duyệt".equals(status) || "Chờ xử lý".equals(status) || 
                                    "Đã gửi".equals(status) || "Tạm".equals(status);
                
                if (canApprove) {
                    JButton btnApprove = new JButton("Duyệt");
                    btnApprove.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnApprove.setBackground(new Color(34, 197, 94));
                    btnApprove.setForeground(Color.WHITE);
                    btnApprove.setBorderPainted(false);
                    btnApprove.setPreferredSize(new Dimension(80, 32));
                    btnApprove.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    btnApprove.setToolTipText("Duyệt đăng ký");

                    JButton btnReject = new JButton("Từ chối");
                    btnReject.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    btnReject.setBackground(new Color(239, 68, 68));
                    btnReject.setForeground(Color.WHITE);
                    btnReject.setBorderPainted(false);
                    btnReject.setPreferredSize(new Dimension(80, 32));
                    btnReject.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    btnReject.setToolTipText("Từ chối đăng ký");

                    panel.add(btnApprove);
                    panel.add(btnReject);
                }

                return panel;
            }
        });
        
        // MouseListener để detect click vào buttons
        approvalTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = approvalTable.rowAtPoint(e.getPoint());
                int col = approvalTable.columnAtPoint(e.getPoint());
                
                if (row >= 0 && col == 7) { // Hành động ở cột 7
                    Component comp = approvalTable.prepareRenderer(
                            approvalTable.getCellRenderer(row, col), row, col);
                    Rectangle cellRect = approvalTable.getCellRect(row, col, false);
                    comp.setBounds(cellRect);
                    comp.doLayout();
                    
                    Point relativePoint = new Point(e.getX() - cellRect.x, e.getY() - cellRect.y);
                    Component clickedComp = SwingUtilities.getDeepestComponentAt(comp, relativePoint.x, relativePoint.y);
                    
                    if (clickedComp instanceof JButton) {
                        JButton clickedBtn = (JButton) clickedComp;
                        String text = clickedBtn.getText();
                        
                        if ("Duyệt".equals(text)) {
                            approveRegistration(row);
                        } else if ("Từ chối".equals(text)) {
                            rejectRegistration(row);
                        }
                    }
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(approvalTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        tableScrollPane.setViewportBorder(null);

        CardPanel tableCard = new CardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel tableTitle = new JLabel("Danh sách đăng ký học phần của sinh viên");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(new Color(31, 41, 55));
        tableTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(tableScrollPane, BorderLayout.CENTER);
        
        panel.add(filterCard, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Tạo panel "Cài đặt học phần" (Tab 3)
     * 
     * Panel này cho phép admin quản lý danh sách học phần master (không phụ thuộc học kỳ):
     * - Xem danh sách tất cả học phần trong hệ thống
     * - Tìm kiếm học phần theo mã hoặc tên
     * - Lọc học phần theo loại (Bắt buộc, Tự chọn, Cơ sở)
     * - Thêm học phần mới vào hệ thống
     * - Sửa thông tin học phần (mã, tên, số tín chỉ, loại)
     * - Xóa học phần (chỉ khi chưa có sinh viên nào đăng ký)
     * - Xem thống kê số lượng sinh viên đã đăng ký mỗi học phần
     * 
     * Cấu trúc:
     * - Filter section: Tìm kiếm, lọc theo loại, nút thêm học phần
     * - Table section: Bảng danh sách học phần với các cột thông tin
     * - Form section: Form thêm/sửa học phần (có thể là dialog hoặc panel)
     * 
     * Lưu ý: Học phần master khác với Offering (mở lớp trong học kỳ).
     * Một học phần master có thể được mở lớp ở nhiều học kỳ khác nhau.
     * 
     * @return Panel chứa giao diện cài đặt học phần
     */
    JPanel createCourseSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBackground(new Color(249, 250, 251));
        
        // ========== FILTER SECTION ==========
        CardPanel filterCard = new CardPanel();
        filterCard.setLayout(new BorderLayout());
        filterCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        filterPanel.setOpaque(false);
        
        // Tìm kiếm
        JLabel lbSearch = new JLabel("Tìm kiếm:");
        lbSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbSearch.setForeground(new Color(55, 65, 81));
        searchSettingsField.setPreferredSize(new Dimension(300, 36));
        searchSettingsField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchSettingsField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchSettingsField.setToolTipText("Tìm theo mã hoặc tên học phần...");
        searchSettingsField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshSettingsCourseTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshSettingsCourseTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshSettingsCourseTable(); }
        });
        
        // Loại học phần
        JLabel lbType = new JLabel("Loại học phần:");
        lbType.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbType.setForeground(new Color(55, 65, 81));
        cbSettingsType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbSettingsType.setPreferredSize(new Dimension(150, 36));
        cbSettingsType.addActionListener(e -> refreshSettingsCourseTable());
        
        filterPanel.add(lbSearch);
        filterPanel.add(searchSettingsField);
        filterPanel.add(lbType);
        filterPanel.add(cbSettingsType);
        
        filterCard.add(filterPanel, BorderLayout.CENTER);
        
        // ========== FORM SECTION ==========
        CardPanel formCard = new CardPanel();
        formCard.setLayout(new BorderLayout());
        formCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel formTitle = new JLabel("Thông tin học phần");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setForeground(new Color(31, 41, 55));
        formTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        
        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);
        
        JPanel codePanel = createFieldGroup("Mã học phần", tfSettingsCode, 200, labelFont, inputFont);
        JPanel namePanel = createFieldGroup("Tên học phần", tfSettingsName, 400, labelFont, inputFont);
        JPanel creditsPanel = createSpinnerGroup("Số tín chỉ", spSettingsCredits, 100, labelFont, inputFont);
        JPanel typePanel = createComboBoxGroup("Loại học phần", cbSettingsTypeForm, 150, labelFont, inputFont);
        
        formPanel.add(codePanel);
        formPanel.add(Box.createVerticalStrut(16));
        formPanel.add(namePanel);
        formPanel.add(Box.createVerticalStrut(16));
        formPanel.add(creditsPanel);
        formPanel.add(Box.createVerticalStrut(16));
        formPanel.add(typePanel);
        formPanel.add(Box.createVerticalStrut(20));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonPanel.setOpaque(false);
        
        JButton btnAdd = new JButton("Thêm mới");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setBackground(new Color(59, 130, 246));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setBorderPainted(false);
        btnAdd.setPreferredSize(new Dimension(120, 36));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> addSettingsCourse());
        
        JButton btnUpdate = new JButton("Cập nhật");
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnUpdate.setBackground(new Color(16, 185, 129));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setBorderPainted(false);
        btnUpdate.setPreferredSize(new Dimension(120, 36));
        btnUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpdate.addActionListener(e -> updateSettingsCourse());
        
        JButton btnDelete = new JButton("Xóa");
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setBorderPainted(false);
        btnDelete.setPreferredSize(new Dimension(120, 36));
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.addActionListener(e -> deleteSettingsCourse());
        
        JButton btnClear = new JButton("Làm mới");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnClear.setBackground(Color.WHITE);
        btnClear.setForeground(new Color(107, 114, 128));
        btnClear.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        btnClear.setPreferredSize(new Dimension(120, 36));
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> clearSettingsForm());
        
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);
        
        formPanel.add(buttonPanel);
        
        formCard.add(formTitle, BorderLayout.NORTH);
        formCard.add(formPanel, BorderLayout.CENTER);
        
        // ========== TABLE SECTION ==========
        settingsCourseModel = new DefaultTableModel(new Object[]{
                "Mã học phần", "Tên học phần", "Số tín chỉ", "Loại học phần", "Hành động"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        settingsCourseTable = new JTable(settingsCourseModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return c;
            }
        };
        settingsCourseTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        settingsCourseTable.setRowHeight(50);
        settingsCourseTable.setShowGrid(true);
        settingsCourseTable.setGridColor(new Color(229, 231, 235));
        
        // Custom header
        JTableHeader header = settingsCourseTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(243, 244, 246));
        header.setForeground(new Color(31, 41, 55));
        header.setPreferredSize(new Dimension(0, 45));
        header.setReorderingAllowed(false);
        
        settingsCourseTable.setSelectionBackground(new Color(239, 246, 255));
        settingsCourseTable.setSelectionForeground(Color.BLACK);
        
        // Column widths
        settingsCourseTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        settingsCourseTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        settingsCourseTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        settingsCourseTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        settingsCourseTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        
        // Custom renderer cho cột HÀNH ĐỘNG
        settingsCourseTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : 
                    (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));

                JButton btnEdit = new JButton("Sửa");
                btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnEdit.setBackground(new Color(59, 130, 246));
                btnEdit.setForeground(Color.WHITE);
                btnEdit.setBorderPainted(false);
                btnEdit.setPreferredSize(new Dimension(70, 32));
                btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnEdit.setToolTipText("Sửa học phần");

                panel.add(btnEdit);
                return panel;
            }
        });
        
        // MouseListener để detect click vào buttons
        settingsCourseTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = settingsCourseTable.rowAtPoint(e.getPoint());
                int col = settingsCourseTable.columnAtPoint(e.getPoint());
                
                if (row >= 0 && col == 4) {
                    editSettingsCourse(row);
                }
            }
        });
        
        // Listener để fill form khi chọn dòng
        settingsCourseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = settingsCourseTable.getSelectedRow();
                if (row >= 0) {
                    fillSettingsFormFromTable(row);
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(settingsCourseTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        tableScrollPane.setViewportBorder(null);

        CardPanel tableCard = new CardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel tableTitle = new JLabel("Danh sách học phần");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(new Color(31, 41, 55));
        tableTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(tableScrollPane, BorderLayout.CENTER);
        
        // Layout: Filter ở trên, Form và Table ở dưới (Form bên trái, Table bên phải)
        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setOpaque(false);
        
        JPanel leftRightPanel = new JPanel(new BorderLayout(16, 0));
        leftRightPanel.setOpaque(false);
        leftRightPanel.add(formCard, BorderLayout.WEST);
        leftRightPanel.add(tableCard, BorderLayout.CENTER);
        
        contentPanel.add(filterCard, BorderLayout.NORTH);
        contentPanel.add(leftRightPanel, BorderLayout.CENTER);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Load danh sách học kỳ vào combobox "Quản lý học phần"
     * 
     * Lấy danh sách học kỳ từ Memory.loadTerms() và thêm vào cbTermCourse.
     * Tự động chọn học kỳ đầu tiên nếu có.
     * 
     * Được gọi khi:
     * - Khởi tạo màn hình
     * - Cần refresh danh sách học kỳ (sau khi thêm/sửa học kỳ)
     */
    void loadTerms() {
        cbTermCourse.removeAllItems();
        for (String t : Memory.loadTerms()) {
            cbTermCourse.addItem(t);
        }
        if (cbTermCourse.getItemCount() > 0) {
            cbTermCourse.setSelectedIndex(0);
        }
    }

    /**
     * Cập nhật combobox học kỳ trong tab "Duyệt đăng ký học phần"
     * 
     * Load lại danh sách học kỳ từ Memory và thêm vào cbTermApproval.
     * Cố gắng giữ lại lựa chọn cũ nếu học kỳ đó vẫn còn trong danh sách.
     * 
     * Được gọi khi:
     * - Khởi tạo màn hình
     * - Sau khi thêm/sửa/xóa học kỳ (cần refresh danh sách)
     */
    void updateApprovalTermComboBox() {
        String selected = (String) cbTermApproval.getSelectedItem();
        cbTermApproval.removeAllItems();
        cbTermApproval.addItem("Tất cả");
        for (String t : Memory.loadTerms()) {
            cbTermApproval.addItem(t);
        }
        // Giữ lại lựa chọn cũ nếu có thể
        if (selected != null) {
            for (int i = 0; i < cbTermApproval.getItemCount(); i++) {
                if (selected.equals(cbTermApproval.getItemAt(i))) {
                    cbTermApproval.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Refresh (làm mới) bảng danh sách học phần trong tab "Quản lý học phần"
     * 
     * Phương thức này:
     * 1. Xóa tất cả dữ liệu cũ trong bảng
     * 2. Lấy học kỳ đã chọn
     * 3. Lọc học phần theo:
     *    - Học kỳ đã chọn
     *    - Từ khóa tìm kiếm (mã hoặc tên học phần)
     *    - Trạng thái mở/đóng (nếu đã chọn filter)
     * 4. Với mỗi học phần, lấy thông tin:
     *    - Thông tin cơ bản (mã, tên, loại)
     *    - Offering (trạng thái mở/đóng, CTĐT được phép)
     *    - Số lượng sinh viên đã đăng ký
     * 5. Thêm vào bảng
     * 
     * Được gọi khi:
     * - Thay đổi học kỳ
     * - Thay đổi từ khóa tìm kiếm
     * - Thay đổi filter trạng thái
     * - Sau khi tạo/sửa/xóa học phần
     * - Sau khi cập nhật Offering
     */
    void refreshCourseTable() {
        courseModel.setRowCount(0);
        
        String term = (String) cbTermCourse.getSelectedItem();
        if (term == null) return;
        
        String searchText = searchCourseField.getText().toLowerCase().trim();
        String selectedStatus = (String) cbCourseStatus.getSelectedItem();
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        for (Course c : Memory.courses.values()) {
            Offering off = Memory.getOffering(term, c.code);
            
            // Filter by search
            if (!searchText.isEmpty()) {
                if (!c.code.toLowerCase().contains(searchText) && 
                    !c.name.toLowerCase().contains(searchText)) {
                    continue;
                }
            }
            
            // Filter by status
            String status = (off != null && off.open) ? "Đang mở" : "Đóng";
            if (selectedStatus != null && !selectedStatus.equals("Tất cả")) {
                if (!selectedStatus.equals(status)) continue;
            }
            
            // Thời gian mở - lấy từ thời gian mở đăng ký của học kỳ
            String timeRange = "-";
            if (term != null && !term.isEmpty()) {
                TermSetting termSetting = Memory.termSettings.get(term);
                if (termSetting != null) {
                    Date startDate = termSetting.startDate;
                    Date endDate = termSetting.endDate;
                    
                    if (startDate != null && endDate != null) {
                        timeRange = sdf.format(startDate) + " đến " + sdf.format(endDate);
                    } else if (startDate != null) {
                        timeRange = sdf.format(startDate) + " đến -";
                    } else if (endDate != null) {
                        timeRange = "- đến " + sdf.format(endDate);
                    }
                }
            }
            
            // Loại học phần (có thể xác định từ mã học phần)
            String type = getCourseType(c.code);
            
            // Số lượng đăng ký
            int regCount = Memory.countRegByCourse(term, c.code);
            
            courseModel.addRow(new Object[]{
                    c.code,
                    c.name,
                    type,
                    timeRange,
                    status,
                    String.valueOf(regCount),
                    ""
            });
        }
    }

    /**
     * Xác định loại học phần dựa trên mã học phần
     */
    private String getCourseType(String code) {
        if (code == null || code.isEmpty()) return "Tự chọn";
        
        if (code.startsWith("CT")) return "Tự chọn";
        if (code.startsWith("MA") || code.startsWith("PH") || code.startsWith("IT")) return "Cơ sở";
        if (code.startsWith("PE") || code.startsWith("MIL") || code.startsWith("SSH")) return "Bắt buộc";
        
        return "Tự chọn";
    }

    /**
     * Toggle trạng thái mở/đóng của học phần
     */
    void toggleCourseStatus(int row) {
        String courseCode = (String) courseModel.getValueAt(row, 0);
        String term = (String) cbTermCourse.getSelectedItem();
        if (term == null || courseCode == null) return;
        
        Offering off = Memory.getOffering(term, courseCode);
        if (off == null) {
            // Tạo mới offering
            Memory.setOffering(term, courseCode, true, "Tất cả");
        } else {
            // Toggle trạng thái
            Memory.setOffering(term, courseCode, !off.open, off.allowedProgram);
        }
        
        refreshCourseTable();
    }

    /**
     * Hiển thị dialog tạo học phần mới
     */
    void showCreateCourseDialog() {
        JDialog dialog = new JDialog(this, "Tạo học phần mới", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(24, 24, 24, 24));
        contentPanel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Thông tin học phần");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(31, 41, 55));
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        contentPanel.add(titleLabel);
        
        // Form fields
        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 14);
        
        JPanel codePanel = createFieldGroup("Mã học phần", tfCode, 200, labelFont, inputFont);
        JPanel namePanel = createFieldGroup("Tên học phần", tfName, 400, labelFont, inputFont);
        JPanel creditsPanel = createSpinnerGroup("Số tín chỉ", spCredits, 100, labelFont, inputFont);
        JPanel typePanel = createComboBoxGroup("Loại học phần", cbType, 150, labelFont, inputFont);
        
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        datePanel.setOpaque(false);
        JSpinner.DateEditor courseStartEditor = new JSpinner.DateEditor(spCourseStartDate, "dd/MM/yyyy");
        spCourseStartDate.setEditor(courseStartEditor);
        ((JSpinner.DefaultEditor) spCourseStartDate.getEditor()).getTextField().setEditable(false);
        spCourseStartDate.setPreferredSize(new Dimension(150, 36));
        spCourseStartDate.setFont(inputFont);
        JPanel startDatePanel = createSpinnerGroup("Thời gian mở (từ ngày)", spCourseStartDate, 150, labelFont, inputFont);
        
        JSpinner.DateEditor courseEndEditor = new JSpinner.DateEditor(spCourseEndDate, "dd/MM/yyyy");
        spCourseEndDate.setEditor(courseEndEditor);
        ((JSpinner.DefaultEditor) spCourseEndDate.getEditor()).getTextField().setEditable(false);
        spCourseEndDate.setPreferredSize(new Dimension(150, 36));
        spCourseEndDate.setFont(inputFont);
        JPanel endDatePanel = createSpinnerGroup("Thời gian mở (đến ngày)", spCourseEndDate, 150, labelFont, inputFont);
        
        contentPanel.add(codePanel);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(namePanel);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(creditsPanel);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(typePanel);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(startDatePanel);
        contentPanel.add(Box.createVerticalStrut(16));
        contentPanel.add(endDatePanel);
        contentPanel.add(Box.createVerticalGlue());
        
        dialog.add(contentPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setForeground(new Color(107, 114, 128));
        btnCancel.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        btnCancel.setPreferredSize(new Dimension(100, 36));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> {
            clearCourseForm();
            dialog.dispose();
        });
        
        JButton btnSave = new JButton("Lưu");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setBackground(new Color(59, 130, 246));
        btnSave.setForeground(Color.WHITE);
        btnSave.setBorderPainted(false);
        btnSave.setPreferredSize(new Dimension(100, 36));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            if (saveCourse()) {
            dialog.dispose();
                refreshCourseTable();
            }
        });
        
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Clear form trước khi hiển thị
        clearCourseForm();
        
        dialog.setVisible(true);
    }

    /**
     * Hiển thị dialog tạo học kỳ mới - Giao diện hiện đại với HTML/CSS
     */
    void showCreateTermDialog() {
        JDialog dialog = new JDialog(this, "", true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        dialog.setBackground(new Color(0, 0, 0, 0)); // Transparent để hiển thị shadow
        
        // Main container với shadow và border radius mượt mà
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int radius = 20;
                int shadowOffset = 8;
                
                // Vẽ multiple shadows để tạo hiệu ứng đẹp hơn
                for (int i = shadowOffset; i >= 0; i--) {
                    float alpha = (float)(0.08 - (i * 0.01));
                    if (alpha > 0) {
                        g2.setColor(new Color(0, 0, 0, alpha));
                        g2.fillRoundRect(i, i, getWidth() - shadowOffset, getHeight() - shadowOffset, radius, radius);
                    }
                }
                
                // Vẽ nền trắng bo góc mượt
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - shadowOffset, getHeight() - shadowOffset, radius, radius);
                
                // Vẽ border nhẹ
                g2.setColor(new Color(229, 231, 235));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1, radius, radius);
                
                g2.dispose();
            }
        };
        mainContainer.setOpaque(false);
        mainContainer.setBorder(new EmptyBorder(0, 0, 8, 8));
        
        // Header với HTML styling
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(28, 30, 24, 32));
        
        // Title với HTML và underline xanh
        JLabel titleLabel = new JLabel("<html><div style='font-size: 17.6px; font-weight: 700; color: #111827; line-height: 1.2; border-bottom: 3px solid #10B981; padding-bottom: 8px; display: inline-block;'>" +
                "Tạo học kỳ mới</div></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        
        // Nút đóng với HTML styling
        JButton btnClose = new JButton("<html><div style='font-size: 18px; color: #6B7280; text-align: center;'>✕</div></html>");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        btnClose.setBackground(Color.WHITE);
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setPreferredSize(new Dimension(36, 36));
        btnClose.addActionListener(e -> dialog.dispose());
        
        btnClose.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnClose.setText("<html><div style='font-size: 18px; color: #EF4444; text-align: center; background: #FEF2F2; border-radius: 8px; padding: 4px;'>✕</div></html>");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnClose.setText("<html><div style='font-size: 18px; color: #6B7280; text-align: center;'>✕</div></html>");
            }
        });
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(btnClose, BorderLayout.EAST);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 30, 24, 32));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 15);
        
        // Mã học kỳ
        JLabel lbTermCode = new JLabel("Mã học kỳ:");
        lbTermCode.setFont(labelFont);
        lbTermCode.setForeground(new Color(55, 65, 81));
        lbTermCode.setBorder(new EmptyBorder(0, 0, 8, 0));
        lbTermCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        PlaceholderTextField tfTermCode = new PlaceholderTextField("Ví dụ: 20261");
        tfTermCode.setFont(inputFont);
        tfTermCode.setPreferredSize(new Dimension(0, 40));
        tfTermCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tfTermCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfTermCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        // Tên học kỳ
        JLabel lbTermName = new JLabel("Tên học kỳ:");
        lbTermName.setFont(labelFont);
        lbTermName.setForeground(new Color(55, 65, 81));
        lbTermName.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbTermName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        PlaceholderTextField tfTermName = new PlaceholderTextField("Nhập tên học kỳ...");
        tfTermName.setFont(inputFont);
        tfTermName.setPreferredSize(new Dimension(0, 40));
        tfTermName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tfTermName.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfTermName.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        // Năm học
        JLabel lbAcademicYear = new JLabel("Năm học:");
        lbAcademicYear.setFont(labelFont);
        lbAcademicYear.setForeground(new Color(55, 65, 81));
        lbAcademicYear.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbAcademicYear.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        PlaceholderTextField tfAcademicYear = new PlaceholderTextField("Ví dụ: 2025");
        tfAcademicYear.setFont(inputFont);
        tfAcademicYear.setPreferredSize(new Dimension(0, 40));
        tfAcademicYear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tfAcademicYear.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfAcademicYear.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        // Trạng thái
        JLabel lbStatus = new JLabel("Trạng thái:");
        lbStatus.setFont(labelFont);
        lbStatus.setForeground(new Color(55, 65, 81));
        lbStatus.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Đang hoạt động", "Đã kết thúc"});
        cbStatus.setFont(inputFont);
        cbStatus.setPreferredSize(new Dimension(0, 40));
        cbStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        cbStatus.setSelectedIndex(0); // Mặc định là "Đang hoạt động"
        
        // Thời gian mở đăng ký bắt đầu
        JLabel lbStartDate = new JLabel("Thời gian mở đăng ký (bắt đầu):");
        lbStartDate.setFont(labelFont);
        lbStartDate.setForeground(new Color(55, 65, 81));
        lbStartDate.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbStartDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JSpinner spStartDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor startDateEditor = new JSpinner.DateEditor(spStartDate, "dd/MM/yyyy");
        spStartDate.setEditor(startDateEditor);
        ((JSpinner.DefaultEditor) spStartDate.getEditor()).getTextField().setEditable(false);
        spStartDate.setPreferredSize(new Dimension(0, 40));
        spStartDate.setFont(inputFont);
        spStartDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        spStartDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        // Thời gian mở đăng ký kết thúc
        JLabel lbEndDate = new JLabel("Thời gian mở đăng ký (kết thúc):");
        lbEndDate.setFont(labelFont);
        lbEndDate.setForeground(new Color(55, 65, 81));
        lbEndDate.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbEndDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JSpinner spEndDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor endDateEditor = new JSpinner.DateEditor(spEndDate, "dd/MM/yyyy");
        spEndDate.setEditor(endDateEditor);
        ((JSpinner.DefaultEditor) spEndDate.getEditor()).getTextField().setEditable(false);
        spEndDate.setPreferredSize(new Dimension(0, 40));
        spEndDate.setFont(inputFont);
        spEndDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        spEndDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        contentPanel.add(lbTermCode);
        contentPanel.add(tfTermCode);
        contentPanel.add(lbTermName);
        contentPanel.add(tfTermName);
        contentPanel.add(lbAcademicYear);
        contentPanel.add(tfAcademicYear);
        contentPanel.add(lbStatus);
        contentPanel.add(cbStatus);
        contentPanel.add(lbStartDate);
        contentPanel.add(spStartDate);
        contentPanel.add(lbEndDate);
        contentPanel.add(spEndDate);
        contentPanel.add(Box.createVerticalGlue());
        
        // Button panel với HTML styling
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(24, 30, 32, 32));
        
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonContainer.setOpaque(false);
        
        JButton btnCancel = new JButton("<html><div style='font-size: 11.2px; color: #6B7280; padding: 8px 19px;'>Hủy</div></html>");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setBorderPainted(false);
        btnCancel.setContentAreaFilled(true);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnCancel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnCancel.setBackground(new Color(249, 250, 251));
                btnCancel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnCancel.setBackground(Color.WHITE);
                btnCancel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
        });
        
        JButton btnSave = new JButton("<html><div style='font-size: 11.2px; font-weight: 600; color: #FFFFFF; padding: 8px 19px;'>Tạo học kỳ</div></html>");
        btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setBorderPainted(false);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            String termCode = tfTermCode.getText().trim();
            String termName = tfTermName.getText().trim();
            String academicYear = tfAcademicYear.getText().trim();
            Date startDate = (Date) spStartDate.getValue();
            Date endDate = (Date) spEndDate.getValue();
            
            // Logic trạng thái: "Đang hoạt động" = registrationOpen = true
            // "Đã kết thúc" = registrationOpen = false (hoặc có thể tự động set endDate = now nếu cần)
            boolean registrationOpen = cbStatus.getSelectedIndex() == 0; // 0 = Đang hoạt động, 1 = Đã kết thúc
            
            // Nếu chọn "Đã kết thúc" và chưa có endDate, tự động set endDate = now
            if (cbStatus.getSelectedIndex() == 1 && endDate == null) {
                endDate = new Date();
                spEndDate.setValue(endDate);
            }
            
            if (saveTerm(termCode, termName, academicYear, startDate, endDate, registrationOpen)) {
                dialog.dispose();
                loadTerms();
                refreshCourseTable();
                updateApprovalTermComboBox();
                refreshTermSettingsTable();
                updateAcademicYearComboBox();
            }
        });
        
        btnSave.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnSave.setBackground(new Color(5, 150, 105));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnSave.setBackground(new Color(16, 185, 129));
            }
        });
        
        buttonContainer.add(btnCancel);
        buttonContainer.add(btnSave);
        buttonPanel.add(buttonContainer, BorderLayout.EAST);
        
        // Wrap content panel trong ScrollPane để có thể scroll nếu cần
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Assemble dialog
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(scrollPane, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainContainer, BorderLayout.CENTER);
        
        // Focus vào input field khi mở dialog
        SwingUtilities.invokeLater(() -> {
            tfTermCode.requestFocus();
        });
        
        dialog.setVisible(true);
    }
    
    /**
     * Helper methods để tạo field groups
     */
    private JPanel createFieldGroup(String labelText, JTextField field, int width, Font labelFont, Font inputFont) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        
        JLabel label = new JLabel(labelText + ":");
        label.setFont(labelFont);
        label.setForeground(new Color(55, 65, 81));
        
        field.setFont(inputFont);
        field.setPreferredSize(new Dimension(width, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        
        return group;
    }
    
    private JPanel createSpinnerGroup(String labelText, JSpinner spinner, int width, Font labelFont, Font inputFont) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        
        JLabel label = new JLabel(labelText + ":");
        label.setFont(labelFont);
        label.setForeground(new Color(55, 65, 81));
        
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setFont(inputFont);
        spinner.setPreferredSize(new Dimension(width, 36));
        spinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        group.add(label, BorderLayout.NORTH);
        group.add(spinner, BorderLayout.CENTER);
        
        return group;
    }
    
    private JPanel createComboBoxGroup(String labelText, JComboBox<String> combo, int width, Font labelFont, Font inputFont) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        
        JLabel label = new JLabel(labelText + ":");
        label.setFont(labelFont);
        label.setForeground(new Color(55, 65, 81));
        
        combo.setFont(inputFont);
        combo.setPreferredSize(new Dimension(width, 36));
        combo.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
        
        group.add(label, BorderLayout.NORTH);
        group.add(combo, BorderLayout.CENTER);
        
        return group;
    }

    /**
     * Lưu học phần mới
     */
    boolean saveCourse() {
        String code = tfCode.getText().trim().toUpperCase();
        String name = tfName.getText().trim();
        int credits = (int) spCredits.getValue();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Mã học phần và Tên học phần.");
            return false;
        }

        Course course = new Course(code, name, credits);
        Memory.addCourse(course);
        
        // Tạo offering nếu có thời gian mở
        Date startDate = (Date) spCourseStartDate.getValue();
        Date endDate = (Date) spCourseEndDate.getValue();
        String term = (String) cbTermCourse.getSelectedItem();
        
        if (term != null && startDate != null && endDate != null) {
            if (startDate.after(endDate)) {
                JOptionPane.showMessageDialog(this, "Ngày bắt đầu phải trước ngày kết thúc!");
                return false;
            }
            Memory.setOffering(term, code, true, "Tất cả");
        }

        JOptionPane.showMessageDialog(this, "Đã tạo học phần thành công!");
        clearCourseForm();
        return true;
    }

    /**
     * Clear form tạo học phần
     */
    void clearCourseForm() {
        tfCode.setText("");
        tfName.setText("");
        spCredits.setValue(2);
        cbType.setSelectedIndex(0);
        spCourseStartDate.setValue(new Date());
        spCourseEndDate.setValue(new Date());
    }

    /**
     * Lưu học kỳ mới
     */
    boolean saveTerm(String termCode, String termName, String academicYear, Date startDate, Date endDate, boolean registrationOpen) {
        if (termCode == null || termCode.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã học kỳ.");
            return false;
        }
        
        termCode = termCode.trim();
        
        // Kiểm tra học kỳ đã tồn tại chưa
        if (Memory.terms.contains(termCode)) {
            JOptionPane.showMessageDialog(this, 
                    "Học kỳ " + termCode + " đã tồn tại. Vui lòng nhập mã học kỳ khác.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Validate thời gian
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            JOptionPane.showMessageDialog(this, "Ngày bắt đầu phải trước ngày kết thúc!");
            return false;
        }
        
        // Validate định dạng mã học kỳ
        try {
            if (!termCode.matches("^[0-9]+$")) {
                int result = JOptionPane.showConfirmDialog(this,
                        "Mã học kỳ nên là số (ví dụ: 20261, 20262).\n" +
                        "Bạn có muốn tiếp tục với mã học kỳ này không?",
                        "Xác nhận",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        } catch (Exception e) {
            // Bỏ qua nếu có lỗi
        }
        
        // Thêm học kỳ vào danh sách
        Memory.terms.add(termCode);
        
        // Tạo TermSetting với đầy đủ thông tin
        TermSetting setting = new TermSetting(registrationOpen, termName, academicYear, startDate, endDate);
        Memory.termSettings.put(termCode, setting);
        
        // Nếu trạng thái là "Đã kết thúc" (registrationOpen = false), đóng tất cả các Offering
        if (!registrationOpen) {
            closeAllOfferings(termCode);
        }
        
        JOptionPane.showMessageDialog(this, 
                "Đã tạo học kỳ " + termCode + " thành công!",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    /**
     * Filter (lọc) bảng duyệt đăng ký học phần
     * 
     * Phương thức này lọc và hiển thị đăng ký theo các tiêu chí:
     * 1. Từ khóa tìm kiếm: tìm trong tên/MSSV sinh viên, mã/tên học phần
     * 2. Trạng thái: "Tất cả", "Chờ duyệt", "Đã duyệt", "Đã từ chối"
     *    (chuẩn hóa: "Tạm" và "Đã gửi" → "Chờ duyệt")
     * 3. Học kỳ: "Tất cả" hoặc học kỳ cụ thể
     * 4. Khoa/Viện: "Tất cả" hoặc CTĐT cụ thể
     * 
     * Duyệt qua tất cả đăng ký trong Memory.regs:
     * - Với mỗi sinh viên
     * - Với mỗi học kỳ của sinh viên đó
     * - Với mỗi RegItem (đăng ký học phần)
     * - Áp dụng các filter và thêm vào bảng nếu phù hợp
     * 
     * Được gọi khi:
     * - Thay đổi từ khóa tìm kiếm (real-time)
     * - Thay đổi filter trạng thái
     * - Thay đổi filter học kỳ
     * - Thay đổi filter khoa/viện
     * - Sau khi duyệt/từ chối đăng ký (để cập nhật trạng thái)
     */
        void filterApprovalTable() {
        String searchText = searchApprovalField.getText().toLowerCase();
        String selectedStatus = (String) cbApprovalStatus.getSelectedItem();
        String selectedTerm = (String) cbTermApproval.getSelectedItem();
        String selectedDept = (String) cbDeptApproval.getSelectedItem();

            approvalModel.setRowCount(0);
            int regCount = 0;

            for (Map.Entry<String, Map<String, List<RegItem>>> studentEntry : Memory.regs.entrySet()) {
                String studentId = studentEntry.getKey();
                Student student = Memory.studentsById.get(studentId);
                if (student == null) continue;

                if (selectedDept != null && !selectedDept.equals("Tất cả")) {
                    if (!selectedDept.equals(student.program)) continue;
                }

                Map<String, List<RegItem>> termRegs = studentEntry.getValue();
                for (Map.Entry<String, List<RegItem>> termEntry : termRegs.entrySet()) {
                    String term = termEntry.getKey();
                    
                    if (selectedTerm != null && !selectedTerm.equals("Tất cả")) {
                        if (!selectedTerm.equals(term)) continue;
                    }

                    List<RegItem> regItems = termEntry.getValue();
                    for (RegItem item : regItems) {
                        String status = item.status;
                        if ("Tạm".equals(status) || "Đã gửi".equals(status)) {
                        status = "Chờ duyệt";
                        }

                        if (selectedStatus != null && !selectedStatus.equals("Tất cả")) {
                            if (!selectedStatus.equals(status)) continue;
                        }

                        if (!searchText.isEmpty()) {
                            if (!student.fullName.toLowerCase().contains(searchText) &&
                                !studentId.toLowerCase().contains(searchText) &&
                                !item.course.name.toLowerCase().contains(searchText) &&
                                !item.course.code.toLowerCase().contains(searchText)) {
                                continue;
                            }
                        }

                        regCount++;
                        String regCode = "DK" + String.format("%03d", regCount);
                        
                        approvalModel.addRow(new Object[]{
                            regCode, student.fullName, studentId, student.program, item.course.code, item.course.name,
                            status, "", term  // Thêm term vào cuối để dùng trong approveAll
                    });
                }
            }
        }
    }

    /**
     * Duyệt đăng ký học phần tại dòng được chỉ định trong bảng
     * 
     * Phương thức này:
     * 1. Lấy thông tin từ bảng: MSSV, mã học phần
     * 2. Xác định học kỳ:
     *    - Nếu filter học kỳ đã chọn cụ thể: dùng học kỳ đó
     *    - Nếu filter = "Tất cả": tìm học kỳ từ dữ liệu (duyệt qua Memory.regs)
     * 3. Tìm RegItem tương ứng trong Memory
     * 4. Cập nhật trạng thái thành "Đã duyệt"
     * 5. Refresh bảng để hiển thị thay đổi
     * 6. Hiển thị thông báo thành công
     * 
     * @param row Số dòng trong bảng (0-based index) của đăng ký cần duyệt
     */
    void approveRegistration(int row) {
        String studentId = (String) approvalModel.getValueAt(row, 2);
        String courseCode = (String) approvalModel.getValueAt(row, 4); // Mã học phần ở cột 4 (sau Khoa/Viện)
        String selectedTerm = (String) cbTermApproval.getSelectedItem();
        String term = selectedTerm != null && !selectedTerm.equals("Tất cả") ? selectedTerm : null;
        
        if (term == null) {
            // Tìm term từ dữ liệu
            for (Map.Entry<String, Map<String, List<RegItem>>> studentEntry : Memory.regs.entrySet()) {
                if (studentEntry.getKey().equals(studentId)) {
                    Map<String, List<RegItem>> termRegs = studentEntry.getValue();
                    for (Map.Entry<String, List<RegItem>> termEntry : termRegs.entrySet()) {
                        List<RegItem> regItems = termEntry.getValue();
                        for (RegItem item : regItems) {
                            if (item.course.code.equals(courseCode)) {
                                item.status = "Đã duyệt";
                                filterApprovalTable();
                                JOptionPane.showMessageDialog(this, "Đã duyệt đăng ký thành công!");
                                return;
                            }
                        }
                    }
                }
            }
                    } else {
            List<RegItem> regs = Memory.loadReg(studentId, term);
            for (RegItem item : regs) {
                if (item.course.code.equals(courseCode)) {
                    item.status = "Đã duyệt";
                    filterApprovalTable();
                    JOptionPane.showMessageDialog(this, "Đã duyệt đăng ký thành công!");
                    return;
                }
            }
        }
    }

    /**
     * Từ chối đăng ký học phần tại dòng được chỉ định trong bảng
     * 
     * Phương thức này:
     * 1. Hiển thị dialog yêu cầu nhập lý do từ chối
     * 2. Nếu người dùng hủy (không nhập lý do): dừng lại
     * 3. Lấy thông tin từ bảng: MSSV, mã học phần
     * 4. Xác định học kỳ (tương tự approveRegistration)
     * 5. Tìm RegItem tương ứng trong Memory
     * 6. Cập nhật trạng thái thành "Đã từ chối" (hoặc "Từ chối")
     * 7. Refresh bảng để hiển thị thay đổi
     * 8. Hiển thị thông báo thành công
     * 
     * Lưu ý: Lý do từ chối được nhập nhưng không được lưu vào RegItem
     * (có thể cần thêm field reason vào RegItem model trong tương lai).
     * 
     * @param row Số dòng trong bảng (0-based index) của đăng ký cần từ chối
     */
    void rejectRegistration(int row) {
            String studentId = (String) approvalModel.getValueAt(row, 2);
        String courseCode = (String) approvalModel.getValueAt(row, 4); // Mã học phần ở cột 4 (sau Khoa/Viện)
        String selectedTerm = (String) cbTermApproval.getSelectedItem();
        String term = selectedTerm != null && !selectedTerm.equals("Tất cả") ? selectedTerm : null;
        
        // Hiển thị dialog nhập lý do từ chối
        String reason = JOptionPane.showInputDialog(this, "Nhập lý do từ chối:", "Từ chối đăng ký", 
                JOptionPane.QUESTION_MESSAGE);
        if (reason == null) return; // User cancelled
        
        if (term == null) {
            // Tìm term từ dữ liệu
            for (Map.Entry<String, Map<String, List<RegItem>>> studentEntry : Memory.regs.entrySet()) {
                if (studentEntry.getKey().equals(studentId)) {
                    Map<String, List<RegItem>> termRegs = studentEntry.getValue();
                    for (Map.Entry<String, List<RegItem>> termEntry : termRegs.entrySet()) {
                        List<RegItem> regItems = termEntry.getValue();
                        for (RegItem item : regItems) {
                            if (item.course.code.equals(courseCode)) {
                                item.status = "Từ chối";
                                filterApprovalTable();
                                JOptionPane.showMessageDialog(this, "Đã từ chối đăng ký!");
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            List<RegItem> regs = Memory.loadReg(studentId, term);
            for (RegItem item : regs) {
                if (item.course.code.equals(courseCode)) {
                    item.status = "Từ chối";
                    filterApprovalTable();
                    JOptionPane.showMessageDialog(this, "Đã từ chối đăng ký!");
                    return;
                }
            }
        }
    }

    /**
     * Duyệt tất cả các đăng ký đang hiển thị trong bảng (hàng loạt)
     * 
     * Phương thức này:
     * 1. Kiểm tra có đăng ký nào trong bảng không
     * 2. Hiển thị dialog xác nhận (số lượng đăng ký sẽ được duyệt)
     * 3. Nếu người dùng xác nhận:
     *    - Duyệt qua tất cả các dòng trong bảng
     *    - Với mỗi dòng, gọi approveRegistration() để duyệt
     *    - Đếm số lượng đăng ký đã duyệt thành công
     * 4. Hiển thị thông báo kết quả (số lượng đã duyệt)
     * 
     * Lưu ý: Chỉ duyệt các đăng ký đang hiển thị trong bảng (đã được filter).
     * Các đăng ký không hiển thị (do filter) sẽ không được duyệt.
     */
    void approveAll() {
        int rowCount = approvalModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Không có đăng ký nào để duyệt.");
            return;
        }
        
        // Xác nhận
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn duyệt tất cả " + rowCount + " đăng ký đang hiển thị?",
                "Xác nhận duyệt tất cả",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        int approvedCount = 0;
        String selectedTerm = (String) cbTermApproval.getSelectedItem();
        
        // Duyệt tất cả các dòng trong bảng
        for (int row = 0; row < rowCount; row++) {
            String studentId = (String) approvalModel.getValueAt(row, 2);
            String courseCode = (String) approvalModel.getValueAt(row, 4); // Mã học phần ở cột 4
            String status = (String) approvalModel.getValueAt(row, 6); // Trạng thái ở cột 6
            
            // Chỉ duyệt các đăng ký đang ở trạng thái chờ duyệt
            if ("Chờ duyệt".equals(status) || "Chờ xử lý".equals(status) || 
                "Đã gửi".equals(status) || "Tạm".equals(status)) {
                
                // Lấy term từ bảng (cột 8 - cột ẩn)
                String term = (String) approvalModel.getValueAt(row, 8);
                
                if (term != null && !term.isEmpty()) {
                    // Duyệt đăng ký với term đã biết
                    List<RegItem> regs = Memory.loadReg(studentId, term);
                    for (RegItem item : regs) {
                        if (item.course.code.equals(courseCode) && 
                            ("Chờ duyệt".equals(item.status) || "Chờ xử lý".equals(item.status) || 
                             "Đã gửi".equals(item.status) || "Tạm".equals(item.status))) {
                            item.status = "Đã duyệt";
                            approvedCount++;
                            break;
                        }
                    }
                }
            }
        }

        filterApprovalTable();
        JOptionPane.showMessageDialog(this, 
                "Đã duyệt thành công " + approvedCount + " đăng ký!", 
                "Thành công", 
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Từ chối tất cả các đăng ký đang hiển thị trong bảng (hàng loạt)
     * 
     * Phương thức này:
     * 1. Kiểm tra có đăng ký nào trong bảng không
     * 2. Hiển thị dialog yêu cầu nhập lý do từ chối (cho tất cả đăng ký)
     * 3. Nếu người dùng hủy hoặc không nhập lý do: dừng lại
     * 4. Hiển thị dialog xác nhận (số lượng đăng ký sẽ bị từ chối)
     * 5. Nếu người dùng xác nhận:
     *    - Duyệt qua tất cả các dòng trong bảng
     *    - Với mỗi dòng, tìm RegItem và cập nhật trạng thái thành "Đã từ chối"
     *    - Chỉ từ chối các đăng ký có trạng thái "Chờ duyệt" (Tạm, Đã gửi)
     *    - Đếm số lượng đăng ký đã từ chối thành công
     * 6. Refresh bảng và hiển thị thông báo kết quả
     * 
     * Lưu ý: Chỉ từ chối các đăng ký đang hiển thị trong bảng (đã được filter).
     * Các đăng ký đã duyệt sẽ không bị từ chối.
     */
    void rejectAll() {
        int rowCount = approvalModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "Không có đăng ký nào để từ chối.");
            return;
        }
        
        // Hiển thị dialog nhập lý do từ chối
        String reason = JOptionPane.showInputDialog(this, 
                "Nhập lý do từ chối cho tất cả " + rowCount + " đăng ký:", 
                "Từ chối tất cả", 
                JOptionPane.QUESTION_MESSAGE);
        
        if (reason == null || reason.trim().isEmpty()) {
            return; // User cancelled or didn't enter reason
        }
        
        // Xác nhận
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn từ chối tất cả " + rowCount + " đăng ký đang hiển thị?",
                "Xác nhận từ chối tất cả",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        int rejectedCount = 0;
        String selectedTerm = (String) cbTermApproval.getSelectedItem();
        
        // Từ chối tất cả các dòng trong bảng
        for (int row = 0; row < rowCount; row++) {
            String studentId = (String) approvalModel.getValueAt(row, 2);
            String courseCode = (String) approvalModel.getValueAt(row, 4); // Mã học phần ở cột 4
            String status = (String) approvalModel.getValueAt(row, 6); // Trạng thái ở cột 6
            
            // Chỉ từ chối các đăng ký đang ở trạng thái chờ duyệt
            if ("Chờ duyệt".equals(status) || "Chờ xử lý".equals(status) || 
                "Đã gửi".equals(status) || "Tạm".equals(status)) {
                
                String term = selectedTerm != null && !selectedTerm.equals("Tất cả") ? selectedTerm : null;
                
                if (term == null) {
                    // Tìm term từ dữ liệu
                    for (Map.Entry<String, Map<String, List<RegItem>>> studentEntry : Memory.regs.entrySet()) {
                        if (studentEntry.getKey().equals(studentId)) {
                            Map<String, List<RegItem>> termRegs = studentEntry.getValue();
                            for (Map.Entry<String, List<RegItem>> termEntry : termRegs.entrySet()) {
                                List<RegItem> regItems = termEntry.getValue();
                                for (RegItem item : regItems) {
                                    if (item.course.code.equals(courseCode) && 
                                        ("Chờ duyệt".equals(item.status) || "Chờ xử lý".equals(item.status) || 
                                         "Đã gửi".equals(item.status) || "Tạm".equals(item.status))) {
                                        item.status = "Từ chối";
                                        rejectedCount++;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    List<RegItem> regs = Memory.loadReg(studentId, term);
                    for (RegItem item : regs) {
                        if (item.course.code.equals(courseCode) && 
                            ("Chờ duyệt".equals(item.status) || "Chờ xử lý".equals(item.status) || 
                             "Đã gửi".equals(item.status) || "Tạm".equals(item.status))) {
                            item.status = "Từ chối";
                            rejectedCount++;
                            break;
                        }
                    }
                }
            }
        }
        
        filterApprovalTable();
        JOptionPane.showMessageDialog(this, 
                "Đã từ chối " + rejectedCount + " đăng ký!", 
                "Hoàn thành", 
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Refresh (làm mới) bảng danh sách học phần trong tab "Cài đặt học phần"
     * 
     * Phương thức này:
     * 1. Xóa tất cả dữ liệu cũ trong bảng
     * 2. Lấy từ khóa tìm kiếm và filter loại học phần
     * 3. Duyệt qua tất cả học phần trong Memory.courses
     * 4. Lọc học phần theo:
     *    - Từ khóa tìm kiếm (mã hoặc tên học phần)
     *    - Loại học phần (Bắt buộc, Tự chọn, Cơ sở) - xác định bằng getCourseType()
     * 5. Thêm vào bảng với các cột: Mã HP, Tên học phần, Số TC, Loại, Hành động
     * 
     * Được gọi khi:
     * - Thay đổi từ khóa tìm kiếm (real-time)
     * - Thay đổi filter loại học phần
     * - Sau khi thêm/sửa/xóa học phần
     */
    void refreshSettingsCourseTable() {
        settingsCourseModel.setRowCount(0);
        
        String searchText = searchSettingsField.getText().toLowerCase().trim();
        String selectedType = (String) cbSettingsType.getSelectedItem();
        
        for (Course c : Memory.courses.values()) {
            // Filter by search
            if (!searchText.isEmpty()) {
                if (!c.code.toLowerCase().contains(searchText) && 
                    !c.name.toLowerCase().contains(searchText)) {
                    continue;
                }
            }
            
            // Filter by type
            String type = getCourseType(c.code);
            if (selectedType != null && !selectedType.equals("Tất cả")) {
                if (!selectedType.equals(type)) continue;
            }
            
            settingsCourseModel.addRow(new Object[]{
                    c.code,
                    c.name,
                    String.valueOf(c.credits),
                    type,
                    ""
            });
        }
    }

    /**
     * Thêm học phần mới vào hệ thống từ form cài đặt
     * 
     * Phương thức này:
     * 1. Lấy dữ liệu từ form: mã HP, tên, số tín chỉ
     * 2. Validate dữ liệu (không được để trống)
     * 3. Chuyển mã HP sang chữ hoa (để thống nhất)
     * 4. Kiểm tra mã học phần đã tồn tại chưa (không được trùng)
     * 5. Tạo đối tượng Course mới và thêm vào Memory.courses
     * 6. Hiển thị thông báo thành công
     * 7. Clear form và refresh bảng
     * 
     * Lưu ý: Thêm học phần master không tự động mở lớp cho học kỳ nào.
     * Admin cần vào tab "Quản lý học phần" để mở lớp cho học kỳ cụ thể.
     */
    void addSettingsCourse() {
        String code = tfSettingsCode.getText().trim().toUpperCase();
        String name = tfSettingsName.getText().trim();
        int credits = (int) spSettingsCredits.getValue();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Mã học phần và Tên học phần.");
            return;
        }

        // Kiểm tra mã học phần đã tồn tại chưa
        if (Memory.courses.containsKey(code)) {
            JOptionPane.showMessageDialog(this, 
                    "Mã học phần " + code + " đã tồn tại. Vui lòng nhập mã khác.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Course course = new Course(code, name, credits);
        Memory.addCourse(course);
        
        JOptionPane.showMessageDialog(this, "Đã thêm học phần thành công!");
        clearSettingsForm();
        refreshSettingsCourseTable();
    }

    /**
     * Cập nhật thông tin học phần từ form cài đặt
     * 
     * Phương thức này:
     * 1. Lấy dữ liệu từ form: mã HP, tên, số tín chỉ, loại
     * 2. Validate dữ liệu (không được để trống)
     * 3. Kiểm tra học phần có tồn tại không
     * 4. Kiểm tra học phần có thể xóa được không (chưa có sinh viên đăng ký):
     *    - Vì Course có các field final, không thể sửa trực tiếp
     *    - Cần xóa học phần cũ và tạo lại với thông tin mới
     *    - Nếu đã có sinh viên đăng ký: không cho phép cập nhật (để tránh mất dữ liệu)
     * 5. Xóa học phần cũ và tạo lại với thông tin mới
     * 6. Refresh bảng và form
     * 
     * Lưu ý: Đây là cách xử lý do hạn chế của model (Course có field final).
     * Trong hệ thống thực tế, nên có phương thức updateCourse() trong CourseService.
     */
    void updateSettingsCourse() {
        String code = tfSettingsCode.getText().trim().toUpperCase();
        String name = tfSettingsName.getText().trim();
        int credits = (int) spSettingsCredits.getValue();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Mã học phần và Tên học phần.");
            return;
        }

        Course existingCourse = Memory.courses.get(code);
        if (existingCourse == null) {
            JOptionPane.showMessageDialog(this, 
                    "Không tìm thấy học phần với mã " + code + ". Vui lòng kiểm tra lại.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Vì Course có các field final, không thể sửa trực tiếp
        // Cần xóa và tạo lại
        if (!Memory.canDeleteCourse(code)) {
            JOptionPane.showMessageDialog(this, 
                    "Không thể cập nhật học phần " + code + " vì đã có sinh viên đăng ký.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Xóa học phần cũ
        Memory.deleteCourse(code);
        
        // Tạo học phần mới
        Course newCourse = new Course(code, name, credits);
        Memory.addCourse(newCourse);
        
        JOptionPane.showMessageDialog(this, "Đã cập nhật học phần thành công!");
        clearSettingsForm();
        refreshSettingsCourseTable();
    }

    /**
     * Xóa học phần từ form cài đặt
     */
    void deleteSettingsCourse() {
        String code = tfSettingsCode.getText().trim().toUpperCase();
        
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã học phần cần xóa.");
            return;
        }

        Course course = Memory.courses.get(code);
        if (course == null) {
            JOptionPane.showMessageDialog(this, 
                    "Không tìm thấy học phần với mã " + code + ".",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Memory.canDeleteCourse(code)) {
            JOptionPane.showMessageDialog(this, 
                    "Không thể xóa học phần " + code + " vì đã có sinh viên đăng ký.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa học phần " + code + " - " + course.name + "?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            Memory.deleteCourse(code);
            JOptionPane.showMessageDialog(this, "Đã xóa học phần thành công!");
            clearSettingsForm();
            refreshSettingsCourseTable();
        }
    }

    /**
     * Clear form cài đặt học phần
     */
    void clearSettingsForm() {
        tfSettingsCode.setText("");
        tfSettingsName.setText("");
        spSettingsCredits.setValue(2);
        cbSettingsTypeForm.setSelectedIndex(0);
        settingsCourseTable.clearSelection();
    }

    /**
     * Fill form từ dòng được chọn trong bảng
     */
    void fillSettingsFormFromTable(int row) {
        if (row < 0 || row >= settingsCourseModel.getRowCount()) return;
        
        String code = (String) settingsCourseModel.getValueAt(row, 0);
        Course course = Memory.courses.get(code);
        
        if (course != null) {
            tfSettingsCode.setText(course.code);
            tfSettingsName.setText(course.name);
            spSettingsCredits.setValue(course.credits);
            
            // Set loại học phần dựa trên mã
            String type = getCourseType(course.code);
            for (int i = 0; i < cbSettingsTypeForm.getItemCount(); i++) {
                if (type.equals(cbSettingsTypeForm.getItemAt(i))) {
                    cbSettingsTypeForm.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Sửa học phần (fill form và focus vào form)
     */
    void editSettingsCourse(int row) {
        fillSettingsFormFromTable(row);
        settingsCourseTable.setRowSelectionInterval(row, row);
        // Có thể scroll đến form nếu cần
    }
    
    /**
     * Tạo panel Cài đặt kỳ học
     */
    /**
     * Tạo panel "Cài đặt kỳ học" (Tab 4)
     * 
     * Panel này cho phép admin quản lý học kỳ:
     * - Xem danh sách tất cả học kỳ trong hệ thống
     * - Tìm kiếm học kỳ theo mã hoặc tên
     * - Lọc học kỳ theo năm học và trạng thái hoạt động
     * - Thêm học kỳ mới
     * - Sửa thông tin học kỳ (tên, năm học, ngày bắt đầu/kết thúc)
     * - Mở/đóng đăng ký cho từng học kỳ
     * - Xem thống kê học kỳ (số lượng đăng ký, v.v.)
     * 
     * Cấu trúc:
     * - Filter section: Tìm kiếm, lọc theo năm học và trạng thái, nút thêm học kỳ
     * - Table section: Bảng danh sách học kỳ với các cột:
     *   + Mã học kỳ, Tên học kỳ, Năm học, Ngày bắt đầu, Ngày kết thúc,
     *   + Trạng thái đăng ký (Mở/Đóng), Trạng thái hoạt động, Hành động
     * 
     * Màu sắc cột "Trạng thái hoạt động":
     * - Xanh lá: Đang hoạt động
     * - Xám: Đã kết thúc
     * 
     * @return Panel chứa giao diện cài đặt kỳ học
     */
    JPanel createTermSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBackground(new Color(249, 250, 251));
        
        // ========== FILTER SECTION ==========
        CardPanel filterCard = new CardPanel();
        filterCard.setLayout(new BorderLayout());
        filterCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header của filter
        JLabel filterTitle = new JLabel("Bộ lọc tìm kiếm");
        filterTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        filterTitle.setForeground(new Color(31, 41, 55));
        filterTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        filterPanel.setOpaque(false);
        
        // Tìm kiếm
        JLabel lbSearch = new JLabel("TỪ KHÓA:");
        lbSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbSearch.setForeground(new Color(55, 65, 81));
        searchTermField.setPreferredSize(new Dimension(300, 36));
        searchTermField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchTermField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchTermField.setToolTipText("Nhập tên học kỳ...");
        searchTermField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshTermSettingsTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshTermSettingsTable(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshTermSettingsTable(); }
        });
        
        // Nút Lọc
        JButton btnFilter = new JButton("Lọc");
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnFilter.setBackground(new Color(59, 130, 246));
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setBorderPainted(false);
        btnFilter.setPreferredSize(new Dimension(100, 36));
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.addActionListener(e -> refreshTermSettingsTable());
        
        // Nút tạo học kỳ mới
        JButton btnCreateTerm = new JButton("+ Tạo học kỳ mới");
        btnCreateTerm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCreateTerm.setBackground(new Color(16, 185, 129));
        btnCreateTerm.setForeground(Color.WHITE);
        btnCreateTerm.setBorderPainted(false);
        btnCreateTerm.setPreferredSize(new Dimension(160, 36));
        btnCreateTerm.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreateTerm.addActionListener(e -> showCreateTermDialog());
        
        // Hover effect cho nút tạo học kỳ
        btnCreateTerm.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnCreateTerm.setBackground(new Color(5, 150, 105));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnCreateTerm.setBackground(new Color(16, 185, 129));
            }
        });
        
        filterPanel.add(lbSearch);
        filterPanel.add(searchTermField);
        filterPanel.add(btnFilter);
        filterPanel.add(Box.createHorizontalStrut(20)); // Khoảng cách
        filterPanel.add(btnCreateTerm);
        
        filterCard.add(filterTitle, BorderLayout.NORTH);
        filterCard.add(filterPanel, BorderLayout.CENTER);
        
        // ========== TABLE SECTION ==========
        termSettingsModel = new DefaultTableModel(new Object[]{
                "Mã học kỳ", "Tên học kỳ", "Năm học", "Thời gian", "Trạng thái", "Hành động"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        termSettingsTable = new JTable(termSettingsModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                return c;
            }
        };
        termSettingsTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        termSettingsTable.setRowHeight(50);
        termSettingsTable.setShowGrid(true);
        termSettingsTable.setGridColor(new Color(229, 231, 235));
        
        // Custom header
        JTableHeader header = termSettingsTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(243, 244, 246));
        header.setForeground(new Color(31, 41, 55));
        header.setPreferredSize(new Dimension(0, 45));
        header.setReorderingAllowed(false);
        
        termSettingsTable.setSelectionBackground(new Color(239, 246, 255));
        termSettingsTable.setSelectionForeground(Color.BLACK);
        
        // Column widths
        termSettingsTable.getColumnModel().getColumn(0).setPreferredWidth(120);  // Mã học kỳ
        termSettingsTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Tên học kỳ
        termSettingsTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Năm học
        termSettingsTable.getColumnModel().getColumn(3).setPreferredWidth(200);  // Thời gian
        termSettingsTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Trạng thái
        termSettingsTable.getColumnModel().getColumn(5).setPreferredWidth(200);  // Hành động
        
        // Custom renderer cho cột TRẠNG THÁI
        termSettingsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                if ("Đang hoạt động".equals(status)) {
                    c.setBackground(new Color(220, 255, 220));
                    c.setForeground(new Color(22, 163, 74));
                } else if ("Đã kết thúc".equals(status)) {
                    c.setBackground(new Color(243, 244, 246));
                    c.setForeground(new Color(107, 114, 128));
                }
                return c;
            }
        });
        
        // Custom renderer cho cột HÀNH ĐỘNG
        termSettingsTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
                panel.setOpaque(true);
                panel.setBackground(isSelected ? table.getSelectionBackground() : 
                    (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));

                JButton btnEdit = new JButton("Sửa");
                btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnEdit.setBackground(new Color(59, 130, 246));
                btnEdit.setForeground(Color.WHITE);
                btnEdit.setBorderPainted(false);
                btnEdit.setPreferredSize(new Dimension(70, 32));
                btnEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnEdit.setToolTipText("Sửa học kỳ");

                JButton btnDelete = new JButton("Xóa");
                btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnDelete.setBackground(new Color(239, 68, 68));
                btnDelete.setForeground(Color.WHITE);
                btnDelete.setBorderPainted(false);
                btnDelete.setPreferredSize(new Dimension(70, 32));
                btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnDelete.setToolTipText("Xóa học kỳ");

                panel.add(btnEdit);
                panel.add(btnDelete);
                return panel;
            }
        });
        
        // MouseListener để detect click vào buttons
        termSettingsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = termSettingsTable.rowAtPoint(e.getPoint());
                int col = termSettingsTable.columnAtPoint(e.getPoint());
                
                if (row >= 0 && col == 5) { // Hành động ở cột 5
                    Component comp = termSettingsTable.prepareRenderer(
                            termSettingsTable.getCellRenderer(row, col), row, col);
                    Rectangle cellRect = termSettingsTable.getCellRect(row, col, false);
                    comp.setBounds(cellRect);
                    comp.doLayout();
                    
                    Point relativePoint = new Point(e.getX() - cellRect.x, e.getY() - cellRect.y);
                    Component clickedComp = SwingUtilities.getDeepestComponentAt(comp, relativePoint.x, relativePoint.y);
                    
                    if (clickedComp instanceof JButton) {
                        JButton clickedBtn = (JButton) clickedComp;
                        String text = clickedBtn.getText();
                        
                        if ("Sửa".equals(text)) {
                            editTerm(row);
                        } else if ("Xóa".equals(text)) {
                            deleteTerm(row);
                        }
                    }
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(termSettingsTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        tableScrollPane.setViewportBorder(null);


        CardPanel tableCard = new CardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel tableTitle = new JLabel("Danh sách học kỳ");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(new Color(31, 41, 55));
        tableTitle.setBorder(new EmptyBorder(0, 0, 16, 0));
        
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(tableScrollPane, BorderLayout.CENTER);
        
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(tableWrapper, BorderLayout.CENTER);
        
        panel.add(filterCard, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Lấy danh sách kỳ học đã lọc
     */
    List<String> getFilteredTerms() {
        List<String> filtered = new ArrayList<>();
        String searchText = searchTermField.getText().toLowerCase().trim();
        
        for (String term : Memory.loadTerms()) {
            TermSetting setting = Memory.termSettings.get(term);
            if (setting == null) setting = new TermSetting(true);
            
            // Filter by search
            if (!searchText.isEmpty()) {
                String termName = setting.termName != null ? setting.termName.toLowerCase() : "";
                if (!term.toLowerCase().contains(searchText) && !termName.contains(searchText)) {
                    continue;
                }
            }
            
            filtered.add(term);
        }
        
        return filtered;
    }
    
    /**
     * Cập nhật combobox năm học
     */
    void updateAcademicYearComboBox() {
        Set<String> years = new TreeSet<>(Collections.reverseOrder());
        for (String term : Memory.loadTerms()) {
            TermSetting setting = Memory.termSettings.get(term);
            if (setting != null && setting.academicYear != null && !setting.academicYear.isEmpty()) {
                years.add(setting.academicYear);
            }
        }
        
        String selected = (String) cbTermAcademicYear.getSelectedItem();
        cbTermAcademicYear.removeAllItems();
        cbTermAcademicYear.addItem("Tất cả năm học");
        for (String year : years) {
            cbTermAcademicYear.addItem(year);
        }
        if (selected != null) {
            for (int i = 0; i < cbTermAcademicYear.getItemCount(); i++) {
                if (selected.equals(cbTermAcademicYear.getItemAt(i))) {
                    cbTermAcademicYear.setSelectedIndex(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Refresh (làm mới) bảng danh sách học kỳ trong tab "Cài đặt kỳ học"
     * 
     * Phương thức này:
     * 1. Xóa tất cả dữ liệu cũ trong bảng
     * 2. Lấy danh sách học kỳ đã được lọc (getFilteredTerms)
     * 3. Với mỗi học kỳ:
     *    - Lấy TermSetting từ Memory (tạo mới nếu chưa có)
     *    - Lấy thông tin: tên học kỳ, năm học, ngày bắt đầu/kết thúc
     *    - Xác định trạng thái đăng ký (Mở/Đóng) và trạng thái hoạt động
     *    - Thêm vào bảng
     * 
     * Các cột trong bảng:
     * - Mã học kỳ, Tên học kỳ, Năm học, Thời gian (ngày bắt đầu - ngày kết thúc),
     *   Trạng thái đăng ký, Trạng thái hoạt động, Hành động
     * 
     * Được gọi khi:
     * - Thay đổi filter (năm học, trạng thái)
     * - Thay đổi từ khóa tìm kiếm
     * - Sau khi thêm/sửa/xóa học kỳ
     * - Sau khi mở/đóng đăng ký cho học kỳ
     */
    void refreshTermSettingsTable() {
        termSettingsModel.setRowCount(0);
        List<String> filtered = getFilteredTerms();
        
        // Hiển thị tất cả kết quả (không phân trang)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        for (int i = 0; i < filtered.size(); i++) {
            String term = filtered.get(i);
            TermSetting setting = Memory.termSettings.get(term);
            if (setting == null) {
                setting = new TermSetting(true);
                Memory.termSettings.put(term, setting);
            }
            
            // Tên học kỳ
            String termName = setting.termName != null && !setting.termName.isEmpty() 
                    ? setting.termName : "Học kỳ " + term.substring(term.length() - 1) + " Năm học " + (term.length() >= 4 ? term.substring(0, 4) : "");
            
            // Năm học
            String academicYear = setting.academicYear != null && !setting.academicYear.isEmpty() 
                    ? setting.academicYear : (term.length() >= 4 ? term.substring(0, 4) + "-" + String.valueOf(Integer.parseInt(term.substring(0, 4)) + 1) : "");
            
            // Thời gian
            String timeRange = "-";
            Date startDate = setting.startDate;
            Date endDate = setting.endDate;
            
            // Nếu không có ngày, tạo ngày mặc định dựa trên mã học kỳ
            if (startDate == null && endDate == null) {
                try {
                    // Parse mã học kỳ (ví dụ: "20252" -> năm 2025, kỳ 2)
                    if (term.length() >= 5) {
                        int year = Integer.parseInt(term.substring(0, 4));
                        int semester = Integer.parseInt(term.substring(4));
                        
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        
                        // Thiết lập ngày bắt đầu dựa trên kỳ học
                        if (semester == 1) {
                            // Học kỳ 1: Tháng 9
                            cal.set(year, java.util.Calendar.SEPTEMBER, 1, 0, 0, 0);
                        } else if (semester == 2) {
                            // Học kỳ 2: Tháng 2
                            cal.set(year, java.util.Calendar.FEBRUARY, 1, 0, 0, 0);
                        } else {
                            // Học kỳ hè hoặc khác: Tháng 6
                            cal.set(year, java.util.Calendar.JUNE, 1, 0, 0, 0);
                        }
                        cal.set(java.util.Calendar.MILLISECOND, 0);
                        startDate = cal.getTime();
                        
                        // Ngày kết thúc = ngày bắt đầu + 4 tháng
                        cal.add(java.util.Calendar.MONTH, 4);
                        endDate = cal.getTime();
                        
                        // Lưu lại vào setting để lần sau không phải tính lại
                        setting.startDate = startDate;
                        setting.endDate = endDate;
                    }
                } catch (NumberFormatException e) {
                    // Nếu không parse được, giữ nguyên null
                }
            }
            
            if (startDate != null && endDate != null) {
                timeRange = sdf.format(startDate) + " đến " + sdf.format(endDate);
            } else if (startDate != null) {
                timeRange = sdf.format(startDate) + " đến -";
            } else if (endDate != null) {
                timeRange = "- đến " + sdf.format(endDate);
            }
            
            // Trạng thái
            String status = setting.isActive() ? "Đang hoạt động" : "Đã kết thúc";
            
            termSettingsModel.addRow(new Object[]{
                    term,
                    termName,
                    academicYear,
                    timeRange,
                    status,
                    ""
            });
        }
    }
    
    
    /**
     * Đếm số sinh viên đăng ký trong một học kỳ
     */
    int countStudentsInTerm(String term) {
        Set<String> studentIds = new HashSet<>();
        for (Map.Entry<String, Map<String, List<RegItem>>> studentEntry : Memory.regs.entrySet()) {
            Map<String, List<RegItem>> termRegs = studentEntry.getValue();
            if (termRegs.containsKey(term) && !termRegs.get(term).isEmpty()) {
                studentIds.add(studentEntry.getKey());
            }
        }
        return studentIds.size();
    }
    
    /**
     * Sửa học kỳ
     */
    void editTerm(int row) {
        String termCode = (String) termSettingsModel.getValueAt(row, 0);
        showEditTermDialog(termCode);
    }
    
    /**
     * Xóa học kỳ
     */
    void deleteTerm(int row) {
        String termCode = (String) termSettingsModel.getValueAt(row, 0);
        
        // Kiểm tra xem có sinh viên đăng ký trong kỳ này không
        int studentCount = countStudentsInTerm(termCode);
        if (studentCount > 0) {
            JOptionPane.showMessageDialog(this, 
                    "Không thể xóa học kỳ " + termCode + " vì đã có " + studentCount + " sinh viên đăng ký.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa học kỳ " + termCode + "?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            Memory.terms.remove(termCode);
            Memory.termSettings.remove(termCode);
            if (Memory.offerings.containsKey(termCode)) {
                Memory.offerings.remove(termCode);
            }
            JOptionPane.showMessageDialog(this, "Đã xóa học kỳ thành công!");
            refreshTermSettingsTable();
            loadTerms();
            refreshCourseTable();
            updateApprovalTermComboBox();
        }
    }
    
    /**
     * Hiển thị dialog sửa học kỳ
     */
    void showEditTermDialog(String oldTermCode) {
        JDialog dialog = new JDialog(this, "", true);
        dialog.setSize(550, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        dialog.setBackground(new Color(0, 0, 0, 0)); // Transparent để hiển thị shadow
        
        // Main container với shadow và border radius mượt mà
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int radius = 20;
                int shadowOffset = 8;
                
                // Vẽ multiple shadows để tạo hiệu ứng đẹp hơn
                for (int i = shadowOffset; i >= 0; i--) {
                    float alpha = (float)(0.08 - (i * 0.01));
                    if (alpha > 0) {
                        g2.setColor(new Color(0, 0, 0, alpha));
                        g2.fillRoundRect(i, i, getWidth() - shadowOffset, getHeight() - shadowOffset, radius, radius);
                    }
                }
                
                // Vẽ nền trắng bo góc mượt
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - shadowOffset, getHeight() - shadowOffset, radius, radius);
                
                // Vẽ border nhẹ
                g2.setColor(new Color(229, 231, 235));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1, radius, radius);
                
                g2.dispose();
            }
        };
        mainContainer.setOpaque(false);
        mainContainer.setBorder(new EmptyBorder(0, 0, 8, 8));
        
        // Header với HTML styling
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(28, 30, 24, 32));
        
        // Title với HTML và underline xanh
        JLabel titleLabel = new JLabel("<html><div style='font-size: 17.6px; font-weight: 700; color: #111827; line-height: 1.2; border-bottom: 3px solid #10B981; padding-bottom: 8px; display: inline-block;'>" +
                "Sửa học kỳ</div></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        
        // Nút đóng với HTML styling
        JButton btnClose = new JButton("<html><div style='font-size: 18px; color: #6B7280; text-align: center;'>✕</div></html>");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        btnClose.setBackground(Color.WHITE);
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setPreferredSize(new Dimension(36, 36));
        btnClose.addActionListener(e -> dialog.dispose());
        
        btnClose.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnClose.setText("<html><div style='font-size: 18px; color: #EF4444; text-align: center; background: #FEF2F2; border-radius: 8px; padding: 4px;'>✕</div></html>");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnClose.setText("<html><div style='font-size: 18px; color: #6B7280; text-align: center;'>✕</div></html>");
            }
        });
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(btnClose, BorderLayout.EAST);
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 30, 24, 32));
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Lấy thông tin hiện tại
        TermSetting currentSetting = Memory.termSettings.get(oldTermCode);
        if (currentSetting == null) {
            currentSetting = new TermSetting(true);
        }
        
        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 15);
        
        // Mã học kỳ (read-only khi sửa)
        JLabel lbTermCode = new JLabel("Mã học kỳ:");
        lbTermCode.setFont(labelFont);
        lbTermCode.setForeground(new Color(55, 65, 81));
        lbTermCode.setBorder(new EmptyBorder(0, 0, 8, 0));
        lbTermCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField tfTermCode = new JTextField(oldTermCode);
        tfTermCode.setFont(inputFont);
        tfTermCode.setPreferredSize(new Dimension(0, 40));
        tfTermCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tfTermCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfTermCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        tfTermCode.setEditable(false); // Không cho sửa mã học kỳ
        tfTermCode.setBackground(new Color(243, 244, 246));
        
        // Tên học kỳ
        JLabel lbTermName = new JLabel("Tên học kỳ:");
        lbTermName.setFont(labelFont);
        lbTermName.setForeground(new Color(55, 65, 81));
        lbTermName.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbTermName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        PlaceholderTextField tfTermName = new PlaceholderTextField("Nhập tên học kỳ...");
        if (currentSetting.termName != null && !currentSetting.termName.isEmpty()) {
            tfTermName.setText(currentSetting.termName);
        }
        tfTermName.setFont(inputFont);
        tfTermName.setPreferredSize(new Dimension(0, 40));
        tfTermName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tfTermName.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfTermName.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        // Năm học
        JLabel lbAcademicYear = new JLabel("Năm học:");
        lbAcademicYear.setFont(labelFont);
        lbAcademicYear.setForeground(new Color(55, 65, 81));
        lbAcademicYear.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbAcademicYear.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        PlaceholderTextField tfAcademicYear = new PlaceholderTextField("Ví dụ: 2025");
        if (currentSetting.academicYear != null && !currentSetting.academicYear.isEmpty()) {
            tfAcademicYear.setText(currentSetting.academicYear);
        }
        tfAcademicYear.setFont(inputFont);
        tfAcademicYear.setPreferredSize(new Dimension(0, 40));
        tfAcademicYear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        tfAcademicYear.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfAcademicYear.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        
        // Trạng thái
        JLabel lbStatus = new JLabel("Trạng thái:");
        lbStatus.setFont(labelFont);
        lbStatus.setForeground(new Color(55, 65, 81));
        lbStatus.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Đang hoạt động", "Đã kết thúc"});
        cbStatus.setFont(inputFont);
        cbStatus.setPreferredSize(new Dimension(0, 40));
        cbStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        // Xác định trạng thái dựa trên registrationOpen để có thể thay đổi được
        cbStatus.setSelectedIndex(currentSetting.registrationOpen ? 0 : 1); // 0 = Đang hoạt động, 1 = Đã kết thúc
        
        // Thời gian mở đăng ký bắt đầu
        JLabel lbStartDate = new JLabel("Thời gian mở đăng ký (bắt đầu):");
        lbStartDate.setFont(labelFont);
        lbStartDate.setForeground(new Color(55, 65, 81));
        lbStartDate.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbStartDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JSpinner spStartDate = new JSpinner(new SpinnerDateModel(
                currentSetting.startDate != null ? currentSetting.startDate : new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor startDateEditor = new JSpinner.DateEditor(spStartDate, "dd/MM/yyyy");
        spStartDate.setEditor(startDateEditor);
        ((JSpinner.DefaultEditor) spStartDate.getEditor()).getTextField().setEditable(false);
        spStartDate.setPreferredSize(new Dimension(0, 40));
        spStartDate.setFont(inputFont);
        spStartDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        spStartDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        // Thời gian mở đăng ký kết thúc
        JLabel lbEndDate = new JLabel("Thời gian mở đăng ký (kết thúc):");
        lbEndDate.setFont(labelFont);
        lbEndDate.setForeground(new Color(55, 65, 81));
        lbEndDate.setBorder(new EmptyBorder(16, 0, 8, 0));
        lbEndDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JSpinner spEndDate = new JSpinner(new SpinnerDateModel(
                currentSetting.endDate != null ? currentSetting.endDate : new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor endDateEditor = new JSpinner.DateEditor(spEndDate, "dd/MM/yyyy");
        spEndDate.setEditor(endDateEditor);
        ((JSpinner.DefaultEditor) spEndDate.getEditor()).getTextField().setEditable(false);
        spEndDate.setPreferredSize(new Dimension(0, 40));
        spEndDate.setFont(inputFont);
        spEndDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        spEndDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        
        contentPanel.add(lbTermCode);
        contentPanel.add(tfTermCode);
        contentPanel.add(lbTermName);
        contentPanel.add(tfTermName);
        contentPanel.add(lbAcademicYear);
        contentPanel.add(tfAcademicYear);
        contentPanel.add(lbStatus);
        contentPanel.add(cbStatus);
        contentPanel.add(lbStartDate);
        contentPanel.add(spStartDate);
        contentPanel.add(lbEndDate);
        contentPanel.add(spEndDate);
        contentPanel.add(Box.createVerticalGlue());
        
        // Button panel với HTML styling
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(24, 30, 32, 32));
        
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonContainer.setOpaque(false);
        
        JButton btnCancel = new JButton("<html><div style='font-size: 11.2px; color: #6B7280; padding: 8px 19px;'>Hủy</div></html>");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        btnCancel.setBackground(Color.WHITE);
        btnCancel.setBorderPainted(false);
        btnCancel.setContentAreaFilled(true);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnCancel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnCancel.setBackground(new Color(249, 250, 251));
                btnCancel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnCancel.setBackground(Color.WHITE);
                btnCancel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                ));
            }
        });
        
        JButton btnSave = new JButton("<html><div style='font-size: 11.2px; font-weight: 600; color: #FFFFFF; padding: 8px 19px;'>Lưu thay đổi</div></html>");
        btnSave.setFont(new Font("Segoe UI", Font.PLAIN, 1));
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setBorderPainted(false);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            String termName = tfTermName.getText().trim();
            String academicYear = tfAcademicYear.getText().trim();
            Date startDate = (Date) spStartDate.getValue();
            Date endDate = (Date) spEndDate.getValue();
            
            // Logic trạng thái: "Đang hoạt động" = registrationOpen = true
            // "Đã kết thúc" = registrationOpen = false
            boolean registrationOpen = cbStatus.getSelectedIndex() == 0; // 0 = Đang hoạt động, 1 = Đã kết thúc
            
            Date now = new Date();
            
            // Nếu chọn "Đã kết thúc" và endDate chưa qua, tự động set endDate = now
            if (cbStatus.getSelectedIndex() == 1) {
                if (endDate == null || endDate.after(now)) {
                    endDate = now;
                    spEndDate.setValue(endDate);
                }
            } else {
                // Nếu chọn "Đang hoạt động" và endDate đã qua, cần set endDate trong tương lai
                // Hoặc nếu endDate = now (do chọn "Đã kết thúc" trước đó), set endDate = null hoặc trong tương lai
                if (endDate != null && (endDate.before(now) || endDate.equals(now))) {
                    // Set endDate = startDate + 3 tháng (hoặc null nếu startDate null)
                    if (startDate != null) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(startDate);
                        cal.add(java.util.Calendar.MONTH, 3);
                        endDate = cal.getTime();
                        spEndDate.setValue(endDate);
                    } else {
                        // Nếu không có startDate, set endDate = now + 3 tháng
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.add(java.util.Calendar.MONTH, 3);
                        endDate = cal.getTime();
                        spEndDate.setValue(endDate);
                    }
                }
            }
            
            if (updateTerm(oldTermCode, oldTermCode, termName, academicYear, startDate, endDate, registrationOpen)) {
                dialog.dispose();
                refreshTermSettingsTable();
                loadTerms();
                refreshCourseTable();
                updateApprovalTermComboBox();
                updateAcademicYearComboBox();
            }
        });
        
        btnSave.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnSave.setBackground(new Color(5, 150, 105));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btnSave.setBackground(new Color(16, 185, 129));
            }
        });
        
        buttonContainer.add(btnCancel);
        buttonContainer.add(btnSave);
        buttonPanel.add(buttonContainer, BorderLayout.EAST);
        
        // Wrap content panel trong ScrollPane để có thể scroll nếu cần
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Assemble dialog
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(scrollPane, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainContainer, BorderLayout.CENTER);
        
        // Focus vào input field khi mở dialog
        SwingUtilities.invokeLater(() -> {
            tfTermName.requestFocus();
        });
        
        dialog.setVisible(true);
    }
    
    /**
     * Cập nhật học kỳ
     */
    boolean updateTerm(String oldTermCode, String newTermCode, String termName, String academicYear, Date startDate, Date endDate, boolean registrationOpen) {
        if (newTermCode == null || newTermCode.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã học kỳ.");
            return false;
        }
        
        newTermCode = newTermCode.trim();
        
        // Validate thời gian
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            JOptionPane.showMessageDialog(this, "Ngày bắt đầu phải trước ngày kết thúc!");
            return false;
        }
        
        // Kiểm tra học kỳ mới đã tồn tại chưa (trừ học kỳ hiện tại)
        if (!oldTermCode.equals(newTermCode) && Memory.terms.contains(newTermCode)) {
            JOptionPane.showMessageDialog(this, 
                    "Học kỳ " + newTermCode + " đã tồn tại. Vui lòng nhập mã học kỳ khác.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Cập nhật mã học kỳ trong danh sách (nếu thay đổi)
        if (!oldTermCode.equals(newTermCode)) {
            int index = Memory.terms.indexOf(oldTermCode);
            if (index >= 0) {
                Memory.terms.set(index, newTermCode);
            }
        }
        
        // Cập nhật termSettings với thông tin mới
        TermSetting setting = Memory.termSettings.get(oldTermCode);
        if (setting == null) {
            setting = new TermSetting(true);
        }
        
        // Cập nhật thông tin
        boolean oldRegistrationOpen = setting.registrationOpen;
        setting.registrationOpen = registrationOpen;
        setting.termName = termName;
        setting.academicYear = academicYear;
        setting.startDate = startDate;
        setting.endDate = endDate;
        
        // Nếu mã học kỳ thay đổi, cần di chuyển setting
        String actualTermCode = oldTermCode.equals(newTermCode) ? oldTermCode : newTermCode;
        if (!oldTermCode.equals(newTermCode)) {
            Memory.termSettings.remove(oldTermCode);
            Memory.termSettings.put(newTermCode, setting);
        }
        
        // Nếu trạng thái chuyển từ "Đang hoạt động" sang "Đã kết thúc", đóng tất cả các Offering
        if (oldRegistrationOpen && !registrationOpen) {
            closeAllOfferings(actualTermCode);
        }
        
        // Cập nhật offerings (nếu mã học kỳ thay đổi)
        if (!oldTermCode.equals(newTermCode)) {
            if (Memory.offerings.containsKey(oldTermCode)) {
                Map<String, Offering> offerings = Memory.offerings.remove(oldTermCode);
                Memory.offerings.put(newTermCode, offerings);
            }
            
            // Cập nhật regs (đăng ký của sinh viên)
            for (Map<String, List<RegItem>> termRegs : Memory.regs.values()) {
                if (termRegs.containsKey(oldTermCode)) {
                    List<RegItem> regItems = termRegs.remove(oldTermCode);
                    termRegs.put(newTermCode, regItems);
                }
            }
        }
        
        JOptionPane.showMessageDialog(this, 
                "Đã cập nhật học kỳ thành công!",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
        return true;
    }
    
    /**
     * Đóng tất cả các Offering (học phần) của một học kỳ
     * 
     * Được gọi khi học kỳ chuyển sang trạng thái "Đã kết thúc"
     * để đảm bảo sinh viên không thể đăng ký các học phần trong học kỳ đó nữa.
     * 
     * @param termCode Mã học kỳ cần đóng tất cả các Offering
     */
    void closeAllOfferings(String termCode) {
        if (termCode == null || termCode.isEmpty()) return;
        
        Map<String, Offering> termOfferings = Memory.offerings.get(termCode);
        if (termOfferings != null) {
            for (Offering offering : termOfferings.values()) {
                offering.open = false;
            }
        }
    }
    
    /**
     * Mở tất cả các học phần trong học kỳ đã chọn
     * 
     * Mở tất cả các Offering của học kỳ hiện tại trong combobox cbTermCourse.
     * Nếu học phần chưa có Offering, sẽ tạo mới với trạng thái mở.
     */
    void openAllCourses() {
        String term = (String) cbTermCourse.getSelectedItem();
        if (term == null || term.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn học kỳ trước.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn mở tất cả các học phần trong học kỳ " + term + "?",
                "Xác nhận mở tất cả",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        int openedCount = 0;
        for (Course course : Memory.courses.values()) {
            Offering offering = Memory.getOffering(term, course.code);
            if (offering == null) {
                // Tạo mới Offering nếu chưa có
                Memory.setOffering(term, course.code, true, "Tất cả");
                openedCount++;
            } else if (!offering.open) {
                // Mở Offering nếu đang đóng
                offering.open = true;
                openedCount++;
            }
        }
        
        refreshCourseTable();
        JOptionPane.showMessageDialog(this, 
                "Đã mở " + openedCount + " học phần thành công!", 
                "Thành công", 
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Đóng tất cả các học phần trong học kỳ đã chọn
     * 
     * Đóng tất cả các Offering của học kỳ hiện tại trong combobox cbTermCourse.
     */
    void closeAllCourses() {
        String term = (String) cbTermCourse.getSelectedItem();
        if (term == null || term.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn học kỳ trước.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn đóng tất cả các học phần trong học kỳ " + term + "?",
                "Xác nhận đóng tất cả",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        int closedCount = 0;
        Map<String, Offering> termOfferings = Memory.offerings.get(term);
        if (termOfferings != null) {
            for (Offering offering : termOfferings.values()) {
                if (offering.open) {
                    offering.open = false;
                    closedCount++;
                }
            }
        }
        
        refreshCourseTable();
        JOptionPane.showMessageDialog(this, 
                "Đã đóng " + closedCount + " học phần thành công!", 
                "Thành công", 
                JOptionPane.INFORMATION_MESSAGE);
    }
}
