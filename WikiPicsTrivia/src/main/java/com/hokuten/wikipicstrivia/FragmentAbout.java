package com.hokuten.wikipicstrivia;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class FragmentAbout extends Fragment implements View.OnClickListener, ManagerFreebase.Listener
{
    private Button      mBtnFreebase;
    private TextView    mTxtVersion;
    //private ProgressBar mPrgFakeAnswers;
    private ImageView   mImgLogo;

    public FragmentAbout()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = getView();

        if (savedInstanceState == null)
        {
            view = inflater.inflate(R.layout.fragment_about, container, false);

            mBtnFreebase = (Button) view.findViewById(R.id.btnFreebase);
            mBtnFreebase.setOnClickListener(this);

            mImgLogo = (ImageView) view.findViewById(R.id.ivLogo);
            mImgLogo.setOnClickListener(this);

            //mPrgFakeAnswers = (ProgressBar) view.findViewById(R.id.prgFakeAnswers);

            ManagerFreebase.instance().addListener(this);

            String version = "0.0";
            try
            {
                PackageManager pm = Application2.instance().getPackageManager();
                String packageName = Application2.instance().getPackageName();
                PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);

                version = packageInfo.versionName;
            }
            catch(PackageManager.NameNotFoundException e)
            {
                if (AppConfig.DEBUG) Log.e("FragmentAbout-onCreateView", "Failed to get Package Name");
            }

            mTxtVersion = (TextView) view.findViewById(R.id.tvVersion);
            mTxtVersion.setText(String.format(Application2.instance().getResString(R.string.about_version), version));
        }

        ((ActivityMain)getActivity()).enableMenuItem(R.id.menu_about, false);
        if (AppConfig.DEBUG)
        {
            ((ActivityMain)getActivity()).showMenuItem(R.id.menu_package_images, true);
            ((ActivityMain)getActivity()).showMenuItem(R.id.menu_create_fake_answers, true);
        }

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        ((ActivityMain)getActivity()).enableMenuItem(R.id.menu_about, true);
        if (AppConfig.DEBUG)
        {
            ((ActivityMain)getActivity()).showMenuItem(R.id.menu_package_images, false);
            ((ActivityMain)getActivity()).showMenuItem(R.id.menu_create_fake_answers, false);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onClick(View view)
    {
        if(view == mBtnFreebase)
        {
            Intent browseIntent = new Intent(Intent.ACTION_VIEW);
            browseIntent.setData(Uri.parse(Application2.instance().getResString(R.string.url_freebase)));
            browseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            getActivity().startActivity(browseIntent);
        }
        else if(view == mImgLogo)
        {
            Intent browseIntent = new Intent(Intent.ACTION_VIEW);
            browseIntent.setData(Uri.parse(Application2.instance().getResString(R.string.url_hokuten)));
            browseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            getActivity().startActivity(browseIntent);
        }
    }

    @Override
    public void onUpdateQuestionsInitiated()
    {
        //mPrgFakeAnswers.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUpdateQuestionsInProgress(int progress)
    {

    }

    @Override
    public void onUpdateQuestionsCancelled()
    {

    }

    @Override
    public void onUpdateQuestionsComplete()
    {
        //mPrgFakeAnswers.setVisibility(View.GONE);
    }

    @Override
    public void onUpdateTestConnectionComplete(Boolean aBoolean)
    {

    }
}
