package com.abdurazaaqmohammed.AntiSplit.main;

import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.abdurazaaqmohammed.AntiSplit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppListArrayAdapter extends ArrayAdapter<AppInfo> implements Filterable {
    private static final LruCache<String, Drawable> ICON_CACHE =
            new LruCache<String, Drawable>(8 * 1024) {
                @Override
                protected int sizeOf(String key, Drawable value) {
                    int width = Math.max(1, value.getIntrinsicWidth());
                    int height = Math.max(1, value.getIntrinsicHeight());
                    return Math.max(1, width * height * 4 / 1024);
                }
            };

    private final MainActivity context;
    private final List<AppInfo> originalAppInfoList = new ArrayList<>();
    public final List<AppInfo> filteredAppInfoList = new ArrayList<>();
    private final boolean showIcon;
    private AppInfoFilter filter;

    public AppListArrayAdapter(MainActivity context, List<AppInfo> appInfoList, boolean showIcon) {
        super(context, R.layout.list_item, appInfoList);
        this.context = context;
        this.showIcon = showIcon;
        setItems(appInfoList);
    }

    public void setItems(List<AppInfo> appInfoList) {
        originalAppInfoList.clear();
        originalAppInfoList.addAll(appInfoList);
        filteredAppInfoList.clear();
        filteredAppInfoList.addAll(appInfoList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredAppInfoList.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return filteredAppInfoList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

        TextView textView = convertView.findViewById(R.id.text_view);
        TextView packageView = convertView.findViewById(R.id.package_view);
        ImageView iconView = convertView.findViewById(R.id.icon_view);

        AppInfo appInfo = getItem(position);
        textView.setText(appInfo.name);
        if (packageView != null)
            packageView.setText(appInfo.packageName);

        if (showIcon) {
            iconView.setVisibility(View.VISIBLE);
            Drawable icon = ICON_CACHE.get(appInfo.packageName);
            if (icon == null) {
                try {
                    icon = context.getPackageManager().getApplicationIcon(appInfo.packageName);
                    ICON_CACHE.put(appInfo.packageName, icon);
                } catch (Exception ignored) {
                }
            }
            iconView.setImageDrawable(icon);
        } else {
            iconView.setImageDrawable(null);
            iconView.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new AppInfoFilter();
        return filter;
    }

    @SuppressWarnings("unchecked")
    private class AppInfoFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                results.values = new ArrayList<>(originalAppInfoList);
                results.count = originalAppInfoList.size();
            } else {
                List<AppInfo> filteredItems = new ArrayList<>();
                String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
                for (AppInfo appInfo : originalAppInfoList) {
                    if (appInfo.name.toLowerCase(Locale.ROOT).contains(filterPattern)
                            || appInfo.packageName.toLowerCase(Locale.ROOT).contains(filterPattern)) {
                        filteredItems.add(appInfo);
                    }
                }
                results.values = filteredItems;
                results.count = filteredItems.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredAppInfoList.clear();
            filteredAppInfoList.addAll((List<AppInfo>) results.values);
            notifyDataSetChanged();
        }
    }
}
