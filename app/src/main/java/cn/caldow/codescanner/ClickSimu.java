package cn.caldow.codescanner;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class ClickSimu extends AccessibilityService{

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        System.out.println("获取到辅助权限");
        System.out.println(accessibilityEvent.isEnabled());
    }

    @Override
    public void onInterrupt() {

    }

}