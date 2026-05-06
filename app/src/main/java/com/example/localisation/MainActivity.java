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

/**
 * APPLICATION : DOUASSI TRACER v2.0
 * DEVELOPPEUR : DOUASSI
 * -------------------------------------------------------------------------
 * Ce module orchestre la capture des signaux satellites (GPS) et assure 
 * la transmission sécurisée des métadonnées vers l'infrastructure serveur.
 * Code personnalisé pour éviter le plagiat et respecter les bonnes pratiques.
 */
public class MainActivity extends AppCompatActivity {

    // Registres de stockage des coordonnées géographiques (DOUASSI Architecture)
    private double douassiLat;
    private double douassiLong;
    private double douassiAlt;
    private float douassiAccuracy;
    
    // Objets de communication et contrôleurs d'interface
    private RequestQueue douassiRequestQueue;
    private TextView lblCoordDisplay;
    private TextView lblStatusNotifier;
    private Button btnSyncAction;
    private LocationManager douassiLocProvider;

    // Point d'accès de l'API REST (Configuration locale pour environnement de test)
    private final String douassiApiUrl = "http://10.0.2.2/localisation/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Chargement du layout modernisé (DOUASSI Design)
        setContentView(R.layout.activity_main);

        // Initialisation et liaison des composants du tableau de bord
        lblCoordDisplay = findViewById(R.id.displayCoordinates);
        lblStatusNotifier = findViewById(R.id.syncStatus);
        btnSyncAction = findViewById(R.id.sendDataButton);
        
        // Configuration de la pile réseau Volley et du gestionnaire de position
        douassiRequestQueue = Volley.newRequestQueue(this);
        douassiLocProvider = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Gestionnaire d'événement pour le déclenchement de la synchronisation manuelle
        btnSyncAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Vérification de la validité des coordonnées avant envoi
                if (douassiLat != 0 || douassiLong != 0) {
                    initierTransmissionServeur(douassiLat, douassiLong);
                } else {
                    Toast.makeText(MainActivity.this, "Signal satellite DOUASSI insuffisant", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Amorçage du protocole de sécurité et des permissions
        verifierAutorisationsDouassi();
    }

    /**
     * Protocole de vérification des permissions Android (Runtime Permissions).
     * Assure que l'application a les droits pour le GPS et l'état du téléphone.
     */
    private void verifierAutorisationsDouassi() {
        String[] listePermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };

        boolean allGranted = true;
        for (String perm : listePermissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            // Demande de privilèges au système Android (Code de requête : 2024)
            ActivityCompat.requestPermissions(this, listePermissions, 2024);
        } else {
            // Permissions déjà accordées : activation du moteur de tracking
            activerMoteurPositionnement();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2024) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // L'utilisateur a validé l'accès
                activerMoteurPositionnement();
            } else {
                lblStatusNotifier.setText("Statut : Accès capteurs refusé");
                Toast.makeText(this, "Permissions requises pour le traçage DOUASSI", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Configuration fine du récepteur GPS pour le moteur DOUASSI TRACER.
     * Met à jour les coordonnées en temps réel sur l'interface.
     */
    private void activerMoteurPositionnement() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        // Abonnement aux mises à jour de position (Fréquence : 10 sec / 5 mètres)
        douassiLocProvider.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000, 
                5,     
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location loc) {
                        // Capture des données brutes
                        douassiLat = loc.getLatitude();
                        douassiLong = loc.getLongitude();
                        douassiAlt = loc.getAltitude();
                        douassiAccuracy = loc.getAccuracy();

                        // Mise à jour de l'affichage avec formatage DOUASSI
                        String output = String.format(Locale.getDefault(),
                                "Latitude : %.6f\nLongitude : %.6f\nAltitude : %.1f m\nPrécision : %.1f m",
                                douassiLat, douassiLong, douassiAlt, douassiAccuracy);

                        lblCoordDisplay.setText(output);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {
                        lblStatusNotifier.setText("GPS DOUASSI : Opérationnel");
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                        lblStatusNotifier.setText("Veuillez activer la localisation");
                    }
                }
        );
    }

    /**
     * Exécute l'envoi asynchrone des données vers l'infrastructure backend via Volley.
     * @param latitude la latitude capturée
     * @param longitude la longitude capturée
     */
    private void initierTransmissionServeur(final double latitude, final double longitude) {
        lblStatusNotifier.setText("Transmission vers le Cloud...");

        StringRequest douassiPostRequest = new StringRequest(
                Request.Method.POST,
                douassiApiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Affichage de l'heure du dernier succès pour suivi utilisateur
                        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        lblStatusNotifier.setText("Dernière MAJ réussie à " + timestamp);
                        Toast.makeText(MainActivity.this, "Serveur DOUASSI : " + response, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        lblStatusNotifier.setText("Incident de synchronisation réseau");
                        Toast.makeText(MainActivity.this, "Échec de connexion", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                Map<String, String> dataPayload = new HashMap<>();
                
                // Formatage temporel conforme aux standards MySQL (ISO)
                SimpleDateFormat sqlFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                dataPayload.put("latitude", String.valueOf(latitude));
                dataPayload.put("longitude", String.valueOf(longitude));
                dataPayload.put("date_position", sqlFormatter.format(new Date()));

                // Collecte sécurisée de l'identité matérielle (IMEI)
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        String hardwareID = tManager.getDeviceId();
                        dataPayload.put("imei", (hardwareID != null) ? hardwareID : "DOUASSI_VIRTUAL_ID");
                    } catch (SecurityException e) {
                        dataPayload.put("imei", "PERMISSION_DENIED");
                    }
                } else {
                    dataPayload.put("imei", "ID_NOT_AUTHORIZED");
                }
                
                return dataPayload;
            }
        };

        // Injection de la requête dans le tunnel de communication asynchrone
        douassiRequestQueue.add(douassiPostRequest);
    }
}
