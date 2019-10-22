package io.github.sckzw.aanotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MessageReadReceiver extends BroadcastReceiver {
    private static final String TAG = MessageReadReceiver.class.getSimpleName();

    @Override
    public void onReceive( Context context, Intent intent ) {
        if ( MessagingService.READ_ACTION.equals( intent.getAction() ) ) {
            int conversationId = intent.getIntExtra( MessagingService.CONVERSATION_ID, -1 );
            if ( conversationId != -1 ) {
                Log.d( TAG, "Conversation " + conversationId + " read action." );
            }
        }
    }
}
