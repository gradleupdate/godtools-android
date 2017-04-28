package org.keynote.godtools.renderer.crureader.bo.GPage.Views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import org.keynote.godtools.renderer.crureader.R;
import org.keynote.godtools.renderer.crureader.bo.GPage.RenderHelpers.RenderSingleton;

public class AutoScaleTextView extends AppCompatTextView {
    private int textSizeScalar;

    public AutoScaleTextView(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public AutoScaleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public AutoScaleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoScaleTextView, defStyleAttr, R.style.AutoScaleTextView);
        textSizeScalar = a.getInteger(R.styleable.AutoScaleTextView_textSizeScalar, 17);
        a.recycle();
    }

    @Override
    public void setTextSize(int unit, float size) {
        float adjustedSize = ((size) * (float)textSizeScalar * RenderSingleton.getInstance().getScreenHeightForNonRotationDesign()) / 80.0F;
        super.setTextSize(unit, adjustedSize);
    }
}