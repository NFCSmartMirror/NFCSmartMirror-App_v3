/*
 * Copyright (C) 2017 IOLITE, All rights reserved.
 */

package iolite;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports, die wir nicht brauchen
//import de.iolite.app.api.device.access.Device;
//import de.iolite.app.api.device.access.DeviceAPI.DeviceAPIObserver;
//import de.iolite.app.api.device.access.DeviceStringProperty;
//import de.iolite.app.api.device.access.DeviceStringProperty.DeviceStringPropertyObserver;
import de.iolite.insys.mirror.api.IOLITEMirrorConstants;
import de.iolite.insys.mirror.api.StaticResourceUploader;
import de.iolite.insys.mirror.api.StaticResourceUploader.InputStreamSupplier;
import de.iolite.insys.mirror.api.StaticResourceUploader.ResourceRegistrationConfig;

/**
 * Instances of this class can be added as observer to the device api. This way it will automatically detect connected mirrors and publish content on each of
 * them as soon as a mirror is available. The content to be added can be defined by the provided methods. The content always needs to be accessible, dynamic
 * content can be added by using static resources as templates and defining a set of replacements used to fill the templates.
 *
 * @author Hendrik Motza
 * @since 17.01
 */
public class ViewRegistrator implements DeviceAPIObserver {

	/**
	 * Defines static resources contained in a resource package.
	 *
	 * @author Hendrik Motza
	 * @since 17.01
	 */
	public static final class ResourcePackageConfig {

		@Nonnull
		private final String resourcePackage;
		@Nonnull
		private final Map<String, String> mainPages = new HashMap<>();
		@Nonnull
		private final Map<String, String> icons = new HashMap<>();

		/**
		 * Defines a resource package.
		 *
		 * @param resourcePackage package path of the resources to be added
		 * @throws IllegalArgumentException Thrown if parameter is {@code null}.
		 */
		public ResourcePackageConfig(@Nonnull final String resourcePackage) {
			if (resourcePackage == null) {
				throw new IllegalArgumentException("'resourcePackage' mustn't be null!");
			}
			this.resourcePackage = resourcePackage;
		}

		/**
		 * Can be used to automatically register new views based on the given resource path to mainPage and icon.
		 *
		 * @param viewId view identifier
		 * @param mainPageResource resource path of the start page
		 * @param iconResource resource path of the representing icon
		 * @throws IllegalArgumentException Thrown if parameter viewId is {@code null}.
		 */
		public void addView(@Nonnull final String viewId, @Nullable final String mainPageResource, @Nullable final String iconResource) {
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

		private Map<String, String> getIcons() {
			return Collections.unmodifiableMap(this.icons);
		}

		private Map<String, String> getMainPages() {
			return Collections.unmodifiableMap(this.mainPages);
		}

		private String getResourcePackage() {
			return this.resourcePackage;
		}

	}

	/**
	 * Defines dynamic resources based on a template file with simple string replacements.
	 *
	 * @author Hendrik Motza
	 * @since 17.01
	 */
	public static final class TemplateConfig {

		@Nonnull
		private final String resourcePath;
		@Nonnull
		private final String targetPath;
		@Nullable
		private final String viewId;
		@Nonnull
		private final Map<String, String> replacements = new HashMap<>();

		/**
		 * Creates instance and specifies a resource template file and its relative target path on webserver.
		 *
		 * @param templateResourcePath resource filepath of template
		 * @param targetFilePath relative path on webserver
		 */
		public TemplateConfig(@Nonnull final String templateResourcePath, @Nonnull final String targetFilePath) {
			this(templateResourcePath, targetFilePath, null);
		}

		public TemplateConfig(@Nonnull final String templateResourcePath, @Nonnull final String targetFilePath, @Nullable final String viewId) {
			if (templateResourcePath == null) {
				throw new IllegalArgumentException("Parameter 'templateResourcePath' mustn't be null!");
			}
			if (targetFilePath == null) {
				throw new IllegalArgumentException("Parameter 'targetFilePath' mustn't be null!");
			}
			this.resourcePath = templateResourcePath;
			this.targetPath = targetFilePath;
			this.viewId = viewId;
		}

		@Override
		public int hashCode() {
			return this.targetPath == null ? 0 : this.targetPath.hashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final TemplateConfig other = (TemplateConfig) obj;
			if (this.targetPath == null) {
				if (other.targetPath != null) {
					return false;
				}
			}
			return this.targetPath.equals(other.targetPath);
		}

		public void putReplacement(final String placeholder, final String replacement) {
			if (placeholder == null) {
				throw new IllegalArgumentException("Parameter 'placeholder' mustn't be null!");
			}

			if (replacement == null) {
				throw new IllegalArgumentException("Parameter 'replacement' mustn't be null!");
			}
			this.replacements.put(placeholder, replacement);
		}

		private Map<String, String> getReplacements() {
			return Collections.unmodifiableMap(this.replacements);
		}

		private String getResourcePath() {
			return this.resourcePath;
		}

		private String getTargetPath() {
			return this.targetPath;
		}

		private String getViewId() {
			return this.viewId;
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(ViewRegistrator.class);

	private final String appId;
	private final String userId;
	private final ResourcePackageConfig staticResConf;
	private final Set<TemplateConfig> dynamicResConf = new HashSet<>();

	private final Map<String, StaticResourceUploader> uploaders = new HashMap<>();

	/**
	 * Creates instance with a configuration for static resource files that shall be added to mirrors automatically.
	 *
	 * @param config configuration for static resource files
	 * @param appId identifier of the app
	 * @param userId identifier of the current user
	 * @throws IllegalArgumentException Thrown if any parameter is {@code null}.
	 */
	public ViewRegistrator(@Nonnull final ResourcePackageConfig config, @Nonnull final String appId, @Nonnull final String userId) {
		if (config == null) {
			throw new IllegalArgumentException("Parameter 'config' mustn't be null!");
		}
		if (appId == null) {
			throw new IllegalArgumentException("Parameter 'appId' mustn't be null!");
		}
		if (userId == null) {
			throw new IllegalArgumentException("Parameter 'userId' mustn't be null!");
		}
		this.staticResConf = config;
		this.appId = appId;
		this.userId = userId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addedToDevices(final Device device) {
		if (device.getProfileIdentifier().equals(IOLITEMirrorConstants.PROFILE_MIRROR_ID)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("New mirror '{}' detected with url '{}'!", device.getName(),
						device.getStringProperty(IOLITEMirrorConstants.PROPERTY_API_URL_ID).getValue());
			}
			addMirror(device);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removedFromDevices(final Device device) {
		synchronized (this.uploaders) {
			this.uploaders.remove(device.getIdentifier());
		}
	}

	//Hier brauchen wir eine eigene Discovery Funktion
	private void addMirror(final Device mirror) {
		final DeviceStringProperty mirrorUrl = mirror.getStringProperty(IOLITEMirrorConstants.PROPERTY_API_URL_ID);
		mirrorUrl.setObserver(new DeviceStringPropertyObserver() {

			@Override
			public void deviceChanged(final Device device) {
				// nothing to do
			}

			@Override
			public void keyChanged(final String key) {
				// nothing to do
			}

			@Override
			public void valueChanged(final String value) {
				LOG.debug("Api Url of mirror '{}' has changed to '{}'!", mirror.getName(), value);
				if (value == null) {
					synchronized (ViewRegistrator.this.uploaders) {
						ViewRegistrator.this.uploaders.remove(mirror.getIdentifier());
					}
					LOG.debug("Mirror '{}' seems to be no longer controllable, removed corresponding uploader!", mirror.getIdentifier());
					return;
				}
				handleNewApiUrl(mirror, value);
			}
		});
		final String apiUrl = mirrorUrl.getValue();
		if (apiUrl != null) {
			handleNewApiUrl(mirror, apiUrl);
		}
	}
	///hier
	//apiURL --> wenn auf dem pc läuft dann localhost von dummymirror
	//appID --> kann Dummy Wert nutzen
	//userID --> erstmal auch Dummy nehmen (vielleicht auch was android nehmen)
	private void handleNewApiUrl(final Device mirror, final String apiUrl) {
		final StaticResourceUploader uploader;
		try {
			uploader = new StaticResourceUploader(apiUrl, this.appId, this.userId);
			uploader.uploadResourcesFromPackage(this.staticResConf.getResourcePackage(), this.staticResConf.getMainPages(), this.staticResConf.getIcons());
		}
		catch (final MalformedURLException e) {
			LOG.error("Mirror API URL '{}' of device '{}' was invalid, current value will be ignored!", apiUrl, mirror.getIdentifier(), e);
			return;
		}
		catch (final URISyntaxException e) {
			LOG.error("Mirror API URL '{}' of device '{}' was invalid, current value will be ignored!", apiUrl, mirror.getIdentifier(), e);
			return;
		}
		catch (final IOException e) {
			LOG.error("Upload to mirror '{}' failed!", mirror.getIdentifier(), e);
			return;
		}
		synchronized (this.uploaders) {
			this.uploaders.put(mirror.getIdentifier(), uploader);
			synchronized (this.dynamicResConf) {
				for (final TemplateConfig templateConf : this.dynamicResConf) {
					uploadTemplate(Collections.singleton(uploader), templateConf);
				}
			}
		}
		LOG.debug("Static resources uploaded to mirror '{}'!", mirror.getName());
	}

	private void uploadTemplate(final Collection<StaticResourceUploader> uploaderSet, final TemplateConfig templateConf) {
		final byte[] resource;
		try {
			resource = buildFromTemplate(templateConf.getResourcePath(), templateConf.getReplacements());
		}
		catch (final IOException e) {
			LOG.error("Failed to load template file", e);
			return;
		}
		final InputStreamSupplier supplier = () -> {
			return new ByteArrayInputStream(resource);
		};
		final String target = templateConf.getTargetPath();
		final String viewId = templateConf.getViewId();
		for (final StaticResourceUploader uploader : uploaderSet) {
			try {
				uploader.uploadResource(supplier, target, viewId == null ? null : new ResourceRegistrationConfig(viewId, true, false));
			}
			catch (final IOException e) {
				LOG.error("Upload to mirror failed!", e);
			}
		}
	}

	private byte[] buildFromTemplate(final String templateResourcePath, final Map<String, String> replacements)
			throws IOException {
		try (final InputStream is = this.getClass().getClassLoader().getResourceAsStream(templateResourcePath)) {
			if (is == null) {
				throw new IOException("Resource not found: " + templateResourcePath);
			}
			String template = IOUtils.toString(is, StandardCharsets.UTF_8.name());
			for (final Entry<String, String> replacement : replacements.entrySet()) {
				template = template.replaceAll(Pattern.quote(replacement.getKey()), replacement.getValue());
			}
			return template.getBytes(StandardCharsets.UTF_8);
		}
	}

	/**
	 * Adds or updates a new dynamic resource based on a template configuration.
	 *
	 * @param templateConf configuration for the dynamic content
	 * @throws NullPointerException Thrown if parameter is {@code null}.
	 */
	public void updateTemplatePage(@Nonnull final TemplateConfig templateConf) {
		if (templateConf == null) {
			throw new NullPointerException("Parameter 'templateConf' mustn't be null!");
		}
		synchronized (this.dynamicResConf) {
			this.dynamicResConf.add(templateConf);
			uploadTemplate(this.uploaders.values(), templateConf);
		}
	}

}
