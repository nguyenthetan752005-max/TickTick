package hcmute.edu.vn.nguyenthetan.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import hcmute.edu.vn.nguyenthetan.R;

/**
 * Gom code đọc/ghi Profile từ SharedPreferences.
 */
public final class ProfilePrefsUtil {
    private ProfilePrefsUtil() {}

    public static final String PREFS_NAME = "ticktick_prefs";
    public static final String KEY_AVATAR_URI = "avatar_uri";
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_EMAIL = "user_email";
    public static final String KEY_BIO = "user_bio";

    public static void loadHeaderProfile(Context context, View headerView) {
        if (headerView == null) return;

        ImageView ivProfile = headerView.findViewById(R.id.ivProfile);
        TextView tvUserName = headerView.findViewById(R.id.tvUserName);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (ivProfile != null) {
            String uriStr = prefs.getString(KEY_AVATAR_URI, null);
            if (uriStr != null) {
                try {
                    ivProfile.setImageURI(Uri.parse(uriStr));
                } catch (Exception e) {
                    ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        if (tvUserName != null) {
            String name = prefs.getString(KEY_DISPLAY_NAME, "Nguyễn Thế Tân");
            tvUserName.setText(name);
        }
    }

    public static void loadProfileData(
            Context context,
            ImageView avatarView,
            EditText etDisplayName,
            EditText etEmail,
            EditText etBio
    ) {
        if (avatarView == null || etDisplayName == null || etEmail == null || etBio == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String avatarUri = prefs.getString(KEY_AVATAR_URI, null);
        if (avatarUri != null) {
            try {
                avatarView.setImageURI(Uri.parse(avatarUri));
            } catch (Exception e) {
                avatarView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        String name = prefs.getString(KEY_DISPLAY_NAME, "Nguyễn Thế Tân");
        String email = prefs.getString(KEY_EMAIL, "thetan.nguyen@example.com");
        String bio = prefs.getString(KEY_BIO, "Yêu thích sự gọn gàng và lập trình di động.");

        etDisplayName.setText(name);
        etEmail.setText(email);
        etBio.setText(bio);
    }

    public static void saveProfileData(
            Context context,
            String displayName,
            String email,
            String bio
    ) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DISPLAY_NAME, displayName);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_BIO, bio);
        editor.apply();
    }

    public static void saveAvatarUri(Context context, String avatarUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_AVATAR_URI, avatarUri).apply();
    }

    public static void removeAvatar(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_AVATAR_URI).apply();
    }
}

