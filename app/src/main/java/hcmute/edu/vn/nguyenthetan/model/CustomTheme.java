package hcmute.edu.vn.nguyenthetan.model;

import android.graphics.Color;

/**
 * Model lưu trữ cấu hình theme custom do người dùng chọn
 * Sử dụng combo 2 màu: Background + Primary
 */
public class CustomTheme {
    private final int backgroundColor;
    private final int primaryColor;
    
    public CustomTheme(int backgroundColor, int primaryColor) {
        this.backgroundColor = backgroundColor;
        this.primaryColor = primaryColor;
    }
    
    public int getBackgroundColor() {
        return backgroundColor;
    }
    
    public int getPrimaryColor() {
        return primaryColor;
    }
    
    /**
     * Chuyển đổi màu sang định dạng hex string để lưu SharedPreferences
     */
    public String getBackgroundColorHex() {
        return String.format("#%06X", (0xFFFFFF & backgroundColor));
    }
    
    public String getPrimaryColorHex() {
        return String.format("#%06X", (0xFFFFFF & primaryColor));
    }
    
    /**
     * Parse từ hex string sang CustomTheme
     */
    public static CustomTheme fromHex(String bgHex, String primaryHex) {
        int bg = Color.parseColor(bgHex);
        int primary = Color.parseColor(primaryHex);
        return new CustomTheme(bg, primary);
    }
    
    /**
     * Kiểm tra theme có hợp lệ không
     */
    public boolean isValid() {
        return backgroundColor != 0 && primaryColor != 0;
    }
}
