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
import android.util.Log;

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
    @android.annotation.SuppressLint("ForegroundServiceType")
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
        
        try {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startForeground(NOTIFICATION_ID, notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error starting foreground: " + e.getMessage());
        }
        return START_STICKY;
    }

    public void playMusic(Uri uri, String title, String artist) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Đảm bảo không bao giờ null
            this.currentTitle = (title != null) ? title : "";
            this.currentArtist = (artist != null) ? artist : "";
            
            mediaPlayer = MediaPlayer.create(this, uri);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    updateNotification();
                });
                mediaPlayer.start();
                updateNotification();
            } else {
                Log.e("MusicPlayerService", "Failed to create MediaPlayer for URI: " + uri);
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error in playMusic: " + e.getMessage());
        }
    }

    public void pauseMusic() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updateNotification();
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error in pauseMusic: " + e.getMessage());
        }
    }

    public void resumeMusic() {
        try {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                updateNotification();
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error in resumeMusic: " + e.getMessage());
        }
    }

    public void stopMusic() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error in stopMusic: " + e.getMessage());
        }
        currentTitle = "";
        currentArtist = "";
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public int getCurrentPosition() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    public int getDuration() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    public void seekTo(int position) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(position);
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error in seekTo: " + e.getMessage());
        }
    }

    public String getCurrentTitle() {
        return currentTitle != null ? currentTitle : "";
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

        String title = (currentTitle == null || currentTitle.isEmpty()) ? "Trình phát nhạc" : currentTitle;
        String text = (currentArtist == null || currentArtist.isEmpty()) ? "TickTick Music" : currentArtist;

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
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, buildNotification());
            }
        } catch (Exception e) {
            Log.e("MusicPlayerService", "Error updating notification: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        stopMusic();
        super.onDestroy();
    }
}
