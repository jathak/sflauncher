package xyz.jathak.sflauncher;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ColorAdapter extends BaseAdapter {

    private LayoutInflater li;
    private List<String> colors;

    public ColorAdapter(LayoutInflater li, List<String> colors)  {
        this.li = li;
        this.colors = colors;
    }

    public ColorAdapter(LayoutInflater li, boolean forCard){
        this.li = li;
        this.colors = new ArrayList<>();
        this.colors.addAll(Card.COLOR_KEYS);
        if (!forCard) {
            this.colors.remove("Automatic");
            this.colors.remove("Transparent");
        }
    }

    @Override
    public int getCount() {
        return colors.size();
    }

    @Override
    public Object getItem(int position) {
        return colors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return Card.COLORS.get(colors.get(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = li.inflate(R.layout.swatch, null);
        }
        String name = colors.get(position);
        int color = Card.COLORS.get(name);
        if (color == Card.DEFAULT_COLOR || color == Card.NO_CARD_COLOR) color = Color.TRANSPARENT;
        TextView tv = (TextView) convertView;
        tv.setText(name.split(":")[0]);
        if(color!=Color.TRANSPARENT&&MainActivity.useBlackText(color)){
            tv.setTextColor(Card.COLORS.get("Black"));
            tv.setShadowLayer(0,0,0,Color.WHITE);
        }else{
            tv.setTextColor(Color.WHITE);
            tv.setShadowLayer(2,1,1,Card.COLORS.get("Black"));
        }
        tv.setBackgroundColor(color);
        return tv;
    }
}
