package com.example.workscheduler;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    protected ThemeManager themeManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeManager = new ThemeManager(this);
        int currentTheme = themeManager.getCurrentTheme();
        setTheme(themeManager.getThemeResId(currentTheme));
        super.onCreate(savedInstanceState);
    }

    public void recreateWithTheme() {
        recreate();
    }

    public int getAlertDialogTheme() {
        int currentTheme = themeManager.getCurrentTheme();
        switch (currentTheme) {
            case ThemeManager.THEME_LIGHT:
                return R.style.AlertDialog_WorkScheduler_Light;
            case ThemeManager.THEME_MEDIUM:
                return R.style.AlertDialog_WorkScheduler_Medium;
            case ThemeManager.THEME_DARK:
            default:
                return R.style.AlertDialog_WorkScheduler;
        }
    }
}
