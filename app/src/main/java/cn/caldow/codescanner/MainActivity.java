package cn.caldow.codescanner;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends Activity {

    private static final int CAMERA_REQ_CODE = 100000;
    private static final int REQUEST_CODE_SCAN_ONE = 9999;

    private int onTimes = 0;

    final int PERMISSIONS_LENGTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //加载历史记录
        EditText edt = findViewById(R.id.editTextTextMultiLine2);
        edt.setKeyListener(null);
        String s = getUrls();
        edt.setText(Objects.equals(s, "") ? "历史记录读取失败" : s);

        // 设置全局异常捕获
        String androidId = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
        strategy.setDeviceID(androidId);
        strategy.setAppVersion("1.0.0");
        strategy.setAppPackageName("cn.caldow.codescanner");
        strategy.setCrashHandleCallback(new CrashReport.CrashHandleCallback() {
            public Map<String, String> onCrashHandleStart(int crashType, String errorType,
                                                          String errorMessage, String errorStack) {
                Toast.makeText(getApplicationContext(), "出现致命错误,程序即将退出", Toast.LENGTH_LONG).show();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return new LinkedHashMap<String, String>();
            }
        });
        CrashReport.initCrashReport(this, "efe91d7c7b", true, strategy);
//        Toast.makeText(this, "全局异常捕获加载完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Objects.equals(GlobalData.getData("from"), "shortcut")) {
            GlobalData.setData("from", "");
            finish();
        }
    }


    // To check if service is enabled
    private boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = getPackageName() + "/" + ClickSimu.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.contains(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public void applyAuth(View view) {
        if (checkPermission()) {
            Toast.makeText(this, "权限已全部获取", Toast.LENGTH_SHORT).show();
            return;
        }
        this.requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INSTALL_SHORTCUT,
                Manifest.permission.UNINSTALL_SHORTCUT,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, CAMERA_REQ_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 判断“requestCode”是否为申请权限时设置请求码CAMERA_REQ_CODE，然后校验权限开启状态
        if (requestCode == CAMERA_REQ_CODE && grantResults.length == PERMISSIONS_LENGTH && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // 调用扫码接口，构建扫码能力
            System.out.println("获取授权成功");
            Toast.makeText(this, "获取授权成功", Toast.LENGTH_SHORT).show();
            ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, null);
        }
    }

    public void createShotcut(View v) {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager.isRequestPinShortcutSupported()) {
            Intent intent = new Intent(this, CameraScan.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("from", "desktop");
            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, "my-shortcut")
                    .setShortLabel("测码")
                    .setLongLabel("测码")
                    .setIcon(Icon.createWithResource(this, R.drawable.shortcut))
                    .setIntent(intent)
                    .build();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("cn.caldow.codescanner.PINNED_BROADCAST");
            PinnedReceiver receiver = new PinnedReceiver();
            registerReceiver(receiver, intentFilter);

            Intent pinnedShortcutCallbackIntent = new Intent("cn.caldow.codescanner.PINNED_BROADCAST");
            PendingIntent successCallback = PendingIntent.getBroadcast(this, 0,
                    pinnedShortcutCallbackIntent, 0);
            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.getIntentSender());
        }
    }

    public static class PinnedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "创建快捷方式成功", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkPermission() {
        String[] perms = {Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INSTALL_SHORTCUT,
                Manifest.permission.UNINSTALL_SHORTCUT,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE
        };

        for (String perm : perms) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public String getUrls() {
        String[] res = new String[0];
        try {
            String filename = "urls.txt";
            File file = new File(getExternalFilesDir(null), filename);
            FileReader reader = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            reader.read(buffer);
            String text = new String(buffer);
            reader.close();
            return text.replace("\n", "\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void ts(View v) {
        String uri;
        Intent jump = new Intent(Intent.ACTION_VIEW);
        uri="openjd://virtual?params={\"category\":\"jump\",\"des\":\"saoasao\"}";
        //uri = "openjd://virtual?params={\"des\":\"productList\",\"keyWord\":\"杯子\",\"from\":\"search\",\"category\":\"jump\"}";
        jump.setData(Uri.parse(uri));
        startActivity(jump);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String textData = data.getStringExtra("textData");
            }
        }
    }

    /**
     * 需要在onCreate和onNewIntent都执行
     */
    private void checkThirdParty(Intent intent) {
        String key = intent.getStringExtra("key");
        String pn = intent.getStringExtra("packageName");
        Log.e(TAG, "跳转结果new，包名：" + pn + "，key：" + key);
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(pn)) {
            //没有key说明该app未对接
            if (intent.getSourceBounds() != null) {
                Log.e(TAG, "launcher启动");
                Log.i("ssss", intent.getSourceBounds().toString());
            } else {
                Log.e(TAG, "未对接的app启动");
            }
        } else {
            if (key.equals("通知栏")) {
                Log.e(TAG, "通知栏启动");
            } else {
                Log.e(TAG, "已对接第三方启动：" + pn + "，key：" + key);
            }
        }
    }
}

