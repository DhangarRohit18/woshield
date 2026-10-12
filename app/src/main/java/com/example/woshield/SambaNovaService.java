package com.example.woshield;

import android.util.Log;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SambaNovaService {
    private static final String BASE_URL = "https://api.sambanova.ai/";
    private static final String TAG = "SambaNovaService";
    private static final String MODEL = "Meta-Llama-3.1-8B-Instruct"; // Or "Meta-Llama-3.1-70B-Instruct"
    private static final String API_KEY = "60d2ca34-6828-4b27-a652-95f51e3a8898";

    public interface ChatCallback {
        void onResponse(String response);

        void onError(String error);
    }

    public static void getChatResponse(String userMessage, ChatCallback callback) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SambaNovaApiService api = retrofit.create(SambaNovaApiService.class);

        SambaNovaRequest.Message message = new SambaNovaRequest.Message("user", userMessage);
        SambaNovaRequest request = new SambaNovaRequest(MODEL, Collections.singletonList(message), false);

        String authHeader = "Bearer " + API_KEY;

        api.getChatCompletion(authHeader, request).enqueue(new Callback<SambaNovaResponse>() {
            @Override
            public void onResponse(@NonNull Call<SambaNovaResponse> call,
                    @NonNull Response<SambaNovaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getChoices() != null && !response.body().getChoices().isEmpty()) {
                        String botResponse = response.body().getChoices().get(0).getMessage().getContent();
                        if (botResponse != null) {
                            callback.onResponse(botResponse);
                        } else {
                            callback.onError("SambaNova returned an empty content.");
                        }
                    } else {
                        callback.onError("SambaNova returned no choices.");
                    }
                } else {
                    try {
                        String errorBody = (response.errorBody() != null) ? response.errorBody().string()
                                : "No error body";
                        Log.e(TAG, "SambaNova API Error [" + response.code() + "]: " + errorBody);
                        callback.onError("Error: " + response.code() + " - " + errorBody);
                    } catch (IOException e) {
                        callback.onError("Failed to parse error response.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<SambaNovaResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Retrofit Failure: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
