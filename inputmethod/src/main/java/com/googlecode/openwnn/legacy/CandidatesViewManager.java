package com.googlecode.openwnn.legacy;

import android.content.SharedPreferences;
import android.view.View;

public interface CandidatesViewManager
{
    int VIEW_TYPE_NORMAL = 0;
    
    int VIEW_TYPE_FULL = 1;
    
    int VIEW_TYPE_CLOSE = 2;
    
    View initView(OpenWnn parent, int width, int height);
    
    View getCurrentView();
    
    int getViewType();
    
    void setViewType(int type);
    
    void displayCandidates(WnnEngine converter);
    
    void clearCandidates();
    
    void setPreferences(SharedPreferences pref);
}
