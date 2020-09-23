package com.googlecode.openwnn.legacy.ZH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.googlecode.openwnn.legacy.CandidateFilter;
import com.googlecode.openwnn.legacy.ComposingText;
import com.googlecode.openwnn.legacy.OpenWnnDictionaryImpl;
import com.googlecode.openwnn.legacy.StrSegmentClause;
import com.googlecode.openwnn.legacy.WnnClause;
import com.googlecode.openwnn.legacy.WnnDictionary;
import com.googlecode.openwnn.legacy.WnnEngine;
import com.googlecode.openwnn.legacy.WnnSentence;
import com.googlecode.openwnn.legacy.WnnWord;

import android.content.SharedPreferences;

public class OpenWnnEngineZH implements WnnEngine
{
    public static final int DIC_LANG_INIT = 0;
    
    public static final int DIC_LANG_ZH = 0;
    
    public static final int DIC_LANG_EN = 1;
    
    public static final int DIC_LANG_ZH_PERSON_NAME = 2;
    
    public static final int DIC_LANG_EN_EMAIL_ADDRESS = 5;
    
    public static final int DIC_LANG_ZH_POSTAL_ADDRESS = 6;
    
    public static final int KEYBOARD_UNDEF = 0;
    
    public static final int KEYBOARD_KEYPAD12 = 1;
    
    public static final int KEYBOARD_QWERTY = 2;
    
    public static final int MAX_OUTPUT_LENGTH = 50;
    
    public static final int PREDICT_LIMIT = 300;
    
    public static final int FREQ_LEARN = 600;
    
    public static final int FREQ_USER = 500;
    
    protected ConsonantPrediction mConsonantPredictConverter;
    
    private int mDictType = DIC_LANG_INIT;
    
    private int mKeyboardType = KEYBOARD_UNDEF;
    
    private WnnDictionary mDictionaryZH;
    
    private ArrayList<WnnWord> mConvResult;
    
    private HashMap<String, WnnWord> mCandTable;
    
    private String mInputPinyin;
    
    private int mOutputNum;
    
    private int mGetCandidateFrom;
    
    private WnnWord mPreviousWord;
    
    private OpenWnnClauseConverterZH mClauseConverter;
    
    private boolean mExactMatchMode;
    
    private boolean mSingleClauseMode;
    
    private WnnSentence mConvertSentence;
    
    private int mSearchLength;
    
    /* Cache for results of search */
    private HashMap<String, ArrayList<WnnWord>> mSearchCache;
    
    private ArrayList<WnnWord> mSearchCacheArray;
    
    private ArrayList<WnnWord> mNoWord;
    
    private CandidateFilter mFilter;
    
    public OpenWnnEngineZH(String dicLib, String dicFilePath)
    {
        /* load Chinese dictionary library */
        mDictionaryZH = new OpenWnnDictionaryImpl("/data/data/com.whitesky.ktv/lib/" + dicLib, dicFilePath);
        if (!mDictionaryZH.isActive())
        {
            mDictionaryZH = new OpenWnnDictionaryImpl("/system/lib/" + dicLib, dicFilePath);
        }
        
        /* clear dictionary settings */
        mDictionaryZH.clearDictionary();
        mDictionaryZH.clearApproxPattern();
        mDictionaryZH.setInUseState(false);
        
        /* work buffers */
        mConvResult = new ArrayList<WnnWord>();
        mCandTable = new HashMap<String, WnnWord>();
        mSearchCache = new HashMap<String, ArrayList<WnnWord>>();
        mNoWord = new ArrayList<WnnWord>();
        
        /* converters */
        mClauseConverter = new OpenWnnClauseConverterZH();
        mConsonantPredictConverter = new ConsonantPrediction();
        mFilter = new CandidateFilter();
    }
    
    private void setDictionaryForPrediction(int strlen)
    {
        WnnDictionary dict = mDictionaryZH;
        
        dict.clearDictionary();
        dict.clearApproxPattern();
        if (strlen == 0)
        {
            dict.setDictionary(3, 300, 400);
            dict.setDictionary(4, 100, 200);
            dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
        }
        else
        {
            dict.setDictionary(0, 300, 400);
            dict.setDictionary(1, 300, 400);
            if (strlen <= PinyinParser.PINYIN_MAX_LENGTH)
            {
                dict.setDictionary(2, 400, 500); /* single Kanji dictionary */
            }
            dict.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
            dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
            dict.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_TOUPPER);
        }
    }
    
    public void setFilter(CandidateFilter filter)
    {
        mFilter = filter;
        mClauseConverter.setFilter(filter);
    }
    
    private WnnWord getCandidate(int index)
    {
        WnnWord word;
        
        if (mGetCandidateFrom == 0)
        {
            if (mSingleClauseMode)
            {
                /* single clause conversion */
                Iterator<?> convResult = mClauseConverter.convert(mInputPinyin);
                if (convResult != null)
                {
                    while (convResult.hasNext())
                    {
                        addCandidate((WnnWord)convResult.next());
                    }
                }
                /* end of candidates by single clause conversion */
                mGetCandidateFrom = -1;
            }
            else
            {
                /* get prefix matching words from the dictionaries */
                while (index >= mConvResult.size())
                {
                    if ((word = mDictionaryZH.getNextWord()) == null)
                    {
                        if (!mExactMatchMode && mSearchLength > 1)
                        {
                            mGetCandidateFrom = 1;
                        }
                        else
                        {
                            mGetCandidateFrom = 2;
                        }
                        break;
                    }
                    if (mSearchLength == word.stroke.length()
                        || (!mExactMatchMode && (mSearchLength == mInputPinyin.length())))
                    {
                        addCandidate(word);
                    }
                }
            }
        }
        
        if (mGetCandidateFrom == 1)
        {
            /* get common prefix matching words from the dictionaries */
            while (index >= mConvResult.size())
            {
                if ((word = mDictionaryZH.getNextWord()) == null)
                {
                    if (--mSearchLength > 0)
                    {
                        String input = mInputPinyin.substring(0, mSearchLength);
                        if (mSearchLength == PinyinParser.PINYIN_MAX_LENGTH)
                        {
                            /*
                             * if length of the key is less than PinyinParser.PINYIN_MAX_LENGTH, use the single Kanji
                             * dictionary.
                             */
                            mDictionaryZH.setDictionary(2, 400, 500); /*
                                                                       * single Kanji dictionary
                                                                       */
                        }
                        
                        ArrayList<WnnWord> cache = mSearchCache.get(input);
                        if (cache != null)
                        {
                            if (cache != mNoWord)
                            {
                                Iterator<WnnWord> cachei = cache.iterator();
                                while (cachei.hasNext())
                                {
                                    addCandidate(cachei.next());
                                }
                                mSearchCacheArray = cache;
                                mDictionaryZH
                                    .searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, input);
                            }
                        }
                        else
                        {
                            if (PinyinParser.isPinyin(input) && mDictionaryZH
                                .searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, input) > 0)
                            {
                                mSearchCacheArray = new ArrayList<WnnWord>();
                            }
                            else
                            {
                                mSearchCacheArray = mNoWord;
                            }
                            mSearchCache.put(input, mSearchCacheArray);
                        }
                        continue;
                    }
                    else
                    {
                        mGetCandidateFrom = 2;
                        break;
                    }
                }
                if (mSearchLength == word.stroke.length()
                    || (!mExactMatchMode && (mSearchLength == mInputPinyin.length())))
                {
                    if (addCandidate(word))
                    {
                        mSearchCacheArray.add(word);
                    }
                }
            }
        }
        
        if (mGetCandidateFrom == 2)
        {
            /* get a candidate from mConsonantPredictConverter. */
            word = mConsonantPredictConverter.convert(mInputPinyin, mExactMatchMode);
            
            if (word == null)
            {
                mGetCandidateFrom = -1;
            }
            else
            {
                mGetCandidateFrom = 3;
                addCandidate(word);
            }
        }
        
        if (mGetCandidateFrom == 3)
        {
            /* get all candidates from mConsonantPredictConverter. */
            while (index >= mConvResult.size())
            {
                if ((word = mConsonantPredictConverter.nextCandidate()) == null)
                {
                    mGetCandidateFrom = -1;
                    break;
                }
                else
                {
                    addCandidate(word);
                }
            }
        }
        
        if (!mSingleClauseMode && mConvResult.size() > PREDICT_LIMIT)
        {
            mGetCandidateFrom = -1;
        }
        
        if (index >= mConvResult.size())
        {
            return null;
        }
        return (WnnWord)mConvResult.get(index);
    }
    
    protected boolean addCandidate(WnnWord word)
    {
        if (word.candidate == null || mCandTable.containsKey(word.candidate)
            || word.candidate.length() > MAX_OUTPUT_LENGTH)
        {
            return false;
        }
        if (mFilter != null && !mFilter.isAllowed(word))
        {
            return false;
        }
        mCandTable.put(word.candidate, word);
        mConvResult.add(word);
        return true;
    }
    
    protected void clearCandidates()
    {
        mConvResult.clear();
        mCandTable.clear();
        mOutputNum = 0;
        mInputPinyin = null;
        mGetCandidateFrom = 0;
        mSingleClauseMode = false;
    }
    
    public boolean setDictionary(int type)
    {
        mDictType = type;
        mSearchCache.clear();
        return true;
    }
    
    protected int setSearchKey(ComposingText text, int maxLen)
    {
        String input = text.toString(ComposingText.LAYER1);
        if (0 <= maxLen && maxLen <= input.length())
        {
            input = input.substring(0, maxLen);
            mExactMatchMode = true;
        }
        else
        {
            mExactMatchMode = false;
        }
        
        if (input.length() == 0)
        {
            mInputPinyin = "";
            return 0;
        }
        
        mInputPinyin = input;
        
        return input.length();
    }
    
    public void clearPreviousWord()
    {
        mPreviousWord = null;
    }
    
    public void setKeyboardType(int keyboardType)
    {
        mKeyboardType = keyboardType;
    }
    
    public void init()
    {
        clearPreviousWord();
        mClauseConverter.setDictionary(mDictionaryZH);
        mConsonantPredictConverter.setDictionary(mDictionaryZH);
    }
    
    public void close()
    {
        mDictionaryZH.setInUseState(false);
    }
    
    public int predict(ComposingText text, int minLen, int maxLen)
    {
        clearCandidates();
        if (text == null)
        {
            return 0;
        }
        
        /* set mInputPinyin and mInputRomaji */
        int len = setSearchKey(text, maxLen);
        /* set dictionaries by the length of input */
        setDictionaryForPrediction(len);
        /* search dictionaries */
        mDictionaryZH.setInUseState(true);
        mSearchLength = len;
        if (len == 0)
        {
            /* search by previously selected word */
            return mDictionaryZH
                .searchWord(WnnDictionary.SEARCH_LINK, WnnDictionary.ORDER_BY_FREQUENCY, mInputPinyin, mPreviousWord);
        }
        else
        {
            mDictionaryZH.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, mInputPinyin);
            return 1;
        }
    }
    
    public int convert(ComposingText text)
    {
        clearCandidates();
        
        if (text == null)
        {
            return 0;
        }
        
        mDictionaryZH.setInUseState(true);
        
        int cursor = text.getCursor(ComposingText.LAYER1);
        String input;
        WnnClause head = null;
        if (cursor > 0)
        {
            /* convert previous part from cursor */
            input = text.toString(ComposingText.LAYER1, 0, cursor - 1);
            Iterator<?> headCandidates = mClauseConverter.convert(input);
            if ((headCandidates == null) || (!headCandidates.hasNext()))
            {
                return 0;
            }
            WnnWord wd = (WnnWord)headCandidates.next();
            head = new WnnClause(wd.stroke, wd);
            
            /* set the rest of input string */
            input = text.toString(ComposingText.LAYER1, cursor, text.size(ComposingText.LAYER1) - 1);
        }
        else
        {
            /* set whole of input string */
            input = text.toString(ComposingText.LAYER1);
        }
        
        WnnSentence sentence = null;
        if (input.length() != 0)
        {
            sentence = mClauseConverter.consecutiveClauseConvert(input);
        }
        if (head != null)
        {
            sentence = new WnnSentence(head, sentence);
        }
        if (sentence == null)
        {
            return 0;
        }
        
        StrSegmentClause[] ss = new StrSegmentClause[sentence.elements.size()];
        int pos = 0;
        int idx = 0;
        Iterator<WnnClause> it = sentence.elements.iterator();
        while (it.hasNext())
        {
            WnnClause clause = (WnnClause)it.next();
            int len = clause.stroke.length();
            ss[idx] = new StrSegmentClause(clause, pos, pos + len - 1);
            pos += len;
            idx += 1;
        }
        text.setCursor(ComposingText.LAYER2, text.size(ComposingText.LAYER2));
        text.replaceStrSegment(ComposingText.LAYER2, ss, text.getCursor(ComposingText.LAYER2));
        mConvertSentence = sentence;
        
        return 0;
    }
    
    public int searchWords(String key)
    {
        clearCandidates();
        return 0;
    }
    
    public int searchWords(WnnWord word)
    {
        clearCandidates();
        return 0;
    }
    
    public WnnWord getNextCandidate()
    {
        if (mInputPinyin == null)
        {
            return null;
        }
        WnnWord word = getCandidate(mOutputNum);
        if (word != null)
        {
            mOutputNum++;
        }
        return word;
    }
    
    public boolean learn(WnnWord word)
    {
        int ret = -1;
        if (word.partOfSpeech.right == 0)
        {
            word.partOfSpeech = mDictionaryZH.getPOS(WnnDictionary.POS_TYPE_MEISI);
        }
        
        WnnDictionary dict = mDictionaryZH;
        if (word instanceof WnnSentence)
        {
            Iterator<WnnClause> clauses = ((WnnSentence)word).elements.iterator();
            while (clauses.hasNext())
            {
                WnnWord wd = clauses.next();
                if (mPreviousWord != null)
                {
                    ret = dict.learnWord(wd, mPreviousWord);
                }
                else
                {
                    ret = dict.learnWord(wd);
                }
                mPreviousWord = wd;
                if (ret != 0)
                {
                    break;
                }
            }
        }
        else
        {
            if (mPreviousWord != null)
            {
                ret = dict.learnWord(word, mPreviousWord);
            }
            else
            {
                ret = dict.learnWord(word);
            }
            mPreviousWord = word;
            mClauseConverter.setDictionary(dict);
            mConsonantPredictConverter.setDictionary(dict);
            mSearchCache.clear();
        }
        
        return (ret == 0);
    }
    
    public int addWord(WnnWord word)
    {
        mDictionaryZH.setInUseState(true);
        mDictionaryZH.addWordToUserDictionary(word);
        mDictionaryZH.setInUseState(false);
        return 0;
    }
    
    public boolean deleteWord(WnnWord word)
    {
        mDictionaryZH.setInUseState(true);
        mDictionaryZH.removeWordFromUserDictionary(word);
        mDictionaryZH.setInUseState(false);
        return false;
    }
    
    public void setPreferences(SharedPreferences pref)
    {
    }
    
    public void breakSequence()
    {
        clearPreviousWord();
    }
    
    public int makeCandidateListOf(int clausePosition)
    {
        clearCandidates();
        
        if ((mConvertSentence == null) || (mConvertSentence.elements.size() <= clausePosition))
        {
            return 0;
        }
        mSingleClauseMode = true;
        WnnClause clause = mConvertSentence.elements.get(clausePosition);
        mInputPinyin = clause.stroke;
        
        return 1;
    }
    
    public boolean initializeDictionary(int dictionary)
    {
        switch (dictionary)
        {
            case WnnEngine.DICTIONARY_TYPE_LEARN:
                mDictionaryZH.setInUseState(true);
                mDictionaryZH.clearLearnDictionary();
                mDictionaryZH.setInUseState(false);
                return true;
            
            case WnnEngine.DICTIONARY_TYPE_USER:
                mDictionaryZH.setInUseState(true);
                mDictionaryZH.clearUserDictionary();
                mDictionaryZH.setInUseState(false);
                return true;
        }
        return false;
    }
    
    public boolean initializeDictionary(int dictionary, int type)
    {
        return initializeDictionary(dictionary);
    }
    
    public WnnWord[] getUserDictionaryWords()
    {
        /* get words in the user dictionary */
        mDictionaryZH.setInUseState(true);
        WnnWord[] result = mDictionaryZH.getUserDictionaryWords();
        mDictionaryZH.setInUseState(false);
        /* sort the array of words */
        Arrays.sort(result, new WnnWordComparator());
        return result;
    }
    
    /* {@link WnnWord} comparator for listing up words in the user dictionary */
    private class WnnWordComparator implements java.util.Comparator
    {
        public int compare(Object object1, Object object2)
        {
            WnnWord wnnWord1 = (WnnWord)object1;
            WnnWord wnnWord2 = (WnnWord)object2;
            return wnnWord1.stroke.compareTo(wnnWord2.stroke);
        }
    }
}
