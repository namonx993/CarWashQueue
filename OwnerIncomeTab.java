import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

/**
 * แท็บสรุปรายได้และสถิติ
 */
public class OwnerIncomeTab {
    private final QueueManager qm;

    private final JTextField dayField = CarWashUI.createTextField(LocalDate.now().toString());
    private final JLabel doneCountLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, CarWashUI.BLUE);
    private final JLabel dayIncomeLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, CarWashUI.GREEN);
    private final JLabel totalIncomeLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, CarWashUI.NAVY);
    private final JLabel waitingLabel = CarWashUI.createLabel("-", 26f, Font.BOLD, CarWashUI.AMBER);

    public OwnerIncomeTab(QueueManager qm) {
        this.qm = qm;
    }

    public OwnerScreen.Tab create() {
        dayField.setColumns(10);
        CarWashUI.onTextChange(dayField, this::refresh);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.add(buildDatePicker(), BorderLayout.NORTH);
        content.add(buildStatsRow(), BorderLayout.CENTER);

        refresh();
        return new OwnerScreen.Tab("สรุปรายได้", CarWashUI.createPage(content), this::refresh);
    }

    private CarWashUI.Card buildDatePicker() {
        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        picker.setOpaque(false);
        picker.add(CarWashUI.createLabel("เลือกวันที่ (yyyy-MM-dd)", 14f, Font.PLAIN, CarWashUI.MUTED));
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
        card.add(CarWashUI.createLabel(title, 13f, Font.PLAIN, CarWashUI.MUTED), BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void refresh() {
        String day = dayField.getText().trim();
        doneCountLabel.setText(qm.countDoneOn(day) + " คัน");
        dayIncomeLabel.setText(String.format("%,d บาท", qm.incomeOn(day)));
        totalIncomeLabel.setText(String.format("%,d บาท", qm.incomeTotal()));
        waitingLabel.setText(qm.countWaiting() + " คิว");
    }
}