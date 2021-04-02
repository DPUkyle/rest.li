package com.linkedin.pegasus.gradle.rules;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.ivy.IvyModuleDescriptor;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.repositories.resolver.ComponentMetadataDetailsAdapter;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.component.external.descriptor.Artifact;
import org.gradle.internal.component.external.model.ivy.IvyDependencyDescriptor;
import org.gradle.internal.component.external.model.ivy.IvyModuleResolveMetadata;
import org.gradle.internal.component.external.model.ivy.MutableIvyModuleResolveMetadata;

import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

  private static final Logger LOG = Logging.getLogger(PegasusIvyVariantDerivationRule.class);

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
                attributes.attribute(RestLiUsage.RESTLI_USAGE_ATTRIBUTE, objects.named(RestLiUsage.class, RestLiUsage.DATA_TEMPLATE));
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
              });
              ModuleVersionIdentifier id = context.getDetails().getId();
              variantMetadata.withCapabilities(capabilities -> {
                capabilities.removeCapability(id.getGroup(), id.getName()); // remove implicit capability from dataTemplateRuntimeElements to avoid conflicts with runtimeElements variant
                capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion());
              });
//      variantMetadata.withDependencies(deps -> {
//        //deps.add("com.fake:broken:1.0.0");
//        LOG.lifecycle("*** Setting dependencies");
//        Map<String, String> dependencyMap = new LinkedHashMap<>();
//        dependencyMap.put("group", "com.linkedin.pegasus-grandparent-demo");
//        dependencyMap.put("name", "grandparent");
//        dependencyMap.put("version", "1.0.0");
//        //dependencyMap.put("configuration", "dataTemplate"); // Too many parameters provided for constructor for type DirectDependencyMetadataImpl. Expected 3, received 4.
//        LOG.lifecycle("Adding HACKED transitive dependency to this variant for {}", dependencyMap);
//        deps.add(dependencyMap);
//      });
              variantMetadata.withDependencies(deps -> {
                LOG.lifecycle("Dumping existing dependencies of legacy-based dataTemplateRuntimeElements variant:");
                deps.forEach(ddm -> {
                  LOG.lifecycle(ddm.getModule().toString());
                  //deps.removeIf(dep -> "com.linkedin.pegasus-grandparent-demo:grandparent".equals(dep.getModule().toString()));
                  if ("com.linkedin.pegasus-grandparent-demo:grandparent".equals(ddm.getModule().toString())) {
                    //deps.add("com.linkedin.pegasus-grandparent-demo:grandparent-data-template");
                    deps.add("com.linkedin.pegasus-grandparent-demo:grandparent:" + ddm.getVersionConstraint().getRequiredVersion(), newDep -> {
                      newDep.attributes(attributes -> {
                        attributes.attribute(RestLiUsage.RESTLI_USAGE_ATTRIBUTE, objects.named(RestLiUsage.class, RestLiUsage.DATA_TEMPLATE));
                        //attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
                        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
                        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
                      });
                      //.because("Adding com.linkedin.restli.usage=dataTemplate attribute et. al. to com.linkedin.pegasus-grandparent-demo:grandparent in dataTemplateRuntimeElements");
                    });
                    deps.remove(ddm);
                  }
                });
              });
            });
        /*Field metadata = null;
        List<ModuleComponentSelector> coordinates = new ArrayList<>();
        try {
          metadata = ComponentMetadataDetailsAdapter.class.getDeclaredField("metadata");
          metadata.setAccessible(true);
          IvyModuleResolveMetadata ivyMetadata = ((MutableIvyModuleResolveMetadata) metadata.get(context.getDetails())).asImmutable();
          List<IvyDependencyDescriptor> dependencies = (List<IvyDependencyDescriptor>) IvyModuleResolveMetadata.class.getDeclaredMethod("getDependencies").invoke(ivyMetadata);
          LOG.lifecycle("Dumping metadata dependencies of {}:", id);
          dependencies.forEach(ivyDependencyDescriptor -> {
            LOG.lifecycle(ivyDependencyDescriptor.toString());
            try {
              //            Object confsMultimap = IvyDependencyDescriptor.class.getDeclaredMethod("getConfMappings").invoke(ivyDependencyDescriptor);
              Object confsMultimap = IvyDependencyDescriptor.class.getDeclaredMethod("getConfMappings").invoke(ivyDependencyDescriptor);
              //            Method asMap = Class.forName("org.gradle.internal.impldep.com.google.common.collect.Multimap", false, Thread.currentThread().getContextClassLoader()).getDeclaredMethod("asMap");
              //            Method asMap = Class.forName("com.google.common.collect.Multimap", false, Thread.currentThread().getContextClassLoader()).getDeclaredMethod("asMap");
              Method asMap = Class.forName("com.google.common.collect.Multimap", false, Gradle.class.getClassLoader()).getDeclaredMethod("asMap");
              Map<String, Collection<String>> confsMap = (Map<String, Collection<String>>) asMap.invoke(confsMultimap);
              //            Map<String, Set<String>> confsMap = ((Multimap) IvyDependencyDescriptor.class.getDeclaredMethod("getConfMappings").invoke(ivyDependencyDescriptor)).asMap();
              //LOG.lifecycle("Dumping confsMap:");
              confsMap.forEach((k, v) -> {
                //LOG.lifecycle("{} -> {}", k, String.join(",", v));
                if ("dataModel".equals(k) && "dataTemplate".equals(String.join(",", v))) {
                  LOG.lifecycle("\tFound a candidate to request transitive capability: {}", ivyDependencyDescriptor);
                  coordinates.add(ivyDependencyDescriptor.getSelector());
                }
              });

            } catch (ReflectiveOperationException e) {
              throw new GradleException("Inner fail", e);
            }
          });

        } catch (ReflectiveOperationException e) {
          throw new GradleException("Fail", e);
        }

        coordinates.forEach(selector -> { //MCS
          // TODO reformat to request capability
          Map<String, String> dependencyMap = new LinkedHashMap<>(); //preserve insertion order; nice for debugging
          dependencyMap.put("group", selector.getGroup());
          dependencyMap.put("name", selector.getModule());
          dependencyMap.put("version", selector.getVersion());
          //dependencyMap.put("configuration", "dataTemplate"); // Too many parameters provided for constructor for type DirectDependencyMetadataImpl. Expected 3, received 4.
          LOG.lifecycle("Adding transitive dependency to this variant for {}", dependencyMap);
          deps.add(dependencyMap, dep -> dep.attributes(attributes -> attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, "dataTemplate"))));
        });*/
        //      coordinates.forEach(selector -> { //MCS
        //        // TODO reformat to request capability
        //        Map<String, String> dependencyMap = new LinkedHashMap<>(); //preserve insertion order; nice for debugging
        //        dependencyMap.put("group", selector.getGroup());
        //        dependencyMap.put("name", "foobar"); //selector.getModule());
        //        dependencyMap.put("version", selector.getVersion());
        //        dependencyMap.put("configuration", "dataTemplate");
        //        LOG.lifecycle("Adding transitive dependency to this variant for {}", dependencyMap);
        //        variantMetadata.withDependencies(deps -> deps.add(dependencyMap));
        //        //variantMetadata.withDependencies(deps -> deps. add("", dep -> dep)); // need o.g.a.a.ModuleDependency
        //      });
        //      variantMetadata.withDependencies(deps -> {
        //        LOG.lifecycle("About to dump deps...");
        //        deps.forEach(dep -> LOG.lifecycle("Dumping dep: {}", dep));
        //      });
        //      variantMetadata.withDependencies(deps -> deps.forEach(dep -> System.out.println("Dumping dep: " + dep)));

    context.getDetails().maybeAddVariant("dataTemplateApiElements", "dataTemplate", variantMetadata -> {
      variantMetadata.attributes(attributes -> {
        attributes.attribute(RestLiUsage.RESTLI_USAGE_ATTRIBUTE, objects.named(RestLiUsage.class, RestLiUsage.DATA_TEMPLATE));
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));
      });
      ModuleVersionIdentifier id = context.getDetails().getId();
      variantMetadata.withCapabilities(capabilities -> {
        capabilities.removeCapability(id.getGroup(), id.getName()); // remove implicit capability from dataTemplateApiElements to avoid conflicts with apiElements variant
        capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion());
      });
      variantMetadata.withDependencies(deps -> {
        LOG.lifecycle("Dumping existing dependencies of legacy-based dataTemplateApiElements variant:");
        deps.forEach(ddm -> {
          LOG.lifecycle(ddm.getModule().toString());
          if ("com.linkedin.pegasus-grandparent-demo:grandparent".equals(ddm.getModule().toString())) {
            ddm.attributes(attributes -> {
              attributes.attribute(RestLiUsage.RESTLI_USAGE_ATTRIBUTE, objects.named(RestLiUsage.class, RestLiUsage.DATA_TEMPLATE));
              //attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
              attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
              attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
              attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));
            })
            .because("Adding com.linkedin.restli.usage=dataTemplate attribute et. al. to com.linkedin.pegasus-grandparent-demo:grandparent in dataTemplateApiElements variant");
          }
        });
      });
    });

    /*context.getDetails().maybeAddVariant("dataTemplateApiElements", "dataTemplate", variantMetadata -> {
      variantMetadata.attributes(attributes -> {
        LOG.lifecycle("*** Setting attributes");
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));
      });
      ModuleVersionIdentifier id = context.getDetails().getId();
      variantMetadata.withCapabilities(capabilities -> {
        LOG.lifecycle("*** Setting capabilities");
        capabilities.removeCapability(id.getGroup(), id.getName()); // remove implicit capability from dataTemplateApiElements to avoid conflicts with apiElements variant
        capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion());
      });
      //TODO? introspect dependencies confs mapping; if dataModel->dataTemplate confs mapping exists, request
      //  TODO or just call withDependencies and dump the collection
      variantMetadata.withDependencies(deps -> {
        deps.add("com.fake:broken:1.0.0");
        LOG.lifecycle("*** Setting dependencies");
        Map<String, String> dependencyMap = new LinkedHashMap<>();
        dependencyMap.put("group", "com.linkedin.pegasus-grandparent-demo");
        dependencyMap.put("name", "grandparent");
        dependencyMap.put("version", "1.0.0");
        dependencyMap.put("configuration", "dataTemplate");
        LOG.lifecycle("Adding HACKED transitive dependency to this variant for {}", dependencyMap);
        deps.add(dependencyMap);
      });

      variantMetadata.withDependencies(deps -> {

        Field metadata = null;
        List<ModuleComponentSelector> coordinates = new ArrayList<>();
        try {
          metadata = ComponentMetadataDetailsAdapter.class.getDeclaredField("metadata");
          metadata.setAccessible(true);
          IvyModuleResolveMetadata ivyMetadata = ((MutableIvyModuleResolveMetadata) metadata.get(context.getDetails())).asImmutable();
          List<IvyDependencyDescriptor> dependencies = (List<IvyDependencyDescriptor>) IvyModuleResolveMetadata.class.getDeclaredMethod("getDependencies").invoke(ivyMetadata);
          LOG.lifecycle("Dumping metadata dependencies of {}:", id);
          dependencies.forEach(ivyDependencyDescriptor -> {
            LOG.lifecycle(ivyDependencyDescriptor.toString());
            try {
    //            Object confsMultimap = IvyDependencyDescriptor.class.getDeclaredMethod("getConfMappings").invoke(ivyDependencyDescriptor);
              Object confsMultimap = IvyDependencyDescriptor.class.getDeclaredMethod("getConfMappings").invoke(ivyDependencyDescriptor);
    //            Method asMap = Class.forName("org.gradle.internal.impldep.com.google.common.collect.Multimap", false, Thread.currentThread().getContextClassLoader()).getDeclaredMethod("asMap");
    //            Method asMap = Class.forName("com.google.common.collect.Multimap", false, Thread.currentThread().getContextClassLoader()).getDeclaredMethod("asMap");
              Method asMap = Class.forName("com.google.common.collect.Multimap", false, Gradle.class.getClassLoader()).getDeclaredMethod("asMap");
              Map<String, Collection<String>> confsMap = (Map<String, Collection<String>>) asMap.invoke(confsMultimap);
              //            Map<String, Set<String>> confsMap = ((Multimap) IvyDependencyDescriptor.class.getDeclaredMethod("getConfMappings").invoke(ivyDependencyDescriptor)).asMap();
              //LOG.lifecycle("Dumping confsMap:");
              confsMap.forEach((k, v) -> {
                //LOG.lifecycle("{} -> {}", k, String.join(",", v));
                if ("dataModel".equals(k) && "dataTemplate".equals(String.join(",", v))) {
                  LOG.lifecycle("\tFound a candidate to request transitive capability: {}", ivyDependencyDescriptor);
                  coordinates.add(ivyDependencyDescriptor.getSelector());
                }
              });

            } catch (ReflectiveOperationException e) {
              throw new GradleException("Inner fail", e);
            }
          });

        } catch (ReflectiveOperationException e) {
          throw new GradleException("Fail", e);
        }

        coordinates.forEach(selector -> { //MCS
          // TODO reformat to request capability
          Map<String, String> dependencyMap = new LinkedHashMap<>(); //preserve insertion order; nice for debugging
          dependencyMap.put("group", selector.getGroup());
          dependencyMap.put("name", "foobar"); //selector.getModule());
          dependencyMap.put("version", selector.getVersion());
          dependencyMap.put("configuration", "dataTemplate");
          LOG.lifecycle("Adding transitive dependency to this variant for {}", dependencyMap);
          deps.add(dependencyMap);
        });
  //      coordinates.forEach(selector -> { //MCS
  //        // TODO reformat to request capability
  //        Map<String, String> dependencyMap = new LinkedHashMap<>(); //preserve insertion order; nice for debugging
  //        dependencyMap.put("group", selector.getGroup());
  //        dependencyMap.put("name", "foobar"); //selector.getModule());
  //        dependencyMap.put("version", selector.getVersion());
  //        dependencyMap.put("configuration", "dataTemplate");
  //        LOG.lifecycle("Adding transitive dependency to this variant for {}", dependencyMap);
  //        variantMetadata.withDependencies(deps -> deps.add(dependencyMap));
  //        //variantMetadata.withDependencies(deps -> deps. add("", dep -> dep)); // need o.g.a.a.ModuleDependency
  //      });
  //      variantMetadata.withDependencies(deps -> {
  //        LOG.lifecycle("About to dump deps...");
  //        deps.forEach(dep -> LOG.lifecycle("Dumping dep: {}", dep));
  //      });
  //      variantMetadata.withDependencies(deps -> deps.forEach(dep -> System.out.println("Dumping dep: " + dep)));
      });
    });
*/
//    context.getDetails().maybeAddVariant("dataTemplateRuntimeElements", "mainGeneratedDataTemplateRuntimeElements", variantMetadata -> {
//      variantMetadata.attributes(attributes -> {
//        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
//        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
//        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
//      });
//      ModuleVersionIdentifier id = context.getDetails().getId();
//      variantMetadata.withCapabilities(capabilities -> capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion()));
//    });

//    context.getDetails().maybeAddVariant("dataTemplateApiElements", "mainGeneratedDataTemplateApiElements", variantMetadata -> {
//      variantMetadata.attributes(attributes -> {
//        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
//        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
//        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_API));
//      });
//      ModuleVersionIdentifier id = context.getDetails().getId();
//      variantMetadata.withCapabilities(capabilities -> capabilities.addCapability(id.getGroup(), id.getName() + "-data-template", id.getVersion()));
//    });

  }
}
