package com.segment.analytics.android.integrations.optimizelyx;

import com.optimizely.ab.android.sdk.OptimizelyClient;
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
  boolean trackKnownUsers;
  static final Options options = new Options().setIntegration(OPTIMIZELYX_KEY, false);

  public static Factory factory(OptimizelyClient client) {
    return new Factory(client);
  }

  private static class Factory implements Integration.Factory {
    private final OptimizelyClient client;

    Factory(OptimizelyClient client) {
      this.client = client;
    }

    @Override
    public Integration<?> create(ValueMap settings, Analytics analytics) {
      Logger logger = analytics.logger(OPTIMIZELYX_KEY);
      return new OptimizelyXIntegration(analytics, client, settings, logger);
    }

    @Override
    public String key() {
      return OPTIMIZELYX_KEY;
    }
  }

  public OptimizelyXIntegration(
      final Analytics analytics, OptimizelyClient client, ValueMap settings, Logger logger) {
    this.client = client;
    this.logger = logger;

    trackKnownUsers = settings.getBoolean("trackKnownUsers", false);
    listener = new OptimizelyNotificationListener(analytics);
    client.addNotificationListener(listener);
  }

  @Override
  public void track(TrackPayload track) {
    super.track(track);

    if (trackKnownUsers && !isNullOrEmpty(track.userId())) {
      client.track(track.event(), track.userId(), track.properties().toStringMap());
      logger.verbose(
          "client.track(%s, %s, %s)",
          track.event(), track.userId(), track.properties().toStringMap());
    } else if (trackKnownUsers && isNullOrEmpty(track.userId())) {
      logger.verbose(
          "Segment will only track users associated with a userId when the "
              + "trackKnownUsers setting is enabled.");
    } else {
      client.track(track.event(), track.anonymousId(), track.properties().toStringMap());
      logger.verbose(
          "client.track(%s, %s, %s)",
          track.event(), track.anonymousId(), track.properties().toStringMap());
    }
  }

  @Override
  public void reset() {
    super.reset();

    client.removeNotificationListener(listener);
    logger.verbose("client.removeNotificationListener(%s)", listener);
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
