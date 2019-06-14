package com.teapps.bolzer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.teapps.bolzer.QRCodeScanActivtiy;
import com.teapps.bolzer.R;
import com.teapps.bolzer.helper.BolzerCardItem;
import com.teapps.bolzer.helper.BolzerCardItemAdapter;
import com.teapps.bolzer.helper.BolzerDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class MyBolzersFragment extends Fragment implements AdapterView.OnItemClickListener {

    private GridView gridView;
    private FirebaseFirestore database;
    private FirebaseAuth auth;

    private List<BolzerCardItem> list = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private BolzerCardItemAdapter bolzerCardItemAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_my_bolzers, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initializeGridViewAndAdapter(view);
        initSwipeRefreshView(view);
        getListData();

    }

    private void initSwipeRefreshView(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.primaryColor);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getListData();
                bolzerCardItemAdapter.notifyDataSetChanged();
                gridView.invalidateViews();
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_my_bolzers_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void initializeGridViewAndAdapter(View view) {
        bolzerCardItemAdapter = new BolzerCardItemAdapter(getActivity(), list);
        gridView = view.findViewById(R.id.my_bolzer_grid_view);
        gridView.setSelector(android.R.color.transparent);
        gridView.setAdapter(bolzerCardItemAdapter);
        gridView.setOnItemClickListener(this);
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    private void getListData() {
        database.collection(getString(R.string.COLLECTION_USERS)).document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String nameValue = auth.getCurrentUser().getEmail()+"#"+documentSnapshot
                        .getString(getString(R.string.KEY_FULLNAME))+"#"+auth.getCurrentUser().getUid();
                        database.collection(getString(R.string.COLLECTION_LOCATIONS))
                                .whereEqualTo(getString(R.string.KEY_CREATOR_INFO), nameValue)
                                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                getData(queryDocumentSnapshots);
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
            }
        });
    }

    private void getData(QuerySnapshot queryDocumentSnapshots) {
        list.clear();
        for (final DocumentSnapshot ds : queryDocumentSnapshots) {
            String address = ds.getString(getString(R.string.KEY_POSTALCODE))
                    + " " + ds.getString(getString(R.string.KEY_CITY));
            BolzerCardItem newItem = new BolzerCardItem(ds.getString(getString(R.string.KEY_DOWNLOAD_URL))
                    , ds.getString(getString(R.string.KEY_TITLE))
                    , address, ds.getString(getString(R.string.KEY_ID)));
            list.add(newItem);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object o = gridView.getItemAtPosition(position);
        BolzerCardItem item = (BolzerCardItem) o;
        database.collection(getString(R.string.COLLECTION_LOCATIONS)).document(item.getID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();
                getDataAndOpenDialog(ds);
            }
        });
    }

    private void getDataAndOpenDialog(final DocumentSnapshot doc) {
        final String latiLongi = doc.getGeoPoint("location").getLatitude() + "#"
                + doc.getGeoPoint("location").getLongitude();

        database.collection(getString(R.string.COLLECTION_USERS)).document(auth.getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();

                String userFullName = ds.getString(getString(R.string.KEY_FULLNAME));
                BolzerCardItem bolzerCardItem = new BolzerCardItem(doc.getString(getString(R.string.KEY_DOWNLOAD_URL))
                        , doc.getString(getString(R.string.KEY_TITLE)), doc.getString(getString(R.string.KEY_POSTALCODE))
                        + " " + doc.getString(getString(R.string.KEY_CITY)), doc.getId(), doc.getString(getString(R.string.KEY_CREATOR_INFO))
                        , doc.getString("date"), doc.getString("time"), doc.getString(getString(R.string.KEY_MEMBER_LIST))
                        , doc.getString(getString(R.string.KEY_AGE_GROUP)), latiLongi);

                BolzerDialogFragment fragment = BolzerDialogFragment.newInstance();
                fragment.setArguments(bolzerCardItem, false, userFullName);
                fragment.show(getActivity().getSupportFragmentManager(), "tag");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_camera:
                Intent camerInt = new Intent(getActivity(), QRCodeScanActivtiy.class);
                startActivity(camerInt);
        }

        return super.onOptionsItemSelected(item);
    }
}
