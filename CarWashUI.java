import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * หน้าต่าง GUI ของระบบศูนย์บริการล้างรถ
 *
 * โครงสร้างไฟล์ (อ่านจากบนลงล่าง):
 *   1. ค่าคงที่ (รหัสผ่าน ชื่อคอลัมน์ สี)
 *   2. Constructor
 *   3. คลาสตกแต่ง (ปุ่มโค้ง การ์ด ฯลฯ)
 *   4. เมธอดช่วยสร้างหน้าจอ
 *   5. หน้าเข้าสู่ระบบ
 *   6. หน้าลูกค้า
 *   (หน้าเจ้าของร้านอยู่ในไฟล์ OwnerScreen.java)
 *
 * คลาสนี้ทำหน้าที่แค่แสดงผลและรับข้อมูลจากผู้ใช้
 * กติกาธุรกิจทั้งหมดอยู่ใน QueueManager
 */
public class CarWashUI extends JFrame {

    // ==========================================
    // 1. ค่าคงที่
    // ==========================================

    private static final String OWNER_PASSWORD = "1234";
    static final String ALL = "ทั้งหมด"; // ตัวเลือก "ทุกสถานะ" ในช่องกรอง

    // หัวตารางการจอง (ลำดับต้องตรงกับ CUSTOMER.toRow())
    static final String[] COLUMNS = {
            "รหัส", "ชื่อ", "โทร", "ทะเบียน", "ประเภทรถ", "บริการ", "วันที่", "เวลา", "ราคา", "สถานะ"};
    static final int COL_ID = 0;
    static final int COL_NAME = 1;
    static final int COL_PHONE = 2;
    static final int COL_STATUS = 9;

    // สีหลักของโปรแกรม
    static final Color NAVY = new Color(0x0F172A);
    static final Color SLATE = new Color(0x334155);
    static final Color BLUE = new Color(0x2563EB);
    static final Color SKY = new Color(0x38BDF8);
    static final Color BG = new Color(0xF1F5F9);
    static final Color LINE = new Color(0xE2E8F0);
    static final Color TEXT = new Color(0x1E293B);
    static final Color MUTED = new Color(0x64748B);
    static final Color GREEN = new Color(0x16A34A);
    static final Color RED = new Color(0xDC2626);
    static final Color AMBER = new Color(0xD97706);

    private final QueueManager qm;
    private final JPanel root = new JPanel(new BorderLayout());

    // ==========================================
    // 2. Constructor
    // ==========================================

    public CarWashUI(QueueManager qm) {
        super("ระบบศูนย์บริการล้างรถ");
        this.qm = qm;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);

        root.setBackground(BG);
        setContentPane(root);
        showScreen(buildLoginScreen()); // เริ่มที่หน้าเข้าสู่ระบบ
    }

    // ==========================================
    // 3. คลาสตกแต่ง (ไม่มีตรรกะของระบบ)
    // ==========================================

    /** ปุ่มมุมโค้ง เปลี่ยนสีเข้มขึ้นเมื่อเมาส์ชี้ */
    static class RoundButton extends JButton {
        private final Color color;
        private boolean hovered = false;

        RoundButton(String text, Color color) {
            super(text);
            this.color = color;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) g2.setColor(new Color(0xCBD5E1));
            else if (hovered) g2.setColor(color.darker());
            else g2.setColor(color);

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** การ์ดสีขาว มุมโค้ง มีเส้นขอบบาง */
    static class Card extends JPanel {
        Card(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(18, 18, 18, 18));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** แผงเรียงของจากบนลงล่าง ยืดเต็มความกว้าง (ใช้ put เพิ่มของทีละชิ้น) */
    static class VerticalStack extends JPanel {
        private final GridBagConstraints rules = new GridBagConstraints();
        private final int minWidth;

        VerticalStack(int minWidth) {
            super(new GridBagLayout());
            this.minWidth = minWidth;
            setOpaque(false);
            rules.gridx = 0;
            rules.gridy = 0;
            rules.weightx = 1;
            rules.fill = GridBagConstraints.HORIZONTAL;
            rules.anchor = GridBagConstraints.NORTH;
            rules.insets = new Insets(0, 0, 12, 0); // ช่องว่างใต้แต่ละชิ้น
        }

        VerticalStack put(JComponent item) {
            add(item, rules);
            rules.gridy++; // ชิ้นถัดไปอยู่แถวถัดไป
            return this;
        }

        @Override public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(size.width, minWidth), size.height);
        }
    }

    /** พื้นหลังไล่สีพร้อมวงกลมตกแต่ง (ฝั่งซ้ายของหน้าเข้าสู่ระบบ) */
    private static class BrandBackground extends JPanel {
        BrandBackground() {
            super(new GridBagLayout());
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new GradientPaint(0, 0, BLUE, getWidth(), getHeight(), NAVY));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 24)); // ขาวโปร่งใส
            g2.fillOval(-90, -90, 320, 320);
            g2.fillOval(getWidth() - 170, getHeight() - 210, 340, 340);
            g2.fillOval(getWidth() / 2 + 40, 60, 100, 100);
            g2.fillOval(40, getHeight() - 140, 70, 70);
            g2.dispose();
        }
    }

    /** วาดเซลล์ตาราง: สลับสีแถว ราคาชิดขวา สถานะมีสี */
    private static class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);

            setBorder(new EmptyBorder(0, 10, 0, 10));
            setForeground(TEXT);
            if (!selected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            }

            String columnName = table.getColumnName(column);

            if (columnName.equals("ราคา")) {
                setHorizontalAlignment(RIGHT);
                if (value instanceof Number) {
                    setText(String.format("%,d", ((Number) value).intValue()));
                }
            } else {
                setHorizontalAlignment(LEFT);
            }

            if (columnName.equals("สถานะ")) {
                setFont(getFont().deriveFont(Font.BOLD));
                String status = String.valueOf(value);
                if (status.equals(QueueManager.DONE)) setForeground(GREEN);
                else if (status.equals(QueueManager.CANCEL)) setForeground(RED);
                else setForeground(AMBER);
            }
            return this;
        }
    }

    /** วาดหัวตาราง */
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setBackground(LINE);
            setForeground(SLATE);
            setFont(getFont().deriveFont(Font.BOLD));
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment("ราคา".equals(String.valueOf(value)) ? RIGHT : LEFT);
            return this;
        }
    }

    // ==========================================
    // 4. เมธอดช่วยสร้างหน้าจอ
    // ==========================================

    // ---------- กล่องข้อความ / เปลี่ยนหน้า ----------

    void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "แจ้งเตือน", JOptionPane.WARNING_MESSAGE);
    }

    void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    /** ถามยืนยัน คืน true ถ้าผู้ใช้กด "ใช่" */
    boolean confirm(String message) {
        int answer = JOptionPane.showConfirmDialog(this, message, "ยืนยัน", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    /** เปลี่ยนหน้าจอทั้งหน้า */
    private void showScreen(JPanel screen) {
        root.removeAll();
        root.add(screen, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    // ---------- สร้างส่วนประกอบพื้นฐาน ----------

    static JLabel createLabel(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
        return label;
    }

    private static void styleTextField(JTextField field) {
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(7, 10, 7, 10)));
        field.setForeground(TEXT);
    }

    static JTextField createTextField(String text) {
        JTextField field = new JTextField(text);
        styleTextField(field);
        return field;
    }

    static JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> box = new JComboBox<>(items);
        box.setBackground(Color.WHITE);
        box.setForeground(TEXT);
        box.setPreferredSize(new Dimension(100, 36));
        return box;
    }

    /** ป้ายเล็กด้านบน + ช่องกรอกด้านล่าง */
    private static JPanel withCaption(String caption, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createLabel(caption, 12f, Font.PLAIN, MUTED), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    /** ห่อเนื้อหาด้วยพื้นหลังเทาและขอบรอบ ๆ */
    static JPanel createPage(JComponent content) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(20, 20, 20, 20));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    static JScrollPane createScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(new LineBorder(LINE));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    /** วางปุ่มชิดขวา */
    static JPanel rightAligned(JComponent button) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.add(button);
        return row;
    }

    /** แถบหัวสีกรมท่า: ชื่อ+คำอธิบายทางซ้าย, เมนูตรงกลาง (ไม่ใส่ก็ได้), ปุ่มทางขวา */
    static JPanel createHeader(String title, String subtitle, JComponent center, JComponent right) {
        JPanel titles = new JPanel(new GridLayout(0, 1));
        titles.setOpaque(false);
        titles.add(createLabel(title, 18f, Font.BOLD, Color.WHITE));
        titles.add(createLabel(subtitle, 12f, Font.PLAIN, new Color(0x94A3B8)));

        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(12, 24, 12, 24));
        header.add(titles, BorderLayout.WEST);
        if (center != null) header.add(center, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    JButton createLogoutButton() {
        JButton button = new RoundButton("ออกจากระบบ", SLATE);
        button.addActionListener(e -> showScreen(buildLoginScreen()));
        return button;
    }

    // ---------- แท็บนำทาง ----------

    static JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }

    /** ตั้งสีตัวอักษรและเส้นใต้ของแท็บ ตามว่ากำลังเลือกอยู่หรือไม่ */
    static void styleTab(JButton button, boolean active,
                                 Color activeText, Color inactiveText, Color activeBar, Color inactiveBar) {
        button.setForeground(active ? activeText : inactiveText);
        button.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 3, 0, active ? activeBar : inactiveBar),
                new EmptyBorder(8, 14, 6, 14)));
    }

    // ---------- วันที่ ----------

    /** แปลงข้อความเป็นวันที่ ถ้าผิดรูปแบบคืน null */
    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** เลื่อนวันที่ในช่องกรอกไปข้างหน้า/ข้างหลัง (ถ้าช่องว่างหรือผิด ใช้วันนี้) */
    private static void shiftDate(JTextField field, int days) {
        LocalDate date = parseDate(field.getText());
        if (date == null) date = LocalDate.now();
        field.setText(date.plusDays(days).toString());
    }

    /** ช่องวันที่ + ปุ่ม < > สำหรับเลื่อนวัน */
    static JPanel createDateField(JTextField dateField) {
        JButton previous = new RoundButton("<", MUTED);
        JButton next = new RoundButton(">", MUTED);
        previous.setBorder(new EmptyBorder(8, 12, 8, 12));
        next.setBorder(new EmptyBorder(8, 12, 8, 12));
        previous.addActionListener(e -> shiftDate(dateField, -1));
        next.addActionListener(e -> shiftDate(dateField, 1));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
        buttons.setOpaque(false);
        buttons.add(previous);
        buttons.add(next);

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.add(dateField, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    /** เรียก action ทุกครั้งที่ข้อความในช่องเปลี่ยน */
    static void onTextChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    // ---------- ตาราง ----------

    /** model ตารางการจอง (แก้ไขในตารางไม่ได้) */
    static DefaultTableModel createBookingModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override public Class<?> getColumnClass(int column) {
                boolean isNumber = (column == COL_ID || column == 8); // รหัส และ ราคา
                return isNumber ? Integer.class : String.class;
            }
        };
    }

    /** ล้างตารางแล้วใส่รายการจองใหม่ทั้งหมด */
    static void loadRows(DefaultTableModel model, List<CUSTOMER> bookings) {
        model.setRowCount(0);
        for (CUSTOMER booking : bookings) {
            model.addRow(booking.toRow());
        }
    }

    static void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(0xDBEAFE));
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);

        CellRenderer renderer = new CellRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(String.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);
        table.setDefaultRenderer(Number.class, renderer);

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
    }

    /** ตั้งความกว้างคอลัมน์ตามชื่อ */
    static void fitColumns(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            int width;
            switch (table.getColumnName(i)) {
                case "รหัส":    width = 55;  break;
                case "เวลา":    width = 70;  break;
                case "ราคา":    width = 80;  break;
                case "วันที่":   width = 100; break;
                case "สถานะ":   width = 120; break;
                case "ทะเบียน": width = 140; break;
                default:        width = 120;
            }
            table.getColumnModel().getColumn(i).setPreferredWidth(width);
        }
    }

    /**
     * คืนรหัสการจองของแถวที่เลือก (ถ้าไม่ได้เลือกจะเตือนและคืน null)
     * ต้องแปลงเลขแถวบนจอเป็นเลขแถวใน model เพราะตารางอาจถูกเรียง/กรองอยู่
     */
    Integer selectedBookingId(JTable table, DefaultTableModel model) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("กรุณาเลือกรายการก่อน");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (Integer) model.getValueAt(modelRow, COL_ID);
    }

    // ==========================================
    // 5. หน้าเข้าสู่ระบบ
    // ==========================================

    /** หน้าเข้าสู่ระบบ: ซ้าย = แบรนด์, ขวา = ฟอร์ม */
    private JPanel buildLoginScreen() {
        JPanel screen = new JPanel(new GridLayout(1, 2));
        screen.add(buildBrandSide());
        screen.add(buildLoginSide());
        return screen;
    }

    private JPanel buildBrandSide() {
        Color bulletColor = new Color(0xDBEAFE);

        VerticalStack content = new VerticalStack(0);
        content.setBorder(new EmptyBorder(0, 40, 0, 40));
        content.put(createLabel("CAR WASH", 42f, Font.BOLD, Color.WHITE));
        content.put(createLabel("ศูนย์บริการล้างรถ", 24f, Font.PLAIN, Color.WHITE));
        content.put((JComponent) Box.createVerticalStrut(18));
        content.put(createLabel("•  จองคิวออนไลน์ เลือกวันและเวลาได้เอง", 15f, Font.PLAIN, bulletColor));
        content.put(createLabel("•  ราคาชัดเจน คำนวณตามประเภทรถ", 15f, Font.PLAIN, bulletColor));
        content.put(createLabel("•  ดูช่วงเวลาว่างและยกเลิกคิวได้ทันที", 15f, Font.PLAIN, bulletColor));

        BrandBackground background = new BrandBackground();
        background.add(content);
        return background;
    }

    /** ฝั่งขวา: แท็บ "ลูกค้า" / "เจ้าของร้าน" สลับฟอร์มด้วย CardLayout */
    private JPanel buildLoginSide() {
        CardLayout cardLayout = new CardLayout();
        JPanel forms = new JPanel(cardLayout);
        forms.setOpaque(false);
        forms.add(buildCustomerLoginForm(), "customer");
        forms.add(buildOwnerLoginForm(), "owner");

        JButton customerTab = createTabButton("ลูกค้า");
        JButton ownerTab = createTabButton("เจ้าของร้าน");

        customerTab.addActionListener(e -> {
            cardLayout.show(forms, "customer");
            styleTab(customerTab, true, BLUE, MUTED, BLUE, LINE);
            styleTab(ownerTab, false, BLUE, MUTED, BLUE, LINE);
        });
        ownerTab.addActionListener(e -> {
            cardLayout.show(forms, "owner");
            styleTab(customerTab, false, BLUE, MUTED, BLUE, LINE);
            styleTab(ownerTab, true, BLUE, MUTED, BLUE, LINE);
        });
        customerTab.doClick(); // เริ่มต้นที่แท็บลูกค้า

        JPanel tabs = new JPanel(new GridLayout(1, 2));
        tabs.setOpaque(false);
        tabs.add(customerTab);
        tabs.add(ownerTab);

        VerticalStack content = new VerticalStack(340);
        content.put(createLabel("ยินดีต้อนรับ", 26f, Font.BOLD, TEXT));
        content.put(createLabel("เลือกประเภทผู้ใช้งานเพื่อเข้าสู่ระบบ", 13f, Font.PLAIN, MUTED));
        content.put(tabs);
        content.put(forms);

        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(Color.WHITE);
        side.add(content);
        return side;
    }

    private VerticalStack buildCustomerLoginForm() {
        JTextField nameField = createTextField("");
        JTextField phoneField = createTextField("");
        JButton loginButton = new RoundButton("เข้าใช้งานสำหรับลูกค้า", BLUE);

        loginButton.addActionListener(e -> {
            String name = QueueManager.clean(nameField.getText());
            String phone = phoneField.getText().trim();

            boolean phoneValid = phone.matches("\\d{9,10}"); // ตัวเลข 9-10 หลัก
            if (name.isEmpty() || !phoneValid) {
                showWarning("กรอกชื่อและเบอร์โทร 9-10 หลักให้ถูกต้อง");
            } else {
                showScreen(buildCustomerScreen(name, phone));
            }
        });

        // กด Enter ในช่องชื่อ -> ไปช่องเบอร์, กด Enter ในช่องเบอร์ -> เข้าสู่ระบบ
        nameField.addActionListener(e -> phoneField.requestFocusInWindow());
        phoneField.addActionListener(e -> loginButton.doClick());

        VerticalStack form = new VerticalStack(0);
        form.put(withCaption("ชื่อ", nameField));
        form.put(withCaption("เบอร์โทร (9-10 หลัก)", phoneField));
        form.put(loginButton);
        return form;
    }

    private VerticalStack buildOwnerLoginForm() {
        JPasswordField passwordField = new JPasswordField();
        styleTextField(passwordField);
        JButton loginButton = new RoundButton("เข้าสู่ระบบเจ้าของ", NAVY);

        loginButton.addActionListener(e -> {
            String typed = new String(passwordField.getPassword());
            if (typed.equals(OWNER_PASSWORD)) {
                showScreen(new OwnerScreen(this, qm).build());
            } else {
                showWarning("รหัสผ่านไม่ถูกต้อง");
            }
        });
        passwordField.addActionListener(e -> loginButton.doClick());

        VerticalStack form = new VerticalStack(0);
        form.put(withCaption("รหัสผ่านเจ้าของร้าน", passwordField));
        form.put(loginButton);
        return form;
    }

    // ==========================================
    // 6. หน้าลูกค้า
    // ==========================================

    private JPanel buildCustomerScreen(String name, String phone) {
        return new CustomerScreen(name, phone).build();
    }

    /**
     * หน้าลูกค้า แยกเป็นคลาสย่อยเพื่อให้ช่องกรอกและตารางเป็นตัวแปรของคลาส
     * ทำให้เมธอดต่าง ๆ เรียกใช้ร่วมกันได้โดยไม่ต้องส่งพารามิเตอร์เยอะ
     *
     * เค้าโครง:
     *   ซ้าย  = ฟอร์มจองคิว
     *   ขวาบน = คิวของฉัน (ตาราง)
     *   ขวาล่าง = ช่วงเวลาว่าง
     */
    private final class CustomerScreen {
        private final String customerName;
        private final String customerPhone;

        // ช่องในฟอร์มจอง
        private final JTextField plateField = createTextField("");
        private final JTextField dateField = createTextField(LocalDate.now().toString());
        private final JComboBox<String> carTypeBox = createComboBox(QueueManager.CARS);
        private final JComboBox<String> serviceBox = createComboBox(qm.names().toArray(new String[0]));
        private final JComboBox<String> slotBox = createComboBox(QueueManager.SLOTS);
        private final JLabel priceLabel = createLabel("", 22f, Font.BOLD, BLUE);

        // ช่วงเวลาว่าง
        private final JLabel availabilityTitle = createLabel(" ", 16f, Font.BOLD, TEXT);
        private final JLabel[] slotChips = new JLabel[QueueManager.SLOTS.length];

        // คิวของฉัน
        private final DefaultTableModel tableModel = createBookingModel();
        private final JTable table = new JTable(tableModel);
        private final JLabel bookingCount = createLabel(" ", 13f, Font.PLAIN, MUTED);

        CustomerScreen(String name, String phone) {
            this.customerName = name;
            this.customerPhone = phone;
        }

        /** ประกอบทั้งหน้า */
        JPanel build() {
            Card bookingForm = buildBookingForm();
            Card myBookings = buildMyBookingsCard();
            Card availability = buildAvailabilityCard();

            // เมื่อเลือกเวลาหรือเปลี่ยนวันที่ ให้วาดช่วงเวลาว่างใหม่
            slotBox.addActionListener(e -> updateAvailability());
            onTextChange(dateField, this::updateAvailability);

            updatePrice();
            refreshBookings();

            JPanel rightColumn = new JPanel(new BorderLayout(0, 16));
            rightColumn.setOpaque(false);
            rightColumn.add(myBookings, BorderLayout.CENTER);
            rightColumn.add(availability, BorderLayout.SOUTH);

            JPanel body = new JPanel(new BorderLayout(16, 0));
            body.setOpaque(false);
            body.add(bookingForm, BorderLayout.WEST);
            body.add(rightColumn, BorderLayout.CENTER);

            JPanel screen = new JPanel(new BorderLayout());
            screen.add(createHeader("สวัสดี, " + customerName, "เบอร์โทร " + customerPhone,
                    null, createLogoutButton()), BorderLayout.NORTH);
            screen.add(createPage(body), BorderLayout.CENTER);
            return screen;
        }

        // ---------- ฟอร์มจอง ----------

        private Card buildBookingForm() {
            // เปลี่ยนประเภทรถหรือบริการ -> คำนวณราคาใหม่
            carTypeBox.addActionListener(e -> updatePrice());
            serviceBox.addActionListener(e -> updatePrice());

            JButton bookButton = new RoundButton("ยืนยันการจอง", BLUE);
            bookButton.addActionListener(e -> submitBooking());

            VerticalStack fields = new VerticalStack(300);
            fields.put(createLabel("จองคิวล้างรถ", 20f, Font.BOLD, TEXT));
            fields.put(withCaption("ทะเบียนรถ", plateField));
            fields.put(withCaption("ประเภทรถ", carTypeBox));
            fields.put(withCaption("บริการ", serviceBox));
            fields.put(withCaption("วันที่ (yyyy-MM-dd)", createDateField(dateField)));
            fields.put(withCaption("เวลา", slotBox));
            fields.put(buildPriceBox());
            fields.put(bookButton);

            Card card = new Card(new BorderLayout());
            card.add(fields, BorderLayout.NORTH);
            return card;
        }

        private JPanel buildPriceBox() {
            JPanel box = new JPanel(new BorderLayout());
            box.setBackground(new Color(0xEFF6FF));
            box.setBorder(new CompoundBorder(
                    new LineBorder(new Color(0xBFDBFE), 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            box.add(createLabel("ค่าบริการ", 14f, Font.PLAIN, MUTED), BorderLayout.WEST);
            box.add(priceLabel, BorderLayout.EAST);
            return box;
        }

        private void updatePrice() {
            String service = (String) serviceBox.getSelectedItem();
            int price = qm.calc(service, carTypeBox.getSelectedIndex());
            priceLabel.setText(String.format("%,d บาท", price));
        }

        private void submitBooking() {
            try {
                CUSTOMER booking = qm.book(customerName, customerPhone, plateField.getText(),
                        carTypeBox.getSelectedIndex(), (String) serviceBox.getSelectedItem(),
                        dateField.getText(), (String) slotBox.getSelectedItem());

                refreshBookings();
                showInfo("จองสำเร็จ\nรหัสการจอง " + booking.id + "\n"
                        + booking.date + " เวลา " + booking.time + "  ราคา " + booking.price + " บาท");
            } catch (RuntimeException ex) {
                showWarning(ex.getMessage()); // QueueManager โยนข้อความ error มาให้
            }
        }

        // ---------- คิวของฉัน ----------

        private Card buildMyBookingsCard() {
            styleTable(table);
            table.setAutoCreateRowSorter(true);

            // ลูกค้าไม่ต้องเห็นคอลัมน์ชื่อและเบอร์โทร (ซ่อนจากมุมมอง ข้อมูลยังอยู่ใน model)
            TableColumnModel columns = table.getColumnModel();
            columns.removeColumn(columns.getColumn(COL_PHONE));
            columns.removeColumn(columns.getColumn(COL_NAME));
            fitColumns(table);

            JButton cancelButton = new RoundButton("ยกเลิกการจองที่เลือก", RED);
            cancelButton.addActionListener(e -> cancelSelectedBooking());

            JPanel titleRow = new JPanel(new BorderLayout());
            titleRow.setOpaque(false);
            titleRow.add(createLabel("คิวของฉัน", 16f, Font.BOLD, TEXT), BorderLayout.WEST);
            titleRow.add(bookingCount, BorderLayout.EAST);

            Card card = new Card(new BorderLayout(0, 12));
            card.add(titleRow, BorderLayout.NORTH);
            card.add(createScrollPane(table), BorderLayout.CENTER);
            card.add(rightAligned(cancelButton), BorderLayout.SOUTH);
            return card;
        }

        private void cancelSelectedBooking() {
            Integer id = selectedBookingId(table, tableModel);
            if (id == null) return;
            if (!confirm("ยืนยันยกเลิกการจองรหัส " + id + " ?")) return;

            try {
                qm.cancelByCustomer(id, customerPhone);
                refreshBookings();
            } catch (RuntimeException ex) {
                showWarning(ex.getMessage());
            }
        }

        /** โหลดคิวของลูกค้าคนนี้ใหม่ (ใหม่สุดอยู่บนสุด) แล้ววาดช่วงเวลาว่างใหม่ */
        private void refreshBookings() {
            List<CUSTOMER> myBookings = new ArrayList<>(qm.byPhone(customerPhone));
            Collections.reverse(myBookings);
            loadRows(tableModel, myBookings);
            bookingCount.setText(myBookings.size() + " รายการ");
            updateAvailability();
        }

        // ---------- ช่วงเวลาว่าง ----------

        private Card buildAvailabilityCard() {
            JPanel chipPanel = new JPanel(new GridLayout(1, QueueManager.SLOTS.length, 8, 0));
            chipPanel.setOpaque(false);

            for (int i = 0; i < slotChips.length; i++) {
                String slot = QueueManager.SLOTS[i];

                JLabel chip = new JLabel("", SwingConstants.CENTER);
                chip.setOpaque(true);
                chip.setPreferredSize(new Dimension(60, 66));
                chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                chip.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        slotBox.setSelectedItem(slot); // คลิกชิป = เลือกเวลานั้นในฟอร์ม
                    }
                });

                slotChips[i] = chip;
                chipPanel.add(chip);
            }

            Card card = new Card(new BorderLayout(0, 12));
            card.add(availabilityTitle, BorderLayout.NORTH);
            card.add(chipPanel, BorderLayout.CENTER);
            card.add(createLabel("คลิกที่ช่วงเวลาเพื่อเลือกในฟอร์มจอง", 12f, Font.PLAIN, MUTED),
                    BorderLayout.SOUTH);
            return card;
        }

        /** นับจำนวนการจองที่ยังไม่ถูกยกเลิก ในวันและช่วงเวลาที่กำหนด */
        private long countBookings(String day, String slot) {
            return qm.all().stream()
                    .filter(b -> b.date.equals(day))
                    .filter(b -> b.time.equals(slot))
                    .filter(b -> !b.status.equals(QueueManager.CANCEL))
                    .count();
        }

        private void updateAvailability() {
            LocalDate date = parseDate(dateField.getText());

            if (date == null) {
                availabilityTitle.setText("ช่วงเวลา (กรอกวันที่ให้ถูกต้อง)");
            } else {
                availabilityTitle.setText("ช่วงเวลาวันที่ " + date
                        + "  (รับพร้อมกันได้ " + QueueManager.BAYS + " คัน/ช่วง)");
            }

            for (int i = 0; i < slotChips.length; i++) {
                paintChip(slotChips[i], QueueManager.SLOTS[i], date);
            }
        }

        /** กำหนดสีและข้อความของชิปหนึ่งอัน: ผ่านแล้ว / เต็ม / ว่าง 1 / ว่างหลายคัน */
        private void paintChip(JLabel chip, String slot, LocalDate date) {
            Color background = new Color(0xF1F5F9);
            Color foreground = new Color(0x94A3B8);
            String caption = "-";

            if (date != null) {
                long free = QueueManager.BAYS - countBookings(date.toString(), slot);
                LocalDateTime slotTime = LocalDateTime.of(date, LocalTime.parse(slot));

                if (slotTime.isBefore(LocalDateTime.now())) {
                    caption = "ผ่านแล้ว";
                } else if (free <= 0) {
                    background = new Color(0xFEE2E2);
                    foreground = new Color(0x991B1B);
                    caption = "เต็ม";
                } else if (free == 1) {
                    background = new Color(0xFEF3C7);
                    foreground = new Color(0x92400E);
                    caption = "ว่าง 1";
                } else {
                    background = new Color(0xDCFCE7);
                    foreground = new Color(0x166534);
                    caption = "ว่าง " + free;
                }
            }

            // ชิปที่ถูกเลือกอยู่ในฟอร์มจะมีขอบสีน้ำเงินหนา
            boolean picked = slot.equals(slotBox.getSelectedItem());
            Color borderColor = picked ? BLUE : background.darker();

            chip.setBackground(background);
            chip.setForeground(foreground);
            chip.setText("<html><center><b>" + slot + "</b><br>" + caption + "</center></html>");
            chip.setBorder(new LineBorder(borderColor, picked ? 3 : 1, true));
        }
    }
}
