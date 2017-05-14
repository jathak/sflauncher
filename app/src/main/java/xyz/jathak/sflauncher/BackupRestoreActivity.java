package xyz.jathak.sflauncher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class BackupRestoreActivity extends Activity {

    private ListView lv;
    private List<String> backupNames;
    private List<File> backupFiles;
    private BaseAdapter adapter;

    private static final int PERMISSION_REQUEST = 243;

    private File getBackupsDir() {
        File data = Environment.getExternalStoragePublicDirectory("data");
        return new File(new File(data, "xyz.jathak.sflauncher"), "backups");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_color);
        setTitle("Backup and Restore");
        Intent intent = getIntent();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED ) {
            savedIntent = intent;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        } else {
            load(intent);
        }
    }

    private Intent savedIntent;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            load(savedIntent);
        } else {
            Toast.makeText(this, "Backup and restore requires access to internal storage!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void load(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            Uri uri = intent.getData();
            String name = uri.getLastPathSegment();
            String input = readFile(new File(uri.getPath()));
            File output = new File(getBackupsDir(), name);
;           boolean result = writeFile(output, input);
            if (result) {
                Toast.makeText(this, name+" successfully imported", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, "Backup could not be imported", Toast.LENGTH_SHORT).show();
        }
        File dir = getBackupsDir();
        if (!dir.exists()) dir.mkdirs();
        backupFiles = getConfigFiles(dir);
        backupNames = new ArrayList<>();
        for (File f : backupFiles) {
            backupNames.add(f.getName().substring(0, f.getName().length() - ".sfcfg".length()));
        }
        lv = (ListView)findViewById(R.id.list);
        adapter = new BaseAdapter(){
            @Override
            public int getCount() {
                return backupFiles.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                ViewHolder vh;
                if(convertView==null){
                    convertView = getLayoutInflater().inflate(R.layout.item_colorlist, null);
                    vh = new ViewHolder();
                    vh.label = (TextView)convertView.findViewById(R.id.label);
                    vh.delete = convertView.findViewById(R.id.delete);
                    vh.color = convertView.findViewById(R.id.color);
                    vh.color.setVisibility(View.INVISIBLE);
                    vh.delete.setVisibility(View.GONE);
                    convertView.setTag(vh);
                }else vh = (ViewHolder) convertView.getTag();
                vh.label.setText(backupNames.get(position));
                return convertView;
            }
        };
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                AlertDialog.Builder d = new AlertDialog.Builder(BackupRestoreActivity.this);
                d.setTitle("Restore this backup?");
                ListView g = new ListView(BackupRestoreActivity.this);
                final String[] options = {"Settings Only", "Settings and Cards", "Cancel"};
                g.setAdapter(new ArrayAdapter<>(BackupRestoreActivity.this, android.R.layout.simple_list_item_1, options));
                d.setView(g);
                final AlertDialog dialog = d.show();
                g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        dialog.dismiss();
                        if (position == 2) return;
                        boolean refreshCards = position == 1;
                        String backup = readFile(backupFiles.get(i));
                        if (backup == null) {
                            Toast.makeText(BackupRestoreActivity.this, "Backup corrupted", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!restoreConfig(backup, refreshCards, BackupRestoreActivity.this)) {
                            Toast.makeText(BackupRestoreActivity.this, "Unable to restore backup", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(BackupRestoreActivity.this, "Backup restored", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(BackupRestoreActivity.this, MainActivity.class);
                        if (refreshCards) intent.putExtra("refresh-cards", true);
                        startActivity(intent);
                    }
                });
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                AlertDialog.Builder d = new AlertDialog.Builder(BackupRestoreActivity.this);
                d.setTitle(backupNames.get(i));
                ListView g = new ListView(BackupRestoreActivity.this);
                final String[] options = {"Share", "Delete"};
                g.setAdapter(new ArrayAdapter<>(BackupRestoreActivity.this, android.R.layout.simple_list_item_1, options));
                d.setView(g);
                final AlertDialog dialog = d.show();
                g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        dialog.dismiss();
                        if (position == 0) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("application/vnd.sflauncher.backup");
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(backupFiles.get(i)));
                            intent.putExtra(Intent.EXTRA_TITLE, backupNames.get(i));
                            startActivity(intent);
                        } else if (position == 1) {
                            backupFiles.get(i).delete();
                            backupFiles.remove(i);
                            backupNames.remove(i);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
                return true;
            }
        });
    }

    static class ViewHolder{
        TextView label;
        View delete;
        View color;
    }

    private List<File> getConfigFiles(File parentDir) {
        List<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        if (files == null) return inFiles;
        for (File f : files) {
            if (!f.isDirectory() && f.getName().endsWith(".sfcfg")) {
                inFiles.add(f);
            }
        }
        return inFiles;
    }

    private String readFile(File file) {
        System.out.println(file.getAbsolutePath());
        try {
            int length = (int) file.length();
            byte[] bytes = new byte[length];

            FileInputStream in = new FileInputStream(file);
            in.read(bytes);
            in.close();
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean writeFile(File file, String contents) {
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(contents.getBytes());
            stream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_backup_restore, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_backup) {
            final AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("Backup Name");
            final EditText ev = new EditText(this);
            d.setView(ev);
            ev.setText("");
            d.setPositiveButton("Backup", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = ev.getText().toString();
                    if (backupNames.contains(name)) {
                        Toast.makeText(BackupRestoreActivity.this, "Backup with that name already exists", Toast.LENGTH_SHORT).show();
                    }
                    String backup = getConfigJson(BackupRestoreActivity.this);
                    if (backup == null) {
                        Toast.makeText(BackupRestoreActivity.this, "Could not complete backup", Toast.LENGTH_SHORT).show();
                    }
                    File backupFile = new File(getBackupsDir(), name + ".sfcfg");
                    if (writeFile(backupFile, backup)) {
                        backupFiles.add(backupFile);
                        backupNames.add(name);
                    } else {
                        Toast.makeText(BackupRestoreActivity.this, "Could not save backup to file", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            d.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            d.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    public static boolean restoreConfig(String json, boolean restoreCards, Context ctx) {
        try {
            JSONObject config = new JSONObject(json);
            JSONObject settings = config.getJSONObject("settings");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            SharedPreferences.Editor e = prefs.edit();
            e.putString("theme", settings.getString("theme"));
            e.putString("city", settings.getJSONObject("header").getString("image"));
            e.putString("headerStyle2", settings.getJSONObject("header").getString("style"));
            e.putInt("searchBarColor", settings.getJSONObject("header").getInt("color"));
            e.putString("orientation", settings.getString("orientation"));
            e.putInt("bgColor", settings.getJSONObject("cards").getInt("background"));
            e.putBoolean("fitwidth", settings.getJSONObject("cards").getBoolean("fitWidth"));
            e.putBoolean("transonwallpaper", settings.getJSONObject("cards").getBoolean("noWidgetBackgrounds"));
            e.putBoolean("widgetScroll", settings.getJSONObject("cards").getBoolean("scrollingWidgets"));
            e.putInt("drawerColor", settings.getJSONObject("drawer").getInt("color"));
            e.putBoolean("drawertrans", settings.getJSONObject("drawer").getBoolean("translucent"));
            e.putBoolean("alphaindex", settings.getJSONObject("drawer").getBoolean("index"));
            e.putBoolean("useGrid", settings.getJSONObject("drawer").getBoolean("grid"));
            e.putString("iconsize", settings.getJSONObject("drawer").getString("iconSize"));
            e.putBoolean("hideStatusBar", settings.getBoolean("hideStatusBar"));
            Set<String> hiddenApps = new HashSet<>();
            JSONArray jsonHide = settings.getJSONArray("hiddenApps");
            for (int i = 0; i < jsonHide.length(); i++) {
                hiddenApps.add(jsonHide.getString(i));
            }
            e.putStringSet("hiddenApps", hiddenApps);
            String swatchStrs = "";
            JSONArray jsonSwatch = settings.getJSONArray("swatches");
            for (int i = 0; i < jsonSwatch.length(); i++) {
                swatchStrs += ";" + jsonSwatch.getString(i);
            }
            if (swatchStrs.length() > 0) swatchStrs = swatchStrs.substring(1);
            e.putString("customColors", swatchStrs);
            if (restoreCards) {
                JSONArray cards = config.getJSONArray("cards");
                String data = "";
                for (int i = 0; i < cards.length(); i++) {
                    JSONObject card = cards.getJSONObject(i);
                    if (card.getString("type").equals("str")) {
                        data += ";" + card.getString("data");
                    }
                }
                if(data.length() > 0) data = data.substring(1);
                e.putString("cards", data);
            }
            e.commit();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static String getConfigJson(Context ctx) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            JSONObject config = new JSONObject();
            JSONObject settings = new JSONObject();
            settings.put("theme", prefs.getString("theme", "Light"));
            JSONObject header = new JSONObject();
            header.put("image", prefs.getString("city", "San Francisco"));
            header.put("style", prefs.getString("headerStyle2", "Search Bar"));
            header.put("color", prefs.getInt("searchBarColor", Card.DEFAULT_COLOR));
            settings.put("header", header);
            settings.put("orientation", prefs.getString("orientation", "Automatic"));
            JSONObject cardSettings = new JSONObject();
            cardSettings.put("background", prefs.getInt("bgColor", Card.DEFAULT_COLOR));
            cardSettings.put("fitWidth", prefs.getBoolean("fitwidth", false));
            cardSettings.put("noWidgetBackgrounds", prefs.getBoolean("transonwallpaper", false));
            cardSettings.put("scrollingWidgets", prefs.getBoolean("widgetScroll", false));
            settings.put("cards", cardSettings);
            JSONObject drawer = new JSONObject();
            drawer.put("color", prefs.getInt("drawerColor", Card.COLORS.get("Blue Grey")));
            drawer.put("translucent", prefs.getBoolean("drawertrans", false));
            drawer.put("index", prefs.getBoolean("alphaindex", true));
            drawer.put("grid", prefs.getBoolean("useGrid", false));
            drawer.put("iconSize", prefs.getString("iconsize", "Medium"));
            settings.put("drawer", drawer);
            settings.put("hideStatusBar", prefs.getBoolean("hideStatusBar", false));
            JSONArray hiddenApps = new JSONArray();
            Set<String> hidden = prefs.getStringSet("hidden", new HashSet<String>());
            for (String hide : hidden) hiddenApps.put(hide);
            settings.put("hiddenApps", hiddenApps);
            JSONArray swatches = new JSONArray();
            String swatchStr = PreferenceManager.getDefaultSharedPreferences(ctx).getString("customColors", "");
            if(swatchStr.length()>0) {
                String[] sStrs = swatchStr.split(";");
                for (String s : sStrs) {
                    swatches.put(s);
                }
            }
            settings.put("swatches", swatches);
            config.put("settings", settings);
            JSONArray cards = new JSONArray();
            String[] strs = prefs.getString("cards", "").split(";");
            for (String s : strs) {
                JSONObject card = jsonCard(s);
                if (card != null) cards.put(card);
            }
            config.put("cards", cards);
            return config.toString(4);
        } catch (JSONException e) {
            return null;
        }
    }

    public static JSONObject jsonCard(String str) {
        String[] parts = str.split(":");
        try {
            JSONObject card = new JSONObject();
            if (parts[0].equals("apps")) {
                card.put("type", "str");
                card.put("data", str);
            } else if (parts[0].equals("tutorial")) {
                card.put("type", "str");
                card.put("data", str);
            } else if (parts[0].equals("widget2")) {
                card.put("type", "str");
                card.put("data", str);
            } else return null;
            return card;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
