package com.example.woshield;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.example.woshield.BuildConfig;

public class HuggingFaceService {
    private static final String API_URL = "https://api-inference.huggingface.co/models/facebook/blenderbot-400M-distill";
    private static final String API_KEY = BuildConfig.HUGGING_FACE_API_KEY; // API key is now in BuildConfig

    public interface ChatbotCallback {
        void onResponse(String response);
        void onError(String error);
    }

    public static void getChatResponse(String userMessage, ChatbotCallback callback) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("inputs", userMessage);
        } catch (Exception e) {
            callback.onError("JSON Error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Request Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("API Error: " + response.body().string());
                    return;
                }

                String responseData = response.body().string();
                try {
                    JSONArray responseArray = new JSONArray(responseData);
                    if (responseArray.length() > 0) {
                        String chatResponse = responseArray.getJSONObject(0).getString("generated_text");
                        callback.onResponse(chatResponse);
                    } else {
                        callback.onError("No response from AI");
                    }
                } catch (Exception e) {
                    callback.onError("Parsing Error: " + e.getMessage());
                }
            }
        });
    }
}
