package hcmute.edu.vn.nguyenthetan.ui.taskdialog;

import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import hcmute.edu.vn.nguyenthetan.util.EdgeInsetsUtil;

public final class BottomSheetConfigurer {
    private BottomSheetConfigurer() {}

    public static void configureExpandedWithImePadding(View bottomSheet) {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
        EdgeInsetsUtil.applyBottomPaddingImeOrNavBar(bottomSheet);
    }
}

