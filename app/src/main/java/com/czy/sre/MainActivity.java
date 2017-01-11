package com.czy.sre;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;

import com.czy.sre.constants.Constants;
import com.czy.sre.service.SREService;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AccessibilityManager.AccessibilityStateChangeListener {
    @BindView(R.id.tv)
    TextView descTv;
    @BindView(R.id.ctr_btn)
    Button ctrlBtn;
    @BindView(R.id.set_btn)
    Button setBtn;

    private Intent intent;
    private AccessibilityManager accessibilityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        intent = new Intent(this, SREService.class);
        initResources();
        initView();
        updateServiceStatus();

    }

    private void initResources() {
        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);

    }

    private void initView() {
        ctrlBtn.setOnClickListener(this);
        setBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.set_btn://设置辅助功能
                Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(accessibleIntent);
                break;
            case R.id.ctr_btn://开启 关闭辅助功能
                if (!Constants.openFunction) {//如果当前是关闭状态 开启时检测服务是否在运行 没有则启动服务
                    if (!isServiceEnabled()) {
                        startService(intent);
                    }
                }
                Constants.openFunction = !Constants.openFunction;//切换状态
                updateServiceStatus();
                break;
        }
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        Log.d("Main", "updateServiceStatus   isServiceEnabled()  = " + isServiceEnabled());
        if (isServiceEnabled() && Constants.openFunction) {
            ctrlBtn.setText("关闭");
            descTv.setText("正在监听微信..");
        } else {
            ctrlBtn.setText("开启");
            descTv.setText(getResources().getString(R.string.use_direction) + "\n\n当前抢红包功能未开启..");
        }
    }

    private boolean isServiceEnabled() {
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.service.SREService")) {
                return true;
            }
        }
        return false;
    }
}
