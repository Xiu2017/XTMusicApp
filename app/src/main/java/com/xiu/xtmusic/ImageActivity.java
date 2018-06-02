package com.xiu.xtmusic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.sdsmdg.tastytoast.TastyToast;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.utils.CallBack;
import com.xiu.utils.FileUtils;
import com.xiu.utils.ImageUtil;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.TimeFormatUtil;

import java.io.File;

public class ImageActivity extends AppCompatActivity {

    private Bitmap bitmap;  //图片bitmap
    private Music music;  //音乐信息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //透明状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_image);
        Intent intent = getIntent();
        music = intent.getParcelableExtra("music");
        if(music != null){
            getImg();
        }
    }

    //获取图片
    public void getImg(){
        ImageView imageView = findViewById(R.id.showImg);
        String innerSDPath = new StorageUtil(this).innerSDPath();
        String name = music.getName();
        final String toPath = innerSDPath + "/XTMusic/AlbumImg/"
                + name.substring(0, name.lastIndexOf(".")) + ".jpg";

        File file = new File(toPath);
        if (file.exists()) {
            imageView.setImageBitmap(ImageUtil.getimage(file.getAbsolutePath(), 500f, 500f));
        } else {
            MusicDao dao = new MusicDao(this);
            bitmap = dao.getAlbumBitmap(music.getPath(), R.mipmap.album_default);
            imageView.setImageBitmap(bitmap);
        }
    }

    //保存图片
    public void saveImg(View view){
        if(music != null && bitmap != null){
            String name = music.getName();
            String savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            savePath += "/Album/"+name.substring(0, name.lastIndexOf("."))+".jpg";
            if(savePath.equals(ImageUtil.saveBitmap(bitmap, savePath))){
                TastyToast.makeText(this, "图片已保存到\n"+savePath, Msg.LENGTH_SHORT, TastyToast.SUCCESS).show();
            }else {
                TastyToast.makeText(this, "保存失败", Msg.LENGTH_SHORT, TastyToast.ERROR).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bitmap != null){
            bitmap.recycle();
            bitmap = null;
        }
    }
}
