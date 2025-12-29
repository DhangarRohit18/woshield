package com.example.woshield;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class FakeCallActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private ImageView sirenPlayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        sirenPlayButton = findViewById(R.id.siren_play);
        mediaPlayer = MediaPlayer.create(this, R.raw.siren); // Ensure the sound file exists in res/raw

        sirenPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        sirenPlayButton.setImageResource(R.drawable.siren_play); // Change icon if needed
                    } else {
                        mediaPlayer.start();
                        sirenPlayButton.setImageResource(R.drawable.siren_stop); // Change icon if needed
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}