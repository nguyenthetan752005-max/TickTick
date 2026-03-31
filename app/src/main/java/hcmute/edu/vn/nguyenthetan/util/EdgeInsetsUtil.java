package hcmute.edu.vn.nguyenthetan.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Gom các đoạn apply window insets bị lặp lại giữa nhiều Activity.
 */
public final class EdgeInsetsUtil {
    private EdgeInsetsUtil() {}

    public static void applySystemBarsPadding(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public static void applySystemBarsAndImePadding(View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBarsAndIme = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(
                    systemBarsAndIme.left,
                    systemBarsAndIme.top,
                    systemBarsAndIme.right,
                    systemBarsAndIme.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Dùng cho BottomSheet: chỉ đẩy padding đáy theo IME hoặc navigation bar (lấy giá trị lớn hơn).
     * Giữ nguyên behavior legacy: set padding (0,0,0,bottomPadding).
     */
    public static void applyBottomPaddingImeOrNavBar(View bottomSheetView) {
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheetView, (v, insets) -> {
            int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int bottomPadding = Math.max(keyboardHeight, navBarHeight);
            v.setPadding(0, 0, 0, bottomPadding);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}

