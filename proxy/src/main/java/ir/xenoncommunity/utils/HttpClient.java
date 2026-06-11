package ir.xenoncommunity.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class HttpClient {

    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0";

    private final Map<URL, List<String>> cachedResponse = Maps.newConcurrentMap();
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();

    private final ExecutorService executorService = new ThreadPoolExecutor(
            4,
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public CompletableFuture<JsonObject> post(String url, JsonObject object) throws MalformedURLException {
        return postAsync(new URL(url), object);
    }

    public CompletableFuture<ArrayList<String>> get(URL url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final HttpURLConnection conn = openConnection(url);
                conn.setRequestMethod("GET");
                conn.setDoOutput(false);

                final int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new java.io.FileNotFoundException("404 Not Found: " + url);
                }
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("HTTP " + status + " from " + url);
                }

                final ArrayList<String> lines = Lists.newArrayList();
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        lines.add(line.trim());
                    }
                }
                return lines;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<JsonObject> postAsync(URL url, JsonObject object) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String json = gson.toJson(object);
                final HttpURLConnection conn = openConnection(url);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                final int status = conn.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("HTTP " + status + " from " + url);
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return parser.parse(br).getAsJsonObject();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<JsonObject> discord(String message, String webhookLink) throws MalformedURLException {
        final JsonObject object = new JsonObject();
        object.addProperty("content", message);
        return postAsync(new URL(webhookLink), object);
    }

    public CompletableFuture<List<String>> getCached(URL url) {
        return CompletableFuture.supplyAsync(
                () -> cachedResponse.computeIfAbsent(url, k -> get(url).join()),
                executorService
        );
    }

    public CompletableFuture<JsonObject> GETJson(URL url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final HttpURLConnection conn = openConnection(url);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(false);

                final int status = conn.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("HTTP " + status + " from " + url);
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    return parser.parse(br).getAsJsonObject();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    public CompletableFuture<JsonObject> GETJson(String url) throws MalformedURLException {
        return GETJson(new URL(url));
    }

    private HttpURLConnection openConnection(URL url) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Host", url.getHost());
        conn.setInstanceFollowRedirects(true);
        return conn;
    }
}
