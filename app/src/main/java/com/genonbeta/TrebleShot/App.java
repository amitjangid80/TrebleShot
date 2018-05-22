package com.genonbeta.TrebleShot;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.preference.DbSharablePreferences;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.PreferenceUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;

import velitasali.updatewithgithub.GitHubUpdater;

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */

public class App extends Application
{
	public static final String TAG = App.class.getSimpleName();
	public static final String ACTION_REQUEST_PREFERENCES_SYNC = "com.genonbeta.intent.action.REQUEST_PREFERENCES_SYNC";

	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent != null)
				if (ACTION_REQUEST_PREFERENCES_SYNC.equals(intent.getAction())) {
					SharedPreferences preferences = AppUtils.getDefaultPreferences(context).getWeakManager();

					if (preferences instanceof DbSharablePreferences)
						((DbSharablePreferences) preferences).sync();
				}
		}
	};

	@Override
	public void onCreate()
	{
		super.onCreate();

		initializeSettings();
		getApplicationContext().registerReceiver(mReceiver, new IntentFilter(ACTION_REQUEST_PREFERENCES_SYNC));

		if (!UpdateUtils.hasNewVersion(getApplicationContext())
				&& (System.currentTimeMillis() - UpdateUtils.getLastTimeCheckedForUpdates(getApplicationContext())) >= AppConfig.DELAY_CHECK_FOR_UPDATES) {
			GitHubUpdater updater = UpdateUtils.getDefaultUpdater(getApplicationContext());
			UpdateUtils.checkForUpdates(getApplicationContext(), updater, false, null);
		}
	}

	@Override
	public void onTerminate()
	{
		super.onTerminate();
		getApplicationContext().unregisterReceiver(mReceiver);
	}

	private void initializeSettings()
	{
		SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);

		boolean nsdDefined = defaultPreferences.contains("nsd_enabled");

		PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false);

		if (!nsdDefined)
			defaultPreferences.edit()
					.putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
					.apply();

		PreferenceUtils.syncDefaults(getApplicationContext());

		NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());
		int migratedVersion = defaultPreferences.getInt("migrated_version", localDevice.versionNumber);

		if (migratedVersion < localDevice.versionNumber) {
			// migrating to a new version

			if (migratedVersion <= 67)
				AppUtils.getViewingPreferences(getApplicationContext()).edit()
						.clear()
						.apply();

			defaultPreferences.edit()
					.putInt("migrated_version", localDevice.versionNumber)
					.apply();
		}
	}
}
