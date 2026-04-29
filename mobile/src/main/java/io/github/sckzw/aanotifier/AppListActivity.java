package io.github.sckzw.aanotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

public class AppListActivity extends AppCompatActivity {
    private static final String PREF_KEY_AVAILABLE_APP_LIST = "available_app_list";
    private static final String PREF_KEY_PERMISSION_CHECK = "permission_check";
    private final List< AppListItem > mAppList = new ArrayList<>();
    private List< AppListItem > mFilterAppList = new ArrayList<>();
    private final AppListAdapter mAppListAdapter = new AppListAdapter();
    private final HashMap< String, Boolean > mAvailableAppList = new HashMap<>();
    private PackageManager mPackageManager;
    private SharedPreferences mSharedPreferences;
    private ExecutorService mExecutorService;
    private boolean mAppListLoaded = false;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        EdgeToEdge.enable( this );
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_app_list );

        ListView listView = findViewById( R.id.app_list_view );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView< ? > adapterView, View view, int i, long l ) {
                AppListItem appListItem = mFilterAppList.get( i );
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

        SearchView searchKeyword = findViewById( R.id.search_keyword );
        searchKeyword.setOnQueryTextListener( new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit( String query ) {
                return false;
            }

            @Override
            public boolean onQueryTextChange( String newText ) {
                mAppListAdapter.getFilter().filter( newText );
                return true;
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

    private class AppListAdapter extends BaseAdapter implements Filterable {
        private final AppListFilter appListFilter = new AppListFilter();

        @Override
        public int getCount() {
            return mFilterAppList.size();
        }

        @Override
        public Object getItem( int position ) {
            return mFilterAppList.get( position );
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

        @Override
        public Filter getFilter() {
            return appListFilter;
        }

        private class AppListFilter extends Filter {
            @Override
            protected FilterResults performFiltering( CharSequence charSequence ) {
                FilterResults filterResults = new FilterResults();

                if ( charSequence == null || charSequence.length() == 0 ) {
                    filterResults.values = mAppList;
                    filterResults.count = mAppList.size();
                }
                else {
                    String keyword = charSequence.toString().toLowerCase();
                    List< AppListItem > filterItems = new ArrayList<>();

                    for ( AppListItem item: mAppList ) {
                        if ( item.appName.toLowerCase().contains( keyword ) ) {
                            filterItems.add( item );
                        }
                    }

                    filterResults.values = filterItems;
                    filterResults.count  = filterItems.size();
                }

                return filterResults;
            }

            @Override
            protected void publishResults( CharSequence charSequence, FilterResults filterResults ) {
                mFilterAppList = (List< AppListItem >)filterResults.values;
                notifyDataSetChanged();
            }
        }
    }

    private class LoadAppListRunnable implements Runnable {
        @Override
        public void run() {
            List< ApplicationInfo > appInfoList = mPackageManager.getInstalledApplications( 0 );
            String availableAppList = ";" + mSharedPreferences.getString( PREF_KEY_AVAILABLE_APP_LIST, "" ) + ";";
            boolean permissionCheck = mSharedPreferences.getBoolean( PREF_KEY_PERMISSION_CHECK, false );
            ProgressBar progressBar = findViewById( R.id.progress_bar );

            int appNum = appInfoList.size();
            int appCnt = 0;

            for ( ApplicationInfo appInfo: appInfoList ) {
                progressBar.setProgress( 100 * ( ++appCnt ) / appNum );

                if ( permissionCheck ) {
                    PackageInfo packageInfo;
                    try {
                        packageInfo = mPackageManager.getPackageInfo( appInfo.packageName, PackageManager.GET_PERMISSIONS );
                    } catch ( PackageManager.NameNotFoundException ex ) {
                        continue;
                    }
                    if ( packageInfo.requestedPermissions == null ) {
                        continue;
                    }

                    boolean hasPermission = false;
                    for ( String permission: packageInfo.requestedPermissions ) {
                        if ( permission.equals( android.Manifest.permission.POST_NOTIFICATIONS ) ) {
                            hasPermission = true;
                            break;
                        }
                    }
                    if ( !hasPermission ) {
                        continue;
                    }
                }

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

            mFilterAppList = mAppList;
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
