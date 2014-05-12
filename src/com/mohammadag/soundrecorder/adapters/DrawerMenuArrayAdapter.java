package com.mohammadag.soundrecorder.adapters;

import java.util.Locale;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mohammadag.soundrecorder.R;

public class DrawerMenuArrayAdapter extends ArrayAdapter<String> {

	public static final int NEW_RECORDING_POS = 0;
	public static final int RECORDINGS_LIST_POS = 1;
	public static final int SETTINGS_POS = 2;
	public static final int ABOUT_POS = 3;

	private int mSelectedPosition = 0;

	public DrawerMenuArrayAdapter(Context context) {
		super(context, 0);	
	}

	@Override
	public int getCount() {
		return 4;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		String title = getTitleForPosition(position);
		int drawableId = 0;

		switch (position) {
		case SETTINGS_POS:
			drawableId = R.drawable.ic_action_settings;
			break;
		case ABOUT_POS:
			drawableId = R.drawable.ic_action_about;
			break;
		}

		int listItemResourceId;
		if (drawableId == 0) {
			listItemResourceId = R.layout.drawer_list_item;
		} else {
			listItemResourceId = R.layout.drawer_small_list_item;
		}

		v = LayoutInflater.from(getContext()).inflate(listItemResourceId, parent, false);
		TextView titleView = (TextView) v.findViewById(android.R.id.text1);
		titleView.setText(title);

		if (drawableId != 0) {
			ImageView icon = (ImageView) v.findViewById(android.R.id.icon);
			icon.setImageResource(drawableId);
			titleView.setTextColor(Color.GRAY);
		} else {
			titleView.setTextColor(Color.BLACK);
		}

		if (position == mSelectedPosition) {
			titleView.setTypeface(null, Typeface.BOLD);
		} else {
			titleView.setTypeface(null, Typeface.NORMAL);
		}

		return v;
	}

	public void setItemSelected(int position) {
		mSelectedPosition = position;
		notifyDataSetChanged();
	}

	public int getSelectedItemPosition() {
		return mSelectedPosition;
	}

	public String getTitleForPosition(int position) {
		String title = null;
		switch (position) {
		case NEW_RECORDING_POS:
			title = getString(R.string.record);
			break;
		case RECORDINGS_LIST_POS:
			title = getString(R.string.my_recordings);
			break;
		case SETTINGS_POS:
			title = getString(R.string.settings).toUpperCase(Locale.getDefault());
			break;
		case ABOUT_POS:
			title = getString(R.string.about).toUpperCase(Locale.getDefault());
			break;
		}

		return title;
	}

	private String getString(int resId) {
		return getContext().getString(resId);
	}

	public String getCurrentTitle() {
		int title = R.string.app_name;
		switch (mSelectedPosition) {
		case RECORDINGS_LIST_POS:
			title = R.string.my_recordings;
			break;
		case SETTINGS_POS:
			title = R.string.settings;
			break;
		}

		return getContext().getString(title);
	}
}
