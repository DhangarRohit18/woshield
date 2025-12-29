package com.example.woshield;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private List<LatLng> dangerZones = new ArrayList<>();
    private LatLng userLocation;
    private DatabaseReference dangerZoneRef;
    private List<Circle> dangerZoneCircles = new ArrayList<>();
    private Button btnCheckDangerZone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btnCheckDangerZone = findViewById(R.id.btn_check_danger_zone);
        btnCheckDangerZone.setOnClickListener(v -> checkUserInDangerZone());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dangerZoneRef = FirebaseDatabase.getInstance().getReference("DangerZones");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getUserLocation();
        loadDangerZones();

        // Allow user to add new danger zones
        mMap.setOnMapLongClickListener(latLng -> addDangerZone(latLng));
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
            }
        });
    }

    private void loadDangerZones() {
        dangerZoneRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dangerZones.clear();
                for (Circle circle : dangerZoneCircles) {
                    circle.remove();
                }
                dangerZoneCircles.clear();
                for (DataSnapshot zoneSnapshot : snapshot.getChildren()) {
                    double lat = zoneSnapshot.child("latitude").getValue(Double.class);
                    double lng = zoneSnapshot.child("longitude").getValue(Double.class);
                    LatLng location = new LatLng(lat, lng);
                    dangerZones.add(location);
                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(location)
                            .radius(100)
                            .strokeColor(0xFFFF0000)
                            .fillColor(0x44FF0000));
                    dangerZoneCircles.add(circle);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to load danger zones", error.toException());
            }
        });
    }

    private void checkUserInDangerZone() {
        for (LatLng zone : dangerZones) {
            float[] results = new float[1];
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, zone.latitude, zone.longitude, results);
            if (results[0] < 100) {
                Toast.makeText(this, "Warning: You are in a danger zone!", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(this, "You are safe.", Toast.LENGTH_SHORT).show();
    }

    private void addDangerZone(LatLng latLng) {
        String key = dangerZoneRef.push().getKey();
        dangerZoneRef.child(key).setValue(new Dataholder(latLng.latitude, latLng.longitude))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Danger Zone Added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show());
    }

    static class Dataholder {
        public double latitude, longitude;

        public Dataholder() {}
        public Dataholder(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
