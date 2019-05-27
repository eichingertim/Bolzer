package com.teapps.bolzer.helper;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.WriterException;
import com.squareup.picasso.Picasso;
import com.teapps.bolzer.R;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class BolzerDialogFragment extends DialogFragment implements View.OnClickListener {

    private CallBack callBack;
    private String userFullName;
    private ImageView imgMap;
    private TextView tvTitle, tvAddress, tvMembers, tvAgegroup, btnAccept, tvDateAndTime;

    private ScrollView scrollView;

    private ImageButton btnTicket;

    private boolean isTicketView = false;

    ListView listView;
    ArrayAdapter<String> arrayAdapter;

    BolzerCardItem bolzerCardItem;

    FirebaseAuth auth;
    FirebaseFirestore db;

    public static BolzerDialogFragment newInstance() {
        return new BolzerDialogFragment();
    }

    public void setArguments(BolzerCardItem bolzerCardItem, boolean isTicketView, String userFullName) {
        this.bolzerCardItem = bolzerCardItem;
        this.isTicketView = isTicketView;
        this.userFullName = userFullName;
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

        tvTitle.setText(bolzerCardItem.getTitle());
        tvMembers.setText(bolzerCardItem.getMembers());
        tvAddress.setText(bolzerCardItem.getAddress());
        tvAgegroup.setText(bolzerCardItem.getAgeGroup());
        tvDateAndTime.setText("Am " + bolzerCardItem.getDate() + " um " + bolzerCardItem.getTime());

        ImageButton btnClose = view.findViewById(R.id.btnDialogClose);
        btnAccept = view.findViewById(R.id.btnDialogAccept);

        Picasso.get().load(bolzerCardItem.getMapURL()).into(imgMap);

        if (isTicketView) {
            btnAccept.setVisibility(View.INVISIBLE);
            btnTicket.setVisibility(View.VISIBLE);
            btnTicket.setOnClickListener(this);
        }

        btnClose.setOnClickListener(this);
        btnAccept.setOnClickListener(this);

        arrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_row_members, R.id.list_row_text
                , bolzerCardItem.getMembers().split(","));
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
                callBack.onActionClicked(bolzerCardItem.getID());
                dismiss();
                break;
            case R.id.imgMap:
                Uri gmmIntentUri = Uri.parse("google.navigation:q="
                        + bolzerCardItem.getLocation().split("#")[0]+","
                        +bolzerCardItem.getLocation().split("#")[1]);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
                break;
            case R.id.btnShowTicket:
                showTicket();
                break;
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
        tvTitle.setText(bolzerCardItem.getTitle());
        tvID.setText(bolzerCardItem.getID());
        tvAddress.setText(bolzerCardItem.getAddress());
        tvTime.setText(bolzerCardItem.getTime());
        tvDate.setText(bolzerCardItem.getDate());

        String qrString = bolzerCardItem.getID() + "#" + auth.getCurrentUser().getUid();

        QRGEncoder qrgEncoder = new QRGEncoder(qrString, null, QRGContents.Type.TEXT, 450);
        try {
            imgQrCode.setImageBitmap(qrgEncoder.encodeAsBitmap());
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    public interface CallBack {
        public void onActionClicked(String name);
    }

}
