package com.example.softwaretest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

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
import android.widget.RelativeLayout;

public class MySoftWare extends RelativeLayout
    implements OnCandidateSelected, OnHandWritingRecognize, OnPinyinQuery, OnClickListener
{
    private Context context;
    
    private HandWritingBoardLayout handWritingBoard;
    
    private RelativeLayout candidateContainer;
    
    private CandidateView mCandidateView;
    
    private WeakReference<CurrentInputListener> mListener;
    
    public MySoftWare(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        initView_Method();
    }
    
    public void initView_Method()
    {
        LayoutInflater.from(context).inflate(R.layout.input_view, this);
        findViewById();
        CloudKeyboardInputManager ckManager = new CloudKeyboardInputManager();
        ckManager.setOnPinyinQuery(this);
        mCandidateView = new CandidateView(context);
        mCandidateView.setOnCandidateSelected(this);
        LayoutParams lp1 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp1.addRule(RelativeLayout.LEFT_OF, R.id.btn_showMore);
        lp1.width = ViewGroup.LayoutParams.MATCH_PARENT;
        candidateContainer.addView(mCandidateView, lp1);
        System.currentTimeMillis();
        handWritingBoard.setVisibility(View.VISIBLE);
        ckManager.delAll();
        mCandidateView.clear();
        handWritingBoard.setOnHandWritingRecognize(this);
    }
    
    @Override
    public void onClick(View view)
    {
    }
    
    @Override
    public void candidateSelected(WnnWord wnnWord)
    {
        String candidate = null;
        if (wnnWord != null)
        {
            candidate = wnnWord.candidate;
        }
        if (!TextUtils.isEmpty(candidate))
        {
            mListener.get().currentInputChange(candidate);
        }
        mCandidateView.clear();
        resetHandWritingRecognize();
    }
    
    public void setCurrentInputListener(CurrentInputListener inputListener)
    {
        this.mListener = new WeakReference<>(inputListener);
    }
    
    @Override
    public void handWritingRecognized(ArrayList<WnnWord> result)
    {
        mCandidateView.setSuggestions(result, false, false);
    }
    
    // TODO 整理一下
    private void resetHandWritingRecognize()
    {
        handWritingBoard.reset_recognize();
    }
    
    @Override
    public void onPinyinQuery(PinyinQueryResult pyQueryResult)
    {
        if (pyQueryResult != null)
        {
            mCandidateView.setSuggestions(pyQueryResult.getCandidateList(), false, false);
            String pinyin = pyQueryResult.getCurrentInput();
            updatePinyin(pinyin);
        }
    }
    
    private void updatePinyin(String pinyin)
    {
        // System.out.println("====" + pinyin);
    }
    
    private void findViewById()
    {
        candidateContainer = (RelativeLayout)findViewById(R.id.candidateContainer);
        handWritingBoard = (HandWritingBoardLayout)findViewById(R.id.handwrtingboard);
        ImageView mIvCancel = (ImageView)findViewById(R.id.iv_cancel);
        mIvCancel.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mCandidateView.clear();
                resetHandWritingRecognize();
            }
        });
    }
}
