package yeyodra;

/** Data satu game. Disimpan di SQLite, gambar diambil dari Steam CDN via AppID. */
public class Game {
    public int id;             // primary key (auto)
    public String appId;       // Steam AppID -> dipakai untuk URL gambar
    public String title;
    public String genre;
    public String description;
    public String exePath;     // lokasi file .exe (untuk dijalankan seperti shortcut)

    // URL gambar dari Hydra API (fallback bila template AppID tidak ada)
    public String coverUrl;
    public String libraryImageUrl;

    public Game() {}

    public Game(String appId, String title, String genre) {
        this.appId = appId;
        this.title = title;
        this.genre = genre;
    }
}
