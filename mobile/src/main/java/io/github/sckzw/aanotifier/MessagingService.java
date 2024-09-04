package io.github.sckzw.aanotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.car.app.connection.CarConnection;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

public class MessagingService extends NotificationListenerService {
    public static final String INTENT_ACTION_SET_PREF = "io.github.sckzw.aanotifier.INTENT_ACTION_SET_PREF";
    public static final String INTENT_ACTION_READ_MESSAGE = "io.github.sckzw.aanotifier.INTENT_ACTION_READ_MESSAGE";
    public static final String INTENT_ACTION_REPLY_MESSAGE = "io.github.sckzw.aanotifier.INTENT_ACTION_REPLY_MESSAGE";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    private static final String AANOTIFIER_PACKAGE_NAME = "io.github.sckzw.aanotifier";
    private static final String TAG = MessagingService.class.getSimpleName();

    private int mConversationId = 0;

    private PreferenceBroadcastReceiver mPreferenceBroadcastReceiver;
    private NotificationManagerCompat mNotificationManager;
    private LiveData<Integer> mConnectionTypeLiveData;
    private Observer<Integer> mConnectionTypeObserver;
    private Integer mConnectionType;

    private String  mAvailableAppList;
    private boolean mAndroidAutoNotification;
    private boolean mCarModeNotification;
    private boolean mCarExtenderNotification;
    private boolean mMediaSessionNotification;
    private boolean mOngoingNotification;
    private boolean mSpuriousNotification;

    @Override
    public void onCreate() {
        Context context = getApplicationContext();

        mPreferenceBroadcastReceiver = new PreferenceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction( INTENT_ACTION_SET_PREF );
        LocalBroadcastManager.getInstance( context ).registerReceiver( mPreferenceBroadcastReceiver, intentFilter );

        mNotificationManager = NotificationManagerCompat.from( context );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            mNotificationManager.createNotificationChannel( new NotificationChannel(
                    AANOTIFIER_PACKAGE_NAME,
                    getString( R.string.app_name ),
                    NotificationManager.IMPORTANCE_MIN ) );
        }

        mConnectionTypeObserver = newConnectionType -> mConnectionType = newConnectionType;
        mConnectionTypeLiveData = new CarConnection( context ).getType();
        mConnectionTypeLiveData.observeForever( mConnectionTypeObserver );

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        mAvailableAppList   = ";" + sharedPreferences.getString ( "available_app_list"        , ""    ) + ";";
        mAndroidAutoNotification  = sharedPreferences.getBoolean( "android_auto_notification" , true  );
        mCarModeNotification      = sharedPreferences.getBoolean( "car_mode_notification"     , true  );
        mCarExtenderNotification  = sharedPreferences.getBoolean( "car_extender_notification" , false );
        mMediaSessionNotification = sharedPreferences.getBoolean( "media_session_notification", false );
        mOngoingNotification      = sharedPreferences.getBoolean( "ongoing_notification"      , false );
        mSpuriousNotification     = sharedPreferences.getBoolean( "spurious_notification"     , false );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mPreferenceBroadcastReceiver != null ) {
            LocalBroadcastManager.getInstance( getApplicationContext() ).unregisterReceiver( mPreferenceBroadcastReceiver );
        }
        mConnectionTypeLiveData.removeObserver( mConnectionTypeObserver );
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        return START_STICKY;
    }

    @Override
    public void onNotificationPosted( StatusBarNotification sbn ) {
        super.onNotificationPosted( sbn );

        String packageName = sbn.getPackageName();
        Log.d( TAG, "onNotificationPosted: " + packageName );

        if ( packageName.equals( AANOTIFIER_PACKAGE_NAME ) ) {
            return;
        }

        if ( !mAndroidAutoNotification ) {
            return;
        }

        if ( mCarModeNotification && mConnectionType == CarConnection.CONNECTION_TYPE_NOT_CONNECTED ) {
            return;
        }

        if ( !mOngoingNotification && sbn.isOngoing() ) {
            return;
        }

        if ( !mAvailableAppList.contains( ";" + packageName + ";" ) ) {
            return;
        }

        Notification notification = sbn.getNotification();

        if ( ( notification.flags & Notification.FLAG_GROUP_SUMMARY ) != 0 ) {
            return;
        }

        if ( notification.extras != null ) {
            if ( !mCarExtenderNotification && notification.extras.containsKey( "android.car.EXTENSIONS" ) ) {
                return;
            }
            if ( !mMediaSessionNotification && notification.extras.containsKey( "android.mediaSession" ) ) {
                return;
            }
        }

        sendNotification( sbn );
    }

    private void sendNotification( StatusBarNotification sbn ) {
        Context appContext = getApplicationContext();
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        long timeStamp = sbn.getPostTime();
        CharSequence charSequence;
        mConversationId++;

        if ( ActivityCompat.checkSelfPermission( this, android.Manifest.permission.POST_NOTIFICATIONS ) != PackageManager.PERMISSION_GRANTED ) {
            return;
        }

        String title = "";

        if ( extras.containsKey( Notification.EXTRA_TITLE ) && ( charSequence = extras.getCharSequence( Notification.EXTRA_TITLE ) ) != null ) {
            title = charSequence.toString();
        } else if ( extras.containsKey( Notification.EXTRA_TITLE_BIG ) && ( charSequence = extras.getCharSequence( Notification.EXTRA_TITLE_BIG ) ) != null ) {
            title = charSequence.toString();
        }

        String text = "";

        if ( extras.containsKey( Notification.EXTRA_TEXT ) && ( charSequence = extras.getCharSequence( Notification.EXTRA_TEXT ) ) != null ) {
            text = charSequence.toString();
        } else if ( extras.containsKey( Notification.EXTRA_BIG_TEXT ) && ( charSequence = extras.getCharSequence( Notification.EXTRA_BIG_TEXT ) ) != null ) {
            text = charSequence.toString();
        } else if ( notification.tickerText != null ) {
            text = notification.tickerText.toString();
        }

        if ( !text.startsWith( title ) ) {
            text = title + ": " + text;
        }

        String appName = getApplicationName( sbn.getPackageName() );

        PendingIntent readPendingIntent = PendingIntent.getBroadcast(
                appContext,
                mConversationId,
                new Intent( appContext, MessageReadReceiver.class )
                        .setAction( INTENT_ACTION_READ_MESSAGE )
                        .putExtra( CONVERSATION_ID, mConversationId )
                        .addFlags( Intent.FLAG_INCLUDE_STOPPED_PACKAGES ),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                appContext,
                mConversationId,
                new Intent( appContext, MessageReplyReceiver.class )
                        .setAction( INTENT_ACTION_REPLY_MESSAGE )
                        .putExtra( CONVERSATION_ID, mConversationId )
                        .addFlags( Intent.FLAG_INCLUDE_STOPPED_PACKAGES ),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );

        RemoteInput remoteInput = new RemoteInput.Builder( EXTRA_VOICE_REPLY ).build();

        NotificationCompat.Action readAction = new NotificationCompat.Action.Builder( R.drawable.ic_launcher_foreground, getString( R.string.action_read_title ), readPendingIntent )
                .setSemanticAction( NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ )
                .setShowsUserInterface( false )
                .build();

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder( R.drawable.ic_launcher_foreground, getString( R.string.action_reply_title ), replyPendingIntent )
                .setSemanticAction( NotificationCompat.Action.SEMANTIC_ACTION_REPLY )
                .setShowsUserInterface( false )
                .addRemoteInput( remoteInput )
                .build();

        Person.Builder personBuilder = new Person.Builder()
                .setName( appName )
                .setKey( sbn.getKey() );

        Icon notificationIcon;
        if ( ( notificationIcon = notification.getLargeIcon() ) != null ) {
            personBuilder.setIcon( IconCompat.createFromIcon( appContext, notificationIcon ) );
        }
        else if ( ( notificationIcon = notification.getSmallIcon() ) != null ) {
            personBuilder.setIcon( IconCompat.createFromIcon( appContext, notificationIcon ) );
        }

        Person appPerson = personBuilder.build();

        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle( appPerson );
        messagingStyle.setConversationTitle( title );
        messagingStyle.setGroupConversation( true );
        messagingStyle.addMessage( text, timeStamp, appPerson );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder( appContext, AANOTIFIER_PACKAGE_NAME )
                .setSmallIcon( R.drawable.ic_notification )
                .setStyle( messagingStyle )
                .addInvisibleAction( replyAction )
                .addInvisibleAction( readAction );

        mNotificationManager.notify( sbn.getKey(), mConversationId, notificationBuilder.build() );

        if ( !mSpuriousNotification ) {
            final String key = sbn.getKey();
            final int conversationId = mConversationId;
            new Handler().postDelayed( new Runnable() {
                @Override
                public void run() {
                    mNotificationManager.cancel( key, conversationId );
                }
            }, 1000 );
        }
    }

    private String getApplicationName( String packageName ) {
        final PackageManager pm = getApplicationContext().getPackageManager();
        ApplicationInfo ai;

        try {
            ai = pm.getApplicationInfo( packageName, 0 );
        } catch ( PackageManager.NameNotFoundException ex ) {
            ai = null;
        }

        return (String)( ai != null ? pm.getApplicationLabel( ai ) : "" );
    }

    private class PreferenceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive( Context context, Intent intent ) {
            String key = intent.getStringExtra( "key" );

            if ( key == null ) {
                return;
            }

            switch ( key ) {
                case "available_app_list":
                    mAvailableAppList = ";" + intent.getStringExtra( "value" ) + ";";
                    break;
                case "android_auto_notification":
                    mAndroidAutoNotification = intent.getBooleanExtra( "value", true );
                    break;
                case "car_mode_notification":
                    mCarModeNotification = intent.getBooleanExtra( "value", true );
                    break;
                case "car_extender_notification":
                    mCarExtenderNotification = intent.getBooleanExtra( "value", false );
                    break;
                case "media_session_notification":
                    mMediaSessionNotification = intent.getBooleanExtra( "value", false );
                    break;
                case "ongoing_notification":
                    mOngoingNotification = intent.getBooleanExtra( "value", false );
                    break;
                case "spurious_notification":
                    mSpuriousNotification = intent.getBooleanExtra( "value", false );
                    break;
            }
        }
    }
}
