package com.mirror.nfc.nfcsmartmirror_app_v3;

        import java.io.BufferedReader;
        import java.io.ByteArrayInputStream;
        import java.io.File;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.OutputStream;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;

//import java.nio.file.Path;
//import java.nio.file.Paths;

//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;

/**
 * Handles the upload of static resources to a mirror. These resources are usually website files to display the views on the mirror (html, css, js, images).
 * After upload these files can be requested on the mirror's webserver. The path under which a file is available is on the webserver is returned after uploading
 * a file. This might be used for example if a manual registration of views is neccessary or preferred.
 *
 * @author Hendrik Motza
 * @since 17.01
 */
public class StaticResourceUploader {

    /**
     * Retrieves the InputStream when needed. The calling code is responsible to handle reading/closing the stream when opened.
     *
     * @author Hendrik Motza
     */
    public interface InputStreamSupplier {

        /**
         * Returns an input stream to read from or throws an error if InputStream can't be opened.
         *
         * @return InputStream to read from, the caller of this method should take care of closing the stream.
         * @throws IOException Thrown if InputStream can no longer be opened.
         */
        InputStream get()
                throws IOException;

    }

    public static class ResourceRegistrationConfig {


        private final String viewId;


        private final boolean mainPage;


        private final boolean icon;

        public ResourceRegistrationConfig( final String appViewId, final boolean isMainPage, final boolean isIcon) {
            if (appViewId == null) {
                throw new IllegalArgumentException("Parameter 'appViewId' mustn't be null!");
            }
            this.viewId = appViewId;
            this.mainPage = isMainPage;
            this.icon = isIcon;
        }


        private String getViewId() {
            return this.viewId;
        }

        private boolean isIcon() {
            return this.icon;
        }

        private boolean isMainPage() {
            return this.mainPage;
        }
    }

    private interface MainPageUploader {

        void upload()
                throws IOException;
    }

    /** URI scheme identifier for JAR files */
    private static final String SCHEME_JAR = "jar";
    /** JAR scheme with ":" */
    private static final String SCHEME_JAR_PREFIX = SCHEME_JAR + ":";
    /**
     * indicator part is in a URI when it points to a file in a packaged file (i.e. zip or jar)
     */
    private static final String JAR_PACKAGE_INDICATOR = "!/";

    /** URI scheme identifier for files in/on filesystems */
    private static final String SCHEME_FILE = "file";
    /** JAR scheme with ":" */
    private static final String SCHEME_FILE_PREFIX = SCHEME_FILE + ":";


    private final ClassLoader classLoader;


    private final URLConnectionFactory.Builder builder = new URLConnectionFactory.Builder();

    /**
     * Creates an instance to upload static resources related to a specified mirror.
     *
     * @param mirrorApiUrl api url of the mirror to which the resources should be uploaded
     * @param appId identifier of the app
     * @param userId identifier of the user
     * @throws MalformedURLException Thrown if mirrorApiUrl is malformed.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     */
    public StaticResourceUploader( final String mirrorApiUrl,  final String appId, final String userId)
            throws MalformedURLException {
        this(mirrorApiUrl, appId, userId, StaticResourceUploader.class.getClassLoader());
    }

    /**
     * Creates an instance to upload static resources related to a specified mirror.
     *
     * @param mirrorApiUrl api url of the mirror to which the resources should be uploaded
     * @param appId identifier of the app
     * @param userId identifier of the user
     * @param classLoader classLoader to be used to load java resource files
     * @throws MalformedURLException Thrown if mirrorApiUrl is malformed.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     */
    public StaticResourceUploader( final String mirrorApiUrl, final String appId,  final String userId,
                                   final ClassLoader classLoader)
            throws MalformedURLException {
        if (mirrorApiUrl == null) {
            throw new IllegalArgumentException("'mirrorApiUrl' mustn't be null!");
        }
        if (appId == null) {
            throw new IllegalArgumentException("'appId' mustn't be null!");
        }
        if (userId == null) {
            throw new IllegalArgumentException("'userId' mustn't be null!");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("'classLoader' mustn't be null!");
        }
        this.builder.setAppId(appId);
        this.builder.setMirrorApiUrl(mirrorApiUrl);
        this.builder.setUserId(userId);
        this.classLoader = classLoader;
    }

    /**
     * Uploads the provided binary stream to the mirror.
     *
     * @param inputStreamSupplier Supplies the stream of binary data to upload.
     * @param connectionFactory Provides the configured connection to upload the file.
     * @return Path on the mirror's webserver under which the resource is available.
     * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     */
    public static String upload( final InputStreamSupplier inputStreamSupplier,  final UploadConnectionFactory connectionFactory)
            throws IOException {
        if (inputStreamSupplier == null) {
            throw new IllegalArgumentException("'inputStreamSupplier' mustn't be null!");
        }
        if (connectionFactory == null) {
            throw new IllegalArgumentException("'connectionFactory' mustn't be null!");
        }
        int read = 0;
        final byte[] bytes = new byte[1024];

        final HttpURLConnection connection = connectionFactory.create();
        try (final InputStream inputStream = inputStreamSupplier.get()) {
            try (final OutputStream os = connection.getOutputStream()) {
                while ((read = inputStream.read(bytes)) != -1) {
                    os.write(bytes, 0, read);
                }
            }
            try (final InputStream responseStream = connection.getInputStream()) {
                final BufferedReader in = new BufferedReader(new InputStreamReader(responseStream));
                return in.readLine();
            }
        }
        finally {
            connection.disconnect();
        }
    }

    /**
     * Uploads the provided binary data as file to the mirror.
     *
     * @param bytes Binary data of the resource to upload.
     * @param urlBasePath Target path on the webserver relative to the app's base directory.
     * @return Path on the mirror's webserver under which the resource is available.
     * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     */

    public String uploadResource( final byte[] bytes,  final String urlBasePath)
            throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("'bytes' mustn't be null!");
        }
        if (urlBasePath == null) {
            throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
        }
        return uploadResource(() -> new ByteArrayInputStream(bytes), urlBasePath);
    }

    /**
     * Uploads the provided binary data as file to the mirror.
     *
     * @param file to upload.
     * @param urlBasePath Target path on the webserver relative to the app's base directory.
     * @return Path on the mirror's webserver under which the resource is available.
     * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     * @throws MalformedURLException Thrown if file path can't be transformed into an url.
     */

    public String uploadResource( final File file,  final String urlBasePath)
            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("'file' mustn't be null!");
        }
        if (urlBasePath == null) {
            throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
        }
        return uploadResource(() -> file.toURI().toURL().openStream(), urlBasePath);
    }

    /**
     * Uploads the provided binary data as file to the mirror.
     *
     * @param inputStreamSupplier Provides the input stream to upload.
     * @param urlBasePath Target path on the webserver relative to the app's base directory.
     * @return Path on the mirror's webserver under which the resource is available.
     * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     */

    public String uploadResource( final InputStreamSupplier inputStreamSupplier,  final String urlBasePath)
            throws IOException {
        if (inputStreamSupplier == null) {
            throw new IllegalArgumentException("'inputStreamSupplier' mustn't be null!");
        }
        if (urlBasePath == null) {
            throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
        }
        return this.uploadResource(inputStreamSupplier, urlBasePath, null);
    }

    /**
     * Uploads the provided binary data as file to the mirror.
     *
     * @param inputStreamSupplier Provides the input stream to upload.
     * @param urlBasePath Target path on the webserver relative to the app's base directory.
     * @param registrationConfig configuration in case that this resource is the mainpage or the icon of a view.
     * @return Path on the mirror's webserver under which the resource is available.
     * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
     * @throws IllegalArgumentException Thrown if inputStreamSupplier or urlBasePath is {@code null}.
     */

    public synchronized String uploadResource( final InputStreamSupplier inputStreamSupplier, final String urlBasePath,
                                               final ResourceRegistrationConfig registrationConfig)
            throws IOException {
        if (inputStreamSupplier == null) {
            throw new IllegalArgumentException("'inputStreamSupplier' mustn't be null!");
        }
        if (urlBasePath == null) {
            throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
        }
        if (registrationConfig != null) {
            this.builder.setAppViewId(registrationConfig.getViewId());
            this.builder.setIcon(registrationConfig.isIcon());
            this.builder.setMainPage(registrationConfig.isMainPage());
        }
        else {
            this.builder.setAppViewId(null);
            this.builder.setIcon(false);
            this.builder.setMainPage(false);
        }
        this.builder.setRelativeUrlFilePath(urlBasePath);
        try {
            return upload(inputStreamSupplier, this.builder.build());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    /**
     * Uploads the provided resource file to the mirror.
     *
     * @param resource Path of the resource that will be loaded by the classloader provided via
     *            {@link #StaticResourceUploader(String, String, String, ClassLoader)}.
     * @param urlBasePath Target path on the webserver relative to the app's base directory.
     * @return Path on the mirror's webserver under which the resource is available.
     * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
     * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
     */

    public String uploadResource( final String resource,  final String urlBasePath)
            throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("'resource' mustn't be null!");
        }
        if (urlBasePath == null) {
            throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
        }
        return uploadResource(() -> this.classLoader.getResourceAsStream(resource), urlBasePath);
    }



}

