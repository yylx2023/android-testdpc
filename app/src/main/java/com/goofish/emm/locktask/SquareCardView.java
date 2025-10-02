package com.goofish.emm.locktask;

import android.content.Context;
import android.util.AttributeSet;
import androidx.cardview.widget.CardView;

/**
 * 正方形 CardView - 高度自动等于宽度
 */
public class SquareCardView extends CardView {

    public SquareCardView(Context context) {
        super(context);
    }

    public SquareCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 让高度等于宽度，形成正方形
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}

