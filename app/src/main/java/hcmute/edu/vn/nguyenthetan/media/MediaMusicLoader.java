package hcmute.edu.vn.nguyenthetan.media;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.adapter.MediaAdapter;

/**
 * Trách nhiệm duy nhất: load danh sách nhạc (internal upload + MediaStore).
 */
public class MediaMusicLoader {

    public List<MediaAdapter.MusicItem> loadAll(Context context) {
        List<MediaAdapter.MusicItem> musicItems = new ArrayList<>();
        musicItems.addAll(loadUploaded(context));
        musicItems.addAll(loadFromMediaStore(context));
        return musicItems;
    }

    public List<MediaAdapter.MusicItem> loadUploaded(Context context) {
        List<MediaAdapter.MusicItem> items = new ArrayList<>();

        File uploadDir = new File(context.getFilesDir(), "uploaded_music");
        if (!uploadDir.exists() || !uploadDir.isDirectory()) return items;

        File[] files = uploadDir.listFiles();
        if (files == null) return items;

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
                
                if (artist == null || artist.isEmpty()) {
                    artist = "Không rõ";
                }

                Uri fileUri = Uri.fromFile(f);
                items.add(new MediaAdapter.MusicItem(title, artist, duration, fileUri));
            } catch (Exception ignored) {
            }
        }

        try {
            retriever.release();
        } catch (Exception ignored) {
        }

        return items;
    }

    public List<MediaAdapter.MusicItem> loadFromMediaStore(Context context) {
        List<MediaAdapter.MusicItem> items = new ArrayList<>();

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

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
        )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

                    if (title == null) title = "Không tiêu đề";
                    if (artist == null) artist = "Không rõ";

                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                    );

                    items.add(new MediaAdapter.MusicItem(title, artist, duration, contentUri));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }
}
