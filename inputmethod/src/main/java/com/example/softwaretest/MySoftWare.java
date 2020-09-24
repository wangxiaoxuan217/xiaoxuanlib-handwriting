package com.example.softwaretest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.example.softwaretest.adapter.ResultAdapter;
import com.googlecode.openwnn.legacy.OnHandWritingRecognize;
import com.googlecode.openwnn.legacy.WnnWord;
import com.googlecode.openwnn.legacy.CLOUDSONG.CandidateView;
import com.googlecode.openwnn.legacy.CLOUDSONG.CloudKeyboardInputManager;
import com.googlecode.openwnn.legacy.CLOUDSONG.OnCandidateSelected;
import com.googlecode.openwnn.legacy.CLOUDSONG.OnPinyinQuery;
import com.googlecode.openwnn.legacy.CLOUDSONG.PinyinQueryResult;
import com.googlecode.openwnn.legacy.handwritingboard.HandWritingBoardLayout;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MySoftWare extends RelativeLayout implements OnHandWritingRecognize, OnPinyinQuery
{
    private HandWritingBoardLayout mHandWritingBoardLayout;
    
    private WeakReference<CurrentInputListener> mListener;
    
    private ResultAdapter mResultAdapter;
    
    public MySoftWare(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.input_view, this);
        RecyclerView rcvResult = (RecyclerView)findViewById(R.id.rcv_result);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        rcvResult.setLayoutManager(linearLayoutManager);
        mResultAdapter = new ResultAdapter();
        rcvResult.setAdapter(mResultAdapter);
        mHandWritingBoardLayout = (HandWritingBoardLayout)findViewById(R.id.handwrtingboard);
        ImageView mIvCancel = (ImageView)findViewById(R.id.iv_cancel);
        mIvCancel.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mResultAdapter.clearResultView();
                resetHandWritingRecognize();
            }
        });
        
        mResultAdapter.setOnResultListener(new ResultAdapter.OnResultListener()
        {
            @Override
            public void onResult(WnnWord candidate)
            {
                String temp = null;
                if (candidate != null)
                {
                    temp = candidate.candidate;
                }
                if (!TextUtils.isEmpty(temp))
                {
                    mListener.get().currentInputChange(temp);
                }
                mResultAdapter.clearResultView();
                resetHandWritingRecognize();
            }
        });
        CloudKeyboardInputManager ckManager = new CloudKeyboardInputManager();
        ckManager.setOnPinyinQuery(this);
        System.currentTimeMillis();
        mHandWritingBoardLayout.setVisibility(View.VISIBLE);
        ckManager.delAll();
        mResultAdapter.clearResultView();
        mHandWritingBoardLayout.setOnHandWritingRecognize(this);
    }
    
    public void setCurrentInputListener(CurrentInputListener inputListener)
    {
        this.mListener = new WeakReference<>(inputListener);
    }
    
    @Override
    public void handWritingRecognized(ArrayList<WnnWord> result)
    {
        mResultAdapter.onBindData(result);
    }
    
    // TODO 整理一下
    private void resetHandWritingRecognize()
    {
        mHandWritingBoardLayout.reset_recognize();
    }
    
    @Override
    public void onPinyinQuery(PinyinQueryResult pyQueryResult)
    {
        if (pyQueryResult != null)
        {
            mResultAdapter.onBindData(pyQueryResult.getCandidateList());
        }
    }
    
}
