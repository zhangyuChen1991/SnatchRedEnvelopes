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

import java.util.HashMap;
import java.util.List;

/**
 * Created by zhangyu on 2017/1/6.
 */

public class SREService extends AccessibilityService {
    private static final String TAG = "SREService";
    public final String weixinPackage = "com.tencent.mm";
    private HashMap<String, Boolean> snatched = new HashMap<>();
    String currActivityName = "com.tencent.mm.ui.LauncherUI";
    private String nowOpenNodeId;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        String packageName = accessibilityEvent.getPackageName().toString();
        if (weixinPackage.equals(packageName)) {//过滤包名
            int eventType = accessibilityEvent.getEventType();

            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://通知栏收到消息
                    List<CharSequence> texts = accessibilityEvent.getText();
                    Log.d(TAG, "texts.size() = " + texts.size() + "  ,eventType = " + accessibilityEvent.getEventType());
                    for (CharSequence charSequence : texts) {
                        String text = charSequence.toString();
                        Log.i(TAG, "text = " + text);
                        if (text.contains("微信红包")) {
                            //模拟打开通知栏消息
                            if (accessibilityEvent.getParcelableData() != null && accessibilityEvent.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) accessibilityEvent.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    Log.d(TAG, "通知栏 收到红包");
                                    pendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED://收到消息 窗口内容变化

                    Log.i(TAG, "TYPE_WINDOW_CONTENT_CHANGED  currActivityName = " + currActivityName);
                    //监听消息列表页面内容是否变化
//                    gotoChatting();

                    //监听是否进入微信红包消息界面
                    if (currActivityName.equals("com.tencent.mm.ui.LauncherUI")) {
                        //开始抢红包
                        getPacket();
                    } else if (currActivityName.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                        //开始打开红包
                        openPacket();
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    currActivityName = accessibilityEvent.getClassName().toString();
//                    Log.i(TAG, "TYPE_WINDOW_STATE_CHANGED  currActivityName = " + currActivityName);
//                    //监听是否进入微信红包消息界面
//                    if (currActivityName.equals("com.tencent.mm.ui.LauncherUI")) {
//                        //开始抢红包
//                        getPacket();
//                    } else if (currActivityName.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
//                        //开始打开红包
//                        openPacket();
//                    }
//                    break;
            }

        }
    }

    @SuppressLint("NewApi")
    private void gotoChatting() {
        if (currActivityName.equals("com.tencent.mm.ui.LauncherUI")) {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo != null) {
                String child0Text = nodeInfo.getChild(0).getText().toString();
                Log.d(TAG, "child0Text = " + child0Text);
                List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText("[微信红包]");
                if (nodes != null && nodes.size() > 0) {
                    AccessibilityNodeInfo n = nodes.get(0);
                    if (n != null && n.isClickable()) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "进入聊天页面  text:" + n.getText().toString());
                    }
                }
            } else
                Log.e(TAG, "gotoChatting  getRootInActiveWindow == null");
        }
    }


    /**
     * 查找到
     */
    @SuppressLint("NewApi")
    private void openPacket() {
        Log.d(TAG, "openPacket..");
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo n = nodeInfo.getChild(i);
                if (n != null && n.isClickable()) {
                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.d(TAG, "执行打开红包第二步");
                }
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
                    //领取红包这个控件不可点击 它的父类可以点击
                    AccessibilityNodeInfo parent = info.getParent();
                    String luckyMoneyDesc = parent.getChild(0).getText().toString();

                    while (parent != null) {
                        if (parent.isClickable()) {
                            nowOpenNodeId = parent.getViewIdResourceName();
                            String markInfo = nowOpenNodeId+"-"+luckyMoneyDesc;
                            if (null == snatched.get(markInfo)) {
                                Log.d(TAG, "nowOpenNodeId = " + nowOpenNodeId);
                                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                snatched.put(markInfo, true);//记录 已被抢过的id
                                Log.d(TAG, "执行打开红包第一步  parent.getViewIdResourceName() = " + parent.getViewIdResourceName());
                            } else
                                Log.w(TAG, "红包已被抢过了  " + nowOpenNodeId);
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
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        info.packageNames = new String[]{weixinPackage};//监听微信
        setServiceInfo(info);
        super.onServiceConnected();
    }
}
