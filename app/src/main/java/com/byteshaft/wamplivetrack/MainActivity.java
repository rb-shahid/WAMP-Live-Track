package com.byteshaft.wamplivetrack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Session mWAMPSession;
    private TextView mStatusText;
    private TextView mSpeedText;

    private boolean onTouchEventOccured;

    private GoogleMap mMap;
    private int reasonCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mStatusText = findViewById(R.id.status_text);
        mSpeedText = findViewById(R.id.speed_text);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectToServerAndPublishLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void connectToServerAndPublishLocation() {
        mWAMPSession = new Session();
        mStatusText.setText("Status: " + "Connecting...");
        mStatusText.setTextColor(Color.BLUE);
        mWAMPSession.addOnJoinListener((session, details) -> {
            mStatusText.setText("Status: " + "Connected");
            mStatusText.setTextColor(Color.GREEN);
            getLocationFromServer();
        });
        Client wampClient = new Client(mWAMPSession, "ws://51.15.107.119:8080/ws", "realm1");
        wampClient.connect().whenComplete((exitInfo, throwable) -> stopLocationUpdates());
    }

    private BitmapDescriptor vectorToBitmap(@DrawableRes int id) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void getLocationFromServer() {
        mWAMPSession.subscribe("io.crossbar.location", this::onLocation, CustomLocation.class);
    }

    private void onLocation(CustomLocation location) {
        LatLng current = new LatLng(location.lat, location.lon);
        mSpeedText.setText(String.format("Speed: %s", location.speed));
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .icon(vectorToBitmap(R.drawable.bike))
                .position(current).title("Marker Label")
                .snippet("Marker Description"));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(current).zoom(16).build();
        if (!onTouchEventOccured) {
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

    }

    private void stopLocationUpdates() {
        if (mWAMPSession.isConnected()) {
            mWAMPSession.leave();
        }
        mStatusText.setText("Disconnected");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(latLng -> {
            // Handle onMapClick Action
        });

        mMap.setOnCameraMoveStartedListener(i -> {
            if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                onTouchEventOccured = true;
            }
        });
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

}
