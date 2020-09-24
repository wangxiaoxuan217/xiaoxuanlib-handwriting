package com.googlecode.openwnn.legacy;

import android.content.SharedPreferences;

public interface WnnEngine
{
    int DICTIONARY_TYPE_LEARN = 1;
    
    int DICTIONARY_TYPE_USER = 2;
    
    void init();
    
    void close();
    
    int predict(ComposingText text, int minLen, int maxLen);
    
    WnnWord getNextCandidate();
    
    WnnWord[] getUserDictionaryWords();
    
    boolean learn(WnnWord word);
    
    int addWord(WnnWord word);
    
    boolean deleteWord(WnnWord word);
    
    boolean initializeDictionary(int dictionary);
    
    void setPreferences(SharedPreferences pref);
    
    void breakSequence();
    
}
