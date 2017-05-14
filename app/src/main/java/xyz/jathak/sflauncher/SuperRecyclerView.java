package xyz.jathak.sflauncher;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SuperRecyclerView extends RecyclerView {
    public SuperRecyclerView(Context context) {
        super(context);
    }

    public SuperRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuperRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private float lastX = -1, lastY = -1;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("widgetScroll", false)) {
            if (ev.getAction() == MotionEvent.ACTION_MOVE || ev.getAction() == MotionEvent.ACTION_SCROLL) {
                if(Math.abs(ev.getX()-lastX)<Math.abs(ev.getY()-lastY)) {
                    for (int i = 0; i < getChildCount(); i++) {
                        View v = getChildAt(i);
                        if (v.getTag() instanceof Card.Apps) {
                            Card.Apps c = (Card.Apps) v.getTag();
                            float x = v.getX() + c.getCardContainer().getX();
                            float y = v.getY() + c.getCardContainer().getY();
                            float w = c.getCardContainer().getWidth();
                            float h = c.getCardContainer().getHeight();
                            if (ev.getX() >= x && ev.getX() < x + w && ev.getY() >= y && ev.getY() < y + h) {
                                return true;
                            } else break;
                        }
                    }
                }
            }
        }
        lastX = ev.getX();
        lastY = ev.getY();
        return super.onInterceptTouchEvent(ev);
    }
}
