package com.osoc.oncera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osoc.oncera.javabean.Rampas;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MedirInclinacion extends AppCompatActivity {

    SensorManager sManager;

    private TextView pitch_text;
    private Button button;
    private ImageButton exit_button;

    private boolean barandilla;

    private DecimalFormat df = new DecimalFormat("#0.00º");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medir_inclinacion);

        getDBValues();

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        pitch_text = (TextView) findViewById(R.id.pitch);
        button = (Button) findViewById(R.id.BotonInclinacionMedir);
        exit_button = (ImageButton) findViewById(R.id.BotonSalir);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Medir();
            }
        });

        exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        _rampa = (Rampas) getIntent().getExtras().get("rampaIntermedio");
        barandilla = getIntent().getBooleanExtra("barandilla",false);

    }

    @Override
    protected void onResume() {
        super.onResume();

        sManager.registerListener(mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }


    // Gravity rotational data
    private float gravity[];
    // Magnetic rotational data
    private float magnetic[]; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];

    // azimuth, pitch and roll
    private float azimuth;
    private float pitch;
    private float roll;

    private class casos {

        public casos(float longitud, float pendiente, float minLong, float maxLong) {
            this.longitud = longitud;
            this.pendiente = pendiente;
            this.minLong = minLong;
            this.maxLong = maxLong;
        }

        public float getLongitud() {
            return longitud;
        }

        public float getPendiente() {
            return pendiente;
        }

        public float getMinLong() {
            return minLong;
        }

        public float getMaxLong() {
            return maxLong;
        }

        private float longitud, pendiente, minLong, maxLong;

    }

    private casos rampa1, rampa2, rampa3;
    private float paramAnch;
    private float minParamPomo, maxParamPomo;
    private Rampas _rampa;




    private SensorEventListener mySensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
            }

            if (mags != null && accels != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X, SensorManager.AXIS_Y, outGravity);
                SensorManager.getOrientation(outGravity, values);

                azimuth = values[0] * 57.2957795f;
                pitch = Math.abs(values[1] * 57.2957795f);
                roll = values[2] * 57.2957795f;
                mags = null;
                accels = null;
            }
        }
    };

    private void Medir() {
        pitch_text.setText(df.format(pitch));
        _rampa.setPendiente(pitch);

       evaluate();
    }

    private void getDBValues() {
        final DatabaseReference anchDB = FirebaseDatabase.getInstance().getReference("Estandares/Rampas/Anchura");

        anchDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                paramAnch = dataSnapshot.getValue(Float.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        final DatabaseReference cas1 = FirebaseDatabase.getInstance().getReference("Estandares/Rampas/Caso1");

        cas1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Float> data = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    data.add(snapshot.getValue(Float.class));
                }
                rampa1 = new casos(data.get(0), data.get(1), -1, -1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference cas2 = FirebaseDatabase.getInstance().getReference("Estandares/Rampas/Caso2");

        cas2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Float> data = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    data.add(snapshot.getValue(Float.class));
                }
                rampa2 = new casos(-1, data.get(0), data.get(1), data.get(2));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference cas3 = FirebaseDatabase.getInstance().getReference("Estandares/Rampas/Caso3");

        cas3.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Float> data = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    data.add(snapshot.getValue(Float.class));
                }
                rampa3 = new casos(-1, data.get(0), data.get(1), data.get(2));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        final DatabaseReference minPomo = FirebaseDatabase.getInstance().getReference("Estandares/Rampas/minAlturaPasamanos");

        minPomo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                minParamPomo = dataSnapshot.getValue(Float.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference maxPomo = FirebaseDatabase.getInstance().getReference("Estandares/Rampas/maxAlturaPasamanos");

        maxPomo.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                maxParamPomo = dataSnapshot.getValue(Float.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void evaluate() {
       boolean cumpleAnch = Evaluator.IsGreaterThan(_rampa.getAnchura(), paramAnch);

        casos aux;

        float lng = _rampa.getLongitud();

        if(lng < rampa1.getLongitud()) aux = rampa1;
        else if(Evaluator.IsInRange(lng,rampa2.getMinLong(), rampa2.getMaxLong())) aux = rampa2;
        else aux = rampa3;

        boolean cumpleIncl = Evaluator.IsLowerThan(_rampa.getPendiente(),aux.getPendiente());

        boolean cumplePasamanos;

        if(barandilla)
            cumplePasamanos = Evaluator.IsInRange(_rampa.getAlturaPasamanosSuperior(),minParamPomo,maxParamPomo);

        else cumplePasamanos = true;

        _rampa.setAccesible(cumpleAnch && cumpleIncl && cumplePasamanos);

        Intent i = new Intent(this,AxesibilityActivity.class);
        i.putExtra(TypesManager.OBS_TYPE,TypesManager.obsType.RAMPAS.getValue());
        i.putExtra(TypesManager.RAMPA_OBS, _rampa);

        startActivity(i);
        finish();



    }

}