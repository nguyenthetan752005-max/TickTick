package hcmute.edu.vn.nguyenthetan.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.LinearLayout;

import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.ThemeType;
import hcmute.edu.vn.nguyenthetan.repository.ThemeRepository;

public class ThemeDialogHelper {
    public static void showDialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_theme_picker);

        // Make background transparent for rounded corners if needed
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }

        ThemeRepository repo = new ThemeRepository(activity);

        dialog.findViewById(R.id.btnThemeDefault).setOnClickListener(v -> {
            repo.saveTheme(ThemeType.DEFAULT);
            dialog.dismiss();
            activity.recreate();
        });

        dialog.findViewById(R.id.btnThemeSummer).setOnClickListener(v -> {
            repo.saveTheme(ThemeType.SUMMER);
            dialog.dismiss();
            activity.recreate();
        });

        dialog.findViewById(R.id.btnThemeHell).setOnClickListener(v -> {
            repo.saveTheme(ThemeType.HELL);
            dialog.dismiss();
            activity.recreate();
        });

        dialog.findViewById(R.id.btnThemeWinter).setOnClickListener(v -> {
            repo.saveTheme(ThemeType.WINTER);
            dialog.dismiss();
            activity.recreate();
        });

        dialog.findViewById(R.id.btnThemeNeon).setOnClickListener(v -> {
            repo.saveTheme(ThemeType.NEON);
            dialog.dismiss();
            activity.recreate();
        });

        dialog.show();
    }
}
