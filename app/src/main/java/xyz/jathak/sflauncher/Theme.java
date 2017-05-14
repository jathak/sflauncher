package xyz.jathak.sflauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Theme {

    public List<City> headers = new ArrayList<>();

    public List<Background> backgrounds = new ArrayList<>();

    public List<Swatch> swatches = new ArrayList<>();

    public String packageName;

    public static List<Theme> loadThemes(Context ctx){
        Intent filter = new Intent(Intent.ACTION_MAIN, null);
        filter.addCategory("xyz.jathak.sflauncher.THEME");
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> ress=  pm.queryIntentActivities(filter, PackageManager.PERMISSION_GRANTED);
        List<Theme> themes = new ArrayList<>();
        if(!MainActivity.isPremium(ctx)) return themes;
        for(ResolveInfo r:ress){
            try {
                String packageName = r.activityInfo.packageName;
                Resources res = pm.getResourcesForApplication(packageName);
                Theme t = new Theme();
                if(res==null)continue;
                int id = res.getIdentifier("appfilter", "xml", packageName);
                if(id==0)continue;
                XmlResourceParser xml = res.getXml(id);
                if(xml==null)continue;
                int event = xml.getEventType();
                Drawable draw = null;
                while (event != XmlPullParser.END_DOCUMENT){
                    event = xml.getEventType();
                    if(event == XmlPullParser.START_TAG && xml.getName().contentEquals("header")){
                        String name = xml.getAttributeValue(null, "name");
                        name = name.replaceAll(":","-");
                        name += ":"+packageName;
                        String dawnStr = xml.getAttributeValue(null, "dawn");
                        int dawni = res.getIdentifier(dawnStr, "drawable", packageName);
                        String dayStr = xml.getAttributeValue(null, "day");
                        int dayi = res.getIdentifier(dayStr, "drawable", packageName);
                        String duskStr = xml.getAttributeValue(null, "dusk");
                        int duski = res.getIdentifier(duskStr, "drawable", packageName);
                        String nightStr = xml.getAttributeValue(null, "night");
                        int nighti = res.getIdentifier(nightStr, "drawable", packageName);
                        t.headers.add(new City(name, dawni, dayi, duski, nighti, packageName));
                    }else if(event == XmlPullParser.START_TAG && xml.getName().contentEquals("swatch")){
                        String name = xml.getAttributeValue(null, "name");
                        String color = xml.getAttributeValue(null, "color");
                        t.swatches.add(new Swatch(name, Color.parseColor(color)));
                    }else if(event == XmlPullParser.START_TAG && xml.getName().contentEquals("background")){
                        String name = xml.getAttributeValue(null, "name");
                        String bgColor = xml.getAttributeValue(null, "background_color");
                        String cardColor = xml.getAttributeValue(null, "card_color");
                        t.backgrounds.add(new Background(name, bgColor, cardColor, t));
                    }
                    xml.next();
                }
                t.packageName = packageName;
                themes.add(t);
            } catch (PackageManager.NameNotFoundException e) {
            } catch (XmlPullParserException e) {
            } catch (IOException e) {
            }
        }
        Theme customtheme = new Theme();
        String swatchStr = PreferenceManager.getDefaultSharedPreferences(ctx).getString("customColors", "");
        if(swatchStr.length()>0) {
            String[] sStrs = swatchStr.split(";");
            for (String s : sStrs) {
                customtheme.swatches.add(new Theme.Swatch(s, Color.parseColor(s)));
            }
        }
        themes.add(customtheme);
        City.addThemes(themes, ctx);
        Card.addThemes(themes);
        return themes;
    }

    public static class Background{
        public String bgColor;
        public String cardColor;
        public String name;
        public Theme parent;
        public Background(String name, String bgColor, String cardColor, Theme parent){
            this.name = name;
            this.bgColor = bgColor;
            this.cardColor = cardColor;
            this.parent = parent;
        }

        public int getBgColor(){
            for(Swatch s : parent.swatches){
                if(s.name.equals(bgColor)) return s.color;
            }
            for(String name : Card.COLOR_KEYS){
                if(name.equals(bgColor)) return Card.COLORS.get(name);
            }
            return Color.WHITE;
        }

        public int getCardColor(){
            for(Swatch s : parent.swatches){
                if(s.name.equals(cardColor)) return s.color;
            }
            for(String name : Card.COLOR_KEYS){
                if(name.equals(cardColor)) return Card.COLORS.get(name);
            }
            return Color.WHITE;
        }
    }

    public static class Swatch{
        public String name;
        public int color;
        public Swatch(String name, int color){
            this.name = name;
            this.color = color;
        }
    }
}
