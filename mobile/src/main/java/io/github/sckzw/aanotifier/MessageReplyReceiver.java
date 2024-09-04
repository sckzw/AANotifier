package io.github.sckzw.aanotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;

public class MessageReplyReceiver extends BroadcastReceiver {
    private static final String TAG = MessageReplyReceiver.class.getSimpleName();

    @Override
    public void onReceive( Context context, Intent intent ) {
        if ( MessagingService.INTENT_ACTION_REPLY_MESSAGE.equals( intent.getAction() ) ) {
            int conversationId = intent.getIntExtra( MessagingService.CONVERSATION_ID, -1 );
            if ( conversationId != -1 ) {
                Log.d( TAG, "Conversation " + conversationId + " reply action." );
            }
        }
    }
}
