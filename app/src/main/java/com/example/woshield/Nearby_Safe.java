package com.example.woshield;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;

import java.util.Arrays;
import java.util.List;

public class Nearby_Safe extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private Button btnFindSafePlaces;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final String TAG = "Nearby_Safe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_safe);

        mapView = findViewById(R.id.mapView);
        btnFindSafePlaces = findViewById(R.id.btnFindSafePlaces);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize Places with the API key from Manifest
        try {
            Bundle metaData = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            String apiKey = metaData.getString("com.google.android.geo.API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                Places.initialize(getApplicationContext(), apiKey);
            } else {
                Log.e(TAG, "API Key not found in Manifest");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        }

        placesClient = Places.createClient(this);

        Bundle mapViewBundle = savedInstanceState != null ? savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY) : null;
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        btnFindSafePlaces.setOnClickListener(v -> fetchNearbySafePlaces());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        googleMap.setMyLocationEnabled(true);
        getLastKnownLocation();
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                }
            });
        }
    }

    private void fetchNearbySafePlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show();
            return;
        }

        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(Arrays.asList(
                Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES
        ));

        Task<FindCurrentPlaceResponse> placeResponseTask = placesClient.findCurrentPlace(request);

        placeResponseTask.addOnSuccessListener(response -> {
            googleMap.clear();
            for (PlaceLikelihood likelihood : response.getPlaceLikelihoods()) {
                Place place = likelihood.getPlace();
                if (place.getLatLng() != null && isSafePlace(place)) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName()));
                }
            }
        }).addOnFailureListener(exception -> {
            Toast.makeText(this, "Error fetching places: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error fetching places", exception);
        });
    }

    private boolean isSafePlace(Place place) {
        List<Place.Type> safePlaceTypes = Arrays.asList(
                Place.Type.HOSPITAL,
                Place.Type.POLICE,
                Place.Type.LOCAL_GOVERNMENT_OFFICE);

        return place.getTypes() != null && place.getTypes().stream().anyMatch(safePlaceTypes::contains);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }

    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override
    protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override
    protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
