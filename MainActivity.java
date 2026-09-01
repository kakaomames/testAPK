package com.kakao.deviceownerapp;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");
        
        // ローカルのassets/index.htmlを読み込み
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
    }

    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public boolean isDeviceOwner() {
            return devicePolicyManager.isDeviceOwnerApp(getPackageName());
        }

        @JavascriptInterface
        public void hidePackage(String packageName, boolean hide) {
            if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                devicePolicyManager.setApplicationHidden(adminComponent, packageName, hide);
                Log.d("DeviceOwner", packageName + " hidden: " + hide);
            }
        }

        @JavascriptInterface
        public void installApk(String apkPath) {
            try {
                File file = new File(apkPath);
                if (!file.exists()) return;

                PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                
                // createSession に修正
                int sessionId = packageInstaller.createSession(params);
                PackageInstaller.Session session = packageInstaller.openSession(sessionId);
                
                OutputStream out = session.openWrite("package", 0, file.length());
                InputStream in = new FileInputStream(file);
                byte[] buffer = new byte[65536];
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                }
                session.fsync(out);
                in.close();
                out.close();

                Intent intent = new Intent(mContext, MainActivity.class);
                session.commit(android.app.PendingIntent.getActivity(
                        mContext, sessionId, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE).getIntentSender());
                
                Log.d("DeviceOwner", "Install session committed: " + sessionId);
            } catch (Exception e) {
                Log.e("DeviceOwner", "Install failed", e);
            }
        }
    }
}
