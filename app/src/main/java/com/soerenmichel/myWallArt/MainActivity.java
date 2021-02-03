package com.soerenmichel.myWallArt;

import android.content.Context;
import android.support.v4.content.FileProvider;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
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

import java.io.File;
import java.io.IOException;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;

    CustomArFragment arFragment;
    Button camera_button, gallery_button,clear_button;
    String currentPhotoPath;
    ImageView picture_c;
    TextView textView;
    Uri picture_path = null;


    Vector<AnchorNode> anchorNodeVector = new Vector<AnchorNode>();
    Vector<ImageView> imageViewVector = new Vector<ImageView>();
    double sizing = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign objects
        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        camera_button = findViewById(R.id.button_camera);
        gallery_button = findViewById(R.id.button_gallery);
        clear_button = findViewById(R.id.clear);
        textView = findViewById(R.id.textView);
        // set a change listener on the SeekBar
        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        int progress = seekBar.getProgress();
        textView.setText("Progress: " + progress);


        //Inflate controls.xml to assign picture_c
        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.controls, null );
        picture_c =(ImageView) view.findViewById(R.id.picture);

        //Create onClick/onTap Listeners
        camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPictureIntent();
            }
        });

        gallery_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });
        arFragment.setOnTapArPlaneListener(
                (HitResult hitresult, Plane plane, MotionEvent motionevent) -> {
                    if (plane.getType() != Plane.Type.VERTICAL)
                        return;

                    Anchor anchor = hitresult.createAnchor();
                    placeObject(arFragment, anchor);
                }
        );

        clear_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                removePictures();
            }
        });



    }
    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // updated continuously as the user slides the thumb
            sizing = progress;
            textView.setText("Size: " + progress+"%");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // called after the user finishes moving the SeekBar
            if (anchorNodeVector.size() == 0){
                Toast.makeText(getApplicationContext(),"Please place a picture first, before trying to resize it.lol", Toast.LENGTH_SHORT).show();
            }
                else {

                for (int i = 0; i < imageViewVector.size(); i++) {
                    ImageView current_imageView = imageViewVector.get(i);
                    int width = current_imageView.getDrawable().getIntrinsicWidth();
                    int height = current_imageView.getDrawable().getIntrinsicHeight();
                    double value = ((double) sizing) /100;
                    height = (int) (height * value);
                    width = (int) (width * value);
                    current_imageView.getLayoutParams().height = height;
                    current_imageView.getLayoutParams().width = width;
                    current_imageView.requestLayout();
                }
            }
        }
    };


    //Get anchor and the viewRenderable from controls.xml
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

    //Add the anchor and set sources
    private void addNodeToScene(Anchor anchor, ViewRenderable viewRenderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        anchorNodeVector.add(anchorNode); //to delete anchor in "removePictures" function

        //Create a Node with a fixed camera axes. Vertical planes causes random rotation
        //Add anchorNode as its parent
        Node fixedNode = new Node();
        fixedNode.setParent(anchorNode);
        Vector3 anchorUp = anchorNode.getUp();
        fixedNode.setLookDirection(Vector3.up(), anchorUp);

        viewRenderable.setShadowCaster(false);              //Removing shadow of the view Renderable?!
        viewRenderable.setShadowReceiver(false);

        //picNode is the child node of fixedNote
        //Setting the node rotation will help us to stick the picture to the wall
        Node picNode = new Node();
        picNode.setLocalRotation(new Quaternion(90f, 0f, 0f, -90f));
        picNode.setRenderable(viewRenderable);
        picNode.setParent(fixedNode);


        //Creating a View from the Render and setting its Uri
        View view = viewRenderable.getView();
        ImageView imageView;
        imageView = view.findViewById(R.id.picture);

        if (picture_path == null) {
            Toast.makeText(this, "Bro select a pic first",
                    Toast.LENGTH_SHORT).show();
        }

        imageView.setImageURI(picture_path);
        imageViewVector.add(imageView);
    }

    //Override onActivityResult the save incoming data
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == CAMERA_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){

                File f = new File(currentPhotoPath);
                Uri contentUri = Uri.fromFile(f);
                picture_path = contentUri;
                Toast.makeText(this, "Tap on a wall to place your picture",
                        Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == GALLERY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                Uri contentUri = data.getData();
                picture_path = contentUri;
                Toast.makeText(this, "Tap on a wall to place your picture",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Start the picture intent and set resource files
    private void startPictureIntent() {

        String filename = "photo_received";
        File storageDiretory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            File imageFile = File.createTempFile(filename, ".jpg",storageDiretory);

            currentPhotoPath = imageFile.getAbsolutePath();

            Uri tmp = FileProvider.getUriForFile(MainActivity.this, "com.soerenmichel.myWallArt.fileprovider",imageFile);

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,tmp);
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removePictures() {
        if (anchorNodeVector.size() == 0){
            Toast.makeText(this, "No picture to delete",
                    Toast.LENGTH_SHORT).show();
        }
        else {
            for (int i = 0; i < anchorNodeVector.size(); i++) {
                AnchorNode nodeToRemove = anchorNodeVector.get(i);
                arFragment.getArSceneView().getScene().removeChild(nodeToRemove);
                nodeToRemove.getAnchor().detach();
                nodeToRemove.setParent(null);
                nodeToRemove = null;
            }
            anchorNodeVector.clear();
            Toast.makeText(this, "All pictures deleted",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
