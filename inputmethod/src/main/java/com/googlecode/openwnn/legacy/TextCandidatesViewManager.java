package com.googlecode.openwnn.legacy;

import java.util.ArrayList;

import com.example.softwaretest.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TextCandidatesViewManager implements CandidatesViewManager, GestureDetector.OnGestureListener
{
    public static final int LINE_NUM_PORTRAIT = 2;
    
    public static final int LINE_NUM_LANDSCAPE = 1;
    
    private static final int CANDIDATE_MINIMUM_WIDTH = 48;
    
    private static final int CANDIDATE_MINIMUM_HEIGHT = 35;
    
    private static final int CANDIDATE_LEFT_ALIGN_THRESHOLD = 120;
    
    private static final int FULL_VIEW_DIV = 4;
    
    private final DisplayMetrics mMetrics = new DisplayMetrics();
    
    private ViewGroup mViewBody;
    
    private ScrollView mViewBodyScroll;
    
    private ViewGroup mViewCandidateBase;
    
    private View mViewScaleUp;
    
    private LinearLayout mViewCandidateList1st;
    
    private RelativeLayout mViewCandidateList2nd;
    
    private OpenWnn mWnn;
    
    private int mViewType;
    
    private boolean mPortrait;
    
    private int mViewWidth;
    
    private boolean mAutoHideMode;
    
    private WnnEngine mConverter;
    
    private int mDisplayLimit;
    
    private MediaPlayer mSound = null;
    
    private int mWordCount;
    
    private ArrayList<WnnWord> mWnnWordArray;
    
    private GestureDetector mGestureDetector;
    
    private WnnWord mWord;
    
    private int mLineLength = 0;
    
    private int mLineCount = 1;
    
    private boolean mIsScaleUp = false;
    
    private boolean mIsFullView = false;
    
    private MotionEvent mMotionEvent = null;
    
    private int mDisplayEndOffset = 0;
    
    private boolean mCanReadMore = false;
    
    private int mTextColor = 0;
    
    private TextView mViewCandidateTemplate;
    
    private int mFullViewWordCount;
    
    private int mFullViewOccupyCount;
    
    private TextView mFullViewPrevView;
    
    private int mFullViewPrevLineTopId;
    
    private RelativeLayout.LayoutParams mFullViewPrevParams;
    
    private boolean mCreateCandidateDone;
    
    private int mNormalViewWordCountOfLine;
    
    private OnTouchListener mCandidateOnTouch = new OnTouchListener()
    {
        public boolean onTouch(View v, MotionEvent event)
        {
            if (mMotionEvent != null)
            {
                return true;
            }
            
            if ((event.getAction() == MotionEvent.ACTION_UP) && (v instanceof TextView))
            {
                Drawable d = v.getBackground();
                if (d != null)
                {
                    d.setState(new int[] {});
                }
            }
            
            mMotionEvent = event;
            boolean ret = mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.CANDIDATE_VIEW_TOUCH));
            mMotionEvent = null;
            return ret;
        }
    };
    
    private OnClickListener mCandidateOnClick = new OnClickListener()
    {
        public void onClick(View v)
        {
            if (!v.isShown())
            {
                return;
            }
            
            if (v instanceof TextView)
            {
                TextView text = (TextView)v;
                int wordcount = text.getId();
                WnnWord word = null;
                word = mWnnWordArray.get(wordcount);
                selectCandidate(word);
            }
        }
    };
    
    private OnLongClickListener mCandidateOnLongClick = new OnLongClickListener()
    {
        public boolean onLongClick(View v)
        {
            if (mViewScaleUp == null)
            {
                return false;
            }
            
            if (!v.isShown())
            {
                return true;
            }
            
            Drawable d = v.getBackground();
            if (d != null)
            {
                if (d.getState().length == 0)
                {
                    return true;
                }
            }
            int wordcount = ((TextView)v).getId();
            mWord = mWnnWordArray.get(wordcount);
            setViewScaleUp(true, mWord);
            return true;
        }
    };
    
    public TextCandidatesViewManager(int displayLimit)
    {
        this.mDisplayLimit = displayLimit;
        this.mWnnWordArray = new ArrayList<WnnWord>();
        this.mAutoHideMode = true;
        mMetrics.setToDefaults();
    }
    
    public void setAutoHide(boolean hide)
    {
        mAutoHideMode = hide;
    }
    
    public View initView(OpenWnn parent, int width, int height)
    {
        mWnn = parent;
        mViewWidth = width;
        mPortrait = (parent.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE);
        Resources r = mWnn.getResources();
        LayoutInflater inflater = parent.getLayoutInflater();
        mViewBody = (ViewGroup)inflater.inflate(R.layout.candidates, null);
        mViewBodyScroll = (ScrollView)mViewBody.findViewById(R.id.candview_scroll);
        mViewBodyScroll.setOnTouchListener(mCandidateOnTouch);
        mViewCandidateBase = (ViewGroup)mViewBody.findViewById(R.id.candview_base);
        createNormalCandidateView();
        mViewCandidateList2nd = (RelativeLayout)mViewBody.findViewById(R.id.candidates_2nd_view);
        mTextColor = r.getColor(R.color.candidate_text);
        setViewType(CandidatesViewManager.VIEW_TYPE_CLOSE);
        mGestureDetector = new GestureDetector(this);
        View scaleUp = (View)inflater.inflate(R.layout.candidate_scale_up, null);
        mViewScaleUp = scaleUp;
        /* select button */
        Button b = (Button)scaleUp.findViewById(R.id.candidate_select);
        b.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                selectCandidate(mWord);
            }
        });
        
        /* cancel button */
        b = (Button)scaleUp.findViewById(R.id.candidate_cancel);
        b.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.UPDATE_CANDIDATE));
            }
        });
        return mViewBody;
    }
    
    private void createNormalCandidateView()
    {
        mViewCandidateList1st = (LinearLayout)mViewBody.findViewById(R.id.candidates_1st_view);
        mViewCandidateList1st.setOnTouchListener(mCandidateOnTouch);
        mViewCandidateList1st.setOnClickListener(mCandidateOnClick);
        int line = getMaxLine();
        int width = mViewWidth;
        for (int i = 0; i < line; i++)
        {
            LinearLayout lineView = new LinearLayout(mViewBodyScroll.getContext());
            lineView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lineView.setLayoutParams(layoutParams);
            for (int j = 0; j < (width / getCandidateMinimumWidth()); j++)
            {
                TextView tv = createCandidateView();
                lineView.addView(tv);
            }
            
            if (i == 0)
            {
                TextView tv = createCandidateView();
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.weight = 0;
                layoutParams.gravity = Gravity.RIGHT;
                tv.setLayoutParams(layoutParams);
                
                lineView.addView(tv);
                mViewCandidateTemplate = tv;
            }
            mViewCandidateList1st.addView(lineView);
        }
    }
    
    public View getCurrentView()
    {
        return mViewBody;
    }
    
    private boolean setViewLayout(int type)
    {
        return true;
    }
    
    public int getViewType()
    {
        return mViewType;
    }
    
    public void setViewType(int type)
    {
        boolean readMore = setViewLayout(type);
        
        if (readMore)
        {
            displayCandidates(this.mConverter, false, -1);
        }
        else
        {
            if (type == CandidatesViewManager.VIEW_TYPE_NORMAL)
            {
                mIsFullView = false;
                if (mDisplayEndOffset > 0)
                {
                    int maxLine = getMaxLine();
                    displayCandidates(this.mConverter, false, maxLine);
                }
                else
                {
                    setReadMore();
                }
            }
            else
            {
                if (mViewBody.isShown())
                {
                    mWnn.setCandidatesViewShown(false);
                }
            }
        }
    }
    
    public void displayCandidates(WnnEngine converter)
    {
        
        mCanReadMore = false;
        mDisplayEndOffset = 0;
        mIsFullView = false;
        mFullViewWordCount = 0;
        mFullViewOccupyCount = 0;
        mFullViewPrevLineTopId = 0;
        mCreateCandidateDone = false;
        mNormalViewWordCountOfLine = 0;
        clearCandidates();
        mConverter = converter;
        setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
        displayCandidates(converter, true, getMaxLine());
    }
    
    @Override
    public void clearCandidates()
    {
        
    }

    @Override
    public void setPreferences(SharedPreferences pref) {

    }

    private int getMaxLine()
    {
        int maxLine = (mPortrait) ? LINE_NUM_PORTRAIT : LINE_NUM_LANDSCAPE;
        return maxLine;
    }
    
    synchronized private void displayCandidates(WnnEngine converter, boolean dispFirst, int maxLine)
    {
        if (converter == null)
        {
            return;
        }
        
        /*
         * Concatenate the candidates already got and the last one in dispFirst mode
         */
        int displayLimit = mDisplayLimit;
        boolean isBreak = false;
        /* Get candidates */
        WnnWord result = null;
        while ((displayLimit == -1 || mWordCount < displayLimit))
        {
            result = converter.getNextCandidate();
            if (result == null)
            {
                break;
            }
            setCandidate(false, result);
        }
        
        if (!isBreak && !mCreateCandidateDone)
        {
            /* align left if necessary */
            createNextLine();
            mCreateCandidateDone = true;
        }
        
        if (mWordCount < 1)
        { /* no candidates */
            if (mAutoHideMode)
            {
                mWnn.setCandidatesViewShown(false);
                return;
            }
            else
            {
                mCanReadMore = false;
                mIsFullView = false;
                setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
            }
        }
        setReadMore();
        if (!(mViewBody.isShown()))
        {
            mWnn.setCandidatesViewShown(true);
        }
        return;
    }
    
    private void setCandidate(boolean isCategory, WnnWord word)
    {
        int textLength = measureText(word.candidate, 0, word.candidate.length());
        TextView template = mViewCandidateTemplate;
        textLength += template.getPaddingLeft() + template.getPaddingRight();
        int maxWidth = mViewWidth;
        
        TextView textView;
        if (mIsFullView || getMaxLine() < mLineCount)
        {
            /* Full view */
            int indentWidth = mViewWidth / FULL_VIEW_DIV;
            int occupyCount = Math.min((textLength + indentWidth) / indentWidth, FULL_VIEW_DIV);
            if (isCategory)
            {
                occupyCount = FULL_VIEW_DIV;
            }
            
            if (FULL_VIEW_DIV < (mFullViewOccupyCount + occupyCount))
            {
                if (FULL_VIEW_DIV != mFullViewOccupyCount)
                {
                    mFullViewPrevParams.width += (FULL_VIEW_DIV - mFullViewOccupyCount) * indentWidth;
                    mViewCandidateList2nd.updateViewLayout(mFullViewPrevView, mFullViewPrevParams);
                }
                mFullViewOccupyCount = 0;
                mFullViewPrevLineTopId = mFullViewPrevView.getId();
                mLineCount++;
            }
            
            RelativeLayout layout = mViewCandidateList2nd;
            
            int width = indentWidth * occupyCount;
            int height = getCandidateMinimumHeight();
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
            
            if (mFullViewPrevLineTopId == 0)
            {
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            }
            else
            {
                params.addRule(RelativeLayout.BELOW, mFullViewPrevLineTopId);
            }
            
            if (mFullViewOccupyCount == 0)
            {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }
            else
            {
                params.addRule(RelativeLayout.RIGHT_OF, (mWordCount - 1));
            }
            
            textView = (TextView)layout.getChildAt(mFullViewWordCount);
            if (textView == null)
            {
                textView = createCandidateView();
                textView.setLayoutParams(params);
                
                mViewCandidateList2nd.addView(textView);
            }
            else
            {
                mViewCandidateList2nd.updateViewLayout(textView, params);
            }
            mFullViewOccupyCount += occupyCount;
            mFullViewWordCount++;
            mFullViewPrevView = textView;
            mFullViewPrevParams = params;
        }
        else
        {
            textLength = Math.max(textLength, getCandidateMinimumWidth());
            /* Normal view */
            int nextEnd = mLineLength + textLength;
            if (mLineCount == 1)
            {
                maxWidth -= getCandidateMinimumWidth();
            }
            
            if ((maxWidth < nextEnd) && (mWordCount != 0))
            {
                createNextLine();
                if (getMaxLine() < mLineCount)
                {
                    mLineLength = 0;
                    /*
                     * Call this method again to add the candidate in the full view
                     */
                    setCandidate(isCategory, word);
                    return;
                }
                mLineLength = textLength;
            }
            else
            {
                mLineLength = nextEnd;
            }
            
            LinearLayout lineView = (LinearLayout)mViewCandidateList1st.getChildAt(mLineCount - 1);
            textView = (TextView)lineView.getChildAt(mNormalViewWordCountOfLine);
            
            if (isCategory)
            {
                if (mLineCount == 1)
                {
                    mViewCandidateTemplate.setBackgroundDrawable(null);
                }
                mLineLength += CANDIDATE_LEFT_ALIGN_THRESHOLD;
            }
            mNormalViewWordCountOfLine++;
        }
        
        textView.setText(word.candidate);
        textView.setTextColor(mTextColor);
        textView.setId(mWordCount);
        textView.setVisibility(View.VISIBLE);
        textView.setPressed(false);
        if (isCategory)
        {
            textView.setOnClickListener(null);
            textView.setOnLongClickListener(null);
            textView.setBackgroundDrawable(null);
        }
        else
        {
            textView.setOnClickListener(mCandidateOnClick);
            textView.setOnLongClickListener(mCandidateOnLongClick);
            textView.setBackgroundResource(R.drawable.cand_back);
        }
        textView.setOnTouchListener(mCandidateOnTouch);
        
        if (maxWidth < textLength)
        {
            textView.setEllipsize(TextUtils.TruncateAt.END);
        }
        else
        {
            textView.setEllipsize(null);
        }
        
        ImageSpan span = null;
        if (word.candidate.equals(" "))
        {
            span = new ImageSpan(mWnn, R.drawable.word_half_space, DynamicDrawableSpan.ALIGN_BASELINE);
        }
        else if (word.candidate.equals("\u3000" /* full-width space */))
        {
            span = new ImageSpan(mWnn, R.drawable.word_full_space, DynamicDrawableSpan.ALIGN_BASELINE);
        }
        
        if (span != null)
        {
            SpannableString spannable = new SpannableString("   ");
            spannable.setSpan(span, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
        }
        
        mWnnWordArray.add(mWordCount, word);
        mWordCount++;
    }
    
    private TextView createCandidateView()
    {
        TextView text = new TextView(mViewBodyScroll.getContext());
        text.setTextSize(20);
        text.setBackgroundResource(R.drawable.cand_back);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine();
        text.setPadding(4, 4, 4, 4);
        text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        text.setMinHeight(getCandidateMinimumHeight());
        text.setMinimumWidth(getCandidateMinimumWidth());
        return text;
    }
    
    private void setReadMore()
    {
        if (mIsScaleUp)
        {
            mViewCandidateTemplate.setVisibility(View.GONE);
            return;
        }
        
        if (mIsFullView)
        {
        }
        else
        {
            if (mCanReadMore)
            {
            }
            else
            {
                mViewCandidateTemplate.setVisibility(View.GONE);
            }
        }
    }
    

    public boolean onTouchSync()
    {
        return mGestureDetector.onTouchEvent(mMotionEvent);
    }
    
    private void selectCandidate(WnnWord word)
    {
        setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
        if (mSound != null)
        {
            try
            {
                mSound.seekTo(0);
                mSound.start();
            }
            catch (Exception ex)
            {
            }
        }
        mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.SELECT_CANDIDATE, word));
    }
    
    public boolean onDown(MotionEvent arg0)
    {
        return false;
    }
    
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3)
    {
        if (mIsScaleUp)
        {
            return false;
        }
        
        boolean consumed = false;
        if (arg1 != null && arg0 != null && arg1.getY() < arg0.getY())
        {
            if ((mViewType == CandidatesViewManager.VIEW_TYPE_NORMAL) && mCanReadMore)
            {
                mIsFullView = true;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_FULL));
                consumed = true;
            }
        }
        else
        {
            if (mViewBodyScroll.getScrollY() == 0)
            {
                mIsFullView = false;
                mWnn.onEvent(new OpenWnnEvent(OpenWnnEvent.LIST_CANDIDATES_NORMAL));
                consumed = true;
            }
        }
        
        return consumed;
    }
    
    public void onLongPress(MotionEvent arg0)
    {
        return;
    }
    
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3)
    {
        return false;
    }
    
    public void onShowPress(MotionEvent arg0)
    {
    }
    
    public boolean onSingleTapUp(MotionEvent arg0)
    {
        return false;
    }
    
    public int measureText(CharSequence text, int start, int end)
    {
        TextPaint paint = mViewCandidateTemplate.getPaint();
        return (int)paint.measureText(text, start, end);
    }
    
    private void setViewScaleUp(boolean up, WnnWord word)
    {
        if (up == mIsScaleUp || (mViewScaleUp == null))
        {
            return;
        }
        
        if (up)
        {
            setViewLayout(CandidatesViewManager.VIEW_TYPE_NORMAL);
            mViewCandidateList1st.setVisibility(View.GONE);
            mViewCandidateBase.setMinimumHeight(-1);
            mViewCandidateBase.addView(mViewScaleUp);
            TextView text = (TextView)mViewScaleUp.findViewById(R.id.candidate_scale_up_text);
            text.setText(word.candidate);
            if (!mPortrait)
            {
                Resources r = mViewBodyScroll.getContext().getResources();
                text.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_delete_word_size_landscape));
            }
            
            mIsScaleUp = true;
            setReadMore();
        }
        else
        {
            mIsScaleUp = false;
            mViewCandidateBase.removeView(mViewScaleUp);
        }
    }
    
    private void createNextLine()
    {
        int lineCount = mLineCount;
        if (mIsFullView || getMaxLine() < lineCount)
        {
            /* Full view */
            mFullViewOccupyCount = 0;
            mFullViewPrevLineTopId = mFullViewPrevView.getId();
        }
        else
        {
            /* Normal view */
            LinearLayout lineView = (LinearLayout)mViewCandidateList1st.getChildAt(lineCount - 1);
            float weight = 0;
            if (mLineLength < CANDIDATE_LEFT_ALIGN_THRESHOLD)
            {
                if (lineCount == 1)
                {
                    mViewCandidateTemplate.setVisibility(View.GONE);
                }
            }
            else
            {
                weight = 1.0f;
            }
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            
            int child = lineView.getChildCount();
            for (int i = 0; i < child; i++)
            {
                View view = lineView.getChildAt(i);
                
                if (view != mViewCandidateTemplate)
                {
                    view.setLayoutParams(params);
                }
            }
            
            mLineLength = 0;
            mNormalViewWordCountOfLine = 0;
        }
        mLineCount++;
    }
    
    private int getCandidateMinimumWidth()
    {
        return (int)(CANDIDATE_MINIMUM_WIDTH * mMetrics.density);
    }
    
    private int getCandidateMinimumHeight()
    {
        return (int)(CANDIDATE_MINIMUM_HEIGHT * mMetrics.density);
    }
}
