package com.example.woshield;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;


public class SplashActivity extends AppCompatActivity {

    public static String TAG= SplashActivity.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_splash);


        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
        ImageView splash = findViewById(R.id.splash_iv);


        Glide.with(this)
                .load(R.drawable.gif_splash)

                .into(splash);

        new Handler().postDelayed(new Runnable() {

            public void run() {
                final Intent mainIntent = new Intent(SplashActivity.this, LoginActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                overridePendingTransition(R.anim.fade_in, 0);
                SplashActivity.this.finish();
            }
        }, 3000);


    }

}




