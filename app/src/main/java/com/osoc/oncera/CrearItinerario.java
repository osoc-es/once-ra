package com.osoc.oncera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;

public class CrearItinerario extends AppCompatActivity implements AdapterView.OnItemSelectedListener, OnLocationUpdatedListener {
    private Button btnCapture;
    private Spinner spin;
    private ImageView imgCapture;
    private ImageView imgIcone;
    private TextView tv_loc;
    private static final int Image_Capture_Code = 1;
    String[] access = { "Rampa", "Bordillo", "Ascensor", "Puerta", "Other"};

    private LocationGooglePlayServicesProvider provider;

    private static final int LOCATION_PERMISSION_ID = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_itinerario);
        btnCapture =(Button)findViewById(R.id.btnTakePicture);
        imgCapture = (ImageView) findViewById(R.id.img_camera);
        imgIcone = (ImageView) findViewById( R.id.img_icon );
        tv_loc = (TextView) findViewById(R.id.tv_loc);
        spin = (Spinner) findViewById(R.id.spin_access);
        spin.setOnItemSelectedListener(this);

        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item,access);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(aa);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cInt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cInt,Image_Capture_Code);
                if (ContextCompat.checkSelfPermission(CrearItinerario.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CrearItinerario.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_ID);
                    return;
                }
                startLocation();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        showLast();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Image_Capture_Code) {
            if (resultCode == RESULT_OK) {
                Bitmap bp = (Bitmap) data.getExtras().get("data");
                imgCapture.setImageBitmap(bp);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            }
        }
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        //Toast.makeText(getApplicationContext(),access[position] , Toast.LENGTH_LONG).show();
        if (access[position] == "Rampa") {
            imgIcone.setImageResource(R.drawable.rampa);
        }
        else if (access[position] == "Bordillo") {
            imgIcone.setImageResource(R.drawable.bordillo);
        }
        else if (access[position] == "Ascensor") {
            imgIcone.setImageResource(R.drawable.ascensor);
        }
        else if (access[position] == "Puerta") {
            imgIcone.setImageResource(R.drawable.puerta);
        }
        else if (access[position] == "Other") {
            imgIcone.setImageResource(R.drawable.otro);
        }

    }
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocation();
        }
    }

    private void showLast() {
        Location lastLocation = SmartLocation.with(this).location().getLastLocation();
        if (lastLocation != null) {
            tv_loc.setText(
                    String.format("[From Cache] Latitude %.6f, Longitude %.6f",
                            lastLocation.getLatitude(),
                            lastLocation.getLongitude())
            );
        }
    }


    private void startLocation() {

        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();

        smartLocation.location(provider).start(this);

    }

    private void stopLocation() {
        SmartLocation.with(this).location().stop();

        SmartLocation.with(this).activity().stop();
    }

    private void showLocation(Location location) {
        if (location != null) {
            final String text = String.format("Latitude %.6f, Longitude %.6f",
                    location.getLatitude(),
                    location.getLongitude());
            tv_loc.setText(text);
            stopLocation();
        } else {
            tv_loc.setText("Null location");
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        showLocation(location);
    }
}