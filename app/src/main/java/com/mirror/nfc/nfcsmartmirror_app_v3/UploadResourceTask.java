package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.os.AsyncTask;

import java.io.IOException;

/**
 * Created by Julian on 15.07.2017.
 */

public class UploadResourceTask extends AsyncTask<Void, Void, String> {
    private final StaticResourceUploader resourceUploader;
    private String resource;
    private String urlBasePath;



    UploadResourceTask(final StaticResourceUploader uploader, final String resource, final String urlBasePath) {
        this.resourceUploader = uploader;
        this.resource = resource;
        this.urlBasePath = urlBasePath;

    }

    @Override
    protected String doInBackground(Void... params){
        try {
            return this.resourceUploader.uploadResource(this.resource, this.urlBasePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}


