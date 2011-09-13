/*
iLinphoneActivity.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone;


import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import net.chrislehmann.linphone.R;
import org.linphone.LinphoneManager.EcCalibrationListener;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.Log;
import org.linphone.core.Version;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

import static android.content.Intent.ACTION_MAIN;

public class LinphoneActivity extends TabActivity  {
	public static final String DIALER_TAB = "dialer";
    public static final String PREF_FIRST_LAUNCH = "pref_first_launch";
    private static final int video_activity = 100;
    static final int FIRST_LOGIN_ACTIVITY = 101;
    static final int INCALL_ACTIVITY = 102;
    static final int INCOMING_CALL_ACTIVITY = 103;
    private static final String PREF_CHECK_CONFIG = "pref_check_config";

	private static LinphoneActivity instance;
	
	
	private FrameLayout mMainFrame;
	private SensorManager mSensorManager;
	private static SensorEventListener mSensorEventListener;
	
	private static final String SCREEN_IS_HIDDEN = "screen_is_hidden";
	private Handler mHandler = new Handler();
	
	
	// Customization
	private static boolean useFirstLoginActivity;
	private static boolean useMenuSettings;
	private static boolean useMenuAbout;
	private boolean checkAccount;

	
	static final boolean isInstanciated() {
		return instance != null;
	}
	
	static final LinphoneActivity instance() {
		if (instance != null) return instance;

		throw new RuntimeException("LinphoneActivity not instantiated yet");
	}

	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SCREEN_IS_HIDDEN, mMainFrame.getVisibility() == View.INVISIBLE);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		setContentView(R.layout.main);

		LinphonePreferenceManager.getInstance(this);
		useFirstLoginActivity = getResources().getBoolean(R.bool.useFirstLoginActivity);
		useMenuSettings = getResources().getBoolean(R.bool.useMenuSettings);
		useMenuAbout = getResources().getBoolean(R.bool.useMenuAbout);
		checkAccount = !useFirstLoginActivity;

		// start linphone as background       
		startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));

		
		mMainFrame = (FrameLayout) findViewById(R.id.main_frame);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		if (!useFirstLoginActivity || pref.getBoolean(getString(R.string.first_launch_suceeded_once_key), false)) {
			fillTabHost();
		} else {
			startActivityForResult(new Intent().setClass(this, FirstLoginActivity.class), FIRST_LOGIN_ACTIVITY);
		}

		if (checkAccount && !useFirstLoginActivity) {
			if (pref.getBoolean(PREF_FIRST_LAUNCH, true)) {
				onFirstLaunch();
			} else if (!pref.getBoolean(PREF_CHECK_CONFIG, false)
					&& !checkDefined(pref, R.string.pref_username_key, R.string.pref_passwd_key, R.string.pref_domain_key)) {
				onBadSettings(pref);
			} else {
				checkAccount = false;
			}
		}
		
	    if (savedInstanceState !=null && savedInstanceState.getBoolean(SCREEN_IS_HIDDEN,false)) {
	    	hideScreen(true);
	    }
	     
	    if (false) {
	    	try {
		    	KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		    	Enumeration<String> al = ks.aliases();
		    	while(al.hasMoreElements()) {
					Log.i(al.nextElement());
				}
		    	Log.i("Enumeration done");
	    	} catch (KeyStoreException e) {
     	    		e.printStackTrace();
	    	}
			
	    } else if (false) {
		try {
			String defaultAlg = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(defaultAlg);
			// init is needed for Android to fill the javax.net.ssl.trustStore property
			// ref : http://groups.google.com/group/android-developers/browse_thread/thread/366a3c8a6b2a7ad/163ff07c8ac39929?lnk=gst&q=SSL+root
			tmf.init((KeyStore)null);
			String trustStore = System.getProperty("javax.net.ssl.trustStore");
			Log.i(trustStore + "\n");
			
			for(TrustManager tm: tmf.getTrustManagers()) {
				X509TrustManager xtm = (X509TrustManager)tm;
				Log.i(xtm.getAcceptedIssuers().length);
				for(X509Certificate ca : xtm.getAcceptedIssuers()) {
					byte[] encoded = ca.getEncoded();
					String s = new String(encoded);
					byte[] d2 = Base64.decode(encoded, 0);
					String s2 = new String(d2);
					Log.i(ca.toString());
					
					
				}
			}
		} catch (KeyStoreException e) {
		} catch (NoSuchAlgorithmException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	    }
	
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (requestCode == FIRST_LOGIN_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				Toast.makeText(this, getString(R.string.ec_calibration_launch_message), Toast.LENGTH_LONG).show();
				try {
					LinphoneManager.getInstance().startEcCalibration(new EcCalibrationListener() {
						public void onEcCalibrationStatus(EcCalibratorStatus status, int delayMs) {
							PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.this)
							.edit().putBoolean(
									getString(R.string.pref_echo_canceller_calibration_key),
									status == EcCalibratorStatus.Done).commit();
						}
					});
				} catch (LinphoneCoreException e) {
					Log.e(e, "Unable to calibrate EC");
				}

				fillTabHost();
			} else {
				finish();
				stopService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}


	private synchronized void fillTabHost() {
		if (((TabWidget) findViewById(android.R.id.tabs)).getChildCount() != 0) return;

	    startActivityInTab("history",
	    		new Intent().setClass(this, HistoryActivity.class),
	    		R.string.tab_history, R.drawable.history_orange);

	    
	    startActivityInTab(DIALER_TAB,
	    		new Intent().setClass(this, DialerActivity.class).setData(getIntent().getData()),
	    		R.string.tab_dialer, R.drawable.dialer_orange);
	    

	    startActivityInTab("contact",
	    		new Intent().setClass(this, Version.sdkAboveOrEqual(5) ?
	    		ContactPickerActivityNew.class : ContactPickerActivityOld.class),
	    		R.string.tab_contact, R.drawable.contact_orange);


	    /*if (LinphoneService.isReady()) {
	    	LinphoneCore lc = LinphoneManager.getLc();
	    	if (lc.isIncall()) {
	    		String caller = LinphoneManager.getInstance().extractADisplayName();
	    		startIncallActivity(caller);
	    	}
	    }*/
	    
	    gotToDialer();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent.getData() == null) {
			Log.e("LinphoneActivity received an intent without data, recreating GUI if needed");
			if (!LinphoneService.isReady() || !LinphoneManager.getLc().isIncall()) return;
			LinphoneCore lc = LinphoneManager.getLc();
			if(lc.isInComingInvitePending()) {
				gotToDialer();
			} else {
				if (getResources().getBoolean(R.bool.use_incall_activity)) {
					startIncallActivity(LinphoneManager.getInstance().extractADisplayName(), null);
				} else {
					// TODO
					Log.e("Not handled case: recreation while in call and not using incall activity");
				}
			}
			return;
		}


		if (DialerActivity.instance() != null) {
			DialerActivity.instance().newOutgoingCall(intent);
		} else {
			Toast.makeText(this, getString(R.string.dialer_null_on_new_intent), Toast.LENGTH_LONG).show();
		}
	}
	@Override
	protected void onPause() {
		super.onPause();
		if  (isFinishing())  {
			//restore audio settings   
			LinphoneManager.getInstance().routeAudioToReceiver();
			stopProxymitySensor();//just in case
			instance = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the currently selected menu XML resource.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.linphone_activity_menu, menu);
		
		menu.findItem(R.id.menu_settings).setVisible(useMenuSettings);
		menu.findItem(R.id.menu_about).setVisible(useMenuAbout);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startprefActivity();
			return true;
		case R.id.menu_exit:
			finish();
			stopService(new Intent(ACTION_MAIN)
				.setClass(this, LinphoneService.class));
			break;
		case R.id.menu_about:
			startActivity(new Intent(ACTION_MAIN)
				.setClass(this, AboutActivity.class));
		default:
			Log.e("Unknown menu item [", item, "]");
			break;
		}

		return false;
	}

	void startprefActivity() {
		Intent intent = new Intent(ACTION_MAIN);
		intent.setClass(this, LinphonePreferencesActivity.class);
		startActivity(intent);
	}



	void hideScreen(boolean isHidden) {
		WindowManager.LayoutParams lAttrs =getWindow().getAttributes(); 
		if (isHidden) {
			lAttrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN; 
			mMainFrame.setVisibility(View.INVISIBLE);
		} else  {
			lAttrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN); 
			mMainFrame.setVisibility(View.VISIBLE);
		}
		getWindow().setAttributes(lAttrs);
	}

	synchronized void startProxymitySensor() {
		if (mSensorEventListener != null) {
			Log.i("proximity sensor already active");
			return;
		}
		List<Sensor> lSensorList = mSensorManager.getSensorList(Sensor.TYPE_PROXIMITY);
		mSensorEventListener = new SensorEventListener() {
			public void onSensorChanged(SensorEvent event) {
				//just ignoring for nexus 1
				if (event.timestamp == 0) return;
				instance().hideScreen(LinphoneManager.isProximitySensorNearby(event));
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {}	
		};
		if (lSensorList.size() >0) {
			mSensorManager.registerListener(mSensorEventListener,lSensorList.get(0),SensorManager.SENSOR_DELAY_UI);
			Log.i("Proximity sensor detected, registering");
		}		
	}

	protected synchronized void stopProxymitySensor() {
		if (mSensorManager!=null) {
			mSensorManager.unregisterListener(mSensorEventListener);
			mSensorEventListener=null; 
		}
		hideScreen(false);
	}


	void showPreferenceErrorDialog(String message) {
		if (!useMenuSettings) {
			Toast.makeText(this, message, Toast.LENGTH_LONG);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setMessage(String.format(getString(R.string.config_error), message))
			.setCancelable(false)
			.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					startprefActivity();
				}
			})
			.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});

			builder.create().show();
		}
	}

	public void onRegistrationStateChanged(RegistrationState state,
			String message) {
		
		if (FirstLoginActivity.instance != null) {
			FirstLoginActivity.instance.onRegistrationStateChanged(state);
		}
	}
	
	
	
	/***** Check Account *******/
	private boolean checkDefined(SharedPreferences pref, int ... keys) {
		for (int key : keys) {
			String conf = pref.getString(getString(key), null);
			if (conf == null || "".equals(conf))
				return false;
		}
		return true;
	}

	private void onFirstLaunch() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		TextView lDialogTextView = new TextView(this);
		lDialogTextView.setAutoLinkMask(0x0f/*all*/);
		lDialogTextView.setPadding(10, 10, 10, 10);

		lDialogTextView.setText(Html.fromHtml(getString(R.string.first_launch_message)));

		builder.setCustomTitle(lDialogTextView)
		.setCancelable(false)
		.setPositiveButton(getString(R.string.cont), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				LinphoneManager.getInstance().initializePayloads();
				startprefActivity();
				checkAccount = false;
			}
		});

		builder.create().show();
	}

	private void onBadSettings(final SharedPreferences pref) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		TextView lDialogTextView = new TextView(this);
		lDialogTextView.setAutoLinkMask(0x0f/*all*/);
		lDialogTextView.setPadding(10, 10, 10, 10);

		lDialogTextView.setText(Html.fromHtml(getString(R.string.initial_config_error)));

		builder.setCustomTitle(lDialogTextView)
		.setCancelable(false)
		.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				startprefActivity();
				checkAccount = false;
			}
		}).setNeutralButton(getString(R.string.no), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				checkAccount = false;
			}
		}).setNegativeButton(getString(R.string.never_remind), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				pref.edit().putBoolean(PREF_CHECK_CONFIG, true).commit();
				dialog.cancel();
				checkAccount = false;
			}
		});

		builder.create().show();
	}

	static void setAddressAndGoToDialer(String number, String name, Uri photo) {
		DialerActivity.instance().setContactAddress(number, name, photo);
		instance.gotToDialer();
	}

	private void gotToDialer() {
		getTabHost().setCurrentTabByTag(DIALER_TAB);
	}

	
	private void startActivityInTab(String tag, Intent intent, int indicatorId, int drawableId) {
	    Drawable tabDrawable = getResources().getDrawable(drawableId);
	    TabSpec spec = getTabHost().newTabSpec(tag)
	    		.setIndicator(getString(indicatorId), tabDrawable)
	    		.setContent(intent);
	    getTabHost().addTab(spec);
	}

	public void startIncallActivity(CharSequence callerName, Uri pictureUri) {
		Intent intent = new Intent().setClass(this, IncallActivity.class)
                                .putExtra(IncallActivity.CONTACT_KEY, callerName);
		if (pictureUri != null)
			intent.putExtra(IncallActivity.PICTURE_URI_KEY, pictureUri.toString());
		startActivityForResult(intent, INCALL_ACTIVITY);
	}

	public void closeIncallActivity() {
		finishActivity(INCALL_ACTIVITY);
	}

	public void startVideoActivity() {
		mHandler.post(new Runnable() {
			public void run() {
				startActivityForResult(new Intent().setClass(
						LinphoneActivity.this,
						VideoCallActivity.class),
						video_activity);
				}
		});

	}
	
	public void finishVideoActivity() {
		mHandler.post(new Runnable() {
			public void run() {
				finishActivity(video_activity);
			}
		});
	}
}
