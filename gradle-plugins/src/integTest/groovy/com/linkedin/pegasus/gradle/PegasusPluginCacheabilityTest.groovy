package com.linkedin.pegasus.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.*;

class PegasusPluginCacheabilityTest extends Specification {
//  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  def 'mainDataTemplateJar tasks are up-to-date'() {
    setup:
    tempDir.create()
    def runner = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments("mainDataTemplateJar")
        .forwardOutput()
        .withDebug(true)
    /*
    |  pegasusPlugin 'com.linkedin.pegasus:data:29.3.0'
    |  pegasusPlugin 'com.linkedin.pegasus:data-avro-generator:29.3.0'
    |  pegasusPlugin 'com.linkedin.pegasus:generator:29.3.0'
    |  pegasusPlugin 'com.linkedin.pegasus:restli-tools:29.3.0'
    |  // below is if i remote the items
     */

    def buildFile = tempDir.newFile('build.gradle')
    buildFile << '''
    |plugins { id 'pegasus' }
    |dependencies {
    |  pegasusPlugin 'com.sun.codemodel:codemodel:2.2'
    |  pegasusPlugin 'org.slf4j:slf4j-api:1.7.30'
    |  pegasusPlugin 'com.linkedin.pegasus:pegasus-common:29.3.0'
    |  pegasusPlugin 'com.fasterxml.jackson.core:jackson-core:2.9.7'
    |  pegasusPlugin 'commons-io:commons-io:2.4'
    |  dataTemplateCompile 'com.linkedin.pegasus:data:29.3.0'
    |  pegasusPlugin 'commons-cli:commons-cli:1.0'
    |}
    '''.stripMargin()
    // Add libs needed to run pegasus plugin
    buildFile << "\nrepositories {\n  flatDir {\n"
    for (File f : runner.pluginClasspath) {
      buildFile << "    dirs '$f'\n"
    }
    buildFile << "  }\n}"

    def pegasusDir = tempDir.newFolder('src', 'main', 'pegasus')
    def pdscFile = new File("$pegasusDir.path/ATypeRef.pdsc")
    pdscFile << '''{
    |  "type"      : "typeref",
    |  "name"      : "ATypeRef",
    |  "ref"       : "string",
    |  "doc"       : "A type ref data."
    |}
    '''.stripMargin()

    when:
    def result = runner.build()

    // Should we allow processMainGeneratedDataTemplateResources task to run?
    // Should we not skip the mainTranslateSchemas task?
    then:
    result.task(':generateDataTemplate').outcome == SUCCESS
    result.task(':compileMainGeneratedDataTemplateJava').outcome == SUCCESS
    result.task(':mainDestroyStaleFiles').outcome == SKIPPED
    result.task(':mainCopyPdscSchemas').outcome == SKIPPED
    result.task(':mainCopySchemas').outcome == SUCCESS
    result.task(':processMainGeneratedDataTemplateResources').outcome == NO_SOURCE
    result.task(':mainGeneratedDataTemplateClasses').outcome ==  SUCCESS
    result.task(':mainTranslateSchemas').outcome == SUCCESS
    result.task(':mainDataTemplateJar').outcome == SUCCESS

    when:
    result = runner.build()

    then:
    result.task(':generateDataTemplate').outcome == UP_TO_DATE
    result.task(':compileMainGeneratedDataTemplateJava').outcome == UP_TO_DATE
    result.task(':mainDestroyStaleFiles').outcome == SKIPPED
    result.task(':mainCopyPdscSchemas').outcome == SKIPPED
    result.task(':mainCopySchemas').outcome == UP_TO_DATE
    result.task(':processMainGeneratedDataTemplateResources').outcome == NO_SOURCE
    result.task(':mainGeneratedDataTemplateClasses').outcome == UP_TO_DATE
    result.task(':mainTranslateSchemas').outcome == UP_TO_DATE
    result.task(':mainDataTemplateJar').outcome == UP_TO_DATE
  }
}
