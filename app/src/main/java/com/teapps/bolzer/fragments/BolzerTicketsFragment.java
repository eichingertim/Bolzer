package com.teapps.bolzer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
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
import com.teapps.bolzer.R;
import com.teapps.bolzer.helper.BolzerCardItem;
import com.teapps.bolzer.helper.BolzerCardItemAdapter;
import com.teapps.bolzer.helper.BolzerDialogFragment;

import java.util.ArrayList;
import java.util.List;

import static com.teapps.bolzer.helper.Constants.COLLECTION_LOCATIONS;
import static com.teapps.bolzer.helper.Constants.COLLECTION_USERS;
import static com.teapps.bolzer.helper.Constants.KEY_AGE_GROUP;
import static com.teapps.bolzer.helper.Constants.KEY_CITY;
import static com.teapps.bolzer.helper.Constants.KEY_DOWNLOAD_URL;
import static com.teapps.bolzer.helper.Constants.KEY_FULLNAME;
import static com.teapps.bolzer.helper.Constants.KEY_ID;
import static com.teapps.bolzer.helper.Constants.KEY_MEMBERS;
import static com.teapps.bolzer.helper.Constants.KEY_CREATOR_NAME_AND_EMAIL;
import static com.teapps.bolzer.helper.Constants.KEY_POSTALCODE;
import static com.teapps.bolzer.helper.Constants.KEY_TITLE;

public class BolzerTicketsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private GridView gridView;

    private FirebaseFirestore database;
    private FirebaseAuth auth;

    private BolzerCardItemAdapter bolzerCardItemAdapter;

    private List<BolzerCardItem> list = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_other_bolzer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeFirebase();
        initializeGridView(view);
        initSwipeRefreshLayout(view);
        getListData();

    }

    private void initSwipeRefreshLayout(View view) {
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

    private void initializeGridView(View view) {
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
        database.collection(COLLECTION_USERS).document(auth.getCurrentUser().getUid())
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                final String nameValue = documentSnapshot.getString(KEY_FULLNAME);
                database.collection(COLLECTION_LOCATIONS).get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        getData(queryDocumentSnapshots, nameValue);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void getData(QuerySnapshot queryDocumentSnapshots, String nameValue) {
        list.clear();
        for (final DocumentSnapshot ds : queryDocumentSnapshots) {
            if (ds.getString(KEY_MEMBERS).contains(nameValue)
                    && !ds.getString(KEY_CREATOR_NAME_AND_EMAIL).contains(nameValue)) {
                String address = ds.getString(KEY_POSTALCODE) + " " + ds.getString(KEY_CITY);
                BolzerCardItem newItem = new BolzerCardItem(ds.getString(KEY_DOWNLOAD_URL), ds.getString(KEY_TITLE)
                        , address, ds.getString(KEY_ID));
                list.add(newItem);
            }

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Object o = gridView.getItemAtPosition(position);
        BolzerCardItem item = (BolzerCardItem) o;
        database.collection(COLLECTION_LOCATIONS).document(item.getID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
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

        database.collection(COLLECTION_USERS).document(auth.getCurrentUser().getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot ds = task.getResult();

                String userFullName = ds.getString(KEY_FULLNAME);

                BolzerCardItem bolzerCardItem = new BolzerCardItem(doc.getString(KEY_DOWNLOAD_URL)
                        , doc.getString(KEY_TITLE), doc.getString(KEY_POSTALCODE)
                        + " " + doc.getString(KEY_CITY), doc.getId(), doc.getString(KEY_CREATOR_NAME_AND_EMAIL)
                        , doc.getString("date"), doc.getString("time"), doc.getString(KEY_MEMBERS)
                        , doc.getString(KEY_AGE_GROUP), latiLongi);

                BolzerDialogFragment fragment = BolzerDialogFragment.newInstance();
                fragment.setArguments(bolzerCardItem,true, userFullName);
                fragment.show(getActivity().getSupportFragmentManager(), "tag");
            }
        });

    }

}