package com.osoc.oncera;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.osoc.oncera.adapters.ImageTitleAdapter;
import com.osoc.oncera.javabean.Puerta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan2;

public class MeasureActivity extends AppCompatActivity {

    private static final String TAG = MeasureActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private float upDistance = 0f;
    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private Anchor myanchor;
    private AnchorNode myanchornode;
    private DecimalFormat form_numbers = new DecimalFormat("#0.00");

    private Anchor anchor1 = null, anchor2 = null;

    private HitResult myhit;

    private String[] tipoPuerta = new String[]{"Giratoria", "Corredera", "Abatible", "Tornos"};
    private String[] tipoMecanismo = new String[]{"Manibela", "Pomo", "Barra", "Agarrador"};
    private boolean measure_height = false;

    private float paramAltura,paramAnchura,minMecApertura,maxMecApertura;

    private Puerta puerta = new Puerta(-1, -1, null, -1, null, null, null, null);

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_measure);

        paramAltura = GetDataFromDatabase.FloatData("Estandares/Puertas/Altura");
        paramAnchura = GetDataFromDatabase.FloatData("Estandares/Puertas/Anchura");

        paramAltura = GetDataFromDatabase.FloatData("Estandares/Puertas/Altura");
        minMecApertura = GetDataFromDatabase.FloatData("Estandares/Puertas/minMecApertura");
        maxMecApertura = GetDataFromDatabase.FloatData("Estandares/Puertas/maxMecApertura");

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        Button restart = (Button) findViewById(R.id.btn_restart);
        Button confirm = (Button) findViewById(R.id.btn_ok);
        TextView data = (TextView) findViewById(R.id.tv_distance);
        TextView width = (TextView) findViewById(R.id.width);
        TextView mechanism = (TextView) findViewById(R.id.height_mecha);
        TextView height = (TextView) findViewById(R.id.height);
        SeekBar z_axis = (SeekBar) findViewById(R.id.z_axis);
        List<AnchorNode> anchorNodes = new ArrayList<>();

        z_axis.setEnabled(false);
        confirm.setEnabled(false);


        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anchor1 = null;
                anchor2 = null;
                z_axis.setProgress(0);
                z_axis.setEnabled(false);
                confirm.setEnabled(false);
                confirm.setText("Next");
                data.setText("Haz click en las esquinas inferiores de la puerta tras calibrar");
                width.setText("Anchura puerta: --");
                mechanism.setText("Altura mecanismo: --");
                height.setText("Altura puerta: --");
                measure_height = false;
                for (AnchorNode n : anchorNodes) {
                    arFragment.getArSceneView().getScene().removeChild(n);
                    n.getAnchor().detach();
                    n.setParent(null);
                    n = null;
                }
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MeasureActivity.this, "Confirmado", Toast.LENGTH_SHORT).show();
                if (!measure_height) {
                    measure_height = true;
                    data.setText("Sube el cubo con el deslizador hasta que su base de con el marco de la puerta");
                    confirm.setEnabled(false);
                    confirm.setText("Confirm");
                } else
                    Confirmar();
            }
        });


        z_axis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                upDistance = progress;
                ascend(myanchornode, upDistance);
                if (measure_height) {
                    height.setText("Altura puerta: " +
                            form_numbers.format(progress / 100f));
                    puerta.setAltura(progress);
                }
                else {
                    mechanism.setText("Altura mecanismo: " +
                            form_numbers.format(progress / 100f));
                    puerta.setAlturaPomo(progress);
                }
                confirm.setEnabled(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.cubito)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (andyRenderable == null) {
                        return;
                    }
                    myhit = hitResult;

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();

                    AnchorNode anchorNode = new AnchorNode(anchor);


                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    anchorNodes.add(anchorNode);

                    if (anchor1 == null) {
                        anchor1 = anchor;
                    } else {
                        anchor2 = anchor;
                        width.setText("Anchura puerta: " +
                                form_numbers.format(getMetersBetweenAnchors(anchor1, anchor2)));

                        puerta.setAnchura((int)(getMetersBetweenAnchors(anchor1, anchor2)*100));

                        data.setText("Sube el cubo con el deslizador hasta que su base de con el mecanismo de apertura");

                        z_axis.setEnabled(true);
                    }
                    myanchornode = anchorNode;

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);
                    andy.select();
                    andy.getScaleController().setEnabled(false);
                });
        puertaDialog();
    }

    void ascend(AnchorNode an, float up) {
        Anchor anchor = myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(0, up / 100f, 0)));

        an.setAnchor(anchor);
    }


    float getMetersBetweenAnchors(Anchor anchor1, Anchor anchor2) {
        float[] distance_vector = anchor1.getPose().inverse()
                .compose(anchor2.getPose()).getTranslation();
        float totalDistanceSquared = 0;
        for (int i = 0; i < 3; ++i)
            totalDistanceSquared += distance_vector[i] * distance_vector[i];
        return (float) Math.sqrt(totalDistanceSquared);
    }

    void Confirmar()
    {
        boolean cumple_altura = Evaluator.IsGreaterThan(puerta.getAltura(), paramAltura);
        boolean cumple_anchura = Evaluator.IsGreaterThan(puerta.getAnchura(),paramAnchura);
        boolean cumple_tipo_puerta = ArrayUtils.contains(new String[]{"Abatible", "Tornos"}, puerta.getTipoPuerta());
        boolean cumple_tipo_mecanismos = ArrayUtils.contains(new String[]{"Manibela", "Barra", "Agarrador"}, puerta.getTipoMecanismo());
        boolean cumple_alto_mecanismo = Evaluator.IsInRange(puerta.getAlturaPomo(), minMecApertura, maxMecApertura);

        puerta.setAccesible(cumple_altura && cumple_altura && cumple_anchura && cumple_tipo_mecanismos && cumple_tipo_puerta);

        Intent i = new Intent(this, AxesibilityActivity.class);
        i.putExtra(TypesManager.OBS_TYPE, TypesManager.obsType.PUERTAS.getValue());
        i.putExtra(TypesManager.PUERTAS_OBS, puerta);

        startActivity(i);
        finish();
    }

    void puertaDialog() {
        int[] spinnerImages = new int[]{R.drawable.puerta_giratoria, R.drawable.puerta_corredera
                , R.drawable.puerta_abatible, R.drawable.puerta_torno};


        int[] spinnerImages2 = new int[]{R.drawable.mecanismo_manibela, R.drawable.mecanismo_pomo
                , R.drawable.mecanismo_barra, R.drawable.mecanismo_agarrador};


        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MeasureActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_puerta_pomo, null);
        mBuilder.setTitle("Selecciona puerta y pomo");
        Spinner mSpinnerDoor = (Spinner) mView.findViewById(R.id.spinner_puerta);
        Spinner mSpinnerMecha = (Spinner) mView.findViewById(R.id.spinner_mecanismo);

        ImageTitleAdapter mCustomAdapter = new ImageTitleAdapter(MeasureActivity.this, spinnerImages, tipoPuerta);
        mSpinnerDoor.setAdapter(mCustomAdapter);

        ImageTitleAdapter mCustomAdapter2 = new ImageTitleAdapter(MeasureActivity.this, spinnerImages2, tipoMecanismo);
        mSpinnerMecha.setAdapter(mCustomAdapter2);


        mBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        mBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                puerta.setTipoPuerta(tipoPuerta[mSpinnerDoor.getSelectedItemPosition()]);
                puerta.setTipoMecanismo(tipoMecanismo[mSpinnerMecha.getSelectedItemPosition()]);
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();

        dialog.show();
    }


    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
