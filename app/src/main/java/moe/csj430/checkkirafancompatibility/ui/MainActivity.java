package moe.csj430.checkkirafancompatibility.ui;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import androidx.annotation.Keep;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    private static final String[] CHECK_PROP_ITEM = {
            "persist.sys.usb.config",
            "ro.build.type",
            "ro.debuggable",
            "ro.secure"
    };

    private static Long totalMem;
    private static StringBuilder instructionset;
    private static int SYSTEM_SDK_INT;
    private static String SYSTEM_VER;
    private static Boolean isUSBDebugOn;
    private static int gsrecode;
    private ArrayList<Object> device_status = new ArrayList<>();
    private int[] deviceColorArr;
    private ArrayList<Object> prop_status = new ArrayList<>();
    private int[] systemColorArr;
    private ArrayList<Integer> status = new ArrayList<>();
    private int[] rootXposedColorArr;
    private List<ResolveInfo> blacklistapps;
    private int[] googleColorArr;
    private static final int ALL_ALLOW = 0777;

    private ImageView statusImg;
    private ListView listView;
    private RelativeLayout cardViewArrowRL;
    private ImageView cardViewArrow;
    private ImageView deviceStatusImg;
    private ListView deviceListView;
    private RelativeLayout deviceCardViewArrowRL;
    private ImageView deviceCardViewArrow;
    private ImageView systemStatusImg;
    private ListView androidListView;
    private RelativeLayout systemCardViewArrowRL;
    private ImageView systemCardViewArrow;
    private ImageView appStatusImg;
    private ListView appDetectListView;
    private RelativeLayout appCardViewArrowRL;
    private ImageView appCardViewArrow;
    private ImageView googleStatusImg;
    private ListView googleServiceView;
    private RelativeLayout googleCardViewArrowRL;
    private ImageView googleCardViewArrow;

    private static boolean LVUpdated = false;
    private static boolean deviceLVUpdated = false;
    private static boolean androidLVUpdated = false;
    private static boolean appDetectLVUpdated = false;
    private static boolean googleServiceLVUpdated = false;

    private LayoutAnimationController viewAniCtrl0;

    View.OnClickListener DS_OCL = view -> {
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
    };

    View.OnClickListener GS_OCL = view -> {
        Toast toast = Toast.makeText(MainActivity.this, getResources().getString(R.string.return_value) + ": " + gsrecode, Toast.LENGTH_SHORT);
        toast.show();
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        if (googleApiAvailability.isUserResolvableError(gsrecode))
            googleApiAvailability.getErrorDialog(MainActivity.this, gsrecode, 8996).show();
    };

    @Override
    protected void onResume() {
        super.onResume();
        refreshChange();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        statusImg = (ImageView) findViewById(R.id.root_xposed_status_img);
        listView = (ListView) findViewById(R.id.b);
        cardViewArrowRL = (RelativeLayout) findViewById(R.id.root_xposed_info_arrow_rl);
        cardViewArrow = (ImageView) findViewById(R.id.root_xposed_info_arrow);
        final Button deviceInfoButton = (Button) findViewById(R.id.device_info);
        final Button androidInfoButton = (Button) findViewById(R.id.android_info);
        final Button xposedRootInfoButton = (Button) findViewById(R.id.xposed_and_root_info);
        final Button googleInfoButton = (Button) findViewById(R.id.google_service_info);
        final Button appDetectInfoButton = (Button) findViewById(R.id.app_detect_info);
        deviceStatusImg = (ImageView) findViewById(R.id.device_status_img);
        deviceListView = (ListView) findViewById(R.id.device_detail);
        deviceCardViewArrowRL = (RelativeLayout) findViewById(R.id.device_info_arrow_rl);
        deviceCardViewArrow = (ImageView) findViewById(R.id.device_info_arrow);
        systemStatusImg = (ImageView) findViewById(R.id.system_status_img);
        androidListView = (ListView) findViewById(R.id.android_detail);
        systemCardViewArrowRL = (RelativeLayout) findViewById(R.id.system_info_arrow_rl);
        systemCardViewArrow = (ImageView) findViewById(R.id.system_info_arrow);
        appStatusImg = (ImageView) findViewById(R.id.app_status_img);
        appDetectListView = (ListView) findViewById(R.id.app_detect_detail);
        appCardViewArrowRL = (RelativeLayout) findViewById(R.id.app_info_arrow_rl);
        appCardViewArrow = (ImageView) findViewById(R.id.app_info_arrow);
        googleStatusImg = (ImageView) findViewById(R.id.google_status_img);
        googleServiceView = (ListView) findViewById(R.id.google_service_detail);
        googleCardViewArrowRL = (RelativeLayout) findViewById(R.id.google_info_arrow_rl);
        googleCardViewArrow = (ImageView) findViewById(R.id.google_info_arrow);

        Animation viewAnimation0 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_item_0);
        viewAniCtrl0 = new LayoutAnimationController(viewAnimation0);
        viewAniCtrl0.setDelay(0.5f);

        if (deviceInfoButton != null) {
            deviceInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.device).setMessage(R.string.device_info_notice).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (androidInfoButton != null) {
            androidInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.android_en).setMessage(R.string.android_info_notice).setNeutralButton(R.string.system_prop_detail, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.system_prop).setMessage(R.string.system_prop_notice).setNeutralButton(R.string.how_to_modify_system_prop, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.method_to_modify_system_prop).setView(R.layout.method_to_modify_system_prop_dialog).setPositiveButton(R.string.know_it, null).show();
                                }
                            }).setPositiveButton(R.string.know_it, null).show();
                        }
                    }).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (googleInfoButton != null) {
            googleInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.google_suite).setMessage(R.string.google_info_notice).setPositiveButton(R.string.know_it, null).show();
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
                            new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.root_solution).setView(R.layout.root_solution_dialog).setPositiveButton(R.string.know_it, null).show();
                        }
                    });
                    ((Button)xposed_root_dialog_view.findViewById(R.id.xposed_guide)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.xposed_solution).setView(R.layout.xposed_solution_dialog).setPositiveButton(R.string.know_it, null).show();
                        }
                    });
                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.xposed_and_root).setView(xposed_root_dialog_view).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        if (appDetectInfoButton != null) {
            appDetectInfoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.app_list).setView(R.layout.app_list_notice_dialog).setPositiveButton(R.string.know_it, null).show();
                }
            });
        }
        totalMem = getTotalMemory(getApplicationContext());
        deviceColorArr = new int[2];
        device_status.clear();
        device_status.add(totalMem);
        if (totalMem < 1536*1024*1024)
            deviceColorArr[0] = Color.RED;
        else if (getTotalMemory(getApplicationContext()) < (long)2048*1024*1024)
            deviceColorArr[0] = Color.YELLOW;
        else
            deviceColorArr[0] = Color.GREEN;
        instructionset = new StringBuilder("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (int insi = 0; insi < Build.SUPPORTED_ABIS.length; insi++) {
                if (insi != 0)
                    instructionset.append(", ");
                instructionset.append(Build.SUPPORTED_ABIS[insi]);
            }
        } else {
            if (!Build.CPU_ABI.equals("")) {
                instructionset.append(Build.CPU_ABI);
                if (!Build.CPU_ABI2.equals(""))
                    instructionset.append(", ").append(Build.CPU_ABI2);
            } else {
                if (!Build.CPU_ABI2.equals(""))
                    instructionset.append(Build.CPU_ABI2);
            }
        }
        device_status.add(instructionset);
        if (instructionset.indexOf("arm") != -1 && (instructionset.indexOf("v7") != -1 || instructionset.indexOf("v8") != -1))
            deviceColorArr[1] = Color.GREEN;
        else
            deviceColorArr[1] = Color.YELLOW;
        int device_img_color = Color.GREEN;
        for (int color:deviceColorArr) {
            if (color == Color.RED) {
                device_img_color = Color.RED;
                break;
            } else if (color == Color.YELLOW)
                device_img_color = Color.YELLOW;
        }
        switch (device_img_color) {
            case Color.GREEN:
                deviceStatusImg.setImageResource(R.drawable.baseline_check_circle_outline_24);
                break;
            case Color.YELLOW:
                deviceStatusImg.setImageResource(R.drawable.baseline_help_outline_24);
                break;
            case Color.RED:
                deviceStatusImg.setImageResource(R.drawable.baseline_highlight_off_24);
                break;
            default:
                break;
        }
        deviceStatusImg.setColorFilter(device_img_color);
        if (device_img_color != Color.GREEN) {
            showDeviceLV(deviceColorArr, viewAniCtrl0);
            deviceCardViewArrow.setRotation(180);
        } else
            deviceListView.setVisibility(View.GONE);
        deviceCardViewArrowRL.setOnClickListener(view -> {
            if (deviceListView.getVisibility() == View.VISIBLE) {
                deviceListView.setVisibility(View.GONE);
                deviceCardViewArrow.setRotation(0);
            } else {
                deviceListView.setVisibility(View.VISIBLE);
                showDeviceLV(deviceColorArr, viewAniCtrl0);
                deviceCardViewArrow.setRotation(180);
            }
        });

        new UpdateTask(MainActivity.this, true, false).update();
    }

    private void showDeviceLV(int[] colorArray, LayoutAnimationController viewAniCtrl) {
        if (deviceListView != null) {
            deviceListView.setVisibility(View.VISIBLE);
                if (!deviceLVUpdated && viewAniCtrl != null)
                    deviceListView.setLayoutAnimation(viewAniCtrl);
                deviceListView.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return 2;
                    }

                    @Override
                    public Object getItem(int i) {
                        return device_status.get(i);
                    }

                    @Override
                    public long getItemId(int i) {
                        return i;
                    }

                    @Override
                    public View getView(int i, View view, ViewGroup viewGroup) {
                        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items_not_selectable, null);
                        TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                        TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                        if (i == 0) {
                            textView1.setText(R.string.ram);
                            textView2.setText((long)getItem(i) / 1024 / 1024 + " MB");
                        } else if (i == 1) {
                            textView1.setText(R.string.instructionset_supported);
                            textView2.setText((StringBuilder)getItem(i));
                        }
                        textView2.setTextColor(colorArray[i]);
                        return layout;
                    }
                });
                deviceLVUpdated = true;
        }
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
                new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.menu_explanation).setView(explanation_dialog_view).setPositiveButton(R.string.know_it, null).show();
                break;
            case R.id.id_menu_check_update:
                new UpdateTask(MainActivity.this, true, true).update();
                break;
            case R.id.id_menu_about:
                View about_dialog_view = View.inflate(MainActivity.this, R.layout.about_dialog, null);
                ((MaterialButton)about_dialog_view.findViewById(R.id.special_thanks_bt)).setOnClickListener(view -> {
                    new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.special_thanks).setMessage(R.string.about_kfcc_origin).setNeutralButton(R.string.support_origin_author, (dialogInterface, i) -> {
                        new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.about_donate_w568w)
                                .setMessage(getString(R.string.donation))
                                .setPositiveButton(R.string.alipay_donate, (dialogInterface1, i1) -> {
                                        if (!AlipayDonate.startAlipayClient(MainActivity.this, "a6x06490c5kpcbnsr84hr23")) {
                                            Toast.makeText(MainActivity.this, R.string.seem_no_alipay_0, Toast.LENGTH_LONG).show();
                                        }
                                }).show();
                    }).setNegativeButton(R.string.know_it, null).setPositiveButton(R.string.about_xposed_checker, (dialogInterface, i) -> {
                        new MaterialAlertDialogBuilder(MainActivity.this).setTitle(getResources().getString(R.string.about_xposed_checker) + " " + getResources().getString(R.string.xposed_checker_version))
                                .setMessage(String.format(getString(R.string.about), Process.myPid()))
                                .setPositiveButton(R.string.i_know_it, null)
                                .setNeutralButton(R.string.project_homepage, ((dialogInterface_p, i_p) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/w568w/XposedChecker")))))
                                .show();
                    }).show();
                });
                TextView about_dialog_textview = (TextView)about_dialog_view.findViewById(R.id.view_update_log);
                about_dialog_textview.setOnClickListener(view -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/430CSJ/KiraFanCompatibilityChecker/releases"))));
                String app_ver = "";
                try {
                    PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
                    app_ver = pInfo.versionName;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new MaterialAlertDialogBuilder(MainActivity.this).setTitle(getResources().getString(R.string.about_kfcc) + " " + app_ver).setView(about_dialog_view).setNeutralButton(R.string.alipay_pay, (dialogInterface, i) -> {
                    if (!AlipayDonate.startAlipayClient(MainActivity.this, "tsx057945er7i5k46ajim06")) {
                        Toast.makeText(MainActivity.this, R.string.seem_no_alipay_1, Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton(R.string.view_how_water_the_project, (dialogInterface, i) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/430CSJ/KiraFanCompatibilityChecker")))).setPositiveButton(R.string.know_it, null).show();
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

    private int[] checkSystem() {
        prop_status.clear();
        int[] colorArray = new int[CHECK_PROP_ITEM.length + 2];
        for (int i = 0; i < colorArray.length; i++)
            colorArray[i] = Color.GREEN;
        int color_i = 0;
        SYSTEM_SDK_INT = Build.VERSION.SDK_INT;
        SYSTEM_VER = getVersion()[1];
        if (SYSTEM_SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            colorArray[color_i] = Color.RED;
        else if (SYSTEM_SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            colorArray[color_i] = Color.YELLOW;
        color_i++;
        int prop_i = 0;
        for (String check_prop:CHECK_PROP_ITEM) {
            if (prop_i < 2)
                prop_status.add(SystemPropertiesProxy.getString(getApplicationContext(), check_prop));
            else
                prop_status.add(SystemPropertiesProxy.getInt(getApplicationContext(), check_prop, 0));
            switch (check_prop) {
                case "persist.sys.usb.config":
                    if (((String)prop_status.get(prop_i)).equals("adb"))
                        colorArray[color_i] = Color.RED;
                    break;
                case "ro.build.type":
                    if (((String)prop_status.get(prop_i)).equals("eng"))
                        colorArray[color_i] = Color.RED;
                    break;
                case "ro.debuggable":
                    if ((Integer)prop_status.get(prop_i) != 0)
                        colorArray[color_i] = Color.RED;
                    break;
                case "ro.secure":
                    if ((Integer)prop_status.get(prop_i) == 0)
                        colorArray[color_i] = Color.RED;
                    break;
                default:
                    break;
            }
            color_i++;
            prop_i++;
        }
        isUSBDebugOn = isUsbDebugOn(getApplicationContext());
        if (isUSBDebugOn)
            colorArray[color_i] = Color.RED;
        return colorArray;
    }

    private void showAndroidLV(int[] colorArray, LayoutAnimationController viewAniCtrl) {
        if (androidListView != null) {
            androidListView.setVisibility(View.VISIBLE);
            if (viewAniCtrl != null)
                androidListView.setLayoutAnimation(viewAniCtrl);
            androidListView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return colorArray.length;
                }

                @Override
                public Object getItem(int i) {
                    if (i == 0)
                        return SYSTEM_VER;
                    if (i == getCount() - 1)
                        return isUSBDebugOn;
                    if (i <= CHECK_PROP_ITEM.length && i <= prop_status.size())
                        return prop_status.get(i - 1);
                    return null;
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout;
                    if (i == getCount() - 1 && isUSBDebugOn)
                        layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                    else
                        layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items_not_selectable, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    textView2.setTextColor(colorArray[i]);
                    if (i == 0) {
                        textView1.setText(R.string.version);
                        textView2.setText(SYSTEM_VER);
                    } else if (i == getCount() - 1) {
                        textView1.setText(R.string.usb_debug);
                        if (isUSBDebugOn) {
                            textView2.setText(R.string.on);
                            layout.setOnClickListener(DS_OCL);
                        } else
                            textView2.setText(R.string.off);
                    } else {
                        textView1.setText(CHECK_PROP_ITEM[i - 1]);
                        String prop_val_str;
                        if (getItem(i) instanceof String)
                            prop_val_str = (String)getItem(i);
                        else
                            prop_val_str = String.valueOf(getItem(i));
                        textView2.setText(prop_val_str);
                    }
                    return layout;
                }
            });
        }
    }

    private void refreshSystemCV() {
        androidLVUpdated = false;
        systemColorArr = checkSystem();
        int status_img_color = Color.GREEN;
        for (int colorint:systemColorArr) {
            if (colorint == Color.RED) {
                status_img_color = Color.RED;
                break;
            }
            if (colorint == Color.YELLOW)
                status_img_color = Color.YELLOW;
        }
        switch (status_img_color) {
            case Color.GREEN:
                systemStatusImg.setImageResource(R.drawable.baseline_check_circle_outline_24);
                break;
            case Color.YELLOW:
                systemStatusImg.setImageResource(R.drawable.baseline_help_outline_24);
                break;
            case Color.RED:
                systemStatusImg.setImageResource(R.drawable.baseline_highlight_off_24);
                break;
            default:
                break;
        }
        systemStatusImg.setColorFilter(status_img_color);
        if (status_img_color != Color.GREEN) {
            showAndroidLV(systemColorArr, viewAniCtrl0);
            androidLVUpdated = true;
            systemCardViewArrow.setRotation(180);
        } else {
            androidListView.setVisibility(View.GONE);
            systemCardViewArrow.setRotation(0);
        }
    }

    private int[] checkRootXposed() {
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
        int[] colorArray = new int[CHECK_ITEM.length + 1];
        for (int position = 0; position < CHECK_ITEM.length + 1; position++) {
            int itemStatus = status.get(position);
            colorArray[position] = itemStatus == 0 ? Color.GREEN : Color.RED;
        }
        return colorArray;
    }

    private void showLV(int[] colorArray, LayoutAnimationController viewAniCtrl) {
        if (listView != null) {
            listView.setVisibility(View.VISIBLE);
            if (viewAniCtrl != null)
                listView.setLayoutAnimation(viewAniCtrl);
            listView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return CHECK_ITEM.length + 1;
                }

                @Override
                public Object getItem(int i) {
                    return status.get(i);
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items_not_selectable, null);
                    TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                    TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                    int itemStatus = (int)getItem(i);
                    if (i == CHECK_ITEM.length) {
                        textView1.setText(R.string.root_check);
                        textView2.setText(ROOT_STATUS[itemStatus + 1]);
                    } else {
                        textView1.setText(CHECK_ITEM[i]);
                        textView2.setText((itemStatus == 0 ? getString(R.string.item_no_xposed) : getString(R.string.item_found_xposed)));
                    }
                    textView2.setTextColor(colorArray[i]);
                    return layout;
                }
            });
        }
    }

    private void refreshCV() {
        LVUpdated = false;
        rootXposedColorArr = checkRootXposed();
        int status_img_color = Color.GREEN;
        for (int colorint:rootXposedColorArr) {
            if (colorint == Color.RED) {
                status_img_color = Color.RED;
                break;
            }
            if (colorint == Color.YELLOW)
                status_img_color = Color.YELLOW;
        }
        switch (status_img_color) {
            case Color.GREEN:
                statusImg.setImageResource(R.drawable.baseline_check_circle_outline_24);
                break;
            case Color.YELLOW:
                statusImg.setImageResource(R.drawable.baseline_help_outline_24);
                break;
            case Color.RED:
                statusImg.setImageResource(R.drawable.baseline_highlight_off_24);
                break;
            default:
                break;
        }
        statusImg.setColorFilter(status_img_color);
        if (status_img_color != Color.GREEN) {
            showLV(rootXposedColorArr, viewAniCtrl0);
            LVUpdated = true;
            cardViewArrow.setRotation(180);
        } else {
            listView.setVisibility(View.GONE);
            cardViewArrow.setRotation(0);
        }
    }

    private int[] checkGoogle() {
        gsrecode=isGoogleServiceAvailable(MainActivity.this);
        int[] colorArray = new int[1];
        switch (gsrecode) {
            case ConnectionResult.SUCCESS:
                colorArray[0] = Color.GREEN;
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                colorArray[0] = Color.YELLOW;
                break;
            case ConnectionResult.SERVICE_UPDATING:
                colorArray[0] = Color.YELLOW;
                break;
            default:
                colorArray[0] = Color.YELLOW;
                break;
        }
        return colorArray;
    }

    private void showGoogleLV(int[] colorArray, LayoutAnimationController viewAniCtrl) {
        if (googleServiceView != null) {
            googleServiceView.setVisibility(View.VISIBLE);
            if (viewAniCtrl != null)
                googleServiceView.setLayoutAnimation(viewAniCtrl);
            googleServiceView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return 1;
                }

                @Override
                public Object getItem(int i) {
                    return gsrecode;
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
                        textView1.setText(R.string.google_service);
                        switch (gsrecode) {
                            case ConnectionResult.SUCCESS:
                                textView2.setText(R.string.available);
                                break;
                            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                                textView2.setText(R.string.update_required);
                                break;
                            case ConnectionResult.SERVICE_UPDATING:
                                textView2.setText(R.string.updating);
                                break;
                            default:
                                textView2.setText(R.string.not_available);
                                break;
                        }
                        layout.setOnClickListener(GS_OCL);
                    }
                    textView2.setTextColor(colorArray[i]);
                    return layout;
                }
            });
        }
    }

    private void refreshGoogleCV() {
        googleServiceLVUpdated = false;
        googleColorArr = checkGoogle();
        int status_img_color = Color.GREEN;
        for (int colorint:googleColorArr) {
            if (colorint == Color.RED) {
                status_img_color = Color.RED;
                break;
            }
            if (colorint == Color.YELLOW)
                status_img_color = Color.YELLOW;
        }
        switch (status_img_color) {
            case Color.GREEN:
                googleStatusImg.setImageResource(R.drawable.baseline_check_circle_outline_24);
                break;
            case Color.YELLOW:
                googleStatusImg.setImageResource(R.drawable.baseline_help_outline_24);
                break;
            case Color.RED:
                googleStatusImg.setImageResource(R.drawable.baseline_highlight_off_24);
                break;
            default:
                break;
        }
        googleStatusImg.setColorFilter(status_img_color);
        if (status_img_color != Color.GREEN) {
            showGoogleLV(googleColorArr, viewAniCtrl0);
            googleServiceLVUpdated = true;
            googleCardViewArrow.setRotation(180);
        } else {
            googleServiceView.setVisibility(View.GONE);
            googleCardViewArrow.setRotation(0);
        }
    }

    private void checkAppList() {
        blacklistapps = getBlackListApps(getApplicationContext());
    }

    private void showAppListLV(LayoutAnimationController viewAniCtrl) {
        if (appDetectListView != null) {
            appDetectListView.setVisibility(View.VISIBLE);
            if (viewAniCtrl != null)
                appDetectListView.setLayoutAnimation(viewAniCtrl);
            appDetectListView.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return blacklistapps.size() > 0 ? blacklistapps.size() : 1;
                }

                @Override
                public Object getItem(int i) {
                    return blacklistapps.size() > 0 ? blacklistapps.get(i) : null;
                }

                @Override
                public long getItemId(int i) {
                    return i;
                }

                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    RelativeLayout layout;
                    if (blacklistapps.size() > 0) {
                        layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items, null);
                        TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                        TextView textView2 = (TextView) layout.findViewById(R.id.check_result);
                        textView1.setText(blacklistapps.get(i).activityInfo.loadLabel(getPackageManager()));
                        textView1.setTextColor(Color.RED);
                        String target_package_name = blacklistapps.get(i).activityInfo.packageName;
                        textView2.setText(target_package_name);
                        textView2.setTextColor(Color.RED);
                        layout.setOnClickListener(view1 -> {
                            Intent uninstall_intent = new Intent(Intent.ACTION_DELETE);
                            uninstall_intent.setData(Uri.parse("package:" + target_package_name));
                            uninstall_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (target_package_name.equals("com.topjohnwu.magisk"))
                                new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.magisk_manager_detected).setMessage(R.string.magisk_manager_advice).setNeutralButton(R.string.uninstall_directly, (dialogInterface, i1) -> startActivity(uninstall_intent)).setNegativeButton(R.string.let_it_go_0, null).setPositiveButton(R.string.open_magisk_manager, (dialogInterface, i1) -> {
                                    PackageManager packageManager = getApplicationContext().getPackageManager();
                                    Intent open_magisk_manager_intent = packageManager.getLaunchIntentForPackage(target_package_name);
                                    startActivity(open_magisk_manager_intent);
                                }).show();
                            else
                                startActivity(uninstall_intent);
                        });
                    } else {
                        layout = (RelativeLayout) LayoutInflater.from(MainActivity.this).inflate(R.layout.items_not_selectable, null);
                        TextView textView1 = (TextView) layout.findViewById(R.id.check_item);
                        textView1.setText(R.string.black_list_apps_not_found);
                        textView1.setTextColor(Color.GREEN);
                    }
                    return layout;
                }
            });
        }
    }

    private void refreshAppListCV() {
        appDetectLVUpdated = false;
        checkAppList();
        int status_img_color = Color.GREEN;
        if (blacklistapps.size() > 0)
            status_img_color = Color.RED;
        switch (status_img_color) {
            case Color.GREEN:
                appStatusImg.setImageResource(R.drawable.baseline_check_circle_outline_24);
                break;
            case Color.RED:
                appStatusImg.setImageResource(R.drawable.baseline_highlight_off_24);
                break;
            default:
                break;
        }
        appStatusImg.setColorFilter(status_img_color);
        if (status_img_color != Color.GREEN) {
            showAppListLV(viewAniCtrl0);
            appDetectLVUpdated = true;
            appCardViewArrow.setRotation(180);
        } else {
            appDetectListView.setVisibility(View.GONE);
            appCardViewArrow.setRotation(0);
        }
    }

    private void refreshChange() {
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

        refreshSystemCV();
        Button system_info_refresh = (Button) findViewById(R.id.system_info_refresh);
        system_info_refresh.setOnClickListener(view -> refreshSystemCV());
        systemCardViewArrowRL.setOnClickListener(view -> {
            if (androidListView.getVisibility() == View.VISIBLE) {
                androidListView.setVisibility(View.GONE);
                systemCardViewArrow.setRotation(0);
            } else {
                androidListView.setVisibility(View.VISIBLE);
                if (!androidLVUpdated) {
                    showAndroidLV(systemColorArr, viewAniCtrl0);
                    androidLVUpdated = true;
                }
                systemCardViewArrow.setRotation(180);
            }
        });

        refreshCV();
        Button root_xposed_refresh = (Button) findViewById(R.id.root_xposed_info_refresh);
        root_xposed_refresh.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, R.string.xposed_root_refresh_notice, Toast.LENGTH_SHORT).show();
            refreshCV();
        });
        cardViewArrowRL.setOnClickListener(view -> {
            if (listView.getVisibility() == View.VISIBLE) {
                listView.setVisibility(View.GONE);
                cardViewArrow.setRotation(0);;
            } else {
                listView.setVisibility(View.VISIBLE);
                if (!LVUpdated) {
                    showLV(rootXposedColorArr, viewAniCtrl0);
                    LVUpdated = true;
                }
                cardViewArrow.setRotation(180);
            }
        });

        refreshGoogleCV();
        Button google_refresh = (Button) findViewById(R.id.google_info_refresh);
        google_refresh.setOnClickListener(view -> refreshGoogleCV());
        googleCardViewArrowRL.setOnClickListener(view -> {
            if (googleServiceView.getVisibility() == View.VISIBLE) {
                googleServiceView.setVisibility(View.GONE);
                googleCardViewArrow.setRotation(0);
            } else {
                googleServiceView.setVisibility(View.VISIBLE);
                if (!googleServiceLVUpdated) {
                    showGoogleLV(googleColorArr, viewAniCtrl0);
                    googleServiceLVUpdated = true;
                }
                googleCardViewArrow.setRotation(180);
            }
        });

        refreshAppListCV();
        Button app_list_refresh = (Button) findViewById(R.id.app_info_refresh);
        app_list_refresh.setOnClickListener(view -> refreshAppListCV());
        appCardViewArrowRL.setOnClickListener(view -> {
            if (appDetectListView.getVisibility() == View.VISIBLE) {
                appDetectListView.setVisibility(View.GONE);
                appCardViewArrow.setRotation(0);
            } else {
                appDetectListView.setVisibility(View.VISIBLE);
                if (!appDetectLVUpdated) {
                    showAppListLV(viewAniCtrl0);
                    appDetectLVUpdated = true;
                }
                appCardViewArrow.setRotation(180);
            }
        });
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
