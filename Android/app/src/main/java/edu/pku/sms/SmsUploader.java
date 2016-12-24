package edu.pku.sms;

import android.util.Log;
import android.util.SparseArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SmsUploader {
    private static final String URL = "http://example.com:1992/post/";
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client = new OkHttpClient();

    private static Random random = new Random();
    private static SparseArray<JSONObject> hanging = new SparseArray<>();

    static Call upload(String receiver, String sender, String content, String date) throws IOException {
        JSONObject json = buildJSON(receiver, sender, content, date);
        try {
            hanging.put(json.getInt("ID"), json);
            return doUpload(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Call doUpload(JSONObject json) throws JSONException {
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(URL + json.getString("Receiver"))
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new MyCallback());

        return call;
    }

    static void uploadAny() {
        Log.i("SmsUploader", String.format("%d message(s) to upload", hanging.size()));

        for (int i = 0; i < hanging.size(); i++) {
            JSONObject json = hanging.valueAt(i);
            try {
                doUpload(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static class MyCallback implements Callback {
        @Override
        public void onFailure(Call call, IOException e) {}

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                String body = response.body().string();

                try {
                    int id = Integer.parseInt(body.substring(0, body.indexOf('\n')));
                    hanging.remove(id);
                } catch (NumberFormatException e) {
                    Log.e("SmsUploader", "Unrecognizable response from server");
                    e.printStackTrace();
                }

                Log.i("SmsUploader", body);
            } else {
                Log.i("SmsUploader", "Failed to upload");
            }
        }
    }

    private static JSONObject buildJSON(String receiver, String sender, String content, String date) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ID", random.nextInt(Integer.MAX_VALUE));
            obj.put("Receiver", receiver);
            obj.put("Sender", sender);
            obj.put("Content", content);
            obj.put("Date", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obj;
    }

    public static void main(String[] args) throws IOException {
        upload("13800138000", "10086", "Hello!", "");
    }
}
