package com.googlecode.openwnn.legacy.CLOUDSONG;

import java.util.ArrayList;

import com.googlecode.openwnn.legacy.WnnWord;

public class PinyinQueryResult
{
    private ArrayList<WnnWord> candidateList;
    
    private String currentInput;
    
    public PinyinQueryResult(ArrayList<WnnWord> candidateList, String currentInput)
    {
        this.candidateList = candidateList;
        this.currentInput = currentInput;
    }
    
    public ArrayList<WnnWord> getCandidateList()
    {
        return candidateList;
    }
    
    public String getCurrentInput()
    {
        return currentInput;
    }
}
