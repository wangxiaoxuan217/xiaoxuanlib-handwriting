package com.googlecode.openwnn.legacy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.softwaretest.R;
import com.googlecode.openwnn.legacy.CLOUDSONG.CandidateCallback;
import com.googlecode.openwnn.legacy.ZH.LetterConverterZH;
import com.googlecode.openwnn.legacy.ZH.CN.OpenWnnEngineZHCN;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.MetaKeyKeyListener;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

public class OpenWnnZHCN extends OpenWnn
{
    public static final int ENGINE_MODE_SYMBOL = 104;
    
    public static final int ENGINE_MODE_OPT_TYPE_QWERTY = 105;
    
    public static final int ENGINE_MODE_OPT_TYPE_12KEY = 106;
    
    private static final boolean FIX_CURSOR_TEXT_END = true;
    
    private static final CharacterStyle SPAN_CONVERT_BGCOLOR_HL = new BackgroundColorSpan(0xFF8888FF);
    
    private static final CharacterStyle SPAN_EXACT_BGCOLOR_HL = new BackgroundColorSpan(0xFF66CDAA);
    
    private static final CharacterStyle SPAN_REMAIN_BGCOLOR_HL = new BackgroundColorSpan(0xFFF0FFFF);
    
    private static final CharacterStyle SPAN_TEXTCOLOR = new ForegroundColorSpan(0xFF000000);
    
    private static final CharacterStyle SPAN_UNDERLINE = new UnderlineSpan();
    
    private static final int STATUS_INIT = 0x0000;
    
    private static final int STATUS_INPUT = 0x0001;
    
    private static final int STATUS_INPUT_EDIT = 0x0003;
    
    private static final int STATUS_CANDIDATE_FULL = 0x0010;
    
    private static final Pattern ENGLISH_CHARACTER_LAST = Pattern.compile(".*[a-zA-Z]$");
    
    private static final int PRIVATE_AREA_CODE = 61184;
    
    private static final int LIMIT_INPUT_NUMBER = 30;
    
    private static final int AUTO_COMMIT_ENGLISH_ON = 0x0000;
    
    private static final int AUTO_COMMIT_ENGLISH_OFF = 0x0001;
    
    private static final int AUTO_COMMIT_ENGLISH_SYMBOL = 0x0010;
    
    private static final int MSG_PREDICTION = 0;
    
    private static final int MSG_START_TUTORIAL = 1;
    
    private static final int MSG_CLOSE = 2;
    
    private static final int PREDICTION_DELAY_MS_SHOWING_CANDIDATE = 200;
    
    private static final String[] SYMBOL_LISTS =
        {SymbolList.SYMBOL_EMOTION, SymbolList.SYMBOL_CHINESE, SymbolList.SYMBOL_ENGLISH};
    
    private static final int[] mShiftKeyToggle =
        {0, MetaKeyKeyListener.META_SHIFT_ON, MetaKeyKeyListener.META_CAP_LOCKED};
    
    private static final int[] mAltKeyToggle = {0, MetaKeyKeyListener.META_ALT_ON, MetaKeyKeyListener.META_ALT_LOCKED};
    
    protected int mStatus = STATUS_INIT;
    
    protected boolean mExactMatchMode = false;
    
    protected SpannableStringBuilder mDisplayText;
    
    private WnnEngine mConverterBack;
    
    private LetterConverterZH mPreConverterBack;
    
    private OpenWnnEngineZHCN mConverterZHCN;
    
    private SymbolList mConverterSymbolEngineBack;
    
    /**
     * Current symbol list
     */
    private int mCurrentSymbol = 0;
    
    /**
     * pre-converter (for symbols)
     */
    private LetterConverterZH mPreConverterSymbols;
    
    /**
     * Conversion Engine's state
     */
    private EngineState mEngineState = new EngineState();
    
    /**
     * Whether learning function is active of not.
     */
    private boolean mEnableLearning = true;
    
    /**
     * Whether prediction is active or not.
     */
    private boolean mEnablePrediction = true;
    
    /**
     * Whether using the converter
     */
    private boolean mEnableConverter = true;
    
    /**
     * Whether displaying the symbol list
     */
    private boolean mEnableSymbolList = true;
    
    /**
     * Whether non ASCII code is enabled
     */
    private boolean mEnableSymbolListNonHalf = true;
    
    /**
     * Auto commit state (in English mode)
     */
    private int mDisableAutoCommitEnglishMask = AUTO_COMMIT_ENGLISH_ON;
    
    /**
     * Whether removing a space before a separator or not. (in English mode)
     */
    private boolean mEnableAutoDeleteSpace = false;
    
    /**
     * Whether auto-spacing is enabled or not.
     */
    private boolean mEnableAutoInsertSpace = true;
    
    /**
     * Whether dismissing the keyboard when the enter key is pressed
     */
    private boolean mEnableAutoHideKeyboard = true;
    
    /**
     * Number of committed clauses on consecutive clause conversion
     */
    
    /**
     * Target layer of the {@link ComposingText}
     */
    private int mTargetLayer = 1;
    
    /**
     * Current orientation of the display
     */
    private int mOrientation = Configuration.ORIENTATION_UNDEFINED;
    
    /**
     * Current normal dictionary set
     */
    private int mPrevDictionarySet = OpenWnnEngineZHCN.DIC_LANG_INIT;
    
    /**
     * Regular expression pattern for English separators
     */
    private Pattern mEnglishAutoCommitDelimiter = null;
    
    /**
     * Cursor position in the composing text
     */
    private int mComposingStartCursor = 0;
    
    /**
     * Cursor position before committing text
     */
    private int mCommitStartCursor = 0;
    
    /**
     * Previous committed text
     */
    private StringBuffer mPrevCommitText = null;
    
    /**
     * Call count of {@code commitText}
     */
    private int mPrevCommitCount = 0;
    
    /**
     * Shift lock status of the Hardware keyboard
     */
    private int mHardShift;
    
    /**
     * SHIFT key state (pressing)
     */
    private boolean mShiftPressing;
    
    /**
     * ALT lock status of the Hardware keyboard
     */
    private int mHardAlt;
    
    /**
     * ALT key state (pressing)
     */
    private boolean mAltPressing;
    
    /**
     * Auto caps mode
     */
    private boolean mAutoCaps = false;
    
    /**
     * List of words in the user dictionary
     */
    private WnnWord[] mUserDictionaryWords = null;
    
    /**
     * Whether there is a continued predicted candidate
     */
    private boolean mHasContinuedPrediction = false;
    
    /**
     * The candidate filter
     */
    private CandidateFilter mFilter;
    
    private CandidateCallback mCandidateCallback;
    
    /**
     * {@code Handler} for drawing candidates/displaying tutorial
     */
    Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case MSG_PREDICTION:
                    updatePrediction();
                    break;
                case MSG_CLOSE:
                    if (mConverterZHCN != null)
                        mConverterZHCN.close();
                    if (mConverterSymbolEngineBack != null)
                        mConverterSymbolEngineBack.close();
                    break;
            }
        }
    };
    
    /**
     * Constructor
     */
    public OpenWnnZHCN()
    {
        super();
        mComposingText = new ComposingText();
        mCandidatesViewManager = new TextCandidatesViewManager(-1);
        mConverter = mConverterZHCN =
            new OpenWnnEngineZHCN("libWnnZHCNDic.so", "/data/data/com.whitesky.ktv/writableZHCN.dic");
        mPreConverter = mPreConverterSymbols = new LetterConverterZH();
        mFilter = new CandidateFilter();
        mDisplayText = new SpannableStringBuilder();
        mAutoHideMode = false;
        mPrevCommitText = new StringBuffer();
    }
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        String delimiter = Pattern.quote(getResources().getString(R.string.en_word_separators));
        mEnglishAutoCommitDelimiter = Pattern.compile(".*[" + delimiter + "]$");
        if (mConverterSymbolEngineBack == null)
        {
            mConverterSymbolEngineBack = new SymbolList(this, SymbolList.LANG_ZHCN);
        }
    }
    
    @Override
    public View onCreateInputView()
    {
        return super.onCreateInputView();
    }
    
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting)
    {
        EngineState state = new EngineState();
        state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
        updateEngineState(state);
        mPrevCommitCount = 0;
        clearCommitInfo();
        super.onStartInputView(attribute, restarting);
        /* initialize views */
        mCandidatesViewManager.clearCandidates();
        /* initialize status */
        mStatus = STATUS_INIT;
        mExactMatchMode = false;
        /* load preferences */
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        /* hardware keyboard support */
        mHardShift = 0;
        mHardAlt = 0;
        updateMetaKeyStateDisplay();
        /* initialize the engine's state */
        fitInputType(pref, attribute);
        ((TextCandidatesViewManager)mCandidatesViewManager).setAutoHide(true);
        if (isEnableL2Converter())
        {
            breakSequence();
        }
    }
    
    @Override
    public void hideWindow()
    {
        mComposingText.clear();
        clearCommitInfo();
        mHandler.removeMessages(MSG_START_TUTORIAL);
        super.hideWindow();
    }
    
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart,
        int candidatesEnd)
    {
        mComposingStartCursor = (candidatesStart < 0) ? newSelEnd : candidatesStart;
        if (newSelStart != newSelEnd)
        {
            clearCommitInfo();
        }
        
        if (mHasContinuedPrediction)
        {
            mHasContinuedPrediction = false;
            if (0 < mPrevCommitCount)
            {
                mPrevCommitCount--;
            }
            return;
        }
        
        boolean isNotComposing = ((candidatesStart < 0) && (candidatesEnd < 0));
        if ((mComposingText.size(ComposingText.LAYER1) != 0) && !isNotComposing)
        {
            updateViewStatus(mTargetLayer, false, true);
        }
        else
        {
            if (0 < mPrevCommitCount)
            {
                mPrevCommitCount--;
            }
            else
            {
                int commitEnd = mCommitStartCursor + mPrevCommitText.length();
                if ((((newSelEnd < oldSelEnd) || (commitEnd < newSelEnd)) && clearCommitInfo()) || isNotComposing)
                {
                    if (isEnableL2Converter())
                    {
                        breakSequence();
                    }
                    
                    if (mInputConnection != null)
                    {
                        if (isNotComposing && (mComposingText.size(ComposingText.LAYER1) != 0))
                        {
                            mInputConnection.finishComposingText();
                        }
                    }
                    initializeScreen();
                }
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        try
        {
            super.onConfigurationChanged(newConfig);
            
            if (mInputConnection != null)
            {
                if (super.isInputViewShown())
                {
                    updateViewStatus(mTargetLayer, true, true);
                }
                
                /* display orientation */
                if (mOrientation != newConfig.orientation)
                {
                    mOrientation = newConfig.orientation;
                    initializeScreen();
                }
            }
        }
        catch (Exception ex)
        {
            /* do nothing if an error occurs. */
        }
    }
    
    /**
     * @see com.googlecode.openwnn.legacy.OpenWnn#onEvent
     */
    @Override
    synchronized public boolean onEvent(OpenWnnEvent ev)
    {
        EngineState state;
        /* handling events which are valid when InputConnection is not active. */
        switch (ev.code)
        {
            case OpenWnnEvent.KEYUP:
                onKeyUpEvent(ev.keyEvent);
                return true;
            case OpenWnnEvent.INITIALIZE_LEARNING_DICTIONARY:
                mConverterZHCN.initializeDictionary(WnnEngine.DICTIONARY_TYPE_LEARN);
                return true;
            case OpenWnnEvent.INITIALIZE_USER_DICTIONARY:
                return mConverterZHCN.initializeDictionary(WnnEngine.DICTIONARY_TYPE_USER);
            case OpenWnnEvent.LIST_WORDS_IN_USER_DICTIONARY:
                mUserDictionaryWords = mConverterZHCN.getUserDictionaryWords();
                return true;
            case OpenWnnEvent.GET_WORD:
                if (mUserDictionaryWords != null)
                {
                    ev.word = mUserDictionaryWords[0];
                    for (int i = 0; i < mUserDictionaryWords.length - 1; i++)
                    {
                        mUserDictionaryWords[i] = mUserDictionaryWords[i + 1];
                    }
                    mUserDictionaryWords[mUserDictionaryWords.length - 1] = null;
                    if (mUserDictionaryWords[0] == null)
                    {
                        mUserDictionaryWords = null;
                    }
                    return true;
                }
                break;
            case OpenWnnEvent.ADD_WORD:
                mConverterZHCN.addWord(ev.word);
                return true;
            case OpenWnnEvent.DELETE_WORD:
                mConverterZHCN.deleteWord(ev.word);
                return true;
            case OpenWnnEvent.CHANGE_MODE:
                changeEngineMode(ev.mode);
                if (ev.mode != ENGINE_MODE_SYMBOL)
                {
                    initializeScreen();
                }
                return true;
            case OpenWnnEvent.UPDATE_CANDIDATE:
                if (mEngineState.isRenbun())
                {
                    mComposingText.setCursor(ComposingText.LAYER1,
                        mComposingText.toString(ComposingText.LAYER1).length());
                    mExactMatchMode = false;
                    updateViewStatusForPrediction(true, true);
                }
                else
                {
                    updateViewStatus(mTargetLayer, true, true);
                }
                return true;
            case OpenWnnEvent.CHANGE_INPUT_VIEW:
                setInputView(onCreateInputView());
                return true;
            case OpenWnnEvent.CANDIDATE_VIEW_TOUCH:
                boolean ret;
                ret = ((TextCandidatesViewManager)mCandidatesViewManager).onTouchSync();
                return ret;
            case OpenWnnEvent.TOUCH_OTHER_KEY:
                mStatus |= STATUS_INPUT_EDIT;
                return true;
            default:
                break;
        }
        KeyEvent keyEvent = ev.keyEvent;
        int keyCode = 0;
        if (keyEvent != null)
        {
            keyCode = keyEvent.getKeyCode();
        }
        if (mDirectInputMode)
        {
            if (ev.code == OpenWnnEvent.INPUT_SOFT_KEY && mInputConnection != null)
            {
                mInputConnection.sendKeyEvent(keyEvent);
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEvent.getKeyCode()));
            }
            return false;
        }
        
        if (!((ev.code == OpenWnnEvent.COMMIT_COMPOSING_TEXT) || ((keyEvent != null)
            && ((keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) || (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
                || (keyCode == KeyEvent.KEYCODE_ALT_LEFT) || (keyCode == KeyEvent.KEYCODE_ALT_RIGHT)
                || (keyEvent.isAltPressed() && (keyCode == KeyEvent.KEYCODE_SPACE))))))
        {
            clearCommitInfo();
        }
        
        /* change back the dictionary if necessary */
        if (!((ev.code == OpenWnnEvent.SELECT_CANDIDATE) || (ev.code == OpenWnnEvent.LIST_CANDIDATES_NORMAL)
            || (ev.code == OpenWnnEvent.LIST_CANDIDATES_FULL)
            || ((keyEvent != null)
                && ((keyCode == KeyEvent.KEYCODE_SHIFT_LEFT) || (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
                    || (keyCode == KeyEvent.KEYCODE_ALT_LEFT) || (keyCode == KeyEvent.KEYCODE_ALT_RIGHT)
                    || (keyCode == KeyEvent.KEYCODE_BACK
                        && mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL)
                    || (keyEvent.isAltPressed() && (keyCode == KeyEvent.KEYCODE_SPACE))))))
        {
            
            state = new EngineState();
            state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
            updateEngineState(state);
        }
        
        if (ev.code == OpenWnnEvent.LIST_CANDIDATES_FULL)
        {
            mStatus |= STATUS_CANDIDATE_FULL;
            mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_FULL);
            return true;
        }
        else if (ev.code == OpenWnnEvent.LIST_CANDIDATES_NORMAL)
        {
            mStatus &= ~STATUS_CANDIDATE_FULL;
            mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
            return true;
        }
        
        boolean ret = false;
        switch (ev.code)
        {
            case OpenWnnEvent.INPUT_CHAR:
                if ((mPreConverter == null) && !isEnableL2Converter())
                {
                    /* direct input (= full-width alphabet/number input) */
                    commitText(false);
                    commitText(new String(ev.chars));
                    mCandidatesViewManager.clearCandidates();
                }
                else if (!isEnableL2Converter())
                {
                    processSoftKeyboardCodeWithoutConversion(ev.chars);
                }
                else
                {
                    processSoftKeyboardCode(ev.chars);
                }
                ret = true;
                break;
            
            case OpenWnnEvent.TOGGLE_CHAR:
                processSoftKeyboardToggleChar(ev.toggleTable);
                ret = true;
                break;
            
            case OpenWnnEvent.TOGGLE_REVERSE_CHAR:
                if (((mStatus & ~STATUS_CANDIDATE_FULL) == STATUS_INPUT) && !(mEngineState.isConvertState()))
                {
                    
                    int cursor = mComposingText.getCursor(ComposingText.LAYER1);
                    if (cursor > 0)
                    {
                        String prevChar = mComposingText.getStrSegment(ComposingText.LAYER1, cursor - 1).string;
                        String c = searchToggleCharacter(prevChar, ev.toggleTable, true);
                        if (c != null)
                        {
                            mComposingText.delete(ComposingText.LAYER1, false);
                            appendStrSegment(new StrSegment(c));
                            updateViewStatusForPrediction(true, true);
                            ret = true;
                            break;
                        }
                    }
                }
                break;
            
            case OpenWnnEvent.REPLACE_CHAR:
                int cursor = mComposingText.getCursor(ComposingText.LAYER1);
                if ((cursor > 0) && !(mEngineState.isConvertState()))
                {
                    
                    String search = mComposingText.getStrSegment(ComposingText.LAYER1, cursor - 1).string;
                    String c = (String)ev.replaceTable.get(search);
                    if (c != null)
                    {
                        mComposingText.delete(1, false);
                        appendStrSegment(new StrSegment(c));
                        updateViewStatusForPrediction(true, true);
                        ret = true;
                        mStatus = STATUS_INPUT_EDIT;
                        break;
                    }
                }
                break;
            
            case OpenWnnEvent.INPUT_KEY:
                /* update shift/alt state */
                switch (keyCode)
                {
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_ALT_LEFT:
                    case KeyEvent.KEYCODE_ALT_RIGHT:
                        if (keyEvent.getRepeatCount() == 0)
                        {
                            if (++mHardAlt > 2)
                            {
                                mHardAlt = 0;
                            }
                        }
                        mAltPressing = true;
                        updateMetaKeyStateDisplay();
                        return true;
                    
                    case KeyEvent.KEYCODE_SHIFT_LEFT:
                    case KeyEvent.KEYCODE_SHIFT_RIGHT:
                        if (keyEvent.getRepeatCount() == 0)
                        {
                            if (++mHardShift > 2)
                            {
                                mHardShift = 0;
                            }
                        }
                        mShiftPressing = true;
                        updateMetaKeyStateDisplay();
                        return true;
                }
                
                /* handle other key event */
                ret = processKeyEvent(keyEvent);
                break;
            
            case OpenWnnEvent.INPUT_SOFT_KEY:
                ret = processKeyEvent(keyEvent);
                if (!ret)
                {
                    mInputConnection.sendKeyEvent(keyEvent);
                    mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEvent.getKeyCode()));
                    ret = true;
                }
                break;
            
            case OpenWnnEvent.SELECT_CANDIDATE:
                initCommitInfoForWatchCursor();
                if (isEnglishPrediction())
                {
                    mComposingText.clear();
                }
                mStatus = commitText(ev.word);
                if (isEnglishPrediction() && !mEngineState.isSymbolList() && mEnableAutoInsertSpace)
                {
                    commitSpaceJustOne();
                }
                checkCommitInfo();
                
                if (mEngineState.isSymbolList())
                {
                    mEnableAutoDeleteSpace = false;
                }
                break;
            
            case OpenWnnEvent.CONVERT:
                startConvert(EngineState.CONVERT_TYPE_RENBUN);
                break;
            
            case OpenWnnEvent.COMMIT_COMPOSING_TEXT:
                commitAllText();
                break;
        }
        
        return ret;
    }
    
    @Override
    public boolean onEvaluateFullscreenMode()
    {
        /* never use full-screen mode */
        return false;
    }
    
    @Override
    public boolean onEvaluateInputViewShown()
    {
        super.onEvaluateInputViewShown();
        return true;
    }
    
    private StrSegment createStrSegment(int charCode)
    {
        if (charCode == 0)
        {
            return null;
        }
        return new StrSegment(Character.toChars(charCode));
    }
    
    private boolean processKeyEvent(KeyEvent ev)
    {
        int key = ev.getKeyCode();
        
        /* keys which produce a glyph */
        if (ev.isPrintingKey())
        {
            /*
             * do nothing if the character is not able to display or the character is dead key
             */
            if ((mHardShift > 0 && mHardAlt > 0) || (ev.isAltPressed() == true && ev.isShiftPressed() == true))
            {
                int charCode = ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON | MetaKeyKeyListener.META_ALT_ON);
                if (charCode == 0 || (charCode & KeyCharacterMap.COMBINING_ACCENT) != 0
                    || charCode == PRIVATE_AREA_CODE)
                {
                    if (mHardShift == 1)
                    {
                        mShiftPressing = false;
                    }
                    if (mHardAlt == 1)
                    {
                        mAltPressing = false;
                    }
                    if (!ev.isAltPressed())
                    {
                        if (mHardAlt == 1)
                        {
                            mHardAlt = 0;
                        }
                    }
                    if (!ev.isShiftPressed())
                    {
                        if (mHardShift == 1)
                        {
                            mHardShift = 0;
                        }
                    }
                    if (!ev.isShiftPressed() && !ev.isAltPressed())
                    {
                        updateMetaKeyStateDisplay();
                    }
                    return true;
                }
            }
            EditorInfo edit = getCurrentInputEditorInfo();
            StrSegment str;
            
            /* get the key character */
            int caps = mHardShift;
            if (mHardShift == 0 && mHardAlt == 0)
            {
                /* no meta key is locked */
                int shift = (mAutoCaps) ? getShiftKeyState(edit) : 0;
                if (shift != mHardShift && (key >= KeyEvent.KEYCODE_A && key <= KeyEvent.KEYCODE_Z))
                {
                    /* handling auto caps for a alphabet character */
                    str = createStrSegment(ev.getUnicodeChar(MetaKeyKeyListener.META_SHIFT_ON));
                }
                else
                {
                    str = createStrSegment(ev.getUnicodeChar());
                }
            }
            else
            {
                if ((key == KeyEvent.KEYCODE_COMMA || key == KeyEvent.KEYCODE_PERIOD) && mHardAlt == 0)
                {
                    str = createStrSegment(ev.getUnicodeChar(mShiftKeyToggle[0] | mAltKeyToggle[0]));
                }
                else
                {
                    str = createStrSegment(ev.getUnicodeChar(mShiftKeyToggle[mHardShift] | mAltKeyToggle[mHardAlt]));
                }
                if (mHardShift == 1)
                {
                    mShiftPressing = false;
                }
                if (mHardAlt == 1)
                {
                    mAltPressing = false;
                }
                /* back to 0 (off) if 1 (on/not locked) */
                if (!ev.isAltPressed())
                {
                    if (mHardAlt == 1)
                    {
                        mHardAlt = 0;
                    }
                }
                if (!ev.isShiftPressed())
                {
                    if (mHardShift == 1)
                    {
                        mHardShift = 0;
                    }
                }
                if (!ev.isShiftPressed() && !ev.isShiftPressed())
                {
                    updateMetaKeyStateDisplay();
                }
            }
            
            if (str == null)
            {
                return true;
            }
            
            /*
             * append the character to the composing text if the character is not TAB
             */
            if (str.string.charAt(0) != '\u0009')
            {
                processHardwareKeyboardInputChar(str, caps);
                return true;
            }
            else
            {
                commitText(true);
                commitText(str.string);
                initializeScreen();
                return true;
            }
            
        }
        else if (key == KeyEvent.KEYCODE_SPACE)
        {
            /* H/W space key */
            processHardwareKeyboardSpaceKey(ev);
            return true;
            
        }
        else if (key == KeyEvent.KEYCODE_SYM)
        {
            /* display the symbol list */
            initCommitInfoForWatchCursor();
            mStatus = commitText(true);
            checkCommitInfo();
            changeEngineMode(ENGINE_MODE_SYMBOL);
            mHardAlt = 0;
            updateMetaKeyStateDisplay();
            return true;
        }
        
        /* Functional key */
        if (mComposingText.size(ComposingText.LAYER1) > 0)
        {
            switch (key)
            {
                case KeyEvent.KEYCODE_DEL:
                    mStatus = STATUS_INPUT_EDIT;
                    if (mEngineState.isConvertState())
                    {
                        mComposingText.setCursor(ComposingText.LAYER1,
                            mComposingText.toString(ComposingText.LAYER1).length());
                        mExactMatchMode = false;
                    }
                    else
                    {
                        if (mComposingText.size(ComposingText.LAYER1) == 1)
                        {
                            initializeScreen();
                            return true;
                        }
                        else
                        {
                            mComposingText.delete(ComposingText.LAYER1, false);
                        }
                    }
                    updateViewStatusForPrediction(true, true);
                    return true;
                
                case KeyEvent.KEYCODE_BACK:
                    if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL)
                    {
                        mStatus &= ~STATUS_CANDIDATE_FULL;
                        mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
                    }
                    else
                    {
                        if (!mEngineState.isConvertState())
                        {
                            initializeScreen();
                            if (mConverter != null)
                            {
                                mConverter.init();
                            }
                        }
                        else
                        {
                            mCandidatesViewManager.clearCandidates();
                            mStatus = STATUS_INPUT_EDIT;
                            mExactMatchMode = false;
                            mComposingText.setCursor(ComposingText.LAYER1,
                                mComposingText.toString(ComposingText.LAYER1).length());
                            updateViewStatusForPrediction(true, true);
                        }
                    }
                    return true;
                
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!isEnableL2Converter())
                    {
                        commitText(false);
                        return false;
                    }
                    else
                    {
                        processLeftKeyEvent();
                        return true;
                    }
                    
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!isEnableL2Converter())
                    {
                        commitText(false);
                    }
                    else
                    {
                        processRightKeyEvent();
                    }
                    return true;
                
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (!isEnglishPrediction())
                    {
                        int cursor = mComposingText.getCursor(ComposingText.LAYER1);
                        if (cursor < 1)
                        {
                            return true;
                        }
                    }
                    initCommitInfoForWatchCursor();
                    mStatus = commitText(true);
                    checkCommitInfo();
                    
                    if (isEnglishPrediction())
                    {
                        initializeScreen();
                    }
                    
                    if (mEnableAutoHideKeyboard)
                    {
                        requestHideSelf(0);
                    }
                    return true;
                
                case KeyEvent.KEYCODE_CALL:
                    return false;
                
                default:
                    return true;
            }
        }
        else
        {
            /* if there is no composing string. */
            if (mCandidatesViewManager.getCurrentView().isShown())
            {
                /* displaying relational prediction candidates */
                switch (key)
                {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if (isEnableL2Converter())
                        {
                            /* initialize the converter */
                            mConverter.init();
                        }
                        mStatus = STATUS_INPUT_EDIT;
                        updateViewStatusForPrediction(true, true);
                        return false;
                    
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if (isEnableL2Converter())
                        {
                            /* initialize the converter */
                            mConverter.init();
                        }
                        mStatus = STATUS_INPUT_EDIT;
                        updateViewStatusForPrediction(true, true);
                        return false;
                    
                    default:
                        return processKeyEventNoInputCandidateShown(ev);
                }
            }
            else
            {
                switch (key)
                {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        if (mEnableAutoHideKeyboard)
                        {
                            requestHideSelf(0);
                            return true;
                        }
                        break;
                    case KeyEvent.KEYCODE_BACK:
                        /*
                         * If 'BACK' key is pressed when the SW-keyboard is shown and the candidates view is not shown,
                         * dismiss the SW-keyboard.
                         */
                        if (isInputViewShown())
                        {
                            requestHideSelf(0);
                            return true;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        
        return false;
    }
    
    private void processHardwareKeyboardSpaceKey(KeyEvent ev)
    {
        /* H/W space key */
        if (ev.isShiftPressed())
        {
            /* change Chinese <-> English mode */
            mHardAlt = 0;
            mHardShift = 0;
            updateMetaKeyStateDisplay();
            if (mEngineState.isEnglish())
            {
                mConverter = mConverterZHCN;
            }
            else
            {
            }
            mCandidatesViewManager.clearCandidates();
            
        }
        else if (ev.isAltPressed())
        {
            /* display the symbol list (G1 specific. same as KEYCODE_SYM) */
            if (!mEngineState.isSymbolList())
            {
                commitAllText();
            }
            changeEngineMode(ENGINE_MODE_SYMBOL);
            mHardAlt = 0;
            updateMetaKeyStateDisplay();
            
        }
        else if (isEnglishPrediction())
        {
            /* Auto commit if English mode */
            if (mComposingText.size(0) == 0)
            {
                commitText(" ");
                mCandidatesViewManager.clearCandidates();
                breakSequence();
            }
            else
            {
                initCommitInfoForWatchCursor();
                commitText(true);
                commitSpaceJustOne();
                checkCommitInfo();
            }
            mEnableAutoDeleteSpace = false;
            
        }
        else
        {
            /* start consecutive clause conversion if Chinese mode */
            if (mComposingText.size(0) == 0)
            {
                commitText(" ");
                mCandidatesViewManager.clearCandidates();
                breakSequence();
            }
            else
            {
                startConvert(EngineState.CONVERT_TYPE_RENBUN);
            }
        }
    }
    
    private void processHardwareKeyboardInputChar(StrSegment str, int caps)
    {
        if (isEnableL2Converter())
        {
            boolean commit = false;
            if (mPreConverter == null)
            {
                Matcher m = mEnglishAutoCommitDelimiter.matcher(str.string);
                if (m.matches())
                {
                    commitText(true);
                    
                    commit = true;
                }
                appendStrSegment(str);
            }
            else
            {
                appendStrSegment(str);
                ((LetterConverterZH)mPreConverter).convert(mComposingText, caps);
            }
            
            if (commit)
            {
                commitText(true);
            }
            else
            {
                mStatus = STATUS_INPUT;
                updateViewStatusForPrediction(true, true);
            }
        }
        else
        {
            appendStrSegment(str);
            boolean completed = true;
            if (mPreConverter != null)
            {
                completed = ((LetterConverterZH)mPreConverter).convert(mComposingText, caps);
            }
            
            if (completed)
            {
                commitText(false);
            }
            else
            {
                updateViewStatus(ComposingText.LAYER1, false, true);
            }
        }
    }
    
    private void updatePrediction()
    {
        int candidates = 0;
        int cursor = mComposingText.getCursor(ComposingText.LAYER1);
        if (isEnableL2Converter() || mEngineState.isSymbolList())
        {
            if (mExactMatchMode)
            {
                /* exact matching */
                candidates = mConverter.predict(mComposingText, 0, cursor);
            }
            else
            {
                /* normal prediction */
                candidates = mConverter.predict(mComposingText, 0, -1);
            }
        }
        
        /* update the candidates view */
        if (candidates > 0)
        {
            mHasContinuedPrediction =
                ((mComposingText.size(ComposingText.LAYER1) == 0) && !mEngineState.isSymbolList());
            // // TODO: 16/4/18
            displayCandidates(mConverter);
        }
        else
        {
            mCandidatesViewManager.clearCandidates();
        }
    }
    
    private void processLeftKeyEvent()
    {
        if (mEngineState.isConvertState())
        {
            if (1 < mComposingText.getCursor(ComposingText.LAYER1))
            {
                mComposingText.moveCursor(ComposingText.LAYER1, -1);
            }
        }
        else if (mExactMatchMode)
        {
            mComposingText.moveCursor(ComposingText.LAYER1, -1);
        }
        else
        {
            if (isEnglishPrediction())
            {
                mComposingText.moveCursor(ComposingText.LAYER1, -1);
            }
            else
            {
                mExactMatchMode = true;
            }
        }
        
        mStatus = STATUS_INPUT_EDIT;
        updateViewStatus(mTargetLayer, true, true);
    }
    
    private void processRightKeyEvent()
    {
        int layer = mTargetLayer;
        ComposingText composingText = mComposingText;
        if (mExactMatchMode || (mEngineState.isConvertState()))
        {
            int textSize = composingText.size(ComposingText.LAYER1);
            if (composingText.getCursor(ComposingText.LAYER1) == textSize)
            {
                mExactMatchMode = false;
                layer = ComposingText.LAYER1; /* convert -> prediction */
                EngineState state = new EngineState();
                state.convertType = EngineState.CONVERT_TYPE_NONE;
                updateEngineState(state);
            }
            else
            {
                composingText.moveCursor(ComposingText.LAYER1, 1);
            }
        }
        else
        {
            if (composingText.getCursor(ComposingText.LAYER1) < composingText.size(ComposingText.LAYER1))
            {
                composingText.moveCursor(ComposingText.LAYER1, 1);
            }
        }
        
        mStatus = STATUS_INPUT_EDIT;
        
        updateViewStatus(layer, true, true);
    }
    
    boolean processKeyEventNoInputCandidateShown(KeyEvent ev)
    {
        boolean ret = true;
        
        switch (ev.getKeyCode())
        {
            case KeyEvent.KEYCODE_DEL:
                ret = true;
                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MENU:
                ret = false;
                break;
            
            case KeyEvent.KEYCODE_CALL:
                return false;
            
            case KeyEvent.KEYCODE_DPAD_CENTER:
                ret = true;
                break;
            
            case KeyEvent.KEYCODE_BACK:
                if (mCandidatesViewManager.getViewType() == CandidatesViewManager.VIEW_TYPE_FULL)
                {
                    mStatus &= ~STATUS_CANDIDATE_FULL;
                    mCandidatesViewManager.setViewType(CandidatesViewManager.VIEW_TYPE_NORMAL);
                    return true;
                }
                else
                {
                    ret = true;
                }
                break;
            
            default:
                return true;
        }
        
        if (mConverter != null)
        {
            /* initialize the converter */
            mConverter.init();
        }
        updateViewStatusForPrediction(true, true);
        return ret;
    }
    
    private void updateViewStatusForPrediction(boolean updateCandidates, boolean updateEmptyText)
    {
        EngineState state = new EngineState();
        state.convertType = EngineState.CONVERT_TYPE_NONE;
        updateEngineState(state);
        sendFindCandidateMessage();
    }
    
    private void updateViewStatus(int layer, boolean updateCandidates, boolean updateEmptyText)
    {
        mTargetLayer = layer;
        
        if (updateCandidates)
        {
            updateCandidateView();
        }
        /* notice to the input view */
        
        /* set the text for displaying as the composing text */
        mDisplayText.clear();
        mDisplayText.insert(0, mComposingText.toString(layer));
        
        /* add decoration to the text */
        int cursor = mComposingText.getCursor(layer);
        if ((mInputConnection != null) && (mDisplayText.length() != 0 || updateEmptyText))
        {
            if (cursor != 0)
            {
                int highlightEnd = 0;
                
                if (mExactMatchMode || (FIX_CURSOR_TEXT_END && isEnglishPrediction()
                    && (cursor < mComposingText.size(ComposingText.LAYER1))))
                {
                    
                    mDisplayText.setSpan(SPAN_EXACT_BGCOLOR_HL, 0, cursor, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    highlightEnd = cursor;
                    
                }
                else if (layer == ComposingText.LAYER2)
                {
                    highlightEnd = mComposingText.toString(layer, 0, 0).length();
                    
                    /* highlights the first segment */
                    mDisplayText.setSpan(SPAN_CONVERT_BGCOLOR_HL, 0, highlightEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                
                if (FIX_CURSOR_TEXT_END && (highlightEnd != 0))
                {
                    /* highlights remaining text */
                    mDisplayText.setSpan(SPAN_REMAIN_BGCOLOR_HL,
                        highlightEnd,
                        mComposingText.toString(layer).length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    
                    /* text color in the highlight */
                    mDisplayText.setSpan(SPAN_TEXTCOLOR,
                        0,
                        mComposingText.toString(layer).length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            
            mDisplayText.setSpan(SPAN_UNDERLINE, 0, mDisplayText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            int displayCursor = mComposingText.toString(layer, 0, cursor - 1).length();
            if (FIX_CURSOR_TEXT_END)
            {
                displayCursor = (cursor == 0) ? 0 : 1;
            }
            /* update the composing text on the EditView */
            mInputConnection.setComposingText(mDisplayText, displayCursor);
        }
    }
    
    private void sendFindCandidateMessage()
    {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PREDICTION), PREDICTION_DELAY_MS_SHOWING_CANDIDATE);
    }
    
    private void updateCandidateView()
    {
        // TODO 注释掉更新view的代码
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PREDICTION), PREDICTION_DELAY_MS_SHOWING_CANDIDATE);
    }
    
    private int commitText(boolean learn)
    {
        if (isEnglishPrediction())
        {
            mComposingText.setCursor(ComposingText.LAYER1, mComposingText.size(ComposingText.LAYER1));
        }
        
        int layer = mTargetLayer;
        int cursor = mComposingText.getCursor(layer);
        if (cursor == 0)
        {
            return mStatus;
        }
        String tmp = mComposingText.toString(layer, 0, cursor - 1);
        
        if (mConverter != null)
        {
            if (learn)
            {
                if (mEngineState.isRenbun())
                {
                    learnWord(0); /* select the top of the clauses */
                }
                else
                {
                    if (mComposingText.size(ComposingText.LAYER1) != 0)
                    {
                        String stroke =
                            mComposingText.toString(ComposingText.LAYER1, 0, mComposingText.getCursor(layer) - 1);
                        WnnWord word = new WnnWord(tmp, stroke);
                        
                        learnWord(word);
                    }
                }
            }
            else
            {
                breakSequence();
            }
        }
        return commitTextThroughInputConnection(tmp);
    }
    
    private void commitAllText()
    {
        initCommitInfoForWatchCursor();
        if (mEngineState.isConvertState())
        {
        }
        else
        {
            mComposingText.setCursor(ComposingText.LAYER1, mComposingText.size(ComposingText.LAYER1));
            mStatus = commitText(true);
        }
        checkCommitInfo();
    }
    
    private int commitText(WnnWord word)
    {
        if (mConverter != null)
        {
            learnWord(word);
        }
        return commitTextThroughInputConnection(word);
    }
    
    private void commitText(String str)
    {
        mInputConnection.commitText(str, (FIX_CURSOR_TEXT_END ? 1 : str.length()));
        mPrevCommitText.append(str);
        mPrevCommitCount++;
        mEnableAutoDeleteSpace = true;
        updateViewStatusForPrediction(false, false);
    }
    
    private int commitTextThroughInputConnection(String string)
    {
        int layer = mTargetLayer;
        
        mInputConnection.commitText(string, (FIX_CURSOR_TEXT_END ? 1 : string.length()));
        mPrevCommitText.append(string);
        mPrevCommitCount++;
        
        int cursor = mComposingText.getCursor(layer);
        if (cursor > 0)
        {
            mComposingText.deleteStrSegment(layer, 0, mComposingText.getCursor(layer) - 1);
            mComposingText.setCursor(layer, mComposingText.size(layer));
        }
        mExactMatchMode = false;
        
        if ((layer == ComposingText.LAYER2) && (mComposingText.size(layer) == 0))
        {
            layer = 1; /* for connected prediction */
        }
        
        boolean commited = autoCommitEnglish();
        mEnableAutoDeleteSpace = true;
        if (layer == ComposingText.LAYER2)
        {
            EngineState state = new EngineState();
            state.convertType = EngineState.CONVERT_TYPE_RENBUN;
            updateEngineState(state);
            updateCandidateView();
        }
        else
        {
            updateViewStatusForPrediction(!commited, false);
        }
        
        if (mComposingText.size(ComposingText.LAYER0) == 0)
        {
            return STATUS_INIT;
        }
        else
        {
            return STATUS_INPUT_EDIT;
        }
    }
    
    public int commitTextThroughInputConnection(WnnWord word)
    {
        String string = word.candidate;
        int layer = mTargetLayer;
        mInputConnection.commitText(string, (FIX_CURSOR_TEXT_END ? 1 : string.length()));
        mPrevCommitText.append(string);
        mPrevCommitCount++;
        
        int cursor = word.stroke.length();
        int position = mComposingText.getCursor(layer);
        
        if (cursor > position)
        {
            cursor = position;
        }
        
        if (cursor > 0)
        {
            mComposingText.deleteStrSegment(layer, 0, cursor - 1);
            mComposingText.setCursor(layer, mComposingText.size(layer));
        }
        mExactMatchMode = false;
        
        if ((layer == ComposingText.LAYER2) && (mComposingText.size(layer) == 0))
        {
            layer = 1;
        }
        
        boolean commited = autoCommitEnglish();
        mEnableAutoDeleteSpace = true;
        
        if (layer == ComposingText.LAYER2)
        {
            EngineState state = new EngineState();
            state.convertType = EngineState.CONVERT_TYPE_RENBUN;
            updateEngineState(state);
            updateViewStatus(layer, !commited, false);
        }
        else
        {
            updateViewStatusForPrediction(!commited, false);
        }
        
        if (mComposingText.size(ComposingText.LAYER0) == 0)
        {
            return STATUS_INIT;
        }
        else
        {
            return STATUS_INPUT_EDIT;
        }
    }
    
    private boolean isEnglishPrediction()
    {
        return (mEngineState.isEnglish() && isEnableL2Converter());
    }
    
    private void changeEngineMode(int mode)
    {
        EngineState state = new EngineState();
        
        switch (mode)
        {
            case ENGINE_MODE_OPT_TYPE_QWERTY:
                state.keyboard = EngineState.KEYBOARD_QWERTY;
                updateEngineState(state);
                clearCommitInfo();
                return;
            
            case ENGINE_MODE_OPT_TYPE_12KEY:
                state.keyboard = EngineState.KEYBOARD_12KEY;
                updateEngineState(state);
                clearCommitInfo();
                return;
            
            case ENGINE_MODE_SYMBOL:
                if (mEnableSymbolList && !mDirectInputMode)
                {
                    state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_SYMBOL;
                    updateEngineState(state);
                    updateViewStatusForPrediction(true, true);
                }
                return;
            
            default:
                break;
        }
        
        state = new EngineState();
        state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
        updateEngineState(state);
        
        state = new EngineState();
        switch (mode)
        {
            case OpenWnnEvent.Mode.DIRECT:
                /* Full/Half-width number or Full-width alphabet */
                mConverter = null;
                mPreConverter = null;
                break;
            
            case OpenWnnEvent.Mode.NO_LV1_CONV:
                /* no Romaji-to-Kana conversion (=English prediction mode) */
                state.dictionarySet = EngineState.DICTIONARYSET_EN;
                updateEngineState(state);
                mPreConverter = null;
                break;
            
            case OpenWnnEvent.Mode.NO_LV2_CONV:
                mConverter = null;
                mPreConverter = null;
                break;
            
            default:
                /* Chinese input mode */
                state.dictionarySet = EngineState.DICTIONARYSET_ZHCN;
                updateEngineState(state);
                mConverter = mConverterZHCN;
                mPreConverter = mPreConverterSymbols;
                break;
        }
        
        mPreConverterBack = (LetterConverterZH)mPreConverter;
        mConverterBack = mConverter;
    }
    
    private void updateEngineState(EngineState state)
    {
        EngineState myState = mEngineState;
        
        /* language */
        if ((state.dictionarySet != EngineState.INVALID) && (myState.dictionarySet != state.dictionarySet))
        {
            
            switch (state.dictionarySet)
            {
                case EngineState.DICTIONARYSET_EN:
                    setDictionary(OpenWnnEngineZHCN.DIC_LANG_EN);
                    break;
                
                case EngineState.DICTIONARYSET_ZHCN:
                default:
                    setDictionary(OpenWnnEngineZHCN.DIC_LANG_ZHCN);
                    break;
            }
            myState.dictionarySet = state.dictionarySet;
            breakSequence();
            
            /* update keyboard setting */
            if (state.keyboard == EngineState.INVALID)
            {
                state.keyboard = myState.keyboard;
            }
        }
        
        /* type of conversion */
        if ((state.convertType != EngineState.INVALID) && (myState.convertType != state.convertType))
        {
            
            switch (state.convertType)
            {
                case EngineState.CONVERT_TYPE_NONE:
                    setDictionary(mPrevDictionarySet);
                    break;
                
                case EngineState.CONVERT_TYPE_RENBUN:
                default:
                    setDictionary(OpenWnnEngineZHCN.DIC_LANG_ZHCN);
                    break;
            }
            myState.convertType = state.convertType;
        }
        
        /* temporary dictionary */
        if (state.temporaryMode != EngineState.INVALID)
        {
            
            switch (state.temporaryMode)
            {
                case EngineState.TEMPORARY_DICTIONARY_MODE_NONE:
                    if (myState.temporaryMode != EngineState.TEMPORARY_DICTIONARY_MODE_NONE)
                    {
                        setDictionary(mPrevDictionarySet);
                        mCurrentSymbol = 0;
                        mPreConverter = mPreConverterBack;
                        mConverter = mConverterBack;
                        mDisableAutoCommitEnglishMask &= ~AUTO_COMMIT_ENGLISH_SYMBOL;
                    }
                    break;
                
                case EngineState.TEMPORARY_DICTIONARY_MODE_SYMBOL:
                    if (++mCurrentSymbol >= SYMBOL_LISTS.length)
                    {
                        mCurrentSymbol = 0;
                    }
                    if (mEnableSymbolListNonHalf)
                    {
                        mConverterSymbolEngineBack.setDictionary(SYMBOL_LISTS[mCurrentSymbol]);
                    }
                    else
                    {
                        mConverterSymbolEngineBack.setDictionary(SymbolList.SYMBOL_ENGLISH);
                    }
                    mConverter = mConverterSymbolEngineBack;
                    mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_SYMBOL;
                    breakSequence();
                    break;
                
                default:
                    break;
            }
            myState.temporaryMode = state.temporaryMode;
        }
        
        /* preference dictionary */
        if ((state.preferenceDictionary != EngineState.INVALID)
            && (myState.preferenceDictionary != state.preferenceDictionary))
        {
            
            myState.preferenceDictionary = state.preferenceDictionary;
            setDictionary(mPrevDictionarySet);
        }
        
        /* keyboard type */
        if (state.keyboard != EngineState.INVALID)
        {
            switch (state.keyboard)
            {
                case EngineState.KEYBOARD_12KEY:
                    mConverterZHCN.setKeyboardType(OpenWnnEngineZHCN.KEYBOARD_KEYPAD12);
                    break;
                
                case EngineState.KEYBOARD_QWERTY:
                default:
                    mConverterZHCN.setKeyboardType(OpenWnnEngineZHCN.KEYBOARD_QWERTY);
                    break;
            }
            myState.keyboard = state.keyboard;
        }
    }
    
    private void setDictionary(int mode)
    {
        int target = mode;
        switch (target)
        {
            
            case OpenWnnEngineZHCN.DIC_LANG_ZHCN:
                
                switch (mEngineState.preferenceDictionary)
                {
                    case EngineState.PREFERENCE_DICTIONARY_PERSON_NAME:
                        target = OpenWnnEngineZHCN.DIC_LANG_ZHCN_PERSON_NAME;
                        break;
                    case EngineState.PREFERENCE_DICTIONARY_POSTAL_ADDRESS:
                        target = OpenWnnEngineZHCN.DIC_LANG_ZHCN_POSTAL_ADDRESS;
                        break;
                    default:
                        break;
                }
                
                break;
            
            case OpenWnnEngineZHCN.DIC_LANG_EN:
                
                switch (mEngineState.preferenceDictionary)
                {
                    case EngineState.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI:
                        target = OpenWnnEngineZHCN.DIC_LANG_EN_EMAIL_ADDRESS;
                        break;
                    default:
                        break;
                }
                
                break;
            
            default:
                break;
        }
        
        switch (mode)
        {
            case OpenWnnEngineZHCN.DIC_LANG_ZHCN:
            case OpenWnnEngineZHCN.DIC_LANG_EN:
                mPrevDictionarySet = mode;
                break;
            default:
                break;
        }
        
        mConverterZHCN.setDictionary(target);
    }
    
    private void processSoftKeyboardToggleChar(String[] table)
    {
        if (table == null)
        {
            return;
        }
        boolean toggled = false;
        if ((mStatus & ~STATUS_CANDIDATE_FULL) == STATUS_INPUT)
        {
            int cursor = mComposingText.getCursor(ComposingText.LAYER1);
            if (cursor > 0)
            {
                String prevChar = mComposingText.getStrSegment(ComposingText.LAYER1, cursor - 1).string;
                String c = searchToggleCharacter(prevChar, table, false);
                if (c != null)
                {
                    mComposingText.delete(ComposingText.LAYER1, false);
                    appendStrSegment(new StrSegment(c));
                    toggled = true;
                }
            }
        }
        
        if (!toggled)
        {
            if (!isEnableL2Converter())
            {
                commitText(false);
            }
            
            String str = table[0];
            /* shift on */
            if (mAutoCaps && isEnglishPrediction() && (getShiftKeyState(getCurrentInputEditorInfo()) == 1))
            {
                
                char top = table[0].charAt(0);
                if (Character.isLowerCase(top))
                {
                    str = Character.toString(Character.toUpperCase(top));
                }
            }
            appendStrSegment(new StrSegment(str));
        }
        
        mStatus = STATUS_INPUT;
        
        updateViewStatusForPrediction(true, true);
    }
    
    private void processSoftKeyboardCodeWithoutConversion(char[] chars)
    {
        if (chars == null)
        {
            return;
        }
        
        ComposingText text = mComposingText;
        appendStrSegment(new StrSegment(chars));
        
        if (!isAlphabetLast(text.toString(ComposingText.LAYER1)))
        {
            /* commit if the input character is not alphabet */
            commitText(false);
        }
    }
    
    public void processSoftKeyboardCode(char[] chars)
    {
        if (chars == null)
        {
            return;
        }
        
        // TODO 在我这里没什么用的代码
        
        /* Auto-commit a word if it is English and Qwerty mode */
        boolean commit = false;
        if (isEnglishPrediction() && (mEngineState.keyboard == EngineState.KEYBOARD_QWERTY))
        {
            
            Matcher m = mEnglishAutoCommitDelimiter.matcher(new String(chars));
            if (m.matches())
            {
                commit = true;
            }
        }
        
        if (commit)
        {
            commitText(true);
            appendStrSegment(new StrSegment(chars));
            commitText(true);
        }
        else
        {
            appendStrSegment(new StrSegment(chars));
            if (mPreConverter != null)
            {
                mPreConverter.convert(mComposingText);
                mStatus = STATUS_INPUT;
            }
            updateViewStatusForPrediction(true, true);
        }
    }
    
    private void startConvert(int convertType)
    {
        if (!isEnableL2Converter())
        {
            return;
        }
        
        if (mEngineState.convertType != convertType)
        {
            /* adjust the cursor position */
            if (!mExactMatchMode)
            {
                if (convertType == EngineState.CONVERT_TYPE_RENBUN)
                {
                    /* not specify */
                    mComposingText.setCursor(ComposingText.LAYER1, 0);
                }
                else
                {
                    if (mEngineState.isRenbun())
                    {
                        /*
                         * EISU-KANA conversion specifying the position of the segment if previous mode is conversion
                         * mode
                         */
                        mExactMatchMode = true;
                    }
                    else
                    {
                        /* specify all range */
                        mComposingText.setCursor(ComposingText.LAYER1, mComposingText.size(ComposingText.LAYER1));
                    }
                }
            }
            
            if (convertType == EngineState.CONVERT_TYPE_RENBUN)
            {
                /* clears variables for the prediction */
                mExactMatchMode = false;
            }
            /* clears variables for the convert */
            EngineState state = new EngineState();
            state.convertType = convertType;
            updateEngineState(state);
            updateViewStatus(ComposingText.LAYER2, true, true);
        }
    }
    
    private boolean autoCommitEnglish()
    {
        if (isEnglishPrediction() && (mDisableAutoCommitEnglishMask == AUTO_COMMIT_ENGLISH_ON))
        {
            CharSequence seq = mInputConnection.getTextBeforeCursor(2, 0);
            Matcher m = mEnglishAutoCommitDelimiter.matcher(seq);
            if (m.matches())
            {
                if ((seq.charAt(0) == ' ') && mEnableAutoDeleteSpace)
                {
                    mInputConnection.deleteSurroundingText(2, 0);
                    CharSequence str = seq.subSequence(1, 2);
                    mInputConnection.commitText(str, 1);
                    mPrevCommitText.append(str);
                    mPrevCommitCount++;
                }
                
                mHandler.removeMessages(MSG_PREDICTION);
                mCandidatesViewManager.clearCandidates();
                return true;
            }
        }
        return false;
    }
    
    private void commitSpaceJustOne()
    {
        CharSequence seq = mInputConnection.getTextBeforeCursor(1, 0);
        if (seq.charAt(0) != ' ')
        {
            commitText(" ");
        }
    }
    
    protected int getShiftKeyState(EditorInfo editor)
    {
        return (getCurrentInputConnection().getCursorCapsMode(editor.inputType) == 0) ? 0 : 1;
    }
    
    private void updateMetaKeyStateDisplay()
    {
        int mode = 0;
    }
    
    private void learnWord(WnnWord word)
    {
        if (mEnableLearning && word != null)
        {
            mConverter.learn(word);
        }
    }
    
    private void learnWord(int index)
    {
        ComposingText composingText = mComposingText;
        
        if (mEnableLearning && composingText.size(ComposingText.LAYER2) > index)
        {
            StrSegment seg = composingText.getStrSegment(ComposingText.LAYER2, index);
            if (seg instanceof StrSegmentClause)
            {
                mConverter.learn(((StrSegmentClause)seg).clause);
            }
            else
            {
                String stroke = composingText.toString(ComposingText.LAYER1, seg.from, seg.to);
                mConverter.learn(new WnnWord(seg.string, stroke));
            }
        }
    }
    
    private void fitInputType(SharedPreferences preference, EditorInfo info)
    {
        if (info.inputType == EditorInfo.TYPE_NULL)
        {
            mDirectInputMode = true;
            return;
        }
        
        mEnableLearning = preference.getBoolean("opt_zhcn_enable_learning", true);
        mEnablePrediction = preference.getBoolean("opt_zhcn_prediction", true);
        mDisableAutoCommitEnglishMask &= ~AUTO_COMMIT_ENGLISH_OFF;
        int preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_NONE;
        mEnableConverter = true;
        mEnableSymbolList = true;
        mEnableSymbolListNonHalf = true;
        mAutoCaps = preference.getBoolean("auto_caps", true);
        mFilter.filter = 0;
        mEnableAutoInsertSpace = true;
        mEnableAutoHideKeyboard = false;
        
        switch (info.inputType & EditorInfo.TYPE_MASK_CLASS)
        {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                mEnableConverter = false;
                break;
            
            case EditorInfo.TYPE_CLASS_PHONE:
                mEnableSymbolList = false;
                mEnableConverter = false;
                break;
            
            case EditorInfo.TYPE_CLASS_TEXT:
                
                switch (info.inputType & EditorInfo.TYPE_MASK_VARIATION)
                {
                    case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME:
                        preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_PERSON_NAME;
                        break;
                    
                    case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
                        mEnableLearning = false;
                        mEnableConverter = false;
                        mEnableSymbolListNonHalf = false;
                        mFilter.filter = CandidateFilter.FILTER_NON_ASCII;
                        mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_OFF;
                        mEnableAutoHideKeyboard = true;
                        break;
                    
                    case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
                        mFilter.filter = CandidateFilter.FILTER_NON_ASCII;
                        mEnableSymbolListNonHalf = false;
                        mEnableAutoInsertSpace = false;
                        mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_OFF;
                        preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI;
                        break;
                    
                    case EditorInfo.TYPE_TEXT_VARIATION_URI:
                        mEnableAutoInsertSpace = false;
                        mDisableAutoCommitEnglishMask |= AUTO_COMMIT_ENGLISH_OFF;
                        preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI;
                        break;
                    
                    case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:
                        preferenceDictionary = EngineState.PREFERENCE_DICTIONARY_POSTAL_ADDRESS;
                        break;
                    
                    case EditorInfo.TYPE_TEXT_VARIATION_PHONETIC:
                        mEnableLearning = false;
                        mEnableConverter = false;
                        mEnableSymbolList = false;
                        break;
                    
                    default:
                        break;
                }
                break;
            
            default:
                break;
        }
        
        if (mFilter.filter == 0)
        {
            mConverterZHCN.setFilter(null);
        }
        else
        {
            mConverterZHCN.setFilter(mFilter);
        }
        
        EngineState state = new EngineState();
        state.preferenceDictionary = preferenceDictionary;
        state.convertType = EngineState.CONVERT_TYPE_NONE;
        state.keyboard = mEngineState.keyboard;
        updateEngineState(state);
        updateMetaKeyStateDisplay();
        
        checkTutorial(info.privateImeOptions);
    }
    
    private void appendStrSegment(StrSegment str)
    {
        ComposingText composingText = mComposingText;
        
        if (composingText.size(ComposingText.LAYER1) >= LIMIT_INPUT_NUMBER)
        {
            return; /* do nothing */
        }
        composingText.insertStrSegment(ComposingText.LAYER0, ComposingText.LAYER1, str);
        return;
    }
    
    public void initializeScreen()
    {
        // // TODO: 16/4/25 更改composingText 表现到ui上
        mComposingText.clear();
        mExactMatchMode = false;
        mStatus = STATUS_INIT;
        mHandler.removeMessages(MSG_PREDICTION);
        View candidateView = mCandidatesViewManager.getCurrentView();
        if ((candidateView != null) && candidateView.isShown())
        {
            mCandidatesViewManager.clearCandidates();
        }
        EngineState state = new EngineState();
        state.temporaryMode = EngineState.TEMPORARY_DICTIONARY_MODE_NONE;
        updateEngineState(state);
    }
    
    private boolean isAlphabetLast(String str)
    {
        Matcher m = ENGLISH_CHARACTER_LAST.matcher(str);
        return m.matches();
    }
    
    @Override
    public void onFinishInput()
    {
        if (mInputConnection != null)
        {
            initializeScreen();
        }
        super.onFinishInput();
    }
    
    private boolean isEnableL2Converter()
    {
        if (mConverter == null || !mEnableConverter)
        {
            return false;
        }
        
        if (mEngineState.isEnglish() && !mEnablePrediction)
        {
            return false;
        }
        
        return true;
    }
    
    private void onKeyUpEvent(KeyEvent ev)
    {
        int key = ev.getKeyCode();
        if (!mShiftPressing)
        {
            if (key == KeyEvent.KEYCODE_SHIFT_LEFT || key == KeyEvent.KEYCODE_SHIFT_RIGHT)
            {
                mHardShift = 0;
                mShiftPressing = true;
                updateMetaKeyStateDisplay();
            }
        }
        if (!mAltPressing)
        {
            if (key == KeyEvent.KEYCODE_ALT_LEFT || key == KeyEvent.KEYCODE_ALT_RIGHT)
            {
                mHardAlt = 0;
                mAltPressing = true;
                updateMetaKeyStateDisplay();
            }
        }
    }
    
    private void initCommitInfoForWatchCursor()
    {
        if (!isEnableL2Converter())
        {
            return;
        }
        
        mCommitStartCursor = mComposingStartCursor;
        mPrevCommitText.delete(0, mPrevCommitText.length());
    }
    
    private boolean clearCommitInfo()
    {
        if (mCommitStartCursor < 0)
        {
            return false;
        }
        
        mCommitStartCursor = -1;
        return true;
    }
    
    private void checkCommitInfo()
    {
        if (mCommitStartCursor < 0)
        {
            return;
        }
        
        int composingLength = mComposingText.toString(mTargetLayer).length();
        CharSequence seq = mInputConnection.getTextBeforeCursor(mPrevCommitText.length() + composingLength, 0);
        seq = seq.subSequence(0, seq.length() - composingLength);
        if (!seq.equals(mPrevCommitText.toString()))
        {
            mPrevCommitCount = 0;
            clearCommitInfo();
        }
    }
    
    private void checkTutorial(String privateImeOptions)
    {
        if (privateImeOptions == null)
            return;
    }
    
    @Override
    protected void close()
    {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CLOSE), 0);
    }
    
    private void breakSequence()
    {
        mEnableAutoDeleteSpace = false;
        mConverterZHCN.breakSequence();
    }
    
    synchronized private void displayCandidates(WnnEngine converter)
    {
        if (mCandidateCallback != null)
        {
            mCandidateCallback.displayCandidate(converter);
        }
    }
    
    public void setCandidateCallBack(CandidateCallback callback)
    {
        this.mCandidateCallback = callback;
    }
    
    public void deleteBy1()
    {
        mComposingText.delete(ComposingText.LAYER1, false);
    }
    
    private class EngineState
    {
        public static final int INVALID = -1;
        
        public static final int DICTIONARYSET_ZHCN = 0;
        
        public static final int DICTIONARYSET_EN = 1;
        
        public static final int CONVERT_TYPE_NONE = 0;
        
        public static final int CONVERT_TYPE_RENBUN = 1;
        
        public static final int TEMPORARY_DICTIONARY_MODE_NONE = 0;
        
        public static final int TEMPORARY_DICTIONARY_MODE_SYMBOL = 1;
        
        public static final int PREFERENCE_DICTIONARY_NONE = 0;
        
        public static final int PREFERENCE_DICTIONARY_PERSON_NAME = 1;
        
        public static final int PREFERENCE_DICTIONARY_POSTAL_ADDRESS = 2;
        
        public static final int PREFERENCE_DICTIONARY_EMAIL_ADDRESS_URI = 3;
        
        public static final int KEYBOARD_QWERTY = 1;
        
        public static final int KEYBOARD_12KEY = 2;
        
        public int dictionarySet = INVALID;
        
        public int convertType = INVALID;
        
        public int temporaryMode = INVALID;
        
        public int preferenceDictionary = INVALID;
        
        public int keyboard = INVALID;
        
        public boolean isRenbun()
        {
            return convertType == CONVERT_TYPE_RENBUN;
        }
        
        public boolean isConvertState()
        {
            return convertType != CONVERT_TYPE_NONE;
        }
        
        public boolean isSymbolList()
        {
            return temporaryMode == TEMPORARY_DICTIONARY_MODE_SYMBOL;
        }
        
        public boolean isEnglish()
        {
            return dictionarySet == DICTIONARYSET_EN;
        }
    }
    
}
