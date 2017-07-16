package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.os.AsyncTask;

import java.io.IOException;

/**
 * Created by Julian on 15.07.2017.
 */

public class UploadBytesTask extends AsyncTask<Void, Void, String> {
    private byte[] bytes;
    private String urlBasePath;
    private final StaticResourceUploader resourceUploader;


    UploadBytesTask(final StaticResourceUploader uploader, final byte[] bytes,  final String urlBasePath) {
        this.bytes = bytes;
        this.urlBasePath = urlBasePath;
        this.resourceUploader = uploader;

    }

    @Override
    protected String doInBackground(Void... params){
        try {
            return this.resourceUploader.uploadResource(this.bytes, this.urlBasePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
