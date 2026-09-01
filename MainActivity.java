package com.kakao.deviceownerapp;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 面倒なXMLレイアウトは使わず、WebViewを画面いっぱいに生成
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        // HTMLとJavaを接続するJavaScriptインターフェースを登録
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");
        
        // assets/index.html を読み込み
        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);

        // デバイスポリシーマネージャーとコンポーネントの初期化
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);
    }

    // HTMLのJavaScriptから叩かれるブリッジクラス
    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void missionLog(String tag, String message) {
            Log.d("GeminiTeam [" + tag + "]", message);
        }

        @JavascriptInterface
        public boolean isDeviceOwner() {
            return devicePolicyManager.isDeviceOwnerApp(getPackageName());
        }

        @JavascriptInterface
        public void setPackageHidden(String packageName, boolean hidden) {
            if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                devicePolicyManager.setApplicationHidden(adminComponent, packageName, hidden);
                missionLog("ACTION", packageName + " の凍結/隠蔽ステータス変更: " + hidden);
            } else {
                missionLog("ERROR", "デバイスオーナー権限がありません！");
            }
        }

        // デバイスオーナー権限によるAPKのサイレントインストール
        @JavascriptInterface
        public void installApkSilently(String apkPath) {
            if (!devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                missionLog("ERROR", "デバイスオーナー権限がないためサイレントインストールできません！");
                return;
            }

            try {
                File file = new File(apkPath);
                if (!file.exists()) {
                    missionLog("ERROR", "指定されたパスにAPKが見つかりません: " + apkPath);
                    return;
                }

                PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                
                int sessionId = packageInstaller.create(params);
                try (OutputStream out = packageInstaller.openSession(sessionId).openWrite("package", 0, -1);
                     InputStream in = new FileInputStream(file)) {
                    byte[] buffer = new byte[65536];
                    int c;
                    while ((c = in.read(buffer)) != -1) {
                        out.write(buffer, 0, c);
                    }
                    packageInstaller.openSession(sessionId).fsync(out);
                }

                Intent intent = new Intent(mContext, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
                packageInstaller.openSession(sessionId).commit(pendingIntent.getIntentSender());
                missionLog("ACTION", "サイレントインストールのコミット成功: " + apkPath);

            } catch (Exception e) {
                missionLog("ERROR", "サイレントインストール例外発生: " + e.getMessage());
            }
        }
    }
}

