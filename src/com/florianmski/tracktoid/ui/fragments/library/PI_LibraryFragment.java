package com.florianmski.tracktoid.ui.fragments.library;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.florianmski.tracktoid.R;
import com.florianmski.tracktoid.TraktListener;
import com.florianmski.tracktoid.Utils;
import com.florianmski.tracktoid.adapters.GridPosterAdapter;
import com.florianmski.tracktoid.image.TraktImage;
import com.florianmski.tracktoid.trakt.tasks.TraktTask;
import com.florianmski.tracktoid.ui.fragments.TraktFragment;
import com.florianmski.traktoid.TraktoidInterface;

public abstract class PI_LibraryFragment<T extends TraktoidInterface<T>> extends TraktFragment implements TraktListener<T>
{
	protected final static int NB_COLUMNS_TABLET_PORTRAIT = 5;
	protected final static int NB_COLUMNS_TABLET_LANDSCAPE = 7;
	protected final static int NB_COLUMNS_PHONE_PORTRAIT = 3;
	protected final static int NB_COLUMNS_PHONE_LANDSCAPE = 5;

	protected GridView gd;
	protected QuickAction quickAction;

	protected int posterClickedPosition = -1;

	protected GridPosterAdapter<T> adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	public abstract void checkUpdateTask();
	public abstract GridPosterAdapter<T> setupAdapter();
	public abstract void onGridItemClick(AdapterView<?> arg0, View v, int position, long arg3);
	public abstract void displayContent();
	public abstract void onRefreshQAClick(QuickAction source, int pos, int actionId);
	public abstract void onDeleteQAClick(QuickAction source, int pos, int actionId);
	public abstract void onRateQAClick(QuickAction source, int pos, int actionId);
	public abstract void onRefreshClick();

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		getStatusView().show().text("Loading ,\nPlease wait...");

		checkUpdateTask();

		refreshGridView();

		adapter = setupAdapter();
		gd.setAdapter(adapter);

		displayContent();

		gd.setOnItemClickListener(new OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
			{
				onGridItemClick(arg0, arg1, position, arg3);
			}
		});

		quickAction = new QuickAction(getActivity());

		ActionItem aiRefresh = new ActionItem();
		aiRefresh.setTitle("Refresh");
		aiRefresh.setIcon(getResources().getDrawable(R.drawable.ab_icon_refresh));

		ActionItem aiDelete = new ActionItem();
		aiDelete.setTitle("Delete");
		aiDelete.setIcon(getResources().getDrawable(R.drawable.ab_icon_delete));

		ActionItem aiRating = new ActionItem();
		aiRating.setTitle("Rate");
		aiRating.setIcon(getResources().getDrawable(R.drawable.ab_icon_rate));

		quickAction.addActionItem(aiRefresh);
		quickAction.addActionItem(aiDelete);
		//not necessary, disable it for the moment
		//		quickAction.addActionItem(aiRating);

		quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() 
		{			
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) 
			{
				switch(pos)
				{
				case 0 :
					onRefreshQAClick(source, pos, actionId);
					break;
				case 1 :
					onDeleteQAClick(source, pos, actionId);
					break;
				case 2:
					onRateQAClick(source, pos, actionId);
					break;
				}
			}
		});

		gd.setOnItemLongClickListener(new OnItemLongClickListener() 
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v, int position, long arg3) 
			{
				onShowQuickAction(v, position);
				return false;
			}

		});

		TraktTask.addObserver(this);
	}

	@Override
	public void onDestroy()
	{
		TraktTask.removeObserver(this);
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle toSave) {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View v = inflater.inflate(R.layout.pager_item_library, null);

		gd = (GridView)v.findViewById(R.id.gridViewLibrary);

		return v;
	}

	public int refreshGridView()
	{
		int nbColumns;

		if(Utils.isTabletDevice(getActivity()))
		{
			if(Utils.isLandscape(getActivity()))
				nbColumns = NB_COLUMNS_TABLET_LANDSCAPE;
			else
				nbColumns = NB_COLUMNS_TABLET_PORTRAIT;	
		}
		else
		{
			if(Utils.isLandscape(getActivity()))
				nbColumns = NB_COLUMNS_PHONE_LANDSCAPE;
			else
				nbColumns = NB_COLUMNS_PHONE_PORTRAIT;	
		}

		gd.setNumColumns(nbColumns);

		if(adapter != null)
			adapter.setHeight(calculatePosterHeight(nbColumns));

		return calculatePosterHeight(nbColumns);
	}

	public void onShowQuickAction(View v, int position) 
	{
		//maybe add a setTag() function in quickAction to avoid this
		posterClickedPosition = position;
		quickAction.show(v);
	}

	private int calculatePosterHeight(int nbColumns)
	{
		int width = (getActivity().getWindowManager().getDefaultDisplay().getWidth()/(nbColumns));
		return (int) (width*TraktImage.RATIO_POSTER);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		SubMenu rateMenu = menu.addSubMenu(0, R.id.action_bar_filter, 0, "Filter");
		rateMenu.add(0, R.id.action_bar_filter_all, 0, "All");
		rateMenu.add(0, R.id.action_bar_filter_unwatched, 0, "Unwatched");
		rateMenu.add(0, R.id.action_bar_filter_loved, 0, "Loved");

		MenuItem rateItem = rateMenu.getItem();
		rateItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		int value = getSherlockActivity().getSupportActionBar().getHeight();
		ProgressBar pbRefresh = new ProgressBar(getActivity());
		pbRefresh.setIndeterminate(true);
		RelativeLayout rl = new RelativeLayout(getActivity());
		rl.setLayoutParams(new LayoutParams(value, value));
		pbRefresh.setLayoutParams(new RelativeLayout.LayoutParams(value, value));
		rl.addView(pbRefresh);

		menu.add(0, R.id.action_bar_refresh, 0, "Refresh")
		.setActionView(rl)
		.setEnabled(false)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
		case R.id.action_bar_refresh:
			onRefreshClick();
			break;
		case R.id.action_bar_filter_all:
			adapter.setFilter(GridPosterAdapter.FILTER_ALL);
			break;
		case R.id.action_bar_filter_loved:
			adapter.setFilter(GridPosterAdapter.FILTER_LOVED);
			break;
		case R.id.action_bar_filter_unwatched:
			adapter.setFilter(GridPosterAdapter.FILTER_UNWATCHED);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	//TODO
	//	@Override
	//	public void onBeforeTraktRequest()
	//	{
	//		getSherlockActivity().invalidateOptionsMenu();
	//	}
	//
	//	@Override
	//	public void onAfterTraktRequest(boolean success) 
	//	{
	//		getSherlockActivity().invalidateOptionsMenu();
	//	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onRestoreState(Bundle savedInstanceState) {}

	@Override
	public void onSaveState(Bundle toSave) {}

	@Override
	public void onTraktItemUpdated(T traktItem) 
	{
		if(adapter != null)
			adapter.updateItem(traktItem);
	}

	@Override
	public void onTraktItemRemoved(T traktItem) 
	{
		if(adapter != null)
			adapter.remove(traktItem);
	}
}