package com.czy.sre.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Binder;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.czy.sre.constants.Constants;

import java.util.HashMap;
import java.util.List;

/**
 * 微信抢红包辅助
 * 存在的问题
 * 1.在聊天列表页面拿不到最新收到的消息内容。导致结果：如果不允许微信显示通知栏信息 收到红包时又不在聊天页面 会抢不到红包
 * 2.标识已抢红包存在问题。导致结果:当一个人连续发送描述内容相同的红包时，后面的红包不会被自动打开
 * Created by zhangyu on 2017/1/6.
 */

public class SREService extends AccessibilityService {
    private static final String TAG = "SREService";
    public final String weixinPackage = "com.tencent.mm";
    private HashMap<String, Boolean> snatched = new HashMap<>();
    private String currActivityName = "com.tencent.mm.ui.LauncherUI";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (!Constants.openFunction)
            return;
        String packageName = accessibilityEvent.getPackageName().toString();
        Log.d(TAG, "onAccessibilityEvent    className: " + accessibilityEvent.getClassName().toString());
        if (weixinPackage.equals(packageName)) {//过滤包名
            int eventType = accessibilityEvent.getEventType();

            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED://通知栏收到消息
                    List<CharSequence> texts = accessibilityEvent.getText();
                    Log.d(TAG, "texts.size() = " + texts.size() + "  ,eventType = " + accessibilityEvent.getEventType());
                    for (CharSequence charSequence : texts) {
                        String text = charSequence.toString();
                        Log.i(TAG, "通知栏收到消息 text = " + text);
                        if (text.contains("[微信红包]")) {
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
                    //在聊天列表页面拿不到最新收到的消息内容
//                    gotoChatting(accessibilityEvent);

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
            }

        }
    }

    @SuppressLint("NewApi")
    private void gotoChatting(AccessibilityEvent accessibilityEvent) {
        if (currActivityName.equals("com.tencent.mm.ui.LauncherUI")) {
            AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();//getRootInActiveWindow();
            recycleNode(nodeInfo, "[微信红包]", false);
        }
    }

    /**
     * 遍历一个节点
     *
     * @param nodeInfo     需要被遍历的节点
     * @param targetStr    要寻找的目标字符
     * @param reverseOrder 是否倒序遍历
     */
    private void recycleNode(AccessibilityNodeInfo nodeInfo, String targetStr, boolean reverseOrder) {
        if (nodeInfo == null) {
            Log.e(TAG, "recycleNode  nodeInfo == null");
            return;
        }
        if (nodeInfo.getChildCount() == 0) {

            if (nodeInfo.getText() == null) {
                Log.w(TAG, "recycleNode  nodeInfo.getText() == null");
                return;
            }
            Log.i(TAG, "nodeInfo.getText().toString() = " + nodeInfo.getText().toString());
            if (targetStr.equals(nodeInfo.getText().toString())) {
                if (nodeInfo.isClickable()) {
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } else {
                    AccessibilityNodeInfo parent = nodeInfo.getParent();
                    int count = 0;
                    while (!parent.isClickable() || parent == null) {
                        parent = parent.getParent();
                        count++;
                    }
                    Log.w(TAG, "get parent count = " + count);
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        } else {
            if (reverseOrder) {
                for (int i = nodeInfo.getChildCount() - 1; i >= 0; i--) {
                    AccessibilityNodeInfo node = nodeInfo.getChild(i);
                    recycleNode(node, targetStr, reverseOrder);
                }
            } else {
                for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                    AccessibilityNodeInfo node = nodeInfo.getChild(i);
                    recycleNode(node, targetStr, reverseOrder);
                }
            }
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
                    Log.d(TAG, "执行打开红包第二步 i = " + i);
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
                            String objStr = parent.toString();
                            int indexStart = objStr.indexOf("@");
                            int indexEnd = objStr.indexOf(";");
                            String markInfo = objStr.substring(indexStart, indexEnd) + "-" + luckyMoneyDesc;
                            Log.d(TAG, "markInfo = " + markInfo);
                            if (null == snatched.get(markInfo)) {
                                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                snatched.put(markInfo, true);//记录 已被抢过的id
                                Log.d(TAG, "执行打开红包第一步  markInfo = " + markInfo);
                            } else
                                Log.w(TAG, "红包已被抢过了  ");
                            break;
                        }
                        parent = parent.getParent();
                    }
                }
            }

        } else {
            for (int i = info.getChildCount() - 1; i >= 0; i--) {//从后往前遍历 先抢最新的
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
//        AccessibilityServiceInfo info = getServiceInfo();
//        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
//        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
//        info.notificationTimeout = 100;
//        info.packageNames = new String[]{weixinPackage};//监听微信
//        setServiceInfo(info);
        super.onServiceConnected();
    }

}
