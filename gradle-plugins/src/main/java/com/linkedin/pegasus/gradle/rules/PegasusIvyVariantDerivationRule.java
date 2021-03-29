package com.linkedin.pegasus.gradle.rules;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ivy.IvyModuleDescriptor;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.repositories.resolver.ComponentMetadataDetailsAdapter;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.component.external.model.ivy.IvyDependencyDescriptor;
import org.gradle.internal.component.external.model.ivy.IvyModuleResolveMetadata;
import org.gradle.internal.component.external.model.ivy.MutableIvyModuleResolveMetadata;
import org.gradle.internal.impldep.com.google.common.collect.Multimap;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rule for deriving Gradle variants from a software component which publishes pegasus jars.
 *
 * <p>Instead of consuming the dataTemplate configuration directly, this rule adds a "-data-template" capability to the
 * primary GAV coordinates of the component.
 *
 * <p>build.gradle usage example before this change:
 * <pre>
 *   configurations {
 *    dataModel group: 'com.acme.foo', name: 'foo', version: '1.0.0', configuration: 'dataTemplate'
 *   }
 * </pre>
 *
 * build.gradle usage example with this rule:
 * <pre>
 *   configurations {
 *     dataModel ('com.acme.foo:foo:1.0.0') {
 *       capabilities {
 *         requireCapability('com.acme.foo:foo-data-template:1.0.0')
 *       }
 *     }
 *     components.all(com.linkedin.pegasus.gradle.rules.PegasusIvyVariantDerivationRule)
 *   }
 * </pre>
 *
 */
public class PegasusIvyVariantDerivationRule implements ComponentMetadataRule {

  private final ObjectFactory objects;

  @Inject
  public PegasusIvyVariantDerivationRule(ObjectFactory objects) {
    this.objects = objects;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void execute(ComponentMetadataContext context) {
    if (context.getDescriptor(IvyModuleDescriptor.class) == null) {
      return; // this component's metadata is not Ivy-based; bail out
    }

    // for backwards-compatibility with older Ivy descriptors, first try to derive variants from the dataTemplate configuration
    context.getDetails().maybeAddVariant("dataTemplateRuntimeElements", "dataTemplate", variantMetadata -> {
      variantMetadata.attributes(attributes -> {
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
      });
      ModuleVersionIdentifier id = context.getDetails().getId();
      variantMetadata.withCapabilities(capabilities -> {
        capabilities.removeCapability(id.getGroup(), id.getName()); // remove implicit capability from dataTemplateRuntimeElements to avoid conflicts with runtimeElements variant
        capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion());
      });
    });

    context.getDetails().maybeAddVariant("dataTemplateApiElements", "dataTemplate", variantMetadata -> {
      variantMetadata.attributes(attributes -> {
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));
      });
      ModuleVersionIdentifier id = context.getDetails().getId();
      variantMetadata.withCapabilities(capabilities -> {
        capabilities.removeCapability(id.getGroup(), id.getName()); // remove implicit capability from dataTemplateApiElements to avoid conflicts with apiElements variant
        capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion());
      });
    });

    context.getDetails().maybeAddVariant("dataTemplateRuntimeElements", "mainGeneratedDataTemplateRuntimeElements", variantMetadata -> {
      variantMetadata.attributes(attributes -> {
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
      });
      ModuleVersionIdentifier id = context.getDetails().getId();
      variantMetadata.withCapabilities(capabilities -> capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion()));
    });

    context.getDetails().maybeAddVariant("dataTemplateApiElements", "mainGeneratedDataTemplateApiElements", variantMetadata -> {
      variantMetadata.attributes(attributes -> {
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));
      });
      ModuleVersionIdentifier id = context.getDetails().getId();
      variantMetadata.withCapabilities(capabilities -> capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion()));
    });

  }
}
