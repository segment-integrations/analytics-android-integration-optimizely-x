package com.segment.analytics.android.integrations.optimizelyx;

import android.os.Handler;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.notification.NotificationListener;
import com.segment.analytics.Analytics;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.TrackPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Optimizely X helps marketers and growth hackers make smart decisions through A/B testing.
 *
 * @see <a href="https://www.optimizely.com/">Optimizely</a>
 * @see <a href="https://segment.com/docs/destinations/optimizelyx/">Optimizely Integration</a>
 * @see <a
 *     href="https://developers.optimizely.com/x/solutions/sdks/introduction/index.html?language=android&platform=mobile">Optimizely
 *     Android SDK</a>
 */
public class OptimizelyXIntegration extends Integration<Void> {

  private static final String OPTIMIZELYX_KEY = "Optimizely X";
  NotificationListener listener;
  private OptimizelyClient client;
  private final Logger logger;
  boolean isClientValid = false;
  boolean trackKnownUsers;
  static boolean nonInteraction;
  boolean listen;
  static final Options options = new Options().setIntegration(OPTIMIZELYX_KEY, false);
  private Map<String, String> attributes = new HashMap<>();
  private List<TrackPayload> trackEvents = new ArrayList<>();
  private final Handler handler = new Handler();

  public static Factory factory(OptimizelyManager manager) {
    return new Factory(manager);
  }

  private static class Factory implements Integration.Factory {
    private final OptimizelyManager manager;

    Factory(OptimizelyManager manager) {
      this.manager = manager;
    }

    @Override
    public Integration<?> create(ValueMap settings, Analytics analytics) {
      Logger logger = analytics.logger(OPTIMIZELYX_KEY);
      return new OptimizelyXIntegration(analytics, manager, settings, logger);
    }

    @Override
    public String key() {
      return OPTIMIZELYX_KEY;
    }
  }

  public OptimizelyXIntegration(
      final Analytics analytics, OptimizelyManager manager, ValueMap settings, Logger logger) {
    this.client = manager.getOptimizely();
    this.logger = logger;

    trackKnownUsers = settings.getBoolean("trackKnownUsers", false);
    nonInteraction = settings.getBoolean("nonInteraction", true);
    listen = settings.getBoolean("listen", true);

    if (client.isValid()) {
      isClientValid = true;
      if (listen) {
        this.listener = createListener(analytics);
        client.addNotificationListener(listener);
      }
      return;
    }
    if (!client.isValid()) {
      pollOptimizelyClient(manager, analytics);
    }
  }

  @Override
  public void identify(IdentifyPayload identify) {
    super.identify(identify);

    if (!isNullOrEmpty(identify.traits())) {
      attributes = identify.traits().toStringMap();
    }
  }

  @Override
  public void track(TrackPayload track) {
    super.track(track);

    int queueSize = trackEvents.size();

    synchronized (this) {
      if (!isClientValid) {
        logger.verbose("Optimizely not initialized. Enqueueing action: %s", track);
        if (queueSize < 100) {
          trackEvents.add(track);
          return;
        }
        if (queueSize >= 100) {
          trackEvents.remove(0);
          trackEvents.add(track);
          return;
        }
      }
    }

    // Segment will default sending `track` calls with `anonymousId`s since Optimizely X does not alias known and unknown users
    // https://developers.optimizely.com/x/solutions/sdks/reference/index.html?language=objectivec&platform=mobile#user-ids
    String userId = track.userId();

    if (trackKnownUsers && isNullOrEmpty(userId)) {
      logger.verbose(
          "Segment will only track users associated with a userId "
              + "when the trackKnownUsers setting is enabled.");
      return;
    }

    String id = track.anonymousId();
    String event = track.event();
    Map<String, String> properties = track.properties().toStringMap();

    if (trackKnownUsers && !isNullOrEmpty(userId)) {
      id = track.userId();
    }

    client.track(event, id, attributes, properties);
    logger.verbose("client.track(%s, %s, %s, %s)", event, id, attributes, properties);
  }

  @Override
  public void reset() {
    super.reset();

    client.removeNotificationListener(listener);
    logger.verbose("client.removeNotificationListener(listener)");
  }

  private void pollOptimizelyClient(final OptimizelyManager manager, final Analytics analytics) {
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    final Runnable poll =
        new Runnable() {
          @Override
          public void run() {
            OptimizelyXIntegration.this.client = manager.getOptimizely();
            if (client.isValid()) {
              synchronized (OptimizelyXIntegration.this) {
                isClientValid = true;
              }
              handler.post(
                  new Runnable() {
                    @Override
                    public void run() {
                      setClientAndFlushTracks(analytics);
                    }
                  });
              scheduler.shutdown();
            }
          }
        };
    scheduler.scheduleAtFixedRate(poll, 60, 60, SECONDS);
  }

  private void setClientAndFlushTracks(Analytics analytics) {
    this.listener = createListener(analytics);
    client.addNotificationListener(listener);
    logger.verbose("Flushing track queue");

    for (TrackPayload t : trackEvents) {
      track(t);
    }

    trackEvents = null;
  }

  private OptimizelyNotificationListener createListener(Analytics analytics) {
    return new OptimizelyNotificationListener(analytics);
  }

  static class OptimizelyNotificationListener extends NotificationListener {
    private final Analytics analytics;

    public OptimizelyNotificationListener(Analytics analytics) {
      this.analytics = analytics;
    }

    @Override
    public void onExperimentActivated(
        Experiment experiment, String userId, Map<String, String> attributes, Variation variation) {

      Properties properties =
          new Properties()
              .putValue("experimentId", experiment.getId())
              .putValue("experimentName", experiment.getKey())
              .putValue("variationId", variation.getId())
              .putValue("variationName", variation.getKey())
              .putValue("nonInteraction", 1);

      if (!nonInteraction) {
        properties.remove("nonInteraction");
      }
      analytics.track("Experiment Viewed", properties, options);
    }
  }
}
