package com.example.woshield;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatBotActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private SambaNovaApiService apiService;
    private final String API_KEY = "60d2ca34-6828-4b27-a652-95f51e3a8898";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        ImageView sendBtn = findViewById(R.id.sendBtn);
        ImageView backBtn = findViewById(R.id.backBtn);

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        setupRecyclerView();
        setupSambaNova();

        // Add default message
        addMessage("Ask me any problems", false);

        if (sendBtn != null) {
            sendBtn.setOnClickListener(v -> {
                String userInput = messageInput.getText().toString().trim();
                if (!userInput.isEmpty()) {
                    addMessage(userInput, true);
                    messageInput.setText("");
                    getSambaNovaResponse(userInput);
                }
            });
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // messages start from bottom or keep last visible
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupSambaNova() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.sambanova.ai/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(SambaNovaApiService.class);
    }

    private void getSambaNovaResponse(String query) {
        List<SambaNovaRequest.Message> messages = new ArrayList<>();
        messages.add(new SambaNovaRequest.Message("system", "You are a helpful assistant"));
        messages.add(new SambaNovaRequest.Message("user", query));

        SambaNovaRequest request = new SambaNovaRequest("ALLaM-7B-Instruct-preview", messages, false);

        apiService.getChatCompletion("Bearer " + API_KEY, request).enqueue(new Callback<SambaNovaResponse>() {
            @Override
            public void onResponse(@NonNull Call<SambaNovaResponse> call,
                    @NonNull Response<SambaNovaResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getChoices().isEmpty()) {
                    String responseText = response.body().getChoices().get(0).getMessage().getContent();
                    runOnUiThread(() -> addMessage(responseText, false));
                } else {
                    Log.e("SambaNova", "Error: " + response.code() + " " + response.message());
                    runOnUiThread(() -> addMessage(
                            "Sorry, I couldn't get a response from SambaNova. Error code: " + response.code(), false));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SambaNovaResponse> call, @NonNull Throwable t) {
                Log.e("SambaNova", "Failure: " + t.getMessage());
                runOnUiThread(() -> addMessage("Error connecting to the service.", false));
            }
        });
    }

    private void addMessage(String message, boolean isUser) {
        messageList.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
}
