package com.xiu.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import com.danikula.videocache.HttpProxyCacheServer;
import com.xiu.entity.Music;
import com.xiu.service.MusicService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 应用上下文
 */

public class mApplication extends Application {

    private Intent sIntent;  //音乐服务
    private List<Music> mList;  //正在播放的列表
    private int idx;  //正在播放歌曲编号
    private MusicService mService;  //音乐服务
    private MediaPlayer mp;
    private boolean mobileConnected;  //是否使用移动网络播放
    private int playlist = 0;  //当前播放的列表
    private int playmode = 0;  //播放模式
    private HttpProxyCacheServer proxy;  //缓存
    private Timer timer;
    private Task task;
    private boolean deleteMode = false;

    //缓存开源框架：https://github.com/danikula/AndroidVideoCache
    //获取Proxy
    public static HttpProxyCacheServer getProxy(Context context) {
        mApplication app = (mApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    //Proxy设置
    private HttpProxyCacheServer newProxy() {
        //缓存路径
        String innerPath = new StorageUtil(this).innerSDPath();
        innerPath = innerPath + "/Android/data/com.xiu.xtmusic/cache/";
        File file = new File(innerPath);
        if(!file.exists()){
            file.mkdirs();
        }
        return new HttpProxyCacheServer.Builder(this)
                .maxCacheSize(1024 * 1024 * 1024)  //1G 缓存
                .maxCacheFilesCount(500)  //最多缓存500首歌曲
                .cacheDirectory(file)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 程序启动的时候开始服务
        sIntent = new Intent(this, MusicService.class);
        startService(sIntent);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //加载js
                if (JSSecret.invIsNull()) {
                    JSSecret.init(mApplication.this);
                }
            }
        }).start();

        //执行周期任务检测Service状态
        timer = new Timer();
        task = new Task();
        timer.schedule(task, 10000,5000);
    }

    //当Service被系统杀死时，退出程序
    class Task extends TimerTask {
        @Override
        public void run() {
            if(!isServiceRunning(mApplication.this,"com.xiu.service.MusicService")){
                //TastyToast.makeText(mApplication.this, "炫听音乐已被系统清除", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
                //Log.d("mApplication","程序退出");
                onTerminate();
            }
        }
    };

    //自定义释放资源
    public void onDestroy() {
        mService.onDestroy();  //释放服务资源
        stopService(sIntent);  //停止服务
        timer.cancel();
        task.cancel();
    }

    //自定义Activity栈
    private List<Activity> activities = new ArrayList<>(),
            splash = new ArrayList<>();
    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        for (Activity activity : activities) {
            activity.finish();
        }
        onDestroy();
        System.exit(0);
    }

    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }

    public List<Music> getmList() {
        return mList;
    }

    public void setmList(List<Music> mList) {
        this.mList = mList;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public MusicService getmService() {
        return mService;
    }

    public void setmService(MusicService mService) {
        this.mService = mService;
    }

    public MediaPlayer getMp() {
        return mp;
    }

    public void setMp(MediaPlayer mp) {
        this.mp = mp;
    }

    public boolean isMobileConnected() {
        return mobileConnected;
    }

    public void setMobileConnected(boolean mobileConnected) {
        this.mobileConnected = mobileConnected;
    }

    public int getPlaylist() {
        return playlist;
    }

    public void setPlaylist(int playlist) {
        this.playlist = playlist;
    }

    public int getPlaymode() {
        return playmode;
    }

    public void setPlaymode(int playmode) {
        this.playmode = playmode;
    }

    public boolean isDeleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
    }
}
