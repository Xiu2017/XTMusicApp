package com.xiu.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xiu.entity.Comment;
import com.xiu.xtmusic.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 评论列表适配器
 */

public class CommentAdapter extends BaseAdapter {

    private List<Comment> list;  //保存评论的list
    private Context context;  //上下文
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

    public CommentAdapter(List<Comment> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        if (list != null) {
            return list.size();
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
        CommentGroup group;
        if (view == null) {
            group = new CommentGroup();
            view = View.inflate(context, R.layout.layout_comment_item, null);
            group.username = (TextView) view.findViewById(R.id.username);
            group.likedCount = (TextView) view.findViewById(R.id.likedCount);
            group.content = (TextView) view.findViewById(R.id.content);
            group.bereq = (TextView) view.findViewById(R.id.bereq);
            view.setTag(group);
        } else {
            group = (CommentGroup) view.getTag();
        }

        if (list != null && list.size() > 0) {
            Comment c = list.get(i);
            Date date = new Date(c.getTime());
            group.username.setText(sdf.format(date)+" - "+c.getUsername());
            group.likedCount.setText(c.getLikedCount());
            group.content.setText(c.getContent());
            if(c.getBeRepliedUser() != null && c.getBeRepliedContent() != null){
                group.bereq.setText("@"+c.getBeRepliedUser()+"："+c.getBeRepliedContent());
                group.bereq.setVisibility(View.VISIBLE);
            }else {
                group.bereq.setVisibility(View.GONE);
            }
        }
        return view;
    }

    class CommentGroup{
        TextView username,likedCount,content,bereq;
    }
}
