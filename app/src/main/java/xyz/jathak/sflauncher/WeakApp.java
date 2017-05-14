package xyz.jathak.sflauncher;

import java.util.ArrayList;
import java.util.List;

public class WeakApp{
    public final String name, packageName, className;

    public WeakApp(String name, String packageName, String className){
        this.name = name;
        this.packageName = packageName;
        this.className = className;
    }

    public String getIdentifier(){
        return packageName+"/"+className;
    }

    public static List<WeakApp> from(List<App> apps){
        List<WeakApp> weaks = new ArrayList<>();
        for(App a : apps){
            weaks.add(new WeakApp(a.name, a.packageName, a.className));
        }
        return weaks;
    }

    public static boolean packageExists(List<WeakApp> apps, String packageName){
        for(WeakApp a : apps){
            if(packageName.equals(a.packageName)) return true;
        }
        return false;
    }


}
