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
    |  pegasusPlugin 'com.sun.codemodel:codemodel:2.2'
    |  pegasusPlugin 'org.slf4j:slf4j-api:1.7.30'
    |  pegasusPlugin 'com.linkedin.pegasus:pegasus-common:29.3.0'
    |  pegasusPlugin 'com.fasterxml.jackson.core:jackson-core:2.9.7'
    |  pegasusPlugin 'commons-io:commons-io:2.4'
    |  dataTemplateCompile 'com.linkedin.pegasus:data:29.3.0'
    |  pegasusPlugin 'commons-cli:commons-cli:1.0'
    |}
    |repositories {
    |  flatDir {
    |    dirs '/Users/aashtara/dev/git/rest.li/build/data/libs/'
    |    dirs '/Users/aashtara/dev/git/rest.li/build/data-avro-generator/libs/'
    |    dirs '/Users/aashtara/dev/git/rest.li/build/generator/libs/'
    |    dirs '/Users/aashtara/dev/git/rest.li/build/restli-tools/libs/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-simple/1.7.2/760055906d7353ba4f7ce1b8908bc6b2e91f39fa/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/com.sun.codemodel/codemodel/2.2/220bdce6bf4571c42b9a5cd272503df168cbdb60/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/1.7.30/b5a4b6d16ab13e34a88fae84c35cd5d68cac922c/'
    |    dirs '/Users/aashtara/dev/git/rest.li/build/pegasus-common/libs/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-core/2.9.7/4b7f0e0dc527fab032e9800ed231080fdc3ac015/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/commons-io/commons-io/2.4/b1b6ea3b7e4aa4f492509a4952029cd8e48019ad/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/com.google.code.findbugs/jsr305/3.0.2/25ea2e8b0c338a877313bd4672d3fe056ea78f0d/'
    |    dirs '/Users/aashtara/.gradle/caches/modules-2/files-2.1/commons-cli/commons-cli/1.0/6dac9733315224fc562f6268df58e92d65fd0137/'
    |  }
    |}
    '''.stripMargin()

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
        .withDebug(true)
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
