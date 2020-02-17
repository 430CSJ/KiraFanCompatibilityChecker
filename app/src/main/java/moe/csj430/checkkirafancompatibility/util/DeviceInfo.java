package moe.csj430.checkkirafancompatibility.util;

/**
 * Created by Administrator on 2017/10/6.
 */

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceInfo {
    /**
     * The default return value of any method in this class when an
     * error occurs or when processing fails (Currently set to -1). Use this to check if
     * the information about the device in question was successfully obtained.
     */
    public static final int DEVICEINFO_UNKNOWN = -1;

    /**
     * Reads the number of CPU cores from {@code /sys/devices/system/cpu/}.
     *
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;
        }
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException e) {
            cores = DEVICEINFO_UNKNOWN;
        } catch (NullPointerException e) {
            cores = DEVICEINFO_UNKNOWN;
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    /**
     * Method for reading the clock speed of a CPU core on the device. Will read from either
     * {@code /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq} or {@code /proc/cpuinfo}.
     *
     * @return Clock speed of a core on the device, or -1 in the event of an error.
     */
    public static int getCPUMaxFreqKHz() {
        int maxFreq = DEVICEINFO_UNKNOWN;
        try {
            for (int i = 0; i < getNumberOfCPUCores(); i++) {
                String filename =
                        "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                File cpuInfoMaxFreqFile = new File(filename);
                if (cpuInfoMaxFreqFile.exists()) {
                    byte[] buffer = new byte[128];
                    FileInputStream stream = new FileInputStream(cpuInfoMaxFreqFile);
                    try {
                        stream.read(buffer);
                        int endIndex = 0;
                        //Trim the first number out of the byte buffer.
                        while (buffer[endIndex] >= '0' && buffer[endIndex] <= '9'
                                && endIndex < buffer.length) endIndex++;
                        String str = new String(buffer, 0, endIndex);
                        Integer freqBound = Integer.parseInt(str);
                        if (freqBound > maxFreq) maxFreq = freqBound;
                    } catch (NumberFormatException e) {
                        //Fall through and use /proc/cpuinfo.
                    } finally {
                        stream.close();
                    }
                }
            }
            if (maxFreq == DEVICEINFO_UNKNOWN) {
                FileInputStream stream = new FileInputStream("/proc/cpuinfo");
                try {
                    int freqBound = parseFileForValue("cpu MHz", stream);
                    freqBound *= 1000; //MHz -> kHz
                    if (freqBound > maxFreq) maxFreq = freqBound;
                } finally {
                    stream.close();
                }
            }
        } catch (IOException e) {
            maxFreq = DEVICEINFO_UNKNOWN; //Fall through and return unknown.
        }
        return maxFreq;
    }

    /**
     * Calculates the total RAM of the device through Android API or /proc/meminfo.
     *
     * @param c - Context object for current running activity.
     * @return Total RAM that the device has, or DEVICEINFO_UNKNOWN = -1 in the event of an error.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getTotalMemory(Context c) {
        // memInfo.totalMem not supported in pre-Jelly Bean APIs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            am.getMemoryInfo(memInfo);
            if (memInfo != null) {
                return memInfo.totalMem;
            } else {
                return DEVICEINFO_UNKNOWN;
            }
        } else {
            long totalMem = DEVICEINFO_UNKNOWN;
            try {
                FileInputStream stream = new FileInputStream("/proc/meminfo");
                try {
                    totalMem = parseFileForValue("MemTotal", stream);
                    totalMem *= 1024;
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
            }
            return totalMem;
        }
    }

    /**
     * Helper method for reading values from system files, using a minimised buffer.
     *
     * @param textToMatch - Text in the system files to read for.
     * @param stream      - FileInputStream of the system file being read from.
     * @return A numerical value following textToMatch in specified the system file.
     * -1 in the event of a failure.
     */
    private static int parseFileForValue(String textToMatch, FileInputStream stream) {
        byte[] buffer = new byte[1024];
        try {
            int length = stream.read(buffer);
            for (int i = 0; i < length; i++) {
                if (buffer[i] == '\n' || i == 0) {
                    if (buffer[i] == '\n') i++;
                    for (int j = i; j < length; j++) {
                        int textIndex = j - i;
                        //Text doesn't match query at some point.
                        if (buffer[j] != textToMatch.charAt(textIndex)) {
                            break;
                        }
                        //Text matches query here.
                        if (textIndex == textToMatch.length() - 1) {
                            return extractValue(buffer, j);
                        }
                    }
                }
            }
        } catch (IOException e) {
            //Ignore any exceptions and fall through to return unknown value.
        } catch (NumberFormatException e) {
        }
        return DEVICEINFO_UNKNOWN;
    }

    /**
     * Helper method used by {@link #parseFileForValue(String, FileInputStream) parseFileForValue}. Parses
     * the next available number after the match in the file being read and returns it as an integer.
     * @param index - The index in the buffer array to begin looking.
     * @return The next number on that line in the buffer, returned as an int. Returns
     * DEVICEINFO_UNKNOWN = -1 in the event that no more numbers exist on the same line.
     */
    private static int extractValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (buffer[index] >= '0' && buffer[index] <= '9') {
                int start = index;
                index++;
                while (index < buffer.length && buffer[index] >= '0' && buffer[index] <= '9') {
                    index++;
                }
                String str = new String(buffer, 0, start, index - start);
                return Integer.parseInt(str);
            }
            index++;
        }
        return DEVICEINFO_UNKNOWN;
    }

    public static String[] getVersion() {
        String[] version = {"null", "null", "null", "null"};
        String str1 = "/proc/version";
        String str2;
        String[] arrayOfString;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();
            arrayOfString = str2.split("\\s+");
            version[0] = arrayOfString[2];//KernelVersion
            localBufferedReader.close();
        } catch (IOException e) {
            version[0] = "Fail to get kernel version.";
        }
        version[1] = Build.VERSION.RELEASE;//FirmwareVersion
        version[2] = Build.MODEL;//Model
        version[3] = Build.DISPLAY;//SystemVersion
        return version;
    }

    public static boolean isUsbDebugOn(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            return Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) > 0;
        else
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0;
    }

    public static int isGoogleServiceAvailable(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultcode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultcode;
    }

    public static final String[] BLACK_LIST_APPS_PACKAGE_NAME = {
           "com.cih.game_cih",
            "com.hexview.android.memspector",
            "cn.mm.gk",
            "pl.Nyki.Dax",
            "catch_.me_.if_.you_.can_",
            "com.sbgamehacker",
            "jp.kbc.ma34.devicefaker",
            "com.saurik.substrate",
            "de.robv.android.xposed.installer",
            "com.felixheller.sharedprefseditor",
            "cn.mc.sq",
            "cn.mc1.sq",
            "com.cih.game_cih",
            "pl.aqua.gameguardian",
            "org.sbtools.gamehack",
            "com.hexview.android.memspector",
            "mr.big.stuff",
            "cat.dcat.roothide",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.topjohnwu.magisk",
            "com.loserskater.suhidegui",
            "eu.chainfire.suhide",
            "eu.chainfire.supersu",
            "eu.chainfire.supersu.pro",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "me.phh.superuser"
    };

    public static List<ResolveInfo> getAppList(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> applist = context.getPackageManager().queryIntentActivities(intent, 0);
        return applist;
    }

    public static List<ResolveInfo> getBlackListApps(Context context) {
        List<ResolveInfo> applist = getAppList(context);
        List<ResolveInfo> blacklistapps = new ArrayList<>();
        for (int i = 0; i < applist.size(); i++) {
            ResolveInfo info = applist.get(i);
            if (Arrays.asList(BLACK_LIST_APPS_PACKAGE_NAME).contains(info.activityInfo.packageName))
                blacklistapps.add(info);
        }
        return blacklistapps;
    }
}
