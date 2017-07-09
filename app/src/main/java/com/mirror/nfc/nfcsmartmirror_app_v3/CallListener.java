package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

/**
 * retrieves incoming calls and logs them together with the incoming phone number and call duration.
 */


public class CallListener extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        //create instance of TelephonyManager to retrieve calls in application context
        TelephonyManager callManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        callManager.listen(new PhoneStateListener() {
            @Override
            /**
             * when incoming call registered, show toast notification and create html file with the same contents
             */
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                System.out.println("incomingNumber : " + incomingNumber);
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context,"incomingNumber: "+ incomingNumber, duration );
                toast.show();

                Intent msgIntent = new Intent("Msg");
                msgIntent.putExtra("package", "");
                msgIntent.putExtra("ticker", incomingNumber);
                msgIntent.putExtra("title", incomingNumber);
                msgIntent.putExtra("text", "");

                LocalBroadcastManager.getInstance(context).sendBroadcast(msgIntent);
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }
}