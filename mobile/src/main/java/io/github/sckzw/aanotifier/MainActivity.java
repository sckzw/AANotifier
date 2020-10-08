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

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

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
