package yeyodra;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;

/**
 * MAIN — titik masuk aplikasi.
 * 1) Tampilkan jendela carousel.
 * 2) Di background, ambil data live dari Hydra API; kalau berhasil, ganti data.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameCarousel frame = new GameCarousel();
            frame.setVisible(true);

            new SwingWorker<List<Game>, Void>() {
                @Override protected List<Game> doInBackground() {
                    return HydraApi.weekly();
                }
                @Override protected void done() {
                    try {
                        List<Game> games = get();
                        if (games != null && !games.isEmpty()) frame.setGames(games);
                    } catch (Exception ignored) {
                    }
                }
            }.execute();
        });
    }
}
