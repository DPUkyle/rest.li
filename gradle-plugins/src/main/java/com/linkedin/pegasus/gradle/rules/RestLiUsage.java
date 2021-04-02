package com.linkedin.pegasus.gradle.rules;

import org.gradle.api.Named;
import org.gradle.api.attributes.Attribute;

public interface RestLiUsage extends Named {

  Attribute<RestLiUsage> RESTLI_USAGE_ATTRIBUTE = Attribute.of("com.linkedin.restli.usage", RestLiUsage.class);

  String DATA_TEMPLATE = "dataTemplate";

  String TEST_DATA_TEMPLATE = "testDataTemplate";

}
