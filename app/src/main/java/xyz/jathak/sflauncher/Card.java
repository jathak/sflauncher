package xyz.jathak.sflauncher;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.daimajia.swipe.SwipeLayout;

import java.util.*;

public abstract class Card {
    public static final int DEFAULT_COLOR = 1;
    public static final int NO_CARD_COLOR = 2;

    private static final LinkedHashMap<String, Integer> COLORS_RAW;
    static{
        COLORS_RAW = new LinkedHashMap<>();
        COLORS_RAW.put("Automatic", DEFAULT_COLOR);
        COLORS_RAW.put("Transparent", NO_CARD_COLOR);
        COLORS_RAW.put("White", Color.WHITE);
        COLORS_RAW.put("Black", Color.parseColor("#000000"));
        COLORS_RAW.put("Red", Color.parseColor("#F44336"));
        COLORS_RAW.put("Pink", Color.parseColor("#E91E63"));
        COLORS_RAW.put("Purple", Color.parseColor("#9C27B0"));
        COLORS_RAW.put("Deep Purple", Color.parseColor("#673AB7"));
        COLORS_RAW.put("Indigo", Color.parseColor("#3F51B5"));
        COLORS_RAW.put("Blue", Color.parseColor("#2196F3"));
        COLORS_RAW.put("Light Blue", Color.parseColor("#03A9F4"));
        COLORS_RAW.put("Cyan", Color.parseColor("#00BCD4"));
        COLORS_RAW.put("Teal", Color.parseColor("#009688"));
        COLORS_RAW.put("Green", Color.parseColor("#4CAF50"));
        COLORS_RAW.put("Light Green", Color.parseColor("#8BC34A"));
        COLORS_RAW.put("Lime", Color.parseColor("#CDDC39"));
        COLORS_RAW.put("Yellow", Color.parseColor("#FFEB3B"));
        COLORS_RAW.put("Amber", Color.parseColor("#FFC107"));
        COLORS_RAW.put("Orange", Color.parseColor("#FF9800"));
        COLORS_RAW.put("Deep Orange", Color.parseColor("#FF5722"));
        COLORS_RAW.put("Brown", Color.parseColor("#795548"));
        COLORS_RAW.put("Grey", Color.parseColor("#9E9E9E"));
        COLORS_RAW.put("Blue Grey", Color.parseColor("#607D8B"));
    }

    public static String getColorName(int color){
        for(String name : COLOR_KEYS){
            if(COLORS.get(name)==color) return name;
        }
        return "";
    }
    
    public static String getColorDisplayName(int color){
        return getColorName(color).split(":")[0];
    }

    private static final List<String> COLOR_KEYS_RAW;
    static{
        COLOR_KEYS_RAW = new ArrayList<>();
        for(String s : COLORS_RAW.keySet()){
            COLOR_KEYS_RAW.add(s);
        }
    }

    public static LinkedHashMap<String, Integer> COLORS;
    static{
        COLORS = new LinkedHashMap<>();
        COLORS.putAll(COLORS_RAW);
    }

    public static List<String> COLOR_KEYS;
    static{
        COLOR_KEYS = new ArrayList<>();
        COLOR_KEYS.addAll(COLOR_KEYS_RAW);
    }

    public static void addThemes(List<Theme> themes){
        LinkedHashMap<String, Integer> newColors = new LinkedHashMap<>();
        List<String> newKeys = new ArrayList<>();
        newColors.putAll(COLORS_RAW);
        newKeys.addAll(COLOR_KEYS_RAW);
        for(Theme t : themes){
            for(Theme.Swatch s: t.swatches){
                String key = s.name+":"+t.packageName;
                newColors.put(key, s.color);
                newKeys.add(key);
            }
        }
        COLORS = newColors;
        COLOR_KEYS = newKeys;
    }

    public int color = DEFAULT_COLOR;
    public int trueColor = Color.WHITE;

    protected static float ONE_DP;

    public void refreshColor(){
        CardView v = getCardContainer();
        int rcolor = color;
        int tcolor = color;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
        String theme = prefs.getString("theme","Light");
        if(theme.equals("Auto")){
            int hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);
            theme = "Dark";
            if(hour>=5&&hour<17) theme="Light";
        }
        int bgColor = prefs.getInt("bgColor",Card.DEFAULT_COLOR);
        if(theme.equals("Light")){
            tcolor = COLORS.get("White");
            setTints("#555555", 0xffffff);
        }else if(theme.equals("Dark")){
            tcolor = COLORS.get("Black");
            setTints("#e0e0e0", 0x000000);
        }else if(theme.equals("System Wallpaper")||theme.equals("Wallpaper with Header")){
            setTints("#ffffff", 0xff0000);
            if(this instanceof Apps||prefs.getBoolean("transonwallpaper",false)) {
                tcolor = Color.TRANSPARENT;
            }else{
                tcolor = Color.WHITE;
            }
        }else{
            tcolor = Integer.parseInt(theme.split(":")[1]);
            if(bgColor==Card.DEFAULT_COLOR){
                bgColor = Integer.parseInt(theme.split(":")[0]);
            }
        }
        if(bgColor!=Card.DEFAULT_COLOR){
            if(bgColor==Color.WHITE){
                setTints("#555555",bgColor);
            }else if(bgColor==Color.BLACK){
                setTints("#e0e0e0",bgColor);
            }else if(bgColor==Color.TRANSPARENT){
                setTints("#ffffff",bgColor);
            }else if(MainActivity.useBlackText(bgColor)){
                setTints("#222222",bgColor);
            }else{
                setTints("#ffffff",bgColor);
            }
        }
        if (rcolor == Card.DEFAULT_COLOR) rcolor = tcolor;
        if (rcolor == Card.NO_CARD_COLOR) rcolor = Color.TRANSPARENT;
        v.setCardBackgroundColor(rcolor);
        trueColor = rcolor;
        if (this instanceof Apps) {
            Apps a = (Apps) this;
            if (a.getNumApps() == 1) {
                a.adapter.notifyDataSetChanged();
            }
        }
        if(rcolor == Color.TRANSPARENT){
            v.setCardElevation(0);
            //wrapper.setShowMode(SwipeLayout.ShowMode.PullOut);
        }else{
            v.setCardElevation(ONE_DP * 4);
            //wrapper.setShowMode(SwipeLayout.ShowMode.LayDown);
        }
        if(Build.VERSION.SDK_INT>=21) {
            v.setClipToOutline(true);
        }
    }

    public CardView getCardContainer(){
        if(wrapper==null) return null;
        return (CardView) wrapper.findViewById(R.id.card_container);
    }

    private SwipeLayout wrapper;

    public SwipeLayout getWrapper(){
        return wrapper;
    }

    private void setTints(String color, int bgColor){
        setTints(color);
        float[] hsvA = new float[3];
        Color.colorToHSV(bgColor, hsvA);
        int deleteColor;
        //System.out.println("HSV: "+hsvA[0]+" "+hsvA[1]+" "+hsvA[2]);
        if((hsvA[0]>45&&hsvA[0]<255)||hsvA[1]<0.06f||hsvA[2]<0.08f){
            deleteColor = Color.parseColor("#D32F2F");
        }else{
            deleteColor = Color.parseColor(color);
        }
        if(Build.VERSION.SDK_INT>=21) {
            delete.setImageTintList(ColorStateList.valueOf(deleteColor));
        }else{
            delete.getDrawable().setColorFilter(deleteColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void setTints(String color){
        setTints(Color.parseColor(color));
    }

    private void setTints(int color){
        if(Build.VERSION.SDK_INT<21){
            up.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            down.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            palette.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            resize.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            columns.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }else {
            up.setImageTintList(ColorStateList.valueOf(color));
            down.setImageTintList(ColorStateList.valueOf(color));
            palette.setImageTintList(ColorStateList.valueOf(color));
            resize.setImageTintList(ColorStateList.valueOf(color));
            columns.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    protected ImageButton up, down, palette, resize, columns, delete;
    private View top, bottom;

    public void showBars(boolean showTop, boolean showBottom){
        int t = View.GONE;
        int b = View.GONE;
        if(showTop) t = View.VISIBLE;
        if(showBottom) b = View.VISIBLE;
        top.setVisibility(t);
        bottom.setVisibility(b);
    }

    protected MainActivity main;

    public Card(MainActivity activity){
        wrapper = (SwipeLayout)activity.getLayoutInflater().inflate(R.layout.card, null);
        wrapper.setTag(this);
        wrapper.setShowMode(SwipeLayout.ShowMode.LayDown);
        wrapper.setDragEdge(SwipeLayout.DragEdge.Right);
        wrapper.addSwipeListener(new SwipeLayout.SwipeListener() {
            View back = wrapper.findViewById(R.id.back);
            @Override
            public void onStartOpen(SwipeLayout swipeLayout) {
                back.setAlpha(0);
                back.setScaleX(0.5f);
                back.setScaleY(0.5f);
            }

            @Override
            public void onOpen(SwipeLayout swipeLayout) {
                back.setAlpha(1);
                back.setScaleX(1);
                back.setScaleY(1);
            }

            @Override
            public void onStartClose(SwipeLayout swipeLayout) {
                back.setAlpha(1);
                back.setScaleX(1);
                back.setScaleY(1);
            }

            @Override
            public void onClose(SwipeLayout swipeLayout) {
                back.setAlpha(0);
                back.setScaleX(0.5f);
                back.setScaleY(0.5f);
            }

            @Override
            public void onUpdate(SwipeLayout swipeLayout, int leftOffset, int topOffset) {
                float ratio = Math.abs(((float)leftOffset)/((float)wrapper.getWidth()));
                back.setAlpha(ratio);
                float scale = ratio/2+0.5f;
                back.setScaleX(scale);
                back.setScaleY(scale);
            }

            @Override
            public void onHandRelease(SwipeLayout swipeLayout, float v, float v2) {

            }
        });
        ONE_DP = activity.getResources().getDisplayMetrics().density;
        up = (ImageButton) wrapper.findViewById(R.id.up);
        down = (ImageButton) wrapper.findViewById(R.id.down);
        palette = (ImageButton) wrapper.findViewById(R.id.color);
        resize = (ImageButton) wrapper.findViewById(R.id.resize);
        columns = (ImageButton) wrapper.findViewById(R.id.columns);
        top = wrapper.findViewById(R.id.top);
        bottom = wrapper.findViewById(R.id.bottom);
        delete = (ImageButton) wrapper.findViewById(R.id.delete);
        if(Build.VERSION.SDK_INT>=21) {
            delete.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
        }else{
            delete.getDrawable().setColorFilter(Color.parseColor("#D32F2F"), PorterDuff.Mode.SRC_ATOP);
        }
        main = (MainActivity) activity;
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.moveUp(Card.this);
            }
        });
        up.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                main.cards.moveToTop(Card.this);
                return true;
            }
        });
        down.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                main.cards.moveToBottom(Card.this);
                return true;
            }
        });
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.moveDown(Card.this);
            }
        });
        palette.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeBackground(Card.this);
            }
        });
        resize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.resizeCard(Card.this);
            }
        });
        columns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeColumns(Card.this);
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.cards.remove(Card.this);
            }
        });
        showBars(false, false);
        refreshColor();
    }

    public static class Empty extends Card{
        public Empty(MainActivity activity){
            super(activity);
            getWrapper().setVisibility(View.GONE);
            getWrapper().setLayoutParams(new ViewGroup.LayoutParams(0,0));
        }
        @Override
        public void refreshColor(){

        }
        protected void regenCardContent(SwipeLayout oldLayout, SwipeLayout newLayout){
            newLayout.setVisibility(View.GONE);
            newLayout.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        }
    }

    public static class Tutorial extends Card{
        private View currentView;
        public int current = 0;
        private ViewGroup vf;
        public Tutorial(MainActivity activity){
            super(activity);
            vf = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.tutorial, null);
            getCardContainer().addView(vf);
            columns.setVisibility(View.GONE);
            resize.setVisibility(View.INVISIBLE);
            palette.setVisibility(View.INVISIBLE);
            currentView = vf.getChildAt(vf.getChildCount()-1);
            vf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final View myView = vf.getChildAt(vf.getChildCount() - 1);
                    vf.getChildAt(vf.getChildCount()-2).setVisibility(View.VISIBLE);
                    myView.animate().translationY(-myView.getHeight()).setDuration(350).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            advance();
                            myView.setTranslationY(0);
                        }
                    });
                }
            });
            /*vf.findViewById(R.id.upgrade).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if("plus".equals(currentView.getTag())) {
                        final String appName = "net.alamoapps.launcher.plus";
                        try {
                            main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
                        }
                    }else{
                        final View myView = vf.getChildAt(vf.getChildCount() - 1);
                        vf.getChildAt(vf.getChildCount()-2).setVisibility(View.VISIBLE);
                        myView.animate().translationY(-myView.getHeight()).setDuration(350).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                advance();
                                myView.setTranslationY(0);
                            }
                        });
                    }
                }
            });*/
        }

        public void advance(){
            current++;
            View myView = vf.getChildAt(vf.getChildCount()-1);
            vf.getChildAt(vf.getChildCount()-2).setVisibility(View.VISIBLE);
            vf.removeViewAt(vf.getChildCount() - 1);
            myView.setVisibility(View.GONE);
            vf.addView(myView, 0);
            currentView = vf.getChildAt(vf.getChildCount() - 1);
            if("first".equals(currentView.getTag())) current = 0;
            main.saveCards();
        }
    }


    public static class Widget extends WidgetStub {
        public SFHostView view;
        public Widget(MainActivity activity){
            super(activity);
        }
    }

    public static class WidgetStub extends Resizable {
        public int id;
        public String packageName, className;
        public WidgetStub(MainActivity activity){
            super(activity);
            getWrapper().setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            getCardContainer().getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    public static abstract class Resizable extends Card{
        public static final int AUTOMATIC_HEIGHT = -1;
        public int height = -1;
        public Resizable(MainActivity activity){
            super(activity);
            columns.setVisibility(View.GONE);
        }
    }

    public static class Web extends Resizable{
        public String url;
        public WebView view;
        public Web(MainActivity activity, String url, int height){
            super(activity);
            this.url = url;
            view = new WebView(activity);
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            view.getSettings().setJavaScriptEnabled(true);
            view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            view.getSettings().setUseWideViewPort(true);
            view.getSettings().setLoadWithOverviewMode(true);
            view.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url){
                    view.setBackgroundColor(0x00000000);
                    view.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
                }
            });
            view.loadUrl(url);
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
            getCardContainer().addView(view);
        }

        public void refresh(){

        }
    }

    public static class Apps extends Card{
        public Apps(MainActivity activity){
            super(activity);
            resize.setVisibility(View.GONE);
            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            density = dm.density;
            if(HOLE==null)HOLE = new App.Hole();
            grid = new NonScrollableGridView(activity);
            getCardContainer().addView(grid);
            if(adapter==null) makeAdapter();
            grid.setAdapter(adapter);
            int four = (int)(density*4);
            grid.setPadding(four, four, four, four);
            grid.setClipToPadding(false);
            setColumns(6);
        }

        private boolean startActivitySafely(Intent intent) {
            try {
                main.startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(main, "Activity Not Found", Toast.LENGTH_SHORT).show();
                Log.e("APPLAUNCH", "Unable to launch. intent=" + intent, e);
            } catch (SecurityException e) {
                Toast.makeText(main, "SF Launcher does not have permission to open this", Toast.LENGTH_SHORT).show();
                Log.e("APPLAUNCH", "Launcher does not have the permission to launch " + intent +
                        ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                        "or use the exported attribute for this activity. "
                        + " intent=" + intent, e);
            }
            return false;
        }

        private float density;

        private int numColumns;

        public void setColumns(int n){
            numColumns = n;
            adapter.notifyDataSetChanged();
            redraw();
        }

        private void redraw() {
            if (apps.size() == 1) {
                grid.setNumColumns(1);
            } else if (grid.getNumColumns() != numColumns) {
                grid.setNumColumns(numColumns);
            }
        }

        public int getColumns(){
            return numColumns;
        }

        public void restoreApps(String appString){
            String[] parts = appString.split(",");
            for(String p : parts){
                App a = App.fromIdentifier(this,p);
                if(a!=null) addApps(a);
            }
        }
        private NonScrollableGridView grid;
        private BaseAdapter adapter;
        private List<App> apps = new ArrayList<>();

        public App getApp(int index){
            return apps.get(index);
        }

        public int getNumApps(){
            if (apps == null) return 0;
            return apps.size();
        }

        public BaseAdapter getAdapter(){
            return adapter;
        }

        public NonScrollableGridView getGrid(){
            return grid;
        }

        public void addApps(App... adding){
            for(App a : adding){
                apps.add(a);
            }
            if(adapter==null) makeAdapter();
            redraw();
            adapter.notifyDataSetChanged();
        }

        public void addAppToPosition(int position, App app){
            if(position<0||position>=apps.size()) return;
            apps.add(position, app);
            if(adapter==null) makeAdapter();
            redraw();
            adapter.notifyDataSetChanged();
        }

        public App removeApp(int position){
            if(position<0||position>=apps.size())return null;
            if(apps.size()==1){
                main.cards.remove(this);
                return apps.get(0);
            }else{
                App removed = apps.remove(position);
                if(adapter==null) makeAdapter();
                adapter.notifyDataSetChanged();
                return removed;
            }
        }

        //Replaces hole from makeHole with the given app
        //If not hole exists, adds to end;
        public void addToHole(App add){
            int where = apps.indexOf(HOLE);
            if(where==-1){
                apps.add(add);
            }else {
                apps.remove(where);
                apps.add(where, add);
            }
            redraw();
            adapter.notifyDataSetChanged();
        }

        public void clearHoles(){
            for(int i=0;i<apps.size();i++){
                if(apps.get(i) instanceof App.Hole){
                    apps.remove(i);
                    redraw();
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
            if(apps.size()==0){
                main.cards.remove(this);
                main.mAdapter.notifyDataSetChanged();
            }
        }

        public String getAppString(){
            String str = "";
            for(App a : apps) {
                str += "," + a.getIdentifier();
            }
            if(str.length()>0) str = str.substring(1);
            return str;
        }

        private App.Hole HOLE;

        public boolean makeHole(int row, int column){
            if(row<0)return makeHole(0, column);
            if(column<0)return makeHole(0, column);
            if(column>=numColumns)return makeHole(row, numColumns-1);
            int where = row*numColumns+column;
            if(where>apps.size())where=apps.size();
            if(where<apps.size()&&apps.get(where) instanceof App.Hole) return false;
            int old = apps.indexOf(HOLE);
            if(old>=0){
                apps.remove(old);
                if(where>apps.size()) where = apps.size();
                apps.add(where, HOLE);
            }else apps.add(where, HOLE);
            redraw();
            adapter.notifyDataSetChanged();
            return true;
        }

        public void makeAdapter(){
            adapter = new BaseAdapter() {
                @Override
                public boolean isEnabled(int position){
                    return false;
                }
                @Override
                public int getCount() {
                    return apps.size();
                }

                @Override
                public Object getItem(int position) {
                    return apps.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return 0;
                }

                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    int width = (int)(336*density);
                    DisplayMetrics d = parent.getContext().getResources().getDisplayMetrics();
                    if(PreferenceManager.getDefaultSharedPreferences(parent.getContext()).getBoolean("fitcards",true)){
                        int orientation = parent.getContext().getResources().getConfiguration().orientation;
                        int screenwidth = orientation == Configuration.ORIENTATION_PORTRAIT ? d.widthPixels : d.heightPixels;
                        width = screenwidth - (int)(24*d.density);
                    }
                    int size = (int) (96 * density);
                    size = Math.min(size, width / numColumns);
                    if (getCount() == 1) {
                        View view = main.getLayoutInflater().inflate(R.layout.item_applist, null);
                        ImageView image = (ImageView) view.findViewById(R.id.image);
                        TextView text = (TextView) view.findViewById(R.id.label);
                        App app = apps.get(position);
                        view.setPadding((int)(density), 0, 0, 0);
                        image.setImageDrawable(app.getIcon(parent.getContext()));
                        image.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                        image.setMinimumWidth(size);
                        image.setMinimumHeight(size);
                        image.setMaxHeight(size);
                        image.setMaxWidth(size);
                        int m = (int) (density * 4);
                        image.setPadding(m, m, m, m);
                        text.setPadding(m, 0, m, 0);
                        image.setAdjustViewBounds(true);
                        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        text.setText(app.name);
                        if(MainActivity.useBlackText(trueColor)){
                            text.setTextColor(Card.COLORS.get("Black"));
                            text.setShadowLayer(0, 0, 0, Color.WHITE);
                        }else{
                            text.setTextColor(Color.WHITE);
                            text.setShadowLayer(2, 1, 1, Card.COLORS.get("Black"));
                        }
                        view.setBackgroundResource(R.drawable.ripple);
                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivitySafely(apps.get(position).getIntent());
                            }
                        });
                        view.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View v) {
                                grid.setHapticFeedbackEnabled(true);
                                int originalCardPosition = main.cards.indexOf(Card.Apps.this);
                                ImageView i = (ImageView) v.findViewById(R.id.image);
                                return main.startAppListDrag(removeApp(position), i, false, Card.Apps.this, originalCardPosition, position);
                            }
                        });
                        return view;
                    } else {
                        Drawable icon = apps.get(position).getIcon(parent.getContext());
                        ImageView i;
                        if (convertView == null || !(convertView instanceof ImageView)) {
                            convertView = new ImageView(parent.getContext());
                            i = (ImageView) convertView;
                            i.setMinimumHeight(size);
                            i.setMinimumWidth(size);
                            i.setMaxHeight(size);
                            i.setMaxWidth(size);
                            int m = (int) (density * 4);
                            i.setPadding(m, m, m, m);
                            i.setAdjustViewBounds(true);
                            i.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        } else {
                            i = (ImageView) convertView;
                            i.setOnClickListener(null);
                            i.setOnLongClickListener(null);
                        }
                        i.setBackgroundResource(R.drawable.ripple);
                        i.setImageDrawable(icon);
                        i.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivitySafely(apps.get(position).getIntent());
                            }
                        });
                        i.setOnLongClickListener(new View.OnLongClickListener() {

                            @Override
                            public boolean onLongClick(View v) {
                                grid.setHapticFeedbackEnabled(true);
                                int originalCardPosition = main.cards.indexOf(Card.Apps.this);
                                return main.startAppListDrag(removeApp(position), (ImageView) v, false, Card.Apps.this, originalCardPosition, position);
                            }
                        });
                        return convertView;
                    }
                }
            };
        }
    }
}
