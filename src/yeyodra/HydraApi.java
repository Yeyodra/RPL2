package yeyodra;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ambil daftar game dari Hydra API (backend yang dipakai Novella).
 * Base URL: https://hydra-api-us-east-1.losbroxas.org
 *
 * Catatan: parsing JSON di sini sengaja pakai regex sederhana supaya tidak
 * perlu library tambahan. Tiap game pada /catalogue/weekly tidak punya object
 * bersarang, jadi aman.
 */
public class HydraApi {

    static final String BASE = "https://hydra-api-us-east-1.losbroxas.org";

    /** Ambil game "hot" mingguan. */
    public static List<Game> weekly() {
        String body = get("/catalogue/weekly?take=8&skip=0");
        List<Game> games = new ArrayList<>();
        if (body == null) return games;

        Matcher m = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(body);
        while (m.find()) {
            String obj = m.group(1);
            String appId = field(obj, "objectId");
            String title = field(obj, "title");
            if (appId != null && title != null) {
                Game g = new Game(appId, title, firstGenre(obj));
                g.coverUrl = field(obj, "coverImageUrl");
                g.libraryImageUrl = field(obj, "libraryImageUrl");
                games.add(g);
            }
        }
        return games;
    }

    private static String get(String path) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Novella/1.0.0")
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return resp.body();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String field(String obj, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(obj);
        return m.find() ? m.group(1) : null;
    }

    private static String firstGenre(String obj) {
        Matcher m = Pattern.compile("\"genres\"\\s*:\\s*\\[\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(obj);
        return m.find() ? m.group(1) : "";
    }
}
