package com.example.woshield;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Random;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgProfilePhoto;
    private Button btnChangePhoto, btnSaveProfile;
    private EditText etName, etMobileNumber;
    private String generatedOtp; // Stores the simulated OTP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Find views by ID
        imgProfilePhoto = findViewById(R.id.img_profile_photo);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        etName = findViewById(R.id.et_name);
        etMobileNumber = findViewById(R.id.et_mobile_number);

        // Change photo action: open gallery to pick a new profile image.
        btnChangePhoto.setOnClickListener(v -> openGallery());

        // Save profile and initiate mobile number verification.
        btnSaveProfile.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String mobile = etMobileNumber.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            // Validate mobile: must be exactly 10 digits.
            if (!mobile.matches("\\d{10}")) {
                Toast.makeText(ProfileActivity.this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show();
                return;
            }
            // Simulate sending OTP (for demo, we generate a random 4-digit code).
            generatedOtp = String.valueOf(1000 + new Random().nextInt(9000));
            // In a production app, you would send this OTP via SMS.
            Toast.makeText(ProfileActivity.this, "OTP sent: " + generatedOtp, Toast.LENGTH_LONG).show();
            // Prompt the user to enter the OTP.
            showOtpDialog();
        });
    }

    // Open the gallery for image selection.
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgProfilePhoto.setImageBitmap(bitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Display an OTP verification dialog.
    private void showOtpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setTitle("Mobile Verification");
        final EditText inputOtp = new EditText(ProfileActivity.this);
        inputOtp.setHint("Enter OTP");
        inputOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(inputOtp);

        builder.setPositiveButton("Verify", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredOtp = inputOtp.getText().toString().trim();
                if (enteredOtp.equals(generatedOtp)) {
                    Toast.makeText(ProfileActivity.this, "Mobile Verified and Profile Saved", Toast.LENGTH_SHORT).show();
                    // TODO: Save the profile details to SharedPreferences or your backend.
                } else {
                    Toast.makeText(ProfileActivity.this, "Incorrect OTP. Verification failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
