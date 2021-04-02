package com.linkedin.pegasus.gradle.rules;

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;

import java.util.Set;

public class RestLiFeatureAttributeDisambiguationRule implements AttributeDisambiguationRule<RestLiUsage> {
  @Override
  public void execute(MultipleCandidatesDetails<RestLiUsage> details) {
    Set<RestLiUsage> candidateValues = details.getCandidateValues();
    int x = 42;
  }
}
