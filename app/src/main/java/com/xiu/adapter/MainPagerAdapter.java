package com.xiu.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * 主页ViewPager适配器
 */

public class MainPagerAdapter extends PagerAdapter {

    private List<View> pages;  //保存要显示的页

    public MainPagerAdapter(List<View> pages) {
        this.pages = pages;
    }

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
    }

    //返回一个对象，这个对象表明了PagerAdapter适配器选择哪个对象放在当前的ViewPager中
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(pages.get(position));
        return pages.get(position);
    }
}
