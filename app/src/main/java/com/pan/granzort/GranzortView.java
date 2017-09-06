package com.pan.granzort;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * TODO: document your custom view class.
 */
public class GranzortView extends View {
    private String mExampleString;
    private int mExampleColor = Color.RED;
    private float mExampleDimension = 0;
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Paint paint;

    private Path innerCircle;  //内圆 path
    private Path outerCircle; //外圆 path
    private Path triangle1; //第1个三角形 path
    private Path triangle2; //第2个三角形 path
    private Path drawPath;  // 用于截取路径的 path

    private PathMeasure pathMeasure;

    private float mViewWidth;
    private float mViewheight;

    private long duration = 3000;
    private ValueAnimator valueAnimator;

    private Handler handler;

    private float distance; //当前动画执行的百分比取值为0-1
    private ValueAnimator.AnimatorUpdateListener animatorUpdateListener;
    private ValueAnimator.AnimatorListener animatorListener;

    private State mCurrentState = State.CIRCLE_STATE;

    //三个阶段的枚举
    private enum State {
        CIRCLE_STATE,
        TRANGLE_STATE,
        FINISH_STATE
    }

    public GranzortView(Context context) {
        super(context);
        init(null, 0);
    }

    public GranzortView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public GranzortView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GranzortView, defStyle, 0);

        mExampleString = a.getString(
                R.styleable.GranzortView_exampleString);
        mExampleColor = a.getColor(
                R.styleable.GranzortView_exampleColor,
                mExampleColor);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mExampleDimension = a.getDimension(
                R.styleable.GranzortView_exampleDimension,
                mExampleDimension);

        if (a.hasValue(R.styleable.GranzortView_exampleDrawable)) {
            mExampleDrawable = a.getDrawable(
                    R.styleable.GranzortView_exampleDrawable);
            mExampleDrawable.setCallback(this);
        }

        a.recycle();

        initPaint();

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

        initPath();
        initHandler();
        initAnimatorListener();
        initAnimator();

        mCurrentState = State.CIRCLE_STATE;
        valueAnimator.start();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    private void initPaint() {
        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.BEVEL);
        paint.setShadowLayer(15, 0, 0, Color.WHITE);
    }

    private void initPath() {
        innerCircle = new Path();
        outerCircle = new Path();
        triangle1 = new Path();
        triangle2 = new Path();
        drawPath = new Path();

        pathMeasure = new PathMeasure();

        RectF innerRect = new RectF(-220, -220, 220, 220);
        RectF outerRect = new RectF(-280, -280, 280, 280);
        innerCircle.addArc(innerRect, 150, -359.9F);     // 不能取360f，否则可能造成测量到的值不准确
        outerCircle.addArc(outerRect, 60, -359.9F);

        pathMeasure.setPath(innerCircle, false);

        float[] pos = new float[2];
        pathMeasure.getPosTan(0, pos, null);
        triangle1.moveTo(pos[0], pos[1]);
        pathMeasure.getPosTan((1f / 3f) * pathMeasure.getLength(), pos, null);
        System.out.println("pos : " + pos[0] + "  " + pos[1]);

        triangle1.lineTo(pos[0], pos[1]);
        pathMeasure.getPosTan((2f / 3f) * pathMeasure.getLength(), pos, null);
        triangle1.lineTo(pos[0], pos[1]);
        triangle1.close();

        pathMeasure.getPosTan((2f / 3f) * pathMeasure.getLength(), pos, null);
        Matrix matrix = new Matrix();
        matrix.postRotate(-180);
        triangle1.transform(matrix, triangle2);
    }

    private void initAnimatorListener() {
        animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                distance = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        };

        animatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                Log.e("start: ", mCurrentState + "_");
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Log.e("end: ", mCurrentState + "_");
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        };
    }

    private void initAnimator() {
        valueAnimator = new ValueAnimator().ofFloat(0, 1).setDuration(duration);
        valueAnimator.addUpdateListener(animatorUpdateListener);
        valueAnimator.addListener(animatorListener);
    }

    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (mCurrentState) {
                    case CIRCLE_STATE:
                        mCurrentState = State.TRANGLE_STATE;
                        valueAnimator.start();
                        break;
                    case TRANGLE_STATE:
                        mCurrentState = State.FINISH_STATE;
                        valueAnimator.start();
                        break;
                }
            }
        };
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewheight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(getResources().getColor(R.color.colorPrimary));
        canvas.save();
        canvas.translate(mViewWidth / 2, mViewheight / 2);

        switch (mCurrentState) {
            case CIRCLE_STATE:
                drawPath.reset();
                pathMeasure.setPath(innerCircle,false);
                pathMeasure.getSegment(0, distance * pathMeasure.getLength(), drawPath, true);
                canvas.drawPath(drawPath, paint);
                pathMeasure.setPath(outerCircle, false);
                drawPath.reset();
                pathMeasure.getSegment(0, distance * pathMeasure.getLength(), drawPath, true);
                canvas.drawPath(drawPath, paint);
                break;
            case TRANGLE_STATE:
                canvas.drawPath(innerCircle, paint);
                canvas.drawPath(outerCircle, paint);
                drawPath.reset();
                pathMeasure.setPath(triangle1, false);
                float stopD = distance * pathMeasure.getLength();
                float startD = stopD - (0.5f - Math.abs(0.5f - distance)) * 200;
                pathMeasure.getSegment(startD, stopD, drawPath, true);
                canvas.drawPath(drawPath, paint);
                drawPath.reset();
                pathMeasure.setPath(triangle2, false);
//                float stopD2 = distance * pathMeasure.getLength();
//                float startD2 = stopD2 - (0.5f - Math.abs(0.5f - distance)) * 200;
                pathMeasure.getSegment(startD, stopD, drawPath, true);
                canvas.drawPath(drawPath, paint);
                break;
            case FINISH_STATE:
                canvas.drawPath(innerCircle, paint);
                canvas.drawPath(outerCircle, paint);
                drawPath.reset();
                pathMeasure.setPath(triangle1, false);
                pathMeasure.getSegment(0, distance * pathMeasure.getLength(), drawPath, true);
                canvas.drawPath(drawPath, paint);
                drawPath.reset();
                pathMeasure.setPath(triangle2, false);
                pathMeasure.getSegment(0, distance * pathMeasure.getLength(), drawPath, true);
                canvas.drawPath(drawPath, paint);
                break;
        }
        canvas.restore();

//        // TODO: consider storing these as member variables to reduce
//        // allocations per draw cycle.
//        int paddingLeft = getPaddingLeft();
//        int paddingTop = getPaddingTop();
//        int paddingRight = getPaddingRight();
//        int paddingBottom = getPaddingBottom();
//
//        int contentWidth = getWidth() - paddingLeft - paddingRight;
//        int contentHeight = getHeight() - paddingTop - paddingBottom;
//
//        // Draw the text.
//        canvas.drawText(mExampleString,
//                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (contentHeight + mTextHeight) / 2,
//                mTextPaint);
//
//        // Draw the example drawable on top of the text.
//        if (mExampleDrawable != null) {
//            mExampleDrawable.setBounds(paddingLeft, paddingTop,
//                    paddingLeft + contentWidth, paddingTop + contentHeight);
//            mExampleDrawable.draw(canvas);
//        }
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}
