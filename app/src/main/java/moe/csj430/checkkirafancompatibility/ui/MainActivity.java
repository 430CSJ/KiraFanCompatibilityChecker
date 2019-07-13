package moe.csj430.checkkirafancompatibility.ui;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import androidx.annotation.Keep;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import de.robv.android.xposed.XposedBridge;
import moe.csj430.checkkirafancompatibility.R;
import moe.csj430.checkkirafancompatibility.SystemPropertiesProxy;
import moe.csj430.checkkirafancompatibility.UpdateTask;
import moe.csj430.checkkirafancompatibility.util.AlipayDonate;

import static moe.csj430.checkkirafancompatibility.DeviceInfo.*;

/**
 * @author w568w
 */
public class MainActivity extends AppCompatActivity {
    private static String[] CHECK_ITEM = {
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
    private static String[] ROOT_STATUS = {"出错", "未发现Root", "发现Root"};

    private ArrayList<Integer> status = new ArrayList<>();
    private static final int ALL_ALLOW = 0777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CHECK_ITEM[0]=getResources().getString(R.string.load_xposed_toolkit);
        CHECK_ITEM[1]=getResources().getString(R.string.search_feature_dll);
        CHECK_ITEM[2]=getResources().getString(R.string.code_stack_search_invoker);
        CHECK_ITEM[3]=getResources().getString(R.string.check_xposed_install_status);
        CHECK_ITEM[4]=getResources().getString(R.string.judge_system_method_hook);
        CHECK_ITEM[5]=getResources().getString(R.string.check_virtual_xposed);
        CHECK_ITEM[6]=getResources().getString(R.string.search_xposed_runtime_lib);
        CHECK_ITEM[7]=getResources().getString(R.string.kernel_search_xposed_link_lib);
        CHECK_ITEM[8]=getResources().getString(R.string.judge_envir_variable_chara_word);
        ROOT_STATUS[0]=getResources().getString(R.string.get_error);
        ROOT_STATUS[1]=getResources().getString(R.string.item_no_root);
        ROOT_STATUS[2]=getResources().getString(R.string.item_found_root);
        setTitle(R.string.app_name);
        try {
            FutureTask futureTask = new FutureTask<>(new UnpackThread());
            new Thread(futureTask).start();
            futureTask.get();

            futureTask = new FutureTask<>(new CheckThread());
            new Thread(futureTask).start();
            futureTask.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setContentView(R.layout.activity_main);
        final TextView textView = (TextView) findViewById(R.id.a);
        final ListView listView = (ListView) findViewById(R.id.b);
        final Button deviceInfoButton = (Button) findViewById(R.id.device_info);
        final Button androidInfoButton = (Button) findViewById(R.id.android_info);
        final Button xposedRootInfoButton = (Button) findViewById(R.id.xposed_and_root_info);
        final Button googleInfoButton = (Button) findViewById(R.id.google_service_info);
        final Button appDetectInfoButton = (Button) findViewById(R.id.app_detect_info);
        final ListView deviceListView = (ListView) findViewById(R.id.device_detail);
        final ListView androidListView = (ListView) findViewById(R.id.android_detail);
        final ListView appDetectListView = (ListView) findViewById(R.id.app_detect_detail);
        final ListView googleServiceView = (ListView) findViewById(R.id.google_service_detail);
        final TextView about = (TextView) findViewById(R.id.about);
        final TextView donation = (TextView) findViewById(R.id.donation);
        if (about != null) {
            about.getPaint().setAntiAlias(true);
            about.getPaint().setUnderlineText(true);
        }
        if (donation != null) {
            donation.getPaint().setAntiAlias(true);
            donation.getPaint().setUnderlineText(true);
        }

        if (deviceInfoButton != null) {
            deviceInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.device).setMessage(R.string.device_info_notice).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (androidInfoButton != null) {
            androidInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.android_en).setMessage(R.string.android_info_notice).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (googleInfoButton != null) {
            googleInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.google_suite).setMessage(R.string.google_info_notice).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (xposedRootInfoButton != null) {
            xposedRootInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View xposed_root_dialog_view = View.inflate(MainActivity.this, R.layout.xposed_root_dialog, null);
                    ((Button)xposed_root_dialog_view.findViewById(R.id.root_guide)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.root_solution).setView(R.layout.root_solution_dialog).setPositiveButton(R.string.know_it, null).show();
                        }
                    });
                    ((Button)xposed_root_dialog_view.findViewById(R.id.xposed_guide)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.xposed_solution).setView(R.layout.xposed_solution_dialog).setPositiveButton(R.string.know_it, null).show();
                        }
                    });
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.xposed_and_root).setView(xposed_root_dialog_view).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (appDetectInfoButton != null) {
            appDetectInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.app_list).setView(R.layout.app_list_notice_dialog).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (deviceListView != null) {
            deviceListView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return 2;
                }

                @Override
                public Object getItem(int i) {
                    return i;
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                @Override
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    if (i == 0) {
                        textView1.setText(R.string.ram);
                        textView2.setText(getTotalMemory(getApplicationContext()) / 1024 / 1024 + " MB");
                        if (getTotalMemory(getApplicationContext()) < 1536*1024*1024)
                            textView2.setTextColor(Color.RED);
                        else if (getTotalMemory(getApplicationContext()) < 2048*1024*1024)
                            textView2.setTextColor(Color.YELLOW);
                        else
                            textView2.setTextColor(Color.GREEN);
                    } else if (i == 1) {
                        textView1.setText(R.string.instructionset_supported);
                        String instructionset = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            for (int insi = 0; insi < Build.SUPPORTED_ABIS.length; insi++) {
                                if (insi != 0)
                                    instructionset += ", ";
                                instructionset += Build.SUPPORTED_ABIS[insi];
                            }
                        } else {
                            if (!Build.CPU_ABI.equals("")) {
                                instructionset += Build.CPU_ABI;
                                if (!Build.CPU_ABI2.equals(""))
                                    instructionset += ", " + Build.CPU_ABI2;
                            } else {
                                if (!Build.CPU_ABI2.equals(""))
                                    instructionset += Build.CPU_ABI2;
                            }
                        }
                        textView2.setText(instructionset);
                        if (instructionset.indexOf("arm") != -1 && (instructionset.indexOf("v7") != -1 || instructionset.indexOf("v8") != -1))
                            textView2.setTextColor(Color.GREEN);
                        else
                            textView2.setTextColor(Color.YELLOW);
                    }
                    return layout;
                }
            });
        }

        if (androidListView != null) {
            androidListView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return 5;
                }

                @Override
                public Object getItem(int i) {
                    return i;
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    if (i == 0) {
                        textView1.setText(R.string.version);
                        textView2.setText(getVersion()[1]);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
                            textView2.setTextColor(Color.RED);
                        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                            textView2.setTextColor(Color.YELLOW);
                        else
                            textView2.setTextColor(Color.GREEN);
                    }
                    else if (i == 1) {
                        textView1.setText("ro.build.type");
                        String ro_build_type = SystemPropertiesProxy.getString(getApplicationContext(), "ro.build.type");
                        textView2.setText(ro_build_type);
                        if (ro_build_type.equals("eng"))
                            textView2.setTextColor(Color.RED);
                        else
                            textView2.setTextColor(Color.GREEN);
                    }
                    else if (i == 2) {
                        textView1.setText("ro.debuggable");
                        int ro_debuggable = SystemPropertiesProxy.getInt(getApplicationContext(), "ro.debuggable", 0);
                        textView2.setText(String.valueOf(ro_debuggable));
                        if (ro_debuggable == 0)
                            textView2.setTextColor(Color.GREEN);
                        else
                            textView2.setTextColor(Color.RED);
                    }
                    else if (i == 3) {
                        textView1.setText("ro.secure");
                        int ro_secure = SystemPropertiesProxy.getInt(getApplicationContext(), "ro.secure", 0);
                        textView2.setText(String.valueOf(ro_secure));
                        if (ro_secure == 0)
                            textView2.setTextColor(Color.RED);
                        else
                            textView2.setTextColor(Color.GREEN);
                    }
                    else if (i == 4) {
                        textView1.setText(R.string.usb_debug);
                        if (isUsbDebugOn(getApplicationContext())) {
                            textView2.setText(R.string.on);
                            textView2.setTextColor(Color.RED);
                            layout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    try {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        try {
                                            ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings");
                                            Intent intent = new Intent();
                                            intent.setComponent(componentName);
                                            intent.setAction("android.intent.action.View");
                                            startActivity(intent);
                                        } catch (Exception e1) {
                                            try {
                                                Intent intent = new Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS");//部分小米手机采用这种方式跳转
                                                startActivity(intent);
                                            } catch (Exception e2) {
                                                e2.printStackTrace();
                                            }

                                        }
                                    }
                                }
                            });
                        } else {
                            textView2.setText(R.string.off);
                            textView2.setTextColor(Color.GREEN);
                        }
                    }
                    return layout;
                }
            });
        }

        if (listView != null) {
            listView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return CHECK_ITEM.length + 1;
                }

                @Override
                public Object getItem(int position) {
                    return position;
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    int itemStatus = status.get(position);
                    boolean pass = itemStatus == 0;
                    if (position == CHECK_ITEM.length) {
                        textView1.setText(R.string.root_check);
                        textView2.setText(ROOT_STATUS[itemStatus + 1]);
                    } else {
                        textView1.setText(CHECK_ITEM[position]);
                        textView2.setText((pass ? getString(R.string.item_no_xposed) : getString(R.string.item_found_xposed)));
                    }
                    textView2.setTextColor(pass ? Color.GREEN : Color.RED);
                    return layout;
                }
            });
        }

        if (googleServiceView != null) {
            googleServiceView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return 1;
                }

                @Override
                public Object getItem(int i) {
                    return i;
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                final int gsrecode=isGoogleServiceAvailable(MainActivity.this);
                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    if (i == 0) {
                        textView1.setText(R.string.google_service);
                        switch (gsrecode) {
                            case ConnectionResult.SUCCESS:
                                textView2.setText(R.string.available);
                                textView2.setTextColor(Color.GREEN);
                                break;
                            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                                textView2.setText(R.string.update_required);
                                textView2.setTextColor(Color.YELLOW);
                                break;
                            case ConnectionResult.SERVICE_UPDATING:
                                textView2.setText(R.string.updating);
                                textView2.setTextColor(Color.YELLOW);
                                break;
                            default:
                                textView2.setText(R.string.not_available);
                                textView2.setTextColor(Color.YELLOW);
                        }
                    }
                    layout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast toast = Toast.makeText(MainActivity.this, getResources().getString(R.string.return_value) + ": " + gsrecode, Toast.LENGTH_SHORT);
                            toast.show();
                            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
                            if (googleApiAvailability.isUserResolvableError(gsrecode))
                                googleApiAvailability.getErrorDialog(MainActivity.this, gsrecode, 8996).show();
                        }
                    });
                    return layout;
                }
            });
        }

        final List<ResolveInfo> blacklistapps = getBlackListApps(getApplicationContext());
        if (appDetectListView != null) {
            appDetectListView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return blacklistapps.size() > 0 ? blacklistapps.size() : 1;
                }

                @Override
                public Object getItem(int i) {
                    return i;
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    if (blacklistapps.size() > 0) {
                        textView1.setText(blacklistapps.get(i).activityInfo.loadLabel(getPackageManager()));
                        textView1.setTextColor(Color.RED);
                        textView2.setText(blacklistapps.get(i).activityInfo.packageName);
                        textView2.setTextColor(Color.RED);
                    } else {
                        textView1.setText(R.string.black_list_apps_not_found);
                        textView1.setTextColor(Color.GREEN);
                    }
                    return layout;
                }
            });
        }

        int checkCode = 0;
        for (int i = 0; i < CHECK_ITEM.length; ++i) {
            checkCode += status.get(i);
        }
        if (textView != null) {
            if (checkCode > 0) {

                textView.setTextColor(Color.RED);

                textView.setText(String.format(getString(R.string.found_xposed), checkCode, CHECK_ITEM.length));
            } else {
                textView.setTextColor(Color.GREEN);
                textView.setText(R.string.no_xposed);
            }
        }

        new UpdateTask(MainActivity.this, true, false).update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SupportMenuInflater inflater = new SupportMenuInflater(this);
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.id_menu_explanation:
                View explanation_dialog_view = View.inflate(MainActivity.this, R.layout.explanation_dialog, null);
                ((TextView)explanation_dialog_view.findViewById(R.id.green_words)).setTextColor(Color.GREEN);
                ((TextView)explanation_dialog_view.findViewById(R.id.yellow_words)).setTextColor(Color.YELLOW);
                ((TextView)explanation_dialog_view.findViewById(R.id.red_words)).setTextColor(Color.RED);
                new AlertDialog.Builder(MainActivity.this).setTitle(R.string.menu_explanation).setView(explanation_dialog_view).setPositiveButton(R.string.know_it, null).show();
                break;
            case R.id.id_menu_check_update:
                new UpdateTask(MainActivity.this, true, true).update();
                break;
            case R.id.id_menu_about:
                View about_dialog_view = View.inflate(MainActivity.this, R.layout.about_dialog, null);
                TextView about_dialog_textviews[] = new TextView[4];
                about_dialog_textviews[0] = (TextView)about_dialog_view.findViewById(R.id.about_xposed_checker);
                about_dialog_textviews[1] = (TextView)about_dialog_view.findViewById(R.id.support_origin_author);
                about_dialog_textviews[2] = (TextView)about_dialog_view.findViewById(R.id.about_kfcc);
                about_dialog_textviews[3] = (TextView)about_dialog_view.findViewById(R.id.view_update_log);
                for (int tvi = 0; tvi < about_dialog_textviews.length; tvi++) {
                    if (about_dialog_textviews[tvi] != null) {
                        about_dialog_textviews[tvi].getPaint().setAntiAlias(true);
                        about_dialog_textviews[tvi].getPaint().setUnderlineText(true);
                        switch (tvi) {
                            case 0:
                                about_dialog_textviews[tvi].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        about(view);
                                    }
                                });
                                break;
                            case 1:
                                about_dialog_textviews[tvi].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        donation(view);
                                    }
                                });
                                break;
                            case 2:
                                about_dialog_textviews[tvi].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        about_kfcc(view);
                                    }
                                });
                                break;
                            case 3:
                                about_dialog_textviews[tvi].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/430CSJ/KiraFanCompatibilityChecker/releases")));
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    }
                }
                new AlertDialog.Builder(MainActivity.this).setTitle(R.string.about___).setView(about_dialog_view).setPositiveButton(R.string.know_it, null).show();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private class CheckThread implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            status.clear();
            for (int i = 0; i <= CHECK_ITEM.length; i++) {
                Method method = MainActivity.class.getDeclaredMethod("check" + (i + 1));
                method.setAccessible(true);
                try {
                    status.add((int) method.invoke(MainActivity.this));
                } catch (Throwable e) {
                    status.add(0);
                }
            }
            return null;
        }
    }

    public void about(View view) {
        new AlertDialog.Builder(MainActivity.this).setTitle(getResources().getString(R.string.about_xposed_checker) + " " + getResources().getString(R.string.xposed_checker_version))
                .setMessage(String.format(getString(R.string.about), Process.myPid()))
                .setPositiveButton(R.string.i_know_it, null)
                .show();
    }

    public void donation(View view) {
        new AlertDialog.Builder(MainActivity.this).setTitle(R.string.about_donate_w568w)
                .setMessage(getString(R.string.donation))
                .setPositiveButton(R.string.alipay_donate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!AlipayDonate.startAlipayClient(MainActivity.this, "a6x06490c5kpcbnsr84hr23")) {
                            Toast.makeText(MainActivity.this, R.string.seem_no_alipay_0, Toast.LENGTH_LONG).show();
                        }

                    }
                })
                .show();
    }

    public void about_kfcc(View view) {
        String app_ver = "";
        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            app_ver = pInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(MainActivity.this).setTitle(getResources().getString(R.string.about_kfcc) + " " + app_ver).setMessage(R.string.about_kfcc_message).setPositiveButton(R.string.do_not_say_any_more, null).setNegativeButton(R.string.view_how_water_the_project, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/430CSJ/KiraFanCompatibilityChecker")));
            }
        }).setNeutralButton(R.string.alipay_pay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!AlipayDonate.startAlipayClient(MainActivity.this, "tsx057945er7i5k46ajim06")) {
                    Toast.makeText(MainActivity.this, R.string.seem_no_alipay_1, Toast.LENGTH_LONG).show();
                }
            }
        }).show();
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
            List<PackageInfo> list = getPackageManager().getInstalledPackages(0);
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

    @Keep
    private int check7() {
        CommandResult commandResult = Shell.run("ls /system/lib");
        return commandResult.isSuccessful() ? commandResult.getStdout().contains("xposed") ? 1 : 0 : 0;
    }

    @Keep
    private int check8() {
        CommandResult commandResult = Shell.run(getFilesDir().getAbsolutePath() + "/checkman " + Process.myPid());
        return commandResult.isSuccessful() ? 1 : 0;
    }

    @Keep
    private int check9() {
        return System.getenv("CLASSPATH").contains("XposedBridge") ? 1 : 0;
    }

    @Keep
    private int check10() {
        try {
            return RootCheckerUtils.detect(this) ? 1 : 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }
    }

    private class UnpackThread implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            if (!new File(getFilesDir().getAbsolutePath() + "/checkman").exists()) {
                InputStream inputStream = getAssets().open("checkman");
                OutputStream outputStream = openFileOutput("checkman", MODE_PRIVATE);
                int bit;
                while ((bit = inputStream.read()) != -1) {
                    outputStream.write(bit);
                }
            }
            setFilePermissions(getFilesDir(), ALL_ALLOW, -1, -1);
            setFilePermissions(getFilesDir().getAbsolutePath() + "/checkman", ALL_ALLOW, -1, -1);
            return null;
        }

        /**
         * 修改文件权限
         * setFilePermissions(file, 0777, -1, -1);
         */
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
}
