package io.github.sckzw.aanotifier;

import android.content.Context;
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
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class AppListActivity extends AppCompatActivity {
    private List< AppListItem > mAppList = new ArrayList< AppListItem >();
    SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_app_list );
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        new LoadAppListTask().execute();
    }

    private static class AppListItem {
        String appName;
        String pkgName;
        Drawable appIcon;
        boolean isEnabled;

        AppListItem( String appName, String pkgName ) {
            this.appName = appName;
            this.pkgName = pkgName;
        }

        AppListItem( String appName, String pkgName, Drawable appIcon ) {
            this.appName = appName;
            this.pkgName = pkgName;
            this.appIcon = appIcon;
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
                imageAppIcon = listItemView.findViewById( R.id.image_app_icon );
                textAppName = listItemView.findViewById( R.id.text_app_name );
                textPkgName = listItemView.findViewById( R.id.text_pkg_name );
                switchIsEnabled = listItemView.findViewById( ( R.id.switch_is_enabled ) );

                textAppName.setText( appListItem.appName );
                textPkgName.setText( appListItem.pkgName );
                //imageAppIcon.setImageDrawable( appListItem.appIcon );
                switchIsEnabled.setChecked( appListItem.isEnabled );
            }

            return listItemView;
        }
    }

    private class LoadAppListTask extends AsyncTask< Void, Void, Void > {
        ProgressBar mProgressBar;

        @Override
        protected Void doInBackground( Void... voids ) {
            PackageManager pm = getApplicationContext().getPackageManager();
            List< ApplicationInfo > appInfoList = pm.getInstalledApplications( 0 );

            mSharedPreferences.getString( "", "" );

            int appNum = appInfoList.size();
            int appCnt = 0;

            for ( ApplicationInfo appInfo : appInfoList ) {
                mAppList.add( new AppListItem(
                        appInfo.loadLabel( pm ).toString(),
                        appInfo.packageName/*,
                        appInfo.loadIcon( pm ) */
                ) );

                mProgressBar.setProgress( 100 * ( ++appCnt ) / appNum );
            }

            Collections.sort( mAppList, new Comparator< AppListItem >() {
                @Override
                public int compare( AppListItem appListItem1, AppListItem appListItem2 ) {
                    return appListItem1.appName.compareTo( appListItem2.appName );
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
                    appListItem.isEnabled = !appListItem.isEnabled;
                    adapter.notifyDataSetChanged();
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
