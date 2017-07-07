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

	private interface WeatherStationState extends DeviceAPIObserver {

	}
/*
	private final class WeatherStationNotConfigured implements WeatherStationState {

		@Override
		public void addedToDevices(@Nonnull final Device device) {
			if (device.getProfileIdentifier().equals(DriverConstants.PROFILE_WeatherStation_ID)) {
				setWeatherStationState(new WeatherStationConfigured(device));
			}
		}

		@Override
		public void removedFromDevices(@Nonnull final Device device) {
			// nothing to do
		}
	}

	private final class WeatherStationConfigured implements WeatherStationState, WeatherDataObserver {

		/** Defines how long to wait for further value updates before processing the new values */
		private static final int VALUE_UPDATES_COLLECTION_DELAY = 3000;

		@Nonnull
		private final ObjectMapper mapper = new ObjectMapper();
		@Nonnull
		private final Device weatherStation;
		@Nonnull
		private final WeatherstationDeviceObserver weatherManager;
		@Nonnull
		private final Object weatherDataUpdateTimerLock = new Object();
		@Nonnull
		private Timer weatherDataUpdateTimer = new Timer();

		private WeatherStationConfigured(@Nonnull final Device weatherStation) {
			this.weatherStation = weatherStation;
			this.weatherManager = new WeatherstationDeviceObserver(weatherStation, this);
		}

		@Override
		public void onUpdate(@Nonnull final WeatherData weatherData) {
			synchronized (this.weatherDataUpdateTimerLock) {
				this.weatherDataUpdateTimer.cancel();
				// probably multiple properties gets updated in a row. Collect updates first and then send update
				final TimerTask weatherDataUpdateTask = new TimerTask() {

					@Override
					public void run() {
						try {
							LOG.debug("Send updated weather data to mirror");
							final String weatherDataJson;
							weatherDataJson = WeatherStationConfigured.this.mapper.writeValueAsString(weatherData);
							final TemplateConfig weatherDataTemplateConf = new TemplateConfig(VIEW_TEMPLATE_WEATHERDATA, VIEW_WEBPATH_WEATHERDATA);
							weatherDataTemplateConf.putReplacement("null", weatherDataJson);
							MirrorExampleApp.this.viewRegistrator.updateTemplatePage(weatherDataTemplateConf);
						}
						catch (final JsonProcessingException e) {
							LOG.error("Failed to convert weatherData object into json string!", e);
						}
					}
				};
				this.weatherDataUpdateTimer = new Timer();
				this.weatherDataUpdateTimer.schedule(weatherDataUpdateTask, VALUE_UPDATES_COLLECTION_DELAY);
			}
		}

		@Override
		public void addedToDevices(@Nonnull final Device device) {
			// nothing to do
		}

		@Override
		public void removedFromDevices(@Nonnull final Device device) {
			// Validate.notNull(device, "Parameter 'device' mustn't be null.");
			if (this.weatherStation.equals(device)) {
				setWeatherStationState(new WeatherStationNotConfigured());
				this.weatherManager.dispose();
			}
		}


	}

	private static final Logger LOG = LoggerFactory.getLogger(MirrorExampleApp.class);
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

	private static final String VIEW_ID_CLOCK = "DateTimeView";
	private static final String ICON_RESPATH_CLOCK = VIEW_RESOURCES + "clock-icon.jpg";
	private static final String VIEW_RESPATH_CLOCK = VIEW_RESOURCES + "clock.html";

	private static final String VIEW_ID_WELCOME = "WelcomeView";
	private static final String ICON_RESPATH_WELCOME = VIEW_RESOURCES + "welcome.png";
	private static final String VIEW_RESPATH_WELCOME = VIEW_RESOURCES + "welcome.html";
	private static final String VIEW_TEMPLATE_WELCOME = VIEW_RESOURCES + "welcome.template";
	private static final String VIEW_WEBPATH_WELCOME = "welcome.html";

	private static final String VIEW_ID_HELLO_WORLD = "WelcomeWorldView";
	private static final String ICON_RESPATH_HELLO_WORLD = VIEW_RESOURCES + "welcome_friends.jpg";
	private static final String VIEW_RESPATH_HELLO_WORLD = VIEW_RESOURCES + "welcome_world.html";

	//Das hier brauchen wir ...
	private static final String VIEW_ID_QUOTE = "QuoteView";
	private static final String ICON_RESPATH_QUOTE = VIEW_RESOURCES + "quote.png";
	private static final String VIEW_TEMPLATE_QUOTE = VIEW_RESOURCES + "quote.template";
	private static final String VIEW_WEBPATH_QUOTE = "quote.html";

	private static final String VIEW_ID_WEATHER = "WeatherView";
	private static final String ICON_RESPATH_WEATHER = VIEW_RESOURCES + "weather.jpg";
	private static final String VIEW_RESPATH_WEATHER = VIEW_RESOURCES + "weather.html";
	private static final String VIEW_TEMPLATE_WEATHERDATA = VIEW_RESOURCES + "current-weather.json";
	private static final String VIEW_WEBPATH_WEATHERDATA = "current-weather.json";

	private static final String VIEW_ID_LNDW = "LNDWView";
	private static final String ICON_RESPATH_LNDW = VIEW_RESOURCES + "lndw.png";
	private static final String VIEW_RESPATH_LNDW = VIEW_RESOURCES + "lndw.html";

	private static final String STORAGE_KEY_USERNAME = "uname";

	private static final List<Quote> quotes = new ArrayList<>();
	private Quote quoteOfTheDay = null;

	@Nonnull
	private WeatherStationState weatherStationState = new WeatherStationNotConfigured();

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
		staticResourceConfig.addView(VIEW_ID_CLOCK, VIEW_RESPATH_CLOCK, ICON_RESPATH_CLOCK);
		staticResourceConfig.addView(VIEW_ID_HELLO_WORLD, VIEW_RESPATH_HELLO_WORLD, ICON_RESPATH_HELLO_WORLD);
		staticResourceConfig.addView(VIEW_ID_LNDW, VIEW_RESPATH_LNDW, ICON_RESPATH_LNDW);
		staticResourceConfig.addView(VIEW_ID_WELCOME, VIEW_RESPATH_WELCOME, ICON_RESPATH_WELCOME);
		staticResourceConfig.addView(VIEW_ID_WEATHER, VIEW_RESPATH_WEATHER, ICON_RESPATH_WEATHER);
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
