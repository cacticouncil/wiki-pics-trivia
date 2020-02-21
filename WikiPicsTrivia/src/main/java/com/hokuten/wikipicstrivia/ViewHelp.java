package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;


public class ViewHelp extends View
{
    public enum GradientStyle
    {
        NONE,
        LINEAR,
        RADIAL
    }

    public enum GradientDirection
    {
        TOP_BOTTOM,
        LEFT_RIGHT,
        TOPLEFT_BOTTOMRIGHT,
        BOTTOMLEFT_TOPRIGHT,
        CENTER
    }

    private ModelHelp         mHelp;
    private int               mColor;
    private int               mColorGradient;
    private int               mColorHighlight;
    private int               mColorStroke;
    private int               mColorText;
    private int               mColorTextShadow;
    private float             mWidthStroke;
    private float             mWidthHighlight;
    private Shader            mGradient;
    private GradientStyle     mGradientStyle;
    private GradientDirection mGradientDirection;
    private boolean           mInverseGradient;
    private Rect              mDimensions;
    private float             mTextSize;
    private float             mTextPadding;
    private Point             mTextPosition;
    private StaticLayout      mTextLayout;
    private Paint             mPaintBackground;
    private Paint             mPaintHighlight;
    private Paint             mPaintStroke;
    private Paint             mPaintClear;
    private TextPaint         mPaintText;

    public ViewHelp(Context context)
    {
        super(context);
        initialize(context, null, 0);
    }

    public ViewHelp(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public ViewHelp(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle)
    {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // defaults
        mColor = 0xffffffff;
        mColorGradient = mColor;
        mColorHighlight = mColor;
        mColorStroke = mColor;
        mColorText = 0xff000000;
        mColorTextShadow = 0xffffffff;
        mWidthStroke = 6;
        mWidthHighlight = 40;
        mGradientStyle = GradientStyle.NONE;
        mGradientDirection = GradientDirection.TOPLEFT_BOTTOMRIGHT;
        mTextSize          = 14;
        mTextPadding       = 8;
        mDimensions        = new Rect();
        mTextPosition    = new Point();

        mPaintClear = new Paint();
        mPaintClear.setStyle(Paint.Style.FILL);
        mPaintClear.setARGB(255, 255, 255, 255);
        mPaintClear.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // retrieve attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ViewHelp, defStyle, 0);
        if (a != null)
        {
            if (a.hasValue(R.styleable.ViewHelp_color))
            {
                mColor          = a.getColor(R.styleable.ViewHelp_color, mColor);
                mColorGradient  = mColor;
                mColorHighlight = mColor;
                mColorStroke    = mColor;
            }

            mColorStroke = a.getColor(R.styleable.ViewHelp_stroke_color, mColorText);
            mWidthStroke = a.getDimension(R.styleable.ViewHelp_stroke_width, mWidthStroke);

            mColorHighlight = a.getColor(R.styleable.ViewHelp_highlight_color, mColorHighlight);
            mWidthHighlight = a.getDimension(R.styleable.ViewHelp_highlight_width, mWidthHighlight);

            mColorText       = a.getColor(R.styleable.ViewHelp_text_color, mColorText);
            mColorTextShadow = a.getColor(R.styleable.ViewHelp_text_color_shadow, mColorTextShadow);
            mTextSize        = a.getDimension(R.styleable.ViewHelp_text_size, mTextSize);
            mTextPadding     = a.getDimension(R.styleable.ViewHelp_text_padding, mTextPadding);

            mColorGradient     = a.getColor(R.styleable.ViewHelp_gradient_color, mColorGradient);
            mInverseGradient   = a.getBoolean(R.styleable.ViewHelp_gradient_inverse, mInverseGradient);
            mGradientStyle     = GradientStyle.values()[a.getInt(R.styleable.ViewHelp_gradient_style, mGradientStyle.ordinal())];
            mGradientDirection = GradientDirection.values()[a.getInt(R.styleable.ViewHelp_gradient_direction, mGradientDirection.ordinal())];
        }

        mPaintBackground = new Paint();
        mPaintBackground.setStyle(Paint.Style.FILL);
        mPaintBackground.setARGB(Color.alpha(mColor), Color.red(mColor), Color.green(mColor), Color.blue(mColor));

        mPaintHighlight = new Paint();
        mPaintHighlight.setStyle(Paint.Style.FILL);
        mPaintHighlight.setARGB(Color.alpha(mColorHighlight), Color.red(mColorHighlight), Color.green(mColorHighlight), Color.blue(mColorHighlight));
        mPaintHighlight.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        mPaintStroke = new Paint();
        mPaintStroke.setStyle(Paint.Style.FILL);
        //mPaintStroke.setStrokeWidth();
        mPaintStroke.setARGB(Color.alpha(mColorStroke), Color.red(mColorStroke), Color.green(mColorStroke), Color.blue(mColorStroke));

        mPaintText = new TextPaint();
        mPaintText.setStyle(Paint.Style.FILL);
        mPaintText.setARGB(Color.alpha(mColorText), Color.red(mColorText), Color.green(mColorText), Color.blue(mColorText));
        mPaintText.setTextSize(mTextSize);
        //mPaintText.setTextAlign(Paint.Align.LEFT);
        mPaintText.setFakeBoldText(true);
        mPaintText.setShadowLayer(5f, 2f, 2f, mColorTextShadow);

        a.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        mDimensions.set(left, top, right, bottom);

        if (mGradientStyle != GradientStyle.NONE)
        {
            int x1,y1,x2,y2;
            x1 = y1 = x2 = y2 = 0;

            switch (mGradientDirection)
            {
                case TOP_BOTTOM:
                    y2 = mDimensions.bottom;
                    break;
                case LEFT_RIGHT:
                    x2 = mDimensions.right;
                    break;
                case TOPLEFT_BOTTOMRIGHT:
                    x2 = mDimensions.right;
                    y2 = mDimensions.bottom;
                    break;
                case BOTTOMLEFT_TOPRIGHT:
                    y1 = mDimensions.bottom;
                    x2 = mDimensions.right;
                    break;
                case CENTER:
                    x1 = mDimensions.right>>1;
                    y1 = mDimensions.bottom>>1;
                    break;
                default: break;
            }

            if (mGradientStyle == GradientStyle.LINEAR)
            {
                mGradient = new LinearGradient(x1, y1, x2, y2,
                    Color.argb(Color.alpha(mColor), Color.red(mColor), Color.green(mColor), Color.blue(mColor)),
                    Color.argb(Color.alpha(mColorGradient), Color.red(mColorGradient), Color.green(mColorGradient), Color.blue(mColorGradient)),
                    Shader.TileMode.CLAMP);
            }
            else if (mGradientStyle == GradientStyle.RADIAL)
            {
                mGradient = new RadialGradient(x1,y1, x1-y1,
                    Color.argb(Color.alpha(mColor), Color.red(mColor), Color.green(mColor), Color.blue(mColor)),
                    Color.argb(Color.alpha(mColorGradient), Color.red(mColorGradient), Color.green(mColorGradient), Color.blue(mColorGradient)),
                    Shader.TileMode.CLAMP);
            }

            mPaintBackground.setShader(mGradient);
        }

        Layout.Alignment textAlign = Layout.Alignment.ALIGN_NORMAL;

        // right align text if highlight point is on the left
        if (mHelp.highlightPoint.x < mDimensions.right>>1)
            textAlign = Layout.Alignment.ALIGN_OPPOSITE;

        // text horizontal placement
        mTextPosition.x = (int)mTextPadding;

        // text vertical placement
        if (mHelp.highlightPoint.y < mDimensions.bottom>>1)
            mTextPosition.y = mDimensions.bottom - ((mDimensions.bottom - mHelp.highlightPoint.y)>>2);
        else
            mTextPosition.y = (mHelp.highlightPoint.y>>2);

        mTextLayout = new StaticLayout(
                mHelp.message,
                mPaintText,
                mDimensions.right-mTextPosition.x-(int)mTextPadding,
                textAlign,
                1f,
                0,
                true);
    }

    @Override
    protected void onDraw(Canvas c)
    {
        // exit if not initialized
        if (mHelp == null) return;

        // draw to separate layer as to not overwrite underlying view
        c.saveLayer(new RectF(0f,0f,(float)mDimensions.right,(float)mDimensions.bottom), null, Canvas.ALL_SAVE_FLAG);

        // draw background gradient
        c.drawRect(mDimensions, mPaintBackground);

        // draw highlight circle
        c.drawCircle(
                mHelp.highlightPoint.x,
                mHelp.highlightPoint.y,
                mHelp.highlightRadius + (int)mWidthHighlight,
                mPaintHighlight);

        // draw stroke
        c.drawCircle(
                mHelp.highlightPoint.x,
                mHelp.highlightPoint.y,
                mHelp.highlightRadius + (int)mWidthStroke,
                mPaintStroke);

        // draw clear alpha circle
        c.drawCircle(
                mHelp.highlightPoint.x,
                mHelp.highlightPoint.y,
                mHelp.highlightRadius,
                mPaintClear);

        // draw text
        //c.drawText(mHelp.message, mDimensions.right>>1, mDimensions.bottom>>2, mPaintText);
        c.translate(mTextPadding, mTextPosition.y);
        mTextLayout.draw(c);
        c.restore();

        // apply layer
        c.restore();
    }

    public void set(ModelHelp help)
    {
        mHelp = help;
    }
}
