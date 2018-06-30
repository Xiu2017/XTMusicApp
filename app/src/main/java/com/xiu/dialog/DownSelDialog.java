package com.xiu.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.xiu.entity.Music;
import com.xiu.xtmusic.R;

import java.text.DecimalFormat;

/**
 * 列表菜单对话框
 */

public class DownSelDialog extends Dialog {

    private Music music;
    private String num = "";

    public DownSelDialog(@NonNull Context context, Music music, String num) {
        super(context, R.style.CustomDialog);
        this.music = music;
        this.num = num;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_download_sel);
    }

    @Override
    public void show() {
        super.show();
        //设置宽度全屏，要设置在show的后面
        View view = getWindow().getDecorView();

        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        view.setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);

        Button d1 = (Button) findViewById(R.id.menu_download1);
        Button d2 = (Button) findViewById(R.id.menu_download2);
        Button d3 = (Button) findViewById(R.id.menu_download3);

        //绑定数据
        TextView id = (TextView) view.findViewById(R.id.musicNum);
        id.setText(num);

        DecimalFormat df = new DecimalFormat("#0.00M");
        float tmp = music.getTime() / 60.0f / 1000f;
        d1.setText("标准mp3(约"+df.format(tmp)+")");
        d2.setText("高品mp3(约"+df.format(tmp*2.5f)+")");
        d3.setText("无损flac(约"+df.format(tmp*8.0f)+")");

        //判断支持下载的音质
        String path = music.getPath();
        if(path.contains(".m4a")){
            d1.setText("流畅m4a(约"+df.format(music.getSize() / 1024.0f / 1024.0f)+")");
        }else if(path.contains("qqmusic") && path.contains("|")){
            String[] paths = path.split("|");
            if(paths[1] != null && !paths[1].isEmpty()){
                d2.setVisibility(View.VISIBLE);
            }
            if(paths[2] != null && !paths[2].isEmpty()){
                d3.setVisibility(View.VISIBLE);
            }
        }
    }

    //下滑关闭dialog
    private float startY;
    private float moveY = 0;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        View view = getWindow().getDecorView();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = ev.getY() - startY;
                view.scrollBy(0, -(int) moveY);
                startY = ev.getY();
                if (view.getScrollY() > 0) {
                    view.scrollTo(0, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (view.getScrollY() < -this.getWindow().getAttributes().height / 4 && moveY > 0) {
                    this.dismiss();
                }
                view.scrollTo(0, 0);
                break;
        }
        return super.onTouchEvent(ev);
    }
}
