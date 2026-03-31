package hcmute.edu.vn.nguyenthetan;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import hcmute.edu.vn.nguyenthetan.util.DialogUtils;
import hcmute.edu.vn.nguyenthetan.util.EdgeInsetsUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.adapter.MediaAdapter;
import hcmute.edu.vn.nguyenthetan.media.MediaMusicLoader;
import hcmute.edu.vn.nguyenthetan.media.NowPlayingController;
import hcmute.edu.vn.nguyenthetan.service.MusicPlayerService;
import hcmute.edu.vn.nguyenthetan.util.DurationFormatter;

public class MediaActivity extends BaseActivity implements MediaAdapter.OnMusicClickListener {

    private static final int REQUEST_MEDIA_PERMISSION = 3001;

    private RecyclerView rvMusic;
    private TextView tvEmptyMusic;
    private MediaAdapter mediaAdapter;

    private MusicPlayerService musicService;
    private boolean isBound = false;
    private int currentPlayingPosition = -1;

    private ActivityResultLauncher<String> audioPickerLauncher;

    private View layoutNowPlaying;
    private TextView tvNowPlayingTitle, tvNowPlayingArtist, tvCurrentTime, tvTotalTime;
    private ImageView btnNowPlayingPlay;
    private SeekBar seekBarMusic;

    private Handler handler = new Handler(Looper.getMainLooper());
    private List<MediaAdapter.MusicItem> currentMusicList = new ArrayList<>();
    private NowPlayingController nowPlayingController;
    private final MediaMusicLoader musicLoader = new MediaMusicLoader();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            syncUIWithService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_media);

        EdgeInsetsUtil.applySystemBarsPadding(findViewById(android.R.id.content));

        initViews();
        setupAudioPicker();
        bindMusicService();
        checkAndLoadMusic();
    }

    private void initViews() {
        rvMusic = findViewById(R.id.rvMusic);
        tvEmptyMusic = findViewById(R.id.tvEmptyMusic);

        mediaAdapter = new MediaAdapter(this);
        rvMusic.setLayoutManager(new LinearLayoutManager(this));
        rvMusic.setAdapter(mediaAdapter);

        layoutNowPlaying = findViewById(R.id.layoutNowPlaying);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvNowPlayingArtist = findViewById(R.id.tvNowPlayingArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnNowPlayingPlay = findViewById(R.id.btnNowPlayingPlay);
        seekBarMusic = findViewById(R.id.seekBarMusic);

        nowPlayingController = new NowPlayingController(
                layoutNowPlaying, tvNowPlayingTitle, tvNowPlayingArtist,
                tvCurrentTime, tvTotalTime, btnNowPlayingPlay, seekBarMusic, handler
        );

        findViewById(R.id.btnBackMedia).setOnClickListener(v -> finish());
        findViewById(R.id.fabUploadMusic).setOnClickListener(v -> audioPickerLauncher.launch("audio/*"));

        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(DurationFormatter.formatMs(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                nowPlayingController.stopLoop();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && musicService != null) {
                    musicService.seekTo(seekBar.getProgress());
                    nowPlayingController.startLoop(musicService);
                }
            }
        });

        btnNowPlayingPlay.setOnClickListener(v -> {
            if (!isBound || currentPlayingPosition == -1 || currentPlayingPosition >= currentMusicList.size()) return;
            onPlayPauseClick(currentMusicList.get(currentPlayingPosition), currentPlayingPosition);
        });
    }

    private void setupAudioPicker() {
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) saveUploadedAudio(uri); });
    }

    private void saveUploadedAudio(Uri uri) {
        File uploadDir = new File(getFilesDir(), "uploaded_music");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String fileName = getFileNameFromUri(uri);
        if (fileName == null || fileName.isEmpty()) {
            fileName = "audio_" + System.currentTimeMillis() + ".mp3";
        }

        File newFile = new File(uploadDir, fileName);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(newFile)) {
            if (in == null) return;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);

            Toast.makeText(this, "Đã tải nhạc lên thành công!", Toast.LENGTH_SHORT).show();
            loadMusic();
        } catch (Exception e) {
            Log.e("MediaActivity", "Error saving audio", e);
            Toast.makeText(this, "Lỗi khi tải nhạc lên", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) fileName = cursor.getString(nameIndex);
            }
        } catch (Exception e) {
            Log.e("MediaActivity", "Error getting file name", e);
        }
        return fileName;
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void syncUIWithService() {
        if (musicService != null && (!musicService.getCurrentTitle().isEmpty())) {
            String playingTitle = musicService.getCurrentTitle();
            for (int i = 0; i < currentMusicList.size(); i++) {
                MediaAdapter.MusicItem item = currentMusicList.get(i);
                if (item.title != null && item.title.equals(playingTitle)) {
                    currentPlayingPosition = i;
                    mediaAdapter.setCurrentPlaying(i, musicService.isPlaying());
                    nowPlayingController.show(item, musicService.isPlaying());
                    nowPlayingController.startLoop(musicService);
                    break;
                }
            }
        }
    }

    private void checkAndLoadMusic() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusic();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_MEDIA_PERMISSION);
        }
    }

    private void loadMusic() {
        List<MediaAdapter.MusicItem> musicItems = musicLoader.loadAll(this);
        currentMusicList = musicItems;
        if (musicItems.isEmpty()) {
            rvMusic.setVisibility(View.GONE);
            tvEmptyMusic.setVisibility(View.VISIBLE);
        } else {
            rvMusic.setVisibility(View.VISIBLE);
            tvEmptyMusic.setVisibility(View.GONE);
            mediaAdapter.setData(musicItems);
            syncUIWithService(); 
        }
    }

    @Override
    public void onPlayPauseClick(MediaAdapter.MusicItem item, int position) {
        if (!isBound || musicService == null) return;

        try {
            boolean isSameSong = (currentPlayingPosition == position);
            if (isSameSong && musicService.isPlaying()) {
                musicService.pauseMusic();
                mediaAdapter.setCurrentPlaying(position, false);
                nowPlayingController.setPlaying(false);
            } else if (isSameSong && !musicService.isPlaying()) {
                if (musicService.getCurrentTitle().isEmpty()) {
                    musicService.playMusic(item.uri, item.title, item.artist);
                } else {
                    musicService.resumeMusic();
                }
                mediaAdapter.setCurrentPlaying(position, true);
                nowPlayingController.setPlaying(true);
            } else {
                musicService.playMusic(item.uri, item.title, item.artist);
                currentPlayingPosition = position;
                mediaAdapter.setCurrentPlaying(position, true);
                nowPlayingController.show(item, true);
                nowPlayingController.startLoop(musicService);
            }
        } catch (Exception e) {
            Log.e("MediaActivity", "Error in onPlayPauseClick", e);
            Toast.makeText(this, "Lỗi khi phát nhạc", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(MediaAdapter.MusicItem item, int position) {
        DialogUtils.showConfirmDialog(this, "Xoá nhạc", "Xoá bài hát này?", "Xoá", () -> {
            if (item.uri != null && item.uri.getPath() != null) {
                File file = new File(item.uri.getPath());
                if (file.exists() && file.delete()) {
                    if (currentPlayingPosition == position) {
                        if (musicService != null) musicService.stopMusic();
                        currentPlayingPosition = -1;
                        mediaAdapter.setCurrentPlaying(-1, false);
                        nowPlayingController.hideAndStopLoop();
                    }
                    loadMusic();
                }
            }
        }, "Hủy");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusic();
        }
    }

    @Override
    protected void onDestroy() {
        if (nowPlayingController != null) nowPlayingController.stopLoop();
        if (isBound) unbindService(serviceConnection);
        super.onDestroy();
    }
}
