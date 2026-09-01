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

        // 隊長の美しいHTMLパネルをそのままJava文字列として直書き！
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"ja\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                "    <meta name=\"description\" content=\"Gemini Programming Team - Device Owner Control Console\">\n" +
                "    <meta name=\"author\" content=\"カカオマメ\">\n" +
                "    <meta name=\"robots\" content=\"noindex, nofollow\">\n" +
                "    <meta name=\"format-detection\" content=\"telephone=no\">\n" +
                "    <meta name=\"theme-color\" content=\"#1a1a1a\">\n" +
                "    <title>Gemini Programming隊 - 管理コンソール</title>\n" +
                "    <style>\n" +
                "        body { background-color: #121212; color: #00ffcc; font-family: 'Courier New', monospace; margin: 0; padding: 16px; }\n" +
                "        .header { text-align: center; margin-bottom: 20px; }\n" +
                "        .logo { width: 80px; height: 80px; border-radius: 50%; border: 2px solid #00ffcc; margin-bottom: 10px; }\n" +
                "        h1 { font-size: 1.2rem; margin: 5px 0; }\n" +
                "        .card { background: #1e1e1e; border: 1px solid #333; border-radius: 8px; padding: 12px; margin-bottom: 15px; }\n" +
                "        input, button { width: 100%; padding: 10px; margin-top: 8px; background: #2a2a2a; color: #fff; border: 1px solid #00ffcc; border-radius: 4px; box-sizing: border-box; font-family: inherit; }\n" +
                "        button { background: #00ffcc; color: #121212; font-weight: bold; cursor: pointer; }\n" +
                "        button:active { background: #00b386; }\n" +
                "        #logArea { background: #000; color: #00ff00; padding: 10px; font-size: 0.8rem; height: 120px; overflow-y: scroll; border: 1px solid #444; border-radius: 4px; white-space: pre-wrap; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <img src=\"https://kakaomames.github.io/rei/logo.png\" alt=\"隊ロゴ\" class=\"logo\">\n" +
                "        <h1>Gemini Programming隊</h1>\n" +
                "        <p style=\"font-size: 0.8rem; color: #888;\">AQUOS wish2 制圧コントロールパネル</p>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "        <h3>🛡️ 権限ステータス</h3>\n" +
                "        <button onclick=\"checkOwner()\">デバイスオーナー確認</button>\n" +
                "        <p id=\"statusText\" style=\"margin-top: 8px; font-size: 0.9rem;\">未確認</p>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "        <h3>📦 APKサイレントインストール</h3>\n" +
                "        <input type=\"text\" id=\"apkPathInput\" placeholder=\"/sdcard/Download/app.apk\" value=\"/sdcard/Download/\">\n" +
                "        <button onclick=\"installApk()\">サイレントインストール実行</button>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "        <h3>🧊 アプリ凍結・隠蔽</h3>\n" +
                "        <input type=\"text\" id=\"pkgInput\" placeholder=\"パッケージ名 (例: com.nttdocomo.android...)\" value=\"\">\n" +
                "        <button onclick=\"toggleHidePackage(true)\">パッケージを隠蔽/凍結</button>\n" +
                "        <button onclick=\"toggleHidePackage(false)\" style=\"background:#ff5555; color:#fff;\">凍結を解除</button>\n" +
                "    </div>\n" +
                "    <div class=\"card\">\n" +
                "        <h3>📜 ミッションログ</h3>\n" +
                "        <div id=\"logArea\">初期化完了... 指令を待っています。</div>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        function log(tag, message) {\n" +
                "            const time = new Date().toLocaleTimeString();\n" +
                "            const logArea = document.getElementById('logArea');\n" +
                "            logArea.innerText += `\\n[${time}] [${tag}] ${message}`;\n" +
                "            logArea.scrollTop = logArea.scrollHeight;\n" +
                "            if (window.AndroidBridge) {\n" +
                "                AndroidBridge.missionLog(tag, message);\n" +
                "            }\n" +
                "        }\n" +
                "        function checkOwner() {\n" +
                "            if (window.AndroidBridge) {\n" +
                "                let isOwner = AndroidBridge.isDeviceOwner();\n" +
                "                document.getElementById('statusText').innerText = isOwner ? \"✅ デバイスオーナー有効 (完全掌握)\" : \"❌ 権限なし\";\n" +
                "                log(\"STATUS\", \"デバイスオーナー状態: \" + isOwner);\n" +
                "            } else {\n" +
                "                log(\"ERROR\", \"AndroidBridgeが接続されていません。\");\n" +
                "            }\n" +
                "        }\n" +
                "        function installApk() {\n" +
                "            const path = document.getElementById('apkPathInput').value;\n" +
                "            if (!path) { alert(\"APKのパスを入力してください！\"); return; }\n" +
                "            log(\"INSTALL\", \"サイレントインストール要求送信: \" + path);\n" +
                "            if (window.AndroidBridge) {\n" +
                "                AndroidBridge.installApkSilently(path);\n" +
                "            }\n" +
                "        }\n" +
                "        function toggleHidePackage(hide) {\n" +
                "            const pkg = document.getElementById('pkgInput').value;\n" +
                "            if (!pkg) { alert(\"パッケージ名を入力してください！\"); return; }\n" +
                "            log(\"PACKAGE\", `${pkg} の隠蔽ステータス変更 -> ${hide}`);\n" +
                "            if (window.AndroidBridge) {\n" +
                "                AndroidBridge.setPackageHidden(pkg, hide);\n" +
                "            }\n" +
                "        }\n" +
                "        window.onload = function() { checkOwner(); };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
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
        public void missionLog(String tag, String message) {
            Log.d("GeminiTeam-UI", "[" + tag + "] " + message);
        }

        @JavascriptInterface
        public boolean isDeviceOwner() {
            return devicePolicyManager.isDeviceOwnerApp(getPackageName());
        }

        @JavascriptInterface
        public void setPackageHidden(String packageName, boolean hide) {
            if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                devicePolicyManager.setApplicationHidden(adminComponent, packageName, hide);
                Log.d("DeviceOwner", "値変更: " + packageName + " hidden -> " + hide);
            }
        }

        @JavascriptInterface
        public void installApkSilently(String apkPath) {
            try {
                File file = new File(apkPath);
                if (!file.exists()) {
                    Log.e("DeviceOwner", "APKファイルが見つかりません: " + apkPath);
                    return;
                }

                PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                
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
                
                Log.d("DeviceOwner", "値変更: インストールセッション発行成功 ID=" + sessionId);
            } catch (Exception e) {
                Log.e("DeviceOwner", "インストール失敗", e);
            }
        }
    }
}

