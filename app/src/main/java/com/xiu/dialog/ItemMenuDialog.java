package com.xiu.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.xiu.entity.Music;
import com.xiu.xtmusic.R;

/**
 * 列表菜单对话框
 */

public class ItemMenuDialog extends Dialog {

    private Music music;
    private String num = "";

    public ItemMenuDialog(@NonNull Context context, Music music, String num) {
        super(context, R.style.CustomDialog);
        this.music = music;
        this.num = num;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (music.getPath().contains("http://")) {
            setContentView(R.layout.dialog_item_menu_kugou);
        } else {
            setContentView(R.layout.dialog_item_menu);
        }
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

        //绑定数据
        TextView title = view.findViewById(R.id.title);
        title.setText(music.getTitle());
        TextView id = view.findViewById(R.id.musicNum);
        id.setText(num);
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
