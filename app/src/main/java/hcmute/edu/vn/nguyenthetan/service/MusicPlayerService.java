package hcmute.edu.vn.nguyenthetan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.nguyenthetan.MediaActivity;
import hcmute.edu.vn.nguyenthetan.R;

public class MusicPlayerService extends Service {

    public static final String CHANNEL_ID_MUSIC = "music_player_channel";
    private static final int NOTIFICATION_ID = 2001;

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private String currentTitle = "";
    private String currentArtist = "";

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("STOP".equals(action)) {
                stopMusic();
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // Start foreground with a basic notification
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    public void playMusic(Uri uri, String title, String artist) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            currentTitle = title;
            currentArtist = artist;
            mediaPlayer = MediaPlayer.create(this, uri);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    // Update notification when song ends
                    updateNotification();
                });
                mediaPlayer.start();
                updateNotification();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateNotification();
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentTitle = "";
        currentArtist = "";
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_MUSIC,
                    "Trình phát nhạc",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Thông báo điều khiển phát nhạc");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MediaActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingOpen = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MusicPlayerService.class);
        stopIntent.setAction("STOP");
        PendingIntent pendingStop = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = currentTitle.isEmpty() ? "Trình phát nhạc" : currentTitle;
        String text = currentArtist.isEmpty() ? "TickTick Music" : currentArtist;

        return new NotificationCompat.Builder(this, CHANNEL_ID_MUSIC)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("🎵 " + title)
                .setContentText(text)
                .setContentIntent(pendingOpen)
                .addAction(android.R.drawable.ic_delete, "Dừng", pendingStop)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    @Override
    public void onDestroy() {
        stopMusic();
        super.onDestroy();
    }
}
