package com.mirror.nfc.nfcsmartmirror_app_v3;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Julian on 15.07.2017.
 */

public class UploadResourceTask extends AsyncTask<Void, Void, String> {
    private final StaticResourceUploader resourceUploader;
    private StaticResourceUploader.InputStreamSupplier supplier;
    private String urlBasePath;



    UploadResourceTask(final StaticResourceUploader uploader, Context context, final int resource, final String urlBasePath) {
        this.resourceUploader = uploader;
        this.urlBasePath = urlBasePath;

        this.supplier = new StaticResourceUploader.InputStreamSupplier() {
            @Override
            public InputStream get() throws IOException {
                return context.getResources().openRawResource(resource);
            }
        };

    }

    @Override
    protected String doInBackground(Void... params){
        try {
            return this.resourceUploader.uploadResource(this.supplier, this.urlBasePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}


