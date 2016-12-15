package edu.pku.sms;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Callback;
import okhttp3.Response;

public class SmsUploader {
    private static final String URL = "http://example.com:1992/post/";
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client = new OkHttpClient();

    Call upload(String receiver, String sender, String content, String date) throws IOException {
        String json = buildJSON(sender, content, date);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(URL + receiver)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new MyCallback());
        return call;
    }

    private class MyCallback implements Callback {
        @Override
        public void onFailure(Call call, IOException e) {}

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                String body = response.body().string();
                Log.i("SmsUploader", body);
            } else {
                Log.i("SmsUploader", "Failed to upload.");
            }
        }
    }

    private static String buildJSON(String sender, String content, String date) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("Sender", sender);
            obj.put("Content", content);
            obj.put("Date", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    public static void main(String[] args) throws IOException {
        SmsUploader uploader = new SmsUploader();
        uploader.upload("13800138000", "10086", "Hello!", "");
    }
}
