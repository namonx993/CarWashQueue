import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
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

/**
 * หน้าต่าง GUI หลักของระบบศูนย์บริการล้างรถ
 */
public class CarWashUI extends JFrame {

    private static final String OWNER_PASSWORD = "1234";
    public static final String ALL = "ทั้งหมด";

    public static final String[] COLUMNS = {
            "รหัส", "ชื่อ", "โทร", "ทะเบียน", "ประเภทรถ", "บริการ", "วันที่", "เวลา", "ราคา", "สถานะ"};
    public static final int COL_ID = 0;
    public static final int COL_NAME = 1;
    public static final int COL_PHONE = 2;
    public static final int COL_STATUS = 9;

    public static final Color NAVY = new Color(0x0F172A);
    public static final Color SLATE = new Color(0x334155);
    public static final Color BLUE = new Color(0x2563EB);
    public static final Color SKY = new Color(0x38BDF8);
    public static final Color BG = new Color(0xF1F5F9);
    public static final Color LINE = new Color(0xE2E8F0);
    public static final Color TEXT = new Color(0x1E293B);
    public static final Color MUTED = new Color(0x64748B);
    public static final Color GREEN = new Color(0x16A34A);
    public static final Color RED = new Color(0xDC2626);
    public static final Color AMBER = new Color(0xD97706);

    private final QueueManager qm;
    private final JPanel root = new JPanel(new BorderLayout());

    public CarWashUI(QueueManager qm) {
        super("ระบบศูนย์บริการล้างรถ");
        this.qm = qm;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);

        root.setBackground(BG);
        setContentPane(root);
        showScreen(buildLoginScreen());
    }

    // ---------- ส่วนประกอบ UI ที่ใช้ร่วมกัน ----------

    public static class RoundButton extends JButton {
        private final Color color;
        private boolean hovered = false;

        public RoundButton(String text, Color color) {
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

    public static class Card extends JPanel {
        public Card(LayoutManager layout) {
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

    public static class VerticalStack extends JPanel {
        private final GridBagConstraints rules = new GridBagConstraints();
        private final int minWidth;

        public VerticalStack(int minWidth) {
            super(new GridBagLayout());
            this.minWidth = minWidth;
            setOpaque(false);
            rules.gridx = 0;
            rules.gridy = 0;
            rules.weightx = 1;
            rules.fill = GridBagConstraints.HORIZONTAL;
            rules.anchor = GridBagConstraints.NORTH;
            rules.insets = new Insets(0, 0, 12, 0);
        }

        public VerticalStack put(JComponent item) {
            add(item, rules);
            rules.gridy++;
            return this;
        }

        @Override public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(size.width, minWidth), size.height);
        }
    }

    private static class BrandBackground extends JPanel {
        BrandBackground() { super(new GridBagLayout()); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, BLUE, getWidth(), getHeight(), NAVY));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 255, 255, 24));
            g2.fillOval(-90, -90, 320, 320);
            g2.fillOval(getWidth() - 170, getHeight() - 210, 340, 340);
            g2.fillOval(getWidth() / 2 + 40, 60, 100, 100);
            g2.fillOval(40, getHeight() - 140, 70, 70);
            g2.dispose();
        }
    }

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

    // ---------- เมธอดช่วยเหลือ ----------

    public void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "แจ้งเตือน", JOptionPane.WARNING_MESSAGE);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public boolean confirm(String message) {
        int answer = JOptionPane.showConfirmDialog(this, message, "ยืนยัน", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    public void showScreen(JPanel screen) {
        root.removeAll();
        root.add(screen, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    public static JLabel createLabel(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(style, size));
        label.setForeground(color);
        return label;
    }

    public static void styleTextField(JTextField field) {
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCBD5E1), 1, true),
                new EmptyBorder(7, 10, 7, 10)));
        field.setForeground(TEXT);
    }

    public static JTextField createTextField(String text) {
        JTextField field = new JTextField(text);
        styleTextField(field);
        return field;
    }

    public static JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> box = new JComboBox<>(items);
        box.setBackground(Color.WHITE);
        box.setForeground(TEXT);
        box.setPreferredSize(new Dimension(100, 36));
        return box;
    }

    public static JPanel withCaption(String caption, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createLabel(caption, 12f, Font.PLAIN, MUTED), BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel createPage(JComponent content) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(20, 20, 20, 20));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    public static JScrollPane createScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(new LineBorder(LINE));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public static JPanel rightAligned(JComponent button) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.add(button);
        return row;
    }

    public static JPanel createHeader(String title, String subtitle, JComponent center, JComponent right) {
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

    public JButton createLogoutButton() {
        JButton button = new RoundButton("ออกจากระบบ", SLATE);
        button.addActionListener(e -> showScreen(buildLoginScreen()));
        return button;
    }

    public static JButton createTabButton(String text) {
        JButton button = new JButton(text);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }

    public static void styleTab(JButton button, boolean active,
                                 Color activeText, Color inactiveText, Color activeBar, Color inactiveBar) {
        button.setForeground(active ? activeText : inactiveText);
        button.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 3, 0, active ? activeBar : inactiveBar),
                new EmptyBorder(8, 14, 6, 14)));
    }

    public static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void shiftDate(JTextField field, int days) {
        LocalDate date = parseDate(field.getText());
        if (date == null) date = LocalDate.now();
        field.setText(date.plusDays(days).toString());
    }

    public static JPanel createDateField(JTextField dateField) {
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

    public static void onTextChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    public static DefaultTableModel createBookingModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                boolean isNumber = (column == COL_ID || column == 8);
                return isNumber ? Integer.class : String.class;
            }
        };
    }

    public static void loadRows(DefaultTableModel model, List<CUSTOMER> bookings) {
        model.setRowCount(0);
        for (CUSTOMER booking : bookings) {
            model.addRow(booking.toRow());
        }
    }

    public static void styleTable(JTable table) {
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

    public static void fitColumns(JTable table) {
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

    public Integer selectedBookingId(JTable table, DefaultTableModel model) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showWarning("กรุณาเลือกรายการก่อน");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return (Integer) model.getValueAt(modelRow, COL_ID);
    }

    // ---------- หน้าเข้าสู่ระบบ ----------

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
        customerTab.doClick();

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
            boolean phoneValid = phone.matches("\\d{9,10}");
            if (name.isEmpty() || !phoneValid) {
                showWarning("กรอกชื่อและเบอร์โทร 9-10 หลักให้ถูกต้อง");
            } else {
                showScreen(new CustomerScreen(this, qm, name, phone).build());
            }
        });

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
}