package com.hokuten.wikipicstrivia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.OpenGraphAction;
import com.facebook.model.OpenGraphObject;
import com.facebook.widget.FacebookDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AdapterIntents extends ArrayAdapter
{
    Activity          mContext;
    Object[]          mItems;
    ModelQuestion     mQuestion;
    UiLifecycleHelper mUiLifeCycleHelper;
    AlertDialog       mDialog;

    public AdapterIntents(Activity context, int layoutId, Object[] items, ModelQuestion question, UiLifecycleHelper uiLifecycleHelper, AlertDialog dialog) {
        super(context, layoutId, items);

        mContext           = context;
        mItems             = items;
        mQuestion          = question;
        mUiLifeCycleHelper = uiLifecycleHelper;
        mDialog            = dialog;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null)
        {
            convertView = new ViewIntent(mContext);
        }

        ((ViewIntent) convertView).setData(((ResolveInfo)mItems[position]).activityInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString(),
                                           ((ResolveInfo)mItems[position]).activityInfo.applicationInfo.loadIcon(mContext.getPackageManager()));

        ((ViewIntent) convertView).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                ResolveInfo info = (ResolveInfo) mItems[position];
                if(info.activityInfo.packageName.equals("com.facebook.katana")) {
                    if (FacebookDialog.canPresentShareDialog(Application2.instance().getApplicationContext(), FacebookDialog.ShareDialogFeature.SHARE_DIALOG))
                    {
                        shareFbDialog(mQuestion);
                    }
                }
                else
                {
                    shareGeneric(mQuestion, info);
                }

                mDialog.dismiss();
            }
        });

        return convertView;
    }

    private void shareGeneric(ModelQuestion question, ResolveInfo info)
    {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
        sharingIntent.setType(Application2.instance().getResString(R.string.share_intent_type_text_image));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, String.format(Application2.instance().getResString(R.string.share_intent_body_question),
                                                                Application2.instance().getResString(R.string.app_name),
                                                                question.question,
                                                                Application2.instance().getResString(R.string.url_hokuten)));

        // Use fileprovider to serve the image
        File srcFile = ManagerMedia.instance().getTopicImageFile(question.mid);
        Uri contentUri = FileProvider.getUriForFile(mContext, "com.hokuten.wikipicstrivia.fileprovider", srcFile);

        if (contentUri != null)
        {
            sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            mContext.startActivity(sharingIntent);
        }
        else
        {
            Application2.toast(R.string.toast_error_share_generic);
        }
    }

    public void shareFbDialog(ModelQuestion question)
    {
        OpenGraphObject triviaQuestion = OpenGraphObject.Factory.createForPost(Application2.instance().getResString(R.string.fb_namespace));
        triviaQuestion.setProperty("title", Application2.instance().getResString(R.string.fb_object_title));
        triviaQuestion.setProperty("description", String.format(Application2.instance().getResString(R.string.fb_object_description), question.question));

        OpenGraphAction action = GraphObject.Factory.create(OpenGraphAction.class);
        action.setProperty(Application2.instance().getResString(R.string.fb_object_type), triviaQuestion);
        action.setType(Application2.instance().getResString(R.string.fb_action_type));

        Bitmap bitmap = ManagerMedia.instance().getTopicImage(question.mid);
        List<Bitmap> images = new ArrayList<Bitmap>();
        images.add(bitmap);

        FacebookDialog shareDialog = new FacebookDialog.OpenGraphActionDialogBuilder(mContext, action, Application2.instance().getResString(R.string.fb_object_type))
                .setImageAttachmentsForObject(Application2.instance().getResString(R.string.fb_object_type), images, false)
                .build();
        // TODO: show a progress dialog if fb isn't signed in?
        mUiLifeCycleHelper.trackPendingDialogCall(shareDialog.present());
    }
}
