import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * แท็บตั้งราคาบริการ
 */
public class OwnerPriceTab {
    private static final int SERVICE_COLUMN = 0;
    private static final int PRICE_COLUMN = 1;

    private final CarWashUI ui;
    private final QueueManager qm;

    private final DefaultTableModel model = createPriceModel();
    private final JTable table = new JTable(model);

    public OwnerPriceTab(CarWashUI ui, QueueManager qm) {
        this.ui = ui;
        this.qm = qm;
    }

    public OwnerScreen.Tab create() {
        CarWashUI.styleTable(table);
        fillRows();

        CarWashUI.Card card = new CarWashUI.Card(new BorderLayout(0, 4));
        card.add(buildHeading(), BorderLayout.NORTH);
        card.add(CarWashUI.createScrollPane(table), BorderLayout.CENTER);
        card.add(buildFooter(), BorderLayout.SOUTH);
        return new OwnerScreen.Tab("ตั้งราคาบริการ", CarWashUI.createPage(card), () -> { });
    }

    private DefaultTableModel createPriceModel() {
        return new DefaultTableModel(new String[]{"บริการ", "ราคาพื้นฐาน (รถเก๋ง)"}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column == PRICE_COLUMN;
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
        head.put(CarWashUI.createLabel("ตั้งราคาบริการ", 18f, Font.BOLD, CarWashUI.TEXT));
        head.put(CarWashUI.createLabel("ดับเบิลคลิกที่ช่องราคาเพื่อแก้ไข แล้วกดบันทึก", 13f, Font.PLAIN, CarWashUI.MUTED));
        return head;
    }

    private JPanel buildFooter() {
        JButton saveButton = new CarWashUI.RoundButton("บันทึกราคา", CarWashUI.BLUE);
        saveButton.addActionListener(e -> savePrices());

        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.add(CarWashUI.createLabel(describeMultipliers(), 12f, Font.PLAIN, CarWashUI.MUTED), BorderLayout.NORTH);
        footer.add(CarWashUI.rightAligned(saveButton), BorderLayout.SOUTH);
        return footer;
    }

    private void savePrices() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        try {
            for (int row = 0; row < model.getRowCount(); row++) {
                qm.setPrice(serviceAt(row), priceAt(row));
            }
            qm.savePrices();
            ui.showInfo("บันทึกราคาแล้ว (ลูกค้าจะเห็นราคาใหม่เมื่อเข้าสู่ระบบครั้งถัดไป)");
        } catch (IllegalArgumentException ex) {
            ui.showWarning("ราคาต้องเป็นตัวเลขจำนวนเต็มไม่ติดลบ");
        } catch (IllegalStateException ex) {
            ui.showWarning(ex.getMessage());
        }
    }

    private String serviceAt(int row) {
        return (String) model.getValueAt(row, SERVICE_COLUMN);
    }

    private int priceAt(int row) {
        return Integer.parseInt(model.getValueAt(row, PRICE_COLUMN).toString().trim());
    }

    private static String describeMultipliers() {
        StringBuilder text = new StringBuilder("ตัวคูณตามประเภทรถ:  ");
        for (int i = 0; i < QueueManager.CARS.length; i++) {
            if (i > 0) text.append("   |   ");
            text.append(QueueManager.CARS[i]).append(" ×").append(QueueManager.MULT[i]);
        }
        return text.toString();
    }
}