package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Defining layout of the Mirror Connect App...
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class AppCompatPreferenceActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;
    //NFC
    private TextView mTextView;
    private NfcAdapter mNfcAdapter;
    //

    //Here starts a part of the NetworkServiceDiscovery

    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    //public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String SERVICE_TYPE = "_mirror._tcp.";

    public static final String TAG = "NsdService";
    public String SmartMirrorSR = "Smart Mirror";
    public String SmartMirrorHD = "SmartMirrorHD";
    public String mirrorIPRU;

    private NsdManager mNsdManager;


    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }


            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                //     mNsdManager.stopServiceDiscovery(this);
            }


            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Service Type: " + service.getServiceType());
                    mNsdManager.resolveService(service, initializeResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);



        //NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(mNfcAdapter == null){
            Toast.makeText(this, " this device doesnt support NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(!mNfcAdapter.isEnabled()){
            mTextView.setText ("NFC is disabled");
        }else{ // mTextView.setText (R.string.explanation);
        }
        handleIntent (getIntent());
    }


    private void handleIntent(Intent intent) {
       // Toast.makeText(this, "THIS IS WHERE THE MAGIC HAPPENS", Toast.LENGTH_SHORT).show();
        //Method for connecting the mirror
        //NetworkServiceDiscovery nsd = new NetworkServiceDiscovery(this);
        this.mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            String type = intent.getType();
            Toast.makeText(this, "jetzt startet der bums ", Toast.LENGTH_SHORT).show();
        }
    }

    public NsdManager.ResolveListener initializeResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                NsdServiceInfo mService = serviceInfo;
                InetAddress host = mService.getHost();
                String mirrorHostname = serviceInfo.getServiceName();
                String mirrorPort = String.valueOf(serviceInfo.getPort());
                String mirrorIP = host.getHostAddress();

                Log.i("NSD_Test_Port", mirrorIP);
                Log.i("NSD_Test", String.valueOf(serviceInfo));
                Log.i("NSD_InetName",serviceInfo.getServiceName());
                //String port = new String(mService.getAttributes().get("port"));
                Log.i("schreib",host.getAddress().toString());
                Log.i("Attributes", String.valueOf(serviceInfo.getAttributes().size()));
                //Log.i("Port", port);

                if (host == null){
                    Log.i("Host", "Host is null");
                    return;
                }
                //Creation of ipv6 address of mirror
                if (host instanceof Inet4Address){
                    String inetAdressv4Mirror = "http://" +mirrorIP+":"+mirrorPort +"/api";
                    Log.i("NSD_InetAddressV4Mirror",inetAdressv4Mirror);
                    mirrorIPRU = inetAdressv4Mirror;
                    Toast.makeText(getApplicationContext(),"Saved in String!",Toast.LENGTH_SHORT).show();

                }else{
                    //Creation of ipv6 address of mirror
                    String inetAdressv6Mirror = "http://["+mirrorIP +"]:"+mirrorPort +"/api";
                    Log.i("NSD_InetAddressV6Mirror", inetAdressv6Mirror);
                    Toast.makeText(getApplicationContext(),"NSD next try please!",Toast.LENGTH_SHORT).show();
                }
                Log.i("Host", host.getHostAddress());
            }
        };
    }



    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}
