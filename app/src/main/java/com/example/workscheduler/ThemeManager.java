package com.example.workscheduler;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    private static final String PREFS_NAME = "workscheduler_prefs";
    private static final String KEY_THEME = "app_theme";
    
    public static final int THEME_DARK = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_MEDIUM = 2;
    
    private final SharedPreferences prefs;
    
    public ThemeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public int getCurrentTheme() {
        return prefs.getInt(KEY_THEME, THEME_DARK);
    }
    
    public void setTheme(int theme) {
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }
    
    public int getThemeResId(int theme) {
        switch (theme) {
            case THEME_LIGHT:
                return R.style.Theme_WorkScheduler_Light;
            case THEME_MEDIUM:
                return R.style.Theme_WorkScheduler_Medium;
            case THEME_DARK:
            default:
                return R.style.Theme_WorkScheduler;
        }
    }
    
    public String getThemeName(int theme) {
        switch (theme) {
            case THEME_LIGHT:
                return "Light";
            case THEME_MEDIUM:
                return "Medium";
            case THEME_DARK:
            default:
                return "Dark";
        }
    }
    
    public int getNextTheme() {
        int current = getCurrentTheme();
        return (current + 1) % 3;
    }
}
