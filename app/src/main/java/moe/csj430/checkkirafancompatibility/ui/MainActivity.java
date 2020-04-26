package moe.csj430.checkkirafancompatibility.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import moe.csj430.checkkirafancompatibility.R;
import moe.csj430.checkkirafancompatibility.UpdateTask;
import moe.csj430.checkkirafancompatibility.util.AlipayDonate;
import moe.csj430.checkkirafancompatibility.viewmodel.MainViewModel;
import moe.csj430.checkkirafancompatibility.widget.ResultCardView;
import moe.csj430.checkkirafancompatibility.widget.WrapContentListView;

import static moe.csj430.checkkirafancompatibility.App.getAppContext;
import static moe.csj430.checkkirafancompatibility.App.getCurrAppTheme;
import static moe.csj430.checkkirafancompatibility.App.getDarkModeStatus;
import static moe.csj430.checkkirafancompatibility.App.getSysUiMode;
import static moe.csj430.checkkirafancompatibility.App.setCurrAppTheme;

public class MainActivity extends AppCompatActivity {

    MainViewModel mViewModel;

    ResultCardView deviceResultCardView;

    ContentLoadingProgressBar clpbToolbar;
    MenuItem itemShare;

    private static int currUiMode;
    private int cti;

    private interface MainCardView {
        String getStr();
        boolean getIsLoading();
    }

    private List<MainCardView> mcvs = new ArrayList<>();

    private static class DeviceCardView implements MainCardView {
        private ResultCardView mResultCardView;
        private String instSet = null, tMMB = null;

        @Override
        public boolean getIsLoading() {
            return false;
        }

        public ResultCardView getResultCardView() {
            return mResultCardView;
        }

        private void setResultCardView(ResultCardView resultCardView) {
            this.mResultCardView = resultCardView;
        }

        public String getStr() {
            StringBuilder sb = new StringBuilder();
            sb.append(getAppContext().getString(R.string.device));
            sb.append(": \n");
            if (tMMB != null) {
                sb.append(getAppContext().getString(R.string.ram));
                sb.append("\t");
                sb.append(tMMB);
                sb.append(" MB\n");
            }
            if (instSet != null) {
                sb.append(getAppContext().getString(R.string.instructionset_supported));
                sb.append("\t");
                sb.append(instSet);
                sb.append("\n");
            }
            return sb.toString();
        }

        DeviceCardView(ResultCardView resultCardView, MainViewModel viewModel) {
            setResultCardView(resultCardView);
            mResultCardView.init(getAppContext());
            mResultCardView.getTitleTV().setText(R.string.device);
            ExpandableLayout.LayoutParams elpr = new ExpandableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elpr.setMargins(8, 8, 8, 8);
            Context acontext = mResultCardView.getContext();
            LayoutInflater inflater = LayoutInflater.from(acontext);
            while (viewModel.getTotalMen() == null);
            long totalMen = viewModel.getTotalMen();
            tMMB = String.valueOf(totalMen / 1024 / 1024);
            while (viewModel.getInstructionSet() == null);
            instSet = viewModel.getInstructionSet();
            int mencolor, instscolor, mci, ici;
            if (totalMen < 1536 * 1024 * 1024)
                mci = 0;
            else if (totalMen < (long)2048 * 1024 * 1024)
                mci = 1;
            else
                mci = 2;
            if (instSet.contains("arm") && (instSet.contains("v7") || instSet.contains("v8")))
                ici = 2;
            else
                ici = 1;
            mencolor = viewModel.getStatusColors()[mci];
            instscolor = viewModel.getStatusColors()[ici];
            List<ExpandableLayout> dels = getResultCardView().getDetailELs();
            ExpandableLayout elm = dels.get(mci), eli = dels.get(ici), elinf = mResultCardView.getInfoEL();
            RelativeLayout rlm = (RelativeLayout)inflater.inflate(R.layout.items_not_selectable, elm, false), rli = (RelativeLayout)inflater.inflate(R.layout.items_not_selectable, eli, false);
            TextView tv_cim = rlm.findViewById(R.id.check_item), tv_cii = rli.findViewById(R.id.check_item);
            TextView tv_crm = rlm.findViewById(R.id.check_result), tv_cri = rli.findViewById(R.id.check_result);
            tv_cim.setText(R.string.ram);
            tv_cii.setText(R.string.instructionset_supported);
            tv_crm.setText(tMMB + " MB");
            tv_crm.setTextColor(mencolor);
            tv_cri.setText(instSet);
            tv_cri.setTextColor(instscolor);
            List<Boolean> neal = mResultCardView.getNotEmpty();
            if (elm == eli) {
                LinearLayout ll0 = new LinearLayout(acontext);
                ll0.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                llp.setMargins(8, 4, 8, 4);
                ll0.addView(rlm, llp);
                ll0.addView(rli, llp);
                elm.addView(ll0);
            } else {
                elm.addView(rlm, elpr);
                eli.addView(rli, elpr);
            }
            neal.set(mci, true);
            neal.set(ici, true);
            boolean f0 = false;
            if (mci == 0 || mci == 1 || ici == 1) {
                dels.get(0).expand();
                dels.get(1).expand();
                f0 = true;
            }
            if (f0 && !neal.get(2))
                mResultCardView.getArrowIV().setRotation(180.0f);
            mResultCardView.getArrowIV().setVisibility(View.VISIBLE);
            int stivci = 2;
            if (neal.get(0))
                stivci = 0;
            else if (neal.get(1))
                stivci = 1;
            ImageView stiv = mResultCardView.getStatusIV();
            switch (stivci) {
                case 2:
                    stiv.setImageResource(R.drawable.baseline_check_circle_outline_24);
                    break;
                case 1:
                    stiv.setImageResource(R.drawable.baseline_help_outline_24);
                    break;
                case 0:
                    stiv.setImageResource(R.drawable.baseline_highlight_off_24);
                    break;
                default:
                    break;
            }
            mResultCardView.getProgressBar().hide();
            stiv.setColorFilter(viewModel.getStatusColors()[stivci]);
            stiv.setVisibility(View.VISIBLE);
            TextView tv_info = new TextView(acontext);
            tv_info.setText(R.string.device_info_notice);
            elinf.addView(tv_info, elpr);
            mResultCardView.getInfoIV().setVisibility(View.VISIBLE);
        }
    }

    private static class SystemCardView implements MainCardView {
        @Override
        public String getStr() {
            if (ci == null)
                return "";
            Context context = mResultCardView.getContext();
            MainViewModel vm = ((MainActivity)context).mViewModel;
            StringBuilder sb = new StringBuilder();
            sb.append(getAppContext().getString(R.string.system));
            sb.append(": \n");
            String prefix = "- ";
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < cis[i].size(); ++j) {
                    sb.append(cis[i].get(j));
                    sb.append("\t");
                    sb.append(crs[i].get(j));
                    sb.append("\n");
                    if (cis[i].get(j).equals(ci[6])) {
                        if (vm.getThisProcessMounts() != null && vm.getThisProcessMounts().length > 0 && vm.getThisProcessMounts()[0]) {
                            sb.append(prefix);
                            sb.append(context.getString(R.string.magisk_found_in_this_mounts));
                            sb.append("\n");
                        }
                        SparseArray<String>[] msas = vm.getOtherAppProcessMounts();
                        if (msas != null && msas.length > 0 && msas[0] != null && msas[0].size() > 0) {
                            sb.append(prefix);
                            sb.append(context.getString(R.string.magisk_found_in_other_mounts));
                            sb.append(": \n");
                            SparseArray<String> msa = msas[0];
                            for (int k = 0; k < msa.size(); ++k) {
                                sb.append(prefix);
                                sb.append(msa.keyAt(k));
                                sb.append("\t");
                                sb.append(msa.valueAt(k));
                                sb.append("\n");
                            }
                        }
                    } else if (cis[i].get(j).equals(ci[7])) {
                        Collection<String>[] fdss = vm.getDPathFile();
                        if (fdss != null && fdss.length >= 2) {
                            if (fdss[0].size() > 0) {
                                sb.append(prefix);
                                sb.append(context.getString(R.string.file_found));
                                sb.append(": ");
                                for (String fp : fdss[0]) {
                                    sb.append(fp);
                                    sb.append("\t");
                                }
                                sb.append("\n");
                            }
                            if (fdss[1].size() > 0) {
                                sb.append(prefix);
                                sb.append(context.getString(R.string.directory_found));
                                sb.append(": ");
                                for (String dp : fdss[1]) {
                                    sb.append(dp);
                                    sb.append("\t");
                                }
                                sb.append("\n");
                            }
                        }
                    }
                }
            }
            return sb.toString();
        }

        private String[] ci;

        @Override
        public boolean getIsLoading() {
            return isLoading;
        }

        private ResultCardView mResultCardView;
        private List<WrapContentListView> lvs = new ArrayList<>();
        private List<Object> sys_status = new ArrayList<>();
        private boolean isLoading = true;

        private List<String>[] cis = new ArrayList[3];
        private List<String>[] crs = new ArrayList[3];

        public ResultCardView getResultCardView() {
            return mResultCardView;
        }

        private void setResultCardView(ResultCardView resultCardView) {
            this.mResultCardView = resultCardView;
        }

        SystemCardView(ResultCardView resultCardView, MainViewModel viewModel) {
            setResultCardView(resultCardView);
            mResultCardView.init(getAppContext());
            mResultCardView.getTitleTV().setText(R.string.system);
            ExpandableLayout.LayoutParams elpr = new ExpandableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elpr.setMargins(8, 8, 8, 8);
            for (int i = 0; i < 3; ++i) {
                cis[i] = new ArrayList<>();
                crs[i] = new ArrayList<>();
            }
            Context acontext = mResultCardView.getContext();
            for (int i = 0; i < 3; ++i) {
                lvs.add(new WrapContentListView(acontext));
                lvs.get(i).setDivider(null);
            }
            for (int i = 0; i < 3; ++i)
                mResultCardView.getDetailELs().get(i).addView(lvs.get(i), elpr);
            final Observer<List<Object>> sysObserver = sStatus -> {
                changeViewToWait();
                ((MainActivity)acontext).refreshTB();
                sys_status = sStatus;
                refresh(viewModel);
            };
            viewModel.getSysStatus().observe((AppCompatActivity)acontext, sysObserver);
            LayoutInflater inflater = LayoutInflater.from(acontext);
            LinearLayout infoLL = (LinearLayout)inflater.inflate(R.layout.sys_info, mResultCardView.getInfoEL(), false);
            mResultCardView.getInfoEL().addView(infoLL);
            mResultCardView.getInfoIV().setVisibility(View.VISIBLE);
            mResultCardView.getRefreshIV().setOnClickListener(v -> update(viewModel));
        }

        private void changeViewToWait() {
            isLoading = true;
            mResultCardView.getRefreshIV().setVisibility(View.GONE);
            mResultCardView.getStatusIV().setVisibility(View.GONE);
            mResultCardView.getArrowIV().setVisibility(View.GONE);
            mResultCardView.getProgressBar().show();
            for (ExpandableLayout expandableLayout : mResultCardView.getDetailELs())
                expandableLayout.collapse();
            for (Boolean isne : mResultCardView.getNotEmpty())
                isne = false;
        }

        private void changeViewAfterRefresh() {
            isLoading = false;
            mResultCardView.getProgressBar().hide();
            ImageView ivstatus = mResultCardView.getStatusIV();
            ivstatus.setImageResource(R.drawable.baseline_check_circle_outline_24);
            ivstatus.setColorFilter(sct[2]);
            List<Boolean> nel = mResultCardView.getNotEmpty();
            if (nel.get(0)) {
                ivstatus.setImageResource(R.drawable.baseline_highlight_off_24);
                ivstatus.setColorFilter(sct[0]);
            } else if (nel.get(1)) {
                ivstatus.setImageResource(R.drawable.baseline_help_outline_24);
                ivstatus.setColorFilter(sct[1]);
            }
            ivstatus.setVisibility(View.VISIBLE);
            ImageView arrowiv = mResultCardView.getArrowIV();
            arrowiv.setRotation(0.0f);
            arrowiv.setVisibility(View.VISIBLE);
            if (nel.get(0) || nel.get(1)) {
                List<ExpandableLayout> els = mResultCardView.getDetailELs();
                els.get(0).expand();
                els.get(1).expand();
                if (!nel.get(2))
                    arrowiv.setRotation(180.0f);
            }
            mResultCardView.getRefreshIV().setVisibility(View.VISIBLE);
        }

        private void update(MainViewModel viewModel) {
            if (!isLoading && viewModel.getFutureTasks()[0] == null) {
                changeViewToWait();
                ((MainActivity)mResultCardView.getContext()).refreshTB();
                viewModel.checkSys();
            }
        }

        int[] sct;

        private void refresh(MainViewModel viewModel) {
            int[] carr = checkSystem(viewModel);
            ci = viewModel.getCheckSysItem();
            for (List<String> ls : cis)
                ls.clear();
            for (List<String> lr : crs)
                lr.clear();
            addToLV(carr[0], ci[0], viewModel.getSystemVer());
            String[] CHECK_PROP_ITEM = viewModel.getCheckPropItem();
            for (int i = 1; i < sys_status.size(); ++i) {
                if (i <= CHECK_PROP_ITEM.length) {
                    String rs;
                    if (i <= 3)
                        rs = (String)sys_status.get(i);
                    else
                        rs = String.valueOf((Integer)sys_status.get(i));
                    addToLV(carr[i], ci[i], rs);
                } else if (i == CHECK_PROP_ITEM.length + 1) {
                    if ((Boolean)sys_status.get(i))
                        addToLV(carr[i], ci[i], getAppContext().getString(R.string.magisk_found));
                    else
                        addToLV(carr[i], ci[i], getAppContext().getString(R.string.magisk_not_found));
                } else if (i == CHECK_PROP_ITEM.length + 2) {
                    if ((Boolean)sys_status.get(i))
                        addToLV(carr[i], ci[i], getAppContext().getString(R.string.found));
                    else
                        addToLV(carr[i], ci[i], getAppContext().getString(R.string.not_found));
                }
            }
            if (ci.length == sys_status.size()) {
                if ((Boolean)sys_status.get(sys_status.size() - 1))
                    addToLV(carr[sys_status.size() - 1], ci[ci.length - 1], getAppContext().getString(R.string.on));
                else
                    addToLV(carr[sys_status.size() - 1], ci[ci.length - 1], getAppContext().getString(R.string.off));
            }
            Context acontext = mResultCardView.getContext();
            for (int i = 0; i < 3; ++i) {
                int icnt = cis[i].size();
                if (icnt > 0)
                    mResultCardView.getNotEmpty().set(i, true);
                List<String> cr = crs[i];
                int ii = i;
                lvs.get(i).setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return icnt;
                    }

                    @Override
                    public Object getItem(int position) {
                        return cr.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        RelativeLayout layout;
                        LayoutInflater inflater = LayoutInflater.from(acontext);
                        if (cis[ii].get(position).equals(ci[ci.length - 1])) {
                            if (ii < 2) {
                                layout = (RelativeLayout)inflater.inflate(R.layout.items, parent, false);
                                layout.setOnClickListener(DS_OCL);
                            } else
                                layout = (RelativeLayout)inflater.inflate(R.layout.items_not_selectable, parent, false);
                        } else if (cis[ii].get(position).equals(ci[6])) {
                            if (ii < 2) {
                                layout = (RelativeLayout)inflater.inflate(R.layout.items, parent, false);
                                layout.setOnClickListener(v -> {
                                    StringBuilder msb = new StringBuilder();
                                    boolean[] hasThisMount = viewModel.getThisProcessMounts();
                                    if (hasThisMount != null && hasThisMount.length > 0 && hasThisMount[0]) {
                                        msb.append(acontext.getString(R.string.magisk_found_in_this_mounts));
                                        msb.append("\n\n");
                                    }
                                    SparseArray<String>[] otherMounts = viewModel.getOtherAppProcessMounts();
                                    if (otherMounts != null && otherMounts.length > 0 && otherMounts[0] != null && otherMounts[0].size() > 0) {
                                        msb.append(acontext.getString(R.string.magisk_found_in_other_mounts));
                                        msb.append(": \n");
                                        for (int i = 0; i < otherMounts[0].size(); ++i) {
                                            msb.append(otherMounts[0].keyAt(i));
                                            msb.append("\t");
                                            msb.append(otherMounts[0].valueAt(i));
                                            msb.append("\n");
                                        }
                                    }
                                    new MaterialAlertDialogBuilder(acontext).setTitle(cis[ii].get(position)).setMessage(msb).setPositiveButton(R.string.know_it, null).create().show();
                                });
                            } else
                                layout = (RelativeLayout)inflater.inflate(R.layout.items_not_selectable, parent, false);
                        } else if (cis[ii].get(position).equals(ci[7])) {
                            if (ii < 2) {
                                layout = (RelativeLayout)inflater.inflate(R.layout.items, parent, false);
                                layout.setOnClickListener(v -> {
                                    StringBuilder asb = new StringBuilder();
                                    Collection<String>[] dfp = viewModel.getDPathFile();
                                    if (dfp != null && dfp.length >= 2) {
                                        if (dfp[0].size() > 0) {
                                            asb.append(acontext.getString(R.string.file_found));
                                            asb.append(": \n");
                                            for (String fp : dfp[0]) {
                                                asb.append(fp);
                                                asb.append("\t");
                                            }
                                            asb.append("\n\n");
                                        }
                                        if (dfp[1].size() > 0) {
                                            asb.append(acontext.getString(R.string.directory_found));
                                            asb.append(": \n");
                                            for (String dp : dfp[1]) {
                                                asb.append(dp);
                                                asb.append("\t");
                                            }
                                            asb.append("\n");
                                        }
                                    }
                                    new MaterialAlertDialogBuilder(acontext).setTitle(cis[ii].get(position)).setMessage(asb).setPositiveButton(R.string.know_it, null).create().show();
                                });
                            } else
                                layout = (RelativeLayout)inflater.inflate(R.layout.items_not_selectable, parent, false);
                        } else
                            layout = (RelativeLayout)inflater.inflate(R.layout.items_not_selectable, parent, false);
                        ((TextView)layout.findViewById(R.id.check_item)).setText(cis[ii].get(position));
                        TextView tvr = layout.findViewById(R.id.check_result);
                        tvr.setText(crs[ii].get(position));
                        tvr.setTextColor(sct[ii]);
                        return layout;
                    }
                });
            }
            changeViewAfterRefresh();
            ((MainActivity)acontext).refreshTB();
        }

        private void addToLV(int color, String cistr, String crstr) {
            int si = 0, i;
            for (i = 0; i < sct.length; ++i) {
                if (sct[i] == color) {
                    si = i;
                    break;
                }
            }
            if (i >= 3)
                return;
            cis[si].add(cistr);
            crs[si].add(crstr);
        }

        private int[] checkSystem(MainViewModel viewModel) {
            if (sys_status.size() <= 0)
                return null;
            int[] colorArray = new int[sys_status.size()];
            sct = viewModel.getStatusColors();
            for (int i = 0; i < colorArray.length; i++)
                colorArray[i] = sct[2];
            int SYSTEM_SDK_INT = (Integer)sys_status.get(0);
            if (SYSTEM_SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
                colorArray[0] = sct[0];
            else if (SYSTEM_SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                colorArray[0] = sct[1];
            int syss_i = 1;
            String[] CHECK_PROP_ITEM = viewModel.getCheckPropItem();
            for (String check_prop:CHECK_PROP_ITEM) {
                switch (check_prop) {
                    case "persist.sys.usb.config":
                        if ("adb".equals(sys_status.get(syss_i)))
                            colorArray[syss_i] = sct[0];
                        break;
                    case "ro.build.type":
                        if ("eng".equals(sys_status.get(syss_i)))
                            colorArray[syss_i] = sct[0];
                        break;
                    case "ro.build.tags":
                        if (!"release-keys".equals(sys_status.get(syss_i)))
                            colorArray[syss_i] = sct[0];
                        break;
                    case "ro.debuggable":
                        if ((Integer)sys_status.get(syss_i) != 0)
                            colorArray[syss_i] = sct[0];
                        break;
                    case "ro.secure":
                        if ((Integer)sys_status.get(syss_i) == 0)
                            colorArray[syss_i] = sct[0];
                        break;
                    default:
                        break;
                }
                ++syss_i;
            }
            if ((Boolean)sys_status.get(syss_i))
                colorArray[syss_i] = sct[0];
            ++syss_i;
            if ((Boolean)sys_status.get(syss_i))
                colorArray[syss_i] = sct[1];
            ++syss_i;
            if (syss_i < sys_status.size()) {
                Boolean isUSBDebugOn = (Boolean)sys_status.get(syss_i);
                if (isUSBDebugOn)
                    colorArray[syss_i] = sct[0];
            }
            return colorArray;
        }

        View.OnClickListener DS_OCL = view -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                mResultCardView.getContext().startActivity(intent);
            } catch (Exception e) {
                try {
                    ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.DevelopmentSettings");
                    Intent intent = new Intent();
                    intent.setComponent(componentName);
                    intent.setAction("android.intent.action.View");
                    mResultCardView.getContext().startActivity(intent);
                } catch (Exception e1) {
                    try {
                        Intent intent = new Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS");//部分小米手机采用这种方式跳转
                        mResultCardView.getContext().startActivity(intent);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }

                }
            }
        };
    }

    private static class XposedCardView implements MainCardView {
        @Override
        public String getStr() {
            StringBuilder sb = new StringBuilder();
            sb.append(getAppContext().getString(R.string.xposed_and_root));
            sb.append(": \n");
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < cis[i].size(); ++j) {
                    sb.append(cis[i].get(j));
                    sb.append("\t");
                    sb.append(crs[i].get(j));
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        @Override
        public boolean getIsLoading() {
            return isLoading;
        }

        private ResultCardView mResultCardView;
        private List<WrapContentListView> lvs = new ArrayList<>();
        private List<Integer> xposed_status = new ArrayList<>();
        private boolean isLoading = true;

        private List<String>[] cis = new ArrayList[3];
        private List<String>[] crs = new ArrayList[3];

        public ResultCardView getResultCardView() {
            return mResultCardView;
        }

        private void setResultCardView(ResultCardView resultCardView) {
            this.mResultCardView = resultCardView;
        }

        XposedCardView(ResultCardView resultCardView, MainViewModel viewModel) {
            setResultCardView(resultCardView);
            mResultCardView.init(getAppContext());
            mResultCardView.getTitleTV().setText(R.string.xposed_and_root);
            ExpandableLayout.LayoutParams elpr = new ExpandableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elpr.setMargins(8, 8, 8, 8);
            for (int i = 0; i < 3; ++i) {
                cis[i] = new ArrayList<>();
                crs[i] = new ArrayList<>();
            }
            Context acontext = mResultCardView.getContext();
            for (int i = 0; i < 3; ++i) {
                lvs.add(new WrapContentListView(acontext));
                lvs.get(i).setDivider(null);
            }
            for (int i = 0; i < 3; ++i)
                mResultCardView.getDetailELs().get(i).addView(lvs.get(i), elpr);
            final Observer<List<Integer>> xposedObserver = xStatus -> {
                changeViewToWait();
                ((MainActivity)acontext).refreshTB();
                xposed_status = xStatus;
                refresh(viewModel);
            };
            viewModel.getXposedStatus().observe((AppCompatActivity)acontext, xposedObserver);
            LayoutInflater inflater = LayoutInflater.from(acontext);
            LinearLayout infoLL = (LinearLayout)inflater.inflate(R.layout.xposed_root_dialog, mResultCardView.getInfoEL(), false);
            ((Button)infoLL.findViewById(R.id.root_guide)).setOnClickListener(v -> new MaterialAlertDialogBuilder(acontext).setTitle(R.string.root_solution).setView(R.layout.root_solution_dialog).setPositiveButton(R.string.know_it, null).show());
            ((Button)infoLL.findViewById(R.id.xposed_guide)).setOnClickListener(v -> new MaterialAlertDialogBuilder(acontext).setTitle(R.string.xposed_solution).setView(R.layout.xposed_solution_dialog).setPositiveButton(R.string.know_it, null).show());
            mResultCardView.getInfoEL().addView(infoLL);
            mResultCardView.getInfoIV().setVisibility(View.VISIBLE);
            mResultCardView.getRefreshIV().setOnClickListener(v -> update(viewModel));
        }

        private void changeViewToWait() {
            isLoading = true;
            mResultCardView.getRefreshIV().setVisibility(View.GONE);
            mResultCardView.getStatusIV().setVisibility(View.GONE);
            mResultCardView.getArrowIV().setVisibility(View.GONE);
            mResultCardView.getProgressBar().show();
            for (ExpandableLayout expandableLayout : mResultCardView.getDetailELs())
                expandableLayout.collapse();
            for (Boolean isne : mResultCardView.getNotEmpty())
                isne = false;
        }

        private void changeViewAfterRefresh() {
            isLoading = false;
            mResultCardView.getProgressBar().hide();
            ImageView ivstatus = mResultCardView.getStatusIV();
            ivstatus.setImageResource(R.drawable.baseline_check_circle_outline_24);
            ivstatus.setColorFilter(sct[2]);
            List<Boolean> nel = mResultCardView.getNotEmpty();
            if (nel.get(0)) {
                ivstatus.setImageResource(R.drawable.baseline_highlight_off_24);
                ivstatus.setColorFilter(sct[0]);
            } else if (nel.get(1)) {
                ivstatus.setImageResource(R.drawable.baseline_help_outline_24);
                ivstatus.setColorFilter(sct[1]);
            }
            ivstatus.setVisibility(View.VISIBLE);
            ImageView arrowiv = mResultCardView.getArrowIV();
            arrowiv.setRotation(0.0f);
            arrowiv.setVisibility(View.VISIBLE);
            if (nel.get(0) || nel.get(1)) {
                List<ExpandableLayout> els = mResultCardView.getDetailELs();
                els.get(0).expand();
                els.get(1).expand();
                if (!nel.get(2))
                    arrowiv.setRotation(180.0f);
            }
            mResultCardView.getRefreshIV().setVisibility(View.VISIBLE);
        }

        private void update(MainViewModel viewModel) {
            if (!isLoading && viewModel.getFutureTasks()[1] == null) {
                Toast.makeText(mResultCardView.getContext(), R.string.xposed_root_refresh_notice, Toast.LENGTH_SHORT).show();
                changeViewToWait();
                ((MainActivity)mResultCardView.getContext()).refreshTB();
                viewModel.checkXposed();
            }
        }

        int[] sct;

        private void refresh(MainViewModel viewModel) {
            int[] carr = checkXposed(viewModel);
            String[] ci = viewModel.getCheckXposedItem();
            for (List<String> ls : cis)
                ls.clear();
            for (List<String> lr : crs)
                lr.clear();
            Context context = mResultCardView.getContext();
            int cxicnt = viewModel.getCheckXposedItem().length;
            for (int i = 0; i < cxicnt; ++i)
                addToLV(carr[i], ci[i], xposed_status.get(i) == 0 ? context.getString(R.string.item_no_xposed) : context.getString(R.string.item_found_xposed));
            addToLV(carr[cxicnt], context.getString(R.string.root_check), viewModel.getRootStatus()[xposed_status.get(cxicnt) + 1]);
            for (int i = 0; i < 3; ++i) {
                int icnt = cis[i].size();
                if (icnt > 0)
                    mResultCardView.getNotEmpty().set(i, true);
                List<String> crl = crs[i];
                List<String> cil = cis[i];
                int ic = sct[i];
                lvs.get(i).setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return icnt;
                    }

                    @Override
                    public Object getItem(int position) {
                        return crl.get(position);
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null)
                            convertView = LayoutInflater.from(context).inflate(R.layout.items_not_selectable, parent, false);
                        ((TextView)convertView.findViewById(R.id.check_item)).setText(cil.get(position));
                        TextView tvr = convertView.findViewById(R.id.check_result);
                        tvr.setText(crl.get(position));
                        tvr.setTextColor(ic);
                        return convertView;
                    }
                });
            }
            changeViewAfterRefresh();
            ((MainActivity)context).refreshTB();
        }

        private void addToLV(int color, String cistr, String crstr) {
            int si = 0, i;
            for (i = 0; i < sct.length; ++i) {
                if (sct[i] == color) {
                    si = i;
                    break;
                }
            }
            if (i >= sct.length)
                return;
            cis[si].add(cistr);
            crs[si].add(crstr);
        }

        private int[] checkXposed(MainViewModel viewModel) {
            if (xposed_status.size() <= 0)
                return new int[0];
            int[] colorArray = new int[xposed_status.size()];
            sct = viewModel.getStatusColors();
            for (int i = 0; i < colorArray.length; ++i)
                colorArray[i] = xposed_status.get(i) == 0 ? sct[2] : sct[0];
            return colorArray;
        }
    }

    private static class GoogleCardView implements MainCardView {
        @Override
        public String getStr() {
            StringBuilder sb = new StringBuilder();
            sb.append(getAppContext().getString(R.string.google_suite));
            sb.append(": \n");
            sb.append(getAppContext().getString(R.string.google_service));
            sb.append("\t");
            sb.append(rs);
            sb.append("\t");
            sb.append(gs_re_code);
            sb.append("\n");
            return sb.toString();
        }

        @Override
        public boolean getIsLoading() {
            return isLoading;
        }

        private ResultCardView mResultCardView;
        private Integer gs_re_code;
        private boolean isLoading = true;
        private String rs;

        public ResultCardView getResultCardView() {
            return mResultCardView;
        }

        private void setResultCardView(ResultCardView resultCardView) {
            this.mResultCardView = resultCardView;
        }

        GoogleCardView(ResultCardView resultCardView, MainViewModel viewModel) {
            setResultCardView(resultCardView);
            mResultCardView.init(getAppContext());
            mResultCardView.getTitleTV().setText(R.string.google_suite);
            Context acontext = mResultCardView.getContext();
            ExpandableLayout.LayoutParams elpr = new ExpandableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            final Observer<Integer> gsReObserver = gsrec -> {
                changeViewToWait();
                ((MainActivity)acontext).refreshTB();
                gs_re_code = gsrec;
                refresh(viewModel);
            };
            viewModel.getGsReCode().observe((AppCompatActivity)acontext, gsReObserver);
            TextView infoTV = new TextView(acontext);
            infoTV.setText(R.string.google_info_notice);
            mResultCardView.getInfoEL().addView(infoTV, elpr);
            mResultCardView.getInfoIV().setVisibility(View.VISIBLE);
            mResultCardView.getRefreshIV().setOnClickListener(v -> update(viewModel));
        }

        private void changeViewToWait() {
            isLoading = true;
            mResultCardView.getRefreshIV().setVisibility(View.GONE);
            mResultCardView.getStatusIV().setVisibility(View.GONE);
            mResultCardView.getArrowIV().setVisibility(View.GONE);
            mResultCardView.getProgressBar().show();
            for (ExpandableLayout expandableLayout : mResultCardView.getDetailELs())
                expandableLayout.collapse();
            for (Boolean isne : mResultCardView.getNotEmpty())
                isne = false;
        }

        private void changeViewAfterRefresh() {
            isLoading = false;
            mResultCardView.getProgressBar().hide();
            ImageView ivstatus = mResultCardView.getStatusIV();
            ivstatus.setImageResource(R.drawable.baseline_check_circle_outline_24);
            ivstatus.setColorFilter(sct[2]);
            List<Boolean> nel = mResultCardView.getNotEmpty();
            if (nel.get(0)) {
                ivstatus.setImageResource(R.drawable.baseline_highlight_off_24);
                ivstatus.setColorFilter(sct[0]);
            } else if (nel.get(1)) {
                ivstatus.setImageResource(R.drawable.baseline_help_outline_24);
                ivstatus.setColorFilter(sct[1]);
            }
            ivstatus.setVisibility(View.VISIBLE);
            ImageView arrowiv = mResultCardView.getArrowIV();
            arrowiv.setRotation(0.0f);
            arrowiv.setVisibility(View.VISIBLE);
            if (nel.get(0) || nel.get(1)) {
                List<ExpandableLayout> els = mResultCardView.getDetailELs();
                els.get(0).expand();
                els.get(1).expand();
                if (!nel.get(2))
                    arrowiv.setRotation(180.0f);
            }
            mResultCardView.getRefreshIV().setVisibility(View.VISIBLE);
        }

        private void update(MainViewModel viewModel) {
            if (!isLoading && viewModel.getFutureTasks()[1] == null) {
                changeViewToWait();
                ((MainActivity)mResultCardView.getContext()).refreshTB();
                viewModel.checkGsReCode();
            }
        }

        int[] sct;

        private void refresh(MainViewModel viewModel) {
            int ci;
            sct = viewModel.getStatusColors();
            List<ExpandableLayout> els = mResultCardView.getDetailELs();
            for (ExpandableLayout el : els)
                el.removeAllViews();
            Context acontext = mResultCardView.getContext();
            switch (gs_re_code) {
                case ConnectionResult.SUCCESS:
                    ci = 2;
                    rs = acontext.getString(R.string.available);
                    break;
                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                    ci = 1;
                    rs = acontext.getString(R.string.update_required);
                    break;
                case ConnectionResult.SERVICE_UPDATING:
                    ci = 1;
                    rs = acontext.getString(R.string.updating);
                    break;
                default:
                    ci = 0;
                    rs = acontext.getString(R.string.not_available);
                    break;
            }
            RelativeLayout gsrl = (RelativeLayout)LayoutInflater.from(acontext).inflate(R.layout.items, els.get(ci), false);
            ((TextView)gsrl.findViewById(R.id.check_item)).setText(R.string.google_service);
            TextView tvr = gsrl.findViewById(R.id.check_result);
            tvr.setText(rs);
            tvr.setTextColor(sct[ci]);
            gsrl.setOnClickListener(GS_OCL);
            els.get(ci).addView(gsrl);
            mResultCardView.getNotEmpty().set(ci, true);
            changeViewAfterRefresh();
            ((MainActivity)acontext).refreshTB();
        }

        View.OnClickListener GS_OCL = view -> {
            Context acontext = mResultCardView.getContext();
            Toast toast = Toast.makeText(acontext, acontext.getString(R.string.return_value) + ": " + gs_re_code, Toast.LENGTH_SHORT);
            toast.show();
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            if (googleApiAvailability.isUserResolvableError(gs_re_code))
                googleApiAvailability.getErrorDialog((Activity)acontext, gs_re_code, 8996).show();
        };
    }

    private static class BlackAppCardView implements MainCardView {
        @Override
        public String getStr() {
            StringBuilder sb = new StringBuilder();
            Context appcontext = getAppContext();
            sb.append(appcontext.getString(R.string.app_list));
            sb.append(": \n");
            if (ris[0].size() > 0) {
                for (ResolveInfo ri : ris[0]) {
                    sb.append(ri.activityInfo.loadLabel(appcontext.getPackageManager()));
                    sb.append("\t");
                    sb.append(ri.activityInfo.packageName);
                    sb.append("\n");
                }
            } else {
                sb.append(getAppContext().getString(R.string.black_list_apps_not_found));
                sb.append("\n");
            }
            return sb.toString();
        }

        @Override
        public boolean getIsLoading() {
            return isLoading;
        }

        private ResultCardView mResultCardView;
        private List<WrapContentListView> lvs = new ArrayList<>();
        private List<ResolveInfo> resolveInfos = new ArrayList<>();
        private boolean isLoading = true;

        private List<ResolveInfo>[] ris = new ArrayList[3];

        public ResultCardView getResultCardView() {
            return mResultCardView;
        }

        private void setResultCardView(ResultCardView resultCardView) {
            this.mResultCardView = resultCardView;
        }

        BlackAppCardView(ResultCardView resultCardView, MainViewModel viewModel) {
            setResultCardView(resultCardView);
            mResultCardView.init(getAppContext());
            mResultCardView.getTitleTV().setText(R.string.app_list);
            ExpandableLayout.LayoutParams elpr = new ExpandableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elpr.setMargins(8, 8, 8, 8);
            for (int i = 0; i < 3; ++i)
                ris[i] = new ArrayList<>();
            Context acontext = mResultCardView.getContext();
            for (int i = 0; i < 3; ++i) {
                lvs.add(new WrapContentListView(acontext));
                lvs.get(i).setDivider(null);
            }
            for (int i = 0; i < 3; ++i)
                mResultCardView.getDetailELs().get(i).addView(lvs.get(i), elpr);
            final Observer<List<ResolveInfo>> blacklistAppsObserver = resolveInfs -> {
                changeViewToWait();
                ((MainActivity)acontext).refreshTB();
                resolveInfos = resolveInfs;
                refresh(viewModel);
            };
            viewModel.getBlacklistApps().observe((AppCompatActivity)acontext, blacklistAppsObserver);
            LinearLayout infoLL = (LinearLayout)LayoutInflater.from(acontext).inflate(R.layout.app_list_notice_dialog, mResultCardView.getInfoEL(), false);
            mResultCardView.getInfoEL().addView(infoLL);
            mResultCardView.getInfoIV().setVisibility(View.VISIBLE);
            mResultCardView.getRefreshIV().setOnClickListener(v -> update(viewModel));
        }

        private void changeViewToWait() {
            isLoading = true;
            mResultCardView.getRefreshIV().setVisibility(View.GONE);
            mResultCardView.getStatusIV().setVisibility(View.GONE);
            mResultCardView.getArrowIV().setVisibility(View.GONE);
            mResultCardView.getProgressBar().show();
            for (ExpandableLayout expandableLayout : mResultCardView.getDetailELs())
                expandableLayout.collapse();
            for (Boolean isne : mResultCardView.getNotEmpty())
                isne = false;
        }

        private void changeViewAfterRefresh() {
            isLoading = false;
            mResultCardView.getProgressBar().hide();
            ImageView ivstatus = mResultCardView.getStatusIV();
            ivstatus.setImageResource(R.drawable.baseline_check_circle_outline_24);
            ivstatus.setColorFilter(sct[2]);
            List<Boolean> nel = mResultCardView.getNotEmpty();
            if (nel.get(0)) {
                ivstatus.setImageResource(R.drawable.baseline_highlight_off_24);
                ivstatus.setColorFilter(sct[0]);
            } else if (nel.get(1)) {
                ivstatus.setImageResource(R.drawable.baseline_help_outline_24);
                ivstatus.setColorFilter(sct[1]);
            }
            ivstatus.setVisibility(View.VISIBLE);
            ImageView arrowiv = mResultCardView.getArrowIV();
            arrowiv.setRotation(0.0f);
            arrowiv.setVisibility(View.VISIBLE);
            if (nel.get(0) || nel.get(1)) {
                List<ExpandableLayout> els = mResultCardView.getDetailELs();
                els.get(0).expand();
                els.get(1).expand();
                if (!nel.get(2))
                    arrowiv.setRotation(180.0f);
            }
            mResultCardView.getRefreshIV().setVisibility(View.VISIBLE);
        }

        private void update(MainViewModel viewModel) {
            if (!isLoading && viewModel.getFutureTasks()[2] == null) {
                changeViewToWait();
                ((MainActivity)mResultCardView.getContext()).refreshTB();
                viewModel.checkBlacklistApps();
            }
        }

        int[] sct;

        private void refresh(MainViewModel viewModel) {
            for (List<ResolveInfo> ri : ris)
                ri.clear();
            ris[0] = resolveInfos;
            sct = viewModel.getStatusColors();
            Context acontext = mResultCardView.getContext();
            LayoutInflater inflater = LayoutInflater.from(acontext);
            if (ris[0].size() > 0)
                mResultCardView.getNotEmpty().set(0, true);
            else
                mResultCardView.getNotEmpty().set(2, true);
            lvs.get(0).setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return ris[0].size();
                }

                @Override
                public Object getItem(int position) {
                    return ris[0].get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                View.OnClickListener oclun = v -> {
                    String packName = ((TextView)v.findViewById(R.id.check_result)).getText().toString();
                    Intent unintent = new Intent(Intent.ACTION_DELETE);
                    unintent.setData(Uri.parse("package:" + packName));
                    unintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (packName.equals("com.topjohnwu.magisk")) {
                        new MaterialAlertDialogBuilder(acontext).setTitle(R.string.magisk_manager_detected).setMessage(R.string.magisk_manager_advice).setNeutralButton(R.string.uninstall_directly, (dialogInterface, i1) -> acontext.startActivity(unintent)).setNegativeButton(R.string.let_it_go_0, null).setPositiveButton(R.string.open_magisk_manager, (dialogInterface, i1) -> {
                            PackageManager packageManager = acontext.getPackageManager();
                            Intent open_magisk_manager_intent = packageManager.getLaunchIntentForPackage(packName);
                            acontext.startActivity(open_magisk_manager_intent);
                        }).show();
                    } else
                        acontext.startActivity(unintent);
                };

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.items, parent, false);
                        convertView.setOnClickListener(oclun);
                    }
                    TextView tv01 = convertView.findViewById(R.id.check_item);
                    tv01.setText(ris[0].get(position).activityInfo.loadLabel(acontext.getPackageManager()));
                    TextView tv02 = convertView.findViewById(R.id.check_result);
                    tv02.setText(ris[0].get(position).activityInfo.packageName);
                    tv01.setTextColor(sct[0]);
                    tv02.setTextColor(sct[0]);
                    return convertView;
                }
            });
            lvs.get(2).setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return resolveInfos.size() > 0 ? 0 : 1;
                }

                @Override
                public Object getItem(int position) {
                    return null;
                }

                @Override
                public long getItemId(int position) {
                    return 0;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (position == 0) {
                        RelativeLayout rl2 = (RelativeLayout)inflater.inflate(R.layout.item_not_selectable, parent, false);
                        TextView tv2 = rl2.findViewById(R.id.check_item);
                        tv2.setText(R.string.black_list_apps_not_found);
                        tv2.setTextColor(sct[2]);
                        return rl2;
                    }
                    return null;
                }
            });
            changeViewAfterRefresh();
            ((MainActivity)acontext).refreshTB();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChange();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getCurrAppTheme() != 0x10 && getCurrAppTheme() != 0x01 && newConfig.uiMode != currUiMode)
            recreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name_short_0);
        currUiMode = getSysUiMode();
        setTheme(R.style.MainActivityTheme);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.tv_stabar)).getLayoutParams().height = getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
        Toolbar toolbar = findViewById(R.id.toolbar);
        clpbToolbar = toolbar.findViewById(R.id.clpb_toolbar);
        setSupportActionBar(toolbar);

        mViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(MainViewModel.class);

        int scrori = getResources().getConfiguration().orientation;
        LayoutInflater inflater = LayoutInflater.from(this);
        ResultCardView sysResultCardView, xposedResultCardView, googleResultCardView, baCardView;
        LinearLayout mainLL = findViewById(R.id.ll_main);
        if (scrori == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout mainLLL = findViewById(R.id.ll_main_l), mainLLR = findViewById(R.id.ll_main_r);
            deviceResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLLL, false);
            mainLLL.addView(deviceResultCardView);
            sysResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLLL, false);
            mainLLL.addView(sysResultCardView);
            xposedResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLLR, false);
            mainLLR.addView(xposedResultCardView);
            googleResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLLL, false);
            mainLLL.addView(googleResultCardView);
            baCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLLR, false);
            mainLLR.addView(baCardView);
        } else {
            deviceResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLL, false);
            mainLL.addView(deviceResultCardView);
            sysResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLL, false);
            mainLL.addView(sysResultCardView);
            xposedResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLL, false);
            mainLL.addView(xposedResultCardView);
            googleResultCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLL, false);
            mainLL.addView(googleResultCardView);
            baCardView = (ResultCardView)inflater.inflate(R.layout.result_cardview, mainLL, false);
            mainLL.addView(baCardView);
        }

        SystemCardView sysCardView = new SystemCardView(sysResultCardView, mViewModel);
        mcvs.add(sysCardView);
        XposedCardView xposedCardView = new XposedCardView(xposedResultCardView, mViewModel);
        mcvs.add(xposedCardView);
        GoogleCardView googleCardView = new GoogleCardView(googleResultCardView, mViewModel);
        mcvs.add(googleCardView);
        BlackAppCardView blackAppCardView = new BlackAppCardView(baCardView, mViewModel);
        mcvs.add(blackAppCardView);

        TextView tvbtm = new TextView(this);
        LinearLayout.LayoutParams mainLLL = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(getResources().getIdentifier("navigation_bar_height", "dimen", "android")));
        mainLL.addView(tvbtm, mainLLL);

        new UpdateTask(MainActivity.this, true, false).update();
    }

    boolean hideitemshare = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SupportMenuInflater inflater = new SupportMenuInflater(this);
        inflater.inflate(R.menu.activity_main, menu);
        itemShare = menu.findItem(R.id.id_menu_share);
        if (hideitemshare)
            itemShare.setVisible(false);
        else
            itemShare.setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    public void refreshTB() {
        boolean isloading = false;
        for (MainCardView mcv : mcvs) {
            if (mcv.getIsLoading())
                isloading = true;
        }
        if (isloading) {
            hideitemshare = true;
            invalidateOptionsMenu();
            clpbToolbar.show();
        } else {
            clpbToolbar.hide();
            hideitemshare = false;
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.id_menu_share:
                StringBuilder sb = new StringBuilder();
                sb.append(getString(R.string.app_name));
                sb.append("\t");
                String appver = "";
                try {
                    PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
                    appver = pInfo.versionName;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sb.append(appver);
                sb.append("\n");
                for (MainCardView mcv : mcvs) {
                    String res = mcv.getStr();
                    if (res != null) {
                        sb.append(res);
                        sb.append("\n");
                    }
                }
                Intent shareintent = new Intent();
                shareintent.setAction(Intent.ACTION_SEND);
                shareintent.setType("text/plain");
                String sharetitle = getString(R.string.app_name) + "\t" + getString(R.string.check_results);
                shareintent.putExtra(Intent.EXTRA_SUBJECT, sharetitle);
                shareintent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                shareintent = Intent.createChooser(shareintent, sharetitle);
                startActivity(shareintent);
                break;
            case R.id.id_menu_explanation:
                View explanation_dialog_view = View.inflate(MainActivity.this, R.layout.explanation_dialog, null);
                ((TextView)explanation_dialog_view.findViewById(R.id.green_words)).setTextColor(mViewModel == null ? Color.GREEN : mViewModel.getStatusColors()[2]);
                ((TextView)explanation_dialog_view.findViewById(R.id.yellow_words)).setTextColor(mViewModel == null ? Color.YELLOW : mViewModel.getStatusColors()[1]);
                ((TextView)explanation_dialog_view.findViewById(R.id.red_words)).setTextColor(mViewModel == null ? Color.RED : mViewModel.getStatusColors()[0]);
                new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.menu_explanation).setView(explanation_dialog_view).setPositiveButton(R.string.know_it, null).show();
                break;
            case R.id.id_menu_set_theme:
                int ti;
                switch (getCurrAppTheme()) {
                    case 0x01:
                        ti = 1;
                        break;
                    case 0x10:
                        ti = 2;
                        break;
                    default:
                        ti = 0;
                        break;
                }
                cti = ti;
                final String[] themes = {getString(R.string.default_theme), getString(R.string.light), getString(R.string.dark)};
                new MaterialAlertDialogBuilder(MainActivity.this).setTitle(R.string.menu_settheme)
                        .setSingleChoiceItems(themes, ti, (dialog, which) -> cti = which)
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                            if (cti != ti) {
                                if (cti == 0) {
                                    setCurrAppTheme(0x00);
                                    if ((ti == 1 && getDarkModeStatus(getSysUiMode())) || (ti == 2 && !getDarkModeStatus(getSysUiMode())))
                                        recreate();
                                } else if (cti == 1) {
                                    setCurrAppTheme(0x01);
                                    if (ti == 2 || getDarkModeStatus(getSysUiMode()))
                                        recreate();
                                } else if (cti == 2) {
                                    setCurrAppTheme(0x10);
                                    if (ti == 1 || !getDarkModeStatus(getSysUiMode()))
                                        recreate();
                                }
                            }
                        }).create().show();
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
                }).setNegativeButton(R.string.view_how_the_project, (dialogInterface, i) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/430CSJ/KiraFanCompatibilityChecker")))).setPositiveButton(R.string.know_it, null).show();
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    DeviceCardView deviceCardView = null;

    private void refreshChange() {

        if (mViewModel.getHasInit()) {
            mViewModel.checkXposed();
            mViewModel.checkBlacklistApps();
            mViewModel.checkSys();
            mViewModel.checkGsReCode();
        } else {
            mViewModel.initVM();
        }

        if (deviceCardView == null) {
            deviceCardView = new DeviceCardView(deviceResultCardView, mViewModel);
            mcvs.add(deviceCardView);
        }
    }
}
