package com.instax.android;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class SupabaseAuth {
    static final String BASE_URL = "https://pffhmjfwamcyuxvpolvc.supabase.co";
    static final String PUBLISHABLE_KEY = "sb_publishable_aprx9AsvP6ZAcyByIxri6A_fh1v6YD2";

    private SupabaseAuth() {}

    static JSONObject signIn(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        return request("/auth/v1/token?grant_type=password", "POST", body, null);
    }

    static JSONObject signUp(String email, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);
        return request("/auth/v1/signup", "POST", body, null);
    }

    static void createProfile(String accessToken, String userId, String email) throws Exception {
        JSONObject body = new JSONObject();
        body.put("id", userId);
        body.put("display_name", email.contains("@") ? email.substring(0, email.indexOf("@")) : email);
        request("/rest/v1/profiles?on_conflict=id", "POST", body, accessToken, true);
    }

    private static JSONObject request(String path, String method, JSONObject body, String token) throws Exception {
        return request(path, method, body, token, false);
    }

    private static JSONObject request(String path, String method, JSONObject body, String token, boolean upsert) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY);
        connection.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (upsert) connection.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
        }
        connection.disconnect();
        if (status < 200 || status >= 300) {
            String message = response.toString();
            try {
                JSONObject error = new JSONObject(message);
                message = error.optString("msg", error.optString("message", error.optString("error_description", message)));
            } catch (Exception ignored) { }
            throw new Exception("Supabase (" + status + "): " + message);
        }
        if (response.length() == 0) return new JSONObject();
        return new JSONObject(response.toString());
    }

    static String uploadMedia(String accessToken, String userId, android.content.ContentResolver resolver,
                              android.net.Uri uri, String mime) throws Exception {
        String extension;
        if ("image/jpeg".equals(mime)) extension = "jpg";
        else if ("image/png".equals(mime)) extension = "png";
        else if ("image/webp".equals(mime)) extension = "webp";
        else if ("image/gif".equals(mime)) extension = "gif";
        else if ("video/mp4".equals(mime)) extension = "mp4";
        else if ("video/webm".equals(mime)) extension = "webm";
        else if ("video/quicktime".equals(mime)) extension = "mov";
        else throw new Exception("Unsupported media type: " + mime);
        String filename = java.util.UUID.randomUUID().toString() + "." + extension;
        String path = userId + "/" + filename;
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL +
                "/storage/v1/object/instax-media/" + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(90000);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(65536);
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", mime);
        connection.setRequestProperty("x-upsert", "false");
        long total = 0;
        try (InputStream input = resolver.openInputStream(uri);
             OutputStream output = connection.getOutputStream()) {
            if (input == null) throw new Exception("Unable to read the selected media.");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > 50L * 1024L * 1024L) throw new Exception("Media exceeds the 50 MB upload limit.");
                output.write(buffer, 0, count);
            }
        } catch (Exception error) {
            connection.disconnect();
            throw error;
        }
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        connection.disconnect();
        if (status < 200 || status >= 300) throw new Exception("Media upload failed (" + status + "): " + response);
        return BASE_URL + "/storage/v1/object/public/instax-media/" + path;
    }

    static void publishPost(String accessToken, String userId, String mediaUrl, String mime, String caption) throws Exception {
        JSONObject post = new JSONObject();
        post.put("user_id", userId);
        post.put("media_url", mediaUrl);
        post.put("media_type", mime != null && mime.startsWith("video/") ? "video" : "image");
        post.put("caption", caption == null ? "" : caption);
        request("/rest/v1/posts", "POST", post, accessToken);
    }

    static org.json.JSONArray loadFeed(String accessToken) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL +
                "/rest/v1/posts?select=id,user_id,media_url,media_type,caption,created_at&order=created_at.desc&limit=30").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY);
        connection.setRequestProperty("Authorization", "Bearer " +
                (accessToken == null || accessToken.isEmpty() ? PUBLISHABLE_KEY : accessToken));
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        connection.disconnect();
        if (status < 200 || status >= 300) throw new Exception("Feed request failed (" + status + "): " + response);
        return new org.json.JSONArray(response.toString());
    }
\
}
