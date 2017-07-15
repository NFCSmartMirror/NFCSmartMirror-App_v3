package com.mirror.nfc.nfcsmartmirror_app_v3;

/**
 * Created by Julian on 13.07.2017.
 */

/* Copyright (C) 2017 IOLITE GmbH, All rights reserved.
 * Created:    10.01.2017
 * Created by: lehmann
 */

        import android.annotation.TargetApi;
        import android.os.Build;

        import java.io.IOException;
        import java.io.UnsupportedEncodingException;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.net.URLEncoder;
        import java.nio.charset.StandardCharsets;
        import java.util.Optional;


/**
 * Provides HTTP POST upload connections based on Java's URL connections.
 *
 * @author Grzegorz Lehmann
 * @since 17.01
 */
public final class URLConnectionFactory implements UploadConnectionFactory {

    @TargetApi(Build.VERSION_CODES.N)
    public static class Builder {


        private Optional<String> mirrorApiUrl = Optional.empty();


        private Optional<String> userId = Optional.empty();


        private Optional<String> appId = Optional.empty();

        private boolean isMainPage = false;

        private boolean isIcon = false;

        private Optional<String> relativeUrlFilePath = Optional.empty();

        private Optional<String> appViewId = Optional.empty();


        public UploadConnectionFactory build() throws Throwable {
            return new URLConnectionFactory(this);
        }

        public final void setMirrorApiUrl( final String mirrorApiUrl) throws MalformedURLException {
            if (mirrorApiUrl == null) {
                throw new IllegalArgumentException("'mirrorApiUrl' must not be null");
            }
            new URL(mirrorApiUrl);
            this.mirrorApiUrl = Optional.of(mirrorApiUrl);
        }

        public final void setUserId( final String newUserId) {
            if (newUserId == null) {
                throw new IllegalArgumentException("'newUserId' must not be null");
            }
            try {
                this.userId = Optional.of(URLEncoder.encode(newUserId, StandardCharsets.UTF_8.name()));
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding not supported, failed to encode URL parameter value",
                        e);
            }
        }

        public final void setAppId( final String newAppId) {
            if (newAppId == null) {
                throw new IllegalArgumentException("'newAppId' must not be null");
            }
            try {
                this.appId = Optional.of(URLEncoder.encode(newAppId, StandardCharsets.UTF_8.name()));
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding not supported, failed to encode URL parameter value",
                        e);
            }
        }

        public final void setMainPage(final boolean isMainPage) {
            this.isMainPage = isMainPage;
        }

        public final void setIcon(final boolean isIcon) {
            this.isIcon = isIcon;
        }

        public final void setRelativeUrlFilePath( final String newFilePath) {
            if (newFilePath == null) {
                throw new IllegalArgumentException("'newFilePath' must not be null");
            }
            try {
                this.relativeUrlFilePath = Optional.of(URLEncoder.encode(newFilePath, StandardCharsets.UTF_8.name()));
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding not supported, failed to encode URL parameter value",
                        e);
            }
        }

        public final void setAppViewId( final String newAppViewId) {
            if (newAppViewId == null) {
                this.appViewId = Optional.of("");
                return;
            }
            try {
                this.appViewId = Optional.of(URLEncoder.encode(newAppViewId, StandardCharsets.UTF_8.name()));
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding not supported, failed to encode URL parameter value",
                        e);
            }
        }
    }


    private final URL url;

    private URLConnectionFactory( final Builder builder) throws Throwable {
        final String mirrorApiUrl = builder.mirrorApiUrl
                .orElseThrow(() -> new IllegalStateException("mirror API URL not set"));
        final String appId = builder.appId.orElseThrow(() -> new IllegalStateException("App ID not set"));
        final String appViewId = builder.appViewId.orElse("");
        final String userId = builder.userId.orElseThrow(() -> new IllegalStateException("User Id not set"));
        final String relativeUrlFilePath = builder.relativeUrlFilePath
                .orElseThrow(() -> new IllegalStateException("File path not set"));

        final String urlString = String.format(
                "%s/staticResourceUpload?appId=%s&appViewId=%s&userId=%s&filePath=%s&mainPage=%b&icon=%b", mirrorApiUrl,
                appId, appViewId, userId, relativeUrlFilePath, builder.isMainPage, builder.isIcon);

        try {
            this.url = new URL(urlString);
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(String.format("Failed to construct mirror upload URL from '%s'", urlString),
                    e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override

    public HttpURLConnection create() throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setRequestMethod("POST");
        return connection;
    }
}

