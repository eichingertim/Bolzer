package com.teapps.bolzer.helper;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.teapps.bolzer.R;

import java.util.List;

public class BolzerListItemAdapter extends BaseAdapter {

    private List<BolzerCardItem> dataList;
    private LayoutInflater layoutInflater;
    private Context context;

    public BolzerListItemAdapter(Context acontext, List<BolzerCardItem> dataList) {
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
            convertView = layoutInflater.inflate(R.layout.list_row_map_bolzers, null);
            holder = new ViewHolder();
            holder.mapView = convertView.findViewById(R.id.img_bolzer_preview);
            holder.titleView = convertView.findViewById(R.id.tv_title_bolzer);
            holder.addressView = convertView.findViewById(R.id.tv_adress_bolzer);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BolzerCardItem bolzerCardItem = dataList.get(position);
        holder.titleView.setText(bolzerCardItem.getTitle());
        holder.addressView.setText(bolzerCardItem.getAddress());
        Picasso.get().load(bolzerCardItem.getMapURL()).into(holder.mapView);

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    static class ViewHolder {
        ImageView mapView;
        TextView titleView;
        TextView addressView;
    }

}
