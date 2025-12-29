package com.example.woshield;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class EmergencyContactActivity extends AppCompatActivity {

    private EditText etEmergencyName, etEmergencyContact;
    private Button btnSaveContact;
    private String generatedOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contact);

        etEmergencyName = findViewById(R.id.et_emergency_name);
        etEmergencyContact = findViewById(R.id.et_emergency_contact);
        btnSaveContact = findViewById(R.id.btn_save_contact);

        btnSaveContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emergencyName = etEmergencyName.getText().toString().trim();
                String emergencyContact = etEmergencyContact.getText().toString().trim();

                if (emergencyName.isEmpty() || emergencyContact.isEmpty() || !emergencyContact.matches("\\d{10}")) {
                    Toast.makeText(EmergencyContactActivity.this, "Please enter a valid name and 10-digit contact", Toast.LENGTH_SHORT).show();
                    return;
                }

                generatedOtp = String.valueOf(1000 + new Random().nextInt(9000));
                Toast.makeText(EmergencyContactActivity.this, "OTP sent: " + generatedOtp, Toast.LENGTH_LONG).show();
                showOtpDialog(emergencyName, emergencyContact);
            }
        });
    }

    private void showOtpDialog(final String emergencyName, final String emergencyContact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(EmergencyContactActivity.this);
        builder.setTitle("Verify Emergency Contact");

        final EditText inputOtp = new EditText(EmergencyContactActivity.this);
        inputOtp.setHint("Enter OTP");
        inputOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(inputOtp);

        builder.setPositiveButton("Verify", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredOtp = inputOtp.getText().toString().trim();
                if (enteredOtp.equals(generatedOtp)) {
                    saveContact(emergencyName, emergencyContact);
                    Toast.makeText(EmergencyContactActivity.this, "Emergency contact verified and saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(EmergencyContactActivity.this, ContactsActivity.class));
                } else {
                    Toast.makeText(EmergencyContactActivity.this, "Incorrect OTP. Verification failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveContact(String name, String number) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            JSONArray contactsArray = new JSONArray(prefs.getString("emergency_contacts", "[]"));
            JSONObject newContact = new JSONObject();
            newContact.put("name", name);
            newContact.put("number", number);

            contactsArray.put(newContact);
            editor.putString("emergency_contacts", contactsArray.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
