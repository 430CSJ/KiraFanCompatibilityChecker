package moe.csj430.checkkirafancompatibility.viewmodel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Process;
import android.util.SparseArray;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;
import com.unionpay.mobile.device.utils.RootCheckerUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import de.robv.android.xposed.XposedBridge;
import moe.csj430.checkkirafancompatibility.R;
import moe.csj430.checkkirafancompatibility.util.SystemPropertiesProxy;

import static moe.csj430.checkkirafancompatibility.App.getAppContext;
import static moe.csj430.checkkirafancompatibility.util.DeviceInfo.BLACK_LIST_APPS_PACKAGE_NAME;
import static moe.csj430.checkkirafancompatibility.util.DeviceInfo.getBlackListApps;
import static moe.csj430.checkkirafancompatibility.util.DeviceInfo.getTotalMemory;
import static moe.csj430.checkkirafancompatibility.util.DeviceInfo.getVersion;
import static moe.csj430.checkkirafancompatibility.util.DeviceInfo.isGoogleServiceAvailable;
import static moe.csj430.checkkirafancompatibility.util.DeviceInfo.isUsbDebugOn;

public class MainViewModel extends ViewModel {

    private Long total_men = null;
    private String instruction_set = null;
    private int system_sdk_int;
    private String system_ver;
    private MutableLiveData<Integer> gs_re_code = new MutableLiveData<>();
    private MutableLiveData<List<Object>> sys_status = new MutableLiveData<>();
    private MutableLiveData<List<Integer>> xposed_status = new MutableLiveData<>();
    private MutableLiveData<List<ResolveInfo>> blacklist_apps = new MutableLiveData<>();
    private final FutureTask[] futureTasks = {null, null, null};
    private boolean done[] = {false, false, false};
    private final int ALL_ALLOW = 0777;
    private int[] statusColors = {Color.RED, Color.YELLOW, Color.GREEN};


    private String[] CHECK_XPOSED_ITEM = {
            "载入Xposed工具类",
            "寻找特征动态链接库",
            "代码堆栈寻找调起者",
            "检测Xposed安装情况",
            "判定系统方法调用钩子",
            "检测虚拟Xposed环境",
            "寻找Xposed运行库文件",
            "内核查找Xposed链接库",
            "环境变量特征字判断",
    };

    private String[] ROOT_STATUS = {"出错", "未发现Root", "发现Root"};

    private final String[] CHECK_PROP_ITEM = {
            "persist.sys.usb.config",
            "ro.build.type",
            "ro.debuggable",
            "ro.secure"
    };

    private String[] CHECK_SYS_ITEM = {
            "版本",
            CHECK_PROP_ITEM[0],
            CHECK_PROP_ITEM[1],
            CHECK_PROP_ITEM[2],
            CHECK_PROP_ITEM[3],
            "进程挂载信息",
            "可能被检测到的文件和目录",
            "USB调试"
    };

    public String[] getCheckSysItem() {
        return CHECK_SYS_ITEM;
    }

    public String[] getCheckXposedItem() {
        return CHECK_XPOSED_ITEM;
    }

    public String[] getRootStatus() {
        return ROOT_STATUS;
    }

    public String[] getCheckPropItem() {
        return CHECK_PROP_ITEM;
    }

    private Context app_context = getAppContext();
    private boolean has_init = false;

    public FutureTask[] getFutureTasks() {
        return futureTasks;
    }

    public String getSystemVer() {
        return system_ver;
    }

    public Long getTotalMen() {
        return total_men;
    }

    public String getInstructionSet() {
        return instruction_set;
    }

    public MutableLiveData<Integer> getGsReCode() {
        return gs_re_code;
    }

    public MutableLiveData<List<Object>> getSysStatus() {
        return sys_status;
    }

    public MutableLiveData<List<Integer>> getXposedStatus() {
        return xposed_status;
    }

    public MutableLiveData<List<ResolveInfo>> getBlacklistApps() {
        return blacklist_apps;
    }

    public boolean getHasInit() {
        return has_init;
    }

    public void initVM() {
        if (has_init)
            return;
        checkXposed();
        checkBlacklistApps();
        system_sdk_int = Build.VERSION.SDK_INT;
        system_ver = getVersion()[1];
        checkSys();
        total_men = getTotalMemory(app_context);
        StringBuilder instset = new StringBuilder("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int abicnt = Build.SUPPORTED_ABIS.length;
            if (abicnt > 0) {
                instset.append(Build.SUPPORTED_ABIS[0]);
                for (int insi = 1; insi < abicnt; ++insi) {
                    instset.append(", ");
                    instset.append(Build.SUPPORTED_ABIS[insi]);
                }
            } else {
                if (!Build.CPU_ABI.equals("")) {
                    instset.append(Build.CPU_ABI);
                    if (!Build.CPU_ABI2.equals(""))
                        instset.append(", ").append(Build.CPU_ABI2);
                } else {
                    if (!Build.CPU_ABI2.equals(""))
                        instset.append(Build.CPU_ABI2);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            statusColors[0] = app_context.getColor(R.color.status_red);
            statusColors[1] = app_context.getColor(R.color.status_yellow);
            statusColors[2] = app_context.getColor(R.color.status_green);
        } else {
            statusColors[0] = app_context.getResources().getColor(R.color.status_red);
            statusColors[1] = app_context.getResources().getColor(R.color.status_yellow);
            statusColors[2] = app_context.getResources().getColor(R.color.status_green);
        }
        instruction_set = instset.toString();
        checkGsReCode();

        refreshItemText();

        boolean test_init = true;
        for (int i = 0; i < 3; ++i)
            test_init = test_init & done[i];
        has_init = test_init;
    }

    public int[] getStatusColors() {
        return statusColors;
    }

    private void refreshItemText() {
        CHECK_XPOSED_ITEM[0]=app_context.getResources().getString(R.string.load_xposed_toolkit);
        CHECK_XPOSED_ITEM[1]=app_context.getResources().getString(R.string.search_feature_dll);
        CHECK_XPOSED_ITEM[2]=app_context.getResources().getString(R.string.code_stack_search_invoker);
        CHECK_XPOSED_ITEM[3]=app_context.getResources().getString(R.string.check_xposed_install_status);
        CHECK_XPOSED_ITEM[4]=app_context.getResources().getString(R.string.judge_system_method_hook);
        CHECK_XPOSED_ITEM[5]=app_context.getResources().getString(R.string.check_virtual_xposed);
        CHECK_XPOSED_ITEM[6]=app_context.getResources().getString(R.string.search_xposed_runtime_lib);
        CHECK_XPOSED_ITEM[7]=app_context.getResources().getString(R.string.kernel_search_xposed_link_lib);
        CHECK_XPOSED_ITEM[8]=app_context.getResources().getString(R.string.judge_envir_variable_chara_word);
        ROOT_STATUS[0]=app_context.getResources().getString(R.string.get_error);
        ROOT_STATUS[1]=app_context.getResources().getString(R.string.item_no_root);
        ROOT_STATUS[2]=app_context.getResources().getString(R.string.item_found_root);
        CHECK_SYS_ITEM[0]=app_context.getResources().getString(R.string.version);
        CHECK_SYS_ITEM[5]=app_context.getResources().getString(R.string.proc_mounts);
        CHECK_SYS_ITEM[6]=app_context.getResources().getString(R.string.files_may_detected);
        CHECK_SYS_ITEM[7]=app_context.getResources().getString(R.string.usb_debug);
    }

    public void checkGsReCode() {
        gs_re_code.postValue(isGoogleServiceAvailable(app_context));
    }

    private class CheckThread<T> implements Callable<T> {
        @Override
        public T call() throws Exception {
            if (futureTasks[fti] != null)
                return null;
            synchronized (lockobj) {
                if (futureTasks[fti] != null)
                    return null;
                futureTasks[fti] = new FutureTask<>(mThread);
            }
            new Thread(futureTasks[fti]).start();
            T re = (T)futureTasks[fti].get();
            futureTasks[fti] = null;
            done[fti] = true;
            return re;
        }

        Callable<T> mThread;
        int fti;
        final Object lockobj = new Object();

        CheckThread(Callable<T> thread) {
            mThread = thread;
            if (mThread instanceof CheckSysThread)
                fti = 0;
            else if (mThread instanceof CheckXposedThread)
                fti = 1;
            else if (mThread instanceof CheckBlacklistAppsThread)
                fti = 2;
        }
    }

    private class CheckBlacklistAppsThread implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            blacklist_apps.postValue(getBlackListApps(app_context));
            return null;
        }
    }

    private final CheckThread checkBlacklistAppsThread = new CheckThread<>(new CheckBlacklistAppsThread());

    private boolean[] thisProcessMounts = null;

    public boolean[] getThisProcessMounts() {
        return thisProcessMounts;
    }

    private class CheckSysThread implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            List<Object> sstatus = new ArrayList<>();
            sstatus.add(system_sdk_int);
            int sysi = 1;
            boolean tocheckusb = false;
            for (String checkprop : CHECK_PROP_ITEM) {
                if (sysi <= 2)
                    sstatus.add(SystemPropertiesProxy.getString(app_context, checkprop));
                else
                    sstatus.add(SystemPropertiesProxy.getInt(app_context, checkprop, 0));
                if (checkprop.equals("persist.sys.usb.config") && "adb".equals(sstatus.get(sysi)))
                    tocheckusb = true;
                ++sysi;
            }
            String[] checkTarget = new String[]{"magisk"};
            thisProcessMounts = checkThisProcessMounts(checkTarget);
            otherAppProcessMounts = checkOtherAppProcessMounts(checkTarget);
            boolean findMagisk = (thisProcessMounts != null && ((thisProcessMounts.length > 0 && thisProcessMounts[0])) || (otherAppProcessMounts != null && otherAppProcessMounts.length > 0 && otherAppProcessMounts[0] != null && otherAppProcessMounts[0].size() > 0));
            sstatus.add(findMagisk);
            dPathFile = checkPathFile(Arrays.asList("su", "xposed"));
            boolean findDPF = (dPathFile[0] != null && dPathFile[0].size() > 0) || (dPathFile[1] != null && dPathFile[1].size() > 0);
            sstatus.add(findDPF);
            if (tocheckusb) {
                sstatus.add(isUsbDebugOn(app_context));
            }
            sys_status.postValue(sstatus);
            return null;
        }
    }

    private final CheckThread checkSysThread = new CheckThread<>(new CheckSysThread());

    private class CheckXposedThread implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            List<Integer> xstatus = new ArrayList<>();
            for (int i = 0; i <= CHECK_XPOSED_ITEM.length; ++i) {
                Method method = MainViewModel.class.getDeclaredMethod("check" + (i + 1));
                method.setAccessible(true);
                try {
                    xstatus.add((int) method.invoke(MainViewModel.this));
                } catch (Throwable e) {
                    xstatus.add(0);
                }
            }
            xposed_status.postValue(xstatus);
            return null;
        }
    }

    private final CheckThread checkXposedThread = new CheckThread<>(new CheckXposedThread());

    private class UnpackThread implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            if (!new File(app_context.getFilesDir().getAbsolutePath() + "/checkman").exists()) {
                InputStream inputStream = app_context.getAssets().open("checkman");
                OutputStream outputStream = app_context.openFileOutput("checkman", Context.MODE_PRIVATE);
                int bit;
                while ((bit = inputStream.read()) != -1)
                    outputStream.write(bit);
            }
            setFilePermissions(app_context.getFilesDir(), ALL_ALLOW, -1, -1);
            setFilePermissions(app_context.getFilesDir().getAbsolutePath() + "/checkman", ALL_ALLOW, -1, -1);
            return null;
        }

        boolean setFilePermissions(File file, int chmod, int uid, int gid) {
            if (file != null) {
                Class<?> fileUtils;
                try {
                    fileUtils = Class.forName("android.os.FileUtils");
                    Method setPermissions = fileUtils.getMethod("setPermissions", File.class, int.class, int.class, int.class);
                    int result = (Integer) setPermissions.invoke(null, file, chmod, uid, gid);

                    return result == 0;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return false;
            } else {
                return false;
            }
        }

        boolean setFilePermissions(String file, int chmod, int uid, int gid) {
            return setFilePermissions(new File(file), chmod, uid, gid);
        }
    }

    private final UnpackThread unpackThread = new UnpackThread();
    private FutureTask unpackTask = null;

    public void checkXposed() {
        try {
            if (unpackTask == null) {
                synchronized (unpackThread) {
                    unpackTask = new FutureTask<>(unpackThread);
                    new Thread(unpackTask).start();
                    unpackTask.get();
                    unpackTask = null;
                }
            }
            FutureTask futureTask = new FutureTask<>(checkXposedThread);
            new Thread(futureTask).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkSys() {
        try {
            FutureTask futureTask = new FutureTask<>(checkSysThread);
            new Thread(futureTask).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkBlacklistApps() {
        try {
            FutureTask futureTask = new FutureTask<>(checkBlacklistAppsThread);
            new Thread(futureTask).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Keep
    private int check1() {

        return testClassLoader() || testUseClassDirectly() ? 1 : 0;
    }

    private boolean testClassLoader() {
        try {
            ClassLoader.getSystemClassLoader()
                    .loadClass("de.robv.android.xposed.XposedHelpers");

            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean testUseClassDirectly() {
        try {
            XposedBridge.log("fuck wechat");
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    @Keep
    private int check2() {
        return checkContains("XposedBridge") ? 1 : 0;
    }

    @Keep
    private int check3() {
        try {
            throw new Exception();
        } catch (Exception e) {
            StackTraceElement[] arrayOfStackTraceElement = e.getStackTrace();
            for (StackTraceElement s : arrayOfStackTraceElement) {
                if ("de.robv.android.xposed.XposedBridge".equals(s.getClassName())) {
                    return 1;
                }
            }
            return 0;
        }
    }

    @Keep
    private int check4() {
        try {
            List<PackageInfo> list = app_context.getPackageManager().getInstalledPackages(0);
            for (PackageInfo info : list) {
                if ("de.robv.android.xposed.installer".equals(info.packageName)) {
                    return 1;
                }
                if ("io.va.exposed".equals(info.packageName)) {
                    return 1;
                }
            }
        } catch (Throwable ignored) {

        }
        return 0;
    }

    @Keep
    private int check5() {
        try {
            Method method = Throwable.class.getDeclaredMethod("getStackTrace");
            return Modifier.isNative(method.getModifiers()) ? 1 : 0;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Keep
    private int check6() {
        return System.getProperty("vxp") != null ? 1 : 0;
    }


    /**
     * @param paramString check string
     * @return whether check string is found in maps
     */
    public static boolean checkContains(String paramString) {
        try {
            HashSet<String> localObject = new HashSet<>();
            // 读取maps文件信息
            BufferedReader localBufferedReader =
                    new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/maps"));
            while (true) {
                String str = localBufferedReader.readLine();
                if (str == null) {
                    break;
                }
                localObject.add(str.substring(str.lastIndexOf(" ") + 1));
            }
            //应用程序的链接库不可能是空，除非是高于7.0。。。
            if (localObject.isEmpty() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                return true;
            }
            localBufferedReader.close();
            for (String aLocalObject : localObject) {
                if (aLocalObject.contains(paramString)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private final static String[] C_DIRS = {
            "/data",
            "/data/data",
            "/data/local",
            "/data/local/tmp",
            "/data/user/0",
            "/data/misc_ce/0"
    };

    private final static String[] C_FILES = {
            "com.sudocode.sudohide/shared_prefs/com.sudocode.sudohide_preferences.xml",
            "/system/app/superuser.apk",
            "/system/app/Superuser.apk",
            "/system/app/SuperUser.apk",
            "/system/app/SUPERUSER.apk"
    };

    private Collection<String>[] dPathFile;

    public Collection<String>[] getDPathFile() {
        return dPathFile;
    }

    private static Collection<String>[] checkPathFile(@NonNull List<String> targets) {
        Collection<String>[] results = new HashSet[2];
        results[0] = new HashSet<>();
        results[1] = new HashSet<>();
        for (String fn : C_FILES) {
            File f = new File(fn);
            if (f.exists()) {
                if (f.isDirectory())
                    results[1].add(fn);
                else
                    results[0].add(fn);
            }
        }
        List<String> blappspn = new ArrayList<>(Arrays.asList(BLACK_LIST_APPS_PACKAGE_NAME));
        Collection<String> appspn = new HashSet<>(blappspn);
        try {
            List<PackageInfo> list = getAppContext().getPackageManager().getInstalledPackages(0);
            for (PackageInfo info : list)
                appspn.add(info.packageName);
        } catch (Throwable ignored) {
        }
        Stack<File> fileStack = new Stack<>();
        for (String pn : appspn) {
            String fn = "/" + pn;
            File fd = new File(fn);
            if (fd.exists()) {
                if (blappspn.contains(pn)) {
                    if (fd.isDirectory())
                        results[1].add(fn);
                    else
                        results[0].add(fn);
                } else
                    fileStack.push(fd);
            }
        }
        while (!fileStack.empty()) {
            File currfd = fileStack.pop();
            String currfn = currfd.getName();
            if (targets.contains(currfn) || blappspn.contains(currfn)) {
                if (currfd.isDirectory())
                    results[1].add(currfd.getAbsolutePath());
                else
                    results[0].add(currfd.getAbsolutePath());
            } else if (currfd.exists() && currfd.isDirectory()) {
                File[] tmpfl = currfd.listFiles();
                if (tmpfl != null)
                    fileStack.addAll(Arrays.asList(tmpfl));
            }
        }
        for (String cd : C_DIRS) {
            for (String pn : appspn) {
                File fd = new File(cd + "/" + pn);
                if (fd.exists()) {
                    if (blappspn.contains(pn)) {
                        if (fd.isDirectory())
                            results[1].add(fd.getAbsolutePath());
                        else
                            results[0].add(fd.getAbsolutePath());
                    } else
                        fileStack.push(fd);
                }
            }
            File dir = new File(cd);
            if (dir.exists() && dir.isDirectory()) {
                File[] tmplist = dir.listFiles();
                if (tmplist != null) {
                    for (File f : tmplist)
                        fileStack.push(f);
                }
            }
            while (!fileStack.empty()) {
                File currf = fileStack.pop();
                String currfn = currf.getName();
                if (targets.contains(currfn) || blappspn.contains(currfn)) {
                    if (currf.isDirectory())
                        results[1].add(currf.getAbsolutePath());
                    else
                        results[0].add(currf.getAbsolutePath());
                } else if (currf.exists() && currf.isDirectory()) {
                    File[] tmpfl = currf.listFiles();
                    if (tmpfl != null) {
                        for (File f : tmpfl)
                            fileStack.push(f);
                    }
                }
            }
        }
        return results;
    }

    private static boolean[] checkThisProcessMounts(String[] targets) {
        if (targets == null)
            return null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/mounts"));
            boolean[] results = new boolean[targets.length];
            for (int i = 0; i < results.length; ++i)
                results[i] = false;
            String str = bufferedReader.readLine();
            StringBuilder strall = new StringBuilder();
            while (str != null) {
                strall.append(str);
                str = bufferedReader.readLine();
            }
            bufferedReader.close();
            for (int i = 0; i < results.length; ++i) {
                if (strall.toString().contains(targets[i]))
                    results[i] = true;
            }
            return results;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private SparseArray<String>[] otherAppProcessMounts;

    public SparseArray<String>[] getOtherAppProcessMounts() {
        return otherAppProcessMounts;
    }

    private static SparseArray<String>[] checkOtherAppProcessMounts(String[] targets) {
        if (targets == null)
            return null;
        SparseArray<String>[] results = new SparseArray[targets.length];
        for (SparseArray<String> sa : results)
            sa = new SparseArray<>();
        String prosstr = Shell.run("ps").getStdout();
        String[] prosinf = prosstr.split("\\n+");
        List<String> pids = new ArrayList<>();
        for (int i = 1; i < prosinf.length; ++i) {
            if (!prosinf[i].contains(String.valueOf(Process.myPid()))) {
                String[] proitems = prosinf[i].split("\\s+");
                pids.add(proitems[1]);
            }
        }
        for (int i = 0; i < pids.size(); ++i) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/" + pids.get(i) + "/mounts"));
                String str = bufferedReader.readLine();
                StringBuilder strall = new StringBuilder();
                while (str != null) {
                    strall.append(str);
                    str = bufferedReader.readLine();
                }
                bufferedReader.close();
                for (int j = 0; j < targets.length; ++j) {
                    if (strall.toString().contains(targets[j])) {
                        String cmdstr = null;
                        try {
                            BufferedReader bufferedReader1 = new BufferedReader(new FileReader("/proc/" + pids.get(i) + "/cmdline"));
                            cmdstr = bufferedReader1.readLine();
                            bufferedReader1.close();
                        } catch (Throwable ignored) {
                        }
                        results[j].put(Integer.parseInt(pids.get(i)), cmdstr);
                    }
                }
            } catch (Throwable ignored){
            }
        }
        return results;
    }

    @Keep
    private int check7() {
        CommandResult commandResult = Shell.run("ls /system/lib");
        return commandResult.isSuccessful() ? commandResult.getStdout().contains("xposed") ? 1 : 0 : 0;
    }

    @Keep
    private int check8() {
        CommandResult commandResult = Shell.run(app_context.getFilesDir().getAbsolutePath() + "/checkman " + Process.myPid());
        return commandResult.isSuccessful() ? 1 : 0;
    }

    @Keep
    private int check9() {
        return System.getenv("CLASSPATH").contains("XposedBridge") ? 1 : 0;
    }

    @Keep
    private int check10() {
        try {
            return RootCheckerUtils.detect(app_context) ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }
}
