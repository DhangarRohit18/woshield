package com.example.woshield;

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class BuzzerActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST_CODE = 102;
    private static final int SMS_REQUEST_CODE = 103;

    private FusedLocationProviderClient fusedLocationClient;
    private String emergencyNumber = "+919975819201";  // Default fallback
    private Handler handler = new Handler();
    private Runnable alertRunnable;
    private boolean isAlertScheduled = false;  // Track if an alert is scheduled

    private Button buzzerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load the emergency contact
        loadEmergencyContact();

        // Find UI components
        buzzerButton = findViewById(R.id.shareButton);

        // Set click listener for buzzer button
        buzzerButton.setOnClickListener(v -> {
            if (isAlertScheduled) {
                cancelEmergencyAlert();
            } else {
                promptDelayTime();
            }
        });
    }

    // Load the emergency contact from SharedPreferences
    private void loadEmergencyContact() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        emergencyNumber = prefs.getString("emergency_contact", "+919975819201" +
                "");
    }

    // Prompt the user to enter delay time
    private void promptDelayTime() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BuzzerActivity.this);
        builder.setTitle("Set Delay Time (Minutes)");

        final EditText input = new EditText(BuzzerActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String delayStr = input.getText().toString();
                if (!delayStr.isEmpty()) {
                    int delayMinutes = Integer.parseInt(delayStr);
                    scheduleAlert(delayMinutes * 60000);
                    Toast.makeText(BuzzerActivity.this,
                            "Alert will be sent in " + delayMinutes + " minutes", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BuzzerActivity.this,
                            "Please enter a valid delay time", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Schedule the emergency alert
    private void scheduleAlert(long delayMillis) {
        isAlertScheduled = true;
        buzzerButton.setText("Cancel Alert");

        alertRunnable = new Runnable() {
            @Override
            public void run() {
                triggerEmergencyAlert();
            }
        };

        handler.postDelayed(alertRunnable, delayMillis);
    }

    // Cancel the scheduled emergency alert
    private void cancelEmergencyAlert() {
        handler.removeCallbacks(alertRunnable);
        isAlertScheduled = false;
        buzzerButton.setText("Press Buzzer");
        Toast.makeText(this, "Emergency alert canceled", Toast.LENGTH_SHORT).show();
    }

    // Trigger the emergency alert when time is up
    public void triggerEmergencyAlert() {
        isAlertScheduled = false;
        buzzerButton.setText("Press Buzzer");

        // Get location and send emergency alert
        getLastLocationAndSendAlert();
    }

    // Get user's last known location and send an emergency message
    private void getLastLocationAndSendAlert() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    String message = "EMERGENCY ALERT!\nI am in danger. My location: " +
                            "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                    sendEmergencyMessage(emergencyNumber, message);
                } else {
                    Toast.makeText(BuzzerActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Send an emergency SMS
    private void sendEmergencyMessage(String number, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_REQUEST_CODE);
        } else {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0,
                        new Intent("SMS_SENT"),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                smsManager.sendTextMessage(number, null, message, sentIntent, null);
                Toast.makeText(this, "Emergency message sent!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("BuzzerActivity", "Failed to send message", e);
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndSendAlert();
            } else {
                Toast.makeText(this, "Location permission denied!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SMS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndSendAlert();
            } else {
                Toast.makeText(this, "SMS permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}