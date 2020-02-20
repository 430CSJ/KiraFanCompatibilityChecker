package moe.csj430.checkkirafancompatibility;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by 430CSJ on 19-7-13.
 *
 * @author 430CSJ
 */

public class App extends Application {

    private static Context appContext;
    private static int currTheme;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        CrashReport.initCrashReport(appContext, "557ff46e02", false);
        SharedPreferences themepref = getSharedPreferences("theme_pref", MODE_PRIVATE);
        currTheme = themepref.getInt("theme", 0x00);
        configAppTheme(currTheme);
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static int getCurrAppTheme() {
        return currTheme;
    }

    public static void setCurrAppTheme(int currTheme) {
        App.currTheme = currTheme;
        configAppTheme(currTheme);
        SharedPreferences.Editor themeeditor = appContext.getSharedPreferences("theme_pref", MODE_PRIVATE).edit();
        themeeditor.putInt("theme", currTheme);
        themeeditor.apply();
    }

    public static int getSysUiMode() {
        return appContext.getResources().getConfiguration().uiMode;
    }

    public static boolean getDarkModeStatus(int uiMode) {
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void configAppTheme(int themeid) {
        if (themeid == 0x10)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (themeid == 0x01)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
