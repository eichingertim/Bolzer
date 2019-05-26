package com.teapps.bolzer;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter;
import com.teapps.bolzer.services.AlarmReceiver;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.teapps.bolzer.helper.Constants.*;

public class NewBolzerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 120;

    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private EditText title, country, city, postalcode, location, name;
    private Spinner ageFrom, ageTo;
    private Button btnPickLocation;

    private Button btnTime, btnDate;

    private StorageReference mStorageRef;

    private Calendar alarmCalender = Calendar.getInstance();
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_bolzer);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initializeFirebase();
        initializeActionBar();
        initializeObjects();
        fillKnownObjects();
    }

    /**
     * Reads the values from all text-fields and converts the string data into the respective
     * data-type. Then all data is inserted into a Hash-Map. To upload the data the method
     * "uploadData()" is called at the end of this method.
     */
    private void publishNewBolzer() {

        String bolzerLocationStr = location.getText().toString();
        String[] bolzerLocation = bolzerLocationStr.split(", ");
        String bolzerCreatorNameAndEmail = firebaseUser.getEmail() + "#"
                + name.getText().toString() + "#" + firebaseUser.getUid();
        String strAgeGroup = String.format(getString(R.string.age_data_to_string)
                , ageFrom.getSelectedItem().toString(), ageTo.getSelectedItem().toString());
        String randomId = randomID();
        double lati = 0;
        double longi = 0;
        try {
            lati = Double.parseDouble(bolzerLocation[0]);
            longi = Double.parseDouble(bolzerLocation[1]);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), getString(R.string.select_an_address_location)
                    , Toast.LENGTH_LONG).show();
        }

        Map<String, Object> map = new HashMap<>();
        map.put(KEY_TITLE, title.getText().toString());
        map.put(KEY_COUNTRY, country.getText().toString());
        map.put(KEY_CITY, city.getText().toString());
        map.put(KEY_POSTALCODE, postalcode.getText().toString());
        map.put(KEY_LOCATION, new GeoPoint(lati, longi));
        map.put(KEY_CREATOR_NAME_AND_EMAIL, bolzerCreatorNameAndEmail);
        map.put(KEY_AGE_GROUP, strAgeGroup);
        map.put(KEY_ID, randomId);
        map.put(KEY_MEMBERS, name.getText().toString());
        map.put(KEY_MEMBERS_ID, firebaseUser.getUid());
        map.put(KEY_TIME, btnTime.getText().toString());
        map.put(KEY_DATE, btnDate.getText().toString());

        checkingDataAndCreateSnapshot(bolzerLocationStr, lati, longi, map);

    }

    /**
     * This method makes a snapshot from the map where the bolzer is located, uploads the bitmap to
     * firebase storage and puts the downlaod link to the map with all the data. After that, the map
     * is uploaded to the firebase-firestore-database.
     */
    private void checkingDataAndCreateSnapshot(String bolzerLocationStr
            ,double lati, double longi, final Map<String, Object> map) {

        if (isValidInput(map.get(KEY_TITLE).toString(), map.get(KEY_COUNTRY).toString()
                , map.get(KEY_CITY).toString(), map.get(KEY_POSTALCODE).toString(), bolzerLocationStr)) {

            progressDialog = new ProgressDialog(NewBolzerActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("wird veröffentlicht....");
            progressDialog.setCancelable(false);
            progressDialog.show();

            MapSnapshotter.Options snapShotOptions = new MapSnapshotter
                    .Options(500, 350);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lati, longi)).zoom(15).build();
            snapShotOptions.withCameraPosition(cameraPosition);
            MapSnapshotter mapSnapshotter = new MapSnapshotter(NewBolzerActivity.this
                    , snapShotOptions);
            mapSnapshotter.start(new MapSnapshotter.SnapshotReadyCallback() {
                @Override
                public void onSnapshotReady(MapSnapshot snapshot) {
                    createMapBitmapAndUpload(snapshot, map.get(KEY_ID).toString(), map);
                }
            });
        }
    }

    private void createMapBitmapAndUpload(MapSnapshot snapshot, final String randomId, final Map<String, Object> map) {

        Bitmap mapImg = snapshot.getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mapImg.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        final StorageReference ref = mStorageRef.child("location_pictures/" + randomId);

        UploadTask uploadTask = ref.putBytes(data);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    uploadToFirebase(task, map, randomId);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext()
                            , getString(R.string.error_occurred)
                            , Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadToFirebase(Task<Uri> task, final Map<String, Object> map, final String randomId) {
        Uri downloadUri = task.getResult();
        map.put(KEY_DOWNLOAD_URL, downloadUri.toString());
        final Map<String, Object> mapMember = new HashMap<>();
        mapMember.put("name", map.get(KEY_CREATOR_NAME_AND_EMAIL));
        mapMember.put("confirmed", true);
        database.collection(COLLECTION_LOCATIONS)
                .document(randomId).set(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        database.collection(COLLECTION_LOCATIONS).document(randomId)
                                .collection("member").document(firebaseUser.getUid())
                                .set(mapMember).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(getApplicationContext()
                                        , getString(R.string.new_bolzer_published)
                                        , Toast.LENGTH_SHORT).show();
                                setAlarm(map);
                                database.collection(COLLECTION_USERS).document(firebaseAuth.getCurrentUser().getUid())
                                        .update("bolzers_created", FieldValue.increment(1));
                                progressDialog.dismiss();
                                onBackPressed();
                            }
                        });
                    }
                });

    }

    private void setAlarm(Map<String, Object> map) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.putExtra(KEY_ID, map.get(KEY_ID).toString());
        alarmIntent.putExtra(KEY_TITLE, map.get(KEY_TITLE).toString());
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(getApplicationContext()
                , 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP
                , alarmCalender.getTimeInMillis()-30*60*1000, alarmPendingIntent);

    }

    /**
     * Checks whether all the input-fields have a valid input
     * @return validation of input
     */
    private boolean isValidInput(String bolzerTitle, String bolzerCountry
            , String bolzerCity, String bolzerPostal, String bolzerLocation) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyyHH.mm");
        Date d = null;
        try {
            d = sdf.parse(btnDate.getText().toString()+btnTime.getText().toString());
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
        }

        if (bolzerTitle.isEmpty() || bolzerCountry.isEmpty() || bolzerCity.isEmpty()
                || bolzerPostal.isEmpty() || bolzerLocation.isEmpty()) {
            Toast.makeText(getApplicationContext(), getString(R.string.fill_all_fields),
                    Toast.LENGTH_LONG).show();
            return false;

        } else if (Integer.parseInt(ageFrom.getSelectedItem().toString())
                > Integer.parseInt(ageTo.getSelectedItem().toString())) {
            Toast.makeText(getApplicationContext()
                    , "Das Alter \"von\" muss kleiner als das Alter \"bis\" sein."
                    , Toast.LENGTH_LONG).show();
            return false;

        } else if (d.before(Calendar.getInstance().getTime())){
            Toast.makeText(getApplicationContext()
                    , "Datum und Uhrzeit müssen in der Zukunft liegen"
                    , Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }

    }

    private void fillKnownObjects() {
        database.collection(COLLECTION_USERS).document(firebaseUser.getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot ds = task.getResult();
                        String nameStr = ds.getString(KEY_FULLNAME);
                        name.setText(nameStr);
                    }
                });
    }

    private void getDate() {
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(NewBolzerActivity.this, R.style.DialogTheme,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        String date = dayOfMonth + "." + (monthOfYear + 1) + "." + year;
                        alarmCalender.set(Calendar.YEAR, year);
                        alarmCalender.set(Calendar.MONTH, monthOfYear);
                        alarmCalender.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        btnDate.setTextColor(Color.parseColor("#000000"));
                        btnDate.setText(date);

                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }

    private void getTime() {
        // Get Current Time
        final Calendar c = Calendar.getInstance();
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);

        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(NewBolzerActivity.this, R.style.DialogTheme,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay,
                                          int minute) {
                        String hour = String.valueOf(hourOfDay);
                        String min = String.valueOf(minute);
                        if (hourOfDay < 10) {
                            hour = "0"+hourOfDay;
                        } else if (minute < 10) {
                            min = "0"+minute;
                        }
                        alarmCalender.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        alarmCalender.set(Calendar.MINUTE, minute);
                        String time = hour+"."+min;
                        btnTime.setTextColor(Color.parseColor("#000000"));
                        btnTime.setText(time);
                    }
                }, mHour, mMinute, true);
        timePickerDialog.show();
    }

    private void pickLocation() {
        Intent intent = new Intent(NewBolzerActivity.this, LocationPickerActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * generates a random id out of big and small letters
     * @return a random string id
     */
    public String randomID() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int length = 16;
        char tempChar;
        for (int i = 0; i < length / 2; i++) {
            tempChar = (char) (generator.nextInt(90 - 65) + 65);
            randomStringBuilder.append(tempChar);
            tempChar = (char) (generator.nextInt(122 - 97) + 97);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    private void initializeActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        database = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
    }
    private void initializeObjects() {
        title = findViewById(R.id.title);
        country = findViewById(R.id.country);
        city = findViewById(R.id.city);
        postalcode = findViewById(R.id.postalcode);
        location = findViewById(R.id.location);
        name = findViewById(R.id.name);

        location.setEnabled(false);
        name.setEnabled(false);
        country.setEnabled(false);
        city.setEnabled(false);
        postalcode.setEnabled(false);

        btnPickLocation = findViewById(R.id.pickAdress);
        btnPickLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickLocation();
            }
        });

        ageFrom = findViewById(R.id.spAgeGroupFrom);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(this, R.array.age_numbers, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ageFrom.setAdapter(adapter);
        ageTo = findViewById(R.id.spAgeGroupTo);
        ageTo.setAdapter(adapter);

        btnDate = findViewById(R.id.btnDate);
        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate();
            }
        });
        btnTime = findViewById(R.id.btnTime);
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTime();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_bolzer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_publish_bolzer:
                publishNewBolzer();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                Bundle bundle = data.getExtras();
                String lati = bundle.get("latitude").toString();
                String longi = bundle.get("longitude").toString();
                LatLng latLng = new LatLng(Double.parseDouble(lati), Double.parseDouble(longi));
                String strLocation = latLng.getLatitude() + ", " + latLng.getLongitude();
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(latLng.getLatitude()
                            , latLng.getLongitude(), 1);
                    Address address = addresses.get(0);
                    postalcode.setText(address.getPostalCode());
                    city.setText(address.getLocality());
                    country.setText(address.getCountryName());
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Location konnte keiner " +
                            "Addresse zugeordnet werden!", Toast.LENGTH_LONG).show();
                }
                location.setText(strLocation);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), getString(R.string.no_data_recieved)
                        , Toast.LENGTH_SHORT).show();
            }
        }
    }
}
