analytics-android-integration-optimizelyx
========================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/optimizelyx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/optimizelyx)
[![Javadocs](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/optimizelyx.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/optimizelyx)

Optimizely X integration for [analytics-android](https://github.com/segmentio/analytics-android).

## Installation

To install the Segment-OptimizelyX integration, simply add this line to your gradle file:

```
compile 'com.segment.analytics.android.integrations:optimizelyx:1.0.0-beta'
```

## Usage

After adding the dependency, you must register the integration with our SDK. To do this, import the Optimizely X integration:

```
import com.segment.analytics.android.integrations.optimizelyx.OptimizelyXIntegration;

```

Since the Optimizely X manager should be initialized as soon as possible in your application subclass, we leave it up to you to create this instance. You must then pass it to Segment's factory:

```
Analytics analytics = new Analytics.Builder(context, writeKey)
  .use(OptimizelyXIntegration.factory(optimizelyManager))
  ...
  .build();
```

Note the different syntax used here - instead of using a global singleton instance, we use a method that creates the factory on demand.

Please see [our documentation](https://segment.com/docs/destinations/optimizelyx/) for more information.

## License

```
WWWWWW||WWWWWW
 W W W||W W W
      ||
    ( OO )__________
     /  |           \
    /o o|    MIT     \
    \___/||_||__||_|| *
         || ||  || ||
        _||_|| _||_||
       (__|__|(__|__|

The MIT License (MIT)

Copyright (c) 2014 Segment, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
