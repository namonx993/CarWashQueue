import java.awt.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

/**
 * แผงควบคุมเจ้าของร้าน: ประกอบด้วย 3 แท็บ
 */
public class OwnerScreen {
    private static final Color NAVY = CarWashUI.NAVY;
    private static final Color SKY = CarWashUI.SKY;
    private static final Color BG = CarWashUI.BG;
    private static final Color TAB_INACTIVE_TEXT = new Color(0x94A3B8);

    private final CarWashUI ui;
    private final QueueManager qm;

    public OwnerScreen(CarWashUI ui, QueueManager qm) {
        this.ui = ui;
        this.qm = qm;
    }

    public static final class Tab {
        public final String title;
        public final JPanel content;
        public final Runnable onShow;

        public Tab(String title, JPanel content, Runnable onShow) {
            this.title = title;
            this.content = content;
            this.onShow = onShow;
        }
    }

    public JPanel build() {
        List<Tab> tabs = Arrays.asList(
                new OwnerBookingTab(ui, qm).create(),
                new OwnerIncomeTab(qm).create(),
                new OwnerPriceTab(ui, qm).create());

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
        highlightTab(buttons, tabs.get(0).title);
        return menu;
    }

    private static void highlightTab(JButton[] buttons, String selectedTitle) {
        for (JButton button : buttons) {
            boolean active = button.getText().equals(selectedTitle);
            CarWashUI.styleTab(button, active, Color.WHITE, TAB_INACTIVE_TEXT, SKY, NAVY);
        }
    }
}