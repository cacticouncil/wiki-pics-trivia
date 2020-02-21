package com.hokuten.wikipicstrivia;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ViewIntent extends LinearLayout
{
    private ImageView           mIntentImage;
    private TextView            mIntentLabel;

    public ViewIntent(Context context)
    {
        super(context);
        initialize(context);
    }

    public ViewIntent(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public ViewIntent(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public void initialize(Context context)
    {
        // ensure hardware acceleration for this view on older (< 14) devices
        if (!isInEditMode()) setLayerType(View.LAYER_TYPE_HARDWARE, null);

        inflate(context, R.layout.view_intent, this);

        mIntentImage = (ImageView)findViewById(R.id.ivIntentImage);
        mIntentLabel = (TextView)findViewById(R.id.tvIntentLabel);
    }

    public void setData(String label, Drawable image)
    {
        // Set the label
        mIntentLabel.setText(label);

        // Set the image
        mIntentImage.setImageDrawable(image);
    }

}
