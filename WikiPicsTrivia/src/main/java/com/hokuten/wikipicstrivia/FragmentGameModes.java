package com.hokuten.wikipicstrivia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;


public class FragmentGameModes extends Fragment implements View.OnClickListener, ExpandableListView.OnChildClickListener
{
    private ExpandableListView mELvModes;
    private Button             mBtnCreateMode;
    private boolean            mExpandMyGames;

    public FragmentGameModes()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null)
        {
            if (args.containsKey("expandMyGames"))
                mExpandMyGames = args.getBoolean("expandMyGames");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_gamemodes, container, false);

        // views
        mBtnCreateMode = (Button)view.findViewById(R.id.btnCreateMode);
        mELvModes      = (ExpandableListView)view.findViewById(R.id.eLvModes);

        // listeners
        mBtnCreateMode.setOnClickListener(this);
        mELvModes.setOnChildClickListener(this);
        mELvModes.setOnCreateContextMenuListener(this);
        mELvModes.setAdapter(new AdapterGameModes(getActivity()));

        // Expand the default modes - start collapsed?
        if (mExpandMyGames)
            mELvModes.expandGroup(2);

        // show help
        //ModelSettings settings = ManagerDB.instance().getSettings();
        //if (settings.isFirstTimeHelp(ModelHelp.Help.CUSTOM_MODES))
        //{
        //    ((ActivityMain)getActivity()).showHelp(ModelHelp.Help.CUSTOM_MODES, mBtnCreateMode);
        //
        //    // do not show this help again
        //    settings.setDoNotShowHelpAgain(ModelHelp.Help.CUSTOM_MODES);
        //    ManagerDB.instance().updateSettings(settings);
        //}

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
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
        if (view == mBtnCreateMode)
        {
            ManagerMedia.instance().playSound(R.raw.sfx_button);
            ((ActivityMain)getActivity()).show(R.layout.fragment_createmode);
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
    {
        if (v instanceof ViewGameMode)
        {
            ModelGameMode mode = ((ViewGameMode)v).getGameMode();
            ModelSettings settings = ManagerDB.instance().getSettings();
            String message = ManagerGame.instance().canUserProceedWithGameMode(settings, mode);

            // user will proceed with game mode
            if (message == null)
            {
                ManagerMedia.instance().playSound(R.raw.sfx_correct);

                ManagerGame.instance().startRound(mode);
                ((ActivityMain)getActivity()).show(R.layout.fragment_playscreen);
            }
            // user needs to download more questions
            else
            {
                AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
                ad.setCancelable(false);
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(Application2.instance().getResString(R.string.dialog_need_questions_title));
                ad.setMessage(message);
                ad.setButton(
                        DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_ok),
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                                ((ActivityMain)getActivity()).show(R.layout.fragment_questions);
                            }
                        }
                );
                ad.show();
            }
        }

        return true;
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)menuInfo;

        if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
        {
            ModelGameMode mode = ((ViewGameMode)info.targetView).getGameMode();

            MenuInflater inflater = ((ActivityMain)getActivity()).getMenuInflater();
            inflater.inflate(R.menu.gamemode, menu);

            // Only remove custom game modes
            if (mode.categories == ModelGameMode.Categories.CUSTOM)
            {
                menu.findItem(R.id.menu_edit).setVisible(true);
                menu.findItem(R.id.menu_delete).setVisible(true);
                menu.findItem(R.id.menu_untouchable).setVisible(false);
            }
            else
            {
                menu.findItem(R.id.menu_edit).setVisible(false);
                menu.findItem(R.id.menu_delete).setVisible(false);
                menu.findItem(R.id.menu_untouchable).setVisible(true);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
        if (ExpandableListView.getPackedPositionType(info.packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
        {
            switch (item.getItemId())
            {
                case R.id.menu_delete:
                {
                    ModelGameMode mode = ((ViewGameMode)info.targetView).getGameMode();
                    ManagerDB.instance().removeGameMode(mode.id);

                    ((AdapterGameModes)mELvModes.getExpandableListAdapter()).removeItem(ExpandableListView.getPackedPositionChild(info.packedPosition));
                    ((AdapterGameModes)mELvModes.getExpandableListAdapter()).notifyDataSetChanged();

                    Application2.toast(R.string.toast_success_delete_gamemode);
                    return true;
                }
                case R.id.menu_edit:
                {
                    ModelGameMode mode = ((ViewGameMode)info.targetView).getGameMode();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("gamemode", mode);
                    ((ActivityMain)getActivity()).show(R.layout.fragment_createmode, bundle);
                    return true;
                }
                default:
                    return super.onContextItemSelected(item);
            }
        }

        return super.onContextItemSelected(item);
    }
}
