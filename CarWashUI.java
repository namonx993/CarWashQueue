import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * หน้าต่าง GUI: หน้าเข้าสู่ระบบ ฝั่งลูกค้า (ฟอร์มจอง ประวัติ ยกเลิก) ฝั่งเจ้าของ (รายการจอง รายได้ ตั้งราคา)
 * รับอินพุตจากผู้ใช้แล้วส่งให้ QueueManager ตัดสิน — ไม่มีกติกาธุรกิจ ไม่แตะไฟล์
 */
public class CarWashUI extends JFrame {
    private static final String OWNER_PASS = "aa1234";
    private static final String[] HEAD =
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
        go(loginPanel());
    }

    // ---------- ตัวช่วย ----------
    private void err(String m) { JOptionPane.showMessageDialog(this, m, "แจ้งเตือน", JOptionPane.WARNING_MESSAGE); }
    private void info(String m) { JOptionPane.showMessageDialog(this, m); }

    private void go(JPanel p) {
        root.removeAll();
        root.add(p, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    private static JPanel row(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.add(new JLabel(label), BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private static DefaultTableModel model() {
        return new DefaultTableModel(HEAD, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static void fill(DefaultTableModel m, List<CUSTOMER> list) {
        m.setRowCount(0);
        for (CUSTOMER c : list) m.addRow(c.toRow());
    }

    /** คืนรหัสของแถวที่เลือก หรือ null (พร้อมแจ้งเตือน) ถ้ายังไม่ได้เลือก */
    private Integer selectedId(JTable t, DefaultTableModel m) {
        int r = t.getSelectedRow();
        if (r < 0) { err("กรุณาเลือกรายการก่อน"); return null; }
        return (Integer) m.getValueAt(r, 0);
    }

    // ---------- หน้าเข้าสู่ระบบ ----------
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

    // ---------- ฝั่งลูกค้า ----------
    private JPanel customerPanel(String name, String phone) {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField plate = new JTextField(), date = new JTextField(LocalDate.now().toString());
        JComboBox<String> car = new JComboBox<>(QueueManager.CARS);
        JComboBox<String> svc = new JComboBox<>(qm.names().toArray(new String[0]));
        JComboBox<String> slot = new JComboBox<>(QueueManager.SLOTS);
        JLabel price = new JLabel();
        price.setFont(price.getFont().deriveFont(Font.BOLD, 18f));
        Runnable upd = () -> price.setText(qm.calc((String) svc.getSelectedItem(), car.getSelectedIndex()) + " บาท");
        car.addActionListener(e -> upd.run());
        svc.addActionListener(e -> upd.run());
        upd.run();

        JButton book = new JButton("ยืนยันการจอง"), out = new JButton("ออกจากระบบ");
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
        form.setPreferredSize(new Dimension(320, 0));

        DefaultTableModel m = model();
        JTable t = new JTable(m);
        JButton cancel = new JButton("ยกเลิกการจองที่เลือก");
        Runnable refresh = () -> fill(m, qm.byPhone(phone));
        refresh.run();

        book.addActionListener(e -> {
            try {
                CUSTOMER c = qm.book(name, phone, plate.getText(), car.getSelectedIndex(),
                        (String) svc.getSelectedItem(), date.getText(), (String) slot.getSelectedItem());
                refresh.run();
                info("จองสำเร็จ รหัสการจอง " + c.id);
            } catch (RuntimeException ex) { err(ex.getMessage()); }
        });
        cancel.addActionListener(e -> {
            Integer id = selectedId(t, m);
            if (id == null) return;
            try { qm.cancelByCustomer(id, phone); refresh.run(); }
            catch (RuntimeException ex) { err(ex.getMessage()); }
        });
        out.addActionListener(e -> go(loginPanel()));

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("ประวัติการจองของฉัน"));
        right.add(new JScrollPane(t), BorderLayout.CENTER);
        right.add(cancel, BorderLayout.SOUTH);
        p.add(form, BorderLayout.WEST);
        p.add(right, BorderLayout.CENTER);
        return p;
    }

    // ---------- ฝั่งเจ้าของ ----------
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

    private void changeStatus(JTable t, DefaultTableModel m, String status, Runnable refresh) {
        Integer id = selectedId(t, m);
        if (id == null) return;
        try { qm.setStatus(id, status); refresh.run(); }
        catch (RuntimeException ex) { err(ex.getMessage()); }
    }

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
