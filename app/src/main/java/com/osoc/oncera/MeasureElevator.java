package com.osoc.oncera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.osoc.oncera.javabean.Elevator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MeasureElevator extends AppCompatActivity {

    private static final String TAG = MeasureElevator.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private DecimalFormat form_numbers = new DecimalFormat("#0.00");
    private float widthParam;
    private float depthParam;
    private ImageView img_instr;

    private List<AnchorNode> anchorNodes;

    private boolean depthMeasure = false;

    private boolean braille = false, auto = false, sound = false, gap = false, step = false;

    private Elevator elevator = new Elevator(null,null,null,null,null,null,null,null,null,null, null);

    Button restart;
    Button confirm;
    TextView data;
    TextView elevator_width;
    TextView elevator_depth;
    private String message;

    private Anchor anchor1=null, anchor2=null;

    private HitResult myhit;

    private AnchorNode myanchornode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_measure_lift);

        final DatabaseReference anch = FirebaseDatabase.getInstance().getReference("Standards/Elevator/Width");

        anch.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                widthParam = dataSnapshot.getValue(Float.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference prof = FirebaseDatabase.getInstance().getReference("Standards/Elevator/Depth");

        prof.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                depthParam = dataSnapshot.getValue(Float.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        restart = (Button)findViewById(R.id.btn_restart);
        confirm = (Button)findViewById(R.id.btn_ok);
        data = (TextView) findViewById(R.id.tv_distance);

        elevator_width = (TextView) findViewById(R.id.elevator_width);
        elevator_depth = (TextView) findViewById(R.id.elevator_depth);
        ImageButton btnBack = (ImageButton) findViewById(R.id.btnBack);
        img_instr = (ImageView) findViewById(R.id.img_instr);

        anchorNodes = new ArrayList<>();

        confirm.setEnabled(false);


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                resetLayout();
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!depthMeasure){
                    depthMeasure = true;
                    measureDepthLayout();
                }
                else{
                    Toast.makeText(MeasureElevator.this, "Confirmado", Toast.LENGTH_SHORT).show();
                    validateEvaluation();

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
                        if(!depthMeasure){
                            elevator_width.setText("Anchura ascensor: " +
                                    form_numbers.format(getMetersBetweenAnchors(anchor1, anchor2)));
                            elevator.setStallWidth((getMetersBetweenAnchors(anchor1, anchor2))*100);
                        }
                        else {
                            elevator_depth.setText("Profundidad ascensor: " +
                                    form_numbers.format(getMetersBetweenAnchors(anchor1, anchor2)));
                            elevator.setStallDepth(getMetersBetweenAnchors(anchor1, anchor2)*100);
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
        questionsElevatorDialog();
    }

    /**
     * Function to return the distance in meters between two objects placed in ArPlane
     * @param anchor1 first object's anchor
     * @param anchor2 second object's anchor
     * @return the distance between the two anchors in meters
     */
    private float getMetersBetweenAnchors(Anchor anchor1, Anchor anchor2) {
        float[] distance_vector = anchor1.getPose().inverse()
                .compose(anchor2.getPose()).getTranslation();
        float totalDistanceSquared = 0;
        for(int i=0; i<3; ++i)
            totalDistanceSquared += distance_vector[i]*distance_vector[i];
        return (float) Math.sqrt(totalDistanceSquared);
    }

    /**
     * Dialog to ask user questions about certain accessibility aspects of the elevator
     */
    private void questionsElevatorDialog(){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MeasureElevator.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_lift, null);
        mBuilder.setTitle("Rellena el cuestionario");
        CheckBox chkBraille = (CheckBox) mView.findViewById(R.id.chkBraille);
        CheckBox chkSound = (CheckBox) mView.findViewById(R.id.chkSound);
        CheckBox chkAutomatic = (CheckBox) mView.findViewById(R.id.chkAutomatic);
        CheckBox chkStep = (CheckBox) mView.findViewById(R.id.chkStep);
        CheckBox chkGap = (CheckBox) mView.findViewById(R.id.chkGap);


        mBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i){
                dialogInterface.dismiss();
            }
        });

        mBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                braille = chkBraille.isChecked();
                auto = chkAutomatic.isChecked();
                sound = chkSound.isChecked();
                gap = chkGap.isChecked();
                step = chkStep.isChecked();

                elevator.setBrailleButtons(braille);
                elevator.setAutoDoors(auto);
                elevator.setAudioMark(sound);

            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();

        dialog.show();

    }

    /**
     * Check whether the elevator is accessible or not and start Axesibility activity to display the
     * result
     */
    void validateEvaluation(){
        String s = "";

        Boolean width = Evaluator.IsGreaterThan(elevator.getStallWidth(),
                widthParam);

        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_anch) + widthParam, width);

        Boolean depth = Evaluator.IsGreaterThan(elevator.getStallDepth(),
                depthParam);

        s = UpdateStringIfNeeded(s, "y", s == "" || depth);
        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_prof) + depthParam, depth);


        s = UpdateStringIfNeeded(s, "y", s == "" || braille);
        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_braille), braille);

        s = UpdateStringIfNeeded(s, "y", s == "" || auto);
        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_automat), auto);

        s = UpdateStringIfNeeded(s, "y", s == "" || sound);
        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_audio), sound);

        s = UpdateStringIfNeeded(s, "y", s == "" || gap);
        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_dist), gap);

        s = UpdateStringIfNeeded(s, "y", s == "" || step);
        s = UpdateStringIfNeeded(s, getString(R.string.asc_n_alt_resal), auto);

        elevator.setAccessible(width && depth && braille && auto && sound && gap && step);

        UpdateMessage(elevator.getAccessible(), s);

        elevator.setMessage(message);

        Intent i = new Intent(this,AccessibilityChecker.class);
        i.putExtra(TypesManager.OBS_TYPE,TypesManager.obsType.ELEVATOR.getValue());
        i.putExtra(TypesManager.ELEVATOR_OBS, elevator);

        startActivity(i);
        finish();
    }

    /**
     * Prepare layout to start measuring the ramp's width
     */
    void measureDepthLayout(){
        anchor1=null;
        anchor2=null;
        confirm.setEnabled(false);
        confirm.setText("Confirmar");
        img_instr.setImageResource(R.drawable.ascensor_02);
        data.setText(R.string.instr_ascensor_02);
        for(AnchorNode n : anchorNodes){
            arFragment.getArSceneView().getScene().removeChild(n);
            n.getAnchor().detach();
            n.setParent(null);
            n = null;
        }
    }

    /**
     * Set layout to its initial state
     */
    private void resetLayout(){
        anchor1=null;
        anchor2=null;
        confirm.setEnabled(false);
        confirm.setText("Siguiente");
        img_instr.setImageResource(R.drawable.ascensor_01);
        data.setText(R.string.instr_ascensor_01);

        elevator_width.setText("Anchura ascensor: --");
        elevator_depth.setText("Profundidad ascensor: --");

        depthMeasure = false;

        for(AnchorNode n : anchorNodes){
            arFragment.getArSceneView().getScene().removeChild(n);
            n.getAnchor().detach();
            n.setParent(null);
            n = null;
        }
    }

    /**
     * Check whether the device supports the tools required to use the measurement tools
     * @param activity
     * @return boolean determining whether the device is supported or not
     */
    private boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
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

    /**
     * Add string to another string if a condition is met
     * @param base initial base string
     * @param to_add string that will be added to the base if the condition is met
     * @param condition condition that determines whether the string is altered or not
     * @return result string
     */
    private String UpdateStringIfNeeded(String base, String to_add, boolean condition)
    {
        return condition ? base : base + " " + to_add;
    }

    /**
     * Updates message to show whether an element is accessible or not with an explanation
     * @param condition boolean determining whether the element is accessible or not
     * @param aux explanation of the accessibility result
     */
    private void UpdateMessage(boolean condition, String aux)
    {
        message = condition? getString(R.string.accessible) : getString(R.string.no_accesible);
        message += aux;
    }
}
