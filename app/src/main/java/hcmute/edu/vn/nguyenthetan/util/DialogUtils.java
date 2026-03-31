package hcmute.edu.vn.nguyenthetan.util;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * Chuẩn hoá các loại dialog hay lặp để giảm boilerplate.
 */
public final class DialogUtils {
    private DialogUtils() {}

    public interface OnTextInputListener {
        void onPositive(String text);

        default void onNeutral(String text) {}
    }

    public static void showConfirmDialog(
            Context context,
            String title,
            String message,
            String positiveText,
            Runnable onPositive,
            @Nullable String negativeText
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, (d, w) -> {
            if (onPositive != null) onPositive.run();
        });
        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (d, w) -> d.dismiss());
        }
        builder.show();
    }

    public static void showTextInputDialog(
            Context context,
            String title,
            String hint,
            @Nullable String initialValue,
            String positiveText,
            String negativeText,
            @Nullable String neutralText,
            OnTextInputListener listener
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        EditText input = new EditText(context);
        input.setHint(hint);
        if (initialValue != null) {
            input.setText(initialValue);
        }
        builder.setView(input);

        builder.setPositiveButton(positiveText, (d, w) -> {
            if (listener != null) {
                String text = input.getText().toString().trim();
                listener.onPositive(text);
            }
        });

        builder.setNegativeButton(negativeText, (d, w) -> d.dismiss());

        if (neutralText != null) {
            builder.setNeutralButton(neutralText, (d, w) -> {
                if (listener != null) {
                    String text = input.getText().toString().trim();
                    listener.onNeutral(text);
                }
            });
        }

        builder.show();
    }
}

