package com.segment.analytics.android.integrations.optimizelyx;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.event.LogEvent;
import com.segment.analytics.Analytics;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Integration;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import com.optimizely.ab.notification.NotificationListener;
import com.optimizely.ab.config.Variation;

import java.util.Map;

/**
 * Optimizely X helps marketers and growth hackers make smart decisions through A/B testing.
 *
 * @see <a href="https://www.optimizely.com/">Optimizely</a>
 * @see <a href="https://segment.com/docs/destinations/optimizelyx/">Optimizely Integration</a>
 * @see <a
 *     href="https://developers.optimizely.com/x/solutions/sdks/introduction/index.html?language=android&platform=mobile">Optimizely
 *     Android SDK</a>
 */
public class OptimizelyXIntegration extends Integration<NotificationListener> {
  static final Options options = new Options().setIntegration("Optimizely X", false);
  private static final String OPTIMIZELYX_KEY = "Optimizely X";
  final NotificationListener listener;

  public static final Factory FACTORY =
      new Factory() {
        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics) {
          return new OptimizelyXIntegration(analytics);
        }

        @Override
        public String key() {
          return OPTIMIZELYX_KEY;
        }
      };

  OptimizelyXIntegration(final Analytics analytics) {

    listener =
        new NotificationListener() {
          @Override
          public void onEventTracked(
              String eventKey,
              String userId,
              Map<String, String> attributes,
              Long eventValue,
              LogEvent logEvent) {

            if (!isNullOrEmpty(attributes)) {
              Traits traits = new Traits();
              for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String trait = entry.getKey();
                String value = entry.getValue();
                traits.putValue(trait, value);
              }
              analytics.identify(userId, traits, options);
            }
            analytics.track(eventKey, null, options);
          }

          @Override
          public void onExperimentActivated(
              Experiment experiment,
              String userId,
              Map<String, String> attributes,
              Variation variation) {

            if (!isNullOrEmpty(attributes)) {
              Traits traits = new Traits();
              for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String trait = entry.getKey();
                String value = entry.getValue();
                traits.putValue(trait, value);
              }
              analytics.identify(userId, traits, options);
            }

            Properties properties =
                new Properties()
                    .putValue("experimentId", experiment.getId())
                    .putValue("experimentName", experiment.getKey())
                    .putValue("variationId", variation.getId())
                    .putValue("variationName", variation.getKey());
            analytics.track("Experiment Viewed", properties, options);
          }
        };
  }

  @Override
  public NotificationListener getUnderlyingInstance() {
    return listener;
  }
}
