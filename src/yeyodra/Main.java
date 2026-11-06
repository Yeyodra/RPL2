package yeyodra;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * MAIN — titik masuk aplikasi Yeyodra.
 * Membuka jendela (Home + Kelola Game). Data disimpan di SQLite (game.db).
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new GameCarousel().setVisible(true));
    }
}
