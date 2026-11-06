package yeyodra;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Ambil gambar dari Steam CDN berdasarkan AppID (sama seperti Novella).
 * Card carousel memakai gambar landscape.
 */
public class Steam {

    private static final String CDN = "https://cdn.akamai.steamstatic.com/steam/apps/";

    /**
     * Daftar URL gambar landscape untuk sebuah game (dicoba berurutan).
     * Berdasarkan AppID (seperti Novella), lalu URL asli dari Hydra sebagai
     * cadangan supaya game baru tetap tampil.
     */
    public static String[] cardUrls(Game g) {
        String appId = g.appId;
        String[] utama = {
                CDN + appId + "/capsule_616x353.jpg",
                CDN + appId + "/header.jpg",
                CDN + appId + "/library_hero.jpg"
        };
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String u : utama) list.add(u);
        if (g.coverUrl != null && !g.coverUrl.isBlank()) list.add(g.coverUrl);
        if (g.libraryImageUrl != null && !g.libraryImageUrl.isBlank()) list.add(g.libraryImageUrl);
        return list.toArray(new String[0]);
    }

    /** Download gambar pertama yang berhasil. Null jika semua gagal. */
    public static BufferedImage load(String[] urls) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        for (String url : urls) {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET().build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200 && resp.body().length > 0) {
                    BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(resp.body()));
                    if (img != null) return img;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
