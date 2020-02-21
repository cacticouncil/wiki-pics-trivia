package com.hokuten.wikipicstrivia;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PauseHandler extends Handler
{
    public enum Message_Types
    {
        ROUND_OVER,
        DB_INITIALIZED
    }
    private final List<Message> mQueueBuffer = Collections.synchronizedList(new ArrayList<Message>());
    private Activity            mActivity;


    public final synchronized void resume(Activity activity)
    {
        this.mActivity = activity;

        while (mQueueBuffer.size() > 0)
        {
            final Message msg = mQueueBuffer.get(0);
            mQueueBuffer.remove(0);
            sendMessage(msg);
        }
    }

    public final synchronized void pause()
    {
        mActivity = null;
    }

    @Override
    public final synchronized void handleMessage(Message msg)
    {
        if (mActivity == null)
        {
            final Message msgCopy = new Message();
            msgCopy.copyFrom(msg);
            mQueueBuffer.add(msgCopy);
        }
        else
        {
            processMessage(msg);
        }
    }


    protected void processMessage(Message message)
    {
        Message_Types type = Message_Types.values()[message.what];
        switch(type)
        {
            case ROUND_OVER:
            {
                Application2.instance().getMainActivity().show(R.layout.fragment_roundover);
                break;
            }
            case DB_INITIALIZED:
            {
                Application2.instance().getMainActivity().back();
                // show help
                //ModelSettings settings = ManagerDB.instance().getSettings();
                //if (settings.isFirstTimeHelp(ModelHelp.Help.DISCLAIMER))
                //    Application2.instance().getMainActivity().popupDisclaimer();
                break;
            }
            default: break;
        }
    }

}
