package yeyodra;

/** Data satu game (dari Hydra API / Steam AppID). */
public class Game {
    public String appId;   // Steam AppID -> dipakai untuk URL gambar
    public String title;
    public String genre;
    public String coverUrl;        // URL gambar dari Hydra (fallback, opsional)
    public String libraryImageUrl; // URL gambar dari Hydra (fallback, opsional)

    public Game(String appId, String title, String genre) {
        this.appId = appId;
        this.title = title;
        this.genre = genre;
    }
}
