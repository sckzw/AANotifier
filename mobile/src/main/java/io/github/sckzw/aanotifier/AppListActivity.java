package io.github.sckzw.aanotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

public class AppListActivity extends AppCompatActivity {
    private static final String PREF_KEY_AVAILABLE_APP_LIST = "available_app_list";
    private final List< AppListItem > mAppList = new ArrayList<>();
    private final AppListAdapter mAppListAdapter = new AppListAdapter();
    private final HashMap< String, Boolean > mAvailableAppList = new HashMap<>();
    private PackageManager mPackageManager;
    private SharedPreferences mSharedPreferences;
    private ExecutorService mExecutorService;
    private boolean mAppListLoaded = false;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_app_list );

        ListView listView = findViewById( R.id.app_list_view );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView< ? > adapterView, View view, int i, long l ) {
                AppListItem appListItem = mAppList.get( i );
                appListItem.isAvailable = !appListItem.isAvailable;

                if ( appListItem.isAvailable ) {
                    mAvailableAppList.put( appListItem.pkgName, true );
                }
                else {
                    mAvailableAppList.remove( appListItem.pkgName );
                }

                mAppListAdapter.notifyDataSetChanged();
            }
        } );

        mPackageManager = getApplicationContext().getPackageManager();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        mExecutorService = Executors.newSingleThreadExecutor();
        mExecutorService.submit( new LoadAppListRunnable() );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutorService.shutdownNow();
    }

    @Override
    public void onPause() {
        super.onPause();

        if ( !mAppListLoaded ) {
            return;
        }

        String availableAppList = String.join( ";", mAvailableAppList.keySet() );

        mSharedPreferences.edit().putString( PREF_KEY_AVAILABLE_APP_LIST, availableAppList ).apply();

        Intent intent = new Intent( MessagingService.INTENT_ACTION_SET_PREF );
        intent.putExtra( "key", PREF_KEY_AVAILABLE_APP_LIST );
        intent.putExtra( "value", availableAppList );
        LocalBroadcastManager.getInstance( getApplicationContext() ).sendBroadcast( intent );
    }

    private static class AppListItem {
        String appName;
        String pkgName;
        Drawable appIcon;
        boolean isAvailable;

        AppListItem( String pkgName, String appName, Drawable appIcon, boolean isAvailable ) {
            this.pkgName = pkgName;
            this.appName = appName;
            this.appIcon = appIcon;
            this.isAvailable = isAvailable;
        }
    }

    private class AppListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mAppList.size();
        }

        @Override
        public Object getItem( int position ) {
            return mAppList.get( position );
        }

        @Override
        public long getItemId( int position ) {
            return position;
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            View listItemView = convertView;
            ImageView imageAppIcon;
            TextView textAppName;
            TextView textPkgName;
            SwitchCompat switchIsEnabled;

            if ( listItemView == null ) {
                listItemView = ( (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE ) ).inflate( R.layout.app_list_item, null );
            }

            AppListItem appListItem = (AppListItem)getItem( position );

            if ( appListItem != null ) {
                imageAppIcon    = listItemView.findViewById( R.id.image_app_icon );
                textAppName     = listItemView.findViewById( R.id.text_app_name );
                textPkgName     = listItemView.findViewById( R.id.text_pkg_name );
                switchIsEnabled = listItemView.findViewById( R.id.switch_is_enabled );

                if ( appListItem.appName == null ) {
                    try {
                        ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo( appListItem.pkgName, 0 );
                        appListItem.appName = mPackageManager.getApplicationLabel( applicationInfo ).toString();
                    } catch ( PackageManager.NameNotFoundException ex ) {
                        appListItem.appName = "";
                    }
                }

                if ( appListItem.appIcon == null ) {
                    try {
                        appListItem.appIcon = mPackageManager.getApplicationIcon( appListItem.pkgName );
                    } catch ( PackageManager.NameNotFoundException ex ) {
                        appListItem.appIcon = ResourcesCompat.getDrawable( getResources(), android.R.drawable.sym_def_app_icon, null );
                    }
                }

                textPkgName.setText( appListItem.pkgName );
                textAppName.setText( appListItem.appName );
                imageAppIcon.setImageDrawable( appListItem.appIcon );
                switchIsEnabled.setChecked( appListItem.isAvailable );
            }

            return listItemView;
        }
    }

    private class LoadAppListRunnable implements Runnable {
        @Override
        public void run() {
            List< ApplicationInfo > appInfoList = mPackageManager.getInstalledApplications( 0 );
            String availableAppList = ";" + mSharedPreferences.getString( PREF_KEY_AVAILABLE_APP_LIST, "" ) + ";";
            ProgressBar progressBar = findViewById( R.id.progress_bar );

            int appNum = appInfoList.size();
            int appCnt = 0;

            for ( ApplicationInfo appInfo: appInfoList ) {
                boolean isAvailable = availableAppList.contains( ";" + appInfo.packageName + ";" );

                mAppList.add( new AppListItem(
                        appInfo.packageName,
                        appInfo.loadLabel( mPackageManager ).toString(),
                        null, // appInfo.loadIcon( mPackageManager )
                        isAvailable
                ) );

                if ( isAvailable ) {
                    mAvailableAppList.put( appInfo.packageName, true );
                }

                progressBar.setProgress( 100 * ( ++appCnt ) / appNum );

                if ( Thread.currentThread().isInterrupted() ) {
                    return;
                }
            }

            mAppList.sort( new Comparator< AppListItem >() {
                @Override
                public int compare( AppListItem appListItem1, AppListItem appListItem2 ) {
                    if ( appListItem1.isAvailable == appListItem2.isAvailable ) {
                        return appListItem1.appName.compareTo( appListItem2.appName );
                    }
                    else {
                        return appListItem1.isAvailable ? -1 : 1;
                    }
                }
            } );

            mAppListLoaded = true;

            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    ProgressBar progressBar = findViewById( R.id.progress_bar );
                    progressBar.setVisibility( android.widget.ProgressBar.INVISIBLE );

                    ListView listView = findViewById( R.id.app_list_view );
                    listView.setAdapter( mAppListAdapter );
                }
            } );
        }
    }
}
