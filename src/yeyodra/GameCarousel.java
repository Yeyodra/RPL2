package yeyodra;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * UI: card carousel "Hot now".
 * - 4 kartu per halaman, gambar landscape 16:9 diambil dari Steam CDN via AppID.
 * - Tombol panah kiri/kanan untuk pindah halaman.
 */
public class GameCarousel extends JFrame {

    private static final int PER_PAGE = 4;
    private static final int GAP = 16;

    private static final Color BG = new Color(0x0a0a0a);
    private static final Color MUTED = new Color(0xf0f1f7);
    private static final Color DIM = new Color(0x9a9ba4);
    private static final Color TEAL = new Color(0x16b195);

    private List<Game> games = new ArrayList<>();
    private int page = 0;

    private final JPanel cardsRow = new JPanel(null);
    private final JLabel pageLabel = new JLabel();
    private final JButton prev = new JButton("\u2039");
    private final JButton next = new JButton("\u203A");
    private final CardPanel[] slots = new CardPanel[PER_PAGE];

    public GameCarousel() {
        setTitle("Yeyodra \u2014 Hot now");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);

        cardsRow.setOpaque(false);
        root.add(cardsRow, BorderLayout.CENTER);

        for (int i = 0; i < PER_PAGE; i++) {
            slots[i] = new CardPanel();
            cardsRow.add(slots[i]);
        }

        cardsRow.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutCards();
            }
        });

        setGames(dataContoh());

        // Ukuran awal: 4 kartu 16:9 kelihatan penuh, rata atas di bawah judul.
        int w0 = 1280;
        int innerW = w0 - 80; // padding kiri+kanan (40+40)
        int cardW = (innerW - GAP * (PER_PAGE - 1)) / PER_PAGE;
        int cardH = (int) Math.round(cardW * 353.0 / 616.0);
        int height = cardH + 108; // padding atas/bawah + judul header + kartu
        setSize(w0, height);
        setMinimumSize(new Dimension(820, 210));
        setLocationRelativeTo(null);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JLabel title = new JLabel("HOT NOW");
        title.setForeground(MUTED);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        pageLabel.setForeground(DIM);
        pageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        styleArrow(prev);
        styleArrow(next);
        prev.addActionListener(e -> { if (page > 0) { page--; refresh(); } });
        next.addActionListener(e -> { if (page < maxPage()) { page++; refresh(); } });
        right.add(pageLabel);
        right.add(prev);
        right.add(next);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void styleArrow(JButton b) {
        b.setPreferredSize(new Dimension(34, 34));
        b.setFocusPainted(false);
        b.setBackground(new Color(0x1c1c1c));
        b.setForeground(MUTED);
        b.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 40)));
        b.setFont(new Font("Segoe UI", Font.BOLD, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private int maxPage() {
        return Math.max(0, (int) Math.ceil(games.size() / (double) PER_PAGE) - 1);
    }

    /** Isi data carousel (dipanggil dari Main setelah ambil Hydra API). */
    public void setGames(List<Game> list) {
        this.games = (list == null) ? new ArrayList<>() : list;
        this.page = 0;
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            slots[i].setGame(idx < games.size() ? games.get(idx) : null);
        }
        pageLabel.setText((page + 1) + " / " + (maxPage() + 1));
        prev.setEnabled(page > 0);
        next.setEnabled(page < maxPage());
        layoutCards();
    }

    private void layoutCards() {
        int w = cardsRow.getWidth();
        int rowH = cardsRow.getHeight();
        if (w <= 0 || rowH <= 0) return;
        int cardW = (w - GAP * (PER_PAGE - 1)) / PER_PAGE;
        int cardH = (int) Math.round(cardW * 353.0 / 616.0); // rasio 16:9
        int h = Math.min(cardH, rowH);
        int y = 0; // rata atas, tepat di bawah header (seperti home)
        for (int i = 0; i < PER_PAGE; i++) {
            slots[i].setBounds(i * (cardW + GAP), y, cardW, h);
        }
    }

    private static List<Game> dataContoh() {
        List<Game> l = new ArrayList<>();
        l.add(new Game("1091500", "Cyberpunk 2077", "RPG"));
        l.add(new Game("1245620", "ELDEN RING", "RPG"));
        l.add(new Game("2358720", "Black Myth: Wukong", "Action"));
        l.add(new Game("2215430", "Ghost of Tsushima", "Action"));
        l.add(new Game("1086940", "Baldur's Gate 3", "RPG"));
        l.add(new Game("1174180", "Red Dead Redemption 2", "Adventure"));
        l.add(new Game("1030300", "Hollow Knight: Silksong", "Metroidvania"));
        l.add(new Game("292030", "The Witcher 3", "RPG"));
        return l;
    }

    /** Satu kartu: gambar cover (dari AppID) + judul di bawah. */
    static class CardPanel extends JComponent {
        private Game game;
        private BufferedImage img;
        private boolean hover = false;

        CardPanel() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setGame(Game g) {
            this.game = g;
            this.img = null;
            repaint();
            if (g == null) return;
            final Game target = g;
            new SwingWorker<BufferedImage, Void>() {
                @Override protected BufferedImage doInBackground() {
                    return Steam.load(Steam.cardUrls(target));
                }
                @Override protected void done() {
                    try {
                        if (game == target) { img = get(); repaint(); }
                    } catch (Exception ignored) {
                    }
                }
            }.execute();
        }

        @Override protected void paintComponent(Graphics g) {
            if (game == null) return;
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));

            g2.setColor(new Color(0x111111));
            g2.fillRect(0, 0, w, h);

            if (img != null) {
                double ir = img.getWidth() / (double) img.getHeight();
                double tr = w / (double) h;
                int dw, dh;
                if (ir > tr) { dh = h; dw = (int) (h * ir); }
                else { dw = w; dh = (int) (w / ir); }
                g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                g2.setColor(DIM);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString("Memuat\u2026", 12, h / 2);
            }

            if (hover) {
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRect(0, 0, w, h);
            }

            // gradient gelap di bawah supaya judul terbaca
            g2.setPaint(new GradientPaint(0, h - h / 2, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 220)));
            g2.fillRect(0, h - h / 2, w, h / 2);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.drawString(game.title, 12, h - 16);
            if (game.genre != null && !game.genre.isEmpty()) {
                g2.setColor(TEAL);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString(game.genre, 12, h - 34);
            }
            g2.dispose();

            Graphics2D g3 = (Graphics2D) g.create();
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g3.setColor(hover ? TEAL : new Color(255, 255, 255, 28));
            g3.drawRoundRect(1, 1, w - 2, h - 2, 16, 16);
            g3.dispose();
        }
    }
}
