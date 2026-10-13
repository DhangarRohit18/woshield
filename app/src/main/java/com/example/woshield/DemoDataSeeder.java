package com.example.woshield;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative Demo Data Seeder for Woshield.
 * Seeds synchronized, interconnected datasets across:
 * 1. User Safety Dashboard (HomePage, Routines, Safe Stops, Check-Ins, Privacy)
 * 2. Family Guardian Dashboard (Wards, Live Telemetry, Safety Circle Alerts)
 * 3. Admin Governance Dashboard (Danger Zone Approvals, Emergency Audit Logs)
 */
public class DemoDataSeeder {
    private static final String TAG = "DemoDataSeeder";

    public static final String DEMO_UID = "demo_woshield_user";
    public static final String PREF_DEMO_MODE = "woshield_demo_mode_active";

    public static boolean isDemoMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_DEMO_MODE, false);
    }

    public static void setDemoMode(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_DEMO_MODE, enabled).apply();
    }

    /**
     * Seeds all interconnected demo data into Firebase Realtime Database and local SharedPreferences.
     */
    public static void seedAll(Context context, boolean showToast, @Nullable Runnable onDone) {
        setDemoMode(context, true);

        // 1. Seed Local Emergency Contacts
        seedLocalEmergencyContacts(context);

        // 2. Determine target UIDs
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        String activeUid = (currentUser != null) ? currentUser.getUid() : DEMO_UID;

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // Seed for active user and also standard demo user ID
        List<String> targetUids = new ArrayList<>();
        targetUids.add(activeUid);
        if (!activeUid.equals(DEMO_UID)) {
            targetUids.add(DEMO_UID);
        }

        for (String uid : targetUids) {
            seedUserData(rootRef, uid);
            seedRoutines(rootRef, uid);
            seedSafePlaces(rootRef, uid);
            seedCheckIns(rootRef, uid);
            seedSharingSessions(rootRef, uid);
            seedFamilyWards(rootRef, uid);
            seedFamilyAlerts(rootRef, uid);
            seedSosLogs(rootRef, uid);
        }

        // Global nodes: LiveLocations & DangerZones
        seedWardLiveLocations(rootRef);
        seedAdminDangerZones(rootRef);

        Log.d(TAG, "All interconnected demo datasets seeded successfully!");
        if (showToast) {
            Toast.makeText(context, "⚡ Interconnected demo dataset seeded! Dashboards synchronized.", Toast.LENGTH_LONG).show();
        }

        if (onDone != null) {
            onDone.run();
        }
    }

    private static void seedLocalEmergencyContacts(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            JSONArray array = new JSONArray();

            JSONObject c1 = new JSONObject();
            c1.put("name", "Priya Sharma (Sister)");
            c1.put("number", "+919876543210");
            array.put(c1);

            JSONObject c2 = new JSONObject();
            c2.put("name", "Rajesh Sharma (Father)");
            c2.put("number", "+919876543211");
            array.put(c2);

            JSONObject c3 = new JSONObject();
            c3.put("name", "Women Helpline (National)");
            c3.put("number", "1091");
            array.put(c3);

            JSONObject c4 = new JSONObject();
            c4.put("name", "Police Emergency");
            c4.put("number", "100");
            array.put(c4);

            prefs.edit()
                    .putString("emergency_contacts", array.toString())
                    .putString("user_name", "Ananya Sharma")
                    .putString("user_phone", "+919876543210")
                    .apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving demo emergency contacts", e);
        }
    }

    private static void seedUserData(DatabaseReference rootRef, String uid) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Ananya Sharma");
        userMap.put("email", "ananya.sharma@woshield.org");
        userMap.put("phone", "+919876543210");
        userMap.put("role", "user");
        userMap.put("safetyStatus", "SAFE");
        userMap.put("batteryPercent", 94);
        userMap.put("lastUpdated", System.currentTimeMillis());

        rootRef.child("Users").child(uid).updateChildren(userMap);
    }

    private static void seedRoutines(DatabaseReference rootRef, String uid) {
        DatabaseReference routinesRef = rootRef.child("Users").child(uid).child("routines");

        Routine r1 = new Routine(
                "routine_metro_commute",
                uid,
                "Evening Metro Commute",
                "COMMUTE",
                "University Campus Gate",
                "Home - Model Colony",
                "18:00",
                "19:30",
                18,
                0,
                19,
                30,
                Arrays.asList("MON", "TUE", "WED", "THU", "FRI"),
                90,
                true
        );

        Routine r2 = new Routine(
                "routine_morning_walk",
                uid,
                "Morning Campus Jog",
                "EXERCISE",
                "Hostel Block A",
                "Sports Complex Track",
                "06:30",
                "07:30",
                6,
                30,
                7,
                30,
                Arrays.asList("MON", "WED", "FRI", "SAT", "SUN"),
                60,
                true
        );

        routinesRef.child(r1.getId()).setValue(r1);
        routinesRef.child(r2.getId()).setValue(r2);
    }

    private static void seedSafePlaces(DatabaseReference rootRef, String uid) {
        DatabaseReference placesRef = rootRef.child("Users").child(uid).child("safePlaces");

        SafePlace p1 = new SafePlace(
                "safe_place_home",
                uid,
                "Home Safe Haven",
                "HOME",
                "Model Colony, Shivajinagar, Pune",
                18.5308,
                73.8475,
                true,
                "Primary Contact: Rajesh Sharma (+919876543211)",
                true
        );

        SafePlace p2 = new SafePlace(
                "safe_place_campus",
                uid,
                "University Women Safety Cell",
                "UNIVERSITY",
                "Ganeshkhind Road, Pune",
                18.5529,
                73.8266,
                true,
                "Campus Security Helpdesk: 1091",
                true
        );

        SafePlace p3 = new SafePlace(
                "safe_place_metro",
                uid,
                "Shivaji Nagar Metro Police Post",
                "POLICE_STATION",
                "Station Concourse Level 1, Pune",
                18.5312,
                73.8510,
                true,
                "Metro Police Desk: 100",
                true
        );

        placesRef.child(p1.getId()).setValue(p1);
        placesRef.child(p2.getId()).setValue(p2);
        placesRef.child(p3.getId()).setValue(p3);
    }

    private static void seedCheckIns(DatabaseReference rootRef, String uid) {
        DatabaseReference checkInsRef = rootRef.child("Users").child(uid).child("checkIns");

        long now = System.currentTimeMillis();

        CheckIn c1 = new CheckIn(
                "checkin_demo_01",
                uid,
                "routine_metro_commute",
                "Evening Metro Commute",
                "",
                "Metro Commute Checkpoint: Shivaji Nagar Metro",
                now + 20 * 60 * 1000L
        );
        c1.setStatus(CheckIn.STATUS_PENDING);

        CheckIn c2 = new CheckIn(
                "checkin_demo_02",
                uid,
                "routine_morning_walk",
                "Morning Campus Jog",
                "",
                "Jogging Checkpoint: University Sports Complex",
                now - 3 * 3600 * 1000L
        );
        c2.setStatus(CheckIn.STATUS_CONFIRMED_SAFE);
        c2.setRespondedAt(now - 3 * 3600 * 1000L - 10 * 60 * 1000L);

        checkInsRef.child(c1.getCheckInId()).setValue(c1);
        checkInsRef.child(c2.getCheckInId()).setValue(c2);
    }

    private static void seedSharingSessions(DatabaseReference rootRef, String uid) {
        DatabaseReference sessionsRef = rootRef.child("Users").child(uid).child("sharing_sessions");

        long now = System.currentTimeMillis();

        SharingSession s1 = new SharingSession(
                "session_demo_priya",
                uid,
                "Evening Commute with Priya",
                Collections.singletonList("ward_priya_sharma"),
                Collections.singletonList("Priya Sharma (Sister)"),
                now + 45 * 60 * 1000L
        );

        SharingSession s2 = new SharingSession(
                "session_demo_rajesh",
                uid,
                "Safeguard Monitoring with Rajesh",
                Collections.singletonList("ward_rajesh_sharma"),
                Collections.singletonList("Rajesh Sharma (Father)"),
                now + 24 * 3600 * 1000L
        );

        sessionsRef.child(s1.getSessionId()).setValue(s1);
        sessionsRef.child(s2.getSessionId()).setValue(s2);
    }

    private static void seedFamilyWards(DatabaseReference rootRef, String uid) {
        DatabaseReference wardsRef = rootRef.child("user_wards").child(uid);

        CirclePermissions perms = new CirclePermissions(true, true, true, true, true, true, true, 0);

        FamilyMember m1 = new FamilyMember(
                "ward_priya_sharma",
                "Priya Sharma (Sister)",
                "priya.sharma@woshield.org",
                "+919876543210",
                perms
        );
        m1.setRelationship("Sister");
        m1.setSafetyStatus(FamilyMember.STATUS_SAFE);
        m1.setStatusDetails("Active Metro Journey in Progress");
        m1.setLatitude(18.5204);
        m1.setLongitude(73.8567);
        m1.setLocationAddress("FC Road, Deccan Gymkhana, Pune");
        m1.setLocationTimestamp(System.currentTimeMillis());
        m1.setSharingActive(true);

        FamilyMember m2 = new FamilyMember(
                "ward_rajesh_sharma",
                "Rajesh Sharma (Father)",
                "rajesh.sharma@woshield.org",
                "+919876543211",
                perms
        );
        m2.setRelationship("Father");
        m2.setSafetyStatus(FamilyMember.STATUS_SAFE);
        m2.setStatusDetails("In Transit - Kothrud Corridor");
        m2.setLatitude(18.5074);
        m2.setLongitude(73.8077);
        m2.setLocationAddress("Kothrud Central, Pune");
        m2.setLocationTimestamp(System.currentTimeMillis() - 180000L);
        m2.setSharingActive(true);

        FamilyMember m3 = new FamilyMember(
                "ward_sunita_sharma",
                "Sunita Sharma (Mother)",
                "sunita.sharma@woshield.org",
                "+919876543212",
                perms
        );
        m3.setRelationship("Mother");
        m3.setSafetyStatus(FamilyMember.STATUS_SAFE);
        m3.setStatusDetails("At Verified Safe Haven: Home");
        m3.setLatitude(18.5308);
        m3.setLongitude(73.8475);
        m3.setLocationAddress("Model Colony Safe Haven, Pune");
        m3.setLocationTimestamp(System.currentTimeMillis() - 600000L);
        m3.setSharingActive(true);

        wardsRef.child(m1.getUid()).setValue(m1);
        wardsRef.child(m2.getUid()).setValue(m2);
        wardsRef.child(m3.getUid()).setValue(m3);
    }

    private static void seedWardLiveLocations(DatabaseReference rootRef) {
        DatabaseReference locRef = rootRef.child("LiveLocations");

        Map<String, Object> l1 = new HashMap<>();
        l1.put("latitude", 18.5204);
        l1.put("longitude", 73.8567);
        l1.put("address", "FC Road, Deccan Gymkhana, Pune");
        l1.put("batteryPercent", 92);
        l1.put("batteryLevel", 92);
        l1.put("speed", 0.0);
        l1.put("timestamp", System.currentTimeMillis());

        Map<String, Object> l2 = new HashMap<>();
        l2.put("latitude", 18.5074);
        l2.put("longitude", 73.8077);
        l2.put("address", "Kothrud Central, Pune");
        l2.put("batteryPercent", 78);
        l2.put("batteryLevel", 78);
        l2.put("speed", 18.2);
        l2.put("timestamp", System.currentTimeMillis() - 180000L);

        Map<String, Object> l3 = new HashMap<>();
        l3.put("latitude", 18.5308);
        l3.put("longitude", 73.8475);
        l3.put("address", "Model Colony Safe Haven, Pune");
        l3.put("batteryPercent", 64);
        l3.put("batteryLevel", 64);
        l3.put("speed", 0.0);
        l3.put("timestamp", System.currentTimeMillis() - 600000L);

        locRef.child("ward_priya_sharma").setValue(l1);
        locRef.child("ward_rajesh_sharma").setValue(l2);
        locRef.child("ward_sunita_sharma").setValue(l3);
    }

    private static void seedFamilyAlerts(DatabaseReference rootRef, String uid) {
        DatabaseReference alertsRef = rootRef.child("family_alerts").child(uid);

        FamilyAlert fa1 = new FamilyAlert(
                "fa_01",
                "ward_priya_sharma",
                "Priya Sharma",
                FamilyAlert.TYPE_ROUTINE_ARRIVAL,
                "Priya confirmed Safe Check-In at College Stop on schedule.",
                18.5204,
                73.8567,
                ""
        );

        FamilyAlert fa2 = new FamilyAlert(
                "fa_02",
                "ward_sunita_sharma",
                "Sunita Sharma",
                FamilyAlert.TYPE_ROUTINE_ARRIVAL,
                "Sunita arrived at Verified Safe Haven: Model Colony.",
                18.5308,
                73.8475,
                ""
        );

        alertsRef.child(fa1.getAlertId()).setValue(fa1);
        alertsRef.child(fa2.getAlertId()).setValue(fa2);
    }

    private static void seedSosLogs(DatabaseReference rootRef, String uid) {
        SOSLog log1 = new SOSLog(
                String.valueOf(System.currentTimeMillis() - 15 * 60 * 1000L),
                "Routine Checkpoint Completed Safely",
                18.5204,
                73.8567,
                ""
        );

        SOSLog log2 = new SOSLog(
                String.valueOf(System.currentTimeMillis() - 24 * 3600 * 1000L),
                "Quick SOS Triggered & Acknowledged by Circle",
                18.5310,
                73.8450,
                ""
        );

        rootRef.child("alerts").child(uid).push().setValue(log1);
        rootRef.child("alerts").child(uid).push().setValue(log2);

        rootRef.child("emergency_logs").push().setValue(log1);
        rootRef.child("emergency_logs").push().setValue(log2);
    }

    private static void seedAdminDangerZones(DatabaseReference rootRef) {
        DatabaseReference dzRef = rootRef.child("DangerZones");

        Dataholder dz1 = new Dataholder(
                "ananya.sharma@woshield.org",
                "Dark alleyway with broken streetlights near Shivaji Nagar underpass",
                "Requires municipal street lighting and police patrol between 8 PM - 6 AM",
                18.5312,
                73.8510
        );
        dz1.setStatus("pending");

        Dataholder dz2 = new Dataholder(
                "priya.sharma@woshield.org",
                "Unpatrolled bus stop behind Hinjawadi Phase 1",
                "Isolated spot with zero security after 9 PM. Recommend women safety booth.",
                18.5910,
                73.7380
        );
        dz2.setStatus("pending");

        Dataholder dz3 = new Dataholder(
                "citizen.alert@woshield.org",
                "FC Road Back Gate construction junction",
                "Patrolled and verified safe zone by local police unit.",
                18.5240,
                73.8410
        );
        dz3.setStatus("approved");

        dzRef.child("zone_demo_01").setValue(dz1);
        dzRef.child("zone_demo_02").setValue(dz2);
        dzRef.child("zone_demo_03").setValue(dz3);
    }

    /**
     * Ensures authentication (via existing session or anonymous sign-in), seeds data, and opens target activity.
     */
    public static void prepareAndLaunch(@NonNull Activity activity, @NonNull Class<?> targetActivityClass) {
        setDemoMode(activity, true);
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            seedAll(activity, false, () -> {
                activity.startActivity(new Intent(activity, targetActivityClass));
            });
        } else {
            // Attempt anonymous authentication to provide valid non-null FirebaseUser
            auth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        seedAll(activity, false, () -> {
                            activity.startActivity(new Intent(activity, targetActivityClass));
                        });
                    });
        }
    }
}
