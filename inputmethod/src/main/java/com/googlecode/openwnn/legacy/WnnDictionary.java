package com.googlecode.openwnn.legacy;

public interface WnnDictionary
{
    int APPROX_PATTERN_EN_TOUPPER = 0;
    
    int SEARCH_EXACT = 0;
    
    int SEARCH_PREFIX = 1;
    
    int SEARCH_LINK = 2;
    
    int ORDER_BY_FREQUENCY = 0;
    
    int ORDER_BY_KEY = 1;
    
    int POS_TYPE_V1 = 0;
    
    int POS_TYPE_V2 = 1;
    
    int POS_TYPE_V3 = 2;
    
    int POS_TYPE_MEISI = 6;
    
    int INDEX_USER_DICTIONARY = -1;
    
    int INDEX_LEARN_DICTIONARY = -2;
    
    boolean isActive();
    
    void setInUseState(boolean flag);
    
    int clearDictionary();
    
    int setDictionary(int index, int base, int high);
    
    void clearApproxPattern();
    
    int setApproxPattern(int approxPattern);
    
    int searchWord(int operation, int order, String keyString);
    
    int searchWord(int operation, int order, String keyString, WnnWord wnnWord);
    
    WnnWord getNextWord();
    
    WnnWord getNextWord(int length);
    
    WnnWord[] getUserDictionaryWords();
    
    byte[][] getConnectMatrix();
    
    WnnPOS getPOS(int type);
    
    int clearUserDictionary();
    
    int clearLearnDictionary();
    
    int addWordToUserDictionary(WnnWord[] word);
    
    int addWordToUserDictionary(WnnWord word);
    
    int removeWordFromUserDictionary(WnnWord[] word);
    
    int removeWordFromUserDictionary(WnnWord word);
    
    int learnWord(WnnWord word);
    
    int learnWord(WnnWord word, WnnWord previousWord);
}
