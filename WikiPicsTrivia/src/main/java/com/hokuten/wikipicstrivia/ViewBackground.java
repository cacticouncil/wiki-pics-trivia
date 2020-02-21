package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ViewBackground extends SurfaceView implements SurfaceHolder.Callback
{
    private final ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor();

    private Bitmap    mBackground;
    private int       mBackgroundID;
    private Rect      mBackgroundDim;
    private Rect      mDimensions;
    private boolean   mScrolling;
    private float     mOffsetX;
    private float     mOffsetY;
    private float     mVelocityX;
    private float     mVelocityY;
    private Future<?> mThread;
    private boolean   mOKtoDraw;
    private boolean   mRunning;
    private Rect      mRenderDim;
    private Paint     mPaintClear;

    public ViewBackground(Context context)
    {
        super(context);
        initialize(context, null, 0);
    }

    public ViewBackground(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public ViewBackground(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context, attrs, defStyle);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle)
    {
        getHolder().addCallback(this);
        setWillNotDraw(false);

        mDimensions = new Rect();
        mBackgroundDim = new Rect();
        mRenderDim = new Rect();
        mScrolling = false;
        mRunning = false;

        // retrieve custom attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ViewBackground, defStyle, 0);
        if (a != null)
        {
            mBackgroundID = a.getResourceId(R.styleable.ViewBackground_src, 0);
            mScrolling    = a.getBoolean(R.styleable.ViewBackground_scrolling, false);
            mVelocityX    = a.getFloat(R.styleable.ViewBackground_scroll_x, 0.1f);
            mVelocityY    = a.getFloat(R.styleable.ViewBackground_scroll_y, 0.1f);
            a.recycle();
        }

        mPaintClear = new Paint();
        mPaintClear.setStyle(Paint.Style.FILL);
        mPaintClear.setARGB(255, 255, 255, 255);
        //mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        holder.setFormat(PixelFormat.TRANSPARENT);

        // start rendering thread
        final SurfaceView view = this;
        mThread = mScheduler.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                mBackground = BitmapFactory.decodeResource(getResources(), mBackgroundID);
                // if fail to load specified background, create an empty one
                if (mBackground == null)
                    mBackground = Bitmap.createBitmap(1,1, Bitmap.Config.ARGB_8888);

                // cache dimensions of background
                mBackgroundDim.set(0,0,mBackground.getWidth(),mBackground.getHeight());

                // get dimensions of this view
                Canvas c = holder.lockCanvas();
                if (c!=null) mDimensions.set(0,0,c.getWidth(),c.getHeight());
                holder.unlockCanvasAndPost(c);

                // scrolling offsets start 1 tile width/height behind origin,
                // so that scrolling in any direction still keeps offset off screen.
                // this is necessary for allowing scrolling velocities in any direction.
                mOffsetX = -mBackgroundDim.width();
                mOffsetY = -mBackgroundDim.height();

                // clear screen paint draws a gradient that matches Holo theme
                // TODO: change this to read the current theme's background gradient
                mPaintClear.setShader(new LinearGradient(
                        mDimensions.right << 1, 0,
                        mDimensions.right << 1, mDimensions.bottom,
                        0xff000000, 0xff272d33,
                        Shader.TileMode.CLAMP
                ));

                float  maxfps   = 1 / 30.0f;
                double currtime = ((double)System.nanoTime()) / 1e9;
                mRunning = true;

                while (mRunning)
                {
                    c = null;

                    try
                    {
                        c = holder.lockCanvas(null);
                        synchronized (holder)
                        {
                            double newtime   = ((double)System.nanoTime()) / 1e9;
                            double frametime = newtime - currtime;
                            currtime = newtime;

                            while (frametime > 0)
                            {
                                float dt = Math.min((float)frametime, maxfps);

                                // update
                                mOffsetX += mVelocityX;
                                mOffsetY += mVelocityY;
                                if (mOffsetX > 0 || mOffsetX < (-(mBackgroundDim.width()*2f)))
                                    mOffsetX = -mBackgroundDim.width();
                                if (mOffsetY > 0 || mOffsetY < (-(mBackgroundDim.height()*2f)))
                                    mOffsetY = -mBackgroundDim.height();

                                frametime -= dt;
                            }

                            // draw
                            render(c);
                        }
                    }
                    finally
                    {
                        if (c != null)
                            holder.unlockCanvasAndPost(c);
                    }
                }
            }
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3)
    {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {

    }

    public void setScrolling(boolean b)
    {
        mScrolling = b;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        // wait until views have been drawn by the UI thread before doing our own drawing
        mOKtoDraw = true;
    }

    private void render(Canvas c)
    {
        if (mRunning && mOKtoDraw)
        {
            // scrolling
            if (mScrolling)
            {
                c.drawRect(mDimensions, mPaintClear);

                mRenderDim.set(
                        (int)mOffsetX,
                        (int)mOffsetY,
                        (int)(mOffsetX + (float)mBackgroundDim.right),
                        (int)(mOffsetY + (float)mBackgroundDim.bottom));

                while (mRenderDim.top < mDimensions.height())
                {
                    // draw horizontally
                    while (mRenderDim.left < mDimensions.width())
                    {
                        mPaintClear.setXfermode(null);
                        c.drawBitmap(mBackground, null, mRenderDim, null);
                        mRenderDim.left  += mBackgroundDim.right;
                        mRenderDim.right += mBackgroundDim.right;
                    }

                    // next line
                    mRenderDim.left    = (int)mOffsetX;
                    mRenderDim.right   = (int)(mOffsetX + (float)mBackgroundDim.right);
                    mRenderDim.top    += mBackgroundDim.bottom;
                    mRenderDim.bottom += mBackgroundDim.bottom;
                }
            }
            // stretched
            else
            {
                c.drawBitmap(mBackground, null, mDimensions, null);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        mRunning = false;

        if (mThread != null)
            mThread.cancel(false);
    }
}
