//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ml.w568w.checkxposed.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import java.net.URISyntaxException;

/**
 * @author didikee
 */
public class AlipayDonate {
    private static final String INTENT_URL_FORMAT = "intent://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2F{payCode}%3F_s%3Dweb-other&_t=1472443966571#Intent;scheme=alipayqr;package=com.eg.android.AlipayGphone;end";

    public AlipayDonate() {
    }

    public static boolean startAlipayClient(Activity activity, String payCode) {
        return startIntentUrl(activity,INTENT_URL_FORMAT.replace("{payCode}", payCode));
    }

    public static boolean startIntentUrl(Activity activity, String intentFullUrl) {
        try {
            Intent e = Intent.parseUri(intentFullUrl, 1);
            activity.startActivity(e);
            return true;
        } catch (URISyntaxException var3) {
            var3.printStackTrace();
            return false;
        } catch (ActivityNotFoundException var4) {
            var4.printStackTrace();
            return false;
        }
    }
}
