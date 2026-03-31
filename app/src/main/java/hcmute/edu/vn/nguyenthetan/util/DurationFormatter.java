package hcmute.edu.vn.nguyenthetan.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DurationFormatter {
    private DurationFormatter() {}

    public static String formatMs(long ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}

