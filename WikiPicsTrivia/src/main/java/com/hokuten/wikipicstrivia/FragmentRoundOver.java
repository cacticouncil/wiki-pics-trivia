package com.hokuten.wikipicstrivia;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.UiLifecycleHelper;

import java.util.List;


public class FragmentRoundOver extends Fragment implements View.OnClickListener,  AdapterView.OnItemClickListener
{
    public static int QUESTION_IMG_SIZE;

    private ViewGameMode       mVGameMode;
    private Button             mBtnReturnToTitle;
    private Button             mBtnReplay;
    private ListView           mLVQuestions;
    private View               mSelectedView;
    private TextView           mTxtStatsTotal;
    private TextView           mTxtStatsPercent;
    public  UiLifecycleHelper  mUiHelper;

    public FragmentRoundOver()
    {
        super();

        QUESTION_IMG_SIZE = (int)Application2.instance().getResDimen(R.dimen.over_question_img_size);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_roundover, container, false);

        // views
        mVGameMode = (ViewGameMode)view.findViewById(R.id.vRoundOverGameMode);
        mTxtStatsTotal = (TextView)view.findViewById(R.id.tvRoundStatsTotal);
        mTxtStatsPercent = (TextView)view.findViewById(R.id.tvRoundStatsPercent);
        mLVQuestions = (ListView)view.findViewById(R.id.lvQuestions);
        mBtnReplay = (Button)view.findViewById(R.id.btnReplay);
        mBtnReturnToTitle = (Button)view.findViewById(R.id.btnReturnToTitle);

        // listeners
        mBtnReplay.setOnClickListener(this);
        mBtnReturnToTitle.setOnClickListener(this);
        mLVQuestions.setOnItemClickListener(this);

        // game information
        ModelGameRound roundInfo = ManagerGame.instance().getGameRoundInfo();
        mVGameMode.setGameMode(roundInfo.mode);
        mLVQuestions.setAdapter(new AdapterRoundOver(getActivity(), ManagerGame.instance().getQuestions()));

        // stats
        mTxtStatsTotal.setText(Integer.toString((mLVQuestions.getAdapter()).getCount()));
        float mPercentCorrect = ((float)roundInfo.correct/(float)mLVQuestions.getAdapter().getCount());
        mTxtStatsPercent.setText((int)(mPercentCorrect * 100) + "%");
        mTxtStatsPercent.setTextColor(Application2.instance().interpolateColor(Color.RED, Color.GREEN, mPercentCorrect));

        mUiHelper = new UiLifecycleHelper(((ActivityMain)getActivity()), null);
        mUiHelper.onCreate(savedInstanceState);

        ManagerMedia.instance().stopBGM();

        return view;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mUiHelper.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mUiHelper.onResume();
        ManagerMedia.instance().stopBGM();
    }

    @Override
    public void onClick(View view)
    {
        ModelSettings settings = ManagerDB.instance().getSettings();
        boolean menu = true;

        if (view == mBtnReturnToTitle)
        {
            menu = true;
        }
        else if (view == mBtnReplay)
        {
            String message = ManagerGame.instance().canUserProceedWithGameMode(settings, ManagerGame.instance().getGameRoundInfo().mode);
            menu = (message != null);
        }

        // user will return to main menu
        if (menu)
        {
            ManagerMedia.instance().playSound(R.raw.sfx_button);
            ((ActivityMain)getActivity()).back();
        }
        // user will replay current mode
        else
        {
            ManagerMedia.instance().playSound(R.raw.sfx_correct);
            ((ActivityMain)getActivity()).back();
            ManagerGame.instance().startRound(ManagerGame.instance().getGameRoundInfo().mode);
            ((ActivityMain)getActivity()).show(R.layout.fragment_playscreen);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        int mSelectedPosition  = ((AdapterRoundOver)mLVQuestions.getAdapter()).getSelectedPosition();
        ModelQuestion mQuestion = ((AdapterRoundOver)mLVQuestions.getAdapter()).getItem(position);

        // Set new position to visible and change old view to not visibile
        if(mSelectedPosition == -1 || position == mSelectedPosition)
        {
            enableSelectedView((ViewQuestion)view, mQuestion);
        }
        else
        {
            enableSelectedView((ViewQuestion)view, mQuestion);

            // If the view hasn't been overwritten by getView in AdapterRoundOver
            // otherwise getView will ensure that the correct selection is highlighted
            if((((ViewQuestion)mSelectedView).getQuestion().index) != position)
            {
                mSelectedView.setBackgroundColor(Color.TRANSPARENT);
                ((ViewQuestion) mSelectedView).getShareButton().setVisibility(View.INVISIBLE);
                ((ViewQuestion) mSelectedView).getWikipediaButton().setVisibility(View.INVISIBLE);
            }
        }

        ((AdapterRoundOver)mLVQuestions.getAdapter()).setSelectedPosition(position);
        mSelectedView = view;
    }

    // Enables the passed in view by highlighting and enabling the share button
    private void enableSelectedView(final ViewQuestion view, final ModelQuestion question)
    {
        view.setBackgroundColor(Color.GRAY);

        // Set the wikipedia button visible and add the click listener
        view.getWikipediaButton().setVisibility(View.VISIBLE);
        view.getWikipediaButton().setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse(String.format(Application2.instance().getResString(R.string.url_wikipage),
                        question.wid)));
                browseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                ((ActivityMain) getActivity()).startActivity(Intent.createChooser(browseIntent, "Go to"));

            }
        });

        // Set the share button visible and add the click listener
        view.getShareButton().setVisibility(View.VISIBLE);
        view.getShareButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create dummy intent to populate activities
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType(Application2.instance().getResString(R.string.share_intent_type_text_image));

                List activities = Application2.instance().getPackageManager().queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);

                // Get the intent list view
                LayoutInflater inflater = Application2.instance().getMainActivity().getLayoutInflater();
                View convertView = (View) inflater.inflate(R.layout.view_intentlist, null);

                // Create the custom alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(Application2.instance().getMainActivity());
                builder.setTitle(Application2.instance().getResString(R.string.share_intent_text));
                builder.setView(convertView);

                AlertDialog dialog = builder.create();
                // Create and set the listview and adapter for the dialog to display
                ListView lv = (ListView) convertView.findViewById(R.id.lvShareIntents);
                AdapterIntents adapter = new AdapterIntents(Application2.instance().getMainActivity(),
                                                                      R.layout.view_intentlist,
                                                                      activities.toArray(),
                                                                      question,
                                                                      mUiHelper,
                                                                      dialog);
                lv.setAdapter(adapter);

                dialog.show();
            }
        });
    }
}
