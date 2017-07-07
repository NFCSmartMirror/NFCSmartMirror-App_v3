package iolite;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.iolite.api.IOLITEAPINotResolvableException;
import de.iolite.api.IOLITEAPIProvider;
import de.iolite.api.IOLITEPermissionDeniedException;
import de.iolite.app.AbstractIOLITEApp;
import de.iolite.app.api.device.access.Device;
import de.iolite.app.api.device.access.DeviceAPI;
import de.iolite.app.api.device.access.DeviceAPI.DeviceAPIObserver;
import de.iolite.app.api.frontend.FrontendAPI;
import de.iolite.app.api.frontend.FrontendAPIException;
import de.iolite.app.api.frontend.util.FrontendAPIUtility;
import de.iolite.app.api.storage.StorageAPI;
import de.iolite.app.api.storage.StorageAPIException;
import de.iolite.app.api.user.access.UserAPI;
import de.iolite.common.identifier.EntityIdentifier;
import de.iolite.common.lifecycle.exception.CleanUpFailedException;
import de.iolite.common.lifecycle.exception.InitializeFailedException;
import de.iolite.common.lifecycle.exception.StartFailedException;
import de.iolite.common.lifecycle.exception.StopFailedException;
import de.iolite.common.requesthandler.HTTPStatus;
import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPRequestHandler;
import de.iolite.common.requesthandler.IOLITEHTTPResponse;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;
import de.iolite.common.requesthandler.StaticResources;
import de.iolite.drivers.basic.DriverConstants;
import de.iolite.insys.mirror.ViewRegistrator.ResourcePackageConfig;
import de.iolite.insys.mirror.ViewRegistrator.TemplateConfig;
//import de.iolite.insys.mirror.api.MirrorApiException;
//import de.iolite.insys.mirror.weather.WeatherData;
//import de.iolite.insys.mirror.weather.WeatherDataObserver;
//import de.iolite.insys.mirror.weather.WeatherstationDeviceObserver;

/**
 * Starter class for IOLITE framework that handles the app lifecycle.
 *
 * @author Hendrik Motza
 * @since 17.05
 */
public class MirrorExampleApp extends AbstractIOLITEApp {

	//private static final Logger LOG = LoggerFactory.getLogger(MirrorExampleApp.class);
	private static final String RES_BASE = "de/iolite/insys/mirror/";
	private static final String RES_QUOTES = RES_BASE + "quotes.csv";
	private static final String HTML_RESOURCES = RES_BASE + "html/";
	private static final String VIEW_RESOURCES = RES_BASE + "views/";
	private static final String APP_ID = "de.iolite.insys.mirror.MirrorExampleApp";

	private static final String MSG_ERR_RETRIEVE_FRONTENDAPI = "Could not retrieve instance of FrontendAPI!";
	private static final String MSG_ERR_RETRIEVE_STORAGEAPI = "Could not retrieve instance of StorageAPI!";
	private static final String MSG_ERR_RETRIEVE_USERAPI = "Could not retrieve instance of UserAPI!";
	private static final String MSG_ERR_RETRIEVE_DEVICEAPI = "Could not retrieve instance of DeviceAPI!";
	private static final String MSG_ERR_REGISTER_FRONTEND_RESOURCES = "An error appeared during the registration of frontend resources. This could result in errors when trying to display the app gui!";

	//Das hier brauchen wir ...
	private static final String VIEW_ID_QUOTE = "QuoteView";
	private static final String ICON_RESPATH_QUOTE = VIEW_RESOURCES + "quote.png";
	private static final String VIEW_TEMPLATE_QUOTE = VIEW_RESOURCES + "quote.template";
	private static final String VIEW_WEBPATH_QUOTE = "quote.html";

	private static final String STORAGE_KEY_USERNAME = "uname";

	private static final List<Quote> quotes = new ArrayList<>();
	private Quote quoteOfTheDay = null;

	private ScheduledFuture<?> quoteUpdateThread = null;
	private ViewRegistrator viewRegistrator;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanUpHook()
			throws CleanUpFailedException {
		quotes.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initializeHook()
			throws InitializeFailedException {
		final String quoteFileContent;
		try {
			quoteFileContent = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(RES_QUOTES), "UTF-8");
		}
		catch (final IOException e) {
			throw new InitializeFailedException("Missing resource file ['+RES_QUOTES+']!", e);
		}
		final String[] quoteLines = quoteFileContent.split(Pattern.quote("\n"));
		for (final String quoteLine : quoteLines) {
			final String[] quote = quoteLine.split(Pattern.quote(";"), 2);
			final String author = quote[0].isEmpty() ? "Unknown" : quote[0];
			quotes.add(new Quote(quote[1], author));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void startHook(final IOLITEAPIProvider context)
			throws StartFailedException {
		final FrontendAPI frontendApi;
		try {
			frontendApi = context.getAPI(FrontendAPI.class);
		}
		catch (final IOLITEAPINotResolvableException | IOLITEPermissionDeniedException e) {
			LOG.error(MSG_ERR_RETRIEVE_FRONTENDAPI, e);
			throw new StartFailedException(MSG_ERR_RETRIEVE_FRONTENDAPI, e);
		}

		final StorageAPI storageApi;
		try {
			storageApi = context.getAPI(StorageAPI.class);
		}
		catch (final IOLITEAPINotResolvableException | IOLITEPermissionDeniedException e) {
			LOG.error(MSG_ERR_RETRIEVE_STORAGEAPI, e);
			throw new StartFailedException(MSG_ERR_RETRIEVE_STORAGEAPI, e);
		}

		try {
			FrontendAPIUtility.registerPublicHandlers(frontendApi, StaticResources.scanClasspath(HTML_RESOURCES, getClass().getClassLoader()));
			frontendApi.registerPublicClasspathStaticResource("", HTML_RESOURCES + "index.html");
			frontendApi.registerRequestHandler("updateUsername", new IOLITEHTTPRequestHandler() {

				@Override
				public IOLITEHTTPResponse handleRequest(final IOLITEHTTPRequest request, final EntityIdentifier callerEntityID, final String subPath) {
					final String username = request.getParameter("username");
					updateUsername(username);
					try {
						storageApi.saveString(STORAGE_KEY_USERNAME, username);
					}
					catch (final StorageAPIException e) {
						LOG.error("Failed to save configured username!");
						return new IOLITEHTTPStaticResponse(HTTPStatus.InternalServerError, "text/plain");
					}
					return new IOLITEHTTPStaticResponse(HTTPStatus.OK, "text/plain");
				}

				@Override
				public void handlerRemoved(final String mapping, final EntityIdentifier callerEntityID) {
					// nothing to do
				}
			});
		}
		catch (final FrontendAPIException e) {
			LOG.error(MSG_ERR_REGISTER_FRONTEND_RESOURCES, e);
		}

		final UserAPI userApi;
		final String userId;
		try {
			userApi = context.getAPI(UserAPI.class);
			userId = userApi.getUser().getIdentifier();
		}
		catch (final IOLITEAPINotResolvableException | IOLITEPermissionDeniedException e) {
			LOG.error(MSG_ERR_RETRIEVE_USERAPI, e);
			throw new StartFailedException(MSG_ERR_RETRIEVE_USERAPI, e);
		}

		final DeviceAPI deviceApi;
		try {
			deviceApi = context.getAPI(DeviceAPI.class);
		}
		catch (final IOLITEAPINotResolvableException | IOLITEPermissionDeniedException e) {
			LOG.error(MSG_ERR_RETRIEVE_DEVICEAPI, e);
			throw new StartFailedException(MSG_ERR_RETRIEVE_DEVICEAPI, e);
		}

		final ResourcePackageConfig staticResourceConfig = new ResourcePackageConfig(VIEW_RESOURCES);

		staticResourceConfig.addView(VIEW_ID_QUOTE, null, ICON_RESPATH_QUOTE);
		this.viewRegistrator = new ViewRegistrator(staticResourceConfig, APP_ID, userId);
		final DeviceAPIObserver deviceObserver = new DeviceAPIObserver() {

			@Override
			public synchronized void addedToDevices(final Device element) {
				MirrorExampleApp.this.viewRegistrator.addedToDevices(element);
				MirrorExampleApp.this.weatherStationState.addedToDevices(element);
			}

			@Override
			public synchronized void removedFromDevices(final Device element) {
				MirrorExampleApp.this.viewRegistrator.removedFromDevices(element);
				MirrorExampleApp.this.weatherStationState.removedFromDevices(element);
			}
		};
		deviceApi.setObserver(deviceObserver);
		deviceApi.getDevices().forEach(deviceObserver::addedToDevices);

		try {
			updateUsername(storageApi.loadString(STORAGE_KEY_USERNAME));
		}
		catch (final StorageAPIException e1) {
			LOG.debug("No username set yet, personal welcome view will not be registered on mirrors!");
		}
// Das hier brauchen wir .....
		this.quoteUpdateThread = context.getScheduler().scheduleAtFixedRate(() -> {
			try {
				final long position = Math.round(Math.random() * (quotes.size() - 1));
				this.quoteOfTheDay = quotes.get((int) position);
				final TemplateConfig templateConf = new TemplateConfig(VIEW_TEMPLATE_QUOTE, VIEW_WEBPATH_QUOTE, VIEW_ID_QUOTE);
				templateConf.putReplacement("{QUOTE}", this.quoteOfTheDay.getQuote());
				templateConf.putReplacement("{AUTHOR}", this.quoteOfTheDay.getAuthor());
				this.viewRegistrator.updateTemplatePage(templateConf);
			}
			catch (final MirrorApiException e) {
				LOG.error("Could not create views!", e);
			}
		}, 0, 1, TimeUnit.MINUTES);
		LOG.debug("Mirror Views got registered!");

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void stopHook()
			throws StopFailedException {
		LOG.debug("Stopping APP ");
		this.quoteUpdateThread.cancel(false);
	}

	private void updateUsername(final String username) {
		final TemplateConfig welcomeTemplateConf = new TemplateConfig(VIEW_TEMPLATE_WELCOME, VIEW_WEBPATH_WELCOME, VIEW_ID_WELCOME);
		welcomeTemplateConf.putReplacement("{USERNAME}", username);
		this.viewRegistrator.updateTemplatePage(welcomeTemplateConf);
		LOG.debug("Welcome page with username '" + username + "' created!");
	}

	private synchronized void setWeatherStationState(@Nonnull final WeatherStationState weatherStationState) {
		this.weatherStationState = weatherStationState;
	}

}
