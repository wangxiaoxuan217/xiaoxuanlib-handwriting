package com.example.softwaretest.adapter;

import android.view.View;

import com.googlecode.openwnn.legacy.CLOUDSONG.CandidateView;

/**
 * @Version：1.0 @Author： qiyuanyuan @Date：2020-09-07 09:14:16 @Description：
 **/
public class ResultViewHolder extends BaseRecyclerViewHolder
{
    protected CandidateView mCvResultItem;
    
    private View mItemView;
    
    public ResultViewHolder(View itemView)
    {
        super(itemView);
        this.mItemView = itemView;
    }
    
    @Override
    protected View getView()
    {
        return mItemView;
    }
}
