package com.example.woshield;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ChatBotActivity extends AppCompatActivity {

    private EditText userInput;
    private TextView chatOutput;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        userInput = findViewById(R.id.userInput);
        chatOutput = findViewById(R.id.chatOutput);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = userInput.getText().toString();
                if (!userMessage.isEmpty()) {
                    chatOutput.append("\nUser: " + userMessage);
                    userInput.setText(""); // Clear input field

                    HuggingFaceService.getChatResponse(userMessage, new HuggingFaceService.ChatbotCallback() {
                        @Override
                        public void onResponse(String response) {
                            runOnUiThread(() -> chatOutput.append("\nBot: " + response));
                        }

                        @Override
                        public void onError(String error) {
                          }
                    });
                }
            }
        });
    }
}
