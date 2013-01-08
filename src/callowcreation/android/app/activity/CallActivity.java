package callowcreation.android.app.activity;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import callowcreation.android.app.justcall.R;

public class CallActivity extends Activity {

	private final String SETTINGS_PREFS_NAME = "SETTINGS_PREFERENCE";
	static final int PICK_CONTACT_REQUEST = 1; // The request code

	SharedPreferences settings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		settings = getSharedPreferences(SETTINGS_PREFS_NAME, 0);
		boolean firstRun = settings.getBoolean("firstRun", false);
		//String callName = settings.getString("callName", "");
		final String callNumber = settings.getString("callNumber", "");
 
		Intent in = getIntent();
		if(in.hasExtra("fromShortcut") && in.getExtras().getBoolean("fromShortcut")) {
			makeCall(callNumber);
		} else {
			if (firstRun == false) {
				Builder b = new Builder(this);
				b.setCancelable(false);
				b.setTitle(R.string.app_name);
				b.setMessage(textWithAppName(R.string.dialog_text));
				AlertDialog dialog = b.create();
				OnClickListener listener = new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						pickContact();
					}
				};
				dialog.setButton(DialogInterface.BUTTON_POSITIVE,
						getString(R.string.dialog_ok), listener);
				dialog.show();
			} else {
				
				Builder b = new Builder(this);
				b.setCancelable(false);
				b.setTitle(R.string.app_name);
				b.setMessage(textWithAppName(R.string.dialog_changing_contact_text));
				AlertDialog dialog = b.create();
				OnClickListener yeslistener = new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						pickContact();
					}
				};
				OnClickListener nolistener = new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callContactNow(callNumber);
					}
				};
				dialog.setButton(DialogInterface.BUTTON_POSITIVE,
						getString(R.string.dialog_yes), yeslistener);
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						getString(R.string.dialog_no), nolistener);
				dialog.show();
				
			}
		}
		
		/*if (firstRun == false) {
			Builder b = new Builder(this);
			b.setCancelable(false);
			b.setTitle(R.string.app_name);
			b.setMessage(textWithAppName(R.string.dialog_not_phone_contact_text));
			AlertDialog dialog = b.create();
			OnClickListener listener = new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					pickContact();
				}

			};
			dialog.setButton(DialogInterface.BUTTON_POSITIVE,
					getString(R.string.dialog_ok), listener);
			dialog.show();
		} else {
			makeCall(callNumber);
		}*/
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request it is that we're responding to
		if (requestCode == PICK_CONTACT_REQUEST) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {

				settings.edit().putBoolean("firstRun", true).commit();

				// Get the URI that points to the selected contact
				Uri contactUri = data.getData();
				String[] projection = { Phone.DISPLAY_NAME, Phone.NUMBER};

				Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
				cursor.moveToFirst();

				// Retrieve the phone number from the NUMBER column
				int nameColumn = cursor.getColumnIndex(Phone.DISPLAY_NAME);
				int numberColumn = cursor.getColumnIndex(Phone.NUMBER);
				
				try {
					final String callName = cursor.getString(nameColumn);
					final String callNumber = cursor.getString(numberColumn);
					
					Bitmap bmp = loadLocalContactPhotoBytes(getContentResolver(), contactUri);

					if(bmp == null) {
						bmp = defaultImage(callName);
					}
					
					// Do something with the phone number...
					settings.edit().putString("callNumber", callNumber).commit();
					//createLauncher(callName, bmp);
					boolean shortcutCreated = createShortCut(getString(R.string.app_name), this, bmp);
					if(shortcutCreated == false) {
						
					}
					
					
					callContactNow(callNumber);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					//Log.d(CallActivity.class.getSimpleName(), e.getMessage());
					Builder b = new Builder(this);
					b.setCancelable(false);
					b.setTitle(R.string.app_name);
					b.setMessage(textWithAppName(R.string.dialog_not_phone_contact_text));
					AlertDialog dialog = b.create();
					OnClickListener listener = new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							pickContact();
						}

					};
					dialog.setButton(DialogInterface.BUTTON_POSITIVE,
							getString(R.string.dialog_ok), listener);
					dialog.show();
				} finally {
					cursor.close();
				}
			}
		}
	}

	private void callContactNow(final String callNumber) {
		Builder b = new Builder(this);
		b.setCancelable(false);
		b.setTitle(R.string.app_name);
		b.setMessage(textWithAppName(R.string.dialog_call_text));
		AlertDialog dialog = b.create();
		OnClickListener yeslistener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				makeCall(callNumber);
			}
		};
		OnClickListener nolistener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}

		};
		dialog.setButton(DialogInterface.BUTTON_POSITIVE,
				getString(R.string.dialog_yes), yeslistener);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				getString(R.string.dialog_no), nolistener);
		dialog.show();
	}
	private String textWithAppName(int resId) {
		return getString(resId).replace("%APP_NAME", getString(R.string.app_name));
	}
	
    private Bitmap defaultImage(String callName) {
    	return drawTextToBitmap(this, R.drawable.icon, callName);
    }
    public Bitmap drawTextToBitmap(Context mContext,  int resourceId,  String mText) {
	    try {
	         Resources resources = mContext.getResources();
	            float scale = resources.getDisplayMetrics().density;
	            Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId);
	
	            android.graphics.Bitmap.Config bitmapConfig =   bitmap.getConfig();
	            // set default bitmap config if none
	            if(bitmapConfig == null) {
	              bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
	            }
	            // resource bitmaps are imutable,
	            // so we need to convert it to mutable one
	            bitmap = bitmap.copy(bitmapConfig, true);
	
	            Canvas canvas = new Canvas(bitmap);
	            // new antialised Paint
	            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	            // text color - #3D3D3D
	            paint.setColor(Color.rgb(110,110, 110));
	            // text size in pixels
	            paint.setTextSize((int) (11 * scale));
	            // text shadow
	            paint.setShadowLayer(1f, 0f, 1f, Color.DKGRAY);
	
	            String[] tSplit = mText.split(" ");
	            int tLength = tSplit.length;
	            for(int i=0;i<tLength;i++) {
	            	String text = tSplit[i];
		            // draw text to the Canvas center
		            Rect bounds = new Rect();
		            paint.getTextBounds(text, 0, text.length(), bounds);	            
		            int x = (bitmap.getWidth() / 2 - bounds.width() / 2);
		            int y = (bitmap.getHeight() / 2 + (i)*(bounds.height() / 2)) + (2 * i) - 2;
		
		            canvas.drawText(text, x * scale, y * scale, paint);
	            }
	
	            return bitmap;
	    } catch (Exception e) {
	        // TODO: handle exception
	
	    	Log.d(CallActivity.class.getSimpleName(), e.getMessage());
	
	        return null;
	    }

    }    

    private Bitmap loadLocalContactPhotoBytes(ContentResolver cr, Uri contactUri)
    {                  
    	String[] projection = {ContactsContract.CommonDataKinds.Photo.PHOTO};
    	Cursor cursor = this.getContentResolver().query(contactUri, projection, null, null, null);

    	if(cursor!=null && cursor.moveToFirst()){
    	    do{
    	        byte[] photoData = cursor.getBlob(0);
    	        if(photoData != null) {
	    	        Bitmap photo = BitmapFactory.decodeByteArray(photoData, 0,
	    	                photoData.length, null);
	
	    	        //Do whatever with your photo here...
	    	        if(photo != null) {
	    	        	cursor.close();
	    	        	return photo;
	    	        } 
    	        }
    	    }while(cursor.moveToNext());
    	}
    	cursor.close();
		return null;
    }
	private void pickContact() {
		Intent pickContactIntent = new Intent(Intent.ACTION_PICK,
				Uri.parse("content://contacts"));
		pickContactIntent.setType(Phone.CONTENT_TYPE); // Show user only
														// contacts w/ phone
														// numbers
		startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
	}

	private void makeCall(String callNumber) {
		Uri number = Uri.parse("tel:" + callNumber);
		Intent callIntent = new Intent(Intent.ACTION_CALL, number);
		startActivity(callIntent);
		finish();
	}

	private static boolean createShortCut(String appName, Context context, Bitmap newbit)// throws
																		// UserException
	{
 
		try {
			// Log.i("shortcut method in androidhelper start","in the shortcutapp on create method ");
			boolean flag = false;
			int app_id = -1;
			PackageManager p = context.getPackageManager();
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> res = p.queryIntentActivities(i, 0);
			// System.out.println("the res size is: "+res.size());

			for (int k = 0; k < res.size(); k++) {
				// Log.i("","the application name is: "+res.get(k).activityInfo.loadLabel(p));
				if (res.get(k).activityInfo.loadLabel(p).toString().equals(appName)) {
					flag = true;
					app_id = k;
					break;
				}
			}

			if (flag) {
				ActivityInfo ai = res.get(app_id).activityInfo;
				Intent shortcutIntent = new Intent();
				shortcutIntent.setClassName(ai.packageName, ai.name);
				shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				shortcutIntent.addCategory(Intent.ACTION_PICK_ACTIVITY);
				shortcutIntent.putExtra("fromShortcut", true);
				
				
				Intent removeIntent = new Intent();
				removeIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				removeIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Shortcut Name");
				removeIntent.putExtra("duplicate", false);
				removeIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");

				context.sendBroadcast(removeIntent);
				
				
				Intent intent = new Intent();
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				// Sets the custom shortcut's title
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, ""/*context.getString(R.string.call) + " " + callName*/);

				if(newbit == null) {
					BitmapDrawable bd = (BitmapDrawable) (res.get(app_id).activityInfo.loadIcon(p).getCurrent());
					newbit = bd.getBitmap();					
				}
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newbit);

				intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
				context.sendBroadcast(intent);

			} else {
				// throw new
				// UserException(UserException.KErrGeneral,"Application not found");
				return false;
			}

		}

		catch (ActivityNotFoundException e) {
			e.printStackTrace();
			// throw new
			// UserException(UserException.KErrGsmRRNoActivityOnRadioPath,e.getMessage());
			return false;
		}

		catch (Exception e) {
			e.printStackTrace();
			// throw new
			// UserException(UserException.KErrGeneral,e.getMessage());
			return false;
		}
		return true;
	}

}
