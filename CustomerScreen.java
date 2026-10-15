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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * หน้าจอแสดงผลสำหรับลูกค้า
 */
public class CustomerScreen {
    private final CarWashUI ui;
    private final QueueManager qm;
    private final String customerName;
    private final String customerPhone;

    // ช่องในฟอร์มจอง
    private final JTextField plateField = CarWashUI.createTextField("");
    private final JTextField dateField = CarWashUI.createTextField(LocalDate.now().toString());
    private final JComboBox<String> carTypeBox = CarWashUI.createComboBox(QueueManager.CARS);
    private final JComboBox<String> serviceBox;
    private final JComboBox<String> slotBox = CarWashUI.createComboBox(QueueManager.SLOTS);
    private final JLabel priceLabel = CarWashUI.createLabel("", 22f, Font.BOLD, CarWashUI.BLUE);

    // ช่วงเวลาว่าง
    private final JLabel availabilityTitle = CarWashUI.createLabel(" ", 16f, Font.BOLD, CarWashUI.TEXT);
    private final JLabel[] slotChips = new JLabel[QueueManager.SLOTS.length];

    // คิวของฉัน
    private final DefaultTableModel tableModel = CarWashUI.createBookingModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel bookingCount = CarWashUI.createLabel(" ", 13f, Font.PLAIN, CarWashUI.MUTED);

    public CustomerScreen(CarWashUI ui, QueueManager qm, String name, String phone) {
        this.ui = ui;
        this.qm = qm;
        this.customerName = name;
        this.customerPhone = phone;
        this.serviceBox = CarWashUI.createComboBox(qm.names().toArray(new String[0]));
    }

    public JPanel build() {
        CarWashUI.Card bookingForm = buildBookingForm();
        CarWashUI.Card myBookings = buildMyBookingsCard();
        CarWashUI.Card availability = buildAvailabilityCard();

        slotBox.addActionListener(e -> updateAvailability());
        CarWashUI.onTextChange(dateField, this::updateAvailability);

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
        screen.add(CarWashUI.createHeader("สวัสดี, " + customerName, "เบอร์โทร " + customerPhone,
                null, ui.createLogoutButton()), BorderLayout.NORTH);
        screen.add(CarWashUI.createPage(body), BorderLayout.CENTER);
        return screen;
    }

    private CarWashUI.Card buildBookingForm() {
        carTypeBox.addActionListener(e -> updatePrice());
        serviceBox.addActionListener(e -> updatePrice());

        JButton bookButton = new CarWashUI.RoundButton("ยืนยันการจอง", CarWashUI.BLUE);
        bookButton.addActionListener(e -> submitBooking());

        CarWashUI.VerticalStack fields = new CarWashUI.VerticalStack(300);
        fields.put(CarWashUI.createLabel("จองคิวล้างรถ", 20f, Font.BOLD, CarWashUI.TEXT));
        fields.put(CarWashUI.withCaption("ทะเบียนรถ", plateField));
        fields.put(CarWashUI.withCaption("ประเภทรถ", carTypeBox));
        fields.put(CarWashUI.withCaption("บริการ", serviceBox));
        fields.put(CarWashUI.withCaption("วันที่ (yyyy-MM-dd)", CarWashUI.createDateField(dateField)));
        fields.put(CarWashUI.withCaption("เวลา", slotBox));
        fields.put(buildPriceBox());
        fields.put(bookButton);

        CarWashUI.Card card = new CarWashUI.Card(new BorderLayout());
        card.add(fields, BorderLayout.NORTH);
        return card;
    }

    private JPanel buildPriceBox() {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(new Color(0xEFF6FF));
        box.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xBFDBFE), 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        box.add(CarWashUI.createLabel("ค่าบริการ", 14f, Font.PLAIN, CarWashUI.MUTED), BorderLayout.WEST);
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
            ui.showInfo("จองสำเร็จ\nรหัสการจอง " + booking.id + "\n"
                    + booking.date + " เวลา " + booking.time + "  ราคา " + booking.price + " บาท");
        } catch (RuntimeException ex) {
            ui.showWarning(ex.getMessage());
        }
    }

    private CarWashUI.Card buildMyBookingsCard() {
        CarWashUI.styleTable(table);
        table.setAutoCreateRowSorter(true);

        TableColumnModel columns = table.getColumnModel();
        columns.removeColumn(columns.getColumn(CarWashUI.COL_PHONE));
        columns.removeColumn(columns.getColumn(CarWashUI.COL_NAME));
        CarWashUI.fitColumns(table);

        JButton cancelButton = new CarWashUI.RoundButton("ยกเลิกการจองที่เลือก", CarWashUI.RED);
        cancelButton.addActionListener(e -> cancelSelectedBooking());

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(CarWashUI.createLabel("คิวของฉัน", 16f, Font.BOLD, CarWashUI.TEXT), BorderLayout.WEST);
        titleRow.add(bookingCount, BorderLayout.EAST);

        CarWashUI.Card card = new CarWashUI.Card(new BorderLayout(0, 12));
        card.add(titleRow, BorderLayout.NORTH);
        card.add(CarWashUI.createScrollPane(table), BorderLayout.CENTER);
        card.add(CarWashUI.rightAligned(cancelButton), BorderLayout.SOUTH);
        return card;
    }

    private void cancelSelectedBooking() {
        Integer id = ui.selectedBookingId(table, tableModel);
        if (id == null) return;
        if (!ui.confirm("ยืนยันยกเลิกการจองรหัส " + id + " ?")) return;

        try {
            qm.cancelByCustomer(id, customerPhone);
            refreshBookings();
        } catch (RuntimeException ex) {
            ui.showWarning(ex.getMessage());
        }
    }

    private void refreshBookings() {
        List<CUSTOMER> myBookings = new ArrayList<>(qm.byPhone(customerPhone));
        Collections.reverse(myBookings);
        CarWashUI.loadRows(tableModel, myBookings);
        bookingCount.setText(myBookings.size() + " รายการ");
        updateAvailability();
    }

    private CarWashUI.Card buildAvailabilityCard() {
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
                    slotBox.setSelectedItem(slot);
                }
            });
            slotChips[i] = chip;
            chipPanel.add(chip);
        }

        CarWashUI.Card card = new CarWashUI.Card(new BorderLayout(0, 12));
        card.add(availabilityTitle, BorderLayout.NORTH);
        card.add(chipPanel, BorderLayout.CENTER);
        card.add(CarWashUI.createLabel("คลิกที่ช่วงเวลาเพื่อเลือกในฟอร์มจอง", 12f, Font.PLAIN, CarWashUI.MUTED),
                BorderLayout.SOUTH);
        return card;
    }

    private void updateAvailability() {
        LocalDate date = CarWashUI.parseDate(dateField.getText());
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

    private void paintChip(JLabel chip, String slot, LocalDate date) {
        Color background = new Color(0xF1F5F9);
        Color foreground = new Color(0x94A3B8);
        String caption = "-";

        if (date != null) {
            long free = QueueManager.BAYS - qm.countBookings(date.toString(), slot);
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

        boolean picked = slot.equals(slotBox.getSelectedItem());
        Color borderColor = picked ? CarWashUI.BLUE : background.darker();

        chip.setBackground(background);
        chip.setForeground(foreground);
        chip.setText("<html><center><b>" + slot + "</b><br>" + caption + "</center></html>");
        chip.setBorder(new LineBorder(borderColor, picked ? 3 : 1, true));
    }
}