package com.czy.sre.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Created by zhangyu on 2017/1/6.
 */

public class SREService extends AccessibilityService {
    private static final String TAG = "SREService";
    public final String weixinPackage = "com.tencent.mm";

    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        String packageName = accessibilityEvent.getPackageName().toString();
        if (weixinPackage.equals(packageName)) {//过滤包名
            int eventType = accessibilityEvent.getEventType();//事件类型
            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    List<CharSequence> texts = accessibilityEvent.getText();
                    for (CharSequence charSequence : texts) {
                        String text = charSequence.toString();
                        if (text.contains("红包")) {
                            //模拟打开通知栏消息
                            if (accessibilityEvent.getParcelableData() != null &&
                                    accessibilityEvent.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) accessibilityEvent.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    Log.d(TAG, "收到红包");
                                    pendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                //第二步：监听是否进入微信红包消息界面
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    String className = accessibilityEvent.getClassName().toString();
                    Log.i(TAG, "className = " + className);
                    if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                        //开始抢红包
                        getPacket();
                        Log.d(TAG, "开始抢红包");
                    } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                        //开始打开红包
                        openPacket();//这里只是点开了红包 还要再点一次"开"才可以打开领取红包
                        //TODO: 2017/1/7 判断已经点过的 不要再执行 
                        // TODO: 2017/1/7 再点一次开 领取红包
                        
                        Log.d(TAG, "开始打开红包");
                    }
                    break;
            }
        }


    }

    /**
     * 查找到
     */
    @SuppressLint("NewApi")
    private void openPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("抢红包");
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

    }

    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null)
            recycle(rootNode);
        else
            Log.e(TAG, "获取页面根节点为null");
    }

    /**
     * 打印一个节点的结构
     *
     * @param info
     */
    @SuppressLint("NewApi")
    public void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            if (info.getText() != null) {
                if ("领取红包".equals(info.getText().toString())) {
                    //这里有一个问题需要注意，就是需要找到一个可以点击的View
                    Log.i(TAG, "Click" + ",isClick:" + info.isClickable());
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null) {
                        Log.i(TAG, "parent isClick:" + parent.isClickable());
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }

                }
            }

        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }


    @Override
    public void onInterrupt() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        info.packageNames = new String[]{weixinPackage};//监听微信
        setServiceInfo(info);
        super.onServiceConnected();
    }
}
