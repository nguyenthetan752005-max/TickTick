package hcmute.edu.vn.nguyenthetan.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import hcmute.edu.vn.nguyenthetan.ProfileActivity;
import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.util.ProfilePrefsUtil;
import hcmute.edu.vn.nguyenthetan.view.MainViewModel;

public final class NavHeaderActions {
    private NavHeaderActions() {}

    public interface TitleUpdater {
        void onSearchKeyword(String keyword);
    }

    public static void setup(
            Activity activity,
            DrawerLayout drawerLayout,
            NavigationView navigationView,
            MainViewModel viewModel,
            TitleUpdater titleUpdater
    ) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        View ivSearch = headerView.findViewById(R.id.ivSearch);
        if (ivSearch != null) {
            ivSearch.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                MainSearchDialog.show(activity, keyword -> {
                    viewModel.searchTasks(keyword);
                    if (titleUpdater != null) titleUpdater.onSearchKeyword(keyword);
                });
            });
        }

        View ivNotification = headerView.findViewById(R.id.ivNotification);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                viewModel.setFilterMode(0); // Inbox
            });
        }

        View ivProfile = headerView.findViewById(R.id.ivProfile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                activity.startActivity(new Intent(activity, ProfileActivity.class));
            });
        }
    }

    public static void loadProfileHeaderInfo(Activity activity, NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;
        ProfilePrefsUtil.loadHeaderProfile(activity, headerView);
    }
}

