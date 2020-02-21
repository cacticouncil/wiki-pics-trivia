package com.hokuten.wikipicstrivia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;


public class FragmentCreateCategory extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, FragmentCreateMode.Updater
{
    private Button             mBtnCreateModeDone;
    private ExpandableListView mELvCategories;
    private ModelGameMode      mGameMode;
    private RadioGroup         mSelectGroup;
    private RadioButton        mRBtnAll;
    private RadioButton        mRBtnNone;
    private RadioButton        mRBtnCustom;

    public static final FragmentCreateCategory newInstance(ModelGameMode g)
    {
        FragmentCreateCategory p = new FragmentCreateCategory();
        Bundle args = new Bundle();
        args.putParcelable("gamemode", g);
        p.setArguments(args);
        return p;
    }

    public FragmentCreateCategory()
    {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            if (getArguments().getParcelable("gamemode") != null)
                mGameMode = (ModelGameMode)getArguments().getParcelable("gamemode");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_createcategory, container, false);

        // views
        mBtnCreateModeDone = (Button)view.findViewById(R.id.btnCreateModeDone);
        mELvCategories     = (ExpandableListView)view.findViewById(R.id.elvCategories);
        mSelectGroup       = (RadioGroup)view.findViewById(R.id.rGrpSelect);
        mRBtnAll           = (RadioButton)view.findViewById(R.id.rBtnAll);
        mRBtnNone          = (RadioButton)view.findViewById(R.id.rBtnNone);
        mRBtnCustom        = (RadioButton)view.findViewById(R.id.rBtnCustom);

        // listeners
        mBtnCreateModeDone.setOnClickListener(this);
        mSelectGroup.setOnCheckedChangeListener(this);

        // edit mode or not?
        if (mGameMode.id != -1)
        {
            mELvCategories.setAdapter(new AdapterCategories(getActivity(), mGameMode.id, mSelectGroup));
            mBtnCreateModeDone.setText(Application2.instance().getResString(R.string.createmode_category_update));
        }
        else
            mELvCategories.setAdapter(new AdapterCategories(getActivity(), mSelectGroup));

        return view;
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
        if (view == mBtnCreateModeDone)
        {
            mGameMode.categories = ModelGameMode.Categories.CUSTOM;

            if (((AdapterCategories)mELvCategories.getExpandableListAdapter()).modeValid())
            {
                ManagerMedia.instance().playSound(R.raw.sfx_button);

                handleCreateMode();
            }
            else
            {
                AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
                ad.setIcon(android.R.drawable.ic_dialog_alert);
                ad.setTitle(Application2.instance().getResString(R.string.dialog_createmode_category_title));
                ad.setCancelable(false);
                ad.setMessage(Application2.instance().getResString(R.string.dialog_createmode_category_text));
                ad.setButton(
                        DialogInterface.BUTTON_POSITIVE, Application2.instance().getResString(R.string.common_ok), new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        }
                );
                ad.show();
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        if (checkedId == R.id.rBtnAll)
        {
            if(mRBtnAll.isChecked())
                ((AdapterCategories)mELvCategories.getExpandableListAdapter()).selectAll(true);
        }
        else if (checkedId == R.id.rBtnNone)
        {
            if(mRBtnNone.isChecked())
                ((AdapterCategories)mELvCategories.getExpandableListAdapter()).selectAll(false);
        }
        else if (checkedId == R.id.rBtnCustom)
        {
            // do nothing
        }
    }

    private void handleCreateMode()
    {
        long modeId;
        // Previously created mode
        if (mGameMode.id != -1)
        {
            modeId = mGameMode.id;
            ManagerDB.instance().updateGameMode(mGameMode);
            // Create all Mode to Category entries based on selected categories
            SparseArray<List<ModelCategory>> categoriesLists = ((AdapterCategories)mELvCategories.getExpandableListAdapter()).getCategories();
            for(int i = 0; i < categoriesLists.size(); i++)
            {
                List<ModelCategory> categories = categoriesLists.valueAt(i);
                for (ModelCategory category : categories)
                {
                    if (category.checked)
                    {
                        ManagerDB.instance().createModeToCategory(modeId, category.id);
                    }
                    else
                    {
                        ManagerDB.instance().removeModeToCatgory(modeId, category.id);
                    }
                }
            }

            Application2.toast(R.string.toast_success_edit_gamemode);
        }
        else
        {
            modeId = ManagerDB.instance().createGameMode(mGameMode);
            // Create all Mode to Category entries based on selected categories
            if (modeId != -1)
            {
                SparseArray<List<ModelCategory>> categoriesLists = ((AdapterCategories)mELvCategories.getExpandableListAdapter()).getCategories();
                for(int i = 0; i < categoriesLists.size(); i++)
                {
                    List<ModelCategory> categories = categoriesLists.valueAt(i);
                    for (ModelCategory category : categories)
                    {
                        if (category.checked)
                        {
                            ManagerDB.instance().createModeToCategory(modeId, category.id);
                        }
                    }
                }
            }

            Application2.toast(R.string.toast_success_add_gamemode);
        }

        // Back out to title screen
        ((ActivityMain)getActivity()).back();
        ((ActivityMain)getActivity()).back();

        // Show game modes with mygames expanded
        Bundle args = new Bundle();
        args.putBoolean("expandMyGames", true);
        ((ActivityMain)getActivity()).show(R.layout.fragment_gamemodes, args);
    }

    @Override
    public void update(ModelGameMode gameMode)
    {

    }
}
