/* Copyright (C) 2017 IOLITE GmbH, All rights reserved.
 * Created:    10.01.2017
 * Created by: lehmann
 */

package com.mirror.nfc.nfcsmartmirror_app_v3;

import java.io.IOException;
import java.net.HttpURLConnection;


/**
 * Creates HTTP POST connections.
 *
 * @author Grzegorz Lehmann
 * @since 17.01
 */

public interface UploadConnectionFactory {

    /**
     * Opens a new HTTP POST connection.
     *
     * @return constructed connection for the HTTP POST request, never {@code null}
     * @throws IOException if the connection cannot be constructed for any reason
     */

    HttpURLConnection create()
            throws IOException;
}
