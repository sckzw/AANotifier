package io.github.sckzw.aanotifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
    private static final String ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead";
    private static final String TAG = MessagingService.class.getSimpleName();

    private MessagingServiceBroadcastReceiver mMessagingServiceBroadcastReceiver;
    private NotificationManagerCompat mNotificationManager;
    private boolean mCarMode;
    private boolean mOngoingIsDisabled;

    @Override
    public void onCreate() {
        mMessagingServiceBroadcastReceiver = new MessagingServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction( INTENT_ACTION_SET_PREF );
        LocalBroadcastManager.getInstance( getApplicationContext() ).registerReceiver( mMessagingServiceBroadcastReceiver, intentFilter );

        mNotificationManager = NotificationManagerCompat.from( getApplicationContext() );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            // NotificationManager notificationManager = getSystemService( NotificationManager.class );
            mNotificationManager.createNotificationChannel( new NotificationChannel(
                    AANOTIFIER_PACKAGE_NAME,
                    getString( R.string.app_name ),
                    NotificationManager.IMPORTANCE_MIN ) );
        }

        mCarMode = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mMessagingServiceBroadcastReceiver != null ) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver( mMessagingServiceBroadcastReceiver );
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

        if ( packageName.equals( ANDROID_AUTO_PACKAGE_NAME ) ) {
            mCarMode = true;
            Log.d( TAG, "Enter Car Mode." );
        }

        /*
        if ( ! mCarMode ) {
            return;
        }
        */

        if ( mOngoingIsDisabled && sbn.isOngoing() ) {
            return;
        }

        Notification notification = sbn.getNotification();

        if ( ( notification.flags & Notification.FLAG_GROUP_SUMMARY ) != 0 ) {
            return;
        }

        if ( notification.extras != null ) {
            if ( notification.extras.containsKey( "android.car.EXTENSIONS" ) ) {
                return;
            }
            if ( notification.extras.containsKey( "android.mediaSession" ) ) {
                return;
            }
        }

        /*
        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo( packageName, PackageManager.GET_META_DATA );

            if ( appInfo.metaData != null && appInfo.metaData.containsKey( "com.google.android.gms.car.application" ) ) {
                return;
            }
        }
        catch ( PackageManager.NameNotFoundException ex ) {
            Log.d( TAG, ex.getMessage() );
            return;
        }
        */

        sendNotification( sbn );
    }

    @Override
    public void onNotificationRemoved( StatusBarNotification sbn ) {
        super.onNotificationRemoved( sbn );

        String packageName = sbn.getPackageName();
        Log.d( TAG, "onNotificationRemoved: " + packageName );

        if ( packageName.equals( ANDROID_AUTO_PACKAGE_NAME ) ) {
            mCarMode = false;
            Log.d( TAG, "Exit Car Mode." );
        }

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

    private class MessagingServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive( Context context, Intent intent ) {
            if ( intent.getStringExtra( "key" ).equals( "ongoingNotificationIsDisabled" ) ) {
                mOngoingIsDisabled = intent.getBooleanExtra( "value", true );
            }
        }
    }
}
