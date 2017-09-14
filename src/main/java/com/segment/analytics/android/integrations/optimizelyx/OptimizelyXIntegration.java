package com.segment.analytics.android.integrations.optimizelyx;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.TrackPayload;
import java.util.List;

/**
 * Kahuna helps mobile marketers send push notifications and in-app messages.
 *
 * @see <a href="https://www.optimizely.com/">Pptimizely</a>
 * @see <a href="https://segment.com/docs/integrations/optimizely/">Optimizely Integration</a>
 * @see <a href="http://developers.optimizely.com/android/">Optimizely Android SDK</a>
 */
public class OptimizelyXIntegration extends Integration<Void> {
  private static final String OPTIMIZELY_KEY = "Optimizely";

  private final Context context;
  private final Logger logger;
  private final Analytics analytics;

  // Optimizely must be initialized immediately on launch. So we require the token when creating the
  // factory, and initialize it in this method.
//  public static Factory createFactory(final Application application, String token) {
////    Optimizely.startOptimizelyWithAPIToken(token, application);
////    return new Factory() {
////      @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
////        boolean listen = settings.getBoolean("listen", false);
////        Logger logger = analytics.logger(OPTIMIZELY_KEY);
////
////        OptimizelyIntegration integration =
////            new OptimizelyIntegration(analytics, application, logger);
////        if (listen) {
////          Optimizely.addOptimizelyEventListener(integration);
////        }
////        return integration;
////      }
//
////      @Override public String key() {
////      return OPTIMIZELY_KEY;
//////      }
//    };
//  }

  public OptimizelyXIntegration(Analytics analytics, Context context, Logger logger) {
    this.analytics = analytics;
    this.context = context;
    this.logger = logger;
  }

  @Override public void identify(IdentifyPayload identify) {
  }

  @Override public void track(TrackPayload track) {
  }
}
