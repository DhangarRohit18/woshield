package com.example.woshield;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomePage extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 200;
    // List of required permissions for the app.
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);


        // Find views by ID.
        ImageView btnSecuritySuggestions = findViewById(R.id.btn_security_suggestions);
        ImageView btnImportantContacts = findViewById(R.id.btn_important_contacts);
        ImageView btnChatBot = findViewById(R.id.btn_chatbot);
        ImageView btnMaps = findViewById(R.id.btn_maps);


        ImageView Emergency=findViewById(R.id.btn_Emergency_send);
        ImageView btnHelpline = findViewById(R.id.helpline);
        ImageView btnAddContacts = findViewById(R.id.action_emergency_contact);
        ImageView btnUserProfile = findViewById(R.id.action_profile);
        ImageView btnLogout = findViewById(R.id.action_logout);
        ImageView btnSafeZone = findViewById(R.id.SafeZone);
        ImageView btnvoiceMsg = findViewById(R.id.voiceMsg);


        // Navigate to Security Suggestions Activity.
        btnSecuritySuggestions.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, SecuritySuggestionsActivity.class);
            startActivity(intent);
        });

        // Navigate to Important Contacts Activity.
        btnImportantContacts.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, ContactsActivity.class);
            startActivity(intent);
        });

        // Navigate to AI Powered Chat Bot Activity.
        btnChatBot.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, ChatBotActivity.class);
            startActivity(intent);
        });

        // Navigate to Maps Activity.
        btnMaps.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, MapsActivity.class);
            startActivity(intent);
        });
        btnSafeZone.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, Nearby_Safe.class);
            startActivity(intent);
        });

        btnvoiceMsg.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, VoiceHelp.class);
            startActivity(intent);
        });
        btnHelpline.setOnClickListener(v -> startActivity(new Intent(HomePage.this, Helpline.class)));
        btnAddContacts.setOnClickListener(v -> startActivity(new Intent(HomePage.this, EmergencyContactActivity.class)));
        btnUserProfile.setOnClickListener(v -> startActivity(new Intent(HomePage.this, ProfileActivity.class)));
        btnLogout.setOnClickListener(v -> startActivity(new Intent(HomePage.this, LoginActivity.class)));
        Emergency.setOnClickListener(v -> startActivity(new Intent(HomePage.this, BuzzerActivity.class)));
        // Buzzle option: both image and button redirect to Buzzer Activity.
        View.OnClickListener buzzleClickListener = v -> {
            Intent intent = new Intent(HomePage.this, BuzzerActivity.class);
            startActivity(intent);
        };
        Emergency.setOnClickListener(buzzleClickListener);
    }
    public void goToFakeCall(View v)
    {
        Intent i=new Intent(HomePage.this, FakeCallActivity.class);
        startActivity(i);
    }

    // Request all required permissions.
    private void requestRequiredPermissions() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
        }
    }

    // Handle the result of the permission request.
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                // Show a non-cancelable dialog requiring the user to grant all permissions.
                new AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("This app requires microphone and location permissions to function properly. Please grant all permissions.")
                        .setCancelable(false)
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestRequiredPermissions();
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            }
        }
    }

    // Called when the ImageView is clicked (if defined via XML onClick attribute).
    public void goToBuzzer(View v) {
        Intent intent = new Intent(HomePage.this, BuzzerActivity.class);
        startActivity(intent);
    }

    // Inflate the toolbar menu.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    // Handle toolbar menu item clicks.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            // Handle logout.
            Intent intent = new Intent(HomePage.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_profile) {
            // Navigate to Profile activity.
            Intent intent = new Intent(HomePage.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_emergency_contact) {
            // Navigate to Emergency Contact setup activity.
            Intent intent = new Intent(HomePage.this, EmergencyContactActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
