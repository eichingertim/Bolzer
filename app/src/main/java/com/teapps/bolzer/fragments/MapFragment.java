package com.teapps.bolzer.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.AdapterView;
import android.widget.ListView;
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
import com.teapps.bolzer.MainActivity;
import com.teapps.bolzer.NewBolzerActivity;
import com.teapps.bolzer.R;
import com.teapps.bolzer.helper.BolzerCardItem;
import com.teapps.bolzer.helper.BolzerDialogFragment;
import com.teapps.bolzer.helper.BolzerListItemAdapter;
import com.teapps.bolzer.services.AlarmReceiver;

import java.io.IOException;
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

public class MapFragment extends Fragment implements OnMapReadyCallback,
        BolzerDialogFragment.CallBack, MapboxMap.OnInfoWindowClickListener,
        MainActivity.MapIconCallback, AdapterView.OnItemClickListener {

    private MapView mapView;
    private MapboxMap map;

    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private Bundle savedInstanceState;

    private ListView listView;
    private BolzerListItemAdapter adapter;
    private List<BolzerCardItem> bolzerCardItems = new ArrayList<>();

    private FloatingActionButton fab;

    private BottomSheetBehavior sheetBehavior;
    private View bottomSheet;

    private Calendar alarmCalender = Calendar.getInstance();

    private ProgressDialog progressDialog;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setHasOptionsMenu(true);
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.setMapIconCallback(this);
        Mapbox.getInstance(getActivity()
                , getString(R.string.api_key));
        initFirebase();

    }

    /*
    Initializing components of "firebase"
     */
    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFab(view);
        initBottomSheet(view);
    }

    /*
    Initializing FloatingActionButton, with the hands off on click to the "NewBolzer"-Activity
     */
    private void initializeFab(View view) {
        fab = view.findViewById(R.id.btn_profile_edit);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), NewBolzerActivity.class);
                startActivity(intent);
            }
        });
    }

    /*
    1. Initializing the bottom-sheet, its components and behavior.
    2. The adapter for the ListView in the bottom-sheet gets a List of "BolzerItems".
     */
    private void initBottomSheet(View view) {
        bottomSheet = view.findViewById(R.id.bottom_sheet);
        adapter = new BolzerListItemAdapter(getActivity(), bolzerCardItems);
        listView = view.findViewById(R.id.list_view_bottom_sheet);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        sheetBehavior.setPeekHeight(200);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (i == BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior.setPeekHeight(200);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });
    }

    /*
    Hands off to the BolzerDialogFragment when a BolzerListItem is clicked in the bottom-sheet List-
    View.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BolzerCardItem bolzerCardItem = (BolzerCardItem) adapter.getItem(position);
        database.collection(getString(R.string.COLLECTION_LOCATIONS))
                .document(bolzerCardItem.getID()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot doc = task.getResult();
                getDataAndSendToDialog(doc);
            }
        });
    }

    /*
    1. Sets the style of the map from a created URL in MapBox-Studio
    2. Sets markers onto the map where the cloud-saved bolzers are located
     */
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        map = mapboxMap;
        map.setStyle(new Style.Builder().fromUrl(getString(R.string.mapbox_mapstyle_url))
                , new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationPlugin(style);
                database.collection(getString(R.string.COLLECTION_LOCATIONS))
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots
                                    , @javax.annotation.Nullable FirebaseFirestoreException e) {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    if (doc.get(getString(R.string.KEY_CREATOR_INFO)) != null) {

                                        LatLng location = new LatLng(
                                                doc.getGeoPoint(getString(R.string.KEY_LOCATION)).getLatitude(),
                                                doc.getGeoPoint(getString(R.string.KEY_LOCATION)).getLongitude());

                                        map.addMarker(new MarkerOptions().position(location)
                                                .title(doc.getString(getString(R.string.KEY_TITLE))));
                                    }
                                }
                            }
                        });
            }
        });
        map.setOnInfoWindowClickListener(this);
    }

    @Override
    public boolean onInfoWindowClick(@NonNull Marker marker) {
        return MarkerPopUp(marker);
    }

    /*
    Handles the event when the user clicks on a MarkerPopUp
     - It opens a progressDialog and executes the "getDataAndSendToDialog"-Method
     */
    private boolean MarkerPopUp(final Marker marker) {
        final GeoPoint geoPoint = new GeoPoint(marker.getPosition().getLatitude()
                , marker.getPosition().getLongitude());
        database.collection(getString(R.string.COLLECTION_LOCATIONS)).whereEqualTo(getString(R.string.KEY_LOCATION)
                , geoPoint).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc : task.getResult()) {
                    if (doc.getGeoPoint(getString(R.string.KEY_LOCATION)).equals(geoPoint)) {
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setMessage(getString(R.string.message_loading));
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progressDialog.show();
                        getDataAndSendToDialog(doc, marker);
                        break;
                    }
                }
            }
        });
        return true;
    }

    private void getDataAndSendToDialog(final DocumentSnapshot doc, final Marker marker) {
        database.collection(getString(R.string.COLLECTION_USERS)).document(firebaseAuth.getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                String latiLongi = marker.getPosition().getLatitude() + "#"
                        + marker.getPosition().getLongitude();
                String userFullName = ds.getString(getString(R.string.KEY_FULLNAME));

                BolzerCardItem bolzerCardItem = new BolzerCardItem(doc.getString(getString(R.string.KEY_DOWNLOAD_URL))
                        , doc.getString(getString(R.string.KEY_TITLE)), doc.getString(getString(R.string.KEY_POSTALCODE))
                        + " " + doc.getString(getString(R.string.KEY_CITY)), doc.getId(), doc.getString(getString(R.string.KEY_CREATOR_INFO))
                        , doc.getString(getString(R.string.KEY_DATE)), doc.getString(getString(R.string.KEY_TIME))
                        , doc.getString(getString(R.string.KEY_MEMBER_LIST)), doc.getString(getString(R.string.KEY_AGE_GROUP)), latiLongi);

                BolzerDialogFragment fragment = BolzerDialogFragment.newInstance();
                fragment.setArguments(bolzerCardItem, false, userFullName);
                fragment.setCallBack(MapFragment.this);
                fragment.show(getActivity().getSupportFragmentManager(), "tag");
                progressDialog.dismiss();
            }
        });
    }

    private void getDataAndSendToDialog(final DocumentSnapshot doc) {
        database.collection(getString(R.string.COLLECTION_USERS)).document(firebaseAuth.getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                GeoPoint geoPoint =  doc.getGeoPoint(getString(R.string.KEY_LOCATION));
                String latiLongi = geoPoint.getLatitude() + "#"
                        + geoPoint.getLongitude();
                String userFullName = ds.getString(getString(R.string.KEY_FULLNAME));

                BolzerCardItem bolzerCardItem = new BolzerCardItem(doc.getString(getString(R.string.KEY_DOWNLOAD_URL))
                        , doc.getString(getString(R.string.KEY_TITLE)), doc.getString(getString(R.string.KEY_POSTALCODE))
                        + " " + doc.getString(getString(R.string.KEY_CITY)), doc.getId(), doc.getString(getString(R.string.KEY_CREATOR_INFO))
                        , doc.getString(doc.getString(getString(R.string.KEY_DATE)))
                        , doc.getString(getString(R.string.KEY_TIME)), doc.getString(getString(R.string.KEY_MEMBER_LIST))
                        , doc.getString(getString(R.string.KEY_AGE_GROUP)), latiLongi);

                BolzerDialogFragment fragment = BolzerDialogFragment.newInstance();
                fragment.setArguments(bolzerCardItem, false, userFullName);
                fragment.setCallBack(MapFragment.this);
                fragment.show(getActivity().getSupportFragmentManager(), "tag");
            }
        });
    }

    private void getDataFillBottomSheetListView(boolean isTitleQuery) {
        if (isTitleQuery) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            adapter.notifyDataSetChanged();
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            try {
                Geocoder geocoder = new Geocoder(getActivity());
                LatLng latLng = map.getCameraPosition().target;
                List<Address> addressList = geocoder.getFromLocation(latLng.getLatitude()
                        , latLng.getLongitude(), 1);
                if (addressList != null && !addressList.isEmpty()) {
                    Address address = addressList.get(0);
                    String postalcode = address.getPostalCode();

                    getDataFromDatabase(postalcode);
                    adapter.notifyDataSetChanged();

                } else {
                    Toast.makeText(getActivity(), getString(R.string.unable_get_zipcode)
                            , Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void getDataFromDatabase(String postalcode) {
        bolzerCardItems.clear();
        database.collection(getString(R.string.COLLECTION_LOCATIONS))
                .whereEqualTo(getString(R.string.KEY_POSTALCODE), postalcode).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        for (DocumentSnapshot doc : task.getResult()) {
                            bolzerCardItems.add(new BolzerCardItem(doc.getString(getString(R.string.KEY_DOWNLOAD_URL))
                                    , doc.getString(getString(R.string.KEY_TITLE))
                                    , doc.getString(getString(R.string.KEY_CITY))
                                    , doc.getString(getString(R.string.KEY_ID))));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActionClicked(final String id) {
        Map<String, Object> map = new HashMap<>();
        map.put(getString(R.string.KEY_CONFIRMED), false);
        database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(id)
                .collection(getString(R.string.KEY_MEMBER)).document(firebaseAuth.getCurrentUser().getUid())
                .set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateMember(id);
            }
        });
    }

    private void updateMember(final String id) {
        database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                String[] members = ds.getString(getString(R.string.KEY_MEMBER_LIST)).split(",");
                String[] members_id = ds.getString(getString(R.string.KEY_MEMBER_ID_LIST)).split(",");
                final String dateString = ds.getString(getString(R.string.KEY_DATE)) + ds.getString(getString(R.string.KEY_TIME));
                final String title = ds.getString(getString(R.string.KEY_TITLE));
                final ArrayList<String> memberList = new ArrayList<>(Arrays.asList(members));
                final ArrayList<String> memberIDList = new ArrayList<>(Arrays.asList(members_id));
                database.collection(getString(R.string.COLLECTION_USERS)).document(firebaseAuth.getCurrentUser().getUid())
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        getAndUpdateMemberField(task, memberList, memberIDList, id, dateString
                                , title);
                    }
                });

            }
        });
    }

    private void getAndUpdateMemberField(Task<DocumentSnapshot> task, ArrayList<String> memberList
            , ArrayList<String> memberIDList, final String id, final String dateString
            , final String title) {
        DocumentSnapshot docS = task.getResult();
        memberList.add(docS.getString(getString(R.string.KEY_FULLNAME)));
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
        update.put(getString(R.string.KEY_MEMBER_LIST), firebaseMembers);
        update.put(getString(R.string.KEY_MEMBER_ID_LIST), firebaseMembersID);

        database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(id)
                .update(update).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setAlarm(dateString, id, title);
                Toast.makeText(getActivity(), getString(R.string.successfully_accepted)
                        , Toast.LENGTH_LONG)
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
        alarmIntent.putExtra(getString(R.string.KEY_ID), id);
        alarmIntent.putExtra(getString(R.string.KEY_TITLE), title);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(getActivity()
                , 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP
                , alarmCalender.getTimeInMillis()-30*60*1000, alarmPendingIntent);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_map_fragment, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint(getString(R.string.search_hint_map));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearching(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void onSearching(String query) {
        Geocoder geocoder_main = new Geocoder(getActivity());
        try {
            List<Address> addressList = geocoder_main.getFromLocationName(query, 10);
            if (addressList.size() > 0) {
                searchForCityAndPostalcode(query);
            } else {
                searchForTitle(query);
            }
        } catch (IOException e) {
            e.printStackTrace();
            searchForTitle(query);
        }
    }

    private void searchForTitle(final String query) {
        database.collection(getString(R.string.COLLECTION_LOCATIONS)).whereEqualTo(getString(R.string.KEY_TITLE), query)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc : task.getResult()) {
                    bolzerCardItems.clear();
                    bolzerCardItems.add(new BolzerCardItem(doc.getString(getString(R.string.KEY_DOWNLOAD_URL))
                            , doc.getString(getString(R.string.KEY_TITLE))
                            , doc.getString(getString(R.string.KEY_CITY))
                            , doc.getString(getString(R.string.KEY_ID))));
                }
                getDataFillBottomSheetListView(true);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void searchForCityAndPostalcode(final String query) {
        database.collection(getString(R.string.COLLECTION_LOCATIONS)).whereEqualTo(getString(R.string.KEY_POSTALCODE), query)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc : task.getResult()) {
                    Toast.makeText(getActivity(), doc.getString(getString(R.string.KEY_TITLE))
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
                        getDataFillBottomSheetListView(false);
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
    public void mapIconClick() {
        getDataFillBottomSheetListView(false);
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
}
