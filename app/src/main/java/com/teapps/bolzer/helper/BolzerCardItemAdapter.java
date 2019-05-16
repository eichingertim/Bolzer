package com.teapps.bolzer.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teapps.bolzer.R;

import java.util.List;

public class BolzerCardItemAdapter extends BaseAdapter {

    private List<BolzerCardItem> dataList;
    private LayoutInflater layoutInflater;
    private Context context;

    public BolzerCardItemAdapter(Context acontext, List<BolzerCardItem> dataList) {
        this.dataList = dataList;
        context = acontext;
        layoutInflater = LayoutInflater.from(acontext);
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.bolzer_grid_card_item, null);
            holder = new ViewHolder();
            holder.mapView = convertView.findViewById(R.id.imgCardMap);
            holder.titleView = convertView.findViewById(R.id.tvBolzerTitleCard);
            holder.addressView = convertView.findViewById(R.id.tvBolzerAddressCard);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BolzerCardItem bolzerCardItem = this.dataList.get(position);
        holder.titleView.setText(bolzerCardItem.getTitle());
        holder.addressView.setText(bolzerCardItem.getAddress());
        Picasso.get().load(bolzerCardItem.getMapURL()).into(holder.mapView);

        return convertView;

    }

    static class ViewHolder {
        ImageView mapView;
        TextView titleView;
        TextView addressView;
    }

}
