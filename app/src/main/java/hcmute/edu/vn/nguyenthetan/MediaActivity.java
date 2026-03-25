package hcmute.edu.vn.nguyenthetan;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.adapter.MediaAdapter;
import hcmute.edu.vn.nguyenthetan.service.MusicPlayerService;

/**
 * MediaActivity: Màn hình trình phát nhạc (Music only).
 * Chức năng chỉnh sửa ảnh đại diện đã chuyển sang ProfileActivity.
 */
public class MediaActivity extends AppCompatActivity implements MediaAdapter.OnMusicClickListener {

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
    private Runnable updateSeekBarRunnable;
    private List<MediaAdapter.MusicItem> currentMusicList = new ArrayList<>(); // Lưu lại danh sách nhạc

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // === ĐOẠN CODE THÊM MỚI ĐỂ ĐỒNG BỘ UI KHI VÀO LẠI APP ===
            if (musicService.isPlaying() || !musicService.getCurrentTitle().isEmpty()) {
                // Tìm bài hát đang phát trong danh sách hiện tại để lấy thông tin
                String playingTitle = musicService.getCurrentTitle();
                for (int i = 0; i < currentMusicList.size(); i++) {
                    MediaAdapter.MusicItem item = currentMusicList.get(i);
                    if (item.title.equals(playingTitle)) {
                        currentPlayingPosition = i;
                        mediaAdapter.setCurrentPlaying(i);
                        showNowPlaying(item);
                        break;
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_media);

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        // Khởi tạo các View cho Now Playing
        layoutNowPlaying = findViewById(R.id.layoutNowPlaying);
        tvNowPlayingTitle = findViewById(R.id.tvNowPlayingTitle);
        tvNowPlayingArtist = findViewById(R.id.tvNowPlayingArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnNowPlayingPlay = findViewById(R.id.btnNowPlayingPlay);
        seekBarMusic = findViewById(R.id.seekBarMusic);

        // Back button
        findViewById(R.id.btnBackMedia).setOnClickListener(v -> finish());

        // Upload Music
        findViewById(R.id.fabUploadMusic).setOnClickListener(v -> audioPickerLauncher.launch("audio/*"));

        // Xử lý sự kiện kéo SeekBar
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Tạm dừng cập nhật UI khi người dùng đang kéo
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && musicService != null) {
                    musicService.seekTo(seekBar.getProgress());
                }
                // Tiếp tục cập nhật UI
                handler.postDelayed(updateSeekBarRunnable, 1000);
            }
        });

        // Nút Play/Pause trên thanh Now Playing
        btnNowPlayingPlay.setOnClickListener(v -> {
            if (!isBound || currentPlayingPosition == -1) return;
            MediaAdapter.MusicItem item = currentMusicList.get(currentPlayingPosition);
            onPlayPauseClick(item, currentPlayingPosition); // Tái sử dụng hàm hiện tại
        });
    }

    private void setupAudioPicker() {
        audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveUploadedAudio(uri);
                    }
                });
    }

    private void saveUploadedAudio(Uri uri) {
        File uploadDir = new File(getFilesDir(), "uploaded_music");
        if (!uploadDir.exists())
            uploadDir.mkdirs();

        String timeStamp = String.valueOf(System.currentTimeMillis());
        File newFile = new File(uploadDir, "audio_" + timeStamp + ".mp3");

        try (InputStream in = getContentResolver().openInputStream(uri);
                OutputStream out = new FileOutputStream(newFile)) {

            if (in == null)
                return;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            Toast.makeText(this, "Đã tải nhạc lên thành công!", Toast.LENGTH_SHORT).show();
            loadMusic();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tải nhạc lên", Toast.LENGTH_SHORT).show();
        }
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

    private void checkAndLoadMusic() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMusic();
        } else {
            ActivityCompat.requestPermissions(this, new String[] { permission }, REQUEST_MEDIA_PERMISSION);
        }
    }

    private void loadMusic() {
        List<MediaAdapter.MusicItem> musicItems = new ArrayList<>();

        // 1. Load from uploaded music (internal storage)
        File uploadDir = new File(getFilesDir(), "uploaded_music");
        if (uploadDir.exists() && uploadDir.isDirectory()) {
            File[] files = uploadDir.listFiles();
            if (files != null) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                for (File f : files) {
                    try {
                        retriever.setDataSource(f.getAbsolutePath());
                        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                        long duration = 0;
                        if (durationStr != null) {
                            try {
                                duration = Long.parseLong(durationStr);
                            } catch (Exception ignored) {
                            }
                        }

                        if (title == null || title.isEmpty()) {
                            title = f.getName();
                        }

                        Uri fileUri = Uri.fromFile(f);
                        musicItems.add(new MediaAdapter.MusicItem(title, artist, duration, fileUri));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Load from System MediaStore
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(
                collection, projection, selection, null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                musicItems.add(new MediaAdapter.MusicItem(title, artist, duration, contentUri));
            }
            cursor.close();
        }

        if (musicItems.isEmpty()) {
            rvMusic.setVisibility(View.GONE);
            tvEmptyMusic.setVisibility(View.VISIBLE);
        } else {
            rvMusic.setVisibility(View.VISIBLE);
            tvEmptyMusic.setVisibility(View.GONE);
            mediaAdapter.setData(musicItems);
        }

        currentMusicList = musicItems;
    }

    @Override
    public void onPlayPauseClick(MediaAdapter.MusicItem item, int position) {
        if (!isBound) return;

        if (currentPlayingPosition == position && musicService.isPlaying()) {
            musicService.pauseMusic();
            mediaAdapter.setCurrentPlaying(-1);
            updatePlayPauseButton(); // Cập nhật nút trên Now Playing
        } else if (currentPlayingPosition == position && !musicService.isPlaying()) {
            musicService.resumeMusic();
            mediaAdapter.setCurrentPlaying(position);
            updatePlayPauseButton(); // Cập nhật nút trên Now Playing
        } else {
            musicService.playMusic(item.uri, item.title, item.artist);
            currentPlayingPosition = position;
            mediaAdapter.setCurrentPlaying(position);
            showNowPlaying(item); // Hiển thị Now Playing cho bài mới
        }
    }

    @Override
    public void onDeleteClick(MediaAdapter.MusicItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xoá nhạc")
                .setMessage("Bạn có chắc chắn muốn xoá bài hát này khỏi máy?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    if (item.uri != null && item.uri.getPath() != null) {
                        File file = new File(item.uri.getPath());
                        if (file.exists() && file.delete()) {
                            Toast.makeText(this, "Đã xoá", Toast.LENGTH_SHORT).show();

                            // NẾU BÀI ĐANG PHÁT BỊ XÓA
                            if (currentPlayingPosition == position) {
                                if (isBound && musicService != null) {
                                    musicService.stopMusic(); // Dừng hẳn để tắt Notification
                                }
                                currentPlayingPosition = -1;
                                mediaAdapter.setCurrentPlaying(-1);

                                // 1. Ẩn thanh Now Playing
                                layoutNowPlaying.setVisibility(View.GONE);

                                // 2. Dừng vòng lặp cập nhật thanh thời gian (tiết kiệm pin)
                                if (handler != null && updateSeekBarRunnable != null) {
                                    handler.removeCallbacks(updateSeekBarRunnable);
                                }
                            }

                            // Tải lại danh sách nhạc sau khi xóa
                            loadMusic();
                        } else {
                            Toast.makeText(this, "Xoá thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusic();
            } else {
                tvEmptyMusic.setVisibility(View.VISIBLE);
                tvEmptyMusic.setText("Cần cấp quyền truy cập media");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (handler != null && updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }

    private void showNowPlaying(MediaAdapter.MusicItem item) {
        layoutNowPlaying.setVisibility(View.VISIBLE);
        tvNowPlayingTitle.setText(item.title);
        tvNowPlayingArtist.setText(item.artist != null ? item.artist : "Không rõ");
        tvTotalTime.setText(formatDuration(item.duration));
        seekBarMusic.setMax((int) item.duration);
        updatePlayPauseButton();

        startUpdatingSeekBar();
    }

    private void updatePlayPauseButton() {
        if (isBound && musicService != null && musicService.isPlaying()) {
            btnNowPlayingPlay.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnNowPlayingPlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void startUpdatingSeekBar() {
        if (updateSeekBarRunnable == null) {
            updateSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isBound && musicService != null) {
                        int currentPos = musicService.getCurrentPosition();
                        seekBarMusic.setProgress(currentPos);
                        tvCurrentTime.setText(formatDuration(currentPos));
                    }
                    handler.postDelayed(this, 1000); // Lặp lại sau mỗi giây
                }
            };
        }
        handler.postDelayed(updateSeekBarRunnable, 0);
    }

    // Hàm format thời gian (copy giống hệt bên MediaAdapter)
    private String formatDuration(long ms) {
        long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(ms) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}
