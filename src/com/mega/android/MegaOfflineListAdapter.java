package com.mega.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.mega.sdk.MegaNode;
import com.mega.sdk.ShareList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MegaOfflineListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
 
	int positionClicked;
	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";
	public DatabaseHandler dbH;

	ArrayList<MegaOffline> mOffList = new ArrayList<MegaOffline>();	
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	OfflineFragment fragment;
	//ArrayList<MegaOffline> mOffList;
	
	boolean multipleSelect;
	
	/*public static view holder class*/
    public class ViewHolderOfflineList {
    	CheckBox checkbox;
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewFileSize;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
        ImageView optionOpen;
//        ImageView optionProperties;
        ImageView optionDelete;
        int currentPosition;
        String currentPath;
        String currentHandle;
    }
    
    private class OfflineThumbnailAsyncTask extends AsyncTask<String, Void, Bitmap>{

    	ViewHolderOfflineList holder;
    	String currentPath;
    	
    	public OfflineThumbnailAsyncTask(ViewHolderOfflineList holder) {
			this.holder = holder;
		}
    	
		@Override
		protected Bitmap doInBackground(String... params) {

			currentPath = params[0];
			File currentFile = new File(currentPath);
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap thumb = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			
			ExifInterface exif;
			int orientation = ExifInterface.ORIENTATION_NORMAL;
			try {
				exif = new ExifInterface(currentFile.getAbsolutePath());
				orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			} catch (IOException e) {}  
			
			// Calculate inSampleSize
		    options.inSampleSize = Util.calculateInSampleSize(options, 270, 270);
		    
		    // Decode bitmap with inSampleSize set
		    options.inJustDecodeBounds = false;
		    
		    thumb = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
			if (thumb != null){
				thumb = Util.rotateBitmap(thumb, orientation);
				long handle = Long.parseLong(holder.currentHandle);
				ThumbnailUtils.setThumbnailCache(handle, thumb);
				return thumb;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap thumb){
			if (thumb != null){
				if (holder.currentPath.compareTo(currentPath) == 0){
					holder.imageView.setImageBitmap(thumb);
					Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
					holder.imageView.startAnimation(fadeInAnimation);
				}
			}
		}    	
    }
	
	public MegaOfflineListAdapter(OfflineFragment _fragment, Context _context, ArrayList<MegaOffline> _mOffList, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB) {
		this.fragment = _fragment;
		this.context = _context;
		this.mOffList = _mOffList;

		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
	}
	
	public void setNodes(ArrayList<MegaOffline> mOffList){
		this.mOffList = mOffList;
		positionClicked = -1;	
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		log("MegaOfflineListAdapter:getView");
		View v;		
	
		listFragment = (ListView) parent;
		
		final int _position = position;
		
		ViewHolderOfflineList holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_offline_list, parent, false);
			holder = new ViewHolderOfflineList();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.offline_list_item_layout);
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.offline_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.imageView = (ImageView) convertView.findViewById(R.id.offline_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.offline_list_filename);
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.offline_list_filesize);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.offline_list_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.offline_list_options);
			holder.optionOpen = (ImageView) convertView.findViewById(R.id.offline_list_option_open);
			holder.optionOpen.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionOpen.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionDelete = (ImageView) convertView.findViewById(R.id.offline_list_option_delete);
			holder.optionDelete.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.offline_list_arrow_selection);
			holder.arrowSelection.setVisibility(View.GONE);
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderOfflineList) convertView.getTag();
		}
		
		if (!multipleSelect){
			holder.checkbox.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
		}
		else{
			holder.checkbox.setVisibility(View.VISIBLE);
			holder.arrowSelection.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.GONE);
			
			SparseBooleanArray checkedItems = listFragment.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				holder.checkbox.setChecked(true);
			}
			else{
				holder.checkbox.setChecked(false);
			}
		}
				
		MegaOffline currentNode = (MegaOffline) getItem(position);
		
		File currentFile = null;
		if (Environment.getExternalStorageDirectory() != null){
			currentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + currentNode.getPath()+currentNode.getName());
		}
		else{
			currentFile = context.getFilesDir();
		}
		
		holder.currentPath = currentFile.getAbsolutePath();
		holder.currentHandle = currentNode.getHandle();
		holder.currentPosition = position;
		
		holder.textViewFileName.setText(currentNode.getName());
		
		int folders=0;
		int files=0;
		if (currentFile.isDirectory()){
			
			File[] fList = currentFile.listFiles();
			for (File f : fList){
				
				if (f.isDirectory()){
					folders++;						
				}
				else{
					files++;
				}
			}
			
			String info = "";
			if (folders > 0){
				info = folders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, folders);
				if (files > 0){
					info = info + ", " + files + " " + context.getResources().getQuantityString(R.plurals.general_num_files, folders);
				}
			}
			else {
				info = files +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, files);
			}			
					
			holder.textViewFileSize.setText(info);			
		}
		else{
			long nodeSize = currentFile.length();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
		}
		
		holder.imageView.setImageResource(MimeType.typeForName(currentNode.getName()).getIconResourceId());
		if (currentFile.isFile()){
			if (MimeType.typeForName(currentNode.getName()).isImage()){
				Bitmap thumb = null;
								
				if (currentFile.exists()){
					thumb = ThumbnailUtils.getThumbnailFromCache(Long.parseLong(currentNode.getHandle()));
					if (thumb != null){
						holder.imageView.setImageBitmap(thumb);
					}
					else{
						try{
							new OfflineThumbnailAsyncTask(holder).execute(currentFile.getAbsolutePath());
						}
						catch(Exception e){
							//Too many AsyncTasks
						}
					}
				}
			}
		}
		else{
			holder.imageView.setImageResource(R.drawable.mime_folder);
		}
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		
		if (positionClicked != -1){
			if (positionClicked == position){
				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_grey);
				listFragment.smoothScrollToPosition(_position);
				
				holder.optionOpen.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
				((TableRow.LayoutParams) holder.optionOpen.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
				holder.optionDelete.getLayoutParams().width = Util.px2dp((165*scaleW), outMetrics);
				((TableRow.LayoutParams) holder.optionDelete.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			}
			else{
				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
			}
		}
		else{
			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
		}
		
		holder.optionOpen.setTag(holder);
		holder.optionOpen.setOnClickListener(this);
		
//		holder.optionProperties.setTag(holder);
//		holder.optionProperties.setOnClickListener(this);
		
		holder.optionDelete.setTag(holder);
		holder.optionDelete.setOnClickListener(this);
		
		return convertView;
	}

	@Override
	public boolean isEnabled(int position) {
		return super.isEnabled(position);
	}

	@Override
    public int getCount() {
		return mOffList.size();
    }
 
    @Override
    public Object getItem(int position) {
        return mOffList.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }

	@Override
	public void onClick(View v) {
		ViewHolderOfflineList holder = (ViewHolderOfflineList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaOffline mOff = (MegaOffline) getItem(currentPosition);
		String currentPath = mOff.getPath()+mOff.getName(); 
		File currentFile = new File(currentPath);
		
		switch (v.getId()){
			case R.id.offline_list_option_open:{
				positionClicked = -1;
				notifyDataSetChanged();
				
				Intent viewIntent = new Intent(Intent.ACTION_VIEW);
				viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
				if (ManagerActivity.isIntentAvailable(context, viewIntent)){
					context.startActivity(viewIntent);
				}
				else{
					Intent intentShare = new Intent(Intent.ACTION_SEND);
					intentShare.setDataAndType(Uri.fromFile(currentFile), MimeType.typeForName(currentFile.getName()).getType());
					if (ManagerActivity.isIntentAvailable(context, intentShare)){
						context.startActivity(intentShare);
					}
				}
				break;
			}
//			case R.id.offline_list_option_properties:{
//				Intent i = new Intent(context, FilePropertiesActivity.class);
//				i.putExtra("handle", n.getHandle());
//			
//				if (n.isFolder()){
//					i.putExtra("imageId", R.drawable.mime_folder);
//				}
//				else{
//					i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());	
//				}				
//				i.putExtra("name", n.getName());
//				context.startActivity(i);							
//				positionClicked = -1;
//				notifyDataSetChanged();
//				break;
//			}
			case R.id.offline_list_option_delete:{
				setPositionClicked(-1);
				notifyDataSetChanged();				
									
				deleteOffline(context, mOff);
								
				fragment.refreshPaths(mOff);
				
				break;
			}
			case R.id.offline_list_three_dots:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				break;
			}
		}		
	}
	
	/*
	 * Get path at specified position
	 */
	public String getPathAt(int position) {
//		try {
//			if(paths != null){
//				return paths.get(position);
//			}
//		} catch (IndexOutOfBoundsException e) {}
		return null;
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		if(this.multipleSelect != multipleSelect){
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	private int deleteOffline(Context context,MegaOffline node){
		
		log("deleteOffline");

//		dbH = new DatabaseHandler(context);
		dbH = DatabaseHandler.getDbHandler(context);

		ArrayList<MegaOffline> mOffListParent=new ArrayList<MegaOffline>();
		ArrayList<MegaOffline> mOffListChildren=new ArrayList<MegaOffline>();			
		MegaOffline parentNode = null;	
		
		//Delete children
		mOffListChildren=dbH.findByParentId(node.getId());
		if(mOffListChildren.size()>0){
			//The node have childrens, delete
			deleteChildrenDB(mOffListChildren);			
		}
		
		int parentId = node.getParentId();
		log("Finding parents...");
		//Delete parents
		if(parentId!=-1){
			mOffListParent=dbH.findByParentId(parentId);
			
			log("Same Parent?:" +mOffListParent.size());
			
			if(mOffListParent.size()<1){
				//No more node with the same parent, keep deleting				

				parentNode = dbH.findById(parentId);
				log("Recursive parent: "+parentNode.getName());
				if(parentNode != null){
					deleteOffline(context, parentNode);	
						
				}	
			}			
		}	
		
		log("Remove the node physically");
		//Remove the node physically
		File destination = null;								

		if (Environment.getExternalStorageDirectory() != null){
			destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + node.getPath());
		}
		else{
			destination = context.getFilesDir();
		}	

		try{
			File offlineFile = new File(destination, node.getName());	
			log("Delete in phone: "+node.getName());
			Util.deleteFolderAndSubfolders(context, offlineFile);
		}
		catch(Exception e){
			log("EXCEPTION: deleteOffline - adapter");
		};		
		
		log("Delete in DB: "+node.getId());
		dbH.removeById(node.getId());		
		
		return 1;		
	}	

	private void deleteChildrenDB(ArrayList<MegaOffline> mOffListChildren){
		
		log("deleteChildenDB: "+mOffListChildren.size());
		MegaOffline mOffDelete=null;
	
		for(int i=0; i<mOffListChildren.size(); i++){	
			
			mOffDelete=mOffListChildren.get(i);
			
			log("Children "+i+ ": "+ mOffDelete.getName());
			ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
			if(mOffListChildren2.size()>0){
				//The node have children, delete				
				deleteChildrenDB(mOffListChildren2);				
			}	
			
			int lines = dbH.removeById(mOffDelete.getId());		
			log("Borradas; "+lines);
		}		
	}	
	
	
	private static void log(String log) {
		Util.log("MegaOfflineListAdapter", log);
	}
}
