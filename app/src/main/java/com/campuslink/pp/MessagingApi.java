package com.campuslink.pp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Shared Supabase REST helpers for CampusLink conversations and messages. */
public final class MessagingApi {
    public static final String PREFS = "CampusLinkSession";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String USER_ID = "user_id";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private MessagingApi() {}

    public interface ResultCallback {
        void onResult(boolean success, int code, String body, Exception error);
    }

    public static String userId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(USER_ID, "");
    }

    private static String token(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACCESS_TOKEN, "");
    }

    private static Request.Builder request(Context context, String path) {
        return new Request.Builder()
                .url(SupabaseConfig.SUPABASE_URL + path)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token(context))
                .addHeader("Accept", "application/json");
    }

    public static void get(Context context, String path, ResultCallback callback) {
        CLIENT.newCall(request(context, path).get().build()).enqueue(wrap(callback));
    }

    public static void post(Context context, String path, JSONObject payload, ResultCallback callback) {
        RequestBody body = RequestBody.create(payload.toString(), JSON);
        CLIENT.newCall(request(context, path)
                .addHeader("Prefer", "return=representation")
                .post(body).build()).enqueue(wrap(callback));
    }

    public static void patch(Context context, String path, JSONObject payload, ResultCallback callback) {
        RequestBody body = RequestBody.create(payload.toString(), JSON);
        CLIENT.newCall(request(context, path).patch(body).build()).enqueue(wrap(callback));
    }

    public static String enc(String value) {
        try { return URLEncoder.encode(value, StandardCharsets.UTF_8.name()); }
        catch (Exception e) { return value; }
    }

    private static Callback wrap(ResultCallback callback) {
        return new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onResult(false, 0, "", e);
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                callback.onResult(response.isSuccessful(), response.code(), body, null);
                response.close();
            }
        };
    }

    public static JSONArray array(String body) throws Exception {
        return new JSONArray(body);
    }
}
