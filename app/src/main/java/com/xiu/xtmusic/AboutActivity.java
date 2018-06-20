package com.xiu.xtmusic;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.entity.Msg;
import com.xiu.utils.mApplication;

import java.text.DecimalFormat;

public class AboutActivity extends AppCompatActivity {

    private SeekBar speedSB,bassSB,reverbSB;
    private TextView speedTV,bassTV,reverbTV;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private DecimalFormat df = new DecimalFormat("#0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mApplication app = (mApplication) getApplicationContext();
        app.addActivity(this);

        pref = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
        editor = pref.edit();//获取编辑器

        initStatusBar();
        initView();
        initListener();
        initData();
    }

    //初始化监听事件
    public void initListener(){
        speedSB.setOnSeekBarChangeListener(sbChangeListener);
        bassSB.setOnSeekBarChangeListener(sbChangeListener);
        reverbSB.setOnSeekBarChangeListener(sbChangeListener);
    }

    SeekBar.OnSeekBarChangeListener sbChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            Intent sBroadcast = new Intent();
            sBroadcast.setAction("sBroadcast");
            switch (seekBar.getId()){
                case R.id.speedSB:
                    speedTV.setText(df.format((i+75)/100.0f));
                    editor.putInt("speed", i);
                    sBroadcast.putExtra("what", Msg.PLAY_SPEED);
                    sBroadcast.putExtra("speed", i);
                    break;
                case R.id.bassSB:
                    bassTV.setText(String.valueOf(i));
                    editor.putInt("bass", i);
                    sBroadcast.putExtra("what", Msg.BASS_LEVEL);
                    sBroadcast.putExtra("bass", i);
                    break;
                case R.id.reverbSB:
                    reverbTV.setText(reverbToStr(i));
                    editor.putInt("reverb", i);
                    sBroadcast.putExtra("what", Msg.REVERB_LEVEL);
                    sBroadcast.putExtra("reverb", i);
                    break;
            }
            editor.apply();
            sendBroadcast(sBroadcast);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            editor.commit();
        }
    };

    //初始化持久化数据
    public void initData() {
        int bass = pref.getInt("bass", 0);
        int reverb = pref.getInt("reverb", 0);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            speedSB.setProgress(25);
            speedSB.setEnabled(false);
            speedTV.setText("不支持");
        }else {
            int speed = pref.getInt("speed", 25);
            speedSB.setProgress(speed);
            speedTV.setText(df.format((speed+75)/100.0f));
        }

        bassSB.setProgress(bass);
        bassTV.setText(String.valueOf(bass));

        reverbSB.setProgress(reverb);
        reverbTV.setText(reverbToStr(reverb));
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

    //重置效果
    public void resetAll(View view){
        editor.putInt("speed", 25);
        editor.putInt("bass", 0);
        editor.putInt("reverb", 0);
        editor.apply();
        editor.commit();

        speedSB.setProgress(25);
        speedTV.setText(String.valueOf(1.0));
        bassSB.setProgress(0);
        bassTV.setText(String.valueOf(0));
        reverbSB.setProgress(0);
        reverbTV.setText(reverbToStr(0));

        Intent sBroadcast;

        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.PLAY_SPEED);
        sBroadcast.putExtra("speed", 25);
        sendBroadcast(sBroadcast);

        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.BASS_LEVEL);
        sBroadcast.putExtra("bass", 0);
        sendBroadcast(sBroadcast);

        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.REVERB_LEVEL);
        sBroadcast.putExtra("reverb", 0);
        sendBroadcast(sBroadcast);
    }

    //初始化视图
    public void initView(){
        speedSB = (SeekBar) findViewById(R.id.speedSB);
        bassSB = (SeekBar) findViewById(R.id.bassSB);
        reverbSB = (SeekBar) findViewById(R.id.reverbSB);

        speedTV = (TextView) findViewById(R.id.speedTV);
        bassTV = (TextView) findViewById(R.id.bassTV);
        reverbTV = (TextView) findViewById(R.id.reverbTV);
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
    public void openSina(View view){
        Intent intent = new Intent();
        ComponentName cmp = new ComponentName("com.sina.weibo", "com.sina.weibo.page.ProfileInfoActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        intent.putExtra("uid", "45xiuqing");
        //存在
        if ((getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null)){
            try{
                startActivity(intent);
            }catch (ActivityNotFoundException e){
                TastyToast.makeText(this, "你还没有安装微博", Msg.LENGTH_SHORT, TastyToast.WARNING).show();
            }
        }
    }
}
