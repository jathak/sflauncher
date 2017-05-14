package xyz.jathak.sflauncher;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.woozzu.android.widget.IndexableListView;

public class AppDrawerAdapter extends BaseAdapter implements SectionIndexer{

    private MainActivity main;
    private int small, medium, large, smallG, mediumG, largeG;

    public AppDrawerAdapter(MainActivity main){
        this.main = main;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main);
        useGrid = prefs.getBoolean("useGrid",false);
        small = (int)(main.getResources().getDisplayMetrics().density*28);
        medium = (int)(main.getResources().getDisplayMetrics().density*40);
        large = (int)(main.getResources().getDisplayMetrics().density*56);
        smallG = (int)(main.getResources().getDisplayMetrics().density*36);
        mediumG = (int)(main.getResources().getDisplayMetrics().density*48);
        largeG = (int)(main.getResources().getDisplayMetrics().density*64);
        String sizeStr = prefs.getString("iconsize","Medium");
        smallText = false;
        if(sizeStr.equals("Small")){
            if(useGrid){
                size = smallG;
            }else size = small;

            smallText = true;
        }else if(sizeStr.equals("Large")){
            if(useGrid){
                size = largeG;
            }else size = large;
        }else{
            if(useGrid){
                size = mediumG;
            }else size = medium;
        }
    }

    @Override
    public int getCount() {
        return main.notHidden.size();
    }

    @Override
    public Object getItem(int position) {
        return main.notHidden.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public String[] getSections() {
        return new String[]{"#","A","B","C","D","E","F","G","H","I","J","K",
                "L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if(sectionIndex>=getSections().length) return getCount()-1;
        if(sectionIndex<0) return 0;
        for(int i=0;i<getCount();i++){
            if(getSectionForPosition(i)==sectionIndex) return i;
        }
        return getPositionForSection(sectionIndex+1);
    }

    @Override
    public int getSectionForPosition(int position) {
        String initial =  main.notHidden.get(position).name.substring(0,1);
        for(int i=0;i<getSections().length;i++){
            if(initial.toLowerCase().equals(getSections()[i].toLowerCase())){
                return i;
            }
        }
        return 0;
    }

    @Override
    public void notifyDataSetChanged(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(main);
        useGrid = prefs.getBoolean("useGrid",false);
        String sizeStr = prefs.getString("iconsize","Medium");
        smallText = false;
        if(sizeStr.equals("Small")){
            if(useGrid){
                size = smallG;
            }else size = small;

            smallText = true;
        }else if(sizeStr.equals("Large")){
            if(useGrid){
                size = largeG;
            }else size = large;
        }else{
            if(useGrid){
                size = mediumG;
            }else size = medium;
        }
        super.notifyDataSetChanged();
    }

    private int size = medium;

    private boolean useGrid;
    private boolean smallText = false;

    static class ViewHolder{
        ImageView image;
        TextView text;
        boolean useGrid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if(convertView==null||((ViewHolder)convertView.getTag()).useGrid!=useGrid){
            if(useGrid){
                convertView = main.getLayoutInflater().inflate(R.layout.item_appgrid, null);
            }else{
                convertView = main.getLayoutInflater().inflate(R.layout.item_applist, null);
            }
            vh = new ViewHolder();
            vh.image = (ImageView) convertView.findViewById(R.id.image);
            vh.text = (TextView) convertView.findViewById(R.id.label);
            vh.useGrid = useGrid;
            convertView.setTag(vh);
        }else vh = (ViewHolder) convertView.getTag();
        ((IndexableListView)parent).reconfigBar();
        App app = main.notHidden.get(position);
        vh.image.setImageDrawable(app.getIcon(main));
        vh.image.getLayoutParams().height = size;
        vh.image.getLayoutParams().width = size;
        vh.image.setMinimumWidth(size);
        vh.image.setMinimumHeight(size);
        vh.image.setMaxHeight(size);
        vh.image.setMaxWidth(size);
        vh.text.setText(app.name);
        if(useGrid){
            if(smallText){
                vh.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            }else vh.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }else{
            if(smallText){
                vh.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }else vh.text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
        if(MainActivity.useBlackText(MainActivity.drawerColor)){
            vh.text.setTextColor(Card.COLORS.get("Black"));
            vh.text.setShadowLayer(0, 0, 0, Color.WHITE);
        }else{
            vh.text.setTextColor(Color.WHITE);
            vh.text.setShadowLayer(2, 1, 1, Card.COLORS.get("Black"));
        }

        return convertView;
    }
}
