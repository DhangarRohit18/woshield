package com.example.woshield;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuzzerActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST_CODE = 102;
    private static final int SMS_REQUEST_CODE = 103;

    private FusedLocationProviderClient fusedLocationClient;
    private List<String> emergencyNumbers;
    private Handler handler = new Handler();
    private Runnable alertRunnable;
    private boolean isAlertScheduled = false;

    private ExtendedFloatingActionButton sosButton;
    private View pulseView;
    private TextView tvStatus, tvCountdown, tvEmergencyNumber;
    private ObjectAnimator pulseAnimator;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buzzer);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        emergencyNumbers = new ArrayList<>();
        loadEmergencyContacts();

        sosButton = findViewById(R.id.btn_sos);
        pulseView = findViewById(R.id.pulseView);
        tvStatus = findViewById(R.id.tv_status);
        tvCountdown = findViewById(R.id.tv_countdown);
        tvEmergencyNumber = findViewById(R.id.tv_emergency_number);

        updateContactsDisplay();

        sosButton.setOnClickListener(v -> {
            if (isAlertScheduled) {
                cancelEmergencyAlert();
            } else {
                if (emergencyNumbers.isEmpty()) {
                    Toast.makeText(this, "Please add emergency contacts first!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, EmergencyContactActivity.class));
                } else {
                    promptDelayTime();
                }
            }
        });
        
        startIdlePulse();
    }

    private void loadEmergencyContacts() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String contactsJson = prefs.getString("emergency_contacts", "[]");
        try {
            JSONArray array = new JSONArray(contactsJson);
            emergencyNumbers.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                emergencyNumbers.add(obj.getString("number"));
            }
        } catch (JSONException e) {
            Log.e("Buzzer", "Error loading contacts", e);
        }
    }

    private void updateContactsDisplay() {
        if (emergencyNumbers.isEmpty()) {
            tvEmergencyNumber.setText("No Contacts Added");
        } else {
            tvEmergencyNumber.setText(emergencyNumbers.size() + " Contacts Secured");
        }
    }

    private void promptDelayTime() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Safety Delay");
        builder.setMessage("Set countdown for emergency alert (minutes):");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText("1");
        builder.setView(input);

        builder.setPositiveButton("Set Timer", (dialog, which) -> {
            String delayStr = input.getText().toString();
            if (!delayStr.isEmpty()) {
                int delayMinutes = Integer.parseInt(delayStr);
                scheduleAlert(delayMinutes * 60000L);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void scheduleAlert(long delayMillis) {
        isAlertScheduled = true;
        sosButton.setText("CANCEL");
        tvStatus.setText("ALERT SCHEDULED");
        startActivePulse();

        alertRunnable = this::triggerEmergencyAlert;
        handler.postDelayed(alertRunnable, delayMillis);

        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(delayMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("00:00");
            }
        }.start();
    }

    private void cancelEmergencyAlert() {
        handler.removeCallbacks(alertRunnable);
        if (countDownTimer != null) countDownTimer.cancel();
        isAlertScheduled = false;
        sosButton.setText("SOS");
        tvStatus.setText("READY TO PROTECT");
        tvCountdown.setText("");
        startIdlePulse();
        Toast.makeText(this, "Alert canceled", Toast.LENGTH_SHORT).show();
    }

    public void triggerEmergencyAlert() {
        isAlertScheduled = false;
        sosButton.setText("SOS");
        tvStatus.setText("ALERTS DISPATCHED");
        tvCountdown.setText("");
        startIdlePulse();
        getLastLocationAndSendAlert();
    }

    private void startIdlePulse() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                pulseView,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.2f),
                PropertyValuesHolder.ofFloat("alpha", 0.5f, 0.0f)
        );
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void startActivePulse() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                pulseView,
                PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.5f),
                PropertyValuesHolder.ofFloat("alpha", 0.8f, 0.0f)
        );
        pulseAnimator.setDuration(800);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void getLastLocationAndSendAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String locUrl = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                String message = "EMERGENCY ALERT!\nI am in danger. My real-time location: " + locUrl;
                
                for (String number : emergencyNumbers) {
                    sendSms(number, message);
                }
                Toast.makeText(this, "Emergency alerts sent to " + emergencyNumbers.size() + " contacts.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to get precise location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSms(String number, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);
        } catch (Exception e) {
            Log.e("Buzzer", "SMS failed to: " + number, e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocationAndSendAlert();
        }
    }
}
