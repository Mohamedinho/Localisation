package com.example.localisation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;
    private RequestQueue requestQueue;
    private TextView tvInfo;
    private Button btnSend;
    private LocationManager locationManager;

    // Utilisation de 10.0.2.2 pour accéder au localhost depuis l'émulateur
    private String insertUrl = "http://10.0.2.2/localisation/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        btnSend = findViewById(R.id.btnSend);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (latitude != 0 || longitude != 0) {
                    addPosition(latitude, longitude);
                } else {
                    Toast.makeText(MainActivity.this, "Aucune position à envoyer", Toast.LENGTH_SHORT).show();
                }
            }
        });

        checkPermissions();
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, 1);
        } else {
            initLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initLocation();
            } else {
                Toast.makeText(this, "Permissions refusées. L'application ne peut pas fonctionner.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000, // 10 secondes
                10,    // 10 mètres
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        altitude = location.getAltitude();
                        accuracy = location.getAccuracy();

                        String msg = "Latitude : " + latitude
                                + "\nLongitude : " + longitude
                                + "\nAltitude : " + altitude
                                + "\nPrécision : " + accuracy + " m";

                        tvInfo.setText(msg);
                        // On n'envoie plus automatiquement pour laisser l'utilisateur cliquer sur le bouton
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {
                        Toast.makeText(MainActivity.this, "GPS Activé", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        Toast.makeText(MainActivity.this, "Veuillez activer le GPS", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                insertUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getApplicationContext(), "Serveur: " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Erreur réseau: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date_position", sdf.format(new Date()));

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        String deviceId = telephonyManager.getDeviceId();
                        params.put("imei", deviceId != null ? deviceId : "SIMULATOR_ID");
                    } catch (SecurityException e) {
                        params.put("imei", "denied");
                    }
                } else {
                    params.put("imei", "unknown");
                }
                return params;
            }
        };
        requestQueue.add(request);
    }
}
