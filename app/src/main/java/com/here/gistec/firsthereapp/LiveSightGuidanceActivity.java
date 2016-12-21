package com.here.gistec.firsthereapp;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapBuildingGroup;
import com.here.android.mpa.mapping.MapBuildingObject;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.UrlMapRasterTileSourceBase;


public class LiveSightGuidanceActivity extends Activity {

    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // map embedded in the map fragment
    private Map map = null;

  // private MapBuildingGroup buildingGroup = null;

    private LiveMapRasterTileSource tileSource;
    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }

    private void initialize() {
        setContentView(R.layout.activity_main);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center coordinate to the San Francisco region (no animation)
                    map.setCenter(new GeoCoordinate(24.933552, 55.242723, 0.0),
                            Map.Animation.NONE);
                    // Set the map zoom level to close to street level
                    map.setZoomLevel(11);

                    // Listen for gesture events. For example tapping on buildings
                    mapFragment.getMapGesture().addOnGestureListener(gestureListener);

                    tileSource = new LiveMapRasterTileSource();

                    map.addRasterTileSource(tileSource);


                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });
    }

    /**
     * Checks the dynamically controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private MapGesture.OnGestureListener gestureListener = new MapGesture.OnGestureListener.OnGestureListenerAdapter() {

        @Override
        public void onMultiFingerManipulationEnd() {
            Toast.makeText(LiveSightGuidanceActivity.this, "ZoomLevel = " + map.getZoomLevel(), Toast.LENGTH_LONG).show();
            super.onMultiFingerManipulationEnd();
        }

    };


    public class LiveMapRasterTileSource extends UrlMapRasterTileSourceBase {

        private final static String URL_FORMAT =
                "http://tile.openstreetmap.com/{z}/{x}/{y}.png";

               // "https://1.aerial.maps.cit.api.here.com/maptile/2.1/maptile/newest/terrain.day/{z}/{x}/{y}/256/png8?app_id=lyTqc8CuKF6pEZ6K6WRD&app_code=dfHUzP9gUTxaaJw3rpe2nQ";

        public LiveMapRasterTileSource() {
            // We want the tiles placed over everything else
            setOverlayType(MapOverlayType.FOREGROUND_OVERLAY);
            // We don't want the map visible beneath the tiles
            setTransparency(Transparency.OFF);
            // We don't want the tiles visible between these zoom levels
            hideAtZoomRange(12, 20);
            // Do not cache tiles
            setCachingEnabled(false);
        }

        // Implementation of UrlMapRasterTileSourceBase
        public String getUrl(int x, int y, int zoomLevel) {
            String url = URL_FORMAT;

             url = url.replace("{z}", "" + zoomLevel);
             url = url.replace("{x}", "" + x);
             url = url.replace("{y}", "" + y);

            return url;
        }
    }
}
