package moe.csj430.checkkirafancompatibility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UpdateTask extends AsyncTask<String, String, String> {
    private Context context;
    private boolean isUpdateOnRelease;
    private boolean toShowToast;
    //public static final String updateUrl = "https://api.github.com/repos/geeeeeeeeek/WeChatLuckyMoney/releases/latest";
    public static final String updateUrl = "https://api.github.com/repos/430CSJ/KiraFanCompatibilityChecker/releases/latest";

    public UpdateTask(Context context, boolean needUpdate, boolean showToast) {
        this.context = context;
        this.isUpdateOnRelease = needUpdate;
        this.toShowToast = showToast;
        if (this.isUpdateOnRelease && this.toShowToast) Toast.makeText(context, context.getResources().getString(R.string.checking_new_version), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected String doInBackground(String... uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
            } else {
                // Close the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (Exception e) {
            return null;
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            JSONObject release = new JSONObject(result);

            // Get current version
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = pInfo.versionName;

            String latestVersion = release.getString("tag_name");
            boolean isPreRelease = release.getBoolean("prerelease");
            if (!isPreRelease && version.compareToIgnoreCase(latestVersion) >= 0) {
                // Your version is ahead of or same as the latest.
                if (this.isUpdateOnRelease && toShowToast)
                    Toast.makeText(context, R.string.update_already_latest, Toast.LENGTH_SHORT).show();
            } else {
                if (!isUpdateOnRelease && toShowToast) {
                    Toast.makeText(context, context.getString(R.string.update_new_seg1) + latestVersion + context.getString(R.string.update_new_seg3), Toast.LENGTH_LONG).show();
                    return;
                }
                final JSONObject frelease = release;
                final String flatestVersion = latestVersion;
                new AlertDialog.Builder(context).setTitle(context.getResources().getString(R.string.avail_update_found) + latestVersion).setMessage(R.string.do_update).setPositiveButton(R.string.download_and_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Need update.
                        try {
                            String downloadUrl = frelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");

                            // Give up on the fucking DownloadManager. The downloaded apk got renamed and unable to install. Fuck.
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            Toast.makeText(context, context.getString(R.string.update_new_seg1) + flatestVersion + context.getString(R.string.update_new_seg2), Toast.LENGTH_LONG).show();
                            context.startActivity(browserIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton(R.string.cancel_update, null).setNeutralButton(R.string.view_update_log, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/430CSJ/KiraFanCompatibilityChecker/releases")));
                    }
                }).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (this.isUpdateOnRelease && this.toShowToast)
                Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show();
        }
    }

    public void update() {
        super.execute(updateUrl);
    }
}
