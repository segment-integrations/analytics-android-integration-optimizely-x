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
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.TrackPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

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
  final NotificationListener listener;
  private final OptimizelyClient client;
  private final Logger logger;
  private boolean isClientValid = false;
  boolean trackKnownUsers;
  static final Options options = new Options().setIntegration(OPTIMIZELYX_KEY, false);
  private List<TrackPayload> trackEvents = new ArrayList<>();

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
    listener = new OptimizelyNotificationListener(analytics);

    if (client.isValid()) {
      isClientValid = true;
      client.addNotificationListener(listener);
    } else {
      pollOptimizelyClient();
    }
  }

  @Override
  public void track(TrackPayload track) {
    super.track(track);

    synchronized (this) {
      if (!isClientValid) {
        trackEvents.add(track);
        return;
      }
    }

    String id;
    String userId = track.userId();
    String event = track.event();
    Map<String, String> properties = track.properties().toStringMap();

    if (trackKnownUsers && isNullOrEmpty(userId)) {
      logger.verbose(
          "Segment will only track users associated with a userId "
              + "when the trackKnownUsers setting is enabled.");
      return;
    } else if (trackKnownUsers && !isNullOrEmpty(userId)) {
      id = track.userId();
    } else {
      id = track.anonymousId();
    }

    client.track(event, id, properties);
    logger.verbose("client.track(%s, %s, %s)", event, id, properties);
  }

  @Override
  public void reset() {
    super.reset();

    client.removeNotificationListener(listener);
    logger.verbose("client.removeNotificationListener(listener)");
  }

  private void pollOptimizelyClient() {
    final Handler handler = new Handler();
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            try {
              while (!client.isValid()) {
                Thread.sleep(30000);
              }

              List<TrackPayload> trackEvents;

              synchronized (OptimizelyXIntegration.this) {
                isClientValid = true;
                trackEvents = OptimizelyXIntegration.this.trackEvents;
                OptimizelyXIntegration.this.trackEvents = null;
              }
              client.addNotificationListener(listener);
              for (TrackPayload t : trackEvents) {
                track(t);
              }
            } catch (InterruptedException e) {
              logger.verbose(e.toString());
            }
          }
        },
        30000);
  }

  private static class OptimizelyNotificationListener extends NotificationListener {
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
              .putValue("variationName", variation.getKey());
      analytics.track("Experiment Viewed", properties, options);
    }
  }
}
