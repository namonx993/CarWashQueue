import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * หน้าต่าง GUI: หน้าเข้าสู่ระบบ ฝั่งลูกค้า (ฟอร์มจอง ประวัติ ยกเลิก) ฝั่งเจ้าของ (รายการจอง รายได้ ตั้งราคา)
 * รับอินพุตจากผู้ใช้แล้วส่งให้ QueueManager ตัดสิน — ไม่มีกติกาธุรกิจ ไม่แตะไฟล์
 */
public class CarWashUI extends JFrame {
    private static final String OWNER_PASS = "aa1234";
    private static final String PHONE_PATTERN = "\\d{9,10}";
    private static final int BOOKING_YEARS_AHEAD = 1; // ให้เลือกปีปัจจุบัน + อีกกี่ปีข้างหน้า
    private static final String[] BOOKING_HEADERS =
        {"รหัส", "ชื่อ", "โทร", "ทะเบียน", "ประเภทรถ", "บริการ", "วันที่", "เวลา", "ราคา", "สถานะ"};

    private final QueueManager qm;
    private final JPanel root = new JPanel(new BorderLayout());

    public CarWashUI(QueueManager qm) {
        super("ระบบศูนย์บริการล้างรถ");
        this.qm = qm;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 560);
        setLocationRelativeTo(null);
        setContentPane(root);
        showPanel(loginPanel());
    }

    // ================= ตัวช่วยทั่วไป =================

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "แจ้งเตือน", JOptionPane.WARNING_MESSAGE);
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showPanel(JPanel panel) {
        root.removeAll();
        root.add(panel, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    /** รันคำสั่งที่อาจ throw จาก QueueManager แล้วแจ้งเตือนข้อความผิดพลาดให้อัตโนมัติ */
    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            warn(ex.getMessage());
        }
    }

    private static JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JLabel boldLabel(String text, float size) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
        return label;
    }

    private static DefaultTableModel newBookingModel() {
        return new DefaultTableModel(BOOKING_HEADERS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private static void fillTable(DefaultTableModel model, List<CUSTOMER> list) {
        model.setRowCount(0);
        for (CUSTOMER c : list) model.addRow(c.toRow());
    }

    /** คืนรหัสของแถวที่เลือก หรือ null (พร้อมแจ้งเตือน) ถ้ายังไม่ได้เลือก */
    private Integer selectedId(JTable table, DefaultTableModel model) {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("กรุณาเลือกรายการก่อน");
            return null;
        }
        return (Integer) model.getValueAt(row, 0);
    }

    /** ตัวเลือกวันที่แบบแยก 3 ช่อง (วัน / เดือน / ปี) ค่าเริ่มต้นคือวันนี้ */
    private static class DateSelector extends JPanel {
        private static final String[] MONTHS =
            {"ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."};

        private final JComboBox<Integer> day = new JComboBox<>();
        private final JComboBox<String> month = new JComboBox<>(MONTHS);
        private final JComboBox<Integer> year = new JComboBox<>();

        DateSelector() {
            super(new GridLayout(1, 3, 4, 0));
            LocalDate today = LocalDate.now();
            for (int y = today.getYear(); y <= today.getYear() + BOOKING_YEARS_AHEAD; y++) year.addItem(y);
            month.setSelectedIndex(today.getMonthValue() - 1);
            refreshDays();
            day.setSelectedItem(today.getDayOfMonth());

            month.addActionListener(e -> refreshDays());
            year.addActionListener(e -> refreshDays());
            add(day);
            add(month);
            add(year);
        }

        /** ปรับจำนวนวันตามเดือน/ปีที่เลือก (เช่น ก.พ. มี 28-29 วัน) และคงวันเดิมไว้ถ้ายังใช้ได้ */
        private void refreshDays() {
            Integer previous = (Integer) day.getSelectedItem();
            int length = YearMonth.of((Integer) year.getSelectedItem(), month.getSelectedIndex() + 1).lengthOfMonth();
            day.removeAllItems();
            for (int d = 1; d <= length; d++) day.addItem(d);
            if (previous != null) day.setSelectedItem(Math.min(previous, length));
        }

        /** คืนวันที่รูปแบบ yyyy-MM-dd */
        String getDate() {
            return LocalDate.of((Integer) year.getSelectedItem(),
                                month.getSelectedIndex() + 1,
                                (Integer) day.getSelectedItem()).toString();
        }
    }

    // ================= หน้าเข้าสู่ระบบ =================

    private JPanel loginPanel() {
        JTextField name = new JTextField();
        JTextField phone = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton customerBtn = new JButton("เข้าใช้งานสำหรับลูกค้า");
        JButton ownerBtn = new JButton("เข้าสู่ระบบเจ้าของ");

        JLabel title = new JLabel("ศูนย์บริการล้างรถ", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        JPanel box = new JPanel(new GridLayout(0, 1, 8, 8));
        box.add(title);
        box.add(new JLabel("— ลูกค้า —", SwingConstants.CENTER));
        box.add(labeled("ชื่อ", name));
        box.add(labeled("เบอร์โทร", phone));
        box.add(customerBtn);
        box.add(new JLabel("— เจ้าของร้าน —", SwingConstants.CENTER));
        box.add(labeled("รหัสผ่าน", pass));
        box.add(ownerBtn);
        box.setPreferredSize(new Dimension(340, 360));

        customerBtn.addActionListener(e -> {
            String n = QueueManager.clean(name.getText());
            String t = phone.getText().trim();
            if (n.isEmpty() || !t.matches(PHONE_PATTERN)) warn("กรอกชื่อและเบอร์โทร 9-10 หลักให้ถูกต้อง");
            else showPanel(customerPanel(n, t));
        });
        ownerBtn.addActionListener(e -> {
            if (new String(pass.getPassword()).equals(OWNER_PASS)) showPanel(ownerPanel());
            else warn("รหัสผ่านไม่ถูกต้อง");
        });

        JPanel p = new JPanel(new GridBagLayout());
        p.add(box);
        return p;
    }

    // ================= ฝั่งลูกค้า =================

    private JPanel customerPanel(String name, String phone) {
        DefaultTableModel model = newBookingModel();
        Runnable refresh = () -> fillTable(model, qm.byPhone(phone));
        refresh.run();

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(bookingForm(name, phone, refresh), BorderLayout.WEST);
        p.add(historyPanel(phone, model, refresh), BorderLayout.CENTER);
        return p;
    }

    private JPanel bookingForm(String name, String phone, Runnable refresh) {
        JTextField plate = new JTextField();
        JComboBox<String> car = new JComboBox<>(QueueManager.CARS);
        JComboBox<String> service = new JComboBox<>(qm.names().toArray(new String[0]));
        DateSelector date = new DateSelector();
        JComboBox<String> slot = new JComboBox<>(QueueManager.SLOTS);
        JLabel price = boldLabel("", 18f);
        JButton bookBtn = new JButton("ยืนยันการจอง");
        JButton logoutBtn = new JButton("ออกจากระบบ");

        Runnable updatePrice = () ->
            price.setText(qm.calc((String) service.getSelectedItem(), car.getSelectedIndex()) + " บาท");
        car.addActionListener(e -> updatePrice.run());
        service.addActionListener(e -> updatePrice.run());
        updatePrice.run();

        bookBtn.addActionListener(e -> attempt(() -> {
            CUSTOMER c = qm.book(name, phone, plate.getText(), car.getSelectedIndex(),
                    (String) service.getSelectedItem(), date.getDate(),
                    (String) slot.getSelectedItem());
            refresh.run();
            info("จองสำเร็จ รหัสการจอง " + c.id);
        }));
        logoutBtn.addActionListener(e -> showPanel(loginPanel()));

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("จองคิวล้างรถ  (" + name + ")"));
        form.add(labeled("ทะเบียนรถ", plate));
        form.add(labeled("ประเภทรถ", car));
        form.add(labeled("บริการ", service));
        form.add(labeled("วันที่", date));
        form.add(labeled("เวลา", slot));
        form.add(labeled("ค่าบริการ", price));
        form.add(bookBtn);
        form.add(logoutBtn);
        form.setPreferredSize(new Dimension(320, 0));
        return form;
    }

    private JPanel historyPanel(String phone, DefaultTableModel model, Runnable refresh) {
        JTable table = new JTable(model);
        JButton cancelBtn = new JButton("ยกเลิกการจองที่เลือก");
        cancelBtn.addActionListener(e -> {
            Integer id = selectedId(table, model);
            if (id != null) attempt(() -> {
                qm.cancelByCustomer(id, phone);
                refresh.run();
            });
        });

        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createTitledBorder("ประวัติการจองของฉัน"));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(cancelBtn, BorderLayout.SOUTH);
        return p;
    }

    // ================= ฝั่งเจ้าของ =================

    private JPanel ownerPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("รายการจองทั้งหมด", bookingTab());
        tabs.add("รายได้", incomeTab());
        tabs.add("ตั้งราคาบริการ", priceTab());

        JButton logoutBtn = new JButton("ออกจากระบบ");
        logoutBtn.addActionListener(e -> showPanel(loginPanel()));

        JPanel p = new JPanel(new BorderLayout());
        p.add(tabs, BorderLayout.CENTER);
        p.add(logoutBtn, BorderLayout.SOUTH);
        return p;
    }

    private JPanel bookingTab() {
        DefaultTableModel model = newBookingModel();
        JTable table = new JTable(model);
        Runnable refresh = () -> fillTable(model, qm.all());
        refresh.run();

        JButton doneBtn = new JButton("เสร็จสิ้น");
        JButton cancelBtn = new JButton("ยกเลิก");
        JButton deleteBtn = new JButton("ลบ");
        JButton reloadBtn = new JButton("รีเฟรช");

        doneBtn.addActionListener(e -> changeStatus(table, model, QueueManager.DONE, refresh));
        cancelBtn.addActionListener(e -> changeStatus(table, model, QueueManager.CANCEL, refresh));
        deleteBtn.addActionListener(e -> deleteSelected(table, model, refresh));
        reloadBtn.addActionListener(e -> refresh.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton b : new JButton[]{doneBtn, cancelBtn, deleteBtn, reloadBtn}) buttons.add(b);

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private void changeStatus(JTable table, DefaultTableModel model, String status, Runnable refresh) {
        Integer id = selectedId(table, model);
        if (id != null) attempt(() -> {
            qm.setStatus(id, status);
            refresh.run();
        });
    }

    private void deleteSelected(JTable table, DefaultTableModel model, Runnable refresh) {
        Integer id = selectedId(table, model);
        if (id == null) return;
        int answer = JOptionPane.showConfirmDialog(this, "ลบรายการนี้ถาวร?", "ยืนยัน", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        attempt(() -> {
            qm.delete(id);
            refresh.run();
        });
    }

    private JPanel incomeTab() {
        JTextField day = new JTextField(LocalDate.now().toString(), 10);
        JLabel result = boldLabel(" ", 18f);
        JButton sumBtn = new JButton("สรุปรายได้");

        sumBtn.addActionListener(e -> {
            String d = day.getText().trim();
            result.setText("<html>วันที่ " + d + ": " + qm.countDoneOn(d) + " คัน รายได้ " + qm.incomeOn(d)
                    + " บาท<br>รายได้รวมทั้งหมด " + qm.incomeTotal() + " บาท</html>");
        });

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.add(new JLabel("วันที่ (yyyy-MM-dd)"));
        p.add(day);
        p.add(sumBtn);
        p.add(result);
        return p;
    }

    private JPanel priceTab() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"บริการ", "ราคาพื้นฐาน (รถเก๋ง)"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 1; }
        };
        for (String s : qm.names()) model.addRow(new Object[]{s, qm.basePrice(s)});

        JButton saveBtn = new JButton("บันทึกราคา");
        saveBtn.addActionListener(e -> savePrices(model));

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        p.add(saveBtn, BorderLayout.SOUTH);
        return p;
    }

    private void savePrices(DefaultTableModel model) {
        try {
            for (int i = 0; i < model.getRowCount(); i++) {
                String service = (String) model.getValueAt(i, 0);
                int price = Integer.parseInt(model.getValueAt(i, 1).toString().trim());
                qm.setPrice(service, price);
            }
            qm.savePrices();
            info("บันทึกราคาแล้ว (ลูกค้าจะเห็นราคาใหม่เมื่อเข้าสู่ระบบครั้งถัดไป)");
        } catch (IllegalArgumentException ex) {
            warn("ราคาต้องเป็นตัวเลขจำนวนเต็มไม่ติดลบ");
        } catch (IllegalStateException ex) {
            warn(ex.getMessage());
        }
    }
}