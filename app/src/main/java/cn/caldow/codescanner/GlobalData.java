package cn.caldow.codescanner;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class GlobalData extends Application {
    private static HashMap<String, String> globalData;

    private int countActivity = 0;
    private boolean isBackground = false;
    private static boolean ifHead = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("GlobalData.onCreate", "init");
        initBackgroundCallBack();
        globalData = new HashMap<String, String>();
    }

    public static boolean setData(String key, String value) {
        try {
            globalData.put(key, value);
            Log.i("globalData.put", String.format("Key:%s Value:%s", key, value));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getData(String key) {
        try {
            return globalData.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean isAppOnHead() {
        return ifHead;
    }

    private void initBackgroundCallBack() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                countActivity++;
                if (countActivity == 1 && isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入前台");
                    isBackground = false;
                    //说明应用重新进入了前台
                    ifHead = true;
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                countActivity--;
                if (countActivity <= 0 && !isBackground) {
                    Log.e("MyApplication", "onActivityStarted: 应用进入后台");
                    isBackground = true;
                    //说明应用进入了后台
//                    setData("from", "");
                    ifHead = false;

                }

            }

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }

        });
    }
}
