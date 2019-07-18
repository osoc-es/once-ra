/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.osoc.oncera;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
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
import com.osoc.oncera.R;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan2;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class SimulacionActivity extends AppCompatActivity {
    private static final String TAG = SimulacionActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private float x=0f, y=0f, z=0f;
    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private Anchor myanchor;
    private AnchorNode myanchornode;
    TransformableNode mytranode = null;
    private Anchor anchor1, anchor2;

    private HitResult myhit;
    private float mytravel=0.01f, distance_x=0f, distance_z=0f, myangle=0f;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_simulacion);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        Button r_left = (Button)findViewById(R.id.r_left);
        Button r_right = (Button)findViewById(R.id.r_right);
        Button accelerate = (Button)findViewById(R.id.accelerate);
        List<AnchorNode> anchorNodes = new ArrayList<>();

      /*rotate.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              forward(myanchornode);
              data.setText(Double.toString(Math.toDegrees(set(mytranode.getLocalRotation()).x))+" "+
                      Double.toString(Math.toDegrees(set(mytranode.getLocalRotation()).y))+" "+
                      Double.toString(Math.toDegrees(set(mytranode.getLocalRotation()).z)));
          }
      });*/

        accelerate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mytranode != null) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        myangle = set(mytranode.getLocalRotation());
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                    }
                    forward(myanchornode);
                }
                return true;

            }
        });

        r_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mytranode != null){
                    Quaternion q1 = mytranode.getLocalRotation();
                    Quaternion q2 = Quaternion.axisAngle(new Vector3(0, 1f, 0f), .5f);
                    mytranode.setLocalRotation(Quaternion.multiply(q1, q2));
                    myangle = set(mytranode.getLocalRotation());
                }

                return true;
            }

        });

        r_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mytranode != null){
                    myangle+=0.01f;
                    Quaternion q1 = mytranode.getLocalRotation();
                    Quaternion q2 = Quaternion.axisAngle(new Vector3(0, 1f, 0f), -.5f);
                    mytranode.setLocalRotation(Quaternion.multiply(q1, q2));
                    myangle = set(mytranode.getLocalRotation());
                }

                return true;
            }

        });

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.silla)
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

                    myanchor = anchor;
                    myanchornode = anchorNode;

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy;
                    if(mytranode == null)
                    andy = new TransformableNode(arFragment.getTransformationSystem());
                    else andy = mytranode;

                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);
                    andy.select();
                    andy.getScaleController().setEnabled(false);

                    mytranode = andy;
                });
    }

    void ascend(AnchorNode an, float x, float y, float z){
        Anchor anchor =  myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(x/100f, z/100f, y/100f)));

        an.setAnchor(anchor);
    }

    Quaternion rotate(AnchorNode an, float angle) {
        //mytranode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), angle));

        return mytranode.getLocalRotation();
    }

   /* public double set(Quaternion q1) {
      float a, b, c;
        if (q1.w > 1) q1.normalize(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
        double angle = 2 * Math.acos(q1.w);
        double s = Math.sqrt(1-q1.w*q1.w); // assuming quaternion normalised then w is less than 1, so term always positive.
        if (s < 0.001) { // test to avoid divide by zero, s is always positive due to sqrt
            // if s close to zero then direction of axis not important
            a = q1.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            b = q1.y;
            c = q1.z;
        } else {
            a = (float) (q1.x / s); // normalise axis
            b = (float) (q1.y / s);
            c = (float) (q1.z / s);
        }
        return angle;
    }*/


    public float set(Quaternion q1) {
        Vector3 angles = new Vector3();
        double sqw = q1.w*q1.w;
        double sqx = q1.x*q1.x;
        double sqy = q1.y*q1.y;
        double sqz = q1.z*q1.z;
        double unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise is correction factor
        double test = q1.x*q1.y + q1.z*q1.w;
        if (test > 0.499*unit) { // singularity at north pole
            angles.x = (float) (2 * atan2(q1.x,q1.w));
            angles.y = (float) Math.PI/2;
            angles.z = 0;
            return angles.x;
        }
        if (test < -0.499*unit) { // singularity at south pole
            angles.x = (float) (-2 * atan2(q1.x,q1.w));
            angles.y = (float) (-Math.PI/2);
            angles.z = 0;
            return angles.x;
        }
        angles.x = (float) atan2(2*q1.y*q1.w-2*q1.x*q1.z , sqx - sqy - sqz + sqw);
        angles.y = (float) Math.asin(2*test/unit);
        angles.z = (float) atan2(2*q1.x*q1.w-2*q1.y*q1.z , -sqx + sqy - sqz + sqw);
        return angles.x;
    }


    void forward(AnchorNode an){
        //Vector3 forw = mytranode.getForward();
        //System.out.println("Volando voy: "+forw.toString());
        distance_x+=Math.sin(myangle)*mytravel;
        distance_z+=Math.cos(myangle)*mytravel;

        Anchor anchor =  myhit.getTrackable().createAnchor(
                myhit.getHitPose().compose(Pose.makeTranslation(-distance_x, 0f, -distance_z)));


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

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */




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
