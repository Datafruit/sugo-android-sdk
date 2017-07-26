package io.sugo.sdkdemo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Ouwenjie on 2017/7/11.
 */

public class CircleBar extends View {

    private Paint mNormalPaint;
    private Paint mProgressPaint;
    private int mWidth;
    private int mCircleCenterX;
    private int mLineWidth = 20;
    private int mOutInPadding = mLineWidth;
    private int mOutCircleRadius;
    private int mInCircleRadius;

    private int mProgress = 75;

    public CircleBar(Context context) {
        this(context, null, -1);
    }

    public CircleBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public CircleBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initView();
    }

    private void initAttrs(Context context, AttributeSet attrs) {

    }


    private void initView() {

        mNormalPaint = new Paint();
        mNormalPaint.setAntiAlias(true);
        mNormalPaint.setColor(Color.WHITE);
        mNormalPaint.setStrokeWidth(2);
        mNormalPaint.setStyle(Paint.Style.FILL);

        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setColor(Color.RED);
        mProgressPaint.setStrokeWidth(2);
        mProgressPaint.setStyle(Paint.Style.FILL);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = MeasureSpec.getSize(widthMeasureSpec);

        setMeasuredDimension(mWidth, mWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mCircleCenterX = mWidth / 2;
        mOutCircleRadius = mCircleCenterX - mLineWidth;
        mInCircleRadius = mOutCircleRadius - mOutInPadding - mLineWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < 120; i++) {
            canvas.save();
            canvas.rotate(3 * i + 90, mCircleCenterX, mCircleCenterX);
            if (i < mProgress) {
                canvas.drawLine(mCircleCenterX + mOutCircleRadius, mCircleCenterX, mWidth, mCircleCenterX, mProgressPaint);
            } else {
                canvas.drawLine(mCircleCenterX + mOutCircleRadius, mCircleCenterX, mWidth, mCircleCenterX, mNormalPaint);
            }
            canvas.restore();
        }

        for (int i = 0; i < 120; i++) {
            canvas.save();
            canvas.rotate(3 * i + 90, mCircleCenterX, mCircleCenterX);
            if (i < mProgress) {
                canvas.drawLine(mCircleCenterX + mInCircleRadius, mCircleCenterX, mCircleCenterX + mInCircleRadius + mLineWidth, mCircleCenterX, mProgressPaint);
            } else {
                canvas.drawLine(mCircleCenterX + mInCircleRadius, mCircleCenterX, mCircleCenterX + mInCircleRadius + mLineWidth, mCircleCenterX, mNormalPaint);
            }
            canvas.restore();
        }

    }


}
