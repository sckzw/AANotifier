package io.github.sckzw.aanotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import net.grandcentrix.tray.AppPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    SharedPreferences mSharedPreferences;
    AppPreferences mAppPreferences;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        mAppPreferences = new AppPreferences( getApplicationContext() );

        mSharedPreferences.edit().putBoolean( "android_auto_notification" , mAppPreferences.getBoolean( "android_auto_notification" , true  ) ).apply();
        mSharedPreferences.edit().putBoolean( "car_mode_notification"     , mAppPreferences.getBoolean( "car_mode_notification"     , true  ) ).apply();
        mSharedPreferences.edit().putBoolean( "car_extender_notification" , mAppPreferences.getBoolean( "car_extender_notification" , false ) ).apply();
        mSharedPreferences.edit().putBoolean( "media_session_notification", mAppPreferences.getBoolean( "media_session_notification", false ) ).apply();
        mSharedPreferences.edit().putBoolean( "ongoing_notification"      , mAppPreferences.getBoolean( "ongoing_notification"      , false ) ).apply();
        mSharedPreferences.edit().putBoolean( "spurious_notification"     , mAppPreferences.getBoolean( "spurious_notification"     , false ) ).apply();

        setContentView( R.layout.activity_main );

        getSupportFragmentManager()
                .beginTransaction()
                .replace( R.id.layout_settings, new SettingsFragment() )
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
        }
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences pref, String key ) {
        boolean value = pref.getBoolean( key, false );

        mAppPreferences.put( key, value );

        Intent intent = new Intent( MessagingService.INTENT_ACTION_SET_PREF );
        intent.putExtra( "key", key );
        intent.putExtra( "value", value );
        LocalBroadcastManager.getInstance( getApplicationContext() ).sendBroadcast( intent );
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener( this );
    }

    @Override
    public void onPause() {
        super.onPause();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener( this );
    }
}
