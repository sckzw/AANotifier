package io.github.sckzw.aanotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        findViewById( R.id.button_notification_access ).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                Intent intent = new Intent();
                intent.setAction( Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS );
                startActivity( intent );
            }
        } );
        ((Switch)findViewById( R.id.switch_disable_ongoing_notification )).setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged( CompoundButton compoundButton, boolean b ) {
                Intent intent = new Intent( MessagingService.INTENT_ACTION_SET_PREF );
                intent.putExtra( "key", "ongoingNotificationIsDisabled" );
                intent.putExtra( "value", b );
                LocalBroadcastManager.getInstance( getApplicationContext() ).sendBroadcast( intent );
            }
        } );
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
