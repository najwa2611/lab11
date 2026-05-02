package com.example.localisationsmartphone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private TextView tvInfo;
    private LocationManager locationManager;
    private final String insertUrl = "http://172.20.10.2/localisation/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        requestQueue = Volley.newRequestQueue(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 1);
            return;
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                String msg = String.format(Locale.FRANCE, "Latitude: %.6f\nLongitude: %.6f", latitude, longitude);
                tvInfo.setText(msg);
                Toast.makeText(MainActivity.this, "Position detectee", Toast.LENGTH_SHORT).show();

                sendPositionToServer(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                String newStatus;
                if (status == LocationProvider.OUT_OF_SERVICE) {
                    newStatus = "hors service";
                } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                    newStatus = "indisponible";
                } else {
                    newStatus = "disponible";
                }
                tvInfo.setText("Statut " + provider + ": " + newStatus);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Toast.makeText(MainActivity.this, provider + " active", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(MainActivity.this, provider + " desactive", Toast.LENGTH_SHORT).show();
            }
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
            tvInfo.setText("Recherche de position...");
        } catch (SecurityException e) {
            tvInfo.setText("Erreur: permission refusee");
        }
    }

    private void sendPositionToServer(double latitude, double longitude) {
        StringRequest request = new StringRequest(Request.Method.POST, insertUrl,
                response -> {
                    Toast.makeText(MainActivity.this, "Envoye: " + response, Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMsg = "Erreur envoi";
                    if (error.getMessage() != null) {
                        errorMsg += ": " + error.getMessage();
                    }
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE);

                params.put("latitude", String.valueOf(latitude));
                params.put("longitude", String.valueOf(longitude));
                params.put("date_position", sdf.format(new Date()));
                params.put("imei", getDeviceId());

                return params;
            }
        };

        requestQueue.add(request);
    }

    private String getDeviceId() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId != null && !deviceId.isEmpty()) {
            return deviceId;
        }
        return "unknown_device_" + System.currentTimeMillis();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            tvInfo.setText("Permissions refusees");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}