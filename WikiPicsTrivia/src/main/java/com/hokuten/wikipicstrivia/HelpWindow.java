package com.hokuten.wikipicstrivia;

import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;


public class HelpWindow extends PopupWindow implements View.OnClickListener
{
    private ViewHelp mHelp;

    public HelpWindow(View contentView, int width, int height, boolean focusable)
    {
        super(contentView, width, height, focusable);

        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        mHelp = (ViewHelp)contentView.findViewById(R.id.vHelp);
        mHelp.setOnClickListener(this);
    }

    public void show(ModelHelp help, View parent)
    {
        if (help == null) return;
        mHelp.set(help);
        showAtLocation(parent, Gravity.NO_GRAVITY, 0,0);
    }

    @Override
    public void onClick(View view)
    {
        dismiss();
    }
}
