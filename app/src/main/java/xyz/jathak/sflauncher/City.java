package xyz.jathak.sflauncher;

import android.content.Context;
import com.google.android.apps.muzei.api.MuzeiContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class City {
    public final int dawn, day, dusk, night;
    public final String name;
    public final String packageName;
    public City(String name, int dawn, int day, int dusk, int night, String packageName){
        this.name = name;
        this.dawn = dawn;
        this.day = day;
        this.dusk = dusk;
        this.night = night;
        this.packageName = packageName;
    }
    public City(String name, int dawn, int day, int dusk, int night){
        this(name, dawn, day, dusk, night, null);
    }

    public static final City austin = new City("Austin", R.drawable.dawn_a, R.drawable.day_a, R.drawable.dusk_a, R.drawable.night_a);
    public static final City beach = new City("Beach", R.drawable.dawn_p, R.drawable.day_p, R.drawable.dusk_p, R.drawable.night_p);
    public static final City berlin = new City("Berlin", R.drawable.dawn_b, R.drawable.day_b, R.drawable.dusk_b, R.drawable.night_b);
    public static final City chicago = new City("Chicago", R.drawable.dawn_c, R.drawable.day_c, R.drawable.dusk_c, R.drawable.night_c);
    public static final City mountains = new City("Mountains", R.drawable.dawn_g, R.drawable.day_g, R.drawable.dusk_g, R.drawable.night_g);
    public static final City plains = new City("Great Plains", R.drawable.dawn_gp, R.drawable.day_gp, R.drawable.dusk_gp, R.drawable.night_gp);
    public static final City london = new City("London", R.drawable.dawn_l, R.drawable.day_l, R.drawable.dusk_l, R.drawable.night_l);
    public static final City newyork = new City("New York", R.drawable.dawn_ny, R.drawable.day_ny, R.drawable.dusk_ny, R.drawable.night_ny);
    public static final City paris = new City("Paris", R.drawable.dawn_f, R.drawable.day_f, R.drawable.dusk_f, R.drawable.night_f);
    public static final City sanfrancisco = new City("San Francisco", R.drawable.dawn_sf, R.drawable.day_sf, R.drawable.dusk_sf, R.drawable.night_sf);
    public static final City seattle = new City("Seattle", R.drawable.dawn_s, R.drawable.day_s, R.drawable.dusk_s, R.drawable.night_s);
    public static final City tahoe = new City("Tahoe", R.drawable.dawn_t, R.drawable.day_t, R.drawable.dusk_t, R.drawable.night_t);

    public static List<City> cities;
    static {
        cities = new ArrayList<>();
        cities.addAll(Arrays.asList(austin, beach, berlin, chicago, plains, london,
                mountains, newyork, paris, sanfrancisco, seattle, tahoe));
    }

    private static List<City> baseCities;
    static {
        baseCities = new ArrayList<>();
        baseCities.addAll(Arrays.asList(austin, beach, berlin, chicago, plains, london,
                mountains, newyork, paris, sanfrancisco, seattle, tahoe));
    }

    public static void addThemes(List<Theme> themes, Context ctx){
        List<City> newCities = new ArrayList<>();
        if (MuzeiContract.Artwork.getCurrentArtwork(ctx) != null) {
            newCities.add(new City.Muzei());
        }
        if (MainActivity.isPremium(ctx)) {
            newCities.add(new City.Music());
        }
        newCities.addAll(baseCities);
        for(Theme t : themes){
            newCities.addAll(t.headers);
        }
        cities = newCities;
    }

    public static City from(String name){
        for(City c : cities){
            if(c.name.equals(name)) return c;
        }
        return sanfrancisco;
    }

    public static class Muzei extends City {
        public Muzei(){
            super("Muzei:muzei-theme", 0, 0, 0, 0, "muzei-theme");
        }
    }

    public static class Music extends City {
        public Music() {
            super("Music:music-theme", 0, 0, 0, 0, "music-theme");
        }
    }
}
