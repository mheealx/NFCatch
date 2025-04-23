package com.esime.nfcdroid2.ui.informacion.componentes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.esime.nfcdroid2.R;

import java.util.List;

public class InfoAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> titles;
    private final List<String> values;

    public InfoAdapter(Context context, List<String> titles, List<String> values) {
        this.context = context;
        this.titles = titles;
        this.values = values;
    }

    @Override
    public int getCount() {
        return titles.size();
    }

    @Override
    public Object getItem(int position) {
        return titles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_info, parent, false);

        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView valueTextView = view.findViewById(R.id.valueTextView);

        titleTextView.setText(titles.get(position));
        valueTextView.setText(values.get(position));

        return view;
    }
}
