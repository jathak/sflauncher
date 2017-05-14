package xyz.jathak.sflauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;


public class CustomColorActivity extends Activity {

    private ListView lv;
    private Theme theme;
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_color);
        theme = new Theme();
        String swatchStr = PreferenceManager.getDefaultSharedPreferences(this).getString("customColors", "");
        if(swatchStr.length()>0) {
            String[] sStrs = swatchStr.split(";");
            for (String s : sStrs) {
                theme.swatches.add(new Theme.Swatch(s, Color.parseColor(s)));
            }
        }
        lv = (ListView)findViewById(R.id.list);
        adapter = new BaseAdapter(){
            @Override
            public int getCount() {
                return theme.swatches.size();
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
                    convertView.setTag(vh);
                }else vh = (ViewHolder) convertView.getTag();
                vh.label.setText(theme.swatches.get(position).name.split(":")[0]);
                vh.color.setBackgroundColor(theme.swatches.get(position).color);
                vh.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        theme.swatches.remove(position);
                        adapter.notifyDataSetChanged();
                        saveSwatches();
                    }
                });
                return convertView;
            }
        };
        lv.setAdapter(adapter);
    }

    private void saveSwatches(){
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
        String colors = "";
        for(Theme.Swatch s : theme.swatches){
            colors += ";"+s.name;
        }
        if(colors.length()>0)colors=colors.substring(1);
        e.putString("customColors",colors);
        e.commit();
        Theme.loadThemes(this);
    }

    static class ViewHolder{
        TextView label;
        View delete;
        View color;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_custom_color, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add) {
            final AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("Custom Color");
            final EditText ev = new EditText(this);
            d.setView(ev);
            ev.setText("#");
            d.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String hexColor = ev.getText().toString();
                    try {
                        Color.parseColor(hexColor);
                        theme.swatches.add(new Theme.Swatch(hexColor, Color.parseColor(hexColor)));
                        adapter.notifyDataSetChanged();
                        saveSwatches();
                    } catch (IllegalArgumentException iae) {
                        Toast.makeText(CustomColorActivity.this,"Invalid color",Toast.LENGTH_SHORT).show();
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
}
