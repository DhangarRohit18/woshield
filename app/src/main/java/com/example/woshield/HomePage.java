package com.example.woshield;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    // Header & User Views
    private TextView tvUserGreeting, tvUserEmail, tvRoleBadge;
    private MaterialCardView cardRoleBadge, btnHeaderProfile;
    private ImageView imgHeaderAvatar;

    // Safety Status Views
    private LinearLayout layoutStatusBadge;
    private ImageView imgStatusIcon;
    private TextView tvStatusTitle, tvStatusDescription, tvContactsCount;
    private SwitchMaterial switchService;

    // SOS Emergency Views
    private MaterialCardView btnEmergency;
    private TextView tvSosStatus;
    private ProgressBar progressSos;
    private boolean isSosProcessing = false;

    // Secondary & Admin Views
    private MaterialCardView btnAdminDashboard;
    private BottomNavigationView bottomNavigationView;

    // Handlers & State
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isHolding = false;
    private boolean holdTriggered = false;
    private ObjectAnimator pulseAnimator;

    private MediaRecorder sosRecorder;
    private String sosAudioPath;
    private long recordingStartTime = 0;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference mDatabase;
    private DatabaseReference userRef;
    private ValueEventListener userProfileListener;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home_page);

        CloudinaryHelper.init(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupUserFirebaseListener(currentUser.getUid());
        setupViews();
        setupSOSButton();
        setupServiceSwitch();
        setupBottomNavigation();
        requestRequiredPermissions();

        checkInitialServiceState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateEmergencyContactsCount();
        updateSafetyStatusUI();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initViews() {
        tvUserGreeting = findViewById(R.id.tv_user_greeting);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvRoleBadge = findViewById(R.id.tv_role_badge);
        cardRoleBadge = findViewById(R.id.card_role_badge);
        btnHeaderProfile = findViewById(R.id.btn_header_profile);
        imgHeaderAvatar = findViewById(R.id.img_header_avatar);

        layoutStatusBadge = findViewById(R.id.layout_status_badge);
        imgStatusIcon = findViewById(R.id.img_status_icon);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusDescription = findViewById(R.id.tv_status_description);
        tvContactsCount = findViewById(R.id.tv_contacts_count);
        switchService = findViewById(R.id.switch_background_service);

        btnEmergency = findViewById(R.id.btn_Emergency_send);
        tvSosStatus = findViewById(R.id.tv_sos_status);
        progressSos = findViewById(R.id.progress_sos);

        btnAdminDashboard = findViewById(R.id.btn_admin_dashboard);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupUserFirebaseListener(String userId) {
        userRef = mDatabase.child("Users").child(userId);
        userProfileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFinishing() && !isDestroyed() && snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String photoUrl = snapshot.child("profilePhotoUrl").getValue(String.class);

                    if (name != null && !name.trim().isEmpty()) {
                        tvUserGreeting.setText("Hello, " + name.trim());
                    } else if (email != null) {
                        String username = email.split("@")[0];
                        tvUserGreeting.setText("Hello, " + username);
                    } else {
                        tvUserGreeting.setText("Welcome to Woshield");
                    }

                    if (email != null && !email.trim().isEmpty()) {
                        tvUserEmail.setText(email);
                    } else {
                        tvUserEmail.setText("Connected to Woshield Shield Network");
                    }

                    if ("admin".equalsIgnoreCase(role)) {
                        tvRoleBadge.setText("ADMIN");
                        tvRoleBadge.setTextColor(ContextCompat.getColor(HomePage.this, R.color.status_warning));
                        cardRoleBadge.setCardBackgroundColor(ContextCompat.getColor(HomePage.this, R.color.status_warning_bg));
                        if (btnAdminDashboard != null) {
                            btnAdminDashboard.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvRoleBadge.setText("PROTECTED");
                        tvRoleBadge.setTextColor(ContextCompat.getColor(HomePage.this, R.color.deep_purple));
                        cardRoleBadge.setCardBackgroundColor(ContextCompat.getColor(HomePage.this, R.color.light_lavender));
                        if (btnAdminDashboard != null) {
                            btnAdminDashboard.setVisibility(View.GONE);
                        }
                    }

                    if (photoUrl != null && !photoUrl.trim().isEmpty() && imgHeaderAvatar != null) {
                        Glide.with(HomePage.this)
                                .load(photoUrl)
                                .circleCrop()
                                .placeholder(R.drawable.profile_icon)
                                .into(imgHeaderAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read user profile: " + error.getMessage());
            }
        };
        userRef.addValueEventListener(userProfileListener);
    }

    private void setupViews() {
        // Header profile shortcut
        if (btnHeaderProfile != null) {
            btnHeaderProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        // Manage circle text action
        View tvManageCircle = findViewById(R.id.tv_manage_circle);
        if (tvManageCircle != null) {
            tvManageCircle.setOnClickListener(v -> startActivity(new Intent(this, EmergencyContactActivity.class)));
        }

        // Daily Routine & Safe Stops
        View btnDailyRoutine = findViewById(R.id.btn_daily_routine);
        if (btnDailyRoutine != null) {
            btnDailyRoutine.setOnClickListener(v -> startActivity(new Intent(this, RoutineActivity.class)));
        }
        View btnHomeOpenRoutines = findViewById(R.id.btn_home_open_routines);
        if (btnHomeOpenRoutines != null) {
            btnHomeOpenRoutines.setOnClickListener(v -> startActivity(new Intent(this, RoutineActivity.class)));
        }
        View btnHomeOpenSafeStops = findViewById(R.id.btn_home_open_safe_stops);
        if (btnHomeOpenSafeStops != null) {
            btnHomeOpenSafeStops.setOnClickListener(v -> startActivity(new Intent(this, SafeStopsActivity.class)));
        }

        // Primary Feature Cards
        findViewById(R.id.btn_security_suggestions).setOnClickListener(v -> startActivity(new Intent(this, SecuritySuggestionsActivity.class)));
        findViewById(R.id.btn_maps).setOnClickListener(v -> startActivity(new Intent(this, MapsActivity.class)));
        findViewById(R.id.btn_add_contacts).setOnClickListener(v -> startActivity(new Intent(this, EmergencyContactActivity.class)));
        
        View btnSosLogs = findViewById(R.id.btn_sos_logs);
        if (btnSosLogs != null) {
            btnSosLogs.setOnClickListener(v -> startActivity(new Intent(this, SOSLogActivity.class)));
        }

        // Secondary Assistance Tools
        findViewById(R.id.btn_buzzer).setOnClickListener(v -> startActivity(new Intent(this, BuzzerActivity.class)));
        findViewById(R.id.btn_fake_call).setOnClickListener(v -> startActivity(new Intent(this, FakeCallActivity.class)));
        findViewById(R.id.btn_voice_help).setOnClickListener(v -> startActivity(new Intent(this, VoiceHelp.class)));
        findViewById(R.id.btn_chatbot).setOnClickListener(v -> startActivity(new Intent(this, ChatBotActivity.class)));
        findViewById(R.id.btn_nearby_safe).setOnClickListener(v -> startActivity(new Intent(this, Nearby_Safe.class)));
        
        View btnWalkWithMe = findViewById(R.id.btn_walk_with_me);
        if (btnWalkWithMe != null) {
            btnWalkWithMe.setOnClickListener(v -> startActivity(new Intent(this, WalkWithMeActivity.class)));
        }

        findViewById(R.id.helpline).setOnClickListener(v -> startActivity(new Intent(this, Helpline.class)));
        
        View btnReportDanger = findViewById(R.id.btn_report_danger);
        if (btnReportDanger != null) {
            btnReportDanger.setOnClickListener(v -> startActivity(new Intent(this, BookActivity.class)));
        }

        if (btnAdminDashboard != null) {
            btnAdminDashboard.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboard.class)));
        }

        // Profile and Logout Actions
        findViewById(R.id.btn_profile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btn_logout).setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        stopSurakshaService();
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already on Home screen
                return true;
            } else if (id == R.id.nav_journey) {
                startActivity(new Intent(this, MapsActivity.class));
                return true;
            } else if (id == R.id.nav_circle) {
                startActivity(new Intent(this, EmergencyContactActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void updateEmergencyContactsCount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String contactsJson = prefs.getString("emergency_contacts", "[]");
        int count = 0;
        try {
            JSONArray array = new JSONArray(contactsJson);
            count = array.length();
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing contacts json: " + e.getMessage());
        }

        if (tvContactsCount != null) {
            if (count == 0) {
                tvContactsCount.setText("No emergency contacts set — tap to add");
            } else if (count == 1) {
                tvContactsCount.setText("1 Emergency Contact Configured");
            } else {
                tvContactsCount.setText(count + " Emergency Contacts Configured");
            }
        }
    }

    private void updateSafetyStatusUI() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isEnabled = prefs.getBoolean("service_enabled", true);

        if (switchService != null) {
            switchService.setChecked(isEnabled);
        }

        if (isEnabled) {
            if (layoutStatusBadge != null) {
                layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_safe_bg)));
            }
            if (imgStatusIcon != null) {
                imgStatusIcon.setImageResource(R.drawable.ic_check_circle);
                imgStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_safe)));
            }
            if (tvStatusTitle != null) {
                tvStatusTitle.setText(R.string.monitoring_active);
                tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_safe));
            }
            if (tvStatusDescription != null) {
                tvStatusDescription.setText(R.string.monitoring_active_desc);
            }
        } else {
            if (layoutStatusBadge != null) {
                layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_warning_bg)));
            }
            if (imgStatusIcon != null) {
                imgStatusIcon.setImageResource(R.drawable.ic_alert_triangle);
                imgStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_warning)));
            }
            if (tvStatusTitle != null) {
                tvStatusTitle.setText(R.string.monitoring_inactive);
                tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_warning));
            }
            if (tvStatusDescription != null) {
                tvStatusDescription.setText(R.string.monitoring_inactive_desc);
            }
        }
    }

    private void setupServiceSwitch() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isEnabled = prefs.getBoolean("service_enabled", true);
        if (switchService != null) {
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
                updateSafetyStatusUI();
            });
        }
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
                    if (isSosProcessing) {
                        Toast.makeText(this, "SOS Alert currently dispatching...", Toast.LENGTH_SHORT).show();
                        return true;
                    }
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

                    if (isSosProcessing) {
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (holdTriggered) {
                            stopSOSRecordingAndProcess();
                        } else {
                            // User clicked/tapped (less than 2 seconds)
                            startSOSProcess(null);
                        }
                    } else if (holdTriggered) {
                        // ACTION_CANCEL - if recording was started, still process evidence
                        stopSOSRecordingAndProcess();
                    } else {
                        tvSosStatus.setText(R.string.sos_tap_hold_hint);
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
        File cacheDir = getExternalCacheDir();
        if (cacheDir == null) cacheDir = getCacheDir();
        sosAudioPath = cacheDir.getAbsolutePath() + "/sos_manual_" + System.currentTimeMillis() + ".m4a";
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
            tvSosStatus.setText("🔴 RECORDING EVIDENCE...");
            Toast.makeText(this, "Recording Voice Evidence...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Recording failed", e);
            sosRecorder = null;
            tvSosStatus.setText("Mic Error — Sending Quick Alert...");
            startSOSProcess(null);
        }
    }

    private void stopSOSRecordingAndProcess() {
        if (sosRecorder != null) {
            try {
                if (System.currentTimeMillis() - recordingStartTime > 1000) {
                    sosRecorder.stop();
                    tvSosStatus.setText("Securing Audio Evidence...");
                    if (progressSos != null) progressSos.setVisibility(View.VISIBLE);
                    uploadAudioToCloudinary();
                } else {
                    deleteFailedRecording();
                    tvSosStatus.setText("Quick Tap Detected");
                    startSOSProcess(null);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Stop failed", e);
                deleteFailedRecording();
                startSOSProcess(null);
            } finally {
                sosRecorder.release();
                sosRecorder = null;
            }
        }
    }

    private void uploadAudioToCloudinary() {
        isSosProcessing = true;
        CloudinaryHelper.uploadAudio(sosAudioPath, new CloudinaryHelper.UploadResponse() {
            @Override
            public void onSuccess(String url) {
                startSOSProcess(url);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Audio upload error: " + error);
                startSOSProcess(null);
            }
        });
    }

    private void startSOSProcess(String audioUrl) {
        isSosProcessing = true;
        runOnUiThread(() -> {
            if (progressSos != null) progressSos.setVisibility(View.VISIBLE);
            tvSosStatus.setText("Acquiring GPS Location...");
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finalizeSOS(0, 0, audioUrl);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        finalizeSOS(location.getLatitude(), location.getLongitude(), audioUrl);
                    } else {
                        // Fallback to getLastLocation
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                finalizeSOS(lastLoc.getLatitude(), lastLoc.getLongitude(), audioUrl);
                            } else {
                                finalizeSOS(0, 0, audioUrl);
                            }
                        }).addOnFailureListener(e -> finalizeSOS(0, 0, audioUrl));
                    }
                })
                .addOnFailureListener(e -> finalizeSOS(0, 0, audioUrl));
    }

    private void finalizeSOS(double lat, double lon, String audioUrl) {
        logSOSWithLocation(lat, lon, audioUrl);
        sendEmergencyAlerts(lat, lon, audioUrl);

        runOnUiThread(() -> {
            if (progressSos != null) progressSos.setVisibility(View.GONE);
            tvSosStatus.setText("✅ SOS ALERT DISPATCHED");
            Toast.makeText(HomePage.this, "Emergency Alert Dispatched Successfully!", Toast.LENGTH_LONG).show();

            // Reset after 4 seconds
            handler.postDelayed(() -> {
                isSosProcessing = false;
                if (!isFinishing() && !isDestroyed()) {
                    tvSosStatus.setText(R.string.sos_tap_hold_hint);
                }
            }, 4000);
        });
    }

    private void logSOSWithLocation(double lat, double lon, String audioUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();
        String timestamp = String.valueOf(System.currentTimeMillis());

        SOSLog log = new SOSLog(timestamp, audioUrl != null ? "Manual SOS with Audio Evidence" : "Quick SOS Triggered", lat, lon, audioUrl != null ? audioUrl : "");

        // 1. Write to user-scoped alerts path: alerts/{userId}
        mDatabase.child("alerts").child(userId).push().setValue(log)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert logged to alerts/" + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Alert log failed", e));

        // 2. Also write to emergency_logs root path for backward compatibility with SOSLogActivity
        mDatabase.child("emergency_logs").push().setValue(log)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert logged to emergency_logs"))
                .addOnFailureListener(e -> Log.e(TAG, "Emergency logs push failed", e));
    }

    private void sendEmergencyAlerts(double lat, double lon, String audioUrl) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String contactsJson = prefs.getString("emergency_contacts", "[]");

        String mapsLink = (lat != 0) ? "https://maps.google.com/?q=" + lat + "," + lon : "Location unavailable (GPS offline)";
        String message = "🚨 WOSHIELD EMERGENCY SOS! 🚨\nI am in danger! Immediate assistance needed!\nLocation: " + mapsLink;
        if (audioUrl != null && !audioUrl.isEmpty()) {
            message += "\nVoice Evidence: " + audioUrl;
        }

        try {
            JSONArray array = new JSONArray(contactsJson);
            if (array.length() == 0) {
                runOnUiThread(() -> Toast.makeText(HomePage.this, "No emergency contacts configured!", Toast.LENGTH_LONG).show());
                return;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject contact = array.getJSONObject(i);
                SmsHelper.sendSMS(this, contact.getString("number"), message);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing contacts for alert", e);
        }
    }

    private void deleteFailedRecording() {
        if (sosAudioPath != null) {
            File file = new File(sosAudioPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btnEmergency,
                PropertyValuesHolder.ofFloat("scaleX", 1.05f),
                PropertyValuesHolder.ofFloat("scaleY", 1.05f));
        pulseAnimator.setDuration(350);
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
            performLogout();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPulseAnimation();
        handler.removeCallbacksAndMessages(null);

        if (sosRecorder != null) {
            try {
                sosRecorder.release();
            } catch (Exception ignored) {}
            sosRecorder = null;
        }

        if (userRef != null && userProfileListener != null) {
            userRef.removeEventListener(userProfileListener);
        }
    }
}
