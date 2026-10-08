import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * หน้าต่าง GUI: หน้าเข้าสู่ระบบ ฝั่งลูกค้า (ฟอร์มจอง ประวัติ ยกเลิก) ฝั่งเจ้าของ (รายการจอง รายได้ ตั้งราคา)
 * รับอินพุตจากผู้ใช้แล้วส่งให้ QueueManager ตัดสิน — ไม่มีกติกาธุรกิจ ไม่แตะไฟล์
 */
public class CarWashUI extends JFrame {

    // ==========================================
    // 1. CONSTANTS & ATTRIBUTES
    // ==========================================
    private static final String OWNER_PASS = "aa1234"; //รหัสผ่านของเจ้าของร้าน
    private static final String[] HEAD = {"รหัส", "ชื่อ", "โทร", "ทะเบียน", "ประเภทรถ", "บริการ", "วันที่", "เวลา", "ราคา", "สถานะ"};

    private final QueueManager qm;
    private final JPanel root = new JPanel(new BorderLayout());

    // ==========================================
    // 2. CONSTRUCTOR
    // ==========================================
    /**
     * คอนสตรัคเตอร์เริ่มต้นการทำงานของหน้าต่างโปรแกรม:
     * - ตั้งชื่อหน้าต่างและขนาด (960x560 px)
     * - กำหนด root panel สำหรับใช้สลับหน้าจอ (Card/Single-frame navigation)
     * - เปิดหน้าแรกเป็น loginPanel()
     */
    public CarWashUI(QueueManager qm) {
        super("ระบบศูนย์บริการล้างรถ");
        this.qm = qm;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 560);
        setLocationRelativeTo(null);
        setContentPane(root);
        go(loginPanel());
    }

    // ==========================================
    // 3. UI HELPER & UTILITY METHODS
    // ==========================================

    /**
     * แสดงหน้าต่างแจ้งเตือนข้อผิดพลาด/ข้อความเตือน (Warning Dialog)
     */
    private void err(String m) { 
        JOptionPane.showMessageDialog(this, m, "แจ้งเตือน", JOptionPane.WARNING_MESSAGE); 
    }

    /**
     * แสดงหน้าต่างข้อความแจ้งข้อมูลทั่วไป (Info Dialog)
     */
    private void info(String m) { 
        JOptionPane.showMessageDialog(this, m); 
    }

    /**
     * เปลี่ยนหน้าจอแสดงผลหลัก โดยการล้างคอมโพเนนต์เก่าใน root panel 
     * แล้วใส่ panel ใหม่เข้าไปแทนที่ พร้อมสั่ง repaint หน้าจอ
     */
    private void go(JPanel p) {
        root.removeAll();
        root.add(p, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    /**
     * สร้างกล่องข้อความพร้อมฟิลด์กรอกข้อมูลแบบแนวนอน (Label ด้านซ้าย + Component ด้านขวา)
     */
    private static JPanel row(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /**
     * สร้าง TableModel มาตรฐานพร้อมคอลัมน์ HEAD และกำหนดให้ตารางอ่านได้อย่างเดียว (isCellEditable = false)
     */
    private static DefaultTableModel model() {
        return new DefaultTableModel(HEAD, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /**
     * ล้างข้อมูลแถวเก่าและนำข้อมูล List<CUSTOMER> มาเติมลงในตาราง (DefaultTableModel)
     */
    private static void fill(DefaultTableModel m, List<CUSTOMER> list) {
        m.setRowCount(0);
        for (CUSTOMER c : list) m.addRow(c.toRow());
    }

    /**
     * คืนค่ารหัสการจอง (ID จากคอลัมน์แรก) ของแถวที่ผู้ใช้คลิกเลือกในตาราง
     * หากยังไม่ได้เลือกแถวใดๆ จะแสดงแจ้งเตือนผ่าน err() และคืนค่าเป็น null
     */
    private Integer selectedId(JTable t, DefaultTableModel m) {
        int r = t.getSelectedRow();
        if (r < 0) { err("กรุณาเลือกรายการก่อน"); return null; }
        return (Integer) m.getValueAt(r, 0);
    }

    // ==========================================
    // 4. AUTHENTICATION & NAVIGATION
    // ==========================================

    /**
     * หน้าแรกสำหรับการเข้าใช้งาน:
     * - ฝั่งลูกค้า: รับชื่อและเบอร์โทร (ตรวจความถูกต้อง 9-10 หลัก) แล้วนำทางไป customerPanel()
     * - ฝั่งเจ้าของ: รับรหัสผ่าน (ตรวจสอบความถูกต้องเทียบกับ OWNER_PASS) แล้วนำทางไป ownerPanel()
     */
    private JPanel loginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        JPanel box = new JPanel(new GridLayout(0, 1, 8, 8));
        JLabel title = new JLabel("ศูนย์บริการล้างรถ", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JTextField name = new JTextField(), phone = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton cus = new JButton("เข้าใช้งานสำหรับลูกค้า"), own = new JButton("เข้าสู่ระบบเจ้าของ");
        box.add(title);
        box.add(new JLabel("— ลูกค้า —", SwingConstants.CENTER));
        box.add(row("ชื่อ", name));
        box.add(row("เบอร์โทร", phone));
        box.add(cus);
        box.add(new JLabel("— เจ้าของร้าน —", SwingConstants.CENTER));
        box.add(row("รหัสผ่าน", pass));
        box.add(own);
        cus.addActionListener(e -> {
            String n = QueueManager.clean(name.getText()), t = phone.getText().trim();
            if (n.isEmpty() || !t.matches("\\d{9,10}")) err("กรอกชื่อและเบอร์โทร 9-10 หลักให้ถูกต้อง");
            else go(customerPanel(n, t));
        });
        own.addActionListener(e -> {
            if (new String(pass.getPassword()).equals(OWNER_PASS)) go(ownerPanel());
            else err("รหัสผ่านไม่ถูกต้อง");
        });
        box.setPreferredSize(new Dimension(340, 360));
        p.add(box);
        return p;
    }

    // ==========================================
    // 5. CUSTOMER PANEL
    // ==========================================

    /**
     * เมธอดสำหรับสร้างและจัดการหน้าจอฝั่งลูกค้า
     * ประกอบด้วย 2 ส่วนหลัก:
     * 1. ฟอร์มจองคิว (ฝั่งซ้าย)
     * 2. ประวัติการจองคิวทั้งหมด (ฝั่งขวา)
     * 
     * @param name  ชื่อของลูกค้าที่ล็อกอินเข้ามา
     * @param phone เบอร์โทรศัพท์ของลูกค้าที่ล็อกอินเข้ามา
     * @return JPanel ที่ประกอบหน้าจอฝั่งลูกค้าเสร็จเรียบร้อย
     */
    private JPanel customerPanel(String name, String phone) {
        // [1] สร้าง Layout หลักของ Panel เป็นแบบ BorderLayout (แบ่งเป็น ทิศเหนือ, ใต้, ออก, ตก, กลาง)
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // เว้นขอบรอบทิศ 10px

        // [2] ประกาศตัวแปรรับข้อมูลในฟอร์มจอง (ทะเบียนรถ, วันที่, ประเภทรถ, บริการ, เวลา)
        JTextField plate = new JTextField(), date = new JTextField(LocalDate.now().toString());
        JComboBox<String> car = new JComboBox<>(QueueManager.CARS);
        JComboBox<String> svc = new JComboBox<>(qm.names().toArray(new String[0]));
        JComboBox<String> slot = new JComboBox<>(QueueManager.SLOTS);
        
        // [3] ส่วนคำนวณและแสดงราคาค่าบริการ
        JLabel price = new JLabel();
        price.setFont(price.getFont().deriveFont(Font.BOLD, 18f));
        
        // [Runnable: upd] ฟังก์ชันคำนวณราคาอัตโนมัติเมื่อมีการเปลี่ยนตัวเลือก
        Runnable upd = () -> price.setText(qm.calc((String) svc.getSelectedItem(), car.getSelectedIndex()) + " บาท");
        car.addActionListener(e -> upd.run()); // ดักจับเหตุการณ์เมื่อเปลี่ยนประเภทรถ
        svc.addActionListener(e -> upd.run()); // ดักจับเหตุการณ์เมื่อเปลี่ยนประเภทบริการ
        upd.run(); // เรียกทำงานครั้งแรกทันทีที่เปิดหน้าจอ

        // [4] สร้างปุ่มกดยืนยัน และ ปุ่มออกจากระบบ
        JButton book = new JButton("ยืนยันการจอง"), out = new JButton("ออกจากระบบ");
        
        // [5] รวมส่วนอินพุตฝั่งซ้ายใส่ใน form Panel
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("จองคิวล้างรถ  (" + name + ")"));
        form.add(row("ทะเบียนรถ", plate));
        form.add(row("ประเภทรถ", car));
        form.add(row("บริการ", svc));
        form.add(row("วันที่ (yyyy-MM-dd)", date));
        form.add(row("เวลา", slot));
        form.add(row("ค่าบริการ", price));
        form.add(book);
        form.add(out);
        form.setPreferredSize(new Dimension(320, 0)); // กำหนดความกว้างของฟอร์มฝั่งซ้ายเป็น 320px

        // [6] สร้างตาราง JTable สำหรับแสดงผลประวัติการจอง
        DefaultTableModel m = model();
        JTable t = new JTable(m);

        // [7] ซ่อนคอลัมน์ที่ไม่ต้องการแสดงผล เพื่อปกป้องข้อมูลส่วนตัว (Privacy)
        // แสดงเฉพาะ: คอลัมน์ที่ 6 (วันที่) และ คอลัมน์ที่ 7 (เวลา)
        // ซ่อน: 0=รหัส, 1=ชื่อ, 2=เบอร์, 3=ทะเบียน, 4=ประเภทรถ, 5=บริการ, 8=ราคา, 9=สถานะ
        int[] columnsToHide = {0, 1, 2, 3, 4, 5, 8, 9}; 
        for (int colIndex : columnsToHide) {
            if (colIndex < t.getColumnCount()) {
                t.getColumnModel().getColumn(colIndex).setMinWidth(0);
                t.getColumnModel().getColumn(colIndex).setMaxWidth(0);
                t.getColumnModel().getColumn(colIndex).setPreferredWidth(0);
            }
        }

        JButton cancel = new JButton("ยกเลิกการจองที่เลือก");

        // [Runnable: refresh] ฟังก์ชันดึงข้อมูลการจองทั้งหมดมาอัปเดตใส่ตาราง
        Runnable refresh = () -> fill(m, qm.all()); 
        refresh.run(); // โหลดข้อมูลทันทีที่เปิดหน้าจอ

        // [Event Listener 1] ปุ่ม "ยืนยันการจอง"
        book.addActionListener(e -> {
            try {
                // บันทึกการจองใหม่ลงระบบ
                CUSTOMER c = qm.book(name, phone, plate.getText(), car.getSelectedIndex(),
                        (String) svc.getSelectedItem(), date.getText(), (String) slot.getSelectedItem());
                refresh.run(); // อัปเดตตารางเพื่อโชว์รายการใหม่
                info("จองสำเร็จ รหัสการจอง " + c.id);
            } catch (RuntimeException ex) { err(ex.getMessage()); }
        });
        
        // [Event Listener 2] ปุ่ม "ยกเลิกการจองที่เลือก"
        cancel.addActionListener(e -> {
            Integer id = selectedId(t, m); // ดึง ID การจองจากแถวที่กดเลือก
            if (id == null) return;
            try { 
                qm.cancelByCustomer(id, phone); // ส่ง ID ไปยกเลิกที่ QueueManager
                refresh.run(); // อัปเดตตารางหลังยกเลิกสำเร็จ
            } catch (RuntimeException ex) { err(ex.getMessage()); }
        });

        // [Event Listener 3] ปุ่ม "ออกจากระบบ"
        out.addActionListener(e -> go(loginPanel())); // สลับหน้าจอไปยังหน้าเข้าสู่ระบบ

        // [8] จัดเลย์เอาต์ฝั่งขวา (ใส่ตาราง + ปุ่มยกเลิก)
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("ประวัติการจองคิวทั้งหมด")); 
        right.add(new JScrollPane(t), BorderLayout.CENTER);
        right.add(cancel, BorderLayout.SOUTH);

        // [9] รวมฝั่งซ้าย (ฟอร์ม) และ ฝั่งขวา (ตาราง) เข้าด้วยกัน แล้วส่งคืนค่า
        p.add(form, BorderLayout.WEST);
        p.add(right, BorderLayout.CENTER);
        return p;
    }

    // ==========================================
    // 6. OWNER PANEL & TABS
    // ==========================================

    /**
     * หน้าจอสำหรับเจ้าของร้าน รวมเมนูผ่าน JTabbedPane:
     * - แท็บรายการจองทั้งหมด (bookingTab)
     * - แท็บสรุปรายได้ (incomeTab)
     * - แท็บจัดการราคา (priceTab)
     * พร้อมปุ่มออกจากระบบเพื่อกลับไป loginPanel()
     */
    private JPanel ownerPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("รายการจองทั้งหมด", bookingTab());
        tabs.add("รายได้", incomeTab());
        tabs.add("ตั้งราคาบริการ", priceTab());
        JButton out = new JButton("ออกจากระบบ");
        out.addActionListener(e -> go(loginPanel()));
        JPanel p = new JPanel(new BorderLayout());
        p.add(tabs, BorderLayout.CENTER);
        p.add(out, BorderLayout.SOUTH);
        return p;
    }

    /**
     * แท็บรายการจองทั้งหมด:
     * - แสดงตารางคิวจองทั้งหมดในระบบ
     * - มีปุ่มจัดการสถานะ: เสร็จสิ้น, ยกเลิก, ลบข้อมูลถาวร, และรีเฟรชข้อมูล
     */
    private JPanel bookingTab() {
        DefaultTableModel m = model();
        JTable t = new JTable(m);
        Runnable refresh = () -> fill(m, qm.all());
        refresh.run();

        JButton done = new JButton("เสร็จสิ้น"), cancel = new JButton("ยกเลิก"),
                del = new JButton("ลบ"), reload = new JButton("รีเฟรช");
        done.addActionListener(e -> changeStatus(t, m, QueueManager.DONE, refresh));
        cancel.addActionListener(e -> changeStatus(t, m, QueueManager.CANCEL, refresh));
        del.addActionListener(e -> {
            Integer id = selectedId(t, m);
            if (id == null) return;
            if (JOptionPane.showConfirmDialog(this, "ลบรายการนี้ถาวร?", "ยืนยัน", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) return;
            try { qm.delete(id); refresh.run(); }
            catch (RuntimeException ex) { err(ex.getMessage()); }
        });
        reload.addActionListener(e -> refresh.run());

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton b : new JButton[]{done, cancel, del, reload}) btn.add(b);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(btn, BorderLayout.SOUTH);
        return p;
    }

    /**
     * เมธอดเสริมสำหรับอัปเดตสถานะของแถวที่เลือกใน bookingTab แล้วสั่งรีเฟรชตาราง
     */
    private void changeStatus(JTable t, DefaultTableModel m, String status, Runnable refresh) {
        Integer id = selectedId(t, m);
        if (id == null) return;
        try { qm.setStatus(id, status); refresh.run(); }
        catch (RuntimeException ex) { err(ex.getMessage()); }
    }

    /**
     * แท็บคำนวณและสรุปรายได้:
     * - เลือกวันที่เพื่อดูจำนวนคันที่ทำเสร็จและรายได้ของวันนั้น
     * - แสดงผลสรุปรายได้สะสมทั้งหมดในระบบ
     */
    private JPanel incomeTab() {
        JTextField day = new JTextField(LocalDate.now().toString(), 10);
        JLabel res = new JLabel(" ");
        res.setFont(res.getFont().deriveFont(Font.BOLD, 18f));
        JButton sum = new JButton("สรุปรายได้");
        sum.addActionListener(e -> {
            String d = day.getText().trim();
            res.setText("<html>วันที่ " + d + ": " + qm.countDoneOn(d) + " คัน รายได้ " + qm.incomeOn(d)
                    + " บาท<br>รายได้รวมทั้งหมด " + qm.incomeTotal() + " บาท</html>");
        });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.add(new JLabel("วันที่ (yyyy-MM-dd)"));
        p.add(day);
        p.add(sum);
        p.add(res);
        return p;
    }

    /**
     * แท็บจัดการราคาค่าบริการ:
     * - แสดงตารางราคาพื้นฐานของแต่ละแพ็กเกจ (แก้ไขได้เฉพาะคอลัมน์ราคา)
     * - มีปุ่มบันทึกการแก้ไขเพื่อส่งไปอัปเดตใน QueueManager
     */
    private JPanel priceTab() {
        DefaultTableModel pm = new DefaultTableModel(new String[]{"บริการ", "ราคาพื้นฐาน (รถเก๋ง)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };
        for (String s : qm.names()) pm.addRow(new Object[]{s, qm.basePrice(s)});
        JButton save = new JButton("บันทึกราคา");
        save.addActionListener(e -> {
            try {
                for (int i = 0; i < pm.getRowCount(); i++)
                    qm.setPrice((String) pm.getValueAt(i, 0), Integer.parseInt(pm.getValueAt(i, 1).toString().trim()));
                qm.savePrices();
                info("บันทึกราคาแล้ว (ลูกค้าจะเห็นราคาใหม่เมื่อเข้าสู่ระบบครั้งถัดไป)");
            } catch (IllegalArgumentException ex) {
                err("ราคาต้องเป็นตัวเลขจำนวนเต็มไม่ติดลบ");
            } catch (IllegalStateException ex) { err(ex.getMessage()); }
        });
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(new JTable(pm)), BorderLayout.CENTER);
        p.add(save, BorderLayout.SOUTH);
        return p;
    }
}