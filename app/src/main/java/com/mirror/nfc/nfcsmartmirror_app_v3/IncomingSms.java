package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by mukesh on 19/5/15.
 */
public class IncomingSms extends BroadcastReceiver {
//testgithub
    // Get the object of SmsManager
    final SmsManager sms = SmsManager.getDefault();

    public void onReceive(Context context, Intent intent) {

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {

            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + message);


                    // Show Alert
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, "senderNum: "+ senderNum + ", message: " + message, duration);
                    toast.show();
                    Intent msgrcv = new Intent("Msg");
                    msgrcv.putExtra("package", "");
                    msgrcv.putExtra("ticker", senderNum);
                    msgrcv.putExtra("title", senderNum);
                    msgrcv.putExtra("text", message);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(msgrcv);

                    //Write SMS html file here
                    File htmlTemplateFile = new File("../res/templates/templateSMS.html");
                    htmlTemplateFile.createNewFile();
                    String htmlString = FileUtils.readFileToString(htmlTemplateFile);
                    String number = senderNum;
                    String SMSmessage = message;
                    htmlString = htmlString.replace("$Number", number);
                    htmlString = htmlString.replace("$Message", SMSmessage);
                    File SMSfile = new File("data/data/SMSMessageChanged.html");
                    FileUtils.writeStringToFile(SMSfile, htmlString);

                } // end for loop
            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);

        }
    }
}