package xyz.jathak.sflauncher;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.TimeZone;


public class CityAdapter extends BaseAdapter {

    private LayoutInflater li;
    private int hour;

    public CityAdapter(LayoutInflater li){
        this.li = li;
        hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);

    }
    @Override
    public int getCount() {
        return City.cities.size();
    }

    @Override
    public Object getItem(int position) {
        return City.cities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    static class ViewHolder{
        ImageView combo;
        TextView label;
    }

    Bitmap muzeiImage = null;

    @SuppressWarnings("deprecation")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView==null){
            convertView = li.inflate(R.layout.city_item, null);
            vh = new ViewHolder();
            vh.combo = (ImageView) convertView.findViewById(R.id.combo);
            vh.label = (TextView) convertView.findViewById(R.id.label);
            convertView.setTag(vh);
        }else vh = (ViewHolder) convertView.getTag();
        vh.combo.setTag(null);
        City city = City.cities.get(position);
        vh.label.setText(city.name.split(":")[0]);
        int res = city.night;
        if(hour>=5) res = city.dawn;
        if(hour>=8) res = city.day;
        if(hour>=17) res = city.dusk;
        if(hour>=20) res = city.night;
        if(Runtime.getRuntime().maxMemory()>=40*1000*1000) {
            if (city instanceof City.Muzei) {
                vh.combo.setMaxHeight((int) (parent.getWidth() * 0.428823529));
                if (muzeiImage == null) {
                    vh.combo.setImageResource(R.color.darkBack);
                    final ImageView iv = vh.combo;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final Bitmap bmp = MainActivity.getScaledMuzei(li.getContext());
                            iv.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (iv.getTag() instanceof City.Muzei) {
                                        iv.setImageBitmap(bmp);
                                    }
                                    muzeiImage = bmp;
                                }
                            });

                        }
                    }).start();
                } else {
                    vh.combo.setImageBitmap(muzeiImage);
                }
                vh.combo.setTag(city);
            } else if (city instanceof City.Music && Build.VERSION.SDK_INT >= 21) {
                vh.combo.setMaxHeight((int) (parent.getWidth() * 0.428823529));
                vh.combo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (MediaListener.currentArt != null) {
                    vh.combo.setImageBitmap(MediaListener.currentArt);
                } else vh.combo.setImageResource(R.drawable.music_placeholder);
            } else if (city.packageName == null) {
                Drawable d = li.getContext().getResources().getDrawable(res);
                vh.combo.setImageDrawable(d);
            } else {
                try {
                    Resources cityRes = li.getContext().getPackageManager().getResourcesForApplication(city.packageName);
                    Drawable d = cityRes.getDrawable(res);
                    vh.combo.setImageDrawable(d);
                    vh.combo.setMaxHeight((int) (parent.getWidth() * 0.428823529));
                } catch (PackageManager.NameNotFoundException e) {
                    vh.combo.setImageResource(R.color.darkBack);
                }
            }
        }else vh.combo.setImageResource(R.color.darkBack);
        return convertView;
    }
}
