package moe.csj430.checkkirafancompatibility;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by 430CSJ on 19-7-13.
 *
 * @author 430CSJ
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "557ff46e02", false);
    }
}
