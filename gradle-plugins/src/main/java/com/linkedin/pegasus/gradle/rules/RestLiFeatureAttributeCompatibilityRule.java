package com.linkedin.pegasus.gradle.rules;

import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;

public class RestLiFeatureAttributeCompatibilityRule implements AttributeCompatibilityRule<RestLiUsage> {

  @Override
  public void execute(CompatibilityCheckDetails<RestLiUsage> details) {
    RestLiUsage producerValue = details.getProducerValue();
    RestLiUsage consumerValue = details.getConsumerValue();
    int x = 42;
  }
}
