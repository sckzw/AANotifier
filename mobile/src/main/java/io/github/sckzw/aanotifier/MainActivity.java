package io.github.sckzw.aanotifier;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        /*
        final PackageManager mPackageManager = getApplicationContext().getPackageManager();
        List< ApplicationInfo > appInfoList = mPackageManager.getInstalledApplications( PackageManager.GET_META_DATA );
        for ( ApplicationInfo appInfo : appInfoList ) {
            Bundle bundle = appInfo.metaData;
            if ( bundle != null && bundle.containsKey( "com.google.android.gms.car.application" ) ) {
                Log.i( "MainActivity", mPackageManager.getApplicationLabel( appInfo ).toString() + ", " + appInfo.packageName );
            }
        }
        */

        findViewById( R.id.button_notification_access ).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                Intent intent = new Intent();
                intent.setAction( Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS );
                startActivity( intent );
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
