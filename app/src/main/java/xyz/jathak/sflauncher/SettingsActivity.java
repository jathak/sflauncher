package xyz.jathak.sflauncher;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    public static List<WeakApp> apps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String orientation = PreferenceManager.getDefaultSharedPreferences(this).getString("orientation", "Automatic");
        if(orientation.equals("Automatic")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }else if(orientation.equals("Portrait")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else if(orientation.equals("Landscape")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setupActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }else if (id == R.id.action_license) {
            startActivity(new Intent(this, LicenseActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    private void setupSimplePreferencesScreen() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.pref_general);
        PreferenceScreen cardPrefs = (PreferenceScreen)findPreference("cardlist");
        PreferenceScreen drawerPrefs = (PreferenceScreen)findPreference("appdrawer");
        final Preference city = new Preference(this);
        city.setTitle("Header Image");
        city.setOrder(2);
        city.setSummary(prefs.getString("city", "San Francisco").split(":")[0]);
        city.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder d = new AlertDialog.Builder(SettingsActivity.this);
                d.setTitle("Header Image");
                ListView g = new ListView(SettingsActivity.this);
                g.setAdapter(new CityAdapter(getLayoutInflater()));
                d.setView(g);
                final AlertDialog dialog = d.show();
                g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        SharedPreferences.Editor e = prefs.edit();
                        String key = City.cities.get(position).name;
                        e.putString("city", key);
                        city.setSummary(key.split(":")[0]);
                        dialog.dismiss();
                        e.commit();
                    }
                });
                return true;
            }
        });
        getPreferenceScreen().addPreference(city);
        if(!MainActivity.isPremium(this)){
            Preference p = new Preference(this);
            p.setOrder(1000);
            p.setTitle("Upgrade to SF Launcher Plus");
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String appName = "net.alamoapps.launcher.plus";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://jathak.xyz/app_"+appName)));
                    return true;
                }
            });
            getPreferenceScreen().addPreference(p);
            Preference searchColorDisabled = new Preference(this);
            searchColorDisabled.setEnabled(false);
            searchColorDisabled.setOrder(95);
            searchColorDisabled.setTitle("Search Bar Color");
            searchColorDisabled.setSummary("SF Launcher Plus Required");
            getPreferenceScreen().addPreference(searchColorDisabled);
            Preference iconsDisabled = new Preference(this);
            iconsDisabled.setEnabled(false);
            iconsDisabled.setOrder(96);
            iconsDisabled.setTitle("Icon Pack");
            iconsDisabled.setSummary("SF Launcher Plus Required");
            getPreferenceScreen().addPreference(iconsDisabled);
            Preference bgDisabled = new Preference(this);
            bgDisabled.setEnabled(false);
            bgDisabled.setOrder(97);
            bgDisabled.setTitle("Background Color");
            bgDisabled.setSummary("SF Launcher Plus Required");
            cardPrefs.addPreference(bgDisabled);
            final Preference drawerColor = new Preference(this);
            drawerColor.setTitle("App Drawer Color");
            drawerColor.setOrder(98);
            drawerColor.setSummary("Upgrade for more color options");
            drawerColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder d = new AlertDialog.Builder(SettingsActivity.this);
                    d.setTitle("App Drawer Color");
                    GridView g = new GridView(SettingsActivity.this);
                    g.setNumColumns(3);
                    final List<String> colors = new ArrayList<String>();
                    colors.add("Red");
                    colors.add("Orange");
                    colors.add("Yellow");
                    colors.add("Green");
                    colors.add("Blue");
                    colors.add("Indigo");
                    colors.add("Purple");
                    colors.add("Blue Grey");
                    colors.add("White");
                    g.setAdapter(new ColorAdapter(getLayoutInflater(), colors));
                    d.setView(g);
                    final AlertDialog dialog = d.show();
                    g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            SharedPreferences.Editor e = prefs.edit();
                            String key = colors.get(position);
                            int color = Card.COLORS.get(key);
                            e.putInt("drawerColor", color);
                            dialog.dismiss();
                            e.commit();
                        }
                    });
                    return true;
                }
            });
            drawerPrefs.addPreference(drawerColor);
            Preference hiddenDisabled = new Preference(this);
            hiddenDisabled.setEnabled(false);
            hiddenDisabled.setOrder(501);
            hiddenDisabled.setTitle("Hidden Apps");
            hiddenDisabled.setSummary("SF Launcher Plus Required");
            drawerPrefs.addPreference(hiddenDisabled);
        }else{
            ListPreference theme = (ListPreference)findPreference("theme");
            reconfigThemePref(theme);
            ListPreference headerStyle = (ListPreference)findPreference("headerStyle2");
            headerStyle.setEntries(R.array.headerStylesPremium);
            headerStyle.setEntryValues(R.array.headerStylesPremium);
            Intent iapex = new Intent(Intent.ACTION_MAIN, null);
            iapex.addCategory("com.anddoes.launcher.THEME");
            List<ResolveInfo> apex=  getPackageManager().queryIntentActivities(iapex, PackageManager.PERMISSION_GRANTED);
            apex.add(0, null);
            final Preference searchColor = new Preference(this);
            searchColor.setTitle("Search Bar Color");
            searchColor.setOrder(95);
            searchColor.setSummary(Card.getColorDisplayName(prefs.getInt("searchBarColor", Card.DEFAULT_COLOR)));
            searchColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder d = new AlertDialog.Builder(SettingsActivity.this);
                    d.setTitle("Search Bar Color");
                    GridView g = new GridView(SettingsActivity.this);
                    g.setNumColumns(3);
                    final List<String> colors = new ArrayList<>();
                    for (String name : Card.COLOR_KEYS) {
                        if (!name.equals("Transparent")) colors.add(name);
                    }
                    g.setAdapter(new ColorAdapter(getLayoutInflater(), colors));
                    d.setView(g);
                    final AlertDialog dialog = d.show();
                    g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            SharedPreferences.Editor e = prefs.edit();
                            String key = colors.get(position);
                            int color = Card.COLORS.get(key);
                            e.putInt("searchBarColor", color);
                            dialog.dismiss();
                            e.commit();
                        }
                    });
                    return true;
                }
            });
            getPreferenceScreen().addPreference(searchColor);
            ListPreference icons = new ListPreference(this);
            CharSequence[] values = new String[apex.size()];
            CharSequence[] names = new String[apex.size()];
            for(int i=0;i<apex.size();i++){
                ResolveInfo r = apex.get(i);
                if(r!=null) {
                    names[i] = r.loadLabel(getPackageManager());
                    values[i] = r.activityInfo.packageName + "/" + r.activityInfo.name;
                }else{
                    names[i] = "Default";
                    values[i] = "Default";
                }
            }
            icons.setTitle("Icon Pack");
            icons.setKey("iconpack");
            icons.setOrder(96);
            icons.setDefaultValue("Default");
            icons.setSummary(prefs.getString("iconpack", "Default"));
            icons.setEntries(names);
            icons.setEntryValues(values);
            getPreferenceScreen().addPreference(icons);
            bindPreferenceSummaryToValue(icons);
            final Preference bgColor = new Preference(this);
            bgColor.setTitle("Background Color");
            bgColor.setOrder(97);
            bgColor.setSummary(Card.getColorDisplayName(prefs.getInt("bgColor", Card.DEFAULT_COLOR)));
            bgColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder d = new AlertDialog.Builder(SettingsActivity.this);
                    d.setTitle("Background Color");
                    GridView g = new GridView(SettingsActivity.this);
                    g.setNumColumns(3);
                    g.setAdapter(new ColorAdapter(getLayoutInflater(), true));
                    d.setView(g);
                    final AlertDialog dialog = d.show();
                    g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            SharedPreferences.Editor e = prefs.edit();
                            String key = Card.COLOR_KEYS.get(position);
                            int color = Card.COLORS.get(key);
                            e.putInt("bgColor",color);
                            bgColor.setSummary(key.split(":")[0]);
                            dialog.dismiss();
                            e.commit();
                        }
                    });
                    return true;
                }
            });
            cardPrefs.addPreference(bgColor);
            final Preference drawerColor = new Preference(this);
            drawerColor.setTitle("App Drawer Color");
            drawerColor.setOrder(98);
            drawerColor.setSummary(Card.getColorDisplayName(prefs.getInt("drawerColor", Color.parseColor("#607D8B"))));
            drawerColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder d = new AlertDialog.Builder(SettingsActivity.this);
                    d.setTitle("App Drawer Color");
                    GridView g = new GridView(SettingsActivity.this);
                    g.setNumColumns(3);
                    g.setAdapter(new ColorAdapter(getLayoutInflater(), false));
                    d.setView(g);
                    final AlertDialog dialog = d.show();
                    g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            SharedPreferences.Editor e = prefs.edit();
                            String key = Card.COLOR_KEYS.get(position + 2);
                            int color = Card.COLORS.get(key);
                            e.putInt("drawerColor",color);
                            drawerColor.setSummary(key.split(":")[0]);
                            dialog.dismiss();
                            e.commit();
                        }
                    });
                    return true;
                }
            });
            drawerPrefs.addPreference(drawerColor);
            CheckBoxPreference transparency = new CheckBoxPreference(this);
            transparency.setTitle("Translucent App Drawer");
            transparency.setKey("drawertrans");
            transparency.setDefaultValue(false);
            transparency.setOrder(99);
            drawerPrefs.addPreference(transparency);
            MultiSelectListPreference hidden = new MultiSelectListPreference(this);
            hidden.setTitle("Hidden Apps");
            hidden.setKey("hidden");
            hidden.setOrder(501);
            hidden.setSummary("Only Hides Apps in Drawer");
            CharSequence[] hiddenEntries = new CharSequence[apps.size()];
            CharSequence[] hiddenValues = new CharSequence[apps.size()];
            for(int i = 0; i < apps.size(); i++){
                WeakApp a = apps.get(i);
                hiddenEntries[i] = a.name;
                hiddenValues[i] = a.getIdentifier();
            }
            hidden.setEntries(hiddenEntries);
            hidden.setEntryValues(hiddenValues);
            drawerPrefs.addPreference(hidden);
            bindPreferenceSummaryToValue(hidden);
            Preference custom = new Preference(this);
            custom.setOrder(502);
            custom.setTitle("Custom Color Swatches");
            custom.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(SettingsActivity.this, CustomColorActivity.class));
                    return true;
                }
            });
            getPreferenceScreen().addPreference(custom);
        }
        Preference wallpaper = new Preference(this);
        wallpaper.setTitle("Set Wallpaper");
        wallpaper.setSummary("For System Wallpaper theme");
        wallpaper.setOrder(503);
        wallpaper.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, "Select Wallpaper"));
                return true;
            }
        });
        getPreferenceScreen().addPreference(wallpaper);
        Preference backup = new Preference(this);
        backup.setOrder(504);
        backup.setTitle("Backup and Restore");
        backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, BackupRestoreActivity.class));
                return true;
            }
        });
        getPreferenceScreen().addPreference(backup);
        //if(Build.VERSION.SDK_INT>=21) {
        CheckBoxPreference scrolling = new CheckBoxPreference(this);
        scrolling.setTitle("Allow Scrolling Widgets");
        scrolling.setSummary("Can cause issues");
        scrolling.setKey("widgetScroll");
        scrolling.setDefaultValue(false);
        scrolling.setOrder(502);
        cardPrefs.addPreference(scrolling);
        //}

        Preference importFavs = new Preference(this);
        importFavs.setOrder(900);
        importFavs.setTitle("Import Favorites from Old Version");
        importFavs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MainActivity.awaitingImport = true;
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("net.alamoapps.launcher","net.alamoapps.launcher.ExportService"));
                startService(intent);
                finish();
                return true;
            }
        });
        if(WeakApp.packageExists(apps, "net.alamoapps.launcher"))getPreferenceScreen().addPreference(importFavs);
        Preference tutorial = new Preference(this);
        tutorial.setOrder(901);
        tutorial.setTitle("Add Tutorial Card");
        tutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setResult(12345);
                finish();
                return true;
            }
        });
        getPreferenceScreen().addPreference(tutorial);
        bindPreferenceSummaryToValue(findPreference("theme"));
        bindPreferenceSummaryToValue(findPreference("orientation"));
        bindPreferenceSummaryToValue(findPreference("iconsize"));
        bindPreferenceSummaryToValue(findPreference("headerStyle2"));
    }

    private void reconfigThemePref(ListPreference p){
        List<CharSequence> names = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        names.addAll(Arrays.asList(p.getEntries()));
        values.addAll(Arrays.asList(p.getEntryValues()));
        for(Theme t : Theme.loadThemes(this)){
            for(Theme.Background b : t.backgrounds){
                names.add(b.name);
                values.add(b.getBgColor()+":"+b.getCardColor());
            }
        }
        p.setEntries(names.toArray(new CharSequence[names.size()]));
        p.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            if(preference instanceof MultiSelectListPreference){
                int count = ((MultiSelectListPreference) preference).getValues().size();
                if(count==0){
                    preference.setSummary("Only Hides Apps in Drawer");
                }else{
                    preference.setSummary(count+" apps hidden");
                }
            }else if (preference instanceof ListPreference) {
                String stringValue = value.toString();
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                String stringValue = value.toString();
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        if(preference instanceof MultiSelectListPreference){
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getStringSet(preference.getKey(), new HashSet<String>()));
        }else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }
}
