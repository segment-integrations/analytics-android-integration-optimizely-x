package com.segment.analytics.android.integrations.optimizelyx;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.LiveVariableUsageInstance;
import com.optimizely.ab.config.TrafficAllocation;
import com.optimizely.ab.config.Variation;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.android.integrations.optimizelyx.OptimizelyXIntegration.OptimizelyNotificationListener;
import static com.segment.analytics.android.integrations.optimizelyx.OptimizelyXIntegration.options;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@PowerMockIgnore({ "org.mockito.*", "org.roboelectric.*", "android.*" })
@PrepareForTest({ OptimizelyClient.class})
public class OptimizelyXTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Analytics analytics;
  private OptimizelyXIntegration integration;
  private OptimizelyClient client;
  private Map<String, String> attributes = new HashMap<>();

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mock(OptimizelyClient.class);
    PowerMockito.mock(OptimizelyManager.class);
    OptimizelyManager manager = mock(OptimizelyManager.class);
    client = mock(OptimizelyClient.class);
    when(manager.getOptimizely()).thenReturn(client);
    integration = new OptimizelyXIntegration(analytics, manager, new ValueMap()
            .putValue("trackKnownUsers", false)
            .putValue("nonInteraction", true)
            .putValue("listen", true),
            Logger.with(VERBOSE));
  }

  @Test public void track() {
    when(client.isValid()).thenReturn(true);
    Properties properties = new Properties();
    Traits traits = new Traits()
            .putValue("userId", "123")
            .putValue("anonymousId", "456");
    integration.track(new TrackPayloadBuilder().properties(properties).traits(traits).event("event").build());

    verify(client).track("event", "456", attributes, properties.toStringMap());
  }

  @Test public void trackKnownUsers() {
    when(client.isValid()).thenReturn(true);
    integration.trackKnownUsers = true;

    Properties properties = new Properties();
    Traits traits = new Traits()
            .putValue("userId", "123");
    integration.track(new TrackPayloadBuilder().properties(properties).traits(traits).event("event").build());

    verify(client).track("event", "123", attributes, properties.toStringMap());
  }

  @Test public void trackKnownUsersNoUserId() {
    when(client.isValid()).thenReturn(true);
    integration.trackKnownUsers = true;

    Traits traits = new Traits()
            .putValue("anonymousId", "456");
    integration.track(new TrackPayloadBuilder().traits(traits).event("event").build());

    verify(client,times(0)).track("event", "123", attributes);
  }

  @Test public void mapAttributesAndEventTags() {
    when(client.isValid()).thenReturn(true);
    integration.trackKnownUsers = true;

    Traits traits = new Traits()
            .putValue("userId", "123")
            .putValue("gender", "male");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    Properties properties = new Properties()
            .putValue("foo", "bar");
    Traits trackTraits = new Traits()
            .putValue("userId", "123");
    integration.track(new TrackPayloadBuilder().properties(properties).traits(trackTraits).event("event").build());

    attributes.putAll(traits.toStringMap());

    verify(client).track("event", "123", attributes, properties.toStringMap());
  }

  @Test public void pollOptimizelyClient() {
    when(client.isValid()).thenReturn(true);
    Properties properties = new Properties();
    Traits traits = new Traits()
            .putValue("anonymousId", "456");
    integration.trackEvents.add(new TrackPayloadBuilder().properties(properties).traits(traits).event("event").build());
    integration.setClientAndFlushTracks(analytics);

    verify(client).track("event", "456", attributes, properties.toStringMap());
  }

  @Test public void onExperimentActivated() {
    OptimizelyXIntegration.nonInteraction = false;
    String id = "123";
    String experimentKey = "experiment_key";
    List<String> audienceIds = new ArrayList<>();
    List<Variation> variations = new ArrayList<>();
    Map<String, String> userIdToVariationKeyMap = new HashMap<>();
    List<TrafficAllocation> trafficAllocation = new ArrayList<>();
    String groupId = "123";

    String variationKey = "variation_key";
    List<LiveVariableUsageInstance> liveVariableUsageInstancess = new ArrayList<>();

    Experiment experiment = new Experiment(id, experimentKey, null, null, audienceIds, variations, userIdToVariationKeyMap, trafficAllocation, groupId);
    String userId = "123";
    Map<String, String> attributes = new HashMap<>();
    attributes.put("name", "brennan");
    Variation variation = new Variation(id, variationKey, liveVariableUsageInstancess);

    OptimizelyNotificationListener listener = new OptimizelyNotificationListener(analytics);
    listener.onExperimentActivated(experiment, userId, attributes, variation);

    Properties properties = new Properties()
            .putValue("experimentId", "123")
            .putValue("experimentName", "experiment_key")
            .putValue("variationId", "123")
            .putValue("variationName", "variation_key");

    verify(analytics).track("Experiment Viewed", properties, options);
  }

  @Test public void nonInteractionEnabled() {
    String id = "123";
    String experimentKey = "experiment_key";
    List<String> audienceIds = new ArrayList<>();
    List<Variation> variations = new ArrayList<>();
    Map<String, String> userIdToVariationKeyMap = new HashMap<>();
    List<TrafficAllocation> trafficAllocation = new ArrayList<>();
    String groupId = "123";

    String variationKey = "variation_key";
    List<LiveVariableUsageInstance> liveVariableUsageInstancess = new ArrayList<>();

    Experiment experiment = new Experiment(id, experimentKey, null, null, audienceIds, variations, userIdToVariationKeyMap, trafficAllocation, groupId);
    String userId = "123";
    Map<String, String> attributes = new HashMap<>();
    attributes.put("name", "brennan");
    Variation variation = new Variation(id, variationKey, liveVariableUsageInstancess);

    OptimizelyNotificationListener listener = new OptimizelyNotificationListener(analytics);
    listener.onExperimentActivated(experiment, userId, attributes, variation);

    Properties properties = new Properties()
            .putValue("experimentId", "123")
            .putValue("experimentName", "experiment_key")
            .putValue("variationId", "123")
            .putValue("variationName", "variation_key")
            .putValue("nonInteraction", 1);

    verify(analytics).track("Experiment Viewed", properties, options);
  }

  @Test public void listenerDisabled() {
    when(client.isValid()).thenReturn(true);
    integration.listen = false;
    verify(client, times(0)).addNotificationListener(integration.listener);
  }

  @Test public void reset() {
    when(client.isValid()).thenReturn(true);
    integration.reset();
    verify(client).removeNotificationListener(integration.listener);
  }
}
