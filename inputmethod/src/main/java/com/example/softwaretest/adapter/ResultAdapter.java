package com.example.softwaretest.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.softwaretest.R;
import com.googlecode.openwnn.legacy.CLOUDSONG.CandidateView;
import com.googlecode.openwnn.legacy.CLOUDSONG.OnCandidateSelected;
import com.googlecode.openwnn.legacy.WnnWord;

/**
 * @Version：1.0 @Author： qiyuanyuan @Date：2020-09-07 12:07:50 @Description：
 **/
public class ResultAdapter extends BaseRecyclerViewAdapter<WnnWord>
{
    private OnResultListener mOnResultListener;
    
    @Override
    protected BaseRecyclerViewHolder createItem(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
        ResultViewHolder resultViewHolder = new ResultViewHolder(itemView);
        resultViewHolder.mCvResultItem = (CandidateView)itemView.findViewById(R.id.cv_result);
        resultViewHolder.mCvResultItem.setOnCandidateSelected(new OnCandidateSelected()
        {
            @Override
            public void candidateSelected(WnnWord candidate)
            {
                mOnResultListener.onResult(candidate);
            }
        });
        return resultViewHolder;
    }
    
    @Override
    protected void bindData(BaseRecyclerViewHolder holder, int position)
    {
        ((ResultViewHolder)holder).mCvResultItem.setSuggestions(getItemData(position), false, false);
    }
    
    public void clearResultView()
    {
        clearData();
        notifyDataSetChanged();
    }
    
    public interface OnResultListener
    {
        void onResult(WnnWord candidate);
    }
    
    public void setOnResultListener(OnResultListener onResultListener)
    {
        this.mOnResultListener = onResultListener;
    }
}
