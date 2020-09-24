package com.googlecode.openwnn.legacy.CLOUDSONG;

import java.util.ArrayList;

import com.googlecode.openwnn.legacy.OpenWnnZHCN;
import com.googlecode.openwnn.legacy.WnnEngine;
import com.googlecode.openwnn.legacy.WnnWord;

public class CloudKeyboardInputManager implements CandidateCallback
{
    private static final int COUNT_LIMIT = 300;
    
    private OpenWnnZHCN mOpenWnnZHCN = new OpenWnnZHCN();
    
    private StringBuilder currentPinYin = new StringBuilder();
    
    private OnPinyinQuery mOnPinyinQuery;
    
    private ArrayList<WnnWord> mCurrentResult;
    
    public CloudKeyboardInputManager()
    {
        mOpenWnnZHCN.setCandidateCallBack(this);
    }
    
    public void delAll()
    {
        int loop = currentPinYin.length();
        for (int i = 0; i < loop; ++i)
        {
            mOpenWnnZHCN.deleteBy1();
        }
        if (loop > 0)
        {
            currentPinYin.delete(0, loop);
        }
    }
    
    private void candidateQueryed(WnnEngine converter)
    {
        if (converter == null)
        {
            return;
        }
        
        int displayLimit = -1;
        WnnWord result = null;
        ArrayList<WnnWord> resultList = new ArrayList<WnnWord>();
        int count = 0;
        while (displayLimit == -1 && count < COUNT_LIMIT)
        {
            count++;
            try
            {
                result = converter.getNextCandidate();
                if (result == null)
                {
                    break;
                }
                resultList.add(result);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                break;
            }
        }
        // TODO 回调机制
        mCurrentResult = resultList;
        keyboardActionCallBack();
        return;
    }
    
    public void setOnPinyinQuery(OnPinyinQuery onPYQuery)
    {
        this.mOnPinyinQuery = onPYQuery;
    }
    
    private void keyboardActionCallBack()
    {
        if (mOnPinyinQuery != null)
        {
            mOnPinyinQuery.onPinyinQuery(new PinyinQueryResult(mCurrentResult, currentPinYin.toString()));
        }
    }
    
    @Override
    public void displayCandidate(WnnEngine converter)
    {
        candidateQueryed(converter);
    }
    
}
