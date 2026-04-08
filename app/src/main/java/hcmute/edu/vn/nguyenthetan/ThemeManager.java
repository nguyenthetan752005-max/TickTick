package hcmute.edu.vn.nguyenthetan;

import hcmute.edu.vn.nguyenthetan.util.ThemeColorUtils.ThemePalette;

/**
 * Singleton quản lý custom theme palette trong runtime
 * Lưu trữ palette hiện tại để các Activity/Fragment có thể truy cập
 * 
 * Pattern: Singleton - đảm bảo chỉ có 1 instance trong app lifecycle
 */
public class ThemeManager {
    private static ThemeManager instance;
    private ThemePalette currentPalette;
    
    private ThemeManager() {
        // Private constructor
    }
    
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    /**
     * Lưu palette hiện tại (dùng khi apply custom theme)
     */
    public void setCurrentPalette(ThemePalette palette) {
        this.currentPalette = palette;
    }
    
    /**
     * Lấy palette hiện tại
     * @return ThemePalette hoặc null nếu không có custom theme
     */
    public ThemePalette getCurrentPalette() {
        return currentPalette;
    }
    
    /**
     * Kiểm tra có custom palette đang active không
     */
    public boolean hasCustomPalette() {
        return currentPalette != null;
    }
    
    /**
     * Clear palette khi chuyển về preset theme
     */
    public void clearPalette() {
        this.currentPalette = null;
    }
}
