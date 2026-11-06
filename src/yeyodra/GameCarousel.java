package yeyodra;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * UI aplikasi Yeyodra:
 *  - Tab "Home"       : card carousel "Hot now" (data dari database).
 *  - Tab "Kelola Game": tabel + form untuk CRUD (Create/Read/Update/Delete).
 *
 * Gambar kartu diambil dari Steam CDN berdasarkan AppID.
 */
public class GameCarousel extends JFrame {

    private static final int PER_PAGE = 4;
    private static final int GAP = 16;

    private static final Color BG = new Color(0x0a0a0a);
    private static final Color SIDEBAR = new Color(0x0d0d0d);
    private static final Color PANEL = new Color(0x121212);
    private static final Color MUTED = new Color(0xf0f1f7);
    private static final Color DIM = new Color(0x9a9ba4);
    private static final Color TEAL = new Color(0x16b195);

    private final GameRepository repo = new GameRepository();

    // ---- Home ----
    private final JPanel cardsRow = new JPanel(null) {
        @Override public void doLayout() { layoutCards(); }
    };
    private final JLabel pageLabel = new JLabel();
    private final JButton prev = new JButton("\u2039");
    private final JButton next = new JButton("\u203A");
    private final CardPanel[] slots = new CardPanel[PER_PAGE];
    private List<Game> homeGames = new ArrayList<>();
    private int page = 0;

    // ---- Kelola Game ----
    private final DefaultTableModel tableModel =
            new DefaultTableModel(new Object[]{"ID", "AppID", "Judul", "Genre", "EXE"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
    private final JTable table = new JTable(tableModel);
    private final JTextField fAppId = new JTextField();
    private final JTextField fTitle = new JTextField();
    private final JTextField fGenre = new JTextField();
    private final JTextArea fDesc = new JTextArea(3, 20);
    private final JTextField fExe = new JTextField();
    private final JLabel preview = new JLabel("Preview gambar", JLabel.CENTER);
    private int selectedId = -1; // -1 = mode tambah baru

    public GameCarousel() {
        setTitle("Yeyodra");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 680);
        setMinimumSize(new Dimension(920, 560));
        setLocationRelativeTo(null);

        JPanel homePanel = buildHome();
        JPanel managePanel = buildManage();

        JPanel content = new JPanel(new CardLayout());
        content.setBackground(BG);
        content.add(homePanel, "home");
        content.add(managePanel, "manage");
        CardLayout cl = (CardLayout) content.getLayout();

        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        navBar.setBackground(SIDEBAR);
        navBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 22)));
        JButton navHome = navButton("Home");
        JButton navManage = navButton("Kelola Game");
        navHome.addActionListener(e -> { cl.show(content, "home"); setNavActive(navHome, navManage, true); });
        navManage.addActionListener(e -> { cl.show(content, "manage"); setNavActive(navHome, navManage, false); });
        navBar.add(navHome);
        navBar.add(navManage);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(navBar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        setNavActive(navHome, navManage, true);
        if ("manage".equals(System.getenv("YEYODRA_TAB"))) {
            cl.show(content, "manage");
            setNavActive(navHome, navManage, false);
        }

        reloadHome();
        reloadTable();
        clearForm();
    }

    private JButton navButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(MUTED);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private void setNavActive(JButton home, JButton manage, boolean homeActive) {
        home.setForeground(homeActive ? TEAL : MUTED);
        manage.setForeground(homeActive ? MUTED : TEAL);
    }

    private void styleButton(JButton b) {
        b.setBackground(new Color(0x1c1c1c));
        b.setForeground(MUTED);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 38)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
    }

    // =====================================================================
    //  HOME  (carousel Hot now)
    // =====================================================================

    private JPanel buildHome() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 40, 24, 40));

        // -- header: judul + pager + panah --
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 0, 16, 0));

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
        prev.addActionListener(e -> { if (page > 0) { page--; refreshCards(); } });
        next.addActionListener(e -> { if (page < maxPage()) { page++; refreshCards(); } });
        right.add(pageLabel);
        right.add(prev);
        right.add(next);
        header.add(right, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        cardsRow.setOpaque(false);
        for (int i = 0; i < PER_PAGE; i++) {
            slots[i] = new CardPanel();
            cardsRow.add(slots[i]);
        }
        root.add(cardsRow, BorderLayout.CENTER);
        return root;
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
        return Math.max(0, (int) Math.ceil(homeGames.size() / (double) PER_PAGE) - 1);
    }

    /** Muat ulang kartu dari database. */
    private void reloadHome() {
        homeGames = repo.findAll();
        page = 0;
        refreshCards();
    }

    private void refreshCards() {
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            slots[i].setGame(idx < homeGames.size() ? homeGames.get(idx) : null);
        }
        pageLabel.setText((page + 1) + " / " + (maxPage() + 1));
        prev.setEnabled(page > 0);
        next.setEnabled(page < maxPage());
        cardsRow.revalidate();
        cardsRow.repaint();
    }

    private void layoutCards() {
        int w = cardsRow.getWidth();
        int rowH = cardsRow.getHeight();
        if (w <= 0 || rowH <= 0) return;
        int cardW = (w - GAP * (PER_PAGE - 1)) / PER_PAGE;
        int cardH = (int) Math.round(cardW * 353.0 / 616.0); // 16:9
        int h = Math.min(cardH, rowH);
        for (int i = 0; i < PER_PAGE; i++) {
            int x = i * (cardW + GAP);
            java.awt.Rectangle cur = slots[i].getBounds();
            if (cur.x != x || cur.y != 0 || cur.width != cardW || cur.height != h) {
                slots[i].setBounds(x, 0, cardW, h);
            }
        }
    }

    // =====================================================================
    //  KELOLA GAME  (CRUD)
    // =====================================================================

    private JPanel buildManage() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Toolbar atas
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        JLabel t = new JLabel("KELOLA GAME");
        t.setForeground(MUTED);
        t.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JButton btnNew = new JButton("+ Tambah Baru");
        JButton btnRefresh = new JButton("Refresh");
        styleButton(btnNew);
        styleButton(btnRefresh);
        btnNew.addActionListener(e -> { clearForm(); });
        btnRefresh.addActionListener(e -> reloadTable());
        toolbar.add(t);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(btnNew);
        toolbar.add(btnRefresh);
        root.add(toolbar, BorderLayout.NORTH);

        // Tabel (kiri)
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setBackground(PANEL);
        table.setForeground(MUTED);
        table.setSelectionBackground(TEAL);
        table.setSelectionForeground(Color.BLACK);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(0x1a1a1a));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getColumnModel().getColumn(0).setMaxWidth(48);
        table.getSelectionModel().addListSelectionListener(this::onRowSelected);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(PANEL);
        scroll.getViewport().setBackground(PANEL);
        scroll.setBorder(BorderFactory.createTitledBorder(null, "Daftar Game",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, null, DIM));

        // Form (kanan)
        JPanel form = buildForm();
        form.setPreferredSize(new Dimension(380, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, form);
        split.setResizeWeight(1.0);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setBackground(PANEL);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 30)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        JLabel h = new JLabel("Data Game");
        h.setForeground(MUTED);
        h.setFont(new Font("Segoe UI", Font.BOLD, 15));
        h.setAlignmentX(LEFT_ALIGNMENT);
        form.add(h);
        form.add(Box.createVerticalStrut(10));

        form.add(label("Steam AppID (untuk gambar)"));
        styleField(fAppId);
        form.add(fAppId);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Judul"));
        styleField(fTitle);
        form.add(fTitle);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Genre"));
        styleField(fGenre);
        form.add(fGenre);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Deskripsi"));
        fDesc.setLineWrap(true);
        fDesc.setWrapStyleWord(true);
        fDesc.setBackground(new Color(0x1c1c1c));
        fDesc.setForeground(MUTED);
        fDesc.setCaretColor(MUTED);
        fDesc.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JScrollPane descScroll = new JScrollPane(fDesc);
        descScroll.setAlignmentX(LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        form.add(descScroll);
        form.add(Box.createVerticalStrut(8));

        form.add(label("Path .exe (untuk tombol Jalankan)"));
        JPanel exeRow = new JPanel(new BorderLayout(6, 0));
        exeRow.setOpaque(false);
        exeRow.setAlignmentX(LEFT_ALIGNMENT);
        styleField(fExe);
        JButton browse = new JButton("Pilih\u2026");
        styleButton(browse);
        browse.addActionListener(e -> browseExe());
        exeRow.add(fExe, BorderLayout.CENTER);
        exeRow.add(browse, BorderLayout.EAST);
        exeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        form.add(exeRow);
        form.add(Box.createVerticalStrut(10));

        // Preview gambar
        preview.setPreferredSize(new Dimension(340, 160));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        preview.setAlignmentX(LEFT_ALIGNMENT);
        preview.setOpaque(true);
        preview.setBackground(new Color(0x0d0d0d));
        preview.setForeground(DIM);
        preview.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 25)));
        form.add(preview);
        form.add(Box.createVerticalStrut(10));

        // Tombol aksi
        JPanel buttons = new JPanel(new GridLayout(0, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        JButton btnPreview = new JButton("Lihat Gambar");
        JButton btnSave = new JButton("Simpan");
        JButton btnDelete = new JButton("Hapus");
        JButton btnRun = new JButton("Jalankan Game");
        styleButton(btnPreview);
        styleButton(btnSave);
        styleButton(btnDelete);
        styleButton(btnRun);
        btnPreview.addActionListener(e -> loadPreview());
        btnSave.addActionListener(e -> saveGame());
        btnDelete.addActionListener(e -> deleteGame());
        btnRun.addActionListener(e -> runGame());
        buttons.add(btnPreview);
        buttons.add(btnSave);
        buttons.add(btnDelete);
        buttons.add(btnRun);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        form.add(buttons);

        return form;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void styleField(JTextField f) {
        f.setBackground(new Color(0x1c1c1c));
        f.setForeground(MUTED);
        f.setCaretColor(MUTED);
        f.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        f.setAlignmentX(LEFT_ALIGNMENT);
    }

    // ----- Tabel <-> form -----

    private void reloadTable() {
        tableModel.setRowCount(0);
        for (Game g : repo.findAll()) {
            tableModel.addRow(new Object[]{g.id, g.appId, g.title, g.genre, g.exePath});
        }
    }

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 0)));
        Game g = findById(id);
        if (g == null) return;
        selectedId = g.id;
        fAppId.setText(g.appId);
        fTitle.setText(g.title);
        fGenre.setText(g.genre);
        fDesc.setText(g.description);
        fExe.setText(g.exePath);
        loadPreview();
    }

    private Game findById(int id) {
        for (Game g : repo.findAll()) if (g.id == id) return g;
        return null;
    }

    private void clearForm() {
        selectedId = -1;
        table.clearSelection();
        fAppId.setText("");
        fTitle.setText("");
        fGenre.setText("");
        fDesc.setText("");
        fExe.setText("");
        preview.setIcon(null);
        preview.setText("Preview gambar");
        fTitle.requestFocus();
    }

    // ----- CRUD actions -----

    private void saveGame() {
        String appId = fAppId.getText().trim();
        String title = fTitle.getText().trim();
        if (appId.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Steam AppID dan Judul wajib diisi.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Game g = new Game();
        g.id = selectedId;
        g.appId = appId;
        g.title = title;
        g.genre = fGenre.getText().trim();
        g.description = fDesc.getText().trim();
        g.exePath = fExe.getText().trim();

        if (selectedId < 0) {
            repo.insert(g);
        } else {
            repo.update(g);
        }
        reloadTable();
        reloadHome();
        clearForm();
        JOptionPane.showMessageDialog(this,
                selectedId < 0 ? "Game ditambahkan." : "Game diperbarui.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteGame() {
        if (selectedId < 0) {
            JOptionPane.showMessageDialog(this, "Pilih dulu game di tabel yang mau dihapus.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Hapus game ini dari database?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            repo.delete(selectedId);
            reloadTable();
            reloadHome();
            clearForm();
        }
    }

    private void browseExe() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih file .exe game");
        chooser.setFileFilter(new FileNameExtensionFilter("Aplikasi (*.exe)", "exe"));
        File pf = new File("C:\\Program Files");
        if (pf.exists()) chooser.setCurrentDirectory(pf);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fExe.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runGame() {
        String exe = fExe.getText().trim();
        if (exe.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Path .exe belum diisi.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File f = new File(exe);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "File tidak ditemukan:\n" + exe,
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            new ProcessBuilder(exe).directory(f.getParentFile()).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal menjalankan game:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----- Preview gambar (dari AppID via Steam CDN) -----

    private void loadPreview() {
        String appId = fAppId.getText().trim();
        if (appId.isEmpty()) {
            preview.setIcon(null);
            preview.setText("Isi AppID dulu");
            return;
        }
        Game g = new Game(appId, fTitle.getText(), fGenre.getText());
        preview.setIcon(null);
        preview.setText("Memuat gambar\u2026");
        new SwingWorker<BufferedImage, Void>() {
            @Override protected BufferedImage doInBackground() {
                return Steam.load(Steam.cardUrls(g));
            }
            @Override protected void done() {
                try {
                    BufferedImage img = get();
                    if (img != null) {
                        preview.setText("");
                        preview.setIcon(new javax.swing.ImageIcon(
                                img.getScaledInstance(340, 160, java.awt.Image.SCALE_SMOOTH)));
                    } else {
                        preview.setText("Gambar tidak ditemukan");
                    }
                } catch (Exception ex) {
                    preview.setText("Gagal memuat gambar");
                }
            }
        }.execute();
    }

    // =====================================================================
    //  Kartu carousel
    // =====================================================================

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
                    try { if (game == target) { img = get(); repaint(); } }
                    catch (Exception ignored) {}
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
