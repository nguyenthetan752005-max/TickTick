package hcmute.edu.vn.nguyenthetan;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;
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

public class MediaActivity extends AppCompatActivity implements MediaAdapter.OnMusicClickListener {

    private static final int REQUEST_MEDIA_PERMISSION = 3001;
    private static final String PREFS_NAME = "ticktick_prefs";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    private RecyclerView rvMusic;
    private TextView tvEmptyMusic;
    private View musicContainer, avatarContainer;
    private TextView tabMusic, tabAvatar;
    private ImageView ivCurrentAvatar;
    private MediaAdapter mediaAdapter;

    private MusicPlayerService musicService;
    private boolean isBound = false;
    private int currentPlayingPosition = -1;

    // Pickers
    private ActivityResultLauncher<String> avatarPickerLauncher;
    private ActivityResultLauncher<String> audioPickerLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
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
        setupAvatarPicker();
        setupCropLauncher();
        setupAudioPicker();
        bindMusicService();
        checkAndLoadMusic();
        loadSavedAvatar();
    }

    private void initViews() {
        rvMusic = findViewById(R.id.rvMusic);
        tvEmptyMusic = findViewById(R.id.tvEmptyMusic);
        musicContainer = findViewById(R.id.musicContainer);
        avatarContainer = findViewById(R.id.avatarContainer);
        tabMusic = findViewById(R.id.tabMusic);
        tabAvatar = findViewById(R.id.tabAvatar);
        ivCurrentAvatar = findViewById(R.id.ivCurrentAvatar);

        mediaAdapter = new MediaAdapter(this);
        rvMusic.setLayoutManager(new LinearLayoutManager(this));
        rvMusic.setAdapter(mediaAdapter);

        // Back button
        findViewById(R.id.btnBackMedia).setOnClickListener(v -> finish());

        // Tab switching
        tabMusic.setOnClickListener(v -> switchTab(true));
        tabAvatar.setOnClickListener(v -> switchTab(false));

        // Upload Music
        findViewById(R.id.fabUploadMusic).setOnClickListener(v -> audioPickerLauncher.launch("audio/*"));

        // Avatar buttons
        findViewById(R.id.btnChooseAvatar).setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));
        findViewById(R.id.btnRemoveAvatar).setOnClickListener(v -> {
            ivCurrentAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove(KEY_AVATAR_URI).apply();
            Toast.makeText(this, "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show();
        });
    }

    private void switchTab(boolean isMusic) {
        if (isMusic) {
            musicContainer.setVisibility(View.VISIBLE);
            avatarContainer.setVisibility(View.GONE);
            tabMusic.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_primary));
            tabMusic.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabAvatar.setBackgroundColor(ContextCompat.getColor(this, R.color.card_dark));
            tabAvatar.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            musicContainer.setVisibility(View.GONE);
            avatarContainer.setVisibility(View.VISIBLE);
            tabAvatar.setBackgroundColor(ContextCompat.getColor(this, R.color.blue_primary));
            tabAvatar.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabMusic.setBackgroundColor(ContextCompat.getColor(this, R.color.card_dark));
            tabMusic.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void setupAvatarPicker() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startCrop(uri);
                    }
                });
    }

    private void startCrop(Uri sourceUri) {
        try {
            // Copy source sang một file tạm để tránh lỗi SecurityException khi UCrop truy
            // cập
            File tempSource = new File(getCacheDir(), "temp_crop_source.png");
            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                    OutputStream out = new FileOutputStream(tempSource)) {
                if (in != null) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }

            File avatarFile = new File(getFilesDir(), "avatar.png");
            Uri destUri = Uri.fromFile(avatarFile);

            UCrop.Options options = new UCrop.Options();
            options.setCircleDimmedLayer(true);
            options.setShowCropGrid(false);
            options.setToolbarTitle("Cắt Ảnh Đại Diện");

            Intent intent = UCrop.of(Uri.fromFile(tempSource), destUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512)
                    .withOptions(options)
                    .getIntent(this);

            cropImageLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc ảnh gốc", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCropLauncher() {
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            ivCurrentAvatar.setImageURI(null); // xoá cache ảnh cũ
                            ivCurrentAvatar.setImageURI(resultUri);

                            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                            prefs.edit().putString(KEY_AVATAR_URI, resultUri.toString()).apply();

                            Toast.makeText(this, R.string.avatar_updated, Toast.LENGTH_SHORT).show();
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        Throwable cropError = UCrop.getError(result.getData());
                        if (cropError != null)
                            cropError.printStackTrace();
                        Toast.makeText(this, "Lỗi cắt ảnh", Toast.LENGTH_SHORT).show();
                    }
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
            // Reload list to include the newly copied file
            loadMusic();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tải nhạc lên", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedAvatar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uriStr = prefs.getString(KEY_AVATAR_URI, null);
        if (uriStr != null) {
            try {
                ivCurrentAvatar.setImageURI(Uri.parse(uriStr));
            } catch (Exception e) {
                ivCurrentAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }
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
    }

    @Override
    public void onPlayPauseClick(MediaAdapter.MusicItem item, int position) {
        if (!isBound)
            return;

        if (currentPlayingPosition == position && musicService.isPlaying()) {
            // Pause current song
            musicService.pauseMusic();
            mediaAdapter.setCurrentPlaying(-1);
            currentPlayingPosition = -1;
        } else if (currentPlayingPosition == position && !musicService.isPlaying()) {
            // Resume current song
            musicService.resumeMusic();
            mediaAdapter.setCurrentPlaying(position);
        } else {
            // Play new song
            musicService.playMusic(item.uri, item.title, item.artist);
            currentPlayingPosition = position;
            mediaAdapter.setCurrentPlaying(position);
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
                            // Reset nếu đang phát bài này
                            if (currentPlayingPosition == position) {
                                if (isBound)
                                    musicService.pauseMusic();
                                currentPlayingPosition = -1;
                                mediaAdapter.setCurrentPlaying(-1);
                            }
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
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onDestroy();
    }
}
