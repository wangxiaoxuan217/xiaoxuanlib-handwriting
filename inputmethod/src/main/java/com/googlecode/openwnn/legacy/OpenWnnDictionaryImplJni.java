package com.googlecode.openwnn.legacy;

public class OpenWnnDictionaryImplJni
{
    public static final native long createWnnWork(String dicLibPath);
    
    public static final native int freeWnnWork(long work);
    
    public static final native int clearDictionaryParameters(long work);
    
    public static final native int setDictionaryParameter(long work, int index, int base, int high);
    
    public static final native int searchWord(long work, int operation, int order, String keyString);
    
    public static final native int getNextWord(long work, int length);
    
    public static final native String getStroke(long work);
    
    public static final native String getCandidate(long work);
    
    public static final native int getFrequency(long work);
    
    public static final native int getLeftPartOfSpeech(long work);
    
    public static final native int getRightPartOfSpeech(long work);
    
    public static final native void clearApproxPatterns(long work);
    
    public static final native int setApproxPattern(long work, String src, String dst);
    
    public static final native int setApproxPattern(long work, int approxPattern);
    
    public static final native void clearResult(long work);
    
    public static final native int setLeftPartOfSpeech(long work, int partOfSpeech);
    
    public static final native int setRightPartOfSpeech(long work, int partOfSpeech);
    
    public static final native int setStroke(long work, String stroke);
    
    public static final native int setCandidate(long work, String candidate);
    
    public static final native int selectWord(long work);
    
    public static final native byte[] getConnectArray(long work, int leftPartOfSpeech);
    
    public static final native int getNumberOfLeftPOS(long work);
    
    public static final native int getLeftPartOfSpeechSpecifiedType(long work, int type);
    
    public static final native int getRightPartOfSpeechSpecifiedType(long work, int type);
    
    public static final native String[] createBindArray(long work, String keyString, int maxBindsOfQuery,
        int maxPatternOfApprox);
    
    public static final native String createQueryStringBase(long work, int maxBindsOfQuery, int maxPatternOfApprox,
        String keyColumnName);
}
