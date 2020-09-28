package io.github.sckzw.aanotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import net.grandcentrix.tray.AppPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MessagingService extends NotificationListenerService {
    public static final String INTENT_ACTION_SET_PREF = "io.github.sckzw.aanotifier.INTENT_ACTION_SET_PREF";
    public static final String INTENT_ACTION_READ_MESSAGE = "io.github.sckzw.aanotifier.INTENT_ACTION_READ_MESSAGE";
    public static final String INTENT_ACTION_REPLY_MESSAGE = "io.github.sckzw.aanotifier.INTENT_ACTION_REPLY_MESSAGE";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    private static final String AANOTIFIER_PACKAGE_NAME = "io.github.sckzw.aanotifier";
    private static final String TAG = MessagingService.class.getSimpleName();

    private PreferenceBroadcastReceiver mPreferenceBroadcastReceiver;
    private NotificationManagerCompat mNotificationManager;
    private UiModeManager mUiModeManager;

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

        mUiModeManager = (UiModeManager)context.getSystemService( Context.UI_MODE_SERVICE );

        AppPreferences appPreferences = new AppPreferences( context );

        mAndroidAutoNotification  = appPreferences.getBoolean( "android_auto_notification" , true  );
        mCarModeNotification      = appPreferences.getBoolean( "car_mode_notification"     , true  );
        mCarExtenderNotification  = appPreferences.getBoolean( "car_extender_notification" , false );
        mMediaSessionNotification = appPreferences.getBoolean( "media_session_notification", false );
        mOngoingNotification      = appPreferences.getBoolean( "ongoing_notification"      , false );
        mSpuriousNotification     = appPreferences.getBoolean( "spurious_notification"     , false );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mPreferenceBroadcastReceiver != null ) {
            LocalBroadcastManager.getInstance( getApplicationContext() ).unregisterReceiver( mPreferenceBroadcastReceiver );
        }
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

        if ( mCarModeNotification && mUiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR ) {
            return;
        }

        if ( !mOngoingNotification && sbn.isOngoing() ) {
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

    @Override
    public void onNotificationRemoved( StatusBarNotification sbn ) {
        super.onNotificationRemoved( sbn );

        String packageName = sbn.getPackageName();
        Log.d( TAG, "onNotificationRemoved: " + packageName );

        mNotificationManager.cancel( sbn.getKey(), 0 );
    }

    private void sendNotification( StatusBarNotification sbn ) {
        Context appContext = getApplicationContext();
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        long timeStamp = sbn.getPostTime();
        int conversationId = 0;
        CharSequence charSequence;

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

        if ( !text.startsWith( appName ) ) {
            text = appName + ": " + text;
        }
        if ( !title.startsWith( appName ) ) {
            title = appName + ": " + title;
        }

        PendingIntent readPendingIntent = PendingIntent.getBroadcast(
                appContext,
                conversationId,
                new Intent( appContext, MessageReadReceiver.class )
                        .setAction( INTENT_ACTION_READ_MESSAGE )
                        .putExtra( CONVERSATION_ID, conversationId )
                        .addFlags( Intent.FLAG_INCLUDE_STOPPED_PACKAGES ),
                PendingIntent.FLAG_UPDATE_CURRENT );

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                appContext,
                conversationId,
                new Intent( appContext, MessageReplyReceiver.class )
                        .setAction( INTENT_ACTION_REPLY_MESSAGE )
                        .putExtra( CONVERSATION_ID, conversationId )
                        .addFlags( Intent.FLAG_INCLUDE_STOPPED_PACKAGES ),
                PendingIntent.FLAG_UPDATE_CURRENT );

        RemoteInput remoteInput = new RemoteInput.Builder( EXTRA_VOICE_REPLY ).build();

        NotificationCompat.CarExtender.UnreadConversation.Builder unreadConversationBuilder =
                new NotificationCompat.CarExtender.UnreadConversation.Builder( title )
                        .setLatestTimestamp( timeStamp )
                        .setReadPendingIntent( readPendingIntent )
                        .setReplyAction( replyPendingIntent, remoteInput );
        unreadConversationBuilder.addMessage( text );

        NotificationCompat.Builder builder = new NotificationCompat.Builder( appContext )
                .setChannelId( AANOTIFIER_PACKAGE_NAME )
                .setSmallIcon( R.mipmap.ic_launcher )
                .setLargeIcon( (Bitmap)extras.get( Notification.EXTRA_LARGE_ICON ) )
                .setContentTitle( title )
                .setContentText( text )
                .setWhen( timeStamp )
                .setContentIntent( readPendingIntent )
                .extend( new NotificationCompat.CarExtender()
                        .setUnreadConversation( unreadConversationBuilder.build() ) );

        mNotificationManager.notify( sbn.getKey(), conversationId, builder.build() );

        if ( !mSpuriousNotification ) {
            final String key = sbn.getKey();
            new Handler().postDelayed( new Runnable() {
                @Override
                public void run() {
                    mNotificationManager.cancel( key, 0 );
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
