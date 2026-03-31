package hcmute.edu.vn.nguyenthetan;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.nguyenthetan.model.ThemeType;
import hcmute.edu.vn.nguyenthetan.repository.ThemeRepository;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeRepository themeRepository = new ThemeRepository(this);
        ThemeType currentTheme = themeRepository.getCurrentTheme();

        switch (currentTheme) {
            case SUMMER:
                setTheme(R.style.Theme_TickTick_Summer);
                break;
            case HELL:
                setTheme(R.style.Theme_TickTick_Hell);
                break;
            case WINTER:
                setTheme(R.style.Theme_TickTick_Winter);
                break;
            case NEON:
                setTheme(R.style.Theme_TickTick_Neon);
                break;
            case DEFAULT:
            default:
                setTheme(R.style.Theme_TickTick_Default);
                break;
        }

        super.onCreate(savedInstanceState);
    }
}
