package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * retrieves incoming SMS. These are logged together with the phone number of the message's sender.
 * The app then shows a toast alert (mainly for testing reasons) and processes the information in an html file.
 */
public class SMSListener extends BroadcastReceiver {

    private static final String RES_BASE = "app/de/iolite/insys/mirror/";
    private static final String RES_QUOTES = RES_BASE + "quotes.csv";
    private static final String HTML_RESOURCES = RES_BASE + "html/";
    private static final String VIEW_RESOURCES = RES_BASE + "views/";
    //private static final String RESOURCE1 = getResources().openRawResource(quote.png);
    private static final String RESOURCE1 = VIEW_RESOURCES + "quote.png";

    //Besandteile des "Pachages"  für den Upload definieren
    // MirrorExampleApp Zeile 174
    final String VIEW_ID_QUOTE = "QuoteView";
    final String ICON_RESPATH_QUOTE =  "app\\src\\main\\resources\\de\\iolite\\insys\\mirror\\views\\quote.png";
    final String VIEW_WEBPATH_QUOTE = "htmlString";
    // View testen


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
                            "phone number: " + phoneNumber + ", message: " + message, duration);
                    toast.show();
                    Intent msgIntent = new Intent("Msg");
                    //add additional info to toast
                    msgIntent.putExtra("package", "");
                    msgIntent.putExtra("ticker", phoneNumber);
                    msgIntent.putExtra("title", phoneNumber);
                    msgIntent.putExtra("text", message);


                    LocalBroadcastManager.getInstance(context).sendBroadcast(msgIntent);

                    //////////////////////////////////////////////////
                    // Ab hier neuer HTML erstellugscode
                    //////////////////////////////////////////////////


                    FileInputStream fIn = context.openFileInput("smsTest.html");
                    InputStreamReader isr = new InputStreamReader(fIn);

                    String htmlString = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
                            "\"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                            "\n" +
                            "<html>\n" +
                            "\n" +
                            "<head>\n" +
                            "\t<meta charset=\"utf-8\">\n" +
                            "\t<!-- Bootstrap Core CSS -->\n" +
                            " <link rel=\"stylesheet\" href=\"css/bootstrap.min.css\">\n" +
                            " <link rel=\"stylesheet\" type=\"text/css\" href=\"css/style.css\">\n" +
                            "</head>\n" +
                            "\n" +
                            "<body>\n" +
                            "\n" +
                            " <div id=\"content\" class=\"centered-text\">\n" +
                            "\t\t<div class=\"quote\">\n" +
                            "\t\t\t<blockquote class=\"quote-size\">\n" +
                            "\t\t\t\t<p>" + message + "</p>\n" +
                            "\t\t\t\t<footer><cite title=\"Source Title\">" + phoneNumber + "</cite></footer>\n" +
                            "\t\t\t</blockquote>\n" +
                            "\t\t</div>\n" +
                            "\t</div>\n" +
                            "\n" +
                            "\n" +
                            " <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>\n" +
                            " <!-- Bootstrap Core JavaScript -->\n" +
                            " <script src=\"js/bootstrap.min.js\"></script>\n" +
                            " <!-- Custom JavaScript -->\n" +
                            " <script src=\"js/alignment.js\"></script>\n" +
                            "</body>\n" +
                            "\n" +
                            "</html>\n";

                    Log.i("HTML", htmlString);
                    this.mirrors.put("DUMMY", "http://192.168.1.178:2534/api");

                    //this.mirrors.put("DUMMY", "http://10.0.2.2:2534/api");
                    //Kontrolle, ob Methode ausgeführt wird
                    Log.i("ASP", "hallo wird ausgeführt");
                    try {
                        //neue Instanz des StaticResourceUploaders ausführen
                        this.staticResourceUploader = new StaticResourceUploader(mirrors.get("DUMMY"), "Messages", "ASP");
                        InputStream inputStream = context.getResources().openRawResource(R.raw.quote);
                        if(inputStream == null) {
                            Log.i("not fund","not fund");
                        } else {Log.i("fund","fghj");}
                        //new UploadResourceTask(this.staticResourceUploader, RESOURCE1, "quote.png").execute();
                        //this.staticResourceUploader.uploadResource(RESOURCE1, "quote.png");
                        new UploadBytesTask(this.staticResourceUploader, htmlString.getBytes(), "test.txt").execute();
                        //this.staticResourceUploader.uploadResource("MyStringContent".getBytes(), "test.txt");
                    } catch (MalformedURLException e) {
                        this.staticResourceUploader = null;
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
            Log.e("SmsReceiver", "Exception smsReceiver" + e);
        }
    }


    //
    private final Map<String, String> mirrors = new HashMap<>();
    private StaticResourceUploader staticResourceUploader;

///////////////////////////////////////////////////////////////////////////////////////
// Upload an den Spiegel
///////////////////////////////////////////////////////////////////////////////////////

    /*
    public void addView (final String viewId, final String mainPageResource, final String iconResource) {
        if (viewId == null) {
                throw new IllegalArgumentException("Parameter 'viewId' mustn't be null!");
            }
        if (mainPageResource != null) {
                this.mainPages.put(mainPageResource, viewId);
            }
        if (iconResource != null) {
                this.icons.put(iconResource, viewId);
        }
    }
    */


    // MirrorExampleApp Zeile 309
    //addView(VIEW_ID_QUOTE, null, ICON_RESPATH_QUOTE);
    // MirrorExampleApp Zeile 310
    //this.viewRegistrator = new ViewRegistrator(staticResourceConfig, APP_ID, userId);

    final String appID = null;




///////////////////////////////////////////////////////////////////////////////////////
// IP-Adresse im Netzwerk finden via mDNS
///////////////////////////////////////////////////////////////////////////////////////




}