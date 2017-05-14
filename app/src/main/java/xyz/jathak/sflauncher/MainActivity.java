package xyz.jathak.sflauncher;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.google.android.apps.muzei.api.MuzeiContract;
import com.woozzu.android.widget.IndexableListView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends ActionBarActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private IndexableListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private RecyclerView mRecyclerView;
    public RecyclerView.Adapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private BaseAdapter navigationDrawerAdapter;
    public List<App> apps = new ArrayList<>();
    public List<App> notHidden = new ArrayList<>();
    public SuperCardList cards;

    private ImageView headerImage;
    private ImageButton play, next, prev;
    private View clockWrapper, searchWrapper, musicWrapper;
    public AppWidgetManager mAppWidgetManager;
    public SFWidgetHost mAppWidgetHost;

    public static final int DEFAULT_WIDGET_SIZE = 120;

    public List<Theme> themes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.gc();
        initialized = false;
        new Thread(new Runnable(){
            @Override
            public void run() {
                themes = Theme.loadThemes(MainActivity.this);
            }
        }).start();
        mRecyclerView = (RecyclerView)findViewById(R.id.cards);
        if(cards==null)cards = new SuperCardList(this, mRecyclerView);
        String orientation = PreferenceManager.getDefaultSharedPreferences(this).getString("orientation", "Automatic");
        if(orientation.equals("Automatic")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }else if(orientation.equals("Portrait")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else if(orientation.equals("Landscape")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            stopService(new Intent(this, MediaListener.class));
            startService(new Intent(this, MediaListener.class));
        }
        cards.quickAdd(new Card.Empty(this));
        updateWindowFlags();
        setContentView(R.layout.activity_main);
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new SFWidgetHost(this, 17031196);
        cards.widgetHost = mAppWidgetHost;
        mAppWidgetHost.startListening();

        nitView();
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
        }
        initDrawer();
        headerImage = (ImageView) findViewById(R.id.header_image);
        refreshThemes();
        restoreCards();
        refreshHeader();
        refreshAppList();

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(onAppChange, filter);
        //IntentFilter importFilter = new IntentFilter("xyz.jathak.sflauncher.EXPORT");
        //registerReceiver(sfImport, importFilter);
        IntentFilter shortcutFilter = new IntentFilter("com.android.launcher.action.INSTALL_SHORTCUT");
        registerReceiver(shortcutListener, shortcutFilter);
        IntentFilter timeTickFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
        timeTickFilter.addAction(Intent.ACTION_TIME_CHANGED);
        timeTickFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeTickListener, timeTickFilter);
        IntentFilter muzei = new IntentFilter("com.google.android.apps.muzei.ACTION_ARTWORK_CHANGED");
        registerReceiver(muzeiListener, muzei);
        if (Build.VERSION.SDK_INT >= 21) {
            IntentFilter music = new IntentFilter(MediaListener.MEDIA_UPDATE);
            registerReceiver(musicUpdateListener, music);
        }
    }

    private void updateWindowFlags(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        boolean hideStatusBar = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("hideStatusBar",false);
        if(dm.heightPixels>dm.widthPixels||isTablet()) {
            if(hideStatusBar){
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN);
            }else{
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
        }else{
            if(hideStatusBar){
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }else{
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
        }
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            getWindow().getDecorView().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    updateWindowFlags();
                                }
                            }, 3000);
                        }
                    }
                });
    }

    private boolean isTablet(){
        int layout = getResources().getConfiguration().screenLayout;
        return (layout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                (layout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE ;
    }

    BroadcastReceiver onAppChange = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshAppList();
        }

    };

    BroadcastReceiver shortcutListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            createShortcut(i);
        }
    };

    BroadcastReceiver musicUpdateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String image = prefs.getString("city", "San Francisco");
            String style = prefs.getString("headerStyle2", "Search Bar");
            if (image.equals("Music:music-theme")||style.equals("Music Controls")) {
                refreshHeader();
            }
        }
    };

    BroadcastReceiver timeTickListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String theme = PreferenceManager.getDefaultSharedPreferences(context).getString("theme","Light");
            int hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);
            if(theme.equals("Auto")) {
                if (hour >= 5 && hour < 17) {
                    if (lastThemeTime == 1) {
                        refreshThemes();
                    } else refreshHeader();
                } else {
                    if (lastThemeTime == 2) {
                        refreshThemes();
                    } else refreshHeader();
                }
            }else refreshHeader();
        }
    };

    BroadcastReceiver muzeiListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            lastUseMuzei = false;
            refreshHeader();
        }
    };

    private void createShortcut(Intent i){
        Intent launchIntent = i.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        Parcelable iconResourceParcelable =i.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
        String name = i.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Intent.ShortcutIconResource icon = null;
        if (iconResourceParcelable != null && iconResourceParcelable
                instanceof Intent.ShortcutIconResource) {
            icon = (Intent.ShortcutIconResource)iconResourceParcelable;
        }
        if(icon!=null) {
            App.Shortcut shortcut = new App.Shortcut(MainActivity.this, launchIntent, name, icon);
            for(Card c : cards){
                if(c instanceof Card.Apps){
                    ((Card.Apps) c).addApps(shortcut);
                    saveCards();
                    return;
                }
            }
            Card.Apps ca = new Card.Apps(MainActivity.this);
            ca.addApps(shortcut);
            cards.add(ca);
        }else{
            Parcelable parcel = i.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
            if(parcel!=null&&parcel instanceof Bitmap) {
                Bitmap b = (Bitmap) parcel;
                App.BitmapShortcut bshortcut = new App.BitmapShortcut(name, b, launchIntent);
                for(Card c : cards){
                    if(c instanceof Card.Apps){
                        ((Card.Apps) c).addApps(bshortcut);
                        saveCards();
                        return;
                    }
                }
                Card.Apps ca = new Card.Apps(MainActivity.this);
                ca.addApps(bshortcut);
                cards.add(ca);
            }
        }
    }

    BroadcastReceiver sfImport = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            if(awaitingImport){
                awaitingImport = false;
                String favorites = intent.getStringExtra("favorites");
                favorites = favorites.replaceAll(";",",");
                int columns = intent.getIntExtra("favColumns", 6);
                Card.Apps ca = new Card.Apps(MainActivity.this);
                ca.restoreApps(favorites);
                ca.setColumns(columns);
                cards.add(ca);
                SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                e.putBoolean("firstLaunch",false);
                if(includeSettings){
                    e.putString("theme",intent.getStringExtra("theme"));
                    e.putString("city",intent.getStringExtra("city"));
                    e.putString("orientation",intent.getStringExtra("orientation"));
                    e.putString("iconpack",intent.getStringExtra("iconpack"));
                    e.commit();
                    refreshThemes();
                }else e.commit();
            }
        }

    };

    public static boolean awaitingImport = false;
    private boolean includeSettings = false;

    private void promptForImport(boolean override){
        /*final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!override&&!prefs.getBoolean("firstLaunch",true)) return;
        cards.add(1,new Card.Tutorial(MainActivity.this));
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("Import from Old Version?");
        d.setMessage("Do you want to import your favorites and some settings from the old version of SF Launcher?");
        d.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("net.alamoapps.launcher", "net.alamoapps.launcher.ExportService"));
                awaitingImport = true;
                includeSettings = true;
                startService(intent);
            }
        });
        d.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor e = prefs.edit();
                e.putBoolean("firstLaunch",false);
                addDefaultCards();
                e.apply();
            }
        });
        d.show();*/
    }

    private int lastThemeTime = -1;
    private int lastHeaderRes = -1;
    private String lastHeaderStr = "";
    private boolean lastUseMuzei = false;

    private void refreshHeader(){
        refreshHeader(false);
    }

    private void refreshHeader(boolean forceRefresh) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String cityName =prefs.getString("city", "San Francisco");
        String style = prefs.getString("headerStyle2", "Search Bar");
        City city = City.from(cityName);
        final int hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);
        int res = city.night;
        if(hour>=5) res = city.dawn;
        if(hour>=8) res = city.day;
        if(hour>=17) res = city.dusk;
        if(hour>=20) res = city.night;
        if(hour >= 5 && hour < 17){
            lastThemeTime = 2;
        }else{
            lastThemeTime = 1;
        }
        headerImage.setOnClickListener(null);
        View.OnClickListener listener = null;
        if (Build.VERSION.SDK_INT >= 21) {
            if (!MediaListener.online) {
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    }
                };
                findViewById(R.id.musicControls).setVisibility(View.GONE);
            } else {
                listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(MediaListener.MEDIA_ACTION);
                        i.putExtra("type", 3);
                        sendBroadcast(i);
                    }
                };
            }
            if (style.equals("Music Controls") && isPremium(this)) {
                if (MediaListener.currentSong != null) {
                    ((TextView)findViewById(R.id.musicTitle)).setText(MediaListener.currentSong);
                    ((TextView)findViewById(R.id.musicArtist)).setText(MediaListener.currentArtist);
                }
                if (MediaListener.currentlyPlaying) {
                    play.setImageResource(R.drawable.ic_action_pause);
                } else {
                    play.setImageResource(R.drawable.ic_action_play);
                }
                findViewById(R.id.musicTitle).setOnClickListener(listener);
                findViewById(R.id.musicArtist).setOnClickListener(listener);
            }
        }
        boolean useMuzei = cityName.equals("Muzei:muzei-theme");
        if (useMuzei) {
            if (!lastUseMuzei) {
                headerImage.setImageResource(R.drawable.empty);
                loadMuzeiHeader();
            }
            return;
        }
        lastUseMuzei = false;
        if (Build.VERSION.SDK_INT >= 21 && cityName.equals("Music:music-theme") && isPremium(this)) {
            if (MediaListener.currentArt != null) {
                headerImage.setAdjustViewBounds(false);
                headerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                headerImage.setImageBitmap(headerCrop(this, MediaListener.currentArt));
            } else {
                headerImage.setImageResource(R.drawable.music_placeholder);
            }
            headerImage.setOnClickListener(listener);
            return;
        }
        if(forceRefresh||lastHeaderRes!=res||!lastHeaderStr.equals(cityName)) {
            lastHeaderRes = res;
            lastHeaderStr = cityName;
            if (city.packageName == null) {
                headerImage.setImageResource(res);
            } else {
                try {
                    Resources cityRes = getPackageManager().getResourcesForApplication(city.packageName);
                    Drawable d = cityRes.getDrawable(res);
                    headerImage.setImageDrawable(d);
                    headerImage.setMaxHeight((int) (getResources().getDisplayMetrics().widthPixels * 0.428823529));
                } catch (PackageManager.NameNotFoundException e) {
                    city = City.sanfrancisco;
                    res = city.night;
                    if(hour>=5) res = city.dawn;
                    if(hour>=8) res = city.day;
                    if(hour>=17) res = city.dusk;
                    if(hour>=20) res = city.night;
                    headerImage.setImageResource(res);
                }
            }
        }
    }

    public static Bitmap headerCrop(Context ctx, Bitmap bmp) {
        if (bmp == null) {
            return null;
        }
        try {
            int width = ctx.getResources().getDisplayMetrics().widthPixels;
            int height = (int) ((width * 1.0) / (bmp.getWidth() * 1.0) * (bmp.getHeight() * 1.0));
            int trueHeight = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.428823529);
            if (height >= trueHeight) {
                bmp = Bitmap.createScaledBitmap(bmp, width, height, true);
                int offset = (height - trueHeight) / 2;
                bmp = Bitmap.createBitmap(bmp, 0, offset, width, trueHeight);
            } else {
                int scaledWidth = (int) ((trueHeight * 1.0) / (bmp.getHeight() * 1.0) * (bmp.getWidth() * 1.0));
                bmp = Bitmap.createScaledBitmap(bmp, scaledWidth, height, true);
                int offset = (scaledWidth - width) / 2;
                bmp = Bitmap.createBitmap(bmp, offset, 0, width, trueHeight);
            }
            return bmp;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Bitmap getScaledMuzei(Context ctx) {
        Bitmap bmp = null;
        try {
            InputStream is = ctx.getContentResolver().openInputStream(MuzeiContract.Artwork.CONTENT_URI);
            bmp = BitmapFactory.decodeStream(is);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return headerCrop(ctx, bmp);
    }

    private void loadMuzeiHeader() {
        new Thread(new Runnable(){

            @Override
            public void run() {
                final Bitmap art = getScaledMuzei(MainActivity.this);
                headerImage.post(new Runnable(){

                    @Override
                    public void run() {
                        if (art != null) {
                            headerImage.setAdjustViewBounds(false);
                            headerImage.setImageBitmap(art);
                            lastUseMuzei = true;
                        }
                    }
                });
            }
        }).start();
    }
    private void refreshAppList(){
        new Thread(new Runnable(){

            @Override
            public void run() {
                PackageManager pm = getPackageManager();
                Intent intent = new Intent(Intent.ACTION_MAIN, null);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolves = pm.queryIntentActivities(intent, 0);
                List<App> myApps = new ArrayList<>();
                for(ResolveInfo r : resolves){
                    String packageName = r.activityInfo.packageName;
                    if(!packageName.equals("xyz.jathak.sflauncher")) {
                        App app = new App(r, MainActivity.this);
                        myApps.add(app);
                    }
                    Thread.yield();
                }
                Collections.sort(myApps);
                apps = myApps;
                toolbar.post(new Runnable(){

                    @Override
                    public void run() {
                        navigationDrawerAdapter.notifyDataSetChanged();
                        restoreCards();
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        if(prefs.getBoolean("firstLaunch",true)){
                            addDefaultCards();
                            cards.add(1,new Card.Tutorial(MainActivity.this));
                            SharedPreferences.Editor e = prefs.edit();
                            e.putBoolean("firstLaunch",false);
                            e.apply();
                        }
                        refreshThemes();
                    }
                });
            }
        }).start();
    }

    private void addDefaultCards(){
        Card.Apps ca = new Card.Apps(MainActivity.this);
        ca.restoreApps(
                "com.google.android.dialer/com.google.android.dialer.extensions.GoogleDialtactsActivity,"+
                        "com.android.chrome/com.google.android.apps.chrome.Main,"+
                        "com.android.vending/com.android.vending.AssetBrowserActivity,"+
                        "com.google.android.gm/com.google.android.gm.ConversationListActivityGmail,"+
                        "com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchActivity,"+
                        "com.android.settings/com.android.settings.Settings");
        ca.setColumns(ca.getNumApps());
        ca.refreshColor();
        cards.add(ca);
    }

    public void toggleDrawer(){
        if(drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawers();
        }else{
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            toggleDrawer();
            return true;
        }
        return false;
    }

    @Override
    public void finish(){}

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra("refresh-cards")) {
            List<Integer> oldIds = new ArrayList<>();
            for (Card c : cards) {
                if (c instanceof Card.Widget) {
                    oldIds.add(((Card.Widget)c).id);
                }
            }
            refreshThemes();
            restoreCards();
            refreshHeader();
            refreshAppList();
            List<Integer> newIds = new ArrayList<>();
            for (Card c : cards) {
                if (c instanceof Card.Widget) {
                    newIds.add(((Card.Widget)c).id);
                }
            }
            for (int i : oldIds) System.out.print(i+" ");
            System.out.println();
            for (int i : newIds) System.out.print(i+" ");
            System.out.println();
            for (int id : oldIds) {
                if (!newIds.contains(id)) {
                    mAppWidgetHost.deleteAppWidgetId(id);
                }
            }
        }
        if(notHidden.size()==0){
            refreshThemes();
            restoreCards();
            refreshHeader();
            refreshAppList();
        }else if(cards.size()==0){
            refreshThemes();
            restoreCards();
            refreshHeader();
        }else{
            refreshHeader();
        }
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            final boolean alreadyOnHome =
                    ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                            != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            if (alreadyOnHome&&!fromSettings) {
                if(drawerLayout!=null) drawerLayout.closeDrawers();
                if(mRecyclerView!=null) mRecyclerView.smoothScrollToPosition(0);
            }
            fromSettings = false;
        }else{
            super.onNewIntent(intent);
        }
    }

    /**
     * Launches a dialog of app widgets to select from.
     */
    void selectWidget() {
        AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
        d.setTitle("Choose widget");
        ListView g = new ListView(MainActivity.this);
        final List<AppWidgetProviderInfo> providers = mAppWidgetManager.getInstalledProviders();
        g.setAdapter(new WidgetAdapter(this, providers));
        d.setView(g);
        final AlertDialog dialog = d.show();
        g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                ComponentName name = providers.get(position).provider;
                int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                boolean result = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, name);
                if (result) {
                    configureWidget(appWidgetId);
                } else {
                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, name);
                    startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
                }
            }
        });
    }

    private boolean isLandscape(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return dm.widthPixels > dm.heightPixels;
    }

    private String lastTheme = null;
    private int lastHeight = 0;


    public void refreshThemes(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                themes = Theme.loadThemes(MainActivity.this);
            }
        }).start();
        if(clockWrapper==null)clockWrapper = findViewById(R.id.clockWrapper);
        if(searchWrapper==null)searchWrapper = findViewById(R.id.searchbarWrapper);
        if(musicWrapper==null)musicWrapper = findViewById(R.id.musicWrapper);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> hidden = prefs.getStringSet("hidden", new HashSet<String>());
        List<App> newNotHidden = new ArrayList<>();
        for(App a : apps){
            if(!hidden.contains(a.getIdentifier())) newNotHidden.add(a);
        }
        notHidden = newNotHidden;
        updateWindowFlags();
        RelativeLayout.LayoutParams mlp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        int four = (int)(getResources().getDisplayMetrics().density*4);
        if(prefs.getBoolean("hideStatusBar",false)){
            toolbar.setPadding(0,-getResources().getDimensionPixelSize(R.dimen.toolbar_top),0,0);
            clockWrapper.setPadding(0,-(int)(getResources().getDisplayMetrics().density*16),0,0);
            musicWrapper.setPadding(four*15,four*3,four*15,0);
            searchWrapper.setPadding(0,0,0,0);
            mlp.topMargin = 0;
        }else{
            toolbar.setPadding(0,0,0,0);
            clockWrapper.setPadding(0, 0, 0, 0);
            musicWrapper.setPadding(four*15, four*7, four*15, 0);
            searchWrapper.setPadding(0,(int)(getResources().getDisplayMetrics().density*24),0,0);
            mlp.topMargin = (int)(getResources().getDisplayMetrics().density*24);
        }
        drawerList.setLayoutParams(mlp);
        String headerStyle = prefs.getString("headerStyle2","Search Bar");
        if(headerStyle.equals("Clock and Date")){
            musicWrapper.setVisibility(View.GONE);
            clockWrapper.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            searchWrapper.setVisibility(View.GONE);
        }else if(headerStyle.equals("Search Bar")){
            musicWrapper.setVisibility(View.GONE);
            clockWrapper.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            searchWrapper.setVisibility(View.VISIBLE);
            View searchBar = findViewById(R.id.searchbar);
            ImageView drawerToggle = (ImageView) findViewById(R.id.drawerToggle);
            ImageView google = (ImageView) findViewById(R.id.google);
            ImageView overflowMenu = (ImageView) findViewById(R.id.overflowMenu);
            ImageView voice = (ImageView) findViewById(R.id.voice);
            int barColor = prefs.getInt("searchBarColor", Card.DEFAULT_COLOR);
            if (barColor == Card.DEFAULT_COLOR) {
                searchBar.setBackgroundResource(R.drawable.search_shape);
                ((GradientDrawable)searchBar.getBackground()).setColor(Color.WHITE);
                if (Build.VERSION.SDK_INT>=21) {
                    drawerToggle.setImageTintList(null);
                    google.setImageTintList(null);
                    overflowMenu.setImageTintList(null);
                    voice.setImageTintList(null);
                } else {
                    drawerToggle.getDrawable().setColorFilter(null);
                    google.getDrawable().setColorFilter(null);
                    overflowMenu.getDrawable().setColorFilter(null);
                    voice.getDrawable().setColorFilter(null);
                }
            } else {
                ((GradientDrawable)searchBar.getBackground()).setColor(barColor);
                int color = Color.WHITE;
                if (barColor == Color.WHITE) {
                    color = Color.parseColor("#888888");
                } else if (useBlackText(barColor)) {
                    color = Color.parseColor("#444444");
                }
                if(Build.VERSION.SDK_INT<21){
                    drawerToggle.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    google.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    overflowMenu.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    voice.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                }else {
                    drawerToggle.setImageTintList(ColorStateList.valueOf(color));
                    google.setImageTintList(ColorStateList.valueOf(color));
                    overflowMenu.setImageTintList(ColorStateList.valueOf(color));
                    voice.setImageTintList(ColorStateList.valueOf(color));
                }
            }
        } else if (headerStyle.equals("Music Controls")){
            musicWrapper.setVisibility(View.VISIBLE);
            clockWrapper.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            searchWrapper.setVisibility(View.GONE);
        } else{
            musicWrapper.setVisibility(View.GONE);
            clockWrapper.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            searchWrapper.setVisibility(View.GONE);
        }
        String theme = prefs.getString("theme", "Light");
        if(theme.equals("Auto")){
            int hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);
            theme = "Dark";
            lastThemeTime = 1;
            if(hour>=5&&hour<17){
                theme="Light";
                lastThemeTime = 2;
            }
        }
        if(drawerList!=null){
            drawerList.reconfigBar();
        }
        View background = findViewById(R.id.container);
        ImageView headerImage = (ImageView)findViewById(R.id.header_image);
        int color = prefs.getInt("bgColor",Card.DEFAULT_COLOR);
        if(color==Card.DEFAULT_COLOR) {
            if (theme.equals("Light")) {
                color = getResources().getColor(R.color.lightBack);
            }else if (theme.equals("Dark")) {
                color = getResources().getColor(R.color.darkBack);
            }else if (theme.equals("System Wallpaper")||theme.equals("Wallpaper with Header")){
                color = getResources().getColor(R.color.transparent);
            }else{
                color = Integer.parseInt(theme.split(":")[0]);
            }
        }
        if(color==Color.WHITE) color = getResources().getColor(R.color.lightBack);
        if(color==Color.BLACK) color = getResources().getColor(R.color.darkBack);
        background.setBackgroundColor(color);
        float density = getResources().getDisplayMetrics().density;
        int width = getResources().getDisplayMetrics().widthPixels;
        int minimal = (int)(density*72);
        if(headerStyle.equals("Search Bar")&&theme.equals("System Wallpaper")){
            minimal = (int)(density*80);
        }else if(headerStyle.equals("Search Bar")){
            minimal = (int)(density*88);
        }else if(headerStyle.equals("Empty")&&theme.equals("System Wallpaper")){
            minimal = (int)(density*64);
        }
        if(prefs.getBoolean("hideStatusBar",false)) minimal -= (int)(density*24);
        if(theme.equals("System Wallpaper")){
            headerImage.setVisibility(View.INVISIBLE);
            headerImage.setLayoutParams(new RelativeLayout.LayoutParams(width, minimal));
            lastHeight = minimal;
        }else if(isLandscape()){
            headerImage.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, (int) (width * 0.428823529));
            lp.topMargin = minimal-(int) (width * 0.428823529);
            headerImage.setLayoutParams(lp);
            lastHeight = minimal;
        }else{
            headerImage.setVisibility(View.VISIBLE);
            headerImage.setLayoutParams(new RelativeLayout.LayoutParams(width, (int) (width * 0.428823529)));
            lastHeight = (int) (width * 0.428823529);
        }
        if(!theme.equals(lastTheme)&&theme.equals("System Wallpaper")){
            mRecyclerView.scrollToPosition(0);
        }
        int top = (int) (width * 0.428823529);
        int eight = (int)(density*8);
        int bottom = getResources().getDimensionPixelSize(R.dimen.card_bottom);
        if(theme.equals("System Wallpaper")){
            background.setPadding(0, 0, 0, 0);
            mRecyclerView.setPadding(0, minimal+eight, 0, bottom);
        }else if(isLandscape()){
            background.setPadding(0, minimal, 0, 0);
            mRecyclerView.setPadding(0, eight, 0, bottom);
        }else{
            background.setPadding(0, top, 0, 0);
            mRecyclerView.setPadding(0, eight, 0, bottom);
        }
        for(Card c : cards){
            c.refreshColor();
            if(c instanceof Card.Apps){
                ((Card.Apps)c).getAdapter().notifyDataSetChanged();
            }else if(c instanceof Card.Widget){
                SFHostView sfhv = ((Card.Widget)c).view;
                enableNestedScrolling(sfhv);
            }else if(c instanceof Card.Web){
                ((Card.Web)c).refresh();
            }
        }
        drawerColor = prefs.getInt("drawerColor", Color.parseColor("#607D8B"));
        if(prefs.getBoolean("drawertrans",false)){
            drawerColor = Color.argb(120, Color.red(drawerColor), Color.green(drawerColor), Color.blue(drawerColor));
        }
        findViewById(R.id.drawerContainer).setBackgroundColor(drawerColor);
        findViewById(R.id.left_drawer).setBackgroundColor(drawerColor);

        if(prefs.getBoolean("useGrid",false)){
            if(prefs.getString("iconsize","Medium").equals("Small")){
                drawerList.setNumColumns(4);
            }else drawerList.setNumColumns(3);
        }else{
            drawerList.setNumColumns(1);
        }
        if(navigationDrawerAdapter!=null){
            navigationDrawerAdapter.notifyDataSetChanged();
        }
        if (theme.equals("System Wallpaper") || isLandscape()) {
            findViewById(R.id.musicControls).setVisibility(View.GONE);
        } else {
            findViewById(R.id.musicControls).setVisibility(View.VISIBLE);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            String notificationListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            if (notificationListeners == null || !notificationListeners.contains(MediaListener.class.getName())) {
                promptForMusicAccess();
            }
        }
        lastTheme = theme;
        refreshHeader();
        initialized = true;
    }

    private static boolean alreadyPrompted = false;

    private void promptForMusicAccess() {
        if (alreadyPrompted) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getString("headerStyle2", "Search Bar").equals("Music Controls") &&
                !prefs.getString("city", "San Francisco").equals("Music:music-theme")) {
            return;
        }
        alreadyPrompted = true;
        AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
        d.setTitle("Notification Access Required");
        d.setMessage("SF Launcher requires access to notifications in order to display music info and album art. Please enable it on the next page.");
        d.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        d.show();
    }

    public static boolean initialized = false;

    public static int drawerColor = Color.parseColor("#607D8B");

    private boolean fromSettings = false;

    private static final int REQUEST_BIND_APPWIDGET = 4567;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(notHidden.size()==0){
            refreshAppList();
        }else if(cards.size()==0){
            restoreCards();
            refreshThemes();
        }else{
            refreshHeader();
        }
        if(requestCode==101){
            SettingsActivity.apps.clear();
            SettingsActivity.apps = new ArrayList<>();
            if(resultCode==12345){
                cards.add(1,new Card.Tutorial(this));
                mRecyclerView.scrollToPosition(0);
            }
            cancelResize();
            fromSettings = true;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String orientation = prefs.getString("orientation", "Automatic");
            if(orientation.equals("Automatic")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }else if(orientation.equals("Portrait")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }else if(orientation.equals("Landscape")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            refreshThemes();
        }
        if (resultCode == RESULT_OK ) {
            if (requestCode == 1) {
                Bundle extras = data.getExtras();
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                createWidget(appWidgetId);
            } else if(requestCode==119){
                createShortcut(data);
            } else if (requestCode == REQUEST_BIND_APPWIDGET) {
                Bundle extras = data.getExtras();
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                configureWidget(appWidgetId);
            }
        }
        else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId =
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    /**
     * Sets up a widget that the user has selected.
     */
    private void configureWidget(int appWidgetId) {
        AppWidgetProviderInfo appWidgetInfo =
                mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent =
                    new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, 1);
        } else {
            createWidget(appWidgetId);
        }
    }

    /**
     * Creates a new widget for the type the user selected.
     */
    public void createWidget(int appWidgetId) {
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        int height = appWidgetInfo.minHeight;
        float density = getResources().getDisplayMetrics().density;
        if (height < density * 56) height = (int) (density * 56);
        String packageName = appWidgetInfo.provider.getPackageName();
        String className = appWidgetInfo.provider.getClassName();
        Card.WidgetStub w = remakeWidget(appWidgetId, height, Card.Widget.DEFAULT_COLOR, packageName, className);
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i) instanceof Card.WidgetStub) {
                Card.WidgetStub ws = (Card.WidgetStub) cards.get(i);
                if (ws.id == w.id) {
                    w.color = ws.color;
                    w.height = ws.height;
                    w.getCardContainer().setLayoutParams(ws.getCardContainer().getLayoutParams());
                    cards.remove(i);
                    cards.add(i, w);
                    w.refreshColor();
                    saveCards();
                    mAdapter.notifyDataSetChanged();
                    return;
                }
            }
        }
        cards.add(w);
        saveCards();
        mAdapter.notifyDataSetChanged();
    }

    private void restoreCards(){
        Parcelable scrollState = null;
        if (cards.size() > 0) {
            scrollState = mLayoutManager.onSaveInstanceState();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String[] strs = prefs.getString("cards", "").split(";");
        SuperCardList newCards = new SuperCardList(this, mRecyclerView);
        newCards.quickAdd(new Card.Empty(this));
        for(String s : strs){
            String[] parts = s.split(":");
            if(parts[0].equals("widget")){
                int id = Integer.parseInt(parts[1]);
                int height = Integer.parseInt(parts[2]);
                int color = Integer.parseInt(parts[3]);
                AppWidgetProviderInfo info = mAppWidgetManager.getAppWidgetInfo(id);
                String packageName = null, className = null;
                if (info != null) {
                    packageName = info.provider.getPackageName();
                    className = info.provider.getClassName();
                }
                Card.WidgetStub w = remakeWidget(id, height, color, packageName, className);
                w.refreshColor();
                newCards.quickAdd(w);
            }else if(parts[0].equals("widget2")){
                int id = Integer.parseInt(parts[1]);
                int height = Integer.parseInt(parts[2]);
                int color = Integer.parseInt(parts[3]);
                String packageName = parts[4];
                String className = parts[5];
                Card.WidgetStub w = remakeWidget(id, height, color, packageName, className);
                if (id != w.id) {
                    mAppWidgetHost.deleteAppWidgetId(id);
                }
                w.refreshColor();
                newCards.quickAdd(w);
            }else if(parts[0].equals("apps")){
                int columns = Integer.parseInt(parts[1]);
                int color = Integer.parseInt(parts[2]);
                if(parts.length<4)continue;
                String appString = parts[3];
                Card.Apps ca = new Card.Apps(this);
                ca.setColumns(columns);
                ca.color = color;
                ca.restoreApps(appString);
                ca.refreshColor();
                if(ca.getNumApps()>0)newCards.quickAdd(ca);
            }else if(parts[0].equals("web")){
                String url = App.decodeText(parts[1]);
                int height = Integer.parseInt(parts[2]);
                int color = Integer.parseInt(parts[3]);
                Card.Web web = new Card.Web(this, url, height);
                web.color = color;
                web.refreshColor();
                newCards.quickAdd(web);
            }else if(parts[0].equals("tutorial")){
                int current = Integer.parseInt(parts[1]);
                Card.Tutorial t = new Card.Tutorial(this);
                for(int i=0; i<current; i++) t.advance();
                newCards.quickAdd(t);
            }else break;
        }
        cards = newCards;
        cards.adapter = mAdapter;
        cards.widgetHost = mAppWidgetHost;
        mAdapter.notifyDataSetChanged();
        mLayoutManager.scrollToPosition(0);
        if (scrollState != null) {
            mLayoutManager.onRestoreInstanceState(scrollState);
        }
    }

    private Card.WidgetStub remakeWidget(int id, final int oldHeight, int oldColor,
                                         final String packageName, final String className){
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(id);
        if (appWidgetInfo == null) {
            Card.WidgetStub widget = new Card.WidgetStub(this);
            final int widgetId = mAppWidgetHost.allocateAppWidgetId();
            widget.id = widgetId;
            widget.height = oldHeight;
            widget.color = oldColor;
            widget.packageName = packageName;
            widget.className = className;
            ViewGroup v = widget.getCardContainer();
            RelativeLayout view = (RelativeLayout) getLayoutInflater().inflate(R.layout.widget_stub, null);
            ImageView iv = (ImageView) view.findViewById(R.id.image);
            try {
                iv.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ComponentName name = new ComponentName(packageName, className);

                    boolean result = mAppWidgetManager.bindAppWidgetIdIfAllowed(widgetId, name);
                    if (result) {
                        configureWidget(widgetId);
                    } else {
                        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, name);
                        startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
                    }
                }
            });
            int height = widget.height;
            DisplayMetrics d = getResources().getDisplayMetrics();
            if(height==Card.Widget.AUTOMATIC_HEIGHT){
                height=(int)(DEFAULT_WIDGET_SIZE*d.density);
                ViewGroup.LayoutParams lp2 = v.getLayoutParams();
                lp2.height = height;
                v.setLayoutParams(lp2);
            }else{
                ViewGroup.LayoutParams lp2 = v.getLayoutParams();
                lp2.height = height;
                v.setLayoutParams(lp2);
            }
            view.setLongClickable(true);
            v.addView(view);
            widget.refreshColor();
            return widget;
        }
        SFHostView hostView = (SFHostView)mAppWidgetHost.createView(this, id, appWidgetInfo);
        hostView.setPadding(0, 0, 0, 0);
        hostView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        final Card.Widget widget = new Card.Widget(this);
        widget.view = hostView;
        widget.id = id;
        widget.height = oldHeight;
        widget.color = oldColor;
        widget.packageName = packageName;
        widget.className = className;
        ViewGroup v = widget.getCardContainer();
        int height = widget.height;
        DisplayMetrics d = getResources().getDisplayMetrics();
        int width = (int)(328*d.density);
        int maxwidth = width;
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fitcards",true)){
            int screenwidth = Math.max(d.widthPixels, d.heightPixels);
            maxwidth = screenwidth - (int)(32*d.density);
        }
        if(height==Card.Widget.AUTOMATIC_HEIGHT){
            height=(int)(DEFAULT_WIDGET_SIZE*d.density);
            int wheight = height - (int)(d.density*16);
            widget.view.updateAppWidgetSize(null, width, wheight, maxwidth, wheight);
            ViewGroup.LayoutParams lp2 = v.getLayoutParams();
            lp2.height = height;
            v.setLayoutParams(lp2);
        }else{
            int wheight = height - (int)(d.density*16);
            widget.view.updateAppWidgetSize(null, width, wheight, maxwidth, wheight);
            ViewGroup.LayoutParams lp2 = v.getLayoutParams();
            lp2.height = height;
            v.setLayoutParams(lp2);
        }
        widget.view.setLongClickable(true);
        v.addView(widget.view);
        widget.refreshColor();
        return widget;
    }

    public void saveCards(){
        saveCards(this);
    }

    public static void saveCards(MainActivity main){
        String data = "";
        for(Card c : main.cards){
            if(c instanceof Card.WidgetStub){
                Card.WidgetStub w = (Card.WidgetStub) c;
                data+=";widget2:"+w.id+":"+w.height+":"+w.color+":"+w.packageName+":"+w.className;
            }else if(c instanceof Card.Apps){
                Card.Apps a = (Card.Apps) c;
                if(a.getNumApps()==0)continue;
                data+=";apps:"+a.getColumns()+":"+a.color+":"+a.getAppString();
            }else if(c instanceof Card.Web){
                Card.Web web = (Card.Web) c;
                data+=";web:"+App.encodeText(web.url)+":"+web.height+":"+web.color;
            }else if(c instanceof Card.Tutorial){
                Card.Tutorial t = (Card.Tutorial) c;
                data+=";tutorial:"+t.current;
            }
        }
        if(data.length()>0)data = data.substring(1);
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(main).edit();
        e.putString("cards",data);
        e.apply();
    }


    public boolean startAppListDrag(App app, ImageView iv, boolean fromList){
        return startAppListDrag(app,iv,fromList,null,-1,-1);
    }

    public boolean startAppListDrag(final App app, ImageView iv, final boolean fromList,
                                    final Card.Apps ca, final int cardPosition, final int appPosition){
        iv.setBackgroundResource(R.color.transparent);
        final View wrapper = findViewById(R.id.dragwrapper);
        final View remove = findViewById(R.id.remove);
        final View appinfo = findViewById(R.id.appinfo);
        final View uninstall = findViewById(R.id.uninstall);
        if(fromList){
            remove.setVisibility(View.GONE);
            appinfo.setVisibility(View.VISIBLE);
            uninstall.setVisibility(View.VISIBLE);
        }else{
            remove.setVisibility(View.VISIBLE);
            appinfo.setVisibility(View.GONE);
            uninstall.setVisibility(View.GONE);
        }
        int bgrey = Card.COLORS.get("Blue Grey");
        final int bluegrey = Color.argb(160,Color.red(bgrey),Color.green(bgrey),Color.blue(bgrey));
        wrapper.setAlpha(0);
        wrapper.setVisibility(View.VISIBLE);
        wrapper.animate().alpha(1).start();
        ClipData data = ClipData.newPlainText("","");
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(iv);
        View.OnDragListener listener = new View.OnDragListener() {
            boolean alreadyDropped = false;
            @Override
            public boolean onDrag(View v, DragEvent event) {
                final int action = event.getAction();
                float x = event.getX();
                float y = event.getY();
                if(action==DragEvent.ACTION_DRAG_LOCATION){
                    addHoleIfNecessary(x, y);
                    prepNewCard(x, y);
                    if(inBox(uninstall, x, y)){
                        uninstall.setBackgroundColor(bluegrey);
                        appinfo.setBackgroundColor(Color.TRANSPARENT);
                    }else if(inBox(appinfo, x, y)){
                        appinfo.setBackgroundColor(bluegrey);
                        uninstall.setBackgroundColor(Color.TRANSPARENT);
                    }else{
                        appinfo.setBackgroundColor(Color.TRANSPARENT);
                        uninstall.setBackgroundColor(Color.TRANSPARENT);
                    }
                    if(inBox(remove, x, y)){
                        remove.setBackgroundColor(bluegrey);
                    }else remove.setBackgroundColor(Color.TRANSPARENT);

                }else if(action==DragEvent.ACTION_DROP||action==DragEvent.ACTION_DRAG_ENDED){
                    if(action==DragEvent.ACTION_DRAG_ENDED){
                        if(alreadyDropped){
                            for(Card c : cards){
                                if(c instanceof Card.Apps){
                                    ((Card.Apps) c).clearHoles();
                                }
                                c.showBars(false, false);
                            }
                            return false;
                        }else{
                            y = 100000;
                        }
                    }
                    alreadyDropped = true;
                    boolean successfulDrop = dropInList(app, x, y);
                    for(Card c : cards){
                        if(c instanceof Card.Apps){
                            ((Card.Apps) c).clearHoles();
                        }
                        c.showBars(false, false);
                    }
                    wrapper.animate().alpha(0).withEndAction(new Runnable(){

                        @Override
                        public void run() {
                            wrapper.setVisibility(View.GONE);
                        }
                    }).start();
                    if (fromList) {
                        if (inBox(uninstall, x, y)) {
                            Intent intent = new Intent(
                                    Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        } else if (inBox(appinfo, x, y)) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", app.packageName, null));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        }
                    } else if (!successfulDrop) {
                        if (!inBox(remove, x, y)) {
                            if (ca!=null) {
                                ca.addAppToPosition(appPosition, app);
                                if (cards.indexOf(ca)==-1) {
                                    cards.add(cardPosition,ca);
                                    mAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                    saveCards();
                }
                return true;
            }
        };
        getWindow().getDecorView().setOnDragListener(listener);
        if (Build.VERSION.SDK_INT >= 24) {
            iv.startDragAndDrop(data, shadow, null, 0);
        } else {
            iv.startDrag(data, shadow, null, 0);
        }
        drawerLayout.closeDrawers();
        return true;
    }

    private boolean inBox(View v, float x, float y){
        return x>=v.getX()&&x<v.getX()+v.getWidth()&&y>=v.getY()&&y<v.getY()+v.getHeight();
    }

    private void addHoleIfNecessary(float x, float y) {
        if(y<lastHeight) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        y -= findViewById(R.id.container).getY();
        y -= mRecyclerView.getY();
        if(y<0)return;
        int eight = (int)(dm.density*8);
        List<Card.Apps> toClear = new ArrayList<>();
        for(Card c : cards){
            if(c instanceof Card.Apps){
                Card.Apps ca = (Card.Apps) c;
                View v = (View)c.getWrapper().getParent();
                if(v==null)continue;
                float vy = v.getY();
                float vh = v.getHeight();
                float vx = v.getX();
                float vw = v.getWidth();
                if(y>=vy&&y<vy+vh){
                    int wx = (int)(x-vx);
                    int wy = (int)(y-vy);
                    if(wx<0)wx=0;
                    int rows = ca.getNumApps()/ca.getColumns();
                    if(ca.getNumApps()%ca.getColumns()!=0)rows++;
                    int row = (int)(wy/((vh-eight*2)/rows));
                    int column = (int)(wx/((vw-eight*4)/ca.getColumns()));
                    ca.makeHole(row, column);
                }else toClear.add(ca);
            }
        }
        for(Card.Apps ca : toClear) ca.clearHoles();

    }

    private void prepNewCard(float x, float y){
        if(y<lastHeight) return;
        y -= findViewById(R.id.container).getY();
        y -= mRecyclerView.getY();
        if(y<0) return;
        for(int i=1; i<cards.size();i++){
            Card c = cards.get(i);
            c.showBars(false, false);
            if(i<cards.size()-1){
                if(c instanceof Card.Apps) continue;
                View v = (View)c.getWrapper().getParent();
                if(v==null)continue;
                float vy = v.getY();
                float vh = v.getHeight();
                if(y<vy||y>=vy+vh) continue;
                if(c instanceof Card.Resizable||c instanceof Card.Tutorial){
                    if(y>=vy&&y<vy+vh/2){
                        c.showBars(true, false);
                        break;
                    }else if(y>=vy+vh/2&&y<vy+vh){
                        c.showBars(false, true);
                        break;
                    }
                }
            }else{
                View v = (View)c.getWrapper().getParent();
                if(v==null)continue;
                float vy = v.getY();
                float vh = v.getHeight();
                if(y>=vy&&y<vy+vh/2){
                    c.showBars(true, false);
                    break;
                }else{
                    c.showBars(false, true);
                    break;
                }
            }
        }
    }

    private boolean dropInList(App app, float x, float y){
        if(y<lastHeight) return false;
        y -= findViewById(R.id.container).getY();
        y -= mRecyclerView.getY();
        if(y<0) return false;
        for(int i=1; i<cards.size();i++){
            Card c = cards.get(i);
            c.showBars(false, false);
            View v = (View)c.getWrapper().getParent();
            if(v==null)continue;
            float vy = v.getY();
            float vh = v.getHeight();
            if(i<cards.size()-1) {
                if (y < vy || y >= vy + vh) continue;
                if (c instanceof Card.Resizable||c instanceof Card.Tutorial) {
                    if (y >= vy && y < vy + vh / 2) {
                        Card.Apps card = new Card.Apps(MainActivity.this);
                        card.addApps(app);
                        cards.add(i, card);
                        return true;
                    } else if (y >= vy + vh / 2 && y < vy + vh) {
                        Card.Apps card = new Card.Apps(MainActivity.this);
                        card.addApps(app);
                        cards.add(i + 1, card);
                        return true;
                    }
                } else {
                    ((Card.Apps) c).addToHole(app);
                    saveCards();
                    return true;
                }
            }else{
                if (y < vy || y >= vy + vh){
                    Card.Apps card = new Card.Apps(MainActivity.this);
                    card.addApps(app);
                    cards.add(card);
                    return true;
                }else{
                    if (c instanceof Card.Resizable||c instanceof Card.Tutorial) {
                        if (y >= vy && y < vy + vh / 2) {
                            Card.Apps card = new Card.Apps(MainActivity.this);
                            card.addApps(app);
                            cards.add(i, card);
                            return true;
                        } else if (y >= vy + vh / 2 && y < vy + vh) {
                            Card.Apps card = new Card.Apps(MainActivity.this);
                            card.addApps(app);
                            cards.add(i + 1, card);
                            return true;
                        }
                    } else {
                        ((Card.Apps) c).addToHole(app);
                        saveCards();
                        return true;
                    }
                }
            }
        }
        Card.Apps card = new Card.Apps(MainActivity.this);
        card.addApps(app);
        cards.add(card);
        saveCards();
        return true;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void enableNestedScrolling(View v){
        boolean shouldScroll = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("widgetScroll", false);
        if(shouldScroll){
            mRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        }else mRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_DEFAULT);
        if(Build.VERSION.SDK_INT>=21&&v!=null) {
            mRecyclerView.setNestedScrollingEnabled(shouldScroll);
            enableNestedScrolling(v, shouldScroll);
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void enableNestedScrolling(View v, boolean enabled){
        if(v instanceof AbsListView){
            v.setNestedScrollingEnabled(enabled);
        }else if(v instanceof ViewGroup){
            ViewGroup vg = (ViewGroup)v;
            for(int i=0; i < vg.getChildCount(); i++){
                enableNestedScrolling(vg.getChildAt(i), enabled);
            }
        }
    }

    private void nitView() {
        clockWrapper = findViewById(R.id.clockWrapper);
        searchWrapper = findViewById(R.id.searchbarWrapper);
        if (Build.VERSION.SDK_INT >= 21) {
            musicWrapper = findViewById(R.id.musicWrapper);
            play = (ImageButton) findViewById(R.id.musicPlay);
            next = (ImageButton) findViewById(R.id.musicNext);
            prev = (ImageButton) findViewById(R.id.musicPrev);
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(MediaListener.MEDIA_ACTION);
                    i.putExtra("type", 0);
                    sendBroadcast(i);
                }
            });
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(MediaListener.MEDIA_ACTION);
                    i.putExtra("type", 1);
                    sendBroadcast(i);
                }
            });
            prev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(MediaListener.MEDIA_ACTION);
                    i.putExtra("type", 2);
                    sendBroadcast(i);
                }
            });
        }
        findViewById(R.id.drawerToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });
        findViewById(R.id.searchbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "No search app installed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.voice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_SEARCH_LONG_PRESS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }catch(ActivityNotFoundException e){
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e2) {
                        Toast.makeText(MainActivity.this, "No search app installed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        findViewById(R.id.overflowMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu p = new PopupMenu(MainActivity.this, findViewById(R.id.target));
                p.inflate(R.menu.main);
                p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return onOptionsItemSelected(item);
                    }
                });
                p.show();
            }
        });
        drawerList = (IndexableListView) findViewById(R.id.left_drawer);
        drawerList.setFastScrollEnabled(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.cards);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        navigationDrawerAdapter = new AppDrawerAdapter(this);
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    startActivity(notHidden.get(position).getIntent());
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Activity Not Found", Toast.LENGTH_SHORT).show();
                }
                drawerLayout.closeDrawers();
            }
        });
        drawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return startAppListDrag(notHidden.get(position), (ImageView) view.findViewById(R.id.image), true);
            }
        });
        drawerList.setAdapter(navigationDrawerAdapter);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setItemViewCacheSize(1000);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RecyclerView.Adapter<CardViewHolder>() {
            @Override
            public CardViewHolder onCreateViewHolder(ViewGroup parent, int i) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.emptyframe, null);
                margin = 0;
                if(!PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("fitcards", true)){
                    int width = getResources().getDimensionPixelSize(R.dimen.card_width);
                    width = Math.min(width, getResources().getDisplayMetrics().widthPixels);
                    margin = (getResources().getDisplayMetrics().widthPixels - width) / 2;
                }
                v.setLayoutParams(new ViewGroup.LayoutParams(
                        getResources().getDisplayMetrics().widthPixels,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                return new CardViewHolder(v);
            }

            int margin = 0;
            int bonus = (int)(getResources().getDisplayMetrics().density*24);

            @Override
            public void onBindViewHolder(CardViewHolder viewHolder, int position) {
                ViewGroup container = (ViewGroup)viewHolder.itemView;
                container.removeAllViews();
                Card c = cards.get(position);
                container.setTag(c);
                c.getWrapper().setDragDistance(margin+bonus);
                c.getWrapper().findViewById(R.id.front).setPadding(margin, 0, margin, 0);
                c.getWrapper().findViewById(R.id.back).setPadding(margin+bonus, 0, margin+bonus, 0);
                c.getWrapper().setClipToPadding(false);
                viewHolder.card = c;
                if(c.getWrapper().getParent()!=null){
                    ((ViewGroup)c.getWrapper().getParent()).removeAllViews();
                }
                View up = c.getWrapper().findViewById(R.id.up);
                View down = c.getWrapper().findViewById(R.id.down);
                if(position==1){
                    up.setEnabled(false);
                    up.setAlpha(0.35f);
                }else{
                    up.setEnabled(true);
                    up.setAlpha(1);
                }
                if(position==cards.size()-1){
                    down.setEnabled(false);
                    down.setAlpha(0.35f);
                }else{
                    down.setEnabled(true);
                    down.setAlpha(1);
                }
                container.addView(c.getWrapper());
            }

            @Override
            public int getItemCount() {
                return cards.size();
            }
        };
        RecyclerView.ViewCacheExtension vce = new RecyclerView.ViewCacheExtension() {
            @Override
            public View getViewForPositionAndType(RecyclerView.Recycler recycler, int position, int type) {
                recycler.setViewCacheSize(1000);
                return null;
            }
        };
        mAdapter.setHasStableIds(false);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setViewCacheExtension(vce);
        findViewById(R.id.header_image).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
                d.setTitle("Header Image");
                ListView g = new ListView(MainActivity.this);
                g.setAdapter(new CityAdapter(getLayoutInflater()));
                d.setView(g);
                final AlertDialog dialog = d.show();
                g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        SharedPreferences.Editor e = prefs.edit();
                        String key = City.cities.get(position).name;
                        e.putString("city", key);
                        dialog.dismiss();
                        e.commit();
                        refreshHeader();
                    }
                });
                return true;
            }
        });
        View.OnLongClickListener styleLong = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
                d.setTitle("Header Style");
                ListView g = new ListView(MainActivity.this);
                final String options[];
                if (isPremium() && Build.VERSION.SDK_INT >= 21) {
                    options = new String[]{"Search Bar", "Clock and Date", "Music Controls"};
                } else options = new String[]{"Search Bar", "Clock and Date"};
                g.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, options));
                d.setView(g);
                final AlertDialog dialog = d.show();
                g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        SharedPreferences.Editor e = prefs.edit();
                        e.putString("headerStyle2", options[position]);
                        dialog.dismiss();
                        e.commit();
                        refreshThemes();
                        refreshHeader();
                    }
                });
                return true;
            }
        };
        findViewById(R.id.searchbar).setOnLongClickListener(styleLong);
        findViewById(R.id.musicTitle).setOnLongClickListener(styleLong);
        findViewById(R.id.musicArtist).setOnLongClickListener(styleLong);
        for (int i = 0; i < ((ViewGroup)clockWrapper).getChildCount(); i++) {
            ((ViewGroup)clockWrapper).getChildAt(i).setOnLongClickListener(styleLong);
        }
    }

    private static class CardViewHolder extends RecyclerView.ViewHolder{
        Card card;

        public CardViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static boolean useBlackText(int color){
        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        int brightness =
                (int)Math.sqrt(
                        rgb[0] * rgb[0] * .241 +
                                rgb[1] * rgb[1] * .691 +
                                rgb[2] * rgb[2] * .068);
        return brightness > 165;
    }

    public static boolean appInstalled(Context c,String uri){
        PackageManager pm = c.getPackageManager();
        boolean app_installed = false;
        try{
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e){
            app_installed = false;
        }
        return app_installed ;
    }

    public static boolean isPremium(Context c){
        //return appInstalled(c, "net.alamoapps.launcher.plus");
        // premium features are now free
        return true;
    }

    public boolean isPremium(){
        return isPremium(this);
    }

    public void refreshAllColor(){
        for(Card c : cards) c.refreshColor();
    }

    public void moveUp(Card c){
        int position = cards.indexOf(c);
        cards.swap(position-1, position);
    }

    public void moveDown(Card c){
        int position = cards.indexOf(c);
        cards.swap(position, position + 1);
    }

    public void changeColumns(Card c){
        final Card.Apps ca = (Card.Apps) c;
        ca.getWrapper().close();
        final NumberPicker np = (NumberPicker)getLayoutInflater().inflate(R.layout.numberpicker, null);
        np.setMinValue(1);
        np.setMaxValue(10);
        np.setValue(ca.getColumns());
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle("Number of Columns");
        d.setView(np);
        d.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ca.setColumns(np.getValue());
                saveCards();
                mAdapter.notifyItemChanged(cards.indexOf(ca));
            }
        });
        d.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        d.show();
    }

    public void changeBackground(final Card c){
        c.getWrapper().close();
        if(isPremium()){
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("Card Background Color");
            GridView g = new GridView(this);
            g.setNumColumns(3);
            g.setAdapter(new ColorAdapter(getLayoutInflater(), true));
            d.setView(g);
            final AlertDialog dialog = d.show();
            g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    c.color = Card.COLORS.get(Card.COLOR_KEYS.get(position));
                    c.refreshColor();
                    saveCards();
                    dialog.dismiss();
                }
            });
        }else{
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("SF Launcher Plus Required");
            d.setMessage("Changing card background colors requires SF Launcher Plus. Do you want to purchase it now?");
            d.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String appName = "net.alamoapps.launcher.plus";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://jathak.xyz/app_"+appName)));
                }
            });
            d.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            d.show();
        }
    }

    private void cancelResize(){
        for(Card c : cards){
            if(c instanceof Card.Widget){
                Card.Widget widget = (Card.Widget) c;
                View border = widget.getWrapper().findViewById(R.id.card_border);
                View handle = widget.getWrapper().findViewById(R.id.handle);
                border.setVisibility(View.GONE);
                handle.setVisibility(View.GONE);
                widget.getWrapper().setMinimumHeight(0);
                widget.getWrapper().setSwipeEnabled(true);
            }
        }
    }

    public void resizeCard(Card c){
        final Card.Resizable widget = (Card.Resizable) c;
        final View border = widget.getWrapper().findViewById(R.id.card_border);
        final View handle = widget.getWrapper().findViewById(R.id.handle);
        border.setVisibility(View.VISIBLE);
        handle.setVisibility(View.VISIBLE);
        final ViewGroup v = (ViewGroup)widget.getWrapper().findViewById(R.id.card_container);
        final FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(handle.getLayoutParams());
        final int size = (int)(24*getResources().getDisplayMetrics().density);
        handleParams.topMargin = v.getHeight();
        handleParams.bottomMargin = -size/3;
        handleParams.leftMargin = v.getWidth()/2;
        handle.setLayoutParams(handleParams);
        //widget.container.setPadding(0,0,0,3000);
        widget.getWrapper().setMinimumHeight(getResources().getDisplayMetrics().heightPixels);
        widget.getWrapper().close();
        widget.getWrapper().setSwipeEnabled(false);
        final int position = cards.indexOf(c);
        mLayoutManager.scrollToPositionWithOffset(position, 0);
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            float lastY = -1;
            int originalHeight = -1;
            boolean enabled = true;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!enabled) return false;
                float newY = event.getRawY();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int[] pos = new int[2];
                    handle.getLocationOnScreen(pos);
                    float x = event.getRawX();
                    float y = event.getRawY();
                    boolean inbox = x >= pos[0] - size && y >= pos[1] - size && x <= pos[0] + 2 * size && y <= pos[1] + 2 * size;
                    if (inbox) {
                        lastY = newY;
                        originalHeight = v.getLayoutParams().height;
                    } else {
                        border.setVisibility(View.GONE);
                        handle.setVisibility(View.GONE);
                        saveCards();
                        widget.getWrapper().setMinimumHeight(0);
                        widget.getWrapper().setSwipeEnabled(true);
                        mRecyclerView.scrollToPosition(position);
                        mAdapter.notifyDataSetChanged();
                        refreshAllColor();
                        enabled = false;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                    border.setVisibility(View.GONE);
                    handle.setVisibility(View.GONE);
                    saveCards();
                    widget.getWrapper().setMinimumHeight(0);
                    widget.getWrapper().setSwipeEnabled(true);
                    mRecyclerView.scrollToPosition(position);
                    mAdapter.notifyDataSetChanged();
                    refreshAllColor();
                    enabled = false;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && lastY > 0) {
                    float diff = newY - lastY;
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = originalHeight + (int) diff;
                    if(lp.height<size*2) lp.height = size*2;
                    v.setLayoutParams(lp);
                    handleParams.topMargin = lp.height;
                } else if (event.getAction() == MotionEvent.ACTION_UP && lastY > 0) {
                    float diff = newY - lastY;
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = originalHeight + (int) diff;
                    if(lp.height<size*2) lp.height = size*2;
                    v.setLayoutParams(lp);
                    handleParams.topMargin = lp.height;
                    DisplayMetrics d = getResources().getDisplayMetrics();
                    widget.height = lp.height;
                    if(widget instanceof Card.Widget) {
                        int width = (int) (328 * d.density);
                        int landwidth = width;
                        if(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("fitcards",true)){
                            landwidth = Math.max(d.widthPixels, d.heightPixels) - (int)(32 * d.density);
                        }
                        int height = widget.height - (int)(d.density*16);
                        if (height == Card.Widget.AUTOMATIC_HEIGHT) {
                            height = (int) (DEFAULT_WIDGET_SIZE * d.density);
                        }
                        ((Card.Widget)widget).view.updateAppWidgetSize(null, width, height, landwidth, height);
                    }
                    lastY = -1;
                    originalHeight = -1;
                }
                return true;
            }
        });
    }

    private void initDrawer() {

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        cancelResize();
        super.onConfigurationChanged(newConfig);
        fullViewRefresh();
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private void fullViewRefresh(){
        updateWindowFlags();
        setContentView(R.layout.activity_main);
        nitView();
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
        }
        initDrawer();
        drawerToggle.syncState();
        cards.listView = mRecyclerView;
        cards.adapter = mAdapter;
        headerImage = (ImageView) findViewById(R.id.header_image);
        refreshThemes();
        refreshHeader(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_widget) {
            selectWidget();
            return true;
        }else if(id == R.id.action_settings){
            SettingsActivity.apps = WeakApp.from(apps);
            startActivityForResult(new Intent(this, SettingsActivity.class), 101);
            return true;
        }else if(id ==R.id.action_shortcut){
            startActivityForResult(new Intent(Intent.ACTION_CREATE_SHORTCUT),119);
            return true;
        }else if(id == R.id.action_system_settings){
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }else Toast.makeText(this, "Settings could not be opened", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAppWidgetHost.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppWidgetHost.startListening();
        System.gc();
        updateWindowFlags();
        if(notHidden.size()==0){
            refreshAppList();
        }else if(cards.size()==0){
            restoreCards();
            refreshThemes();
        }else{
            refreshHeader();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mAppWidgetHost.stopListening();
        unregisterReceiver(onAppChange);
        unregisterReceiver(sfImport);
        unregisterReceiver(shortcutListener);
        unregisterReceiver(timeTickListener);
        unregisterReceiver(musicUpdateListener);
        unregisterReceiver(muzeiListener);
    }

}