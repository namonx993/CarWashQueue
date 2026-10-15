import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * แท็บรายการจอง: ค้นหา / กรอง / เปลี่ยนสถานะ / ลบ
 */
public class OwnerBookingTab {
    private static final int FIRST_SEARCH_COLUMN = CarWashUI.COL_NAME;
    private static final int LAST_SEARCH_COLUMN = 5;
    private static final int STATUS_COLUMN = CarWashUI.COL_STATUS;

    private final CarWashUI ui;
    private final QueueManager qm;

    private final DefaultTableModel model = CarWashUI.createBookingModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = CarWashUI.createTextField("");
    private final JComboBox<String> statusBox = CarWashUI.createComboBox(
            new String[]{CarWashUI.ALL, QueueManager.WAIT, QueueManager.DONE, QueueManager.CANCEL});
    private final JLabel countLabel = CarWashUI.createLabel(" ", 13f, Font.PLAIN, CarWashUI.MUTED);

    public OwnerBookingTab(CarWashUI ui, QueueManager qm) {
        this.ui = ui;
        this.qm = qm;
    }

    public OwnerScreen.Tab create() {
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
        return new OwnerScreen.Tab("รายการจอง", CarWashUI.createPage(card), this::reload);
    }

    private JPanel buildFilterBar() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(CarWashUI.createLabel("ค้นหา", 13f, Font.PLAIN, CarWashUI.MUTED));
        filters.add(searchField);
        filters.add(CarWashUI.createLabel("สถานะ", 13f, Font.PLAIN, CarWashUI.MUTED));
        filters.add(statusBox);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(filters, BorderLayout.WEST);
        bar.add(countLabel, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildActionButtons() {
        JButton reloadButton = new CarWashUI.RoundButton("รีเฟรช", CarWashUI.MUTED);
        JButton doneButton = new CarWashUI.RoundButton("เสร็จสิ้น", CarWashUI.GREEN);
        JButton cancelButton = new CarWashUI.RoundButton("ยกเลิก", CarWashUI.AMBER);
        JButton deleteButton = new CarWashUI.RoundButton("ลบ", CarWashUI.RED);

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

    private static RowFilter<DefaultTableModel, Integer> createFilter(String status, String keyword) {
        return new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> row) {
                boolean statusMatches = CarWashUI.ALL.equals(status) || row.getStringValue(STATUS_COLUMN).equals(status);
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
}