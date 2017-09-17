package com.segment.analytics.android.integrations.optimizelyx;

import android.content.Context;

import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.event.LogEvent;
import com.segment.analytics.Analytics;
import com.segment.analytics.Options;
import com.segment.analytics.Traits;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import com.optimizely.ab.Optimizely;
import com.optimizely.ab.notification.NotificationListener;
import com.optimizely.ab.config.Variation;

import java.util.Map;

/**
 * Optimizely X helps marketers and growth hackers make smart decisions through A/B testing.
 *
 * @see <a href="https://www.optimizely.com/">Optimizely</a>
 * @see <a href="https://segment.com/docs/destinations/optimizelyx/">Optimizely Integration</a>
 * @see <a href="https://developers.optimizely.com/x/solutions/sdks/introduction/index.html?language=android&platform=mobile">Optimizely Android SDK</a>
 */
public class OptimizelyXIntegration extends Integration<NotificationListener> {

  private static final String OPTIMIZELYX_KEY = "Optimizely X";
  private final Logger logger;
  final NotificationListener listener;

  public static final Factory FACTORY =
          new Factory() {
            @Override
            public Integration<?> create(ValueMap settings, Analytics analytics) {
              Logger logger = analytics.logger(OPTIMIZELYX_KEY);
              Context context = analytics.getApplication();
              return new OptimizelyXIntegration(analytics, settings, logger);
            }

            @Override
            public String key() { return OPTIMIZELYX_KEY; }
          };

  public OptimizelyXIntegration(Analytics analytics, ValueMap settings, Logger logger) {
    this.logger = logger;
    final Analytics finalAnalytics = analytics; // analytics must be set as a final variable to be accessed by "inner class" - why?
    final Options options = new Options().setIntegration("Optimizely", false);

    listener = new NotificationListener() {
      @Override
      public void onEventTracked(String eventKey,
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
          finalAnalytics.identify(userId, traits, options);
        }
        finalAnalytics.track(eventKey);
      }

      @Override
      public void onExperimentActivated(Experiment experiment,
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
          finalAnalytics.identify(userId, traits, options);
        }

        Properties properties = new Properties()
                .putValue("experimentId", experiment.getId())
                .putValue("experimentName", experiment.getKey())
                .putValue("variationId", variation.getId())
                .putValue("variationName", variation.getKey());
        finalAnalytics.track("Experiment Viewed", properties, options);
      }
    };
  }

  @Override
  public NotificationListener getUnderlyingInstance() {
    return listener;
  }
}
