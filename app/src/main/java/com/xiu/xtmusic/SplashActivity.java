package com.xiu.xtmusic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.xiu.utils.CheckPermission;
import com.xiu.utils.JSSecret;
import com.xiu.utils.NotificationsUtils;
import com.xiu.utils.PicUtils;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.mApplication;

import java.io.File;
import java.io.IOException;

public class SplashActivity extends Activity {

    private mApplication app;
    private final int SPLASH_DISPLAY_LENGHT = 500;
    private Handler handler;

    //==========权限相关==========//
    private static final int REQUEST_CODE = 0;  //请求码
    private CheckPermission checkPermission;  //检测权限器

    //配置需要取的权限
    static final String[] PERMISSION = new String[]{
            Manifest.permission.RECORD_AUDIO,  //录音权限
            Manifest.permission.WRITE_EXTERNAL_STORAGE,  // 写入权限
            Manifest.permission.READ_PHONE_STATE,  //电话状态读取权限
            Manifest.permission.INTERNET  //网络访问权限
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //dao = new MusicDao(this);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
        handler = new Handler();

        createNoMedia();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 延迟SPLASH_DISPLAY_LENGHT时间然后跳转到MainActivity
        //app.setmList(dao.getMusicData());
        //tips();
        getPermission();
    }

    //权限的获取
    public void getPermission() {
        if (checkPermission == null) {
            checkPermission = new CheckPermission(SplashActivity.this);
        }
        //缺少权限时，进入权限设置页面
        NotificationManagerCompat manager = NotificationManagerCompat.from(app);
        boolean isOpened = manager.areNotificationsEnabled();
        if (checkPermission.permissionSet(PERMISSION)) {
            startPermissionActivity();
        } else if(!isOpened) {
            tips();
        }else {
            toMainActivity();
        }
    }

    //去到主页
    public void toMainActivity(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DISPLAY_LENGHT);
    }

    //提示用户权限的用途
    AlertDialog dialog;
    public void tips(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle(R.string.tips);//提示帮助
        //builder.setMessage(R.string.context);
        builder.setTitle("通知栏权限");
        builder.setMessage("当前应用没有开启通知权限，将无法在状态栏控制播放");

        //打开设置，让用户选择打开权限
        builder.setPositiveButton("去开启", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startAppSettings();
            }
        });
        builder.setNegativeButton("不用了", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                toMainActivity();
            }
        });
        builder.setCancelable(false);
        dialog = builder.show();
    }

    //创建.nomedia
    public void createNoMedia() {
        String innerSD = new StorageUtil(this).innerSDPath();
        String path = innerSD + "/XTMusic/AlbumImg/.nomedia";
        //所有图片转换为jpg
        //PicUtils.ImgToJPG(new File(innerSD + "/XTMusic/AlbumImg/"));
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //禁用按键事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //打开系统应用设置(ACTION_APPLICATION_DETAILS_SETTINGS:系统设置权限)
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    //进入权限设置页面
    private void startPermissionActivity() {
        PermissionActivity.startActivityForResult(this, REQUEST_CODE, PERMISSION);
    }

    //返回结果回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //拒绝时，没有获取到主要权限，无法运行，关闭页面
/*        if (requestCode == REQUEST_CODE && resultCode == PermissionActivity.PERMISSION_DENIEG) {
            finish();
        }*/
        finish();
    }

    @Override
    protected void onPause() {
        if(dialog != null){
            dialog.dismiss();
        }
        super.onPause();
    }
}
