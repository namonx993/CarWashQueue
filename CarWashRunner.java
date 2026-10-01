import javax.swing.*;
import java.awt.Font;
import java.util.Collections;

/**
 * ตัวรันระบบ: จุดเริ่มต้นโปรแกรม (main) — ตั้งฟอนต์ สร้าง QueueManager แล้วเปิดหน้าต่าง CarWashUI
 * คอมไพล์: javac -encoding UTF-8 *.java     รัน: java CarWashRunner
 */
public class CarWashRunner {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            for (Object k : Collections.list(UIManager.getDefaults().keys()))
                if (UIManager.get(k) instanceof Font) UIManager.put(k, new Font("Tahoma", Font.PLAIN, 14));
            try {
                new CarWashUI(new QueueManager("bookings.txt", "prices.txt")).setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null, "เปิดโปรแกรมไม่สำเร็จ: " + e.getMessage());
            }
        });
    }
}
