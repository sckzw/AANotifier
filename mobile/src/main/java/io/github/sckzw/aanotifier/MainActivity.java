package io.github.sckzw.aanotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        getSupportFragmentManager()
                .beginTransaction()
                .replace( R.id.layout_settings, new SettingsFragment() )
                .commit();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences pref, String key ) {
        Intent intent = new Intent( MessagingService.INTENT_ACTION_SET_PREF );
        intent.putExtra( "key", key );
        intent.putExtra( "value", pref.getBoolean( key, false ) );
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

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
        }
    }

    private boolean isRunning() {
        ActivityManager manager = (ActivityManager)getSystemService( Context.ACTIVITY_SERVICE );

        for ( ActivityManager.RunningServiceInfo info : manager.getRunningServices( Integer.MAX_VALUE ) ) {
            if ( info.service.getClassName().equals( MessagingService.class.getName() ) ) {
                return true;
            }
        }

        return false;
    }
}
