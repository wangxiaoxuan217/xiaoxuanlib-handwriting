package com.googlecode.openwnn.legacy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class CandidateViewButton extends androidx.appcompat.widget.AppCompatButton
{
    private int[] mUpState;
    
    public CandidateViewButton(Context context)
    {
        super(context);
    }
    
    public CandidateViewButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    
    public boolean onTouchEvent(MotionEvent me)
    {
        /* for changing the button on CandidateView when it is pressed. */
        boolean ret = super.onTouchEvent(me);
        Drawable d = getBackground();
        switch (me.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mUpState = d.getState();
                d.setState(View.PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET);
                break;
            case MotionEvent.ACTION_UP:
            default:
                d.setState(mUpState);
                break;
        }
        return ret;
    }
}
