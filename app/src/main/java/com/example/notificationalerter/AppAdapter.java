package com.example.notificationalerter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppAdapter extends ArrayAdapter<AppInfo> {

    private LayoutInflater inflater;
    private List<AppInfo> appList;
    private OnCheckedChangeListener listener;

    public AppAdapter(Context context, List<AppInfo> appList) {
        super(context, 0, appList);
        this.appList = appList;
        inflater = LayoutInflater.from(context);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_app, parent, false);
            holder = new ViewHolder();
            holder.iconImageView = convertView.findViewById(R.id.icon_image_view);
            holder.nameTextView = convertView.findViewById(R.id.name_text_view);
            holder.checkBox = convertView.findViewById(R.id.checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final AppInfo appInfo = appList.get(position);

        holder.iconImageView.setImageDrawable(appInfo.getIcon());
        holder.nameTextView.setText(appInfo.getName());

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(appInfo.isChecked());
        holder.checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            appInfo.setChecked(isChecked);
            if (listener != null) {
                listener.onCheckedChange(appInfo.getPackageName(), isChecked);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        CheckBox checkBox;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChange(String packageName, boolean isChecked);
    }
}
