package hcmute.edu.vn.nguyenthetan.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.dao.TaskDao;

public class TaskContentProvider extends ContentProvider {

    public static final String AUTHORITY = "hcmute.edu.vn.nguyenthetan.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tasks");

    private static final int TASKS = 1;
    private static final int TASK_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "tasks", TASKS);
        uriMatcher.addURI(AUTHORITY, "tasks/#", TASK_ID);
    }

    private TaskDao taskDao;

    @Override
    public boolean onCreate() {
        if (getContext() != null) {
            taskDao = AppDatabase.getInstance(getContext()).taskDao();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case TASKS:
                cursor = taskDao.getTasksCursor();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TASKS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".tasks";
            case TASK_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".tasks";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("Insert is currently not supported via Content Provider.");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Delete is currently not supported via Content Provider.");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Update is currently not supported via Content Provider.");
    }
}
