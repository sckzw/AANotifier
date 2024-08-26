package io.github.sckzw.aanotifier;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
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

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ) {
            if ( ActivityCompat.checkSelfPermission( this, Manifest.permission.POST_NOTIFICATIONS ) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions( this, new String[]{ Manifest.permission.POST_NOTIFICATIONS },0 );
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
            setPreferencesFromResource( R.xml.root_preferences, rootKey );
        }

        @Override
        public boolean onPreferenceTreeClick( @NonNull Preference preference ) {
            if ( preference.hasKey() && preference.getKey().equals( "notification_setting" ) ) {
                startActivity( new Intent( Settings.ACTION_APP_NOTIFICATION_SETTINGS )
                        .putExtra( Settings.EXTRA_APP_PACKAGE, "io.github.sckzw.aanotifier" ) );

                // startActivity( new Intent( Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS )
                //         .putExtra( Settings.EXTRA_APP_PACKAGE, "io.github.sckzw.aanotifier" )
                //         .putExtra( Settings.EXTRA_CHANNEL_ID, "AANotifier" ) );

                return true;
            }

            return super.onPreferenceTreeClick( preference );
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
