package com.teapps.bolzer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.teapps.bolzer.helper.CameraPreview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QRCodeScanActivtiy extends AppCompatActivity {

    private CameraPreview mPreview;
    private FirebaseVisionBarcodeDetectorOptions options;

    Camera mCamera = null;
    ImageView imageView, qrScanField;
    Button button;
    FrameLayout preview;
    ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private Camera.PictureCallback picture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            imageView = findViewById(R.id.img_result_img_view);
            qrScanField = findViewById(R.id.img_qr_scan_field);
            Bitmap bmp = getResizedBitmap(BitmapFactory.decodeByteArray(data, 0, data.length)
                    , 1024);
            startAnalysis(bmp, camera);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_scan_activtiy);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);


        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        initFirebase();
        initFirebaseBarcodeVision();
        initCamera();

        button = findViewById(R.id.btn_capture);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, picture);
                progressDialog = new ProgressDialog(QRCodeScanActivtiy.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Analysiere....");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        });



    }

    private void initFirebase() {
        firebaseAuth = firebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    private void startAnalysis(final Bitmap rotatedImg, final Camera camera) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(rotatedImg);
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector();

        final Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                        camera.stopPreview();
                        imageView.setVisibility(View.VISIBLE);
                        button.setVisibility(View.INVISIBLE);
                        preview.setVisibility(View.INVISIBLE);
                        qrScanField.setVisibility(View.INVISIBLE);

                        for (FirebaseVisionBarcode barcode: barcodes) {
                            Point[] corners = barcode.getCornerPoints();
                            String rawValue = barcode.getRawValue();

                            drawRectangleToQRCode(rotatedImg, corners);
                            checkValidationOfQRCode(rawValue, camera);
                            progressDialog.dismiss();
                        }

                        if (barcodes.isEmpty()) {
                            noQrCodeFound(camera);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Fehler"
                                , Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void noQrCodeFound(Camera camera) {
        progressDialog.dismiss();
        Toast.makeText(getApplicationContext(), "Kein QR-Code gefunden"
                , Toast.LENGTH_SHORT).show();
        imageView.setVisibility(View.INVISIBLE);
        qrScanField.setVisibility(View.VISIBLE);
        preview.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);
        camera.startPreview();
    }

    private void noValidQRCode(Camera camera) {
        progressDialog.dismiss();
        qrScanField.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        preview.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);
        camera.startPreview();
    }

    private void drawRectangleToQRCode(Bitmap rotatedImg, Point[] corners) {
        Canvas c = new Canvas(rotatedImg);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        c.drawLine(corners[0].x, corners[0].y, corners[1].x, corners[1].y, paint);
        c.drawLine(corners[1].x, corners[1].y, corners[2].x, corners[2].y, paint);
        c.drawLine(corners[2].x, corners[2].y, corners[3].x, corners[3].y, paint);
        c.drawLine(corners[3].x, corners[3].y, corners[0].x, corners[0].y, paint);
        imageView.setImageBitmap(rotatedImg);
    }

    private void checkValidationOfQRCode(final String rawValue, final Camera camera) {
        try {
            String bolzer_id = rawValue.split("#")[0];
            database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(bolzer_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    try {
                        String creator_id = documentSnapshot.getString(getString(R.string.KEY_CREATOR_INFO)).split("#")[2];
                        if (creator_id.equals(firebaseAuth.getCurrentUser().getUid())) {
                            checkIfUserIsAlreadyConfirmed(rawValue, camera);
                        } else {
                            Toast.makeText(getApplicationContext(), "Du bist nicht berechtigt " +
                                    "diesen QR-Code zu scannen", Toast.LENGTH_SHORT).show();
                            noValidQRCode(camera);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Kein gültiger QR-Code", Toast.LENGTH_SHORT).show();
                        noValidQRCode(camera);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Kein gültiger QR-Code", Toast.LENGTH_SHORT).show();
            noValidQRCode(camera);
        }
    }

    private void checkIfUserIsAlreadyConfirmed(final String rawValue, final Camera camera) {

        String[] valuesFromRawValue = rawValue.split("#");

        database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(valuesFromRawValue[0])
                .collection("member").document(valuesFromRawValue[1])
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.getBoolean("confirmed")) {
                    Toast.makeText(getApplicationContext(), "Teilnehmer bereits bestätigt"
                            , Toast.LENGTH_LONG).show();
                    noValidQRCode(camera);
                } else {
                    showConfirmationDialog(rawValue, camera);
                }

            }
        });
    }

    private void showConfirmationDialog(String rawValue, final Camera camera) {
        AlertDialog.Builder builder = new AlertDialog.Builder(QRCodeScanActivtiy.this, R.style.DialogTheme);
        builder.setTitle("Teilnahme bestätigen?");
        final String[] values = rawValue.split("#");
        builder.setMessage("Teilnehmer wirklich bestätigen?");
        builder.setPositiveButton("Bestätigen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {

                Map<String, Object> updateConfirmation = new HashMap<>();
                updateConfirmation.put("confirmed", true);

                database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(values[0])
                        .collection("member").document(values[1]).update(updateConfirmation)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                Map<String, Object> mapCreator = new HashMap<>();
                                mapCreator.put("standard_points", FieldValue.increment(5));
                                mapCreator.put("bolzers_scanned_as_creator", FieldValue.increment(1));

                                Map<String, Object> mapMember = new HashMap<>();
                                mapMember.put("standard_points", FieldValue.increment(5));
                                mapMember.put("bolzers_completed_as_member", FieldValue.increment(1));

                                database.collection(getString(R.string.COLLECTION_USERS)).document(firebaseAuth.getCurrentUser().getUid())
                                        .update(mapCreator);
                                database.collection(getString(R.string.COLLECTION_USERS)).document(values[1])
                                        .update(mapMember);
                                dialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Bestätigt", Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            }
                        });
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                imageView.setVisibility(View.INVISIBLE);
                preview.setVisibility(View.VISIBLE);
                button.setVisibility(View.VISIBLE);
                qrScanField.setVisibility(View.VISIBLE);
                camera.startPreview();
            }
        });
        Dialog dialog = builder.create();
        dialog.show();
    }


    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        Bitmap scaledBmp =  Bitmap.createScaledBitmap(image, width, height, true);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(scaledBmp, 0, 0, scaledBmp.getWidth()
                , scaledBmp.getHeight(), matrix, true);
    }

    private void initCamera() {
        if (checkCameraHardware(getApplicationContext())) {
            try {
                mCamera = Camera.open();
            }
            catch (Exception e){
                Toast.makeText(getApplicationContext(), "Keine Kamera"
                        , Toast.LENGTH_SHORT).show();
            }

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            preview = findViewById(R.id.frameL);
            preview.addView(mPreview);
        } else {
            Toast.makeText(getApplicationContext(), "Keine Kamera", Toast.LENGTH_SHORT).show();
        }
    }

    private void initFirebaseBarcodeVision() {
        options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                        .setBarcodeFormats(
                                FirebaseVisionBarcode.FORMAT_QR_CODE)
                        .build();
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qrscanner_activty, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.qr_scanner_help:
                AlertDialog.Builder builder = new AlertDialog.Builder(QRCodeScanActivtiy.this
                        , R.style.DialogThemeAlert);
                builder.setTitle("QR-Scanner Hilfe");
                builder.setMessage("Falls kein QR-Code erkannt wird, probieren sie den QR-Code " +
                        "aus ca. 20 - 30 cm Entfernung erneut zu scannen!");
                builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                Dialog dialog = builder.create();
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
