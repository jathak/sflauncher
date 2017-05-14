package xyz.jathak.sflauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class App implements Comparable<App>{
    public String className, packageName;
    public final String name;
    protected Drawable icon;

    private App(String name){
        this.name = name;
    }

    public App(ResolveInfo info, Context ctx){
        PackageManager pm = ctx.getPackageManager();
        this.name = info.loadLabel(pm).toString();
        this.packageName = info.activityInfo.packageName;
        this.className = info.activityInfo.name;
        this.icon = info.loadIcon(pm);
    }

    protected Drawable getRawIcon(){
        return icon.getConstantState().newDrawable();
    }

    public Drawable getIcon(Context ctx){
        return getIcon(ctx, true);
    }

    public Drawable getIcon(Context ctx, boolean retry){
        String pack = PreferenceManager.getDefaultSharedPreferences(ctx).getString("iconpack","Default");
        if(!lastPack.equals(pack)){
            filter=null;
            iconbacks=new ArrayList<>();
            iconmasks=new ArrayList<>();
            iconupons=new ArrayList<>();
            backgroundIdent=null;
            maskIdent=null;
            uponIdent=null;
            lastPack=pack;
            scale=1;
        }
        if(!pack.equals("Default")){
            PackageManager pm = ctx.getPackageManager();
            String[] packs = pack.split("/");
            String compName = "ComponentInfo{"+packageName+"/"+className+"}";
            try{
                Resources res = pm.getResourcesForApplication(packs[0]);
                if(filter==null) {
                    HashMap<String,String> newfilter = new HashMap<>();
                    List<String> newiconbacks = new ArrayList<>();
                    List<String> newiconmasks = new ArrayList<>();
                    List<String> newiconupons = new ArrayList<>();
                    backgroundIdent = null;
                    maskIdent = null;
                    uponIdent = null;
                    if (res == null) return getRawIcon();
                    int id = res.getIdentifier("appfilter", "xml", packs[0]);
                    if (id == 0) return null;
                    XmlResourceParser xml = res.getXml(id);
                    if (xml == null) return getRawIcon();
                    int event = xml.getEventType();
                    while (event != XmlPullParser.END_DOCUMENT) {
                        event = xml.getEventType();
                        if(event==XmlPullParser.START_TAG) {
                            String name = xml.getName().trim();
                            if (name.equals("item")) {
                                String c = xml.getAttributeValue(null, "component");
                                String d = xml.getAttributeValue(null, "drawable");
                                newfilter.put(c, d);
                            } else if (name.equals("iconback")) {
                                int x = 1;
                                String data = xml.getAttributeValue(null, "img" + x);
                                while (data != null) {
                                    newiconbacks.add(data);
                                    x++;
                                    data = xml.getAttributeValue(null, "img" + x);
                                }
                            } else if (name.equals("iconmask")) {
                                int x = 1;
                                String data = xml.getAttributeValue(null, "img" + x);
                                while (data != null) {
                                    newiconmasks.add(data);
                                    x++;
                                    data = xml.getAttributeValue(null, "img" + x);
                                }
                            } else if (name.equals("iconupon")) {
                                int x = 1;
                                String data = xml.getAttributeValue(null, "img" + x);
                                while (data != null) {
                                    newiconupons.add(data);
                                    x++;
                                    data = xml.getAttributeValue(null, "img" + x);
                                }
                            } else if (name.equals("scale")) {
                                scale = Float.parseFloat(xml.getAttributeValue(null, "factor"));
                            }
                        }
                        xml.next();
                    }
                    filter = newfilter;
                    iconbacks = newiconbacks;
                    iconmasks = newiconmasks;
                    iconupons = newiconupons;
                }
                if(filter.containsKey(compName)){
                    int id2 = res.getIdentifier(filter.get(compName), "drawable", packs[0]);
                    if(id2!=0) return res.getDrawable(id2);
                }
                int random = 0;
                if(iconbacks.size()>0){
                    random = new Random().nextInt(iconbacks.size());
                    if (backgroundIdent == null && iconbacks.size() > 0) backgroundIdent = iconbacks.get(random);
                }
                if(iconmasks.size()>0){
                    random = new Random().nextInt(iconmasks.size());
                    if(maskIdent==null&&iconmasks.size()>0)maskIdent = iconmasks.get(random);
                }
                if(iconupons.size()>0){
                    random = new Random().nextInt(iconupons.size());
                    if(uponIdent==null&&iconupons.size()>0)uponIdent = iconupons.get(random);
                }
                if (backgroundIdent == null && maskIdent == null && uponIdent == null) {
                    return getRawIcon();
                }
                int width;
                Bitmap b;
                Canvas canvas;
                if(backgroundIdent!=null) {
                    int ident = res.getIdentifier(backgroundIdent, "drawable", packs[0]);
                    Drawable background = res.getDrawable(ident);
                    width = Math.min(background.getIntrinsicHeight(), background.getIntrinsicWidth());
                    b = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
                    background.setBounds(0, 0, width, width);
                    canvas = new Canvas(b);
                    background.draw(canvas);
                }else{
                    Drawable mine = getRawIcon();
                    width = Math.min(mine.getIntrinsicHeight(), mine.getIntrinsicWidth());
                    b = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(b);
                }
                Drawable myIcon = getRawIcon();
                int left = (int)((width-width*scale)/2);
                int right = left + (int)(width*scale);
                myIcon.setBounds(left, left, right, right);
                myIcon.draw(canvas);
                if(iconupons.size()>0){
                    int uident = res.getIdentifier(uponIdent, "drawable", packs[0]);
                    Drawable upon = res.getDrawable(uident);
                    upon.setBounds(0, 0, width, width);
                    upon.draw(canvas);
                }
                if(iconmasks.size()>0){
                    int mident = res.getIdentifier(maskIdent, "drawable", packs[0]);
                    Drawable mask = res.getDrawable(mident);
                    Paint paint = new Paint();
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                    mask.setBounds(0, 0, width, width);
                    canvas.drawBitmap(drawableToBitmap(mask), 0, 0, paint);
                }
                return new BitmapDrawable(ctx.getResources(),b);
            }catch(Exception e){
                backgroundIdent = null;
                maskIdent = null;
                uponIdent = null;
                if(retry&&(iconbacks.size()>0||iconmasks.size()>0||iconupons.size()>0)){
                    return getIcon(ctx, false);
                }else {
                    e.printStackTrace();
                    Log.d("POKEMON", "Message: " + e.getMessage());
                }
            }
        }else lastPack = "Default";
        return getRawIcon();
    }

    public static boolean packageExists(List<App> apps, String packageName){
        for(App a : apps){
            if(packageName.equals(a.packageName)) return true;
        }
        return false;
    }

    private static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public String getIdentifier(){
        return packageName+"/"+className;
    }

    private String backgroundIdent = null;
    private String maskIdent = null;
    private String uponIdent = null;

    private static List<String> iconbacks, iconmasks, iconupons;
    private static float scale = 1;

    private static HashMap<String,String> filter;
    private static String lastPack = "Default";

    @Override
    public int compareTo(App another) {
        return name.toLowerCase().compareTo(another.name.toLowerCase());
    }

    public Intent getIntent(){
        Intent res = new Intent(Intent.ACTION_MAIN);
        res.addCategory(Intent.CATEGORY_LAUNCHER);
        res.setComponent(new ComponentName(packageName, className));
        res.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return res;
    }

    public static App fromIdentifier(Card.Apps c, String ident){
        if(ident.split("\\|")[0].equals("shortcut4"))return Shortcut.from(c, ident);
        if(ident.split("\\|")[0].equals("bitmapshortcut"))return BitmapShortcut.from(ident);
        if (c.main.apps.size() == 0) {
            String[] parts = ident.split("/");
            Intent res = new Intent(Intent.ACTION_MAIN);
            res.addCategory(Intent.CATEGORY_LAUNCHER);
            res.setComponent(new ComponentName(parts[0], parts[1]));
            res.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            PackageManager pm = c.main.getPackageManager();
            ResolveInfo info = pm.resolveActivity(res, 0);
            if (info == null) {
                return null;
            }
            return new App(info, c.main);
        }
        for(App a : c.main.apps){
            if(a.getIdentifier().equals(ident)) return a;
        }
        return null;
    }

    public static class Shortcut extends App{
        private int drawableIdent;
        private Intent intent;

        @Override
        public Intent getIntent(){
            return intent;
        }

        public Shortcut(Context ctx, Intent i, String name, Intent.ShortcutIconResource icon){
            super(name);
            this.packageName = icon.packageName;
            this.intent = i;
            try {
                Resources res = ctx.getPackageManager().getResourcesForApplication(icon.packageName);
                this.drawableIdent = res.getIdentifier(icon.resourceName, "drawable", icon.packageName);
                this.icon = res.getDrawable(drawableIdent);
            } catch (PackageManager.NameNotFoundException e) {
                this.icon = ctx.getResources().getDrawable(R.drawable.ic_launcher);

            }
        }

        private Shortcut(String name, Drawable icon, String packageName, Intent i, int drawableIdent){
            super(name);
            this.packageName = packageName;
            this.intent = i;
            this.icon = icon;
            this.drawableIdent = drawableIdent;
        }

        private static Shortcut from(Card.Apps c, String ident){
            String[] parts = ident.split("\\|");
            Context ctx = c.getWrapper().getContext();
            if(parts[0].equals("shortcut4") && parts.length >= 5){
                try {
                    Resources res = ctx.getPackageManager().getResourcesForApplication(parts[1]);
                    int drawableIdent = res.getIdentifier(parts[4], "drawable", parts[1]);
                    Drawable icon = res.getDrawable(drawableIdent);
                    String uri = decodeText(parts[2]);
                    Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                    return new Shortcut(parts[3], icon, parts[1], intent, drawableIdent);
                } catch (PackageManager.NameNotFoundException | URISyntaxException |
                            Resources.NotFoundException ignored) {
                }
            }
            return null;
        }

        @Override
        public String getIdentifier(){
            return "shortcut4|"+packageName+"|"+encodeText(intent.toUri(Intent.URI_INTENT_SCHEME))
                    +"|"+name+"|"+drawableIdent;
        }

    }

    public static String encodeText(String input){
        byte[] data = null;
        try {
            data = input.getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e1) {
        }
        return null;
    }

    public static String decodeText(String input){
        byte[] data1 = Base64.decode(input, Base64.DEFAULT);
        try {
            return new String(data1, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    public static class BitmapShortcut extends App{
        private Intent intent;

        @Override
        public Intent getIntent(){
            return intent;
        }

        public BitmapShortcut(String name, Bitmap bitmapIcon, Intent intent){
            super(name);
            this.intent = intent;
            if (intent.getAction().equals(Intent.ACTION_CALL)) {
                intent.setAction(Intent.ACTION_DIAL);
            }
            this.icon = new BitmapDrawable(bitmapIcon);
            this.bitmapString = encodeBitmap(bitmapIcon);
        }

        public BitmapShortcut(String name, Bitmap bitmapIcon, Intent intent, String bitmapString) {
            super(name);
            this.intent = intent;
            this.icon = new BitmapDrawable(bitmapIcon);
            this.bitmapString = bitmapString;
        }

        private String bitmapString;

        public String getIdentifier(){
            return "bitmapshortcut|"+encodeText(intent.toUri(Intent.URI_INTENT_SCHEME))
                    +"|"+name+"|"+bitmapString;
        }

        public static BitmapShortcut from(String ident){
            String[] parts = ident.split("\\|");
            if (parts.length < 4) return null;
            try {
                String uri = decodeText(parts[1]);
                Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                return new BitmapShortcut(parts[2],decodeBitmap(parts[3]), intent, parts[3]);
            } catch (URISyntaxException ignored) {
            }
            return null;
        }

        private static String encodeBitmap(Bitmap bitmap){
            if (bitmap == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] b = baos.toByteArray();
            ByteArrayOutputStream baosW = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, baosW);
            byte[] bweb = baos.toByteArray();
            if (bweb.length < b.length) {
                b = bweb;
            }
            if (b.length > 32768) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 50, baos);
                b = baos.toByteArray();
            }
            return Base64.encodeToString(b, Base64.DEFAULT);
        }

        private static Bitmap decodeBitmap(String string){
            if (string == null || string.equals("null")) return null;
            try {
                byte[] b = Base64.decode(string, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(b, 0, b.length);
            } catch (IllegalArgumentException e){
                return null;
            }
        }
    }

    //Used to represent a blank space in app cards
    public static class Hole extends App{
        public Hole(){
            super("");
            this.packageName = "xyz.jathak.sflauncher";
            this.className = "";
            this.icon = null;
        }

        @Override
        public Drawable getIcon(Context ctx){
            return ctx.getResources().getDrawable(R.drawable.glow);
        }
    }
}
