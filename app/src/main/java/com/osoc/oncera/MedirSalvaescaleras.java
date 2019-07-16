package com.osoc.oncera;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MedirSalvaescaleras extends AppCompatActivity {
    private static final String TAG = MeasureActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private float upDistance=0f;
    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private Anchor myanchor;
    private AnchorNode myanchornode;
    private DecimalFormat form_numbers = new DecimalFormat("#0.00");

    private List<AnchorNode> anchorNodes;

    private boolean medir_profundidad = false;

    private boolean mando = false, carga = false, velocidad = false;

    Button restart;
    Button confirm;
    TextView data;
    TextView ancho_plat;
    TextView largo_plat;

    private Anchor anchor1=null, anchor2=null;

    private HitResult myhit;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_medir_salvaescaleras);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        restart = (Button)findViewById(R.id.btn_restart);
        confirm = (Button)findViewById(R.id.btn_ok);
        data = (TextView) findViewById(R.id.tv_distance);

        ancho_plat = (TextView) findViewById(R.id.ancho_plat);
        largo_plat = (TextView) findViewById(R.id.largo_plat);

        anchorNodes = new ArrayList<>();

        confirm.setEnabled(false);


        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                anchor1=null;
                anchor2=null;
                confirm.setEnabled(false);
                confirm.setText("Next");
                data.setText("Toca en las esquinas de la plataforma a lo ancho");

                ancho_plat.setText("Anchura plataforma: --");
                largo_plat.setText("Longitud plataforma: --");

                medir_profundidad = false;

                for(AnchorNode n : anchorNodes){
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
                if(!medir_profundidad){
                    medir_profundidad = true;
                    resetMedirAnchura();
                }
                else{
                    Toast.makeText(MedirSalvaescaleras.this, "Confirmado", Toast.LENGTH_SHORT).show();
                }
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

                    if(anchor1 == null) {
                        anchor1 = anchor;
                    }
                    else {
                        anchor2 = anchor;
                        confirm.setEnabled(true);
                        if(!medir_profundidad){
                            ancho_plat.setText("Anchura plataforma: " +
                                    form_numbers.format(getMetersBetweenAnchors(anchor1, anchor2)));
                        }
                        else {
                            largo_plat.setText("Longitud plataforma: " +
                                    form_numbers.format(getMetersBetweenAnchors(anchor1, anchor2)));
                        }
                    }
                    myanchornode = anchorNode;

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);
                    andy.select();
                    andy.getScaleController().setEnabled(false);
                });
        barandillaDialog();
    }

    void ascend(AnchorNode an, float up){
        Anchor anchor =  myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(0, up/100f, 0)));

        an.setAnchor(anchor);
    }


    float getMetersBetweenAnchors(Anchor anchor1, Anchor anchor2) {
        float[] distance_vector = anchor1.getPose().inverse()
                .compose(anchor2.getPose()).getTranslation();
        float totalDistanceSquared = 0;
        for(int i=0; i<3; ++i)
            totalDistanceSquared += distance_vector[i]*distance_vector[i];
        return (float) Math.sqrt(totalDistanceSquared);
    }

    void barandillaDialog(){


        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MedirSalvaescaleras.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_pregunta_salvaescaleras, null);
        mBuilder.setTitle("Rellena el cuestionario");

        CheckBox chkMando = (CheckBox) mView.findViewById(R.id.chkMando);
        CheckBox chkCarga = (CheckBox) mView.findViewById(R.id.chkCarga);
        CheckBox chkVelocidad = (CheckBox) mView.findViewById(R.id.chkVelocidad);


        mBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i){
                dialogInterface.dismiss();
            }
        });

        mBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mando = chkMando.isChecked();
                carga = chkCarga.isChecked();
                velocidad = chkVelocidad.isChecked();
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();

        dialog.show();
    }

    void resetMedirAnchura(){
        anchor1=null;
        anchor2=null;
        confirm.setEnabled(false);
        confirm.setText("Confirm");
        data.setText("Toca en las esquinas de la plataforma a lo largo");
        for(AnchorNode n : anchorNodes){
            arFragment.getArSceneView().getScene().removeChild(n);
            n.getAnchor().detach();
            n.setParent(null);
            n = null;
        }
    }


    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
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
