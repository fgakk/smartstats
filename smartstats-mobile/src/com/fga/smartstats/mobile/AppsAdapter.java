package com.fga.smartstats.mobile;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppsAdapter extends ArrayAdapter<Object>{
	
	private static final String TAG = AppsAdapter.class.getName();
	private Object[] items;
	private Context context;
	
	public AppsAdapter(Context context, Object[] items) {
		super(context, R.layout.row, items);
		this.items = items.clone();
		this.context = context;
	}
	
	@Override
	public View getView(int position, View convertView,
	ViewGroup parent) {
	
		PackageManager pm = context.getPackageManager();
		AppsViewHolder viewHolder;
		
		if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.row, parent, false);
 
            viewHolder = new AppsViewHolder();
            viewHolder.appName = (TextView) convertView.findViewById(R.id.label);
            viewHolder.appIcon = (ImageView) convertView.findViewById(R.id.icon);
          
 
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AppsViewHolder) convertView.getTag();
        }
		CharSequence applicationName = null;
		
			String packageName = (String) items[position];
			if (packageName != null){
				try {
					applicationName = pm.getApplicationLabel(pm.getApplicationInfo(
							packageName, PackageManager.GET_META_DATA));
					Drawable applicationIcon = pm.getApplicationIcon(packageName);

					viewHolder.appName.setText(applicationName);
					viewHolder.appIcon.setImageDrawable(applicationIcon);
				} catch (NameNotFoundException e) {
					Log.e(TAG, e.getMessage());

				}

			}
			
		
		return convertView;
		
	}
	
	static class AppsViewHolder {
		
		TextView appName;
		ImageView appIcon;
	

	}
}
