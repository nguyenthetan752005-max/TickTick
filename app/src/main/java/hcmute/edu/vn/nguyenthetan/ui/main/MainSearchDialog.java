package hcmute.edu.vn.nguyenthetan.ui.main;

import android.content.Context;

import hcmute.edu.vn.nguyenthetan.util.DialogUtils;

public final class MainSearchDialog {
    private MainSearchDialog() {}

    public interface OnSearchListener {
        void onSearch(String keyword);
    }

    public static void show(Context context, OnSearchListener listener) {
        DialogUtils.showTextInputDialog(
                context,
                "🔍 Tìm kiếm nhiệm vụ",
                "Nhập tên nhiệm vụ...",
                null,
                "Tìm",
                "Hủy",
                null,
                new DialogUtils.OnTextInputListener() {
                    @Override
                    public void onPositive(String text) {
                        String keyword = text == null ? "" : text.trim();
                        if (keyword.isEmpty()) return;
                        if (listener != null) listener.onSearch(keyword);
                    }
                }
        );
    }
}

