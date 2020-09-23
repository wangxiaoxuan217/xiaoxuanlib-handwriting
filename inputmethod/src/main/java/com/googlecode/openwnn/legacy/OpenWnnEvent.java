package com.googlecode.openwnn.legacy;

import java.util.HashMap;

import android.view.KeyEvent;

public class OpenWnnEvent
{
    public static final int UNDEFINED = 0;
    
    public static final int TOGGLE_REVERSE_CHAR = 0xF0000001;
    
    public static final int CONVERT = 0xF0000002;
    
    public static final int LIST_CANDIDATES_NORMAL = 0xF0000003;
    
    public static final int LIST_CANDIDATES_FULL = 0xF0000004;
    
    public static final int INPUT_CHAR = 0xF0000006;
    
    public static final int TOGGLE_CHAR = 0xF000000C;
    
    public static final int REPLACE_CHAR = 0xF000000D;
    
    public static final int INPUT_KEY = 0xF0000007;
    
    public static final int INPUT_SOFT_KEY = 0xF000000E;
    
    public static final int SELECT_CANDIDATE = 0xF000000B;
    
    public static final int CHANGE_MODE = 0xF000000F;
    
    public static final int COMMIT_COMPOSING_TEXT = 0xF0000010;
    
    public static final int INITIALIZE_USER_DICTIONARY = 0xF0000013;
    
    public static final int INITIALIZE_LEARNING_DICTIONARY = 0xF0000014;
    
    public static final int LIST_WORDS_IN_USER_DICTIONARY = 0xF0000015;
    
    public static final int GET_WORD = 0xF0000018;
    
    public static final int ADD_WORD = 0xF0000016;
    
    public static final int DELETE_WORD = 0xF0000017;
    
    public static final int UPDATE_CANDIDATE = 0xF0000019;
    
    public static final int CHANGE_INPUT_VIEW = 0xF000001C;
    
    public static final int CANDIDATE_VIEW_TOUCH = 0xF000001D;
    
    public static final int KEYUP = 0xF000001F;
    
    public static final int TOUCH_OTHER_KEY = 0xF0000020;
    
    public int code = UNDEFINED;
    
    public int mode = 0;
    
    public char[] chars = null;
    
    public KeyEvent keyEvent = null;
    
    public String[] toggleTable = null;
    
    public HashMap<?, ?> replaceTable = null;
    
    public WnnWord word = null;
    
    public OpenWnnEvent(int code)
    {
        this.code = code;
    }
    
    public OpenWnnEvent(KeyEvent ev)
    {
        if (ev.getAction() != KeyEvent.ACTION_UP)
        {
            this.code = INPUT_KEY;
        }
        else
        {
            this.code = KEYUP;
        }
        this.keyEvent = ev;
    }
    
    public OpenWnnEvent(int code, WnnWord word)
    {
        this.code = code;
        this.word = word;
    }
    
    public static final class Mode
    {
        public static final int DIRECT = 1;
        
        public static final int NO_LV1_CONV = 2;
        
        public static final int NO_LV2_CONV = 3;
    }
}
