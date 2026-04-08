package hcmute.edu.vn.nguyenthetan.ui.theme;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;

import hcmute.edu.vn.nguyenthetan.R;

/**
 * Simple color picker dialog với preset colors
 * Có thể thay thế bằng thư viện color picker nâng cao nếu cần
 */
public class ColorPickerDialog extends Dialog {
    
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }
    
    private final OnColorSelectedListener listener;
    private final int currentColor;
    
    // Preset colors cho user chọn nhanh
    private static final int[] PRESET_COLORS = {
        Color.parseColor("#000000"), // Black
        Color.parseColor("#1A2332"), // Dark Navy
        Color.parseColor("#001C30"), // Dark Blue
        Color.parseColor("#200000"), // Dark Red
        Color.parseColor("#050511"), // Dark Purple
        Color.parseColor("#1E1E1E"), // Dark Gray
        Color.parseColor("#5271FF"), // Blue
        Color.parseColor("#F59E0B"), // Orange
        Color.parseColor("#E53935"), // Red
        Color.parseColor("#DAFFFB"), // Cyan/White
        Color.parseColor("#FF007F"), // Pink
        Color.parseColor("#00F5FF"), // Neon Cyan
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#FF9800"), // Amber
        Color.parseColor("#FFFFFF"), // White
    };
    
    public ColorPickerDialog(Context context, OnColorSelectedListener listener, int currentColor) {
        super(context);
        this.listener = listener;
        this.currentColor = currentColor;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_color_picker);
        
        GridLayout colorGrid = findViewById(R.id.colorGrid);
        
        for (int color : PRESET_COLORS) {
            ImageView colorView = new ImageView(getContext());
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke(4, Color.WHITE);
            colorView.setImageDrawable(drawable);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 80;
            params.height = 80;
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            
            // Highlight current color
            if (color == currentColor) {
                drawable.setStroke(6, Color.YELLOW);
            }
            
            colorView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(color);
                }
                dismiss();
            });
            
            colorGrid.addView(colorView);
        }
        
        findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
    }
}
