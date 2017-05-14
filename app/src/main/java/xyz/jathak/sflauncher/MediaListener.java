package xyz.jathak.sflauncher;

import android.annotation.TargetApi;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;

import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MediaListener extends NotificationListenerService{
    private MediaSessionManager mediaSessionManager;
    private MediaController mediaController;
    public static boolean online;
    public static String MEDIA_ACTION = "xyz.jathak.sflauncher.MEDIA_ACTION";
    public static String MEDIA_UPDATE = "xyz.jathak.sflauncher.MEDIA_UPDATE";
    private ComponentName componentName = new ComponentName("xyz.jathak.sflauncher","xyz.jathak.sflauncher.MediaListener");
    @Override
    public void onCreate(){
        registerReceiver(button,new IntentFilter(MEDIA_ACTION));
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName);
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(componentName);
            mediaController = pickController(controllers);
            if (mediaController != null) {
                mediaController.registerCallback(callback);
                meta = mediaController.getMetadata();
                updateMetadata();
            }
            online = true;
        } catch (SecurityException e) {

        }
    }

    @Override
    public int onStartCommand(Intent i, int startId, int i2){
        if(mediaController==null){
            try {
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(componentName);
                mediaController = pickController(controllers);
                if (mediaController != null) {
                    mediaController.registerCallback(callback);
                    meta = mediaController.getMetadata();
                    updateMetadata();
                }
            } catch (SecurityException e) {

            }
        }
        return START_STICKY;
    }

    MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            mediaController = null;
            meta = null;
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            updateMetadata();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            currentlyPlaying = state.getState() == PlaybackState.STATE_PLAYING;
            updateMetadata();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            meta = metadata;
            updateMetadata();
        }

        @Override
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            super.onQueueChanged(queue);
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            super.onQueueTitleChanged(title);
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            super.onExtrasChanged(extras);
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            super.onAudioInfoChanged(info);
        }
    };

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {

    }
    @Override
    public void onDestroy(){
        unregisterReceiver(button);
        mediaController = null;
        online = false;
        mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener);
    }

    public static boolean currentlyPlaying = false;
    public static Bitmap currentArt = null;
    public static String currentArtist, currentSong, currentAlbum;

    public void updateMetadata(){
        if(mediaController!=null&&mediaController.getPlaybackState()!=null){
            currentlyPlaying = mediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING;
        }
        if(meta==null)return;
        currentArt=meta.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if(currentArt==null){
            currentArt = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        }
        if(currentArt==null){
            currentArt = meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        }
        currentArtist=meta.getString(MediaMetadata.METADATA_KEY_ARTIST);
        currentSong=meta.getString(MediaMetadata.METADATA_KEY_TITLE);
        if(currentSong==null){
            currentSong=meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        }
        currentAlbum=meta.getString(MediaMetadata.METADATA_KEY_ALBUM);
        if(currentArtist==null){
            currentArtist = meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        }
        if(currentArtist==null) {
            currentArtist = meta.getString(MediaMetadata.METADATA_KEY_AUTHOR);
        }
        if(currentArtist==null) {
            currentArtist = meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
        }
        if(currentArtist==null) {
            currentArtist = meta.getString(MediaMetadata.METADATA_KEY_WRITER);
        }
        if(currentArtist==null) {
            currentArtist = meta.getString(MediaMetadata.METADATA_KEY_COMPOSER);
        }
        if(currentArtist==null) currentArtist = "";
        if(currentSong==null) currentSong = "";
        if(currentAlbum==null) currentAlbum = "";
        sendBroadcast(new Intent(MEDIA_UPDATE));
    }

    private MediaController pickController(List<MediaController> controllers){
        for(int i=0;i<controllers.size();i++){
            MediaController mc = controllers.get(i);
            if(mc!=null&&mc.getPlaybackState()!=null&&
                    mc.getPlaybackState().getState()==PlaybackState.STATE_PLAYING){
                return mc;
            }
        }
        if(controllers.size()>0) return controllers.get(0);
        return null;
    }

    MediaSessionManager.OnActiveSessionsChangedListener sessionListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            mediaController = pickController(controllers);
            if(mediaController==null)return;
            mediaController.registerCallback(callback);
            meta = mediaController.getMetadata();
            updateMetadata();
        }
    };

    public IBinder onBind(Intent intent) {
        return null;
    }
    private MediaMetadata meta;

    BroadcastReceiver button = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            //Play is 0, next is 1, previous is 2
            int action = intent.getIntExtra("type",-1);
            if(mediaController!=null&&action==0){
                mediaController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                mediaController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
            }else if(mediaController!=null&&action ==1){
                mediaController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_NEXT));
                mediaController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_NEXT));
            }else if (mediaController!=null&&action==2){
                mediaController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PREVIOUS));
                mediaController.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PREVIOUS));
            }else if (action==3){
                PackageManager m = context.getPackageManager();
                if(mediaController==null) {
                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
                    String pack = p.getString("appLaunch", "");
                    if (!pack.equals("")) {
                        startActivity(m.getLaunchIntentForPackage(pack));
                    }
                }else{
                    startActivity(m.getLaunchIntentForPackage(mediaController.getPackageName()));
                }
            }
        }
    };
}
