package com.teapps.bolzer.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.teapps.bolzer.R;
import com.teapps.bolzer.helper.BolzerDialogFragment;
import com.teapps.bolzer.services.AlarmReceiver;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.ALARM_SERVICE;
import static com.teapps.bolzer.helper.Constants.COLLECTION_LOCATIONS;
import static com.teapps.bolzer.helper.Constants.COLLECTION_USERS;
import static com.teapps.bolzer.helper.Constants.FIELD_LOCATION;
import static com.teapps.bolzer.helper.Constants.KEY_AGE_GROUP;
import static com.teapps.bolzer.helper.Constants.KEY_CITY;
import static com.teapps.bolzer.helper.Constants.KEY_DATE;
import static com.teapps.bolzer.helper.Constants.KEY_DOWNLOAD_URL;
import static com.teapps.bolzer.helper.Constants.KEY_FULLNAME;
import static com.teapps.bolzer.helper.Constants.KEY_ID;
import static com.teapps.bolzer.helper.Constants.KEY_LOCATION;
import static com.teapps.bolzer.helper.Constants.KEY_MEMBERS;
import static com.teapps.bolzer.helper.Constants.KEY_CREATOR_NAME_AND_EMAIL;
import static com.teapps.bolzer.helper.Constants.KEY_MEMBERS_ID;
import static com.teapps.bolzer.helper.Constants.KEY_POSTALCODE;
import static com.teapps.bolzer.helper.Constants.KEY_TIME;
import static com.teapps.bolzer.helper.Constants.KEY_TITLE;

public class MapFragment extends Fragment implements OnMapReadyCallback,
        BolzerDialogFragment.CallBack, MapboxMap.OnInfoWindowClickListener {

    private MapView mapView;
    private MapboxMap map;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private Bundle savedInstanceState;

    private Calendar alarmCalender;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setHasOptionsMenu(true);
        Mapbox.getInstance(getActivity(), getString(R.string.api_key));
        initializeFirebase();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = mainView.findViewById(R.id.mapView);
        mapView.onSaveInstanceState(savedInstanceState);
        mapView.getMapAsync(this);
        return mainView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_map_fragment, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearching(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        map = mapboxMap;
        map.setStyle(Style.OUTDOORS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationPlugin(style);
                database.collection(COLLECTION_LOCATIONS)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots
                                    , @javax.annotation.Nullable FirebaseFirestoreException e) {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    if (doc.get(KEY_CREATOR_NAME_AND_EMAIL) != null) {

                                        LatLng location = new LatLng(
                                                doc.getGeoPoint(FIELD_LOCATION).getLatitude(),
                                                doc.getGeoPoint(FIELD_LOCATION).getLongitude());

                                        map.addMarker(new MarkerOptions().position(location)
                                                .title(doc.getString(KEY_TITLE)));
                                    }
                                }
                            }
                        });
            }
        });
        map.setOnInfoWindowClickListener(this);
    }

    private boolean MarkerPopUp(final Marker marker) {
        final GeoPoint geoPoint = new GeoPoint(marker.getPosition().getLatitude()
                , marker.getPosition().getLongitude());
        database.collection(COLLECTION_LOCATIONS).whereEqualTo(KEY_LOCATION
                , geoPoint).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc : task.getResult()) {
                    if (doc.getGeoPoint(KEY_LOCATION).equals(geoPoint)) {
                        getDataAndSendToDialog(doc, marker);
                        break;
                    }
                }
            }
        });
        return true;
    }

    private void getDataAndSendToDialog(DocumentSnapshot doc, final Marker marker) {
        final String title = doc.getString(KEY_TITLE);
        final String members = doc.getString(KEY_MEMBERS);
        final String address = doc.getString(KEY_POSTALCODE)
                + " " + doc.getString(KEY_CITY);
        final String ageGroup = doc.getString(KEY_AGE_GROUP);
        final String creatorName = doc.getString(KEY_CREATOR_NAME_AND_EMAIL).split("#")[1];
        final String id = doc.getId();
        final String datetime = "Am " + doc.getString("date") + " um "
                + doc.getString("time") + " Uhr";
        final String downloadURL = doc.getString(KEY_DOWNLOAD_URL);
        database.collection(COLLECTION_USERS).document(firebaseAuth.getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                String latiLongi = marker.getPosition().getLatitude() + "#"
                        + marker.getPosition().getLongitude();
                String userFullName = ds.getString(KEY_FULLNAME);
                DialogFragment fragment = BolzerDialogFragment.newInstance();
                ((BolzerDialogFragment) fragment).setStrings(downloadURL, title, address
                        , members, ageGroup, creatorName, userFullName, id, datetime
                        , latiLongi, false);
                ((BolzerDialogFragment) fragment).setCallBack(MapFragment.this);
                fragment.show(getActivity().getSupportFragmentManager(), "tag");
            }
        });
    }

    @Override
    public void onActionClicked(final String id) {
        Map<String, Object> map = new HashMap<>();
        map.put("confirmed", false);
        database.collection(COLLECTION_LOCATIONS).document(id)
                .collection("member").document(firebaseAuth.getCurrentUser().getUid())
                .set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateMember(id);
            }
        });
    }

    private void updateMember(final String id) {
        database.collection(COLLECTION_LOCATIONS).document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                String[] members = ds.getString(KEY_MEMBERS).split(",");
                String[] members_id = ds.getString(KEY_MEMBERS_ID).split(",");
                final String dateString = ds.getString(KEY_DATE) + ds.getString(KEY_TIME);
                final String title = ds.getString(KEY_TITLE);
                final ArrayList<String> memberList = new ArrayList<>(Arrays.asList(members));
                final ArrayList<String> memberIDList = new ArrayList<>(Arrays.asList(members_id));
                database.collection(COLLECTION_USERS).document(firebaseAuth.getCurrentUser().getUid())
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        getAndUpdateMemberField(task, memberList, memberIDList, id, dateString, title);
                    }
                });

            }
        });
    }

    private void getAndUpdateMemberField(Task<DocumentSnapshot> task, ArrayList<String> memberList
            , ArrayList<String> memberIDList, final String id, final String dateString, final String title) {
        DocumentSnapshot docS = task.getResult();
        memberList.add(docS.getString(KEY_FULLNAME));
        memberIDList.add(firebaseAuth.getCurrentUser().getUid());

        StringBuilder sb = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        for (String s : memberList) {
            sb.append(s).append(",");
        }
        for (String s : memberIDList) {
            sb1.append(s).append(",");
        }
        String firebaseMembers = sb.deleteCharAt(sb.length() - 1).toString();
        String firebaseMembersID = sb1.deleteCharAt(sb1.length() - 1).toString();

        Map<String, Object> update = new HashMap<>();
        update.put(KEY_MEMBERS, firebaseMembers);
        update.put(KEY_MEMBERS_ID, firebaseMembersID);

        database.collection(COLLECTION_LOCATIONS).document(id)
                .update(update).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setAlarm(dateString, id, title);
                Toast.makeText(getActivity(), "Erfolgreich zugesagt.", Toast.LENGTH_LONG)
                        .show();

            }
        });
    }

    private void setAlarm(String dateString, String id, String title) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyyHH.mm");
        Date d = null;
        try {
            d = sdf.parse(dateString);
        } catch (ParseException ex) {
            Log.v("Exception", ex.getLocalizedMessage());
        }
        alarmCalender.setTime(d);
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(getActivity(), AlarmReceiver.class);
        alarmIntent.putExtra(KEY_ID, id);
        alarmIntent.putExtra(KEY_TITLE, title);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(getActivity()
                , 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP
                , alarmCalender.getTimeInMillis()-30*60*1000, alarmPendingIntent);

    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void onSearching(final String query) {
        database.collection(COLLECTION_LOCATIONS).whereEqualTo(KEY_POSTALCODE, query)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc : task.getResult()) {
                    Toast.makeText(getActivity(), doc.getString(KEY_TITLE)
                            , Toast.LENGTH_SHORT).show();
                }
                Geocoder geocoder = new Geocoder(getActivity());
                try {
                    List<Address> addressList = geocoder.getFromLocationName(query, 1);
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        map.animateCamera(CameraUpdateFactory
                                .newLatLngZoom(new LatLng(address.getLatitude()
                                        , address.getLongitude()), 12));
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.unable_get_zipcode)
                                , Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(getActivity())) {

            LocationComponent locationComponent = map.getLocationComponent();
            locationComponent.activateLocationComponent(getActivity(), loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);

            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);

        }
    }

    @Override
    public boolean onInfoWindowClick(@NonNull Marker marker) {
        return MarkerPopUp(marker);
    }
}
