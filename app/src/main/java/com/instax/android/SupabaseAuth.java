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
}
