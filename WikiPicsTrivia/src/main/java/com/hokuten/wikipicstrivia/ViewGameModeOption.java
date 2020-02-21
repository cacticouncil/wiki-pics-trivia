package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;


public class ViewGameModeOption extends View
{
    private Rect    mDimensions;
    private int     mIconID;
    private Bitmap  mIcon;
    private String  mText;
    private float   mTextSize;
    private Paint   mTextPaint;
    private Paint   mTextPaintShadow;
    private Paint   mIconPaint;

    public ViewGameModeOption(Context context)
    {
        super(context);
        initialize(context, null, 0);
    }

    public ViewGameModeOption(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public ViewGameModeOption(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }

    public void initialize(Context context, AttributeSet attrs, int defStyle)
    {
        // ensure hardware acceleration for this view
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mDimensions = new Rect();
        mText = "";
        mTextSize = 14;
        mIcon = null;

        // retrieve custom attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ViewGameModeOption, defStyle, 0);
        if (a != null)
        {
            mIconID   = a.getResourceId(R.styleable.ViewGameModeOption_icon, 0);
            mTextSize = a.getDimension(R.styleable.ViewGameModeOption_text_size, mTextSize);
            a.recycle();
        }

        mTextPaint = new Paint();
        mTextPaint.setARGB(255,200,200,200);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setTextSize(mTextSize);
        mTextPaintShadow = new Paint();
        mTextPaintShadow.setARGB(255,0,0,0);
        mTextPaintShadow.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaintShadow.setTextAlign(Paint.Align.RIGHT);
        mTextPaintShadow.setTextSize(mTextSize);

        mIconPaint = new Paint();
        mIconPaint.setARGB(255,255,255,255);
    }

    public void setText(String text)
    {
        mText = text;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        mDimensions.set(0,0,w,h);

        // load and cache option icon, scaling to fit bounds
        if (!isInEditMode())
        {
            if (mIconID > 0)
            {
                if (ManagerMedia.instance().getCachedDrawable(mIconID) == null)
                {
                    Bitmap b = BitmapFactory.decodeResource(getResources(), mIconID);
                    //b = scaleBitmap(b, ((float)Math.min(mDimensions.width(), mDimensions.height())) / (float) b.getWidth());
                    ManagerMedia.instance().setCachedDrawable(mIconID, b);
                    mIcon = b;
                }
                else mIcon = ManagerMedia.instance().getCachedDrawable(mIconID);
            }
        }
    }

    @Override
    protected void onDraw(Canvas c)
    {
        if (mIcon != null)
        {
            // disabled ones are greyed out
            if (isEnabled()) mIconPaint.setAlpha(255);
            else             mIconPaint.setAlpha(100);

            c.drawBitmap(mIcon, 0,0, mIconPaint);
        }

        if (mText != null && !mText.isEmpty())
        {
            c.drawText(mText, mDimensions.width(),   mDimensions.height(),   mTextPaintShadow);
            c.drawText(mText, mDimensions.width()-4, mDimensions.height()-4, mTextPaint);
        }
    }

    private Bitmap scaleBitmap(Bitmap bm, float scale)
    {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);
        return newbm;
    }
}
