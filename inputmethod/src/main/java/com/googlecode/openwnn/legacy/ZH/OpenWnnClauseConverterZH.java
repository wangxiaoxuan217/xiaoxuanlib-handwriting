package com.googlecode.openwnn.legacy.ZH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.googlecode.openwnn.legacy.CandidateFilter;
import com.googlecode.openwnn.legacy.WnnClause;
import com.googlecode.openwnn.legacy.WnnDictionary;
import com.googlecode.openwnn.legacy.WnnPOS;
import com.googlecode.openwnn.legacy.WnnSentence;
import com.googlecode.openwnn.legacy.WnnWord;

public class OpenWnnClauseConverterZH
{
    public static final int MAX_INPUT_LENGTH = 50;
    
    private static final int FREQ_LEARN = 600;
    
    private static final int FREQ_USER = 500;
    
    private static final int CLAUSE_COST = -1000;
    
    private HashMap<String, ArrayList<WnnWord>> mIndepWordBag;
    
    private HashMap<String, ArrayList<WnnWord>> mAllIndepWordBag;
    
    private byte[][] mConnectMatrix;
    
    private WnnDictionary mDictionary;
    
    private LinkedList<WnnClause> mConvertResult;
    
    private WnnSentence[] mSentenceBuffer;
    
    private WnnPOS mPosDefault;
    
    private WnnPOS mPosEndOfClause1;
    
    private WnnPOS mPosEndOfClause2;
    
    private WnnPOS mPosEndOfClause3;
    
    private CandidateFilter mFilter;
    
    public OpenWnnClauseConverterZH()
    {
        mIndepWordBag = new HashMap<String, ArrayList<WnnWord>>();
        mAllIndepWordBag = new HashMap<String, ArrayList<WnnWord>>();
        mConvertResult = new LinkedList<WnnClause>();
        mSentenceBuffer = new WnnSentence[MAX_INPUT_LENGTH];
    }
    
    public void setDictionary(WnnDictionary dict)
    {
        /* get connect matrix */
        mConnectMatrix = dict.getConnectMatrix();
        /* clear work areas */
        mIndepWordBag.clear();
        mAllIndepWordBag.clear();
        /* clear dictionary settings */
        mDictionary = dict;
        dict.clearDictionary();
        dict.clearApproxPattern();
        
        /* get part of speech tags */
        mPosDefault = dict.getPOS(WnnDictionary.POS_TYPE_MEISI);
        mPosEndOfClause1 = dict.getPOS(WnnDictionary.POS_TYPE_V1);
        mPosEndOfClause2 = dict.getPOS(WnnDictionary.POS_TYPE_V2);
        mPosEndOfClause3 = dict.getPOS(WnnDictionary.POS_TYPE_V3);
    }
    
    public void setFilter(CandidateFilter filter)
    {
        mFilter = filter;
    }
    
    public Iterator<WnnClause> convert(String input)
    {
        /* do nothing if no dictionary is specified */
        if (mConnectMatrix == null || mDictionary == null)
        {
            return null;
        }
        /* do nothing if the length of input exceeds the limit */
        if (input.length() > MAX_INPUT_LENGTH)
        {
            return null;
        }
        
        /* clear the candidates list */
        mConvertResult.clear();
        
        /* try single clause conversion */
        if (!singleClauseConvert(mConvertResult, input, mPosEndOfClause2, true))
        {
            return null;
        }
        return mConvertResult.iterator();
    }
    
    public WnnSentence consecutiveClauseConvert(String input)
    {
        LinkedList<WnnClause> clauses = new LinkedList<WnnClause>();
        
        /* clear the cache which is not matched */
        for (int i = 0; i < input.length(); i++)
        {
            mSentenceBuffer[i] = null;
        }
        WnnSentence[] sentence = mSentenceBuffer;
        
        /* consecutive clause conversion */
        for (int start = 0; start < input.length(); start++)
        {
            if (start != 0 && sentence[start - 1] == null)
            {
                continue;
            }
            
            /* limit the length of a clause */
            int endLimit = input.length();
            if (endLimit > start + 20)
            {
                endLimit = start + 20;
            }
            /* make clauses */
            boolean prefixMatch = true;
            for (int end = start + 1; end <= endLimit; end++)
            {
                int idx = end - 1;
                
                /* cutting a branch */
                if (sentence[idx] != null)
                {
                    if (start != 0)
                    {
                        if (sentence[idx].frequency > sentence[start - 1].frequency + CLAUSE_COST + FREQ_LEARN)
                        {
                            /* there may be no way to be the best sequence from the 'start' */
                            break;
                        }
                    }
                    else
                    {
                        if (sentence[idx].frequency > CLAUSE_COST + FREQ_LEARN)
                        {
                            /* there may be no way to be the best sequence from the 'start' */
                            break;
                        }
                    }
                }
                
                String key = input.substring(start, end);
                WnnClause bestClause = null;
                if (prefixMatch)
                {
                    clauses.clear();
                    if (end == input.length())
                    {
                        /* get the clause which can be the end of the sentence */
                        singleClauseConvert(clauses, key, mPosEndOfClause1, false);
                    }
                    else
                    {
                        /* get the clause which is not the end of the sentence */
                        singleClauseConvert(clauses, key, mPosEndOfClause3, false);
                    }
                    if (clauses.isEmpty())
                    {
                        prefixMatch = false;
                        bestClause = defaultClause(key);
                    }
                    else
                    {
                        bestClause = (WnnClause)clauses.get(0);
                    }
                }
                else
                {
                    bestClause = defaultClause(key);
                }
                
                /* make a sub-sentence */
                WnnSentence ws;
                if (start == 0)
                {
                    ws = new WnnSentence(bestClause.stroke, bestClause);
                }
                else
                {
                    ws = new WnnSentence(sentence[start - 1], bestClause);
                }
                ws.frequency += CLAUSE_COST;
                
                /* update the best sub-sentence on the cache buffer */
                if (sentence[idx] == null || (sentence[idx].frequency < ws.frequency))
                {
                    sentence[idx] = ws;
                }
            }
        }
        
        /* return the result of the consecutive clause conversion */
        if (sentence[input.length() - 1] != null)
        {
            return sentence[input.length() - 1];
        }
        return null;
    }
    
    private boolean singleClauseConvert(LinkedList<WnnClause> clauseList, String input, WnnPOS terminal, boolean all)
    {
        boolean ret = false;
        
        /* get clauses without ancillary word */
        ArrayList<WnnWord> stems = getIndependentWords(input, all);
        if (stems != null)
        {
            if (stems.isEmpty())
            {
                /* all == false && no prefix matched word */
                return false;
            }
            else
            {
                Iterator<WnnWord> stemsi = stems.iterator();
                while (stemsi.hasNext())
                {
                    WnnWord stem = stemsi.next();
                    if (addClause(clauseList, stem.stroke, stem, null, terminal, all))
                    {
                        ret = true;
                        if (!all)
                        {
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    private boolean addClause(LinkedList<WnnClause> clauseList, String input, WnnWord stem, WnnWord fzk,
        WnnPOS terminal, boolean all)
    {
        WnnClause clause = null;
        /* check if the part of speech is valid */
        if (fzk == null)
        {
            if (connectible(stem.partOfSpeech.right, terminal.left))
            {
                clause = new WnnClause(input, stem);
            }
        }
        else
        {
            if (connectible(stem.partOfSpeech.right, fzk.partOfSpeech.left)
                && connectible(fzk.partOfSpeech.right, terminal.left))
            {
                clause = new WnnClause(input, stem, fzk);
            }
        }
        if (clause == null)
        {
            return false;
        }
        if (mFilter != null && !mFilter.isAllowed(clause))
        {
            return false;
        }
        
        /* store to the list */
        if (clauseList.isEmpty())
        {
            /* add if the list is empty */
            clauseList.add(0, clause);
            return true;
        }
        else
        {
            if (!all)
            {
                /* reserve only the best clause */
                WnnClause best = (WnnClause)clauseList.get(0);
                if (best.frequency < clause.frequency)
                {
                    clauseList.set(0, clause);
                    return true;
                }
            }
            else
            {
                /* reserve all clauses */
                Iterator<?> clauseListi = clauseList.iterator();
                int index = 0;
                while (clauseListi.hasNext())
                {
                    WnnClause clausei = (WnnClause)clauseListi.next();
                    if (clausei.frequency < clause.frequency)
                    {
                        break;
                    }
                    index++;
                }
                clauseList.add(index, clause);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean connectible(int right, int left)
    {
        try
        {
            if (mConnectMatrix[left][right] != 0)
            {
                return true;
            }
        }
        catch (Exception ex)
        {
        }
        return false;
    }
    
    private ArrayList<WnnWord> getIndependentWords(String input, boolean all)
    {
        if (input.length() == 0)
        {
            return null;
        }
        
        ArrayList<WnnWord> words = (all) ? mAllIndepWordBag.get(input) : mIndepWordBag.get(input);
        
        if (words == null)
        {
            /* set dictionaries */
            WnnDictionary dict = mDictionary;
            dict.clearDictionary();
            dict.clearApproxPattern();
            
            dict.setDictionary(0, 300, 400);
            dict.setDictionary(1, 300, 400);
            if (input.length() <= PinyinParser.PINYIN_MAX_LENGTH)
            {
                dict.setDictionary(2, 400, 500);
            }
            dict.setDictionary(WnnDictionary.INDEX_USER_DICTIONARY, FREQ_USER, FREQ_USER);
            dict.setDictionary(WnnDictionary.INDEX_LEARN_DICTIONARY, FREQ_LEARN, FREQ_LEARN);
            dict.setApproxPattern(WnnDictionary.APPROX_PATTERN_EN_TOUPPER);
            
            words = new ArrayList<WnnWord>();
            WnnWord word;
            if (all)
            {
                mAllIndepWordBag.put(input, words);
                /* store all words */
                if (dict.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, input) > 0)
                {
                    while ((word = dict.getNextWord(input.length())) != null)
                    {
                        if (input.length() == word.stroke.length())
                        {
                            words.add(word);
                        }
                    }
                }
                /* always add the default candidate */
                addAutoGeneratedCandidates(input, words, all);
            }
            else
            {
                mIndepWordBag.put(input, words);
                /* store the word which has the best score */
                if (dict.searchWord(WnnDictionary.SEARCH_PREFIX, WnnDictionary.ORDER_BY_FREQUENCY, input) > 0)
                {
                    while ((word = dict.getNextWord(input.length())) != null)
                    {
                        if (input.length() == word.stroke.length())
                        {
                            words.add(word);
                            break;
                        }
                    }
                    /* add the default candidate only if there is prefix matching word. */
                    addAutoGeneratedCandidates(input, words, all);
                }
            }
        }
        return words;
    }
    
    private void addAutoGeneratedCandidates(String input, ArrayList<WnnWord> wordList, boolean all)
    {
        wordList.add(defaultClause(input));
    }
    
    private WnnClause defaultClause(String input)
    {
        return (new WnnClause(input, input, mPosDefault, (CLAUSE_COST - 1) * input.length()));
    }
}
