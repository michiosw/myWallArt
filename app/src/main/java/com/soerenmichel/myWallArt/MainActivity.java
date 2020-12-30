package com.soerenmichel.myWallArt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {
   // private static final String TAG = MainActivity.class.getSimpleName();
   // private static final double MIN_OPENGL_VERSION = 3.0;

    CustomArFragment arFragment;

   // ModelRenderable lampPostRenderable;
    //private Uri selectedObject;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // initializeGallery();

        arFragment.setOnTapArPlaneListener(
                (HitResult hitresult, Plane plane, MotionEvent motionevent) -> {
                    if (plane.getType() != Plane.Type.VERTICAL)
                        return;

                    Anchor anchor = hitresult.createAnchor();
                    placeObject(arFragment, anchor);
                }
        );

    }

    private void placeObject(ArFragment arFragment, Anchor anchor) {
        ViewRenderable.builder()
                .setView(this, R.layout.controls)
                .build()
                .thenAccept(viewRenderable -> {addNodeToScene(anchor, viewRenderable );})
                .exceptionally(throwable -> {
                            Toast.makeText(arFragment.getContext(), "Error:" + throwable.getMessage(), Toast.LENGTH_LONG).show();
                            return null;
                        }

                );

    }

    private void addNodeToScene(Anchor anchor, ViewRenderable viewRenderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        arFragment.getArSceneView().getScene().addChild(anchorNode);

        //Create a Node with a fixed camera axes. Vertical planes causes random rotation
        //Add anchorNode as its parent
        Node fixedNode = new Node();
        fixedNode.setParent(anchorNode);
        Vector3 anchorUp = anchorNode.getUp();
        fixedNode.setLookDirection(Vector3.up(), anchorUp);

        //picNode is the child node of fixedNote
        //Setting the node rotation will help us to stick the picture to the wall
        Node picNode = new Node();
        picNode.setLocalRotation(new Quaternion(90f, 0f, 0f, -90f));
        picNode.setRenderable(viewRenderable);
        picNode.setParent(fixedNode);

        //Creating a View from the Render and setting its resources
        View view = viewRenderable.getView();
        ImageView imageView = view.findViewById(R.id.picture);
        imageView.setImageResource(R.mipmap.ic_launcher);
    }

}
