package xyz.jathak.sflauncher;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WidgetAdapter extends BaseAdapter {

    private Context ctx;
    private List<AppWidgetProviderInfo> providers;

    public WidgetAdapter(Context ctx, List<AppWidgetProviderInfo> providers){
        Collections.sort(providers, new Comparator<AppWidgetProviderInfo>() {
            @Override
            public int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                return a.label.compareTo(b.label);
            }
        });
        this.ctx = ctx;
        this.providers = providers;
    }
    @Override
    public int getCount() {
        return providers.size();
    }

    @Override
    public Object getItem(int position) {
        return providers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    static class ViewHolder{
        ImageView image;
        TextView label;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView==null){
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_applist, null);
            vh = new ViewHolder();
            vh.image = (ImageView) convertView.findViewById(R.id.image);
            vh.label = (TextView) convertView.findViewById(R.id.label);
            int size = (int)(ctx.getResources().getDisplayMetrics().density*40);
            vh.image.getLayoutParams().height = size;
            vh.image.getLayoutParams().width = size;
            vh.image.setMinimumWidth(size);
            vh.image.setMinimumHeight(size);
            vh.image.setMaxHeight(size);
            vh.image.setMaxWidth(size);
            vh.label.setTextColor(Color.WHITE);
            convertView.setTag(vh);
        }else vh = (ViewHolder) convertView.getTag();
        AppWidgetProviderInfo provider = providers.get(position);
        PackageManager pm = ctx.getPackageManager();
        int density = ctx.getResources().getDisplayMetrics().densityDpi;
        if (Build.VERSION.SDK_INT >= 21) {
            vh.label.setText(provider.loadLabel(pm));
            Drawable image = provider.loadIcon(ctx, density);
            vh.image.setImageDrawable(image);
        } else {
            try {
                Resources res = pm.getResourcesForApplication(provider.provider.getPackageName());
                vh.image.setImageDrawable(res.getDrawable(provider.icon));
            } catch (PackageManager.NameNotFoundException e) {
                vh.image.setImageDrawable(null);
            }
            vh.label.setText(provider.label);
        }
        return convertView;
    }
}
