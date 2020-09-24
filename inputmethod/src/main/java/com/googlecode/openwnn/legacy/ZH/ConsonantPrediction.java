package com.googlecode.openwnn.legacy.ZH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.googlecode.openwnn.legacy.WnnDictionary;
import com.googlecode.openwnn.legacy.WnnWord;

public class ConsonantPrediction
{
    private static final int FREQ_LEARN = 600;
    
    private static final int FREQ_USER = 500;
    
    private static final int MAX_KANJI_LENGTH = 20;
    
    private WnnDictionary mDictionary;
    
    private HashMap<String, WnnWord> mCheckDuplication;
    
    private List<String> mInputPinyinList;
    
    private boolean mExactMatchMode = false;
    
    private ArrayList<WnnWord> mSearchCache[];
    
    private int mFetchNumFromDict;
    
    private int mCacheNum;
    
    private String mSearchKey;
    
    private Iterator<WnnWord> mCacheIt;
    
    public ConsonantPrediction()
    {
        mCheckDuplication = new HashMap<String, WnnWord>();
        mInputPinyinList = new ArrayList<String>();
        mSearchCache = new ArrayList[MAX_KANJI_LENGTH];
        clearCache();
    }
    
    public void setDictionary(WnnDictionary dict)
    {
        mDictionary = dict;
        clearCache();
    }
    
    private void clearBuffers()
    {
        mCheckDuplication.clear();
        mInputPinyinList.clear();
    }
    
    private void clearCache()
    {
        mSearchKey = "";
        ArrayList<WnnWord>[] cache = mSearchCache;
        for (int i = 0; i < cache.length; i++)
        {
            cache[i] = null;
        }
        mCacheNum = 0;
        mCacheIt = null;
    }
    
    public WnnWord convert(String input, boolean exact)
    {
        /* clear previous result */
        clearBuffers();
        mExactMatchMode = exact;
        /* no result for null or empty string */
        if (input == null || input.length() == 0)
        {
            return null;
        }
        /* split for each pinyin */
        List<String> pinyinList = mInputPinyinList = PinyinParser.getPinyinList(input);
        if (pinyinList.size() < 2)
        {
            return null;
        }
        /* set the dictionary for consonant prediction */
        WnnDictionary dict = mDictionary;
        dict.clearDictionary();
        dict.clearApproxPattern();
        dict.setDictionary(0, 300, 400);
        dict.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
        dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
        dict.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_TOUPPER);
        
        /* get the iterator from the cache */
        String searchKey = pinyinList.get(0);
        if (searchKey.equals(mSearchKey))
        {
            /* can use the cache */
            int pinyinLen = pinyinList.size();
            ArrayList<WnnWord> cache = (mSearchCache.length > pinyinLen) ? mSearchCache[pinyinLen] : null;
            if (cache == null)
            {
                mCacheIt = null; /* not matched */
            }
            else
            {
                mCacheIt = cache.iterator();
            }
        }
        else
        {
            /* cannot use the cache. (search again) */
            clearCache();
            mSearchKey = searchKey;
        }
        
        /* get the first candidate */
        if (mCacheNum < 0)
        {
            /* If all the matched words from the dictionary is fetched already, use the cache. */
            return nextCandidate();
        }
        else if (dict.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, searchKey) > 0)
        {
            mFetchNumFromDict = 0;
            return nextCandidate();
        }
        
        /* no more candidate found */
        mCacheNum = -1;
        return null;
    }
    
    private boolean addCandidate(WnnWord word)
    {
        if (mCheckDuplication.containsKey(word.candidate))
        {
            return false;
        }
        else
        {
            mCheckDuplication.put(word.candidate, word);
            return true;
        }
    }
    
    public WnnWord nextCandidate()
    {
        List<String> pinyinList = mInputPinyinList;
        /* use cache if there are some more candidates */
        if (mCacheIt != null)
        {
            while (mCacheIt.hasNext())
            {
                WnnWord word = mCacheIt.next();
                List<String> pinyinList2 = PinyinParser.getPinyinList(word.stroke);
                if (matchPinyin(pinyinList, pinyinList2, mExactMatchMode))
                {
                    return word;
                }
            }
            return null;
        }
        if (mCacheNum < 0)
        {
            /* End of matched word */
            return null;
        }
        
        /* move the search cursor to the position of the next word. */
        WnnDictionary dict = mDictionary;
        while (mFetchNumFromDict < mCacheNum)
        {
            if (dict.getNextWord() == null)
            {
                mFetchNumFromDict = -1;
                return null;
            }
            mFetchNumFromDict++;
        }
        
        /* fetch words from the dictionary and store into the cache */
        WnnWord word;
        while ((word = dict.getNextWord()) != null)
        {
            mFetchNumFromDict++;
            
            /* store to the cache */
            List<String> pinyinList2 = PinyinParser.getPinyinList(word.stroke);
            int len = pinyinList2.size();
            if (len >= mSearchCache.length)
            {
                len = mSearchCache.length - 1;
            }
            for (int i = 1; i <= len; i++)
            {
                ArrayList<WnnWord> cache = mSearchCache[i];
                if (cache == null)
                {
                    cache = new ArrayList<WnnWord>();
                    mSearchCache[i] = cache;
                }
                cache.add(word);
            }
            mCacheNum++;
            
            /* check if the word is matched */
            if (matchPinyin(pinyinList, pinyinList2, mExactMatchMode))
            {
                if (addCandidate(word))
                {
                    return word;
                }
            }
        }
        
        /* no more words in the dictionary */
        mCacheNum = -1;
        return null;
    }
    
    private boolean matchPinyin(List<String> pinyin1, List<String> pinyin2, boolean exact)
    {
        int len1 = pinyin1.size();
        int len2 = pinyin2.size();
        
        /* check the length of the lists */
        if (exact)
        {
            if (len1 != len2)
            {
                return false;
            }
        }
        else
        {
            if (len1 > len2)
            {
                return false;
            }
        }
        
        /* check each pinyin in the lists */
        int i;
        for (i = 0; i < len1; i++)
        {
            String p1 = pinyin1.get(i);
            String p2 = pinyin2.get(i);
            if (p1.length() > p2.length() || !p1.equals(p2.substring(0, p1.length())))
            {
                break;
            }
        }
        if (i == len1)
        {
            return true;
        }
        return false;
    }
}
