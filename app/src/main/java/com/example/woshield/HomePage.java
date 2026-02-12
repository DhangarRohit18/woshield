package com.example.woshield;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";
    private static final int PERMISSIONS_REQUEST_CODE = 200;
    private static final int BACKGROUND_LOCATION_PERMISSION_CODE = 201;
    private static final long HOLD_THRESHOLD = 2000;

    private MaterialCardView btnEmergency;
    private TextView tvSosStatus;
    private SwitchMaterial switchService;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isHolding = false;
    private boolean holdTriggered = false;
    private ObjectAnimator pulseAnimator;
    
    private MediaRecorder sosRecorder;
    private String sosAudioPath;
    private long recordingStartTime = 0;
    
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        CloudinaryHelper.init(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnEmergency = findViewById(R.id.btn_Emergency_send);
        tvSosStatus = findViewById(R.id.tv_sos_status);
        switchService = findViewById(R.id.switch_background_service);
        
        setupViews();
        setupSOSButton();
        setupServiceSwitch();
        requestRequiredPermissions();
        
        checkInitialServiceState();
    }

    private void setupViews() {
        findViewById(R.id.btn_buzzer).setOnClickListener(v -> startActivity(new Intent(this, BuzzerActivity.class)));
        findViewById(R.id.btn_security_suggestions).setOnClickListener(v -> startActivity(new Intent(this, SecuritySuggestionsActivity.class)));
        findViewById(R.id.btn_chatbot).setOnClickListener(v -> startActivity(new Intent(this, ChatBotActivity.class)));
        findViewById(R.id.btn_maps).setOnClickListener(v -> startActivity(new Intent(this, BookActivity.class)));
        findViewById(R.id.helpline).setOnClickListener(v -> startActivity(new Intent(this, Helpline.class)));
        findViewById(R.id.btn_fake_call).setOnClickListener(v -> startActivity(new Intent(this, FakeCallActivity.class)));
        findViewById(R.id.btn_voice_help).setOnClickListener(v -> startActivity(new Intent(this, VoiceHelp.class)));
        findViewById(R.id.btn_nearby_safe).setOnClickListener(v -> startActivity(new Intent(this, Nearby_Safe.class)));
        
        findViewById(R.id.btn_add_contacts).setOnClickListener(v -> startActivity(new Intent(this, EmergencyContactActivity.class)));
        findViewById(R.id.btn_profile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            stopSurakshaService();
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupServiceSwitch() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isEnabled = prefs.getBoolean("service_enabled", true);
        switchService.setChecked(isEnabled);

        switchService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("service_enabled", isChecked).apply();
            if (isChecked) {
                startSurakshaService();
                Toast.makeText(this, "Voice Shield Activated", Toast.LENGTH_SHORT).show();
            } else {
                stopSurakshaService();
                Toast.makeText(this, "Voice Shield Deactivated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkInitialServiceState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("service_enabled", true)) {
            startSurakshaService();
        }
    }

    private void startSurakshaService() {
        Intent serviceIntent = new Intent(this, SurakshaService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopSurakshaService() {
        Intent serviceIntent = new Intent(this, SurakshaService.class);
        stopService(serviceIntent);
    }

    private void setupSOSButton() {
        btnEmergency.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    holdTriggered = false;
                    handler.postDelayed(holdRunnable, HOLD_THRESHOLD);
                    startPulseAnimation();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isHolding = false;
                    handler.removeCallbacks(holdRunnable);
                    stopPulseAnimation();

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (holdTriggered) {
                            stopSOSRecordingAndProcess();
                        } else {
                            // User clicked/tapped (less than 2 seconds)
                            tvSosStatus.setText("Sending quick alert...");
                            startSOSProcess(null);
                        }
                    } else if (holdTriggered) {
                        // ACTION_CANCEL - if we were recording, still process it
                        stopSOSRecordingAndProcess();
                    } else {
                        tvSosStatus.setText("Tap for Quick SOS | Hold to Record");
                    }
                    return true;
            }
            return false;
        });
    }

    private final Runnable holdRunnable = new Runnable() {
        @Override
        public void run() { 
            if (isHolding) {
                holdTriggered = true;
                startHighQualityRecording();
            }
        }
    };

    private void startHighQualityRecording() {
        sosAudioPath = getExternalCacheDir().getAbsolutePath() + "/sos_manual_" + System.currentTimeMillis() + ".m4a";
        sosRecorder = new MediaRecorder();
        sosRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        sosRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        sosRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        sosRecorder.setAudioEncodingBitRate(128000);
        sosRecorder.setAudioSamplingRate(44100);
        sosRecorder.setOutputFile(sosAudioPath);
        
        try {
            sosRecorder.prepare();
            sosRecorder.start();
            recordingStartTime = System.currentTimeMillis();
            tvSosStatus.setText("🔴 RECORDING SOS...");
            Toast.makeText(this, "SOS RECORDING STARTED!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) { 
            Log.e(TAG, "Recording failed", e);
            sosRecorder = null;
            tvSosStatus.setText("Mic Error");
        }
    }

    private void stopSOSRecordingAndProcess() {
        if (sosRecorder != null) {
            try {
                if (System.currentTimeMillis() - recordingStartTime > 1000) {
                    sosRecorder.stop();
                    tvSosStatus.setText("Securing Evidence...");
                    uploadAudioToCloudinary();
                } else {
                    deleteFailedRecording();
                    tvSosStatus.setText("Hold longer to record");
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Stop failed", e);
                deleteFailedRecording();
                tvSosStatus.setText("Tap for Quick SOS | Hold to Record");
            } finally {
                sosRecorder.release();
                sosRecorder = null;
            }
        }
    }

    private void uploadAudioToCloudinary() {
        CloudinaryHelper.uploadAudio(sosAudioPath, new CloudinaryHelper.UploadResponse() {
            @Override
            public void onSuccess(String url) {
                startSOSProcess(url);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Upload error: " + error);
                startSOSProcess(null);
            }
        });
    }

    private void startSOSProcess(String audioUrl) {
        runOnUiThread(() -> tvSosStatus.setText("Fetching Location..."));
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finalizeSOS(0, 0, audioUrl);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        finalizeSOS(location.getLatitude(), location.getLongitude(), audioUrl);
                    } else {
                        finalizeSOS(0, 0, audioUrl);
                    }
                })
                .addOnFailureListener(e -> finalizeSOS(0, 0, audioUrl));
    }

    private void finalizeSOS(double lat, double lon, String audioUrl) {
        logSOSWithLocation(lat, lon, audioUrl);
        sendEmergencyAlerts(lat, lon, audioUrl);
        
        runOnUiThread(() -> {
            tvSosStatus.setText("✅ SOS ALERT SENT");
            Toast.makeText(HomePage.this, "Emergency Alert Dispatched!", Toast.LENGTH_LONG).show();
        });
    }

    private void logSOSWithLocation(double lat, double lon, String audioUrl) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        SOSLog log = new SOSLog(timestamp, audioUrl != null ? "Manual SOS with Audio" : "Quick SOS Triggered", lat, lon, audioUrl != null ? audioUrl : "");
        mDatabase.child("alerts").child(userId).push().setValue(log)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert logged in Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Firebase log failed", e));
    }

    private void sendEmergencyAlerts(double lat, double lon, String audioUrl) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String contactsJson = prefs.getString("emergency_contacts", "[]");
        
        String mapsLink = (lat != 0) ? "https://maps.google.com/?q=" + lat + "," + lon : "Location unavailable";
        String message = "🚨 EMERGENCY SOS! 🚨\nI am in danger! Help needed!\nLocation: " + mapsLink;
        if (audioUrl != null) {
            message += "\nVoice Evidence: " + audioUrl;
        }

        try {
            JSONArray array = new JSONArray(contactsJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject contact = array.getJSONObject(i);
                SmsHelper.sendSMS(this, contact.getString("number"), message);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing contacts", e);
        }
    }

    private void deleteFailedRecording() {
        if (sosAudioPath != null) {
            File file = new File(sosAudioPath);
            if (file.exists()) file.delete();
        }
    }

    private void startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btnEmergency,
                PropertyValuesHolder.ofFloat("scaleX", 1.15f),
                PropertyValuesHolder.ofFloat("scaleY", 1.15f));
        pulseAnimator.setDuration(400);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            btnEmergency.setScaleX(1f);
            btnEmergency.setScaleY(1f);
        }
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            requestBackgroundLocationPermission();
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            requestBackgroundLocationPermission();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            stopSurakshaService();
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_emergency_contact) {
            startActivity(new Intent(this, EmergencyContactActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
