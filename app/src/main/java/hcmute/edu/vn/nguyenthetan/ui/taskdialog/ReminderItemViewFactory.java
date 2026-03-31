package hcmute.edu.vn.nguyenthetan.ui.taskdialog;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ReminderItemViewFactory {
    private ReminderItemViewFactory() {}

    public static void addReminderItem(
            Context context,
            LinearLayout container,
            String text,
            Runnable onRemove
    ) {
        LinearLayout itemLayout = createReminderItemLayout(context, text);

        ImageView btnRemove = new ImageView(context);
        btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                dpToPx(context, 20),
                dpToPx(context, 20)
        );
        removeParams.gravity = Gravity.CENTER_VERTICAL;
        btnRemove.setLayoutParams(removeParams);

        btnRemove.setOnClickListener(v -> {
            if (onRemove != null) onRemove.run();
            container.removeView(itemLayout);
        });

        itemLayout.addView(btnRemove);
        container.addView(itemLayout);
    }

    private static LinearLayout createReminderItemLayout(Context context, String text) {
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, dpToPx(context, 4), 0, dpToPx(context, 4));
        itemLayout.setLayoutParams(itemParams);
        itemLayout.setPadding(dpToPx(context, 8), dpToPx(context, 6), dpToPx(context, 8), dpToPx(context, 6));

        TextView tvReminder = new TextView(context);
        tvReminder.setText(text);
        tvReminder.setTextColor(0xFFEBEBF5);
        tvReminder.setTextSize(13);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        );
        tvReminder.setLayoutParams(textParams);
        itemLayout.addView(tvReminder);

        return itemLayout;
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}

