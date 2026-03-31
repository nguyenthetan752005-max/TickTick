package hcmute.edu.vn.nguyenthetan.ui.main;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.adapter.TaskAdapter;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.util.DialogUtils;
import hcmute.edu.vn.nguyenthetan.view.MainViewModel;

public final class MultiSelectDeleteController {
    private final Activity activity;
    private final MainViewModel viewModel;
    private final TaskAdapter taskAdapter;

    private final View layoutNormalBar;
    private final View layoutDeleteBar;
    private final TextView tvSelectedCount;
    private final ImageView btnCloseDeleteMode;
    private final ImageView btnDeleteSelected;
    private final ImageView btnEnterDeleteMode;

    public MultiSelectDeleteController(
            Activity activity,
            MainViewModel viewModel,
            TaskAdapter taskAdapter,
            View layoutNormalBar,
            View layoutDeleteBar,
            TextView tvSelectedCount,
            ImageView btnCloseDeleteMode,
            ImageView btnDeleteSelected,
            ImageView btnEnterDeleteMode
    ) {
        this.activity = activity;
        this.viewModel = viewModel;
        this.taskAdapter = taskAdapter;
        this.layoutNormalBar = layoutNormalBar;
        this.layoutDeleteBar = layoutDeleteBar;
        this.tvSelectedCount = tvSelectedCount;
        this.btnCloseDeleteMode = btnCloseDeleteMode;
        this.btnDeleteSelected = btnDeleteSelected;
        this.btnEnterDeleteMode = btnEnterDeleteMode;
    }

    public void bind() {
        if (btnEnterDeleteMode != null) {
            btnEnterDeleteMode.setOnClickListener(v -> {
                taskAdapter.startMultiSelect();
                updateContextualBar(true);
            });
        }

        if (btnCloseDeleteMode != null) {
            btnCloseDeleteMode.setOnClickListener(v -> {
                taskAdapter.clearSelection();
                updateContextualBar(false);
            });
        }

        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> {
                List<Task> selected = taskAdapter.getSelectedTasks();
                if (selected.isEmpty()) return;

                DialogUtils.showConfirmDialog(
                        activity,
                        "Xóa nhiệm vụ",
                        "Bạn chắc chắn muốn xóa " + selected.size() + " nhiệm vụ này?",
                        "Xóa",
                        () -> {
                            viewModel.deleteTasks(selected);
                            taskAdapter.clearSelection();
                            updateContextualBar(false);
                        },
                        "Hủy"
                );
            });
        }
    }

    public void onSelectionChanged(int count) {
        updateContextualBar(count > 0);
    }

    /**
     * @return true nếu đã xử lý (thoát multi-select), false nếu không làm gì.
     */
    public boolean handleBackPressedIfNeeded() {
        if (taskAdapter != null && taskAdapter.getSelectedTasks().size() > 0) {
            taskAdapter.clearSelection();
            updateContextualBar(false);
            return true;
        }
        return false;
    }

    private void updateContextualBar(boolean isDeleteMode) {
        if (layoutNormalBar == null || layoutDeleteBar == null) return;
        layoutNormalBar.setVisibility(isDeleteMode ? View.GONE : View.VISIBLE);
        layoutDeleteBar.setVisibility(isDeleteMode ? View.VISIBLE : View.GONE);
        if (isDeleteMode && tvSelectedCount != null) {
            tvSelectedCount.setText(taskAdapter.getSelectedTasks().size() + " selected");
        }
    }
}

