package moe.csj430.checkkirafancompatibility.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ContentLoadingProgressBar;

import com.google.android.material.card.MaterialCardView;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.List;

import moe.csj430.checkkirafancompatibility.R;

public class ResultCardView extends MaterialCardView {
    public ResultCardView(Context context) {
        super(context);
    }

    public ResultCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initArr() {
        int elCnt = 3;
        notEmpty = new ArrayList<>();
        detailELs = new ArrayList<>();
        for (int i = 0; i < elCnt; ++i)
            notEmpty.add(false);
    }

    private List<Boolean> notEmpty;
    private TextView titleTV;
    private ExpandableLayout infoEL;
    private List<ExpandableLayout> detailELs;
    private ImageView statusIV;
    private ImageView arrowIV;
    private ImageView refreshIV;
    private ImageView infoIV;
    private ContentLoadingProgressBar progressBar;

    public ContentLoadingProgressBar getProgressBar() {
        return progressBar;
    }

    public TextView getTitleTV() {
        return titleTV;
    }

    public List<Boolean> getNotEmpty() {
        return notEmpty;
    }

    public ExpandableLayout getInfoEL() {
        return infoEL;
    }

    public List<ExpandableLayout> getDetailELs() {
        return detailELs;
    }

    public ImageView getArrowIV() {
        return arrowIV;
    }

    public ImageView getRefreshIV() {
        return refreshIV;
    }

    public ImageView getInfoIV() {
        return infoIV;
    }

    public ImageView getStatusIV() {
        return statusIV;
    }

    public void init(Context context) {
        initArr();
        progressBar = findViewById(R.id.clpb_rcv);
        titleTV = findViewById(R.id.tv_title);
        infoEL = findViewById(R.id.el_info);
        detailELs.add((ExpandableLayout)findViewById(R.id.el_detail_0));
        detailELs.add((ExpandableLayout)findViewById(R.id.el_detail_1));
        detailELs.add((ExpandableLayout)findViewById(R.id.el_detail_2));
        statusIV = findViewById(R.id.status_img);
        arrowIV = findViewById(R.id.iv_arrow);
        refreshIV = findViewById(R.id.iv_refresh);
        infoIV = findViewById(R.id.iv_info);
        RelativeLayout headerRL = findViewById(R.id.rl_header);
        headerRL.setOnClickListener(v -> showHideDetail());
        infoIV.setOnClickListener(v -> showHideInfo());
    }

    public String getString() {
        return null;
    }

    private void showHideInfo() {
        if (infoEL != null) {
            if (infoEL.isExpanded()) {
                infoEL.collapse();
                infoIV.setBackgroundColor(Color.TRANSPARENT);
            } else {
                infoEL.expand();
                infoIV.setBackgroundColor(getContext().getResources().getColor(R.color.primary_color_dark));
            }
        }
    }

    private void showHideDetail() {
        List<Integer> neelis = new ArrayList<>();
        int eli = 0;
        for (Boolean b : notEmpty) {
            if (b) {
                neelis.add(eli);
            }
            ++eli;
        }
        if (neelis.size() > 0) {
            if (detailELs.get(neelis.get(neelis.size() - 1)).isExpanded()) {
                for (Integer i : neelis)
                    detailELs.get(i).collapse();
                arrowIV.setRotation(0.0f);
            } else {
                if (detailELs.get(0).isExpanded() && detailELs.get(1).isExpanded()) {
                    detailELs.get(2).expand();
                    arrowIV.setRotation(180.0f);
                } else {
                    detailELs.get(0).expand();
                    detailELs.get(1).expand();
                    if (!notEmpty.get(0) && !notEmpty.get(1) && notEmpty.get(2))
                        detailELs.get(2).expand();
                    if (!notEmpty.get(2))
                        arrowIV.setRotation(180.0f);
                }
            }
            if (notEmpty.get(2) && detailELs.get(2).isExpanded())
                arrowIV.setRotation(180.0f);
        }
    }
}
