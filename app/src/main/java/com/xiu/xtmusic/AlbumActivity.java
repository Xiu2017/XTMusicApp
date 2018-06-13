package com.xiu.xtmusic;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.adapter.LyricListAdapter;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.utils.CallBack;
import com.xiu.utils.DefaultLrcBuilder;
import com.xiu.utils.FileUtils;
import com.xiu.api.KuGouLrc;
import com.xiu.utils.ImageUtil;
import com.xiu.utils.LrcRow;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.TimeFormatUtil;
import com.xiu.utils.mApplication;
import com.xiu.utils.readTextUtil;

import net.qiujuer.genius.blur.StackBlur;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity implements View.OnClickListener {

    //全局变量
    private List<LrcRow> rows;
    private ListView lrcList;
    private LyricListAdapter lyricListAdapter;
    private ObjectAnimator rotation;
    //private LinearLayout dot1, dot2;
    private boolean sbChange;
    private SeekBar currentTime;
    private TextView title, artist, maxTime, minTime, noLrc;
    private Music music;
    private mApplication app;
    private ImageView album, albumbg, playBtn;
    private ViewPager viewPager;
    private List<View> pages;

    private MusicDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        initStatusBar();  //初始化沉浸式状态栏
        initViewPager();  //初始化viewPager
        initView();  //初始化视图和元素
        setAlbumWidth();  //设置专辑封面宽度
        initAnim();  //初始化专辑旋转动画
        initPlayMode();  //初始化播放模式
    }

    //初始化播放模式
    public void initPlayMode(){
        ImageView view = findViewById(R.id.playmodeBtn);
        pref = getSharedPreferences("pref", MODE_PRIVATE);
        int playmode = pref.getInt("playmode", 0);
        switch (playmode) {
            case 0:
                view.setImageResource(R.mipmap.btn_loop);
                break;
            case 1:
                view.setImageResource(R.mipmap.btn_loop_one);
                break;
            case 2:
                view.setImageResource(R.mipmap.btn_random);
                break;
        }
    }

    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
        win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }

    //初始化专辑旋转动画
    public void initAnim() {
        rotation = ObjectAnimator.ofFloat(album, "rotation", 0.0F, 359.9F);
        rotation.setRepeatCount(-1);
        rotation.setDuration(30000);
        rotation.setInterpolator(new LinearInterpolator());
    }

    //初始化视图和元素
    public void initView() {
        album = findViewById(R.id.album);
        albumbg = findViewById(R.id.albumbg);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        maxTime = findViewById(R.id.maxTime);
        minTime = findViewById(R.id.minTime);
        currentTime = findViewById(R.id.currentTime);
        playBtn = findViewById(R.id.playBtn);
        //dot1 = findViewById(R.id.dot1);
        //dot2 = findViewById(R.id.dot2);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
        dao = new MusicDao(app);
    }

    //获取屏幕宽度
    public void setAlbumWidth(){
        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        int width = dm.widthPixels;
        ViewGroup.LayoutParams lp = album.getLayoutParams();
        lp.width = (int)(width*0.9);
        lp.height = lp.width;
        album.setLayoutParams(lp);
    }

    //初始化viewPager
    public void initViewPager() {
        viewPager = findViewById(R.id.viewPager);
        //查找布局文件用LayoutInflater.inflate
        LayoutInflater inflater = getLayoutInflater();
        View view1 = inflater.inflate(R.layout.layout_album, null);
        View view2 = inflater.inflate(R.layout.layout_lyric, null);
        //将view装入数组
        pages = new ArrayList<>();
        pages.add(view1);
        pages.add(view2);

        //绑定适配器
        viewPager.setAdapter(mPagerAdapter);
        viewPager.addOnPageChangeListener(mPageChange);

        lrcList = view2.findViewById(R.id.lyricList);
        noLrc = view2.findViewById(R.id.nolrc);

        lrcList.setEnabled(false);

        view1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(music != null && music.getPath().contains("163.com")){
                    String musicId = music.getPath().replace("http://music.163.com/song/media/outer/url?id=","")
                                                    .replace(".mp3","");
                    Intent intent = new Intent(AlbumActivity.this, CommentActivity.class);
                    intent.putExtra("musicId", musicId);
                    startActivity(intent);
                }/*else {
                    TastyToast.makeText(AlbumActivity.this, "仅支持查看网易云音乐评论", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
                }*/
            }
        });

        view1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(music != null){
                    Intent intent = new Intent(AlbumActivity.this, ImageActivity.class);
                    intent.putExtra("music", music);
                    startActivity(intent);
                }
                return false;
            }
        });
    }


    SharedPreferences pref;
    SharedPreferences.Editor editor;
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                this.finish();
                break;
            case R.id.share:  //分享音乐文件
                if (app.getmList() != null && app.getmList().size() != 0 && app.getIdx() != 0) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    String msg = "分享音乐：" + music.getArtist() + " - " + music.getTitle() + "\n" + music.getPath();
                    if (music.getPath().contains("http://")) {
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, msg);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(intent, "分享音乐链接"));
                    } else {
                        intent.setType("audio/*");
                        File file = new File(music.getPath());
                        Uri uri = Uri.fromFile(file);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(intent, "分享音乐文件"));
                    }
                }
                break;
            case R.id.playmodeBtn:
                pref = getSharedPreferences("pref", MODE_PRIVATE);
                editor = pref.edit();
                String modeStr = "";
                switch (app.getPlaymode()) {
                    case 0:
                        app.setPlaymode(1);
                        ((ImageView)view).setImageResource(R.mipmap.btn_loop_one);
                        editor.putInt("playmode", 1);
                        modeStr = "单曲循环";
                        break;
                    case 1:
                        app.setPlaymode(2);
                        ((ImageView)view).setImageResource(R.mipmap.btn_random);
                        editor.putInt("playmode", 2);
                        modeStr = "随机播放";
                        break;
                    case 2:
                        app.setPlaymode(0);
                        ((ImageView)view).setImageResource(R.mipmap.btn_loop);
                        editor.putInt("playmode", 0);
                        modeStr = "列表循环";
                        break;
                }
                editor.apply();
                editor.commit();
                TastyToast.makeText(AlbumActivity.this, modeStr, Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
                break;
            case R.id.lyricBtn:
                if(viewPager.getCurrentItem() == 0){
                    viewPager.setCurrentItem(1, true);
                }else {
                    viewPager.setCurrentItem(0, true);
                }
                break;
        }
    }

    //广播接收
    BroadcastReceiver aBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.PLAY_COMPLETION:
                    //改变样式
                    refresh();
                    break;
                case Msg.CURRENTTIME:
                    int current = intent.getIntExtra("current", 0);
                    currentTime(current);
                    playLyric(current);
                    break;
                case Msg.BUFFERING_UPDATE:
                    bufferingUpdate(intent.getIntExtra("percent", 0));
                    break;
            }
        }
    };

    //更新缓冲进度
    public void bufferingUpdate(int percent) {
        if (music != null) {
            currentTime.setSecondaryProgress(music.getTime() / 100 * percent);
        }
    }

    //歌词滚动
    public void playLyric(int current) {
        if (lyricListAdapter == null || rows == null || current == 0)
            return;
        for (int i = 0; i < rows.size(); i++) {
            LrcRow row = rows.get(i);
            if (current <= row.time) {
                lyricListAdapter.setIdx(i);
                lyricListAdapter.notifyDataSetChanged();
                lrcList.smoothScrollToPosition(i);
                return;
            } else if (i == rows.size() - 1) {
                lyricListAdapter.setIdx(i + 1);
                lyricListAdapter.notifyDataSetChanged();
                lrcList.smoothScrollToPosition(i + 1);
                return;
            }
        }
    }

    //播放控制
    public void playControl(View view) {
        Intent intent = new Intent();
        intent.setAction("sBroadcast");
        switch (view.getId()) {
            case R.id.lastBtn:
                intent.putExtra("what", Msg.PLAY_LAST);
                sendBroadcast(intent);
                break;
            case R.id.playBtn:
                if (app.getmList() != null && app.getmList().size() != 0 && app.getMp() != null) {
                    try{
                        if (app.getMp().isPlaying()) {
                            app.getMp().pause();
                            playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_play_sel));
                            albumRotate(STOP);
                        } else {
                            app.getMp().start();
                            playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_pause_sel));
                            albumRotate(START);
                        }
                    }catch (IllegalStateException e){
                        Log.d("playControl","MediaPlayer状态异常");
                    }
                    intent.putExtra("what", Msg.NOTIFICATION_REFRESH);
                    sendBroadcast(intent);
                }
                break;
            case R.id.nextBtn:
                intent.putExtra("what", Msg.PLAY_NEXT);
                sendBroadcast(intent);
                break;
        }
    }

    //初始化歌词
    public void readLyric() {
        //空处理
        if (app.getIdx() == 0) return;
        //提示用户查找歌词
        if (rows != null && lyricListAdapter != null) {
            rows.clear();
            lyricListAdapter.notifyDataSetChanged();
        }
        String lrc;
        //应用程序保存的歌词路径
        String temp = getApplicationContext().getFilesDir().getAbsolutePath() + "/lyric/" + music.getName();
        final String lrcPath = temp.substring(0, temp.lastIndexOf(".")) + ".lrc";
        noLrc.setText("正在查找歌词...");
        noLrc.setVisibility(View.VISIBLE);
        //歌曲同目录下的歌词路径
        String musicPath = app.getmList().get(app.getIdx() - 1).getPath();
        musicPath = musicPath.substring(0, musicPath.lastIndexOf("."));
        String lyricPath = musicPath + ".lrc";
        //判断歌词文件是否存在
        File file = new File(lyricPath);
        if (!file.exists()) {
            file = new File(lrcPath);
        }
        final DefaultLrcBuilder builder = new DefaultLrcBuilder();
        if (file.exists()) {
            noLrc.setVisibility(View.GONE);
            lrc = readTextUtil.ReadTxtFile(file);
            //解析歌词返回LrcRow集合
            rows = builder.getLrcRows(lrc);
            lyricListAdapter = new LyricListAdapter(rows, AlbumActivity.this, -1);
            lrcList.setAdapter(lyricListAdapter);
            int padding = lrcList.getHeight() / 2;
            lrcList.setPadding(0, padding - 208, 0, padding);
        } else {
            KuGouLrc.searchLrc(music.getTitle(), music.getTime(), new CallBack() {
                @Override
                public void success(final String str) {
                    rows = builder.getLrcRows(str);
                    if (rows.size() == 0) {
                        failed(null);
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noLrc.setVisibility(View.GONE);
                            lyricListAdapter = new LyricListAdapter(rows, AlbumActivity.this, -1);
                            lrcList.setAdapter(lyricListAdapter);
                            int padding = lrcList.getHeight() / 2;
                            lrcList.setPadding(0, padding - 208, 0, padding);
                        }
                    });
                    FileUtils.TextToFile(lrcPath, str);
                }

                @Override
                public void failed(String str) {
                    //Toast.makeText(AlbumActivity.this, str, Msg.LENGTH_SHORT).show();
                    //根据标题搜不出歌词，尝试根据文件名再搜一次
                    searchLrc(builder, lrcPath);
                }
            });
        }
    }

    //第二次查询歌词
    public void searchLrc(final DefaultLrcBuilder builder, final String lrcPath) {
        String name = music.getName();
        if (name == null) return;
        name = name.substring(0, name.lastIndexOf("."));
        KuGouLrc.searchLrc(name, music.getTime(), new CallBack() {
            @Override
            public void success(final String str) {
                rows = builder.getLrcRows(str);
                if (rows.size() == 0) {
                    failed(null);
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        noLrc.setVisibility(View.GONE);
                        lyricListAdapter = new LyricListAdapter(rows, AlbumActivity.this, -1);
                        lrcList.setAdapter(lyricListAdapter);
                        int padding = lrcList.getHeight() / 2;
                        lrcList.setPadding(0, padding - 208, 0, padding);
                    }
                });
                FileUtils.TextToFile(lrcPath, str);
            }

            @Override
            public void failed(String str) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        noLrc.setText("找不到歌词");
                        noLrc.setVisibility(View.VISIBLE);
                        if (lyricListAdapter != null) {
                            rows.clear();
                            lyricListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });

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

    public void refresh() {
        List<Music> list = app.getmList();
        if (list == null || list.size() == 0 || app.getIdx() == 0) {
            albumDefault(null);
            return;
        }
        //获取正在播放的歌曲信息
        music = list.get(app.getIdx() - 1);
        if (music == null) return;
        //设置专辑封面
        String innerSDPath = new StorageUtil(AlbumActivity.this).innerSDPath();
        String name = music.getName();
        String toPath = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
        File file = new File(toPath);
        if(file.exists()){
            setAlbum(file);
        }else{
            albumDefault(music.getPath());
        }
        //显示音乐信息
        //currentTime.setMax(music.getTime());
        title.setText(music.getTitle());
        artist.setText(music.getArtist());
        maxTime.setText(TimeFormatUtil.secToTime(music.getTime() / 1000) + "");
        currentTime.setMax(music.getTime());
        currentTime.setSecondaryProgress(music.getTime());
        //播放按钮的更新
        try{
            if (app.getMp() != null && app.getMp().isPlaying()) {
                playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_pause_sel));
                albumRotate(START);
            } else {
                playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_play_sel));
                albumRotate(STOP);
            }
            if(viewPager.getCurrentItem() == 1){
                albumRotate(STOP);
            }
        }catch (IllegalStateException e){
            Log.d("refresh","MediaPlayer状态异常");
        }
        readLyric();
    }

    //设置专辑封面
    public void setAlbum(File file) {
        album.setImageBitmap(ImageUtil.getimage(file.getAbsolutePath(), 500f, 500f));
/*        Picasso.with(this)
                .load(file)
                .resize(350, 350)
                .into(album);*/
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        bitmap = ImageUtil.getimage(bitmap, 20f, 20f);
        bitmap = StackBlur.blur(bitmap,4,true);
        albumbg.setImageBitmap(bitmap);
/*        Picasso.with(this)
                .load(file)
                .resize(100, 100)
                .transform(new BlurTransformation(this, 20))
                .into(albumbg);*/
    }

    //设置默认专辑界面
    public void albumDefault(String path) {
        //currentTime.setMax(0);
        //currentTime.setProgress(0);
        Bitmap bitmap = ImageUtil.getimage(dao.getAlbumBitmap(path, R.mipmap.album_default), 500f, 500f);
        album.setImageBitmap(bitmap);
        bitmap = ImageUtil.getimage(dao.getAlbumBitmap(path, R.mipmap.album_default), 20f, 20f);
        bitmap = StackBlur.blur(bitmap, 4, true);
        albumbg.setImageBitmap(bitmap);
/*        Picasso.with(this)
                .load(R.mipmap.album_default)
                .into(album);
        Picasso.with(this)
                .load(R.mipmap.album_default)
                .resize(20, 20)
                .transform(new BlurTransformation(this, 4))
                .into(albumbg);*/
/*        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.album_default);
        album.setImageBitmap(bitmap);
        bitmap = ImageUtil.getimage(bitmap, 20f, 20f);
        try {
            bitmap = StackBlur.blur(bitmap, 4, false);
            albumbg.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    //更新时间进度
    public void currentTime(int current) {
        //如果进度条正在拖动，不执行更新
        if (!sbChange) {
            currentTime.setProgress(current);
        }
    }

    //监听seekbar改变
    SeekBar.OnSeekBarChangeListener mSeekBarChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (app.getMp() != null) {
                minTime.setText(TimeFormatUtil.secToTime(i / 1000) + "");
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            sbChange = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (app.getmList() != null && app.getmList().size() != 0 && app.getMp() != null) {
                app.getMp().seekTo(currentTime.getProgress());
                sbChange = false;
            }
        }
    };

    //监听viewpager改变
    ViewPager.OnPageChangeListener mPageChange = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int offset = Math.round(positionOffset * 30);
            LinearLayout layout = (LinearLayout) album.getParent();
            switch (position) {
                case 0:
                    //切换到歌词界面时让背景变暗
                    if (offset < 10) {
                        albumbg.setColorFilter(Color.parseColor("#0" + offset + "000000"), PorterDuff.Mode.DARKEN);
                    } else {
                        albumbg.setColorFilter(Color.parseColor("#" + offset + "000000"), PorterDuff.Mode.DARKEN);
                    }
                    //隐藏专辑
                    layout.setAlpha(1 - positionOffset);
                    break;
                case 1:
                    //显示专辑
                    layout.setAlpha(positionOffset);
                    break;
            }
        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    //dot1.setBackgroundResource(R.drawable.circle_dot_sel);
                    //dot2.setBackgroundResource(R.drawable.circle_dot);
                    try{
                        if (app.getMp() != null && app.getMp().isPlaying()) {
                            albumRotate(START);
                        }
                    }catch (IllegalStateException e){
                        Log.d("onPageSelected","MediaPlayer状态异常");
                    }
                    break;
                case 1:
                    //dot1.setBackgroundResource(R.drawable.circle_dot);
                    //dot2.setBackgroundResource(R.drawable.circle_dot_sel);
                    int padding = lrcList.getHeight() / 2;
                    lrcList.setPadding(0, padding - 208, 0, padding);
                    albumRotate(STOP);
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        currentTime.setOnSeekBarChangeListener(mSeekBarChange);

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(aBroadcast, filter);

        refresh();  //刷新界面
    }

    //viewPager数据适配器
    PagerAdapter mPagerAdapter = new PagerAdapter() {
        @Override
        //获取当前窗体界面数
        public int getCount() {
            return pages.size();
        }

        @Override
        //断是否由对象生成界面
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        //从ViewGroup中移出当前View
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(pages.get(position));
            //super.destroyItem(container, position, object);
        }

        //返回一个对象，这个对象表明了PagerAdapter适配器选择哪个对象放在当前的ViewPager中
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(pages.get(position));
            return pages.get(position);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        //注销广播
        unregisterReceiver(aBroadcast);
        //移除监听事件
        currentTime.setOnSeekBarChangeListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除背景
        album.setImageResource(0);
        albumbg.setImageResource(0);
        //移除监听事件
        if (pages != null)
            pages.get(0).setOnLongClickListener(null);
        if (viewPager != null)
            viewPager.removeOnPageChangeListener(mPageChange);
    }

/*    //高斯模糊
    public class BlurTransformation implements Transformation {
        RenderScript rs;
        int radius;

        public BlurTransformation(Context context, int radius) {
            super();
            rs = RenderScript.create(context);
            this.radius = radius;
        }

        @SuppressLint("NewApi")
        @Override
        public Bitmap transform(Bitmap bitmap) {
            // 创建一个Bitmap作为最后处理的效果Bitmap
            Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            // 分配内存
            Allocation input = Allocation.createFromBitmap(rs, blurredBitmap, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);
            Allocation output = Allocation.createTyped(rs, input.getType());

            // 根据我们想使用的配置加载一个实例
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            script.setInput(input);

            // 设置模糊半径
            script.setRadius(radius);

            //开始操作
            script.forEach(output);

            // 将结果copy到blurredBitmap中
            output.copyTo(blurredBitmap);

            //释放资源
            bitmap.recycle();

            return blurredBitmap;
        }

        @Override
        public String key() {
            return "blur";
        }
    }*/
}
