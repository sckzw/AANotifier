package io.github.sckzw.aanotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

public class AppListActivity extends AppCompatActivity {
    private static final String PREF_KEY_AVAILABLE_APP_LIST = "available_app_list";
    private final List< AppListItem > mAppList = new ArrayList<>();
    private final HashMap< Integer, String > mAvailableAppList = new HashMap<>();
    private PackageManager mPackageManager;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_app_list );

        mPackageManager = getApplicationContext().getPackageManager();;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        new LoadAppListTask().execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        String availableAppList = String.join( ";", mAvailableAppList.values() );

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
            Switch switchIsEnabled;

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

    private class LoadAppListTask extends AsyncTask< Void, Void, Void > {
        ProgressBar mProgressBar;

        @Override
        protected Void doInBackground( Void... voids ) {
            List< ApplicationInfo > appInfoList = mPackageManager.getInstalledApplications( 0 );
            String availableAppList = mSharedPreferences.getString( PREF_KEY_AVAILABLE_APP_LIST, "" );

            int appNum = appInfoList.size();
            int appCnt = 0;

            for ( ApplicationInfo appInfo : appInfoList ) {
                boolean isAvailable = availableAppList.contains( appInfo.packageName );

                mAppList.add( new AppListItem(
                        appInfo.packageName,
                        appInfo.loadLabel( mPackageManager ).toString(),
                        null, // appInfo.loadIcon( mPackageManager )
                        isAvailable
                ) );

                if ( isAvailable ) {
                    mAvailableAppList.put( appCnt, appInfo.packageName );
                }

                mProgressBar.setProgress( 100 * ( ++appCnt ) / appNum );
            }

            Collections.sort( mAppList, new Comparator< AppListItem >() {
                @Override
                public int compare( AppListItem appListItem1, AppListItem appListItem2 ) {
                    if ( appListItem1.isAvailable == appListItem2.isAvailable ) {
                        return appListItem1.appName.compareTo( appListItem2.appName );
                    }
                    else {
                        return appListItem1.isAvailable ? -1: 1;
                    }
                }
            } );

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar = findViewById( R.id.progress_bar );
        }

        @Override
        protected void onPostExecute( Void aVoid ) {
            super.onPostExecute( aVoid );

            final AppListAdapter adapter = new AppListAdapter();
            ListView listView = findViewById( R.id.app_list_view );
            listView.setAdapter( adapter );
            listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick( AdapterView< ? > adapterView, View view, int i, long l ) {
                    AppListItem appListItem = mAppList.get( i );
                    appListItem.isAvailable = !appListItem.isAvailable;
                    adapter.notifyDataSetChanged();

                    if ( appListItem.isAvailable ) {
                        mAvailableAppList.put( i, appListItem.pkgName );
                    }
                    else {
                        mAvailableAppList.remove( i );
                    }
                }
            } );

            mProgressBar.setVisibility( android.widget.ProgressBar.INVISIBLE );
        }

        @Override
        protected void onProgressUpdate( Void... values ) {
            super.onProgressUpdate( values );
        }
    }
}
