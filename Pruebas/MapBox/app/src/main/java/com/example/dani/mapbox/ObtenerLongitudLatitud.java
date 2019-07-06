package com.example.dani.mapbox;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ObtenerLongitudLatitud extends AppCompatActivity {

    Button gps;
    TextView tvLongitud;
    TextView tvLatitud;

    double longitud;
    double latitud;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_obtener_longitud_latitud );

        gps = (Button) findViewById( R.id.btnUbi );
        tvLongitud = (TextView) findViewById( R.id.tvLongitud );
        tvLatitud = (TextView) findViewById( R.id.tvLatitud );

        validacion();

        gps.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obtenerUbi();



            }
        } );
    }

    private void validacion() {
        int permissionCheck = ContextCompat.checkSelfPermission( ObtenerLongitudLatitud.this,
                Manifest.permission.ACCESS_FINE_LOCATION );

        if (permissionCheck == PackageManager.PERMISSION_DENIED){
            if (ActivityCompat.shouldShowRequestPermissionRationale( this,
                    Manifest.permission.ACCESS_FINE_LOCATION)){

            }else {
                ActivityCompat.requestPermissions( this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);

            }
        }
    }

    private void obtenerUbi() {


        LocationManager locationManager = (LocationManager) ObtenerLongitudLatitud.this.getSystemService( Context.LOCATION_SERVICE );

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                tvLongitud.setText( Double.toString( location.getLongitude() ));
                tvLatitud.setText( Double.toString (location.getLatitude() ));

                longitud = location.getLongitude();
                latitud = location.getLatitude();

                Intent i = new Intent( ObtenerLongitudLatitud.this, MainActivity.class );
                Bundle b = new Bundle();
                b.putDouble("longitud", longitud);
                b.putDouble("latitud", latitud);
                System.out.println( "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" );
                System.out.println(longitud+" "+latitud);

                i.putExtras(b);
                startActivity(i);


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


        int permissionCheck = ContextCompat.checkSelfPermission( ObtenerLongitudLatitud.this,
                Manifest.permission.ACCESS_FINE_LOCATION );


        locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 0, 0, locationListener );
    }
}