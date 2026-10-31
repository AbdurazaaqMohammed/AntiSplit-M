package com.abdurazaaqmohammed.AntiSplit.main;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.abdurazaaqmohammed.AntiSplit.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppListArrayAdapter extends BaseAdapter implements Filterable {

    private static final int ICON_CACHE_SIZE_KB = 8 * 1024;

    private final Context context;
    private final boolean showIcon;
    private final LruCache<String, Drawable> iconCache = new LruCache<String, Drawable>(
            ICON_CACHE_SIZE_KB) {
        @Override
        protected int sizeOf(String key, Drawable value) {
            int width = Math.max(1, value.getIntrinsicWidth());
            int height = Math.max(1, value.getIntrinsicHeight());
            return Math.max(1, width * height * 4 / 1024);
        }
    };
    private final List<AppInfo> allItems = new ArrayList<>();
    private final List<AppInfo> visibleItems = new ArrayList<>();
    private AppInfoFilter filter;

    public AppListArrayAdapter(Context context, List<AppInfo> appInfoList, boolean showIcon) {
        this.context = context;
        this.showIcon = showIcon;
        setItems(appInfoList);
    }

    public void setItems(List<AppInfo> appInfoList) {
        allItems.clear();
        allItems.addAll(appInfoList);
        visibleItems.clear();
        visibleItems.addAll(appInfoList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return visibleItems.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return visibleItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null)
            row = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);

        TextView nameView = row.findViewById(R.id.text_view);
        TextView packageView = row.findViewById(R.id.package_view);
        ImageView iconView = row.findViewById(R.id.icon_view);

        AppInfo appInfo = getItem(position);
        nameView.setText(appInfo.name);
        packageView.setText(appInfo.packageName);

        if (!showIcon) {
            iconView.setVisibility(View.GONE);
            iconView.setImageDrawable(null);
            return row;
        }

        iconView.setVisibility(View.VISIBLE);
        iconView.setImageDrawable(loadIcon(appInfo.packageName));
        return row;
    }

    private Drawable loadIcon(String packageName) {
        Drawable icon = iconCache.get(packageName);
        if (icon != null)
            return icon;
        try {
            icon = context.getPackageManager().getApplicationIcon(packageName);
            iconCache.put(packageName, icon);
        } catch (Exception ignored) {
            icon = null;
        }
        return icon;
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
            List<AppInfo> matches = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                matches.addAll(allItems);
            } else {
                String pattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
                for (AppInfo appInfo : allItems) {
                    if (appInfo.name.toLowerCase(Locale.ROOT).contains(pattern)
                            || appInfo.packageName.toLowerCase(Locale.ROOT).contains(pattern))
                        matches.add(appInfo);
                }
            }
            FilterResults results = new FilterResults();
            results.values = matches;
            results.count = matches.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            visibleItems.clear();
            visibleItems.addAll((List<AppInfo>) results.values);
            notifyDataSetChanged();
        }
    }
}
