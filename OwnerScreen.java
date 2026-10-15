import java.awt.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * หน้าเจ้าของร้าน (แยกออกมาจาก CarWashUI)
 *
 * ประกอบด้วย 3 แท็บ แต่ละแท็บเป็นคลาสย่อยของตัวเอง:
 *   BookingTab  = รายการจอง (ค้นหา / กรอง / เปลี่ยนสถานะ / ลบ)
 *   IncomeTab   = สรุปรายได้
 *   PriceTab    = ตั้งราคาบริการ
 *
 * คลาสนี้ทำหน้าที่แค่แสดงผล กติกาธุรกิจทั้งหมดอยู่ใน QueueManager
 * ตัวช่วยสร้างหน้าจอ (ปุ่ม การ์ด ตาราง) ใช้ร่วมกับ CarWashUI
 */
public class OwnerScreen {

    // ---------- ค่าคงที่ ----------

    private static final Color NAVY = CarWashUI.NAVY;
    private static final Color BLUE = CarWashUI.BLUE;
    private static final Color SKY = CarWashUI.SKY;
    private static final Color BG = CarWashUI.BG;
    private static final Color TEXT = CarWashUI.TEXT;
    private static final Color MUTED = CarWashUI.MUTED;
    private static final Color GREEN = CarWashUI.GREEN;
    private static final Color RED = CarWashUI.RED;
    private static final Color AMBER = CarWashUI.AMBER;

    private static final Color TAB_INACTIVE_TEXT = new Color(0x94A3B8);

    private static final String ALL = CarWashUI.ALL;

    // คอลัมน์ที่ใช้ค้นหา: ตั้งแต่ "ชื่อ" ถึง "บริการ" (ดู CarWashUI.COLUMNS)
    private static final int FIRST_SEARCH_COLUMN = CarWashUI.COL_NAME;
    private static final int LAST_SEARCH_COLUMN = 5;
    private static final int STATUS_COLUMN = CarWashUI.COL_STATUS;

    private final CarWashUI ui; // ใช้แสดงกล่องข้อความและปุ่มออกจากระบบ
    private final QueueManager qm;

    public OwnerScreen(CarWashUI ui, QueueManager qm) {
        this.ui = ui;
        this.qm = qm;
    }

    // ==========================================
    // โครงหน้า: แถบหัว + แท็บ
    // ==========================================

    /** แท็บหนึ่งแท็บ: ชื่อบนเมนู, เนื้อหา, และโค้ดโหลดข้อมูลใหม่ตอนเปิดแท็บ */
    private static final class Tab {
        final String title;
        final JPanel content;
        final Runnable onShow;

        Tab(String title, JPanel content, Runnable onShow) {
            this.title = title;
            this.content = content;
            this.onShow = onShow;
        }
    }

    /** ประกอบทั้งหน้า: แถบหัว (มีเมนูแท็บ) + เนื้อหาที่สลับตามแท็บ */
    public JPanel build() {
        List<Tab> tabs = Arrays.asList(
                new BookingTab().create(),
                new IncomeTab().create(),
                new PriceTab().create());

        CardLayout cards = new CardLayout();
        JPanel body = new JPanel(cards);
        body.setBackground(BG);
        for (Tab tab : tabs) {
            body.add(tab.content, tab.title);
        }

        JPanel menu = buildTabMenu(tabs, cards, body);

        JPanel screen = new JPanel(new BorderLayout());
        screen.add(CarWashUI.createHeader("แผงควบคุมเจ้าของร้าน", "ศูนย์บริการล้างรถ",
                menu, ui.createLogoutButton()), BorderLayout.NORTH);
        screen.add(body, BorderLayout.CENTER);
        return screen;
    }

    private JPanel buildTabMenu(List<Tab> tabs, CardLayout cards, JPanel body) {
        JButton[] buttons = new JButton[tabs.size()];
        JPanel menu = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        menu.setOpaque(false);

        for (int i = 0; i < buttons.length; i++) {
            Tab tab = tabs.get(i);
            buttons[i] = CarWashUI.createTabButton(tab.title);
            buttons[i].addActionListener(e -> {
                cards.show(body, tab.title);
                highlightTab(buttons, tab.title);
                tab.onShow.run();
            });
            menu.add(buttons[i]);
        }
        highlightTab(buttons, tabs.get(0).title); // เริ่มที่แท็บแรก
        return menu;
    }

    /** ทำให้แท็บที่เลือกดูเด่น แท็บอื่นดูจาง */
    private static void highlightTab(JButton[] buttons, String selectedTitle) {
        for (JButton button : buttons) {
            boolean active = button.getText().equals(selectedTitle);
            CarWashUI.styleTab(button, active, Color.WHITE, TAB_INACTIVE_TEXT, SKY, NAVY);
        }
    }

    // ==========================================
    // แท็บ 1: รายการจอง
    // ==========================================

    /** ตารางทุกการจอง + ค้นหา + กรองสถานะ + ปุ่มจัดการ */
    private final class BookingTab {
        private final DefaultTableModel model = CarWashUI.createBookingModel();
        private final JTable table = new JTable(model);
        private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        private final JTextField searchField = CarWashUI.createTextField("");
        private final JComboBox<String> statusBox = CarWashUI.createComboBox(
                new String[]{ALL, QueueManager.WAIT, QueueManager.DONE, QueueManager.CANCEL});
        private final JLabel countLabel = CarWashUI.createLabel(" ", 13f, Font.PLAIN, MUTED);

        Tab create() {
            CarWashUI.styleTable(table);
            CarWashUI.fitColumns(table);
            table.setRowSorter(sorter);

            searchField.setColumns(18);
            statusBox.setPreferredSize(new Dimension(150, 36));
            CarWashUI.onTextChange(searchField, this::applyFilter);
            statusBox.addActionListener(e -> applyFilter());

            CarWashUI.Card card = new CarWashUI.Card(new BorderLayout(0, 12));
            card.add(buildFilterBar(), BorderLayout.NORTH);
            card.add(CarWashUI.createScrollPane(table), BorderLayout.CENTER);
            card.add(buildActionButtons(), BorderLayout.SOUTH);

            reload();
            return new Tab("รายการจอง", CarWashUI.createPage(card), this::reload);
        }

        private JPanel buildFilterBar() {
            JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            filters.setOpaque(false);
            filters.add(CarWashUI.createLabel("ค้นหา", 13f, Font.PLAIN, MUTED));
            filters.add(searchField);
            filters.add(CarWashUI.createLabel("สถานะ", 13f, Font.PLAIN, MUTED));
            filters.add(statusBox);

            JPanel bar = new JPanel(new BorderLayout());
            bar.setOpaque(false);
            bar.add(filters, BorderLayout.WEST);
            bar.add(countLabel, BorderLayout.EAST);
            return bar;
        }

        private JPanel buildActionButtons() {
            JButton reloadButton = new CarWashUI.RoundButton("รีเฟรช", MUTED);
            JButton doneButton = new CarWashUI.RoundButton("เสร็จสิ้น", GREEN);
            JButton cancelButton = new CarWashUI.RoundButton("ยกเลิก", AMBER);
            JButton deleteButton = new CarWashUI.RoundButton("ลบ", RED);

            reloadButton.addActionListener(e -> reload());
            doneButton.addActionListener(e -> changeSelectedStatus(QueueManager.DONE));
            cancelButton.addActionListener(e -> changeSelectedStatus(QueueManager.CANCEL));
            deleteButton.addActionListener(e -> deleteSelected());

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttons.setOpaque(false);
            buttons.add(reloadButton);
            buttons.add(doneButton);
            buttons.add(cancelButton);
            buttons.add(deleteButton);
            return buttons;
        }

        /** โหลดข้อมูลจาก QueueManager ใหม่ แล้วใช้ตัวกรองซ้ำ */
        private void reload() {
            CarWashUI.loadRows(model, qm.all());
            applyFilter();
        }

        private void applyFilter() {
            String status = (String) statusBox.getSelectedItem();
            String keyword = searchField.getText().trim().toLowerCase();
            sorter.setRowFilter(createFilter(status, keyword));
            countLabel.setText("แสดง " + table.getRowCount() + " จาก " + model.getRowCount() + " รายการ");
        }

        private void changeSelectedStatus(String newStatus) {
            Integer id = ui.selectedBookingId(table, model);
            if (id == null) return;

            try {
                qm.setStatus(id, newStatus);
                reload();
            } catch (RuntimeException ex) {
                ui.showWarning(ex.getMessage());
            }
        }

        private void deleteSelected() {
            Integer id = ui.selectedBookingId(table, model);
            if (id == null) return;
            if (!ui.confirm("ลบรายการนี้ถาวร?")) return;

            try {
                qm.delete(id);
                reload();
            } catch (RuntimeException ex) {
                ui.showWarning(ex.getMessage());
            }
        }
    }

    /** ตัวกรองแถว: สถานะต้องตรง และคำค้นต้องอยู่ในคอลัมน์ชื่อ/โทร/ทะเบียน/ประเภทรถ/บริการ */
    private static RowFilter<DefaultTableModel, Integer> createFilter(String status, String keyword) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> row) {
                boolean statusMatches = ALL.equals(status) || row.getStringValue(STATUS_COLUMN).equals(status);
                return statusMatches && (keyword.isEmpty() || containsKeyword(row));
            }

            private boolean containsKeyword(Entry<? extends DefaultTableModel, ? extends Integer> row) {
                for (int col = FIRST_SEARCH_COLUMN; col <= LAST_SEARCH_COLUMN; col++) {
                    if (row.getStringValue(col).toLowerCase().contains(keyword)) return true;
                }
                return false;
            }
        };
    }

    // ==========================================
    // แท็บ 2: สรุปรายได้
    // ==========================================

    /** เลือกวันที่ แล้วแสดงการ์ดสถิติ 4 ใบ */
    private final class IncomeTab {
        private final JTextField dayField = CarWashUI.createTextField(LocalDate.now().toString());
        private final JLabel doneCountLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, BLUE);
        private final JLabel dayIncomeLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, GREEN);
        private final JLabel totalIncomeLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, NAVY);
        private final JLabel waitingLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, AMBER);

        Tab create() {
            dayField.setColumns(10);
            CarWashUI.onTextChange(dayField, this::refresh);

            JPanel content = new JPanel(new BorderLayout(0, 16));
            content.setOpaque(false);
            content.add(buildDatePicker(), BorderLayout.NORTH);
            content.add(buildStatsRow(), BorderLayout.CENTER);

            refresh();
            return new Tab("สรุปรายได้", CarWashUI.createPage(content), this::refresh);
        }

        private CarWashUI.Card buildDatePicker() {
            JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            picker.setOpaque(false);
            picker.add(CarWashUI.createLabel("เลือกวันที่ (yyyy-MM-dd)", 14f, Font.PLAIN, MUTED));
            picker.add(CarWashUI.createDateField(dayField));

            CarWashUI.Card card = new CarWashUI.Card(new BorderLayout());
            card.add(picker, BorderLayout.CENTER);
            return card;
        }

        private JPanel buildStatsRow() {
            JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
            stats.setOpaque(false);
            stats.add(createStatCard("คันที่ล้างเสร็จ (วันที่เลือก)", doneCountLabel));
            stats.add(createStatCard("รายได้ (วันที่เลือก)", dayIncomeLabel));
            stats.add(createStatCard("รายได้รวมทั้งหมด", totalIncomeLabel));
            stats.add(createStatCard("คิวรอดำเนินการทั้งหมด", waitingLabel));

            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(stats, BorderLayout.NORTH);
            return holder;
        }

        private CarWashUI.Card createStatCard(String title, JLabel valueLabel) {
            CarWashUI.Card card = new CarWashUI.Card(new BorderLayout(0, 8));
            card.add(CarWashUI.createLabel(title, 13f, Font.PLAIN, MUTED), BorderLayout.NORTH);
            card.add(valueLabel, BorderLayout.CENTER);
            return card;
        }

        private void refresh() {
            String day = dayField.getText().trim();

            doneCountLabel.setText(qm.countDoneOn(day) + " คัน");
            dayIncomeLabel.setText(String.format("%,d บาท", qm.incomeOn(day)));
            totalIncomeLabel.setText(String.format("%,d บาท", qm.incomeTotal()));
            waitingLabel.setText(countWaiting() + " คิว");
        }

        private long countWaiting() {
            return qm.all().stream()
                    .filter(booking -> booking.status.equals(QueueManager.WAIT))
                    .count();
        }
    }

    // ==========================================
    // แท็บ 3: ตั้งราคาบริการ
    // ==========================================

    /** ตารางแก้ราคาพื้นฐานของแต่ละบริการ แล้วกดบันทึก */
    private final class PriceTab {
        private static final int SERVICE_COLUMN = 0;
        private static final int PRICE_COLUMN = 1;

        private final DefaultTableModel model = createPriceModel();
        private final JTable table = new JTable(model);

        Tab create() {
            CarWashUI.styleTable(table);
            fillRows();

            CarWashUI.Card card = new CarWashUI.Card(new BorderLayout(0, 4));
            card.add(buildHeading(), BorderLayout.NORTH);
            card.add(CarWashUI.createScrollPane(table), BorderLayout.CENTER);
            card.add(buildFooter(), BorderLayout.SOUTH);
            return new Tab("ตั้งราคาบริการ", CarWashUI.createPage(card), () -> { });
        }

        private DefaultTableModel createPriceModel() {
            return new DefaultTableModel(new String[]{"บริการ", "ราคาพื้นฐาน (รถเก๋ง)"}, 0) {
                @Override public boolean isCellEditable(int row, int column) {
                    return column == PRICE_COLUMN; // แก้ได้เฉพาะช่องราคา
                }
            };
        }

        private void fillRows() {
            for (String service : qm.names()) {
                model.addRow(new Object[]{service, qm.basePrice(service)});
            }
        }

        private JPanel buildHeading() {
            CarWashUI.VerticalStack head = new CarWashUI.VerticalStack(0);
            head.put(CarWashUI.createLabel("ตั้งราคาบริการ", 18f, Font.BOLD, TEXT));
            head.put(CarWashUI.createLabel("ดับเบิลคลิกที่ช่องราคาเพื่อแก้ไข แล้วกดบันทึก", 13f, Font.PLAIN, MUTED));
            return head;
        }

        private JPanel buildFooter() {
            JButton saveButton = new CarWashUI.RoundButton("บันทึกราคา", BLUE);
            saveButton.addActionListener(e -> savePrices());

            JPanel footer = new JPanel(new BorderLayout(0, 12));
            footer.setOpaque(false);
            footer.add(CarWashUI.createLabel(describeMultipliers(), 12f, Font.PLAIN, MUTED), BorderLayout.NORTH);
            footer.add(CarWashUI.rightAligned(saveButton), BorderLayout.SOUTH);
            return footer;
        }

        private void savePrices() {
            // ถ้ายังพิมพ์ค้างอยู่ในช่อง ให้ยืนยันค่านั้นก่อน
            if (table.isEditing()) table.getCellEditor().stopCellEditing();

            try {
                for (int row = 0; row < model.getRowCount(); row++) {
                    qm.setPrice(serviceAt(row), priceAt(row));
                }
                qm.savePrices();
                ui.showInfo("บันทึกราคาแล้ว (ลูกค้าจะเห็นราคาใหม่เมื่อเข้าสู่ระบบครั้งถัดไป)");
            } catch (IllegalArgumentException ex) {
                ui.showWarning("ราคาต้องเป็นตัวเลขจำนวนเต็มไม่ติดลบ"); // พิมพ์ไม่ใช่ตัวเลข
            } catch (IllegalStateException ex) {
                ui.showWarning(ex.getMessage());
            }
        }

        private String serviceAt(int row) {
            return (String) model.getValueAt(row, SERVICE_COLUMN);
        }

        /** ถ้าไม่ใช่ตัวเลข parseInt จะโยน NumberFormatException (เป็น IllegalArgumentException) */
        private int priceAt(int row) {
            return Integer.parseInt(model.getValueAt(row, PRICE_COLUMN).toString().trim());
        }
    }

    /** ข้อความสรุปตัวคูณราคาตามประเภทรถ เช่น "เก๋ง ×1.0 | SUV ×1.3" */
    private static String describeMultipliers() {
        StringBuilder text = new StringBuilder("ตัวคูณตามประเภทรถ:  ");
        for (int i = 0; i < QueueManager.CARS.length; i++) {
            if (i > 0) text.append("   |   ");
            text.append(QueueManager.CARS[i]).append(" ×").append(QueueManager.MULT[i]);
        }
        return text.toString();
    }
}
