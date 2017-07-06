package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by mukesh on 19/5/15.
 */
public class ServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                System.out.println("incomingNumber : " + incomingNumber);
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context,"incomingNumber: "+ incomingNumber, duration );
                toast.show();
                Intent msgrcv = new Intent("Msg");
                msgrcv.putExtra("package", "");
                msgrcv.putExtra("ticker", incomingNumber);
                msgrcv.putExtra("title", incomingNumber);
                msgrcv.putExtra("text", "");
                LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);


                //File htmlTemplateFile = new File("smstest.txt");
                // FileWriter fw = new FileWriter(htmlTemplateFile);
                // fw.write(message);
                // fw.flush();
                try {

                File htmlTemplateFile = new File("../res/templates/templateCall.html");
                htmlTemplateFile.createNewFile();
                String htmlString = FileUtils.readFileToString(htmlTemplateFile);
                String number = incomingNumber;
                    Log.i("Das ist die Nummer", number);
                String Callmessage = "Hey you just got a call";
                htmlString = htmlString.replace("$Number", number);
                htmlString = htmlString.replace("$Message", Callmessage);
                File Callfile = new File("data/data/CallChanged.html");
                FileUtils.writeStringToFile(Callfile, htmlString);

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }
}