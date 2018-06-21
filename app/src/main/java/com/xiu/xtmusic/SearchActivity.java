package com.xiu.xtmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.danikula.videocache.HttpProxyCacheServer;
import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.adapter.MainPagerAdapter;
import com.xiu.adapter.SearchListAdapter;
import com.xiu.api.NeteaseMusic;
import com.xiu.api.QQMusic;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.entity.MusicList;
import com.xiu.utils.CallBack;
import com.xiu.utils.FileUtils;
import com.xiu.api.KuGouMusic;
import com.xiu.utils.ImageUtil;
import com.xiu.utils.JSSecret;
import com.xiu.utils.PicUtils;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.mApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener,
        TextView.OnEditorActionListener {

    private int musicHttp;
    private boolean isLoadlist;
    private boolean isLoadPath;
    private int pageKugou, pageQq, pageNet;
    private TextView local, kghttp, qqhttp, nethttp;
    private ProgressBar loadlist;
    private mApplication app;
    private MusicDao dao;
    private List<Music> list, kugou, qq, net;
    private BaseAdapter adapter, kugouAdapter, qqAdapter, netAdapter;
    private ListView seaList, seaKugou, seaQq, seaNet;
    private EditText keywork;
    private ViewPager viewPager;  //页视图
    private View view1, view2, view3, view4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
        dao = new MusicDao(this);
        initStatusBar();
        initViewPager();
        initView();

        list = new ArrayList<>();
        adapter = new SearchListAdapter(list, SearchActivity.this);
        seaList.setAdapter(adapter);
        kugou = new ArrayList<>();
        kugouAdapter = new SearchListAdapter(kugou, SearchActivity.this);
        seaKugou.setAdapter(kugouAdapter);
        qq = new ArrayList<>();
        qqAdapter = new SearchListAdapter(qq, SearchActivity.this);
        seaQq.setAdapter(qqAdapter);
        net = new ArrayList<>();
        netAdapter = new SearchListAdapter(net, SearchActivity.this);
        seaNet.setAdapter(netAdapter);

        isLoadPath = false;
        pageKugou = 1;
        pageQq = 1;
        pageNet = 1;
        AbsListView.OnScrollListener mScroll = new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        List<Music> temp = new ArrayList();
                        switch (viewPager.getCurrentItem()) {
                            case 0:
                                temp = list;
                                break;
                            case 1:
                                temp = kugou;
                                break;
                            case 2:
                                temp = qq;
                                break;
                            case 3:
                                temp = net;
                                break;
                        }
                        if (isListViewReachBottomEdge(absListView) && temp.size() > 0 && !isLoadlist && musicHttp != 0) {
                            isLoadlist = true;
                            loadlist.setVisibility(View.VISIBLE);
                            switch (viewPager.getCurrentItem()) {
                                case 1:
                                    pageKugou++;
                                    break;
                                case 2:
                                    pageQq++;
                                    break;
                                case 3:
                                    pageNet++;
                                    break;
                            }
                            searchMusic(absListView);
                        }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };

        seaList.setOnScrollListener(mScroll);
        seaKugou.setOnScrollListener(mScroll);
        seaQq.setOnScrollListener(mScroll);
        seaNet.setOnScrollListener(mScroll);
    }

    //初始化viewPager
    public void initViewPager() {
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        //查找布局文件
        LayoutInflater inflater = getLayoutInflater();
        view1 = inflater.inflate(R.layout.layout_list, null);
        view2 = inflater.inflate(R.layout.layout_list, null);
        view3 = inflater.inflate(R.layout.layout_list, null);
        view4 = inflater.inflate(R.layout.layout_list, null);

        //将view装入数组中
        List<View> pages = new ArrayList<>();  //所有页面
        pages.add(view1);
        pages.add(view2);
        pages.add(view3);
        pages.add(view4);
        //绑定适配器
        viewPager.setAdapter(new MainPagerAdapter(pages));
        //添加监听器
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                View view = findViewById(R.id.local);
                switch (position) {
                    case 0:
                        musicHttp = 0;
                        view = findViewById(R.id.local);
                        if (list.size() == 0) {
                            switchSearch();
                        }
                        break;
                    case 1:
                        musicHttp = 1;
                        view = findViewById(R.id.kghttp);
                        if (kugou.size() == 0) {
                            switchSearch();
                        }
                        break;
                    case 2:
                        musicHttp = 2;
                        view = findViewById(R.id.qqhttp);
                        if (qq.size() == 0) {
                            switchSearch();
                        }
                        break;
                    case 3:
                        musicHttp = 3;
                        view = findViewById(R.id.nethttp);
                        if (net.size() == 0) {
                            switchSearch();
                        }
                        break;

                }
                int colorText = getResources().getColor(R.color.colorText);
                local.setTextColor(colorText);
                kghttp.setTextColor(colorText);
                qqhttp.setTextColor(colorText);
                nethttp.setTextColor(colorText);
                TextView textView = (TextView) view;
                textView.setTextColor(getResources().getColor(R.color.colorPrimary));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    //初始化布局元素
    private void initView() {
        seaList = (ListView) view1.findViewById(R.id.musicList);
        seaKugou = (ListView) view2.findViewById(R.id.musicList);
        seaQq = (ListView) view3.findViewById(R.id.musicList);
        seaNet = (ListView) view4.findViewById(R.id.musicList);
        keywork = (EditText) findViewById(R.id.keyword);
        loadlist = (ProgressBar) findViewById(R.id.loadlist);
        local = (TextView) findViewById(R.id.local);
        kghttp = (TextView) findViewById(R.id.kghttp);
        qqhttp = (TextView) findViewById(R.id.qqhttp);
        nethttp = (TextView) findViewById(R.id.nethttp);
        keywork.setOnEditorActionListener(this);
    }

    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }


    //广播接收
    BroadcastReceiver seaBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.SEARCH_RESULT:
                    isLoadlist = false;
                    final MusicList musicList = intent.getParcelableExtra("list");
                    if (musicList == null || musicList.getList() == null || musicList.getList().size() == 0) {
                        TastyToast.makeText(SearchActivity.this, "没有更多的数据了", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
                        loadlist.setVisibility(View.GONE);
                        return;
                    }
                    switch (musicHttp) {
                        case 0:
                            list.addAll(musicList.getList());
                            adapter.notifyDataSetChanged();
                            break;
                        case 1:
                            kugou.addAll(musicList.getList());
                            kugouAdapter.notifyDataSetChanged();
                            break;
                        case 2:
                            qq.addAll(musicList.getList());
                            qqAdapter.notifyDataSetChanged();
                            break;
                        case 3:
                            net.addAll(musicList.getList());
                            netAdapter.notifyDataSetChanged();
                            break;
                    }
                    loadlist.setVisibility(View.GONE);
                    break;
                case Msg.GET_MUSIC_PATH:
                    isLoadPath = false;
                    Music music = intent.getParcelableExtra("music");
                    int idx = isExist(music);
                    dao.addToHistory(music);
                    if (app.getmList() != null) app.getmList().clear();
                    app.setmList(dao.selMusicByDate());
                    app.setPlaylist(1);
                    app.setIdx(idx);
                    Intent broadcast = new Intent();
                    broadcast.setAction("sBroadcast");
                    broadcast.putExtra("what", Msg.PLAY_KUGOU_MUSIC);
                    //broadcast.putExtra("idx", idx);
                    sendBroadcast(broadcast);
                    saveLrcAlbum(music);
                    adapter.notifyDataSetChanged();
                    kugouAdapter.notifyDataSetChanged();
                    qqAdapter.notifyDataSetChanged();
                    netAdapter.notifyDataSetChanged();
                    break;
                case Msg.GET_MUSIC_ERROR:
                    isLoadPath = false;
                    loadlist.setVisibility(View.GONE);
                    TastyToast.makeText(SearchActivity.this, "拉取歌曲链接失败", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                    break;
                case Msg.SEARCH_ERROR:
                    isLoadlist = false;
                    loadlist.setVisibility(View.GONE);
                    int pageTmp;
                    if (viewPager.getCurrentItem() == 1) {
                        pageTmp = pageKugou;
                    } else if (viewPager.getCurrentItem() == 2) {
                        pageTmp = pageQq;
                    } else {
                        pageTmp = pageNet;
                    }
                    final int page = pageTmp;
                    if (page == 1) {
                        final MusicList mList = intent.getParcelableExtra("list");
                        if (mList == null || mList.getList() == null || mList.getList().size() == 0) {
                            TastyToast.makeText(SearchActivity.this, "已经是最后一页", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
                            loadlist.setVisibility(View.GONE);
                            return;
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                switch (page) {
                                    case 1:
                                        kugou.addAll(mList.getList());
                                        break;
                                    case 2:
                                        qq.addAll(mList.getList());
                                        break;
                                    case 3:
                                        net.addAll(mList.getList());
                                        break;
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                        kugouAdapter.notifyDataSetChanged();
                                        qqAdapter.notifyDataSetChanged();
                                        netAdapter.notifyDataSetChanged();
                                        loadlist.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }).start();
                    }
                    TastyToast.makeText(SearchActivity.this, "获取列表失败", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
                    break;
                case Msg.PLAY_COMPLETION:  //播放完成
                    //改变样式
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    //保存歌词和专辑
    public void saveLrcAlbum(final Music music) {
        //保存歌词
        new Thread(new Runnable() {
            @Override
            public void run() {
                String temp = getApplicationContext().getFilesDir().getAbsolutePath() + "/lyric/" + music.getName();
                final String lrcPath = temp.substring(0, temp.lastIndexOf(".")) + ".lrc";
                String lrc = music.getLyric();
                File file = new File(lrcPath);
                if (!file.exists() && lrc != null && lrc.length() > 10) {
                    FileUtils.TextToFile(lrcPath, lrc);
                    Music m = music;
                    m.setLyric(null);
                    dao.addToHistory(m);
                }
            }
        }).start();

        //保存专辑
        new Thread(new Runnable() {
            @Override
            public void run() {
                String innerSDPath = new StorageUtil(SearchActivity.this).innerSDPath();
                String name = music.getName();
                final String toPath = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
                File file = new File(toPath);
                String path = music.getAlbumPath();
                if (!file.exists() && path != null) {
                    //读取缓存
                    if (path.contains("http://")) {
                        HttpProxyCacheServer proxy = app.getProxy(app);
                        //proxy.registerCacheListener(this, music.getPath());
                        path = proxy.getProxyUrl(path);
                        //Log.d("path", path);
                    }
                    FileUtils.downLoadFile(path, toPath, new CallBack() {
                        @Override
                        public void success(String str) {
                            //转格式
                            //PicUtils.convertToJpg(toPath,toPath.replace(".png",".jpg"));
                            //压缩图片文件(避免网易云专辑图片发生10几M这种恐怖的事情发生)
                            Bitmap bitmap = ImageUtil.getimage(toPath, 500f, 500f);
                            ImageUtil.saveBitmap(bitmap, toPath);
                            //刷新通知栏
                            Intent intent = new Intent();
                            intent.setAction("sBroadcast");
                            intent.putExtra("what", Msg.NOTIFICATION_REFRESH);
                            sendBroadcast(intent);
                        }

                        @Override
                        public void failed(String str) {
                        }
                    });
                }
            }
        }).start();
    }

    //判断歌曲是否存在本地列表
    public int isExist(Music music) {
        List<Music> list;
        if (music.getPath().contains("http://")) {
            list = dao.selMusicByDate();
        } else {
            list = dao.getMusicData();
        }
        if (list == null || list.size() == 0) return 1;
        for (int i = 0; i < list.size(); i++) {
            Music m = list.get(i);
            if (music.getName().equals(m.getName()) && music.getSize() == m.getSize()) {
                return i + 1;
            }
        }
        return 1;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.list_item:
                clickItem(view);
                break;
            case R.id.local:
                viewPager.setCurrentItem(0, true);
                break;
            case R.id.kghttp:
                viewPager.setCurrentItem(1, true);
                break;
            case R.id.qqhttp:
                viewPager.setCurrentItem(2, true);
                break;
            case R.id.nethttp:
                viewPager.setCurrentItem(3, true);
                break;
        }
    }

    //切换搜索
    public void switchSearch() {
        list.clear();
        //kugou.clear();
        //qq.clear();
        pageKugou = 1;
        pageQq = 1;
        pageNet = 1;
        searchMusic(findViewById(R.id.keyword));
    }

    //去到专辑界面
    public void openAlbum() {
        Intent intent = new Intent(this, AlbumActivity.class);
        startActivity(intent);
    }


    //搜索歌曲
    public void searchMusic(View view) {
        loadlist.setVisibility(View.VISIBLE);
        String str = keywork.getText().toString();
        if (str.replaceAll(" ", "").length() == 0) {
            loadlist.setVisibility(View.GONE);
            //Toast.makeText(this, "请输入搜索内容", Msg.LENGTH_SHORT).show();
            return;
        }
        searchList(str);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //搜索歌曲列表
    public void searchList(final String str) {
        isLoadlist = false;
        switch (musicHttp) {
            case 0:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        list.clear();
                        list.addAll(dao.getMusicData(str));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                loadlist.setVisibility(View.GONE);
                                if (list.size() == 0) {
                                    TastyToast.makeText(SearchActivity.this, "没有搜索到符合条件的歌曲", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
                                }
                            }
                        });
                    }
                }).start();
                break;
            case 1:
                new KuGouMusic(this).search(str, pageKugou);
                break;
            case 2:
                new QQMusic(this).search(str, pageQq);
                break;
            case 3:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new NeteaseMusic(app).search(str, pageNet);
                    }
                }).start();
                break;
        }
    }

    //处理并播放歌曲
    public void clickItem(View view) {
        if (!isLoadPath) {
            isLoadPath = true;
            getMusicPath(getMusicByNum(view));
        }
    }

    //获取歌曲链接
    public void getMusicPath(final Music music) {
        switch (musicHttp) {
            case 0:
                isLoadPath = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (app.getmList() == null || app.getmList().size() == 0 || app.getPlaylist() == 1) {
                            app.setmList(dao.getMusicData());
                            app.setPlaylist(0);
                        }
                        app.setIdx(isExist(music));
                        Intent broadcast = new Intent();
                        broadcast.setAction("sBroadcast");
                        broadcast.putExtra("what", Msg.PLAY_KUGOU_MUSIC);
                        sendBroadcast(broadcast);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
                break;
            case 1:
                TastyToast.makeText(this, "正在获取音乐链接", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
                new KuGouMusic(this).musicUrl(music);
                break;
            case 2:
                TastyToast.makeText(this, "正在获取音乐链接", Msg.LENGTH_SHORT, TastyToast.DEFAULT).show();
                new QQMusic(this).musicUrl(music);
                break;
            case 3:
                Intent kBroadcast = new Intent();
                kBroadcast.setAction("sBroadcast");
                kBroadcast.putExtra("what", Msg.GET_MUSIC_PATH);
                kBroadcast.putExtra("music", music);
                sendBroadcast(kBroadcast);
                break;
        }
    }


    //根据音乐编号获取音乐
    public Music getMusicByNum(View view) {
        LinearLayout layout = (LinearLayout) view;
        TextView textView = (TextView) layout.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        Music music = null;
        switch (viewPager.getCurrentItem()) {
            case 0:
                music = list.get(musicNum - 1);
                break;
            case 1:
                music = kugou.get(musicNum - 1);
                break;
            case 2:
                music = qq.get(musicNum - 1);
                break;
            case 3:
                music = net.get(musicNum - 1);
        }
        return music;
    }

    @Override
    protected void onResume() {
        super.onResume();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(seaBroadcast, filter);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //注销广播接收者
        unregisterReceiver(seaBroadcast);
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_SEARCH || (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            pageKugou = 1;
            pageQq = 1;
            pageNet = 1;
            list.clear();
            kugou.clear();
            qq.clear();
            net.clear();
            searchMusic(textView);
            return true;
        }
        return false;
    }

    public boolean isListViewReachBottomEdge(final AbsListView listView) {
        boolean result = false;
        if (listView.getLastVisiblePosition() == (listView.getCount() - 1)) {
            final View bottomChildView = listView.getChildAt(listView.getLastVisiblePosition() - listView.getFirstVisiblePosition());
            result = (listView.getHeight() >= bottomChildView.getBottom());
        }
        return result;
    }
}
