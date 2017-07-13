package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * retrieves incoming SMS. These are logged together with the phone number of the message's sender.
 * The app then shows a toast alert (mainly for testing reasons) and processes the information in an html file.
 */
public class SMSListener extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {

        //map of all extras previously added with putExtra(), or null if none have been added.
        final Bundle bundle = intent.getExtras();

        try {

            if (bundle != null) {

                //longer SMS are split into more than one protocol data units (PDUs)
                //therefore store them in an array
                final Object[] pduArray = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pduArray.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pduArray[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReceiver", "phoneNumber: " + phoneNumber + "; message: " + message);


                    //display toast in app
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context,
                            "phone number: "+ phoneNumber + ", message: " + message, duration);
                    toast.show();
                    Intent msgIntent = new Intent("Msg");
                    //add additional info to toast
                    msgIntent.putExtra("package", "");
                    msgIntent.putExtra("ticker", phoneNumber);
                    msgIntent.putExtra("title", phoneNumber);
                    msgIntent.putExtra("text", message);


                    LocalBroadcastManager.getInstance(context).sendBroadcast(msgIntent);




                    //Write SMS html file here
                    //File htmlTemplateFile = new File("../res/templates/templateSMS.html");
                    //htmlTemplateFile.createNewFile();
                    File htmlTemplateFile = new File(context.getFilesDir(), "smsTest.html");
                    htmlTemplateFile.createNewFile();
                    String htmlString = FileUtils.readFileToString(htmlTemplateFile);
                    String number = phoneNumber;
                    String SMSmessage = message;
                    htmlString = htmlString.replace("$Number", number);
                    htmlString = htmlString.replace("$Message", SMSmessage);
                    File SMSfile = new File("data/data/SMSMessageChanged.html");

                    FileUtils.writeStringToFile(htmlTemplateFile, htmlString);


                    FileOutputStream fOut = context.openFileOutput("smsTest.html",Context.MODE_WORLD_READABLE);
                    OutputStreamWriter osw = new OutputStreamWriter(fOut);

                    // Write the string to the file
                    osw.write(message);

       /* ensure that everything is
        * really written out and close */
                    osw.flush();
                    osw.close();

                    FileInputStream fIn = context.openFileInput("smsTest.html");
                    InputStreamReader isr = new InputStreamReader(fIn);

        /* Prepare a char-Array that will
         * hold the chars we read back in. */
                  /*  char[] inputBuffer = new char[message.length()];

                    // Fill the Buffer with data from the file
                    isr.read(inputBuffer);

                    // Transform the chars to a String
                    String readString = new String(inputBuffer);

                    // Check if we read back the same chars that we had written out
                    boolean isTheSame = message.equals(readString);

                    Log.i("File Reading stuff", "success = " + isTheSame);
                    */

                } // end for loop
            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);

        }
    }
}