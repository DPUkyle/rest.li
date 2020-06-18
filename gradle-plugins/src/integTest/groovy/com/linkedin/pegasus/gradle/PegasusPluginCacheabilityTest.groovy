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
    def buildFile = tempDir.newFile('build.gradle')
    buildFile << '''
    |plugins { id 'pegasus' }
    |dependencies {
    |  dataTemplateCompile "com.linkedin.pegasus:data:27.7.18"
    |  restClientCompile "com.linkedin.pegasus:restli-client:27.7.18"
    |  pegasusPlugin "com.linkedin.pegasus:data:27.7.18"
    |  pegasusPlugin "com.linkedin.pegasus:data-avro-generator:27.7.18"
    |  pegasusPlugin "com.linkedin.pegasus:generator:27.7.18"
    |  pegasusPlugin "com.linkedin.pegasus:generator-test:27.7.18"
    |  pegasusPlugin "com.linkedin.pegasus:restli-tools:27.7.18"
    |  pegasusPlugin "javax.annotation:javax.annotation-api:1.3.1"
    |}
    |allprojects {
    |  repositories {
    |    jcenter() 
    |  }
    |  tasks.matching { it.name == "mainTranslateSchemas"}.each { it.enabled = false }
    |}
    '''.stripMargin()

//    def resources = tempDir.newFolder('resources')
//    def propFile = tempDir.newFile('resources/pegasus-version.properties')
//    propFile << '''pegasus.version=27.7.18'''

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
    def runner = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments("mainDataTemplateJar")
        .forwardOutput()
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
    result.task(':mainTranslateSchemas').outcome == SKIPPED
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
    result.task(':mainTranslateSchemas').outcome == SKIPPED
    result.task(':mainDataTemplateJar').outcome == UP_TO_DATE
  }
}
