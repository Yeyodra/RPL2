package yeyodra;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses database SQLite untuk tabel `game` (CRUD).
 * File database: game.db (di folder aplikasi).
 *
 * ------------------------------------------------------------------
 * CATATAN MIGRASI KE MySQL:
 * Semua akses DB dikumpulkan di class ini dengan method yang sama
 * (findAll / insert / update / delete). Untuk pindah ke MySQL:
 *   1) ganti driver: sqlite-jdbc  ->  mysql-connector-j
 *   2) ganti connect():  DriverManager.getConnection("jdbc:mysql://...", user, pass)
 *   3) sesuaikan DDL (AUTOINCREMENT -> AUTO_INCREMENT, TEXT -> VARCHAR/TEXT)
 * UI (Main/GameCarousel) dan model (Game) TIDAK perlu diubah.
 * ------------------------------------------------------------------
 */
public class GameRepository {

    private final String url;

    public GameRepository() {
        this("game.db");
    }

    public GameRepository(String dbFile) {
        this.url = "jdbc:sqlite:" + new File(dbFile).getAbsolutePath();
        init();
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(url);
    }

    /** Buat tabel kalau belum ada + isi data contoh kalau masih kosong. */
    private void init() {
        String sql = "CREATE TABLE IF NOT EXISTS game (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "app_id TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "genre TEXT," +
                "description TEXT," +
                "exe_path TEXT," +
                "cover_url TEXT," +
                "library_image_url TEXT" +
                ")";
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute(sql);
            if (count() == 0) seed();
        } catch (Exception e) {
            System.err.println("Gagal init database: " + e.getMessage());
        }
    }

    private void seed() {
        insert(new Game("1091500", "Cyberpunk 2077", "RPG"));
        insert(new Game("1245620", "ELDEN RING", "RPG"));
        insert(new Game("2358720", "Black Myth: Wukong", "Action"));
        insert(new Game("2215430", "Ghost of Tsushima", "Action"));
        insert(new Game("1086940", "Baldur's Gate 3", "RPG"));
        insert(new Game("1174180", "Red Dead Redemption 2", "Adventure"));
    }

    public int count() {
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM game")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Game> findAll() {
        List<Game> list = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM game ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        } catch (Exception e) {
            System.err.println("findAll error: " + e.getMessage());
        }
        return list;
    }

    /** INSERT game baru (id diabaikan). */
    public void insert(Game g) {
        String sql = "INSERT INTO game(app_id,title,genre,description,exe_path,cover_url,library_image_url) " +
                "VALUES(?,?,?,?,?,?,?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, g);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("insert error: " + e.getMessage());
        }
    }

    /** UPDATE berdasarkan id. */
    public void update(Game g) {
        String sql = "UPDATE game SET app_id=?,title=?,genre=?,description=?,exe_path=?,cover_url=?,library_image_url=? " +
                "WHERE id=?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, g);
            ps.setInt(8, g.id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("update error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM game WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("delete error: " + e.getMessage());
        }
    }

    private void bind(PreparedStatement ps, Game g) throws Exception {
        ps.setString(1, g.appId);
        ps.setString(2, g.title);
        ps.setString(3, nvl(g.genre));
        ps.setString(4, nvl(g.description));
        ps.setString(5, nvl(g.exePath));
        ps.setString(6, nvl(g.coverUrl));
        ps.setString(7, nvl(g.libraryImageUrl));
    }

    private Game map(ResultSet rs) throws Exception {
        Game g = new Game();
        g.id = rs.getInt("id");
        g.appId = rs.getString("app_id");
        g.title = rs.getString("title");
        g.genre = rs.getString("genre");
        g.description = rs.getString("description");
        g.exePath = rs.getString("exe_path");
        g.coverUrl = rs.getString("cover_url");
        g.libraryImageUrl = rs.getString("library_image_url");
        return g;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
