package com.hokuten.wikipicstrivia;


public class StopWatch
{
    private boolean mIsRunning;
    private long    mTimeStarted;
    private long    mTimeElapsed;

    public StopWatch()
    {
        mTimeStarted = 0;
        mTimeElapsed = 0;
        mIsRunning = false;
    }

    public void start()
    {
        if (!mIsRunning)
        {
            mTimeStarted = System.nanoTime();
            mIsRunning = true;
        }
    }

    public void stop()
    {
        if (mIsRunning)
        {
            mTimeElapsed += System.nanoTime() - mTimeStarted;
            mIsRunning = false;
        }
    }

    public void reset()
    {
        mTimeElapsed = 0;
        if (mIsRunning) mTimeStarted = System.nanoTime();
    }

    public boolean isRunning()
    {
        return mIsRunning;
    }

    public long getElapsedTimeNanos()
    {
        if (mIsRunning) return (System.nanoTime() - mTimeStarted);
        return mTimeElapsed;
    }

    public long getElapsedTimeMillis()
    {
        return getElapsedTimeNanos() / 1000000L;
    }

    public long getElapsedTimeSeconds()
    {
        return getElapsedTimeMillis() / 1000L;
    }

    public long getElapsedTimeMinutes()
    {
        return getElapsedTimeSeconds() / 60L;
    }

    public String getRemainingTime(long timeRangeMins)
    {
        long timeRange = (((timeRangeMins * 60L) * 1000L) * 1000000L);  // to nanos
        long remaining = (timeRange - getElapsedTimeNanos());           // compute remaining
        long seconds   = (remaining / 1000000L) / 1000L;                // in seconds
        long minutes   = seconds / 60L;                                 // in minutes
        seconds        = seconds % 60L;                                 // with seconds precision
        return String.format("%2d:%02d", minutes, seconds);
    }
}