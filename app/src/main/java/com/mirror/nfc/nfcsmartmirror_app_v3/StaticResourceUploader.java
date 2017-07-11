
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

	public static void main(final String[] args)
			throws IOException, URISyntaxException {
		final String resourcePackage = "de/iolite/insys/mirror/html/WeatherView/";

		final String mainPageResource = resourcePackage + "DummyWeather.html";
		final String iconResource = resourcePackage + "weather.jpg";
		final StaticResourceUploader sru = new StaticResourceUploader("http://localhost:2534/api", "appId", "userId");
		sru.uploadResourcesFromPackage(resourcePackage, "appViewId", mainPageResource, iconResource).forEach((k, v) -> System.out.println(k + " -> " + v));
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
	@Nullable
	public String uploadResource(@Nonnull final InputStreamSupplier inputStreamSupplier, @Nonnull final String urlBasePath)
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
	@Nonnull
	public synchronized String uploadResource(@Nonnull final InputStreamSupplier inputStreamSupplier, @Nonnull final String urlBasePath,
			@Nullable final ResourceRegistrationConfig registrationConfig)
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
		return upload(inputStreamSupplier, this.builder.build());
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
	@Nonnull
	public String uploadResource(@Nonnull final String resource, @Nonnull final String urlBasePath)
			throws IOException {
		if (resource == null) {
			throw new IllegalArgumentException("'resource' mustn't be null!");
		}
		if (urlBasePath == null) {
			throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
		}
		return uploadResource(() -> this.classLoader.getResourceAsStream(resource), urlBasePath);
	}

	/**
	 * Uploads the provided resource files at or below the given path to the mirror.
	 *
	 * @param resourcePackage Base path of the resources to upload
	 * @return Path on the mirror's webserver under which the resource is available.
	 * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
	 * @throws URISyntaxException Thrown if resourcePackage can't be transformed into URI.
	 * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
	 */
	@Nonnull
	public Map<String, String> uploadResourcesFromPackage(@Nonnull final String resourcePackage)
			throws IOException, URISyntaxException {
		if (resourcePackage == null) {
			throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
		}
		return uploadResourcesFromPackage(resourcePackage, "");
	}

	/**
	 * Uploads the provided resource files at or below the given path to the mirror. Main pages or representing icons of views can be defined by using the
	 * resource path of the corresponding resource as keys and the view id's as values. If the defined resource paths for a view is not within the given
	 * resource package path, the definition of the main page or icon has no effect but otherwise the view will be automatically registered on the mirror.
	 *
	 * @param resourcePackage Base path of the resources to upload
	 * @param mainPages Resources (path = key) which are the entry page for their views (viewId = value).
	 * @param icons Resources (path = key) which are the representing icons for their views (viewId = value).
	 * @return Path on the mirror's webserver under which the resource is available.
	 * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
	 * @throws URISyntaxException Thrown if resourcePackage can't be transformed into URI.
	 * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
	 */
	@Nonnull
	public Map<String, String> uploadResourcesFromPackage(@Nonnull final String resourcePackage, @Nonnull final Map<String, String> mainPages,
			@Nonnull final Map<String, String> icons)
			throws IOException, URISyntaxException {
		if (resourcePackage == null) {
			throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
		}
		if (mainPages == null) {
			throw new IllegalArgumentException("'mainPages' mustn't be null!");
		}
		if (icons == null) {
			throw new IllegalArgumentException("'icons' mustn't be null!");
		}
		return this.uploadResourcesFromPackage(resourcePackage, "", mainPages, icons);
	}

	/**
	 * Uploads the provided resource files at or below the given path to the mirror.
	 *
	 * @param resourcePackage Base path of the resources to upload
	 * @param urlBasePath Target path on the webserver relative to the app's base directory.
	 * @return Path on the mirror's webserver under which the resource is available.
	 * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
	 * @throws URISyntaxException Thrown if resourcePackage can't be transformed into URI.
	 * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
	 */
	@Nonnull
	public Map<String, String> uploadResourcesFromPackage(@Nonnull final String resourcePackage, @Nonnull final String urlBasePath)
			throws IOException, URISyntaxException {
		if (resourcePackage == null) {
			throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
		}
		if (urlBasePath == null) {
			throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
		}
		return uploadResourcesFromPackage(resourcePackage, urlBasePath, null, null, null);
	}

	/**
	 * Uploads the provided resource files at or below the given path to the mirror. Main pages or representing icons of views can be defined by using the
	 * resource path of the corresponding resource as keys and the view id's as values. If the defined resource paths for a view is not within the given
	 * resource package path, the definition of the main page or icon has no effect but otherwise the view will be automatically registered on the mirror.
	 *
	 * @param resourcePackage Base path of the resources to upload
	 * @param urlBasePath Target path on the webserver relative to the app's base directory.
	 * @param mainPages Resources (path = key) which are the entry page for their views (viewId = value).
	 * @param icons Resources (path = key) which are the representing icons for their views (viewId = value).
	 * @return Path on the mirror's webserver under which the resource is available.
	 * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
	 * @throws URISyntaxException Thrown if resourcePackage can't be transformed into URI.
	 * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
	 */
	@Nonnull
	public Map<String, String> uploadResourcesFromPackage(@Nonnull final String resourcePackage, @Nonnull final String urlBasePath,
			@Nonnull final Map<String, String> mainPages, @Nonnull final Map<String, String> icons)
			throws IOException, URISyntaxException {
		if (resourcePackage == null) {
			throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
		}
		if (urlBasePath == null) {
			throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
		}
		if (mainPages == null) {
			throw new IllegalArgumentException("'mainPages' mustn't be null!");
		}
		if (icons == null) {
			throw new IllegalArgumentException("'icons' mustn't be null!");
		}
		final String resPackage = resourcePackage.endsWith("/") ? resourcePackage : resourcePackage + "/";
		final String remoteUrlBasePath = urlBasePath.endsWith("/") ? urlBasePath : urlBasePath + "/";
		final Map<String, String> resourceMappings = new HashMap<>();
		final List<MainPageUploader> mainPageUploads = new ArrayList<>(mainPages.size());
		final List<String> resources = listResources(resPackage);
		for (final String resource : resources) {
			final String resourcePath = resPackage + resource;
			final String relativeUrlPath = remoteUrlBasePath + resource;
			final boolean isIcon = icons.containsKey(resourcePath);
			final String viewId = icons.get(resourcePath);
			final InputStreamSupplier isSupplier = () -> this.classLoader.getResourceAsStream(resourcePath);
			resourceMappings.put(resourcePath,
					uploadResource(isSupplier, relativeUrlPath, isIcon ? new ResourceRegistrationConfig(viewId, false, true) : null));
			if (mainPages.containsKey(resourcePath)) {
				mainPageUploads.add(() -> {
					final String mappedPath =
							uploadResource(isSupplier, relativeUrlPath, new ResourceRegistrationConfig(mainPages.get(resourcePath), true, false));
					resourceMappings.put(resourcePath, mappedPath);
				});
			}
		}
		for (final MainPageUploader mpu : mainPageUploads) {
			mpu.upload();
		}
		return resourceMappings;
	}

	/**
	 * Uploads the provided resource files at or below the given path to the mirror. Main pages or representing icons of views can be defined by their resource
	 * paths. If the defined resource paths for a view is not within the given resource package path, the definition of the main page or icon has no effect but
	 * otherwise the view will be automatically registered on the mirror.
	 *
	 * @param resourcePackage Base path of the resources to upload
	 * @param appViewId identifier of the view the resource to upload belongs to.
	 * @param mainPageResource Resource path of the entry page for the given view.
	 * @param iconResource Resource path of the icon representing the given view.
	 * @return Path on the mirror's webserver under which the resource is available.
	 * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
	 * @throws URISyntaxException Thrown if resourcePackage can't be transformed into URI.
	 * @throws IllegalArgumentException Thrown if resourcePackage parameter is {@code null}.
	 */
	@Nonnull
	public Map<String, String> uploadResourcesFromPackage(@Nonnull final String resourcePackage, @Nullable final String appViewId,
			@Nullable final String mainPageResource, @Nullable final String iconResource)
			throws IOException, URISyntaxException {
		if (resourcePackage == null) {
			throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
		}
		return this.uploadResourcesFromPackage(resourcePackage, "", appViewId, mainPageResource, iconResource);
	}

	/**
	 * Uploads the provided resource files at or below the given path to the mirror. Main pages or representing icons of views can be defined by their resource
	 * paths. If the defined resource paths for a view is not within the given resource package path, the definition of the main page or icon has no effect but
	 * otherwise the view will be automatically registered on the mirror.
	 *
	 * @param resourcePackage Base path of the resources to upload
	 * @param urlBasePath Target path on the webserver relative to the app's base directory.
	 * @param appViewId identifier of the view the resource to upload belongs to.
	 * @param mainPageResource Resource path of the entry page for the given view.
	 * @param iconResource Resource path of the icon representing the given view.
	 * @return Path on the mirror's webserver under which the resource is available.
	 * @throws IOException Thrown if connecting or transferring data from/to mirror fails.
	 * @throws URISyntaxException Thrown if resourcePackage can't be transformed into URI.
	 * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
	 */
	@Nonnull
	public Map<String, String> uploadResourcesFromPackage(@Nonnull final String resourcePackage, @Nonnull final String urlBasePath,
			@Nonnull final String appViewId, @Nonnull final String mainPageResource, @Nonnull final String iconResource)
			throws IOException, URISyntaxException {
		if (resourcePackage == null) {
			throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
		}
		if (urlBasePath == null) {
			throw new IllegalArgumentException("'urlBasePath' mustn't be null!");
		}
		if (appViewId == null) {
			throw new IllegalArgumentException("'appViewId' mustn't be null!");
		}
		if (mainPageResource == null) {
			throw new IllegalArgumentException("'mainPageResource' mustn't be null!");
		}
		if (iconResource == null) {
			throw new IllegalArgumentException("'iconResource' mustn't be null!");
		}
		final Map<String, String> mainPages = Collections.singletonMap(mainPageResource, appViewId);
		final Map<String, String> icons = Collections.singletonMap(iconResource, appViewId);
		return uploadResourcesFromPackage(resourcePackage, urlBasePath, mainPages, icons);
	}

	@Nonnull
	private List<String> listResources(@Nonnull final String resourcePackage)
			throws IOException, URISyntaxException {
		final List<String> resourceList = new LinkedList<>();
		final URL resourcePackageUrl = this.classLoader.getResource(resourcePackage);
		if (resourcePackageUrl == null) {
			throw new IOException("The provided resource package '" + resourcePackage + "' was not found, nothing to upload");
		}
		final URI uri = resourcePackageUrl.toURI();
		if (SCHEME_JAR.equalsIgnoreCase(uri.getScheme())) {
			final String uriFilePath = uri.getRawSchemeSpecificPart();
			final int indicatorIndex = uriFilePath.indexOf(JAR_PACKAGE_INDICATOR);
			if (indicatorIndex < 0) {
				throw new IOException("Could not access files in resourcePackage, corresponding jar file has an unexpected format!");
			}
			// class resources are in a packaged file
			String jarPath = uriFilePath.substring(0, indicatorIndex);
			// // remove "jar:" prefix and make sure file: prefix is used
			if (jarPath.startsWith(SCHEME_JAR_PREFIX)) {
				jarPath = SCHEME_FILE_PREFIX + jarPath.substring(SCHEME_JAR_PREFIX.length());
			}
			else if (!jarPath.startsWith(SCHEME_FILE_PREFIX)) {
				jarPath = SCHEME_FILE_PREFIX + jarPath;
			}
			final URI jarUri = URI.create(jarPath);
			String basePath = resourcePackage;
			if (!basePath.endsWith("/")) {
				basePath += "/";
			}
			try (final JarFile jarFile = new JarFile(Paths.get(jarUri).toFile())) {
				// enumerate over all entries
				final Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					final JarEntry entry = entries.nextElement();
					final String name = entry.getName();
					if (name.endsWith("/")) {
						// skip directories
						continue;
					}
					if (name.startsWith(basePath)) {
						resourceList.add(name.substring(basePath.length()));
					}
				}
			}
		}
		else {
			final Path myPath = Paths.get(uri);
			try (final Stream<Path> walk = Files.walk(myPath, Integer.MAX_VALUE)) {
				for (final Iterator<Path> it = walk.iterator(); it.hasNext();) {
					final Path path = it.next();
					if (!path.toFile().isFile()) {
						continue;
					}
					final Path relPath = myPath.relativize(path);
					final String relWebPath = relPath.toString().replaceAll(Pattern.quote(File.separator), "/");
					resourceList.add(relWebPath);
				}
			}
		}
		return resourceList;
	}

}
