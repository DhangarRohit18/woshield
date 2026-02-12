package com.example.woshield;

import android.app.Application;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

public class WoShieldApplication extends Application implements OnMapsSdkInitializedCallback {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Maps SDK early to avoid main thread blocking
        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this);
    }

    @Override
    public void onMapsSdkInitialized(MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                android.util.Log.d("MapsDemo", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                android.util.Log.d("MapsDemo", "The legacy version of the renderer is used.");
                break;
        }
    }
}
