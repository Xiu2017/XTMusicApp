package com.xiu.xtmusic;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.entity.Msg;
import com.xiu.utils.FileUtils;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.mApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AboutActivity extends AppCompatActivity {

    private int versionCode;
    private String versionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mApplication app = (mApplication) getApplicationContext();
        app.addActivity(this);

        initStatusBar();
        versionCode = getLocalVersion();
        versionName = getLocalVersionName();
    }

    BroadcastReceiver uBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what",0)){
                case Msg.UPDATE_ERROR :
                    TastyToast.makeText(AboutActivity.this, "检查更新失败", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                    break;
                case Msg.UPDATE_ISNEW:
                    TastyToast.makeText(AboutActivity.this, "已是最新版本", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
                    break;
                case Msg.UPDATE_NEW:
                    int vc = intent.getIntExtra("vc",0);
                    String vn = intent.getStringExtra("vn");
                    String desc = intent.getStringExtra("desc");
                    String size = intent.getStringExtra("size");
                    updateTips(vn, desc, size);
                    break;

            }
        }
    };

    //检查更新
    public void checkUpdate(View view) {
        TastyToast.makeText(AboutActivity.this, "正在检查更新", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
        String checkUrl = "https://raw.githubusercontent.com/Xiu2017/XTMusicApp/master/app/release/version.json";
        //构建一个请求对象
        Request request = new Request.Builder().url(checkUrl).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient().newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Intent uBroadcast = new Intent();
                uBroadcast.setAction("uBroadcast");
                uBroadcast.putExtra("what", Msg.UPDATE_ERROR);
                sendBroadcast(uBroadcast);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //通过response得到服务器响应内容
                String str = response.body().string();
                Log.d("str", str);
                try {
                    JSONObject json = new JSONObject(str);
                    int vc = json.getInt("versionCode");
                    String vn = json.getString("versionName");
                    String desc = json.getString("desc");
                    String size = json.getString("size");
                    if (vc == versionCode && vn.equals(versionName)) {
                        Intent uBroadcast = new Intent();
                        uBroadcast.setAction("uBroadcast");
                        uBroadcast.putExtra("what", Msg.UPDATE_ISNEW);
                        sendBroadcast(uBroadcast);
                    } else {
                        Intent uBroadcast = new Intent();
                        uBroadcast.setAction("uBroadcast");
                        uBroadcast.putExtra("what", Msg.UPDATE_NEW);
                        uBroadcast.putExtra("vc", vc);
                        uBroadcast.putExtra("vn", vn);
                        uBroadcast.putExtra("desc", desc);
                        uBroadcast.putExtra("size", size);
                        sendBroadcast(uBroadcast);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Intent uBroadcast = new Intent();
                    uBroadcast.setAction("uBroadcast");
                    uBroadcast.putExtra("what", Msg.UPDATE_ERROR);
                    sendBroadcast(uBroadcast);
                }
            }
        });
    }

    //提示更新
    public void updateTips(String versionName, String desc, String size) {
        //自定义控件
        AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
        //设置time布局
        builder.setTitle("检测到新版本");
        builder.setMessage("版本：" + versionName + "("+size+")\n更新内容：" + desc);
        builder.setPositiveButton("更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                update();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    //下载更新
    long mTaskId;
    public void update(){
        TastyToast.makeText(this, "正在后台下载更新", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
        String url = "https://github.com/Xiu2017/XTMusicApp/raw/master/app/release/app-release.apk";
        String path = new StorageUtil(this).innerSDPath()+"/XTMusic/Package/XTMusic.apk";
        File file = new File(path);
        if(file.exists()){
            file.delete();
        }
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir("/XTMusic/Package", "XTMusic.apk");
        //获取下载管理器
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        mTaskId = downloadManager.enqueue(request);

        Intent sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.MTASKID);
        sBroadcast.putExtra("mTaskId", mTaskId);
        sendBroadcast(sBroadcast);
    }

/*    private void checkIsAndroidO() {
        if (Build.VERSION.SDK_INT >= 26) {
            boolean b = getPackageManager().canRequestPackageInstalls();
            if (b) {
                installApk();//安装应用的逻辑(写自己的就可以)
            } else {
                //请求安装未知应用来源的权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
            }
        } else {
            installApk();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case INSTALL_PACKAGES_REQUESTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    installApk();
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GET_UNKNOWN_APP_SOURCES:
                checkIsAndroidO();
                break;

            default:
                break;
        }
    }*/

    /**
     * 获取本地软件版本号
     */
    public int getLocalVersion() {
        int localVersion = 20180626;
        try {
            PackageInfo packageInfo = getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            localVersion = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    /**
     * 获取本地软件版本号名称
     */
    public String getLocalVersionName() {
        String localVersion = "1.8";
        try {
            PackageInfo packageInfo = getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }


    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
        win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }

    //打开开发者微博主页
    public void openSina(View view) {
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.sina.weibo", "com.sina.weibo.page.ProfileInfoActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        intent.putExtra("uid", "45xiuqing");
        //存在
        if ((getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null)) {
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                TastyToast.makeText(this, "你还没有安装微博", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("uBroadcast");
        registerReceiver(uBroadcast, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //注销广播接收者
        unregisterReceiver(uBroadcast);
    }
}
