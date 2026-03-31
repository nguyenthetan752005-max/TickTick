package hcmute.edu.vn.nguyenthetan.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Cung cấp thread pool dùng chung cho các tác vụ I/O (database, file, v.v.).
 */
public class AppExecutors {

    private static volatile AppExecutors sInstance;

    private final Executor diskIO;

    private AppExecutors(Executor diskIO) {
        this.diskIO = diskIO;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (AppExecutors.class) {
                if (sInstance == null) {
                    sInstance = new AppExecutors(Executors.newSingleThreadExecutor());
                }
            }
        }
        return sInstance;
    }

    public Executor diskIO() {
        return diskIO;
    }
}

