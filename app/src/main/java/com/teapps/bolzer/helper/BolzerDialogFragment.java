package com.teapps.bolzer.helper;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.WriterException;
import com.squareup.picasso.Picasso;
import com.teapps.bolzer.R;

import java.util.Calendar;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import static com.teapps.bolzer.helper.Constants.COLLECTION_LOCATIONS;
import static com.teapps.bolzer.helper.Constants.KEY_CITY;
import static com.teapps.bolzer.helper.Constants.KEY_DATE;
import static com.teapps.bolzer.helper.Constants.KEY_ID;
import static com.teapps.bolzer.helper.Constants.KEY_POSTALCODE;
import static com.teapps.bolzer.helper.Constants.KEY_TIME;
import static com.teapps.bolzer.helper.Constants.KEY_TITLE;

public class BolzerDialogFragment extends DialogFragment implements View.OnClickListener {

    private CallBack callBack;
    private String title, address,  members, ageGroup, creatorName, userFullName, id, datetime;
    private ImageView imgMap;
    private TextView tvTitle, tvAddress, tvMembers, tvAgegroup, btnAccept, tvDateAndTime;
    private String latiLongi;
    private String downloadURL = "";

    private ScrollView scrollView;

    private ImageButton btnTicket;

    private boolean isTicketView = false;

    ListView listView;
    ArrayAdapter<String> arrayAdapter;

    FirebaseAuth auth;
    FirebaseFirestore db;

    public static BolzerDialogFragment newInstance() {
        return new BolzerDialogFragment();
    }

    public void setStrings(String downloadURL, String title, String address, String members
            , String ageGroup, String creatorName, String userFullName, String id, String datetime
            , String latiLongi, boolean isTicketView) {
        this.downloadURL = downloadURL;
        this.title = title;
        this.address = address;
        this.members = members;
        this.ageGroup = ageGroup;
        this.creatorName = creatorName;
        this.userFullName = userFullName;
        this.id = id;
        this.datetime = datetime;
        this.latiLongi = latiLongi;
        this.isTicketView = isTicketView;
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullscreenDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_bolzer_dialog, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeObjectsAndFill(view);
        String[] memberArr = tvMembers.getText().toString().split(",");
        for (int i = 0; i < memberArr.length; i++) {
            if (memberArr[i].equals(userFullName)) {
                btnAccept.setVisibility(View.INVISIBLE);
            }
        }
        scrollView = view.findViewById(R.id.bolzer_dialog_Scrollview);
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, 0);
            }
        }, 100L);
        return view;
    }

    private void initializeObjectsAndFill(View view) {

        tvTitle = view.findViewById(R.id.tvTitle);
        tvMembers = view.findViewById(R.id.tvBolzers);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvAgegroup = view.findViewById(R.id.tvAgeGroup);
        imgMap = view.findViewById(R.id.imgMap);
        imgMap.setOnClickListener(this);
        tvDateAndTime = view.findViewById(R.id.tvDateTime);
        btnTicket = view.findViewById(R.id.btnShowTicket);

        tvTitle.setText(title);
        tvMembers.setText(members);
        tvAddress.setText(address);
        tvAgegroup.setText(ageGroup);
        tvDateAndTime.setText(datetime);

        ImageButton btnClose = view.findViewById(R.id.btnDialogClose);
        btnAccept = view.findViewById(R.id.btnDialogAccept);

        Picasso.get().load(downloadURL).into(imgMap);

        if (isTicketView) {
            btnAccept.setVisibility(View.INVISIBLE);
            btnTicket.setVisibility(View.VISIBLE);
            btnTicket.setOnClickListener(this);
        }

        btnClose.setOnClickListener(this);
        btnAccept.setOnClickListener(this);

        arrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_row_members, R.id.list_row_text
                , members.split(","));
        listView =  view.findViewById(R.id.listViewMembers);
        listView.setAdapter(arrayAdapter);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnDialogClose:
                dismiss();
                break;
            case R.id.btnDialogAccept:
                callBack.onActionClicked(id);
                dismiss();
                break;
            case R.id.imgMap:
                Uri gmmIntentUri = Uri.parse("google.navigation:q="
                        + latiLongi.split("#")[0]+","+latiLongi.split("#")[1]);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            case R.id.btnShowTicket:
                showTicket();
        }
    }

    private void showTicket() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getLayoutInflater();
        View view1 = layoutInflater.inflate(R.layout.dialog_ticket_layout, null);
        TextView tvTitle, tvID, tvAddress, tvDate, tvTime;
        tvTitle = view1.findViewById(R.id.tvTitle);
        tvID = view1.findViewById(R.id.tvID);
        tvAddress = view1.findViewById(R.id.tvAddress);
        tvDate = view1.findViewById(R.id.tvDate);
        tvTime = view1.findViewById(R.id.tvTime);
        ImageView imgQrCode = view1.findViewById(R.id.imageView8);
        builder.setView(view1);

        fillDataToDialogFields(tvTitle, tvID, tvAddress, tvTime, tvDate, imgQrCode);

        Dialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

    }

    private void fillDataToDialogFields(final TextView tvTitle, final TextView tvID, final TextView tvAddress, final TextView tvTime, final TextView tvDate, final ImageView imgQrCode) {
        db.collection(COLLECTION_LOCATIONS).document(id).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot ds = task.getResult();
                        tvTitle.setText(ds.getString(KEY_TITLE));
                        tvID.setText(ds.getString(KEY_ID));
                        tvAddress.setText(ds.getString(KEY_POSTALCODE) + " "
                                + ds.getString(KEY_CITY));
                        tvTime.setText(ds.getString(KEY_TIME));
                        tvDate.setText(ds.getString(KEY_DATE));

                        String qrString = ds.getString(KEY_ID) + "#" + auth.getCurrentUser().getUid();

                        QRGEncoder qrgEncoder = new QRGEncoder(qrString, null, QRGContents.Type.TEXT, 450);
                        try {
                            imgQrCode.setImageBitmap(qrgEncoder.encodeAsBitmap());
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public interface CallBack {
        public void onActionClicked(String name);
    }

}
