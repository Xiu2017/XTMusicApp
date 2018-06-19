package com.xiu.xtmusic;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.danikula.videocache.HttpProxyCacheServer;
import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.adapter.MainPagerAdapter;
import com.xiu.adapter.MusicListAdapter;
import com.xiu.dao.MusicDao;
import com.xiu.dialog.ItemMenuDialog;
import com.xiu.dialog.MusicInfoDialog;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.utils.AudioUtil;
import com.xiu.utils.FileSizeUtil;
import com.xiu.utils.FileUtils;
import com.xiu.utils.ImageUtil;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.TimeFormatUtil;
import com.xiu.utils.mApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    //全局变量
    private boolean isTimer;
    private boolean update;
    private MusicDao dao;
    private MenuItem timerItem, speedItem, bassItem, reverbItem, cacheItem;
    private Dialog dialog;
    private ContentResolver contentResolver;
    private MyObserver observer;
    private ProgressBar currentTime;
    private Intent sBroadcast;
    private TextView title, artist, musicSize, musicName;
    private ImageView playBtn, album, hunt;
    private mApplication app;
    private List<Music> list, historyData;
    private BaseAdapter adapter, historyAdapter;
    private ListView musicList, historyList;
    private LinearLayout emptyList;
    private DrawerLayout drawer;  //侧边栏
    private ObjectAnimator rotation;  //专辑旋转动画
    private LinearLayout group;  //顶部的选项卡
    private View localTab, historyTab;  //选项卡：本地，历史
    private ViewPager viewPager;  //页视图

    //private float speed = 1.00f;  //播放速度
    //private float pitch = 1.00f;  //音调

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        setContentView(R.layout.activity_main);
        dao = new MusicDao(this);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);

        initNavigationView();  //初始化NavigationView
        initStatusBar();  //初始化沉浸式状态栏
        initViewPager();  //初始化viewPager
        initView();  //初始化布局元素
        initList();  //初始化列表
        initRegister();  //注册内容观察者,当媒体数据库发生变化时,更新音乐列表
        initAnim();  //初始化专辑旋转动画
        initData();  //初始化持久化数据
        cacheSize();  //计算缓存大小
    }

    //初始化持久化数据
    public void initData() {
        SharedPreferences pref = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
        float speed = pref.getFloat("speed", 1.0f);
        int bass = pref.getInt("bass", 0);
        int reverb = pref.getInt("reverb", 0);

        speedItem.setTitle("播放速度 " + (float) (Math.round(speed * 100)) / 100);
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.PLAY_SPEED);
        sBroadcast.putExtra("speed", speed);
        sendBroadcast(sBroadcast);

        bassItem.setTitle("低音增益 " + bass);
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.BASS_LEVEL);
        sBroadcast.putExtra("bass", bass);
        sendBroadcast(sBroadcast);

        reverbItem.setTitle("混响 [ " + reverbToStr(reverb) + " ]");
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.REVERB_LEVEL);
        sBroadcast.putExtra("reverb", reverb);
        sendBroadcast(sBroadcast);
    }

    //计算缓存大小
    public void cacheSize() {
        String innerPath = new StorageUtil(this).innerSDPath();
        innerPath = innerPath + "/Android/data/com.xiu.xtmusic/cache/";
        File file = new File(innerPath);
        if(file.exists()){
            String size = FileSizeUtil.getAutoFileOrFilesSize(innerPath);
            cacheItem.setTitle("清除缓存 " + size);
        }
    }

    //初始化viewPager
    public void initViewPager() {
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        //查找布局文件
        LayoutInflater inflater = getLayoutInflater();
        localTab = inflater.inflate(R.layout.layout_list, null);
        historyTab = inflater.inflate(R.layout.layout_list, null);

        //将view装入数组中
        List<View> pages = new ArrayList<>();  //所有页面
        pages.add(localTab);
        pages.add(historyTab);
        //绑定适配器
        viewPager.setAdapter(new MainPagerAdapter(pages));
        //添加监听器
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                //获取本地和历史选项卡
                TextView local = (TextView) group.getChildAt(0);
                TextView history = (TextView) group.getChildAt(1);
                switch (position) {
                    case 0:
                        //改变选项卡样式
                        local.setTextColor(Color.parseColor("#FFFFFFFF"));
                        history.setTextColor(Color.parseColor("#99FFFFFF"));
                        adapter.notifyDataSetChanged();
                        if (list != null && list.size() == 0)
                            emptyList.setVisibility(View.VISIBLE);
                        else
                            emptyList.setVisibility(View.GONE);
                        break;
                    case 1:
                        //改变选项卡样式
                        local.setTextColor(Color.parseColor("#99FFFFFF"));
                        history.setTextColor(Color.parseColor("#FFFFFFFF"));
                        historyAdapter.notifyDataSetChanged();
                        if (historyData != null && historyData.size() == 0)
                            emptyList.setVisibility(View.VISIBLE);
                        else
                            emptyList.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    //初始化专辑旋转动画
    public void initAnim() {
        ImageView album = (ImageView) findViewById(R.id.album);
        rotation = ObjectAnimator.ofFloat(album, "rotation", 0.0F, 359.9F);
        rotation.setRepeatCount(-1);
        rotation.setDuration(30000);
        rotation.setInterpolator(new LinearInterpolator());
    }

    //专辑旋转动画控制
    private final static int STOP = 0;
    private final static int START = 1;

    public void albumRotate(int i) {
        ;
        if (i == 0) {
            if (rotation.isRunning() || rotation.isStarted())
                rotation.pause();
        } else {
            if (rotation.isPaused())
                rotation.resume();
            else
                rotation.start();
        }
    }

    //初始化NavigationView
    public void initNavigationView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        timerItem = navigationView.getMenu().getItem(1);
        speedItem = navigationView.getMenu().getItem(2);
        bassItem = navigationView.getMenu().getItem(3);
        reverbItem = navigationView.getMenu().getItem(4);
        cacheItem = navigationView.getMenu().getItem(5);
    }

    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();

        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
        //win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }

    //注册内容观察者
    public void initRegister() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        observer = new MyObserver(new Handler());
        contentResolver = getContentResolver();
        contentResolver.registerContentObserver(uri, true, observer);
    }

    //初始化列表
    public void initList() {
        list = new ArrayList<>();
        adapter = new MusicListAdapter(list, this);
        musicList.setAdapter(adapter);
        historyData = new ArrayList<>();
        historyAdapter = new MusicListAdapter(historyData, this);
        historyList.setAdapter(historyAdapter);
        AbsListView.OnScrollListener mScroll = new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case 1:
                        hunt.clearAnimation();
                        hunt.setVisibility(View.VISIBLE);
                        break;
                    case 0:
                        AlphaAnimation alphaAnimation = (AlphaAnimation) AnimationUtils.loadAnimation(MainActivity.this, R.anim.hunt_push_out);
                        hunt.startAnimation(alphaAnimation);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };
        musicList.setOnScrollListener(mScroll);
        historyList.setOnScrollListener(mScroll);
    }

    //初始化布局元素
    public void initView() {
        group = (LinearLayout) findViewById(R.id.group);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        playBtn = (ImageView) findViewById(R.id.playBtn);
        title = (TextView) findViewById(R.id.title);
        artist = (TextView) findViewById(R.id.artist);
        album = (ImageView) findViewById(R.id.album);
        //mainMenu = findViewById(R.id.mainMenu);
        currentTime = (ProgressBar) findViewById(R.id.currentTime);
        musicList = (ListView) localTab.findViewById(R.id.musicList);
        historyList = (ListView) historyTab.findViewById(R.id.musicList);
        emptyList = (LinearLayout) findViewById(R.id.emptyList);
        hunt = (ImageView) findViewById(R.id.hunt);
        musicSize = (TextView) findViewById(R.id.musicSize);
        musicName = (TextView) findViewById(R.id.musicName);

        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                cacheSize();
            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    //viewpager切换到指定item
    public void switchItem(View view) {
        int item = 0;
        switch (view.getId()) {
            case R.id.group_local:  //切换到了本地类别
                item = 0;
                break;
            case R.id.group_history:  //切换到了历史列表
                item = 1;
                break;
        }
        //切换页面
        viewPager.setCurrentItem(item, true);
    }

    //清除定时器
    public void timerClear() {
        isTimer = false;
        timerItem.setTitle("定时退出");
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.TIMER_CLEAR);
        sendBroadcast(sBroadcast);
    }

    //选择定时退出时间
    public void timerSelect() {
        //自定义控件
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final View view = getLayoutInflater().inflate(R.layout.layout_time_dialog, null);
        final TimePicker timePicker = (TimePicker) view.findViewById(R.id.time_picker);
        //初始化时间
        timePicker.setCurrentHour(0);
        timePicker.setCurrentMinute(20);
        timePicker.setIs24HourView(true);
        //设置time布局
        builder.setView(view);
        builder.setTitle("设置定时退出时间");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int mHour = timePicker.getCurrentHour();
                int mMinute = timePicker.getCurrentMinute();
                int time = (mHour * 60 + mMinute) * 60 * 1000;

                CheckBox timerCk = (CheckBox) view.findViewById(R.id.timerCk);
                boolean checked = timerCk.isChecked();

                timerExit(time, checked);
                TastyToast.makeText(MainActivity.this, (mHour * 60 + mMinute) + "分钟后将退出应用", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
                dialog.cancel();
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

    //通知Service开启定时退出任务
    public void timerExit(int time, boolean checked) {
        isTimer = true;
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.TIMER_EXIT);
        sBroadcast.putExtra("time", time);
        sBroadcast.putExtra("checked", checked);
        sendBroadcast(sBroadcast);
    }

    //广播接收
    BroadcastReceiver mBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.PLAY_COMPLETION:  //播放完成
                    //改变样式
                    refresh();
                    break;
                case Msg.CURRENTTIME:  //刷新进度
                    currentTime(intent.getIntExtra("current", 0));
                    long time = intent.getLongExtra("time", 0);
                    if (time != 0) {
                        refreshTime(time);
                    }
                    break;
            }

            //下载进度
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                DownloadManager.Query query = new DownloadManager.Query();
                //在广播中取出下载任务的id
                query.setFilterById(mTaskId);
                Cursor c = manager.query(query);
                if (c.moveToFirst()) {
                    TastyToast.makeText(MainActivity.this, "音乐下载完成，保存在/XTMusic/Music目录下", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
                }
            }
        }
    };

    //刷新剩余退出时间
    int interval = 10;

    public void refreshTime(long time) {
        time = time - System.currentTimeMillis();
        if (isTimer) {
            if (time > 0) {
                if (interval == 10) {  //刷新间隔，10为一秒
                    interval = 1;
                    timerItem.setTitle("取消定时器（" + TimeFormatUtil.secToTime((int) (time / 1000)) + "）");
                } else {
                    interval += 1;
                }
            } else {
                timerItem.setTitle("取消定时器（00:00）");
            }
        }
    }

    //点击事件
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.list_item:  //点击了item
                clickItem(view);
                break;
            case R.id.hunt:  //点击了定位歌曲
                hunt();
                break;
/*            case R.id.mainMenu:  //点击了菜单
                createMenu();
                break;*/
            case R.id.item_menu:  //点击了item菜单
                LinearLayout layout = (LinearLayout) view.getParent();
                TextView num = (TextView) layout.findViewById(R.id.musicNum);
                Music music = getMusicByNum(view);
                dialog = new ItemMenuDialog(this, music, num.getText().toString());
                dialog.show();
                break;
            case R.id.menu_download:  //下载音乐
                dialog.dismiss();
                downloadMusic(getMusicByNum(view));
                break;
            case R.id.menu_share:  //分享音乐
                dialog.dismiss();
                shareMusic(getMusicByNum(view));
                break;
            case R.id.menu_ring:  //设为铃声
                dialog.dismiss();
                setRing(view);
                break;
            case R.id.menu_info:
                dialog.dismiss();
                dialog = new MusicInfoDialog(this, getMusicByNum(view));
                dialog.show();
                break;
            case R.id.menu_delete:  //删除歌曲
                dialog.dismiss();
                delMusic(getMusicByNum(view));
                break;
            case R.id.search:  //搜索在线歌曲
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
                break;
        }
    }


    //定位歌曲
    public void hunt() {
        if (app.getPlaylist() == 0) {
            viewPager.setCurrentItem(0, true);
            musicList.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
            musicList.setSelection(app.getIdx() > 4 ? app.getIdx() - 4 : 0);
        } else if (app.getPlaylist() == 1) {
            viewPager.setCurrentItem(1, true);
            historyList.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
            historyList.setSelection(app.getIdx() > 4 ? app.getIdx() - 4 : 0);
        }
    }

    //下载音乐
    long mTaskId;

    public void downloadMusic(Music music) {
        //下载链接校验，防止用户操作过快
        if (!music.getPath().contains("http://")) return;

        //读取缓存
        String path = music.getPath();
        HttpProxyCacheServer proxy = app.getProxy(this);
        if (proxy.isCached(path)) {
            path = proxy.getProxyUrl(music.getPath()).replace("file://", "");
            String toPath = new StorageUtil(this).innerSDPath() + "/XTMusic/Music/" + music.getName();
            FileUtils.copyFile(path, toPath);
            //扫描到媒体库
            MediaScannerConnection.scanFile(this, new String[]{toPath}, null, null);
            TastyToast.makeText(this, "音乐下载完成，保存在/XTMusic/Music目录下", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
            return;
        }

        TastyToast.makeText(this, "开始下载\"" + music.getArtist() + " - " + music.getTitle() + "\"，请在通知栏查看下载进度", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(music.getPath()));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir("/XTMusic/Music", music.getName());
        //获取下载管理器
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        mTaskId = downloadManager.enqueue(request);

        registerReceiver(mBroadcast, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    //设置手机铃声
    public void setRing(View view) {
        Music ring = getMusicByNum(view);
        String fromPath = ring.getPath();
        String toPath = new StorageUtil(this).innerSDPath() + "/XTMusic/Ringtone";
        FileUtils.delAllFile(toPath);
        toPath = toPath + "/XTMusicRingtone" + System.currentTimeMillis();
        FileUtils.copyFile(fromPath, toPath);
        new AudioUtil(this).setRing(RingtoneManager.TYPE_RINGTONE, toPath, ring.getTitle());
    }

    //删除歌曲
    public void delMusic(final Music music) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (music.getPath().contains("http://")) {
            builder.setMessage("确定要移除歌曲吗？");
        } else {
            builder.setMessage("确定要将歌曲从本地删除吗？");
        }
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                String extSD = new StorageUtil(MainActivity.this).extSDPath();
                boolean isUrl = music.getPath().contains("http://");
                File file = null;
                if (!isUrl) {
                    file = new File(music.getPath());
                    if (file.exists()) {
                        if (music.getPath().contains(extSD + "")) {
                            TastyToast.makeText(MainActivity.this, "暂不支持删除外置SD卡文件", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                            return;
                        } else if (file != null && file.delete()) {
                            TastyToast.makeText(MainActivity.this, "删除成功", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
                        }
                    }
                }

                //删除专辑图片
                String innerSDPath = new StorageUtil(MainActivity.this).innerSDPath();
                String name = music.getName();
                final String path = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
                file = new File(path);
                if (file.exists()) {
                    file.delete();
                }

                //从数据库和媒体库中删除
                dao.delMusicById(music);
                getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.DATA + "= \"" + music.getPath() + "\"",
                        null);

                int idx = app.getIdx() - 1;
                int delIdx = -1;
                switch (viewPager.getCurrentItem()) {
                    case 0:
                        delIdx = list.indexOf(music);
                        list.clear();
                        list.addAll(dao.getMusicData());
                        app.setmList(list);
                        break;
                    case 1:
                        delIdx = historyData.indexOf(music);
                        historyData.clear();
                        historyData.addAll(dao.selMusicByDate());
                        app.setmList(historyData);
                        break;
                }

                //app.getmList().remove(music);
                if (delIdx == idx && app.getmList().size() > 0 && app.getPlaylist() == viewPager.getCurrentItem()) {
                    app.setIdx(delIdx);
                    playNext();
                } else if (delIdx < idx) {
                    app.setIdx(idx);
                }

                adapter.notifyDataSetChanged();
                historyAdapter.notifyDataSetChanged();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //根据音乐编号获取音乐
    public Music getMusicByNum(View view) {
        LinearLayout layout1 = (LinearLayout) view.getParent();
        TextView textView = (TextView) layout1.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        if (viewPager.getCurrentItem() == 0) {
            return list.get(musicNum - 1);
        }
        return historyData.get(musicNum - 1);
    }

    //分享音乐
    public void shareMusic(Music music) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        if (music.getPath().contains("http://")) {
            String msg = "分享音乐：" + music.getArtist() + " - " + music.getTitle() + "\n" + music.getPath();
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "分享音乐链接"));
        } else {
            File file = new File(music.getPath());
            if (!file.exists()) {
                TastyToast.makeText(this, "文件不存在", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                return;
            }
            //Uri uri = Uri.fromFile(file);
            Uri uri = FileProvider.getUriForFile(this, "com.xiu.xtmusic.MainActivity", file);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType("audio/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "分享音乐文件"));
        }
    }

    //播放控制
    public void playControl(View view) {
        switch (view.getId()) {
            case R.id.playBtn:
                playPause(view);
                break;
            case R.id.nextBtn:
                playNext();
                break;
        }
    }

    //下一首
    public void playNext() {
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.PLAY_NEXT);
        sendBroadcast(sBroadcast);
    }

    //暂停&播放
    public void playPause(View view) {
        if (app.getmList() != null && app.getmList().size() != 0 && app.getMp() != null) {
            try {
                if (app.getMp().isPlaying()) {
                    app.getMp().pause();
                    albumRotate(STOP);
                    playBtn.setImageResource(R.mipmap.play_red);
                } else {
                    app.getMp().start();
                    albumRotate(START);
                    playBtn.setImageResource(R.mipmap.pause_red);
                }
            } catch (IllegalStateException e) {
                Log.d("playPause", "MediaPlayer状态异常");
            }
        }
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.NOTIFICATION_REFRESH);
        sendBroadcast(sBroadcast);
    }

    //点击了item
    public void clickItem(View view) {
        //获取要播放歌曲的编号
        TextView textView = (TextView) view.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        sendPlay(musicNum);
    }

    //通知服务播放音乐
    public void sendPlay(int musicNum) {
        //如果点击的是正在播放的歌曲，直接去到专辑界面
        switch (viewPager.getCurrentItem()) {
            case 0:
                app.setmList(list);
                app.setPlaylist(0);
                break;
            case 1:
                app.setmList(historyData);
                app.setPlaylist(1);
                break;
            default:
                app.setPlaylist(-1);
                break;
        }
        app.setIdx(musicNum);
        //通知服务播放音乐
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.PLAY);
        sendBroadcast(sBroadcast);
        //}
    }

    //改变样式
    //Bitmap bitmap;

    public void refresh() {
        if (app.getmList() == null || app.getmList().size() == 0 || app.getIdx() == 0) {
            try {
                if (app.getmList() != null && app.getmList().size() == 0 && app.getMp() != null && app.getMp().isPlaying()) {
                    app.getMp().pause();
                    emptyList.setVisibility(View.VISIBLE);
                    sBroadcast = new Intent();
                    sBroadcast.setAction("sBroadcast");
                    sBroadcast.putExtra("what", Msg.NOTIFICATION_REFRESH);
                    sendBroadcast(sBroadcast);
                }
            } catch (IllegalStateException e) {
                Log.d("refresh", "MediaPlayer状态异常");
            }
            title.setText("炫听音乐");
            artist.setText("炫酷生活");
            album.setImageResource(R.mipmap.logo_red);
            playBtn.setImageResource(R.mipmap.play_red);
            albumRotate(STOP);
            currentTime.setMax(0);
            currentTime.setProgress(0);
            adapter.notifyDataSetChanged();
            historyAdapter.notifyDataSetChanged();
            return;
        }

        Music music = app.getmList().get(app.getIdx() - 1);

        title.setText(music.getTitle());
        artist.setText(music.getArtist());
        musicSize.setText(music.getSize() + "");
        musicName.setText(music.getName() + "");
        currentTime.setMax(music.getTime());

        String innerSDPath = new StorageUtil(this).innerSDPath();
        String name = music.getName();
        String toPath = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
        setAlbum(new File(toPath), music.getPath());

        //播放按钮的更新
        try {
            if (app.getMp() != null && app.getMp().isPlaying()) {
                playBtn.setImageResource(R.mipmap.pause_red);
                albumRotate(START);
            } else {
                playBtn.setImageResource(R.mipmap.play_red);
                albumRotate(STOP);
            }
        } catch (IllegalStateException e) {
            Log.d("refresh", "MediaPlayer状态异常");
        }

        list.clear();
        list.addAll(dao.getMusicData());
        adapter.notifyDataSetChanged();
        historyData.clear();
        historyData.addAll(dao.selMusicByDate());
        historyAdapter.notifyDataSetChanged();
    }

    //设置专辑封面
    public void setAlbum(File file, String path) {
        if (file.exists()) {
            album.setImageBitmap(ImageUtil.getimage(file.getAbsolutePath(), 150f, 150f));
/*                Picasso.with(this)
                        .load(file)
                        .resize(100, 100)
                        .into(album);*/
        } else {
            album.setImageBitmap(ImageUtil.getimage(dao.getAlbumBitmap(path, R.mipmap.logo_red), 150f, 150f));
        }
    }

    //重置当前播放歌曲编号
    public void resetNumber() {
        if (app.getIdx() == 0) return;
        List<Music> temp = new ArrayList<>();
        switch (app.getPlaylist()) {
            case 0:
                temp = list;
                break;
            case 1:
                temp = historyData;
                break;
        }
        for (int i = 0; i < temp.size(); i++) {
            Music music = temp.get(i);
            TextView name = (TextView) findViewById(R.id.musicName);
            TextView size = (TextView) findViewById(R.id.musicSize);
            if (name.getText().toString().equals(music.getName()) && size.getText().toString().equals(music.getSize() + "")) {
                app.setIdx(i + 1);
                break;
            }
        }
    }

    //去到专辑界面
    public void openAlbum(View view) {
        Intent intent = new Intent(this, AlbumActivity.class);
        startActivity(intent);
    }

    //更新列表
    public void updateList() {
        new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //更新曲库
                        list.clear();
                        //app.setmList(dao.getMusicData());
                        list.addAll(dao.getMusicData());
                        historyData.clear();
                        historyData.addAll(dao.selMusicByDate());
                        resetNumber();
                        refresh();
                        //musicList.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
                        //musicList.setSelection(app.getIdx() > 5 ? app.getIdx() - 5 : 0);

                        if ((list.size() == 0 && viewPager.getCurrentItem() == 0) || (historyData.size() == 0 && viewPager.getCurrentItem() == 1))
                            emptyList.setVisibility(View.VISIBLE);
                        else
                            emptyList.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    //更新媒体库
    public void updateMediaRepertory() {
        if (update) {  //防止重复点击
            return;
        }
        update = true;
        StorageUtil util = new StorageUtil(this);
        String innerSD = util.innerSDPath();
        String extSD = util.extSDPath();
        String[] mimeTypes = new String[]{
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("flac"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("m4a")
        };
        String[] path;
        if (extSD == null) {
            path = new String[]{
                    innerSD
            };
        } else {
            path = new String[]{
                    innerSD,
                    extSD
            };
        }
        MediaScannerConnection.scanFile(this, path, mimeTypes,
                new MediaScannerConnection.MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {
                    }

                    @Override
                    public void onScanCompleted(final String s, Uri uri) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update = false;
                            }
                        });
                    }
                });
    }

    //更新时间进度
    public void currentTime(int current) {
        List<Music> list = app.getmList();
        if (list != null && list.size() > 0 && app.getIdx() > 0) {
            int time = list.get(app.getIdx() - 1).getTime();
            if (currentTime.getMax() != time)
                currentTime.setMax(time);
            currentTime.setProgress(Math.round(current));
        }
    }

    //让播放器后台运行
    @Override
    public void onBackPressed() {
        //如果侧边栏展开，收起侧边栏
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //否则后台运行
            moveTaskToBack(true);
        }
    }

    //完全退出应用
    public void exitApp() {
        //先返回上一层，然后启用线程丝滑的退出
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(300);
                app.onTerminate();
            }
        }).start();
        onBackPressed();
    }

    //==========权限相关==========//
    @Override
    protected void onResume() {
        super.onResume();

        updateList();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(mBroadcast, filter);

        //恢复布局
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //注销广播接收者
        unregisterReceiver(mBroadcast);
        //停止专辑动画
        albumRotate(STOP);
    }

    //销毁时释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //返回结果回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //拒绝时，没有获取到主要权限，无法运行，关闭页面
        if (resultCode == RESULT_OK && requestCode == Msg.WITER_EXTSD) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                //持久化权限
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //保存treeUri
                SharedPreferences sharedPreferences = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
                SharedPreferences.Editor editor = sharedPreferences.edit();//获取编辑器
                editor.putString("treeUri", treeUri.toString());
                editor.apply();
                editor.commit();
                TastyToast.makeText(this, "获取外置SD卡读写权限成功", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
            } else {
                TastyToast.makeText(this, "获取权限失败", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
            }
        }
    }

    //点击侧边栏菜单
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        SharedPreferences pref;
        SharedPreferences.Editor editor;
        switch (item.getItemId()) {
            case R.id.nav_refresh:
                updateMediaRepertory();
                TastyToast.makeText(this, "正在后台更新", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
                break;
            case R.id.nav_timer_exit:
                if (isTimer) {
                    timerClear();
                } else {
                    timerSelect();
                }
                break;
            case R.id.nav_speed:
                if (app.getMp() == null) break;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    TastyToast.makeText(this, "仅支持安卓6以上版本", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                    break;
                }
                pref = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
                float speed = pref.getFloat("speed", 1.0f);
                editor = pref.edit();//获取编辑器
                if (speed < 1.45) {
                    speed += 0.05f;
                } else {
                    speed = 0.75f;
                }
                editor.putFloat("speed", speed);
                editor.apply();
                editor.commit();
                item.setTitle("播放速度 " + (float) (Math.round(speed * 100)) / 100);
                sBroadcast = new Intent();
                sBroadcast.setAction("sBroadcast");
                sBroadcast.putExtra("what", Msg.PLAY_SPEED);
                sBroadcast.putExtra("speed", speed);
                sendBroadcast(sBroadcast);
                break;
            case R.id.nav_bass:
                if (app.getMp() == null) break;
                pref = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
                int bass = (int) pref.getInt("bass", 0);
                editor = pref.edit();//获取编辑器
                if (bass < 10) {
                    bass++;
                } else {
                    bass = 0;
                }
                editor.putInt("bass", bass);
                editor.apply();
                editor.commit();
                item.setTitle("低音增益 " + bass);
                sBroadcast = new Intent();
                sBroadcast.setAction("sBroadcast");
                sBroadcast.putExtra("what", Msg.BASS_LEVEL);
                sBroadcast.putExtra("bass", bass);
                sendBroadcast(sBroadcast);
                break;
            case R.id.nav_reverb:
                if (app.getMp() == null) break;
                pref = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
                int reverb = pref.getInt("reverb", 0);
                editor = pref.edit();//获取编辑器
                if (reverb < 6) {
                    reverb++;
                } else {
                    reverb = 0;
                }
                editor.putInt("reverb", reverb);
                editor.apply();
                editor.commit();
                item.setTitle("混响 [ " + reverbToStr(reverb) + " ]");
                sBroadcast = new Intent();
                sBroadcast.setAction("sBroadcast");
                sBroadcast.putExtra("what", Msg.REVERB_LEVEL);
                sBroadcast.putExtra("reverb", reverb);
                sendBroadcast(sBroadcast);
                break;
            case R.id.nav_cache:
                cleanCacheTips();
                break;
            case R.id.nav_about:
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_exit:
                //收起侧边栏
                moveTaskToBack(false);
                exitApp();
                break;
        }
        //收起侧边栏
        int itemId = item.getItemId();
        if (itemId != R.id.nav_speed && itemId != R.id.nav_bass && itemId != R.id.nav_reverb && itemId != R.id.nav_cache) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    //混响等级转字符串
    public String reverbToStr(int reverb){
        String strReverb = "关闭";
        switch (reverb){
            case 0:
                strReverb = "关闭";
                break;
            case 1:
                strReverb = "小房间";
                break;
            case 2:
                strReverb = "中房间";
                break;
            case 3:
                strReverb = "大房间";
                break;
            case 4:
                strReverb = "中厅";
                break;
            case 5:
                strReverb = "大厅";
                break;
            case 6:
                strReverb = "板式";
                break;
        }
        return strReverb;
    }

    //清除缓存提示
    public void cleanCacheTips() {
        //自定义控件
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //设置time布局
        builder.setTitle("清除缓存说明");
        builder.setMessage("清除缓存后，播放历史列表的歌曲需要重新联网缓存，且因歌曲链接的时效性，有可能无法重新缓存，是否继续？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                String innerPath = new StorageUtil(MainActivity.this).innerSDPath();
                innerPath = innerPath + "/Android/data/com.xiu.xtmusic/cache/video-cache/";
                try {
                    FileUtils.delFolder(innerPath);
                    TastyToast.makeText(MainActivity.this, "清除成功", Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
                    cacheSize();
                    historyAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    TastyToast.makeText(MainActivity.this, "清除失败", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                    e.printStackTrace();
                }
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

    //内容观察者,观察媒体数据库的变化,实时更新音乐列表
    class MyObserver extends ContentObserver {
        public MyObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateList();
        }
    }
}
