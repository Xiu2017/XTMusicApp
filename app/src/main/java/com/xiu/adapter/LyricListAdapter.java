package com.xiu.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xiu.utils.LrcRow;
import com.xiu.xtmusic.R;

import java.util.List;

/**
 * 歌词ListView适配器
 */

public class LyricListAdapter extends BaseAdapter {

    private List<LrcRow> list;  //保存歌词的list
    private Context context;  //上下文
    private int idx;  //高亮显示第几行

    public LyricListAdapter(List<LrcRow> list, Context context, int idx) {
        this.list = list;
        this.context = context;
        this.idx = idx;
    }

    @Override
    public int getCount() {
        if (list != null) {
            return list.size() + 1;
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (list != null) {
            return list.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        if (list != null) {
            return i;
        }
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView line;
        if (view == null) {
            view = View.inflate(context, R.layout.layout_lyric_list, null);
            line = view.findViewById(R.id.line);
            view.setTag(line);
        } else {
            line = (TextView) view.getTag();
        }
        if (list != null && list.size() > 1) {
            if (i == 0) {
                line.setText("");
                return view;
            }
            LrcRow lrcRow = list.get(i - 1);
            line.setText(lrcRow.content);
            if (idx == i) {
                line.setTextColor(context.getResources().getColor(R.color.colorWrite));
            } else {
                line.setTextColor(context.getResources().getColor(R.color.colorLrc));
            }
            return view;
        }
        line.setText("");
        return view;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }
}
