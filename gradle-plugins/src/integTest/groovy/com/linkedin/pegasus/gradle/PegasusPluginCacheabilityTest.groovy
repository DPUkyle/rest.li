package com.linkedin.pegasus.gradle

import groovy.json.JsonOutput
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.*

class PegasusPluginCacheabilityTest extends Specification {
  @Rule
  TemporaryFolder tempDir = new TemporaryFolder() {
    @Override
    protected void after() { println 'Not cleaning ' + this.root }
  }

  def 'mainDataTemplateJar tasks are up-to-date'() {
    setup:
    tempDir.create()

    def runner = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments("mainDataTemplateJar", '-is')
        //.forwardOutput()
        //.withDebug(true)

    def settingsFile = tempDir.newFile('settings.gradle')
    settingsFile << "rootProject.name = 'test-project'"

    def buildFile = tempDir.newFile('build.gradle')
    buildFile << """
    |plugins { 
    |  id 'pegasus' 
    |}
    |
    |repositories { 
    |  jcenter()
    |}
    |
    |dependencies {
    |  dataTemplateCompile files(${System.getProperty('integTest.dataTemplateCompileDependencies')})
    |  pegasusPlugin files(${System.getProperty('integTest.pegasusPluginDependencies')})
    |}
    """.stripMargin()

    def pegasusDir = tempDir.newFolder('src', 'main', 'pegasus')
    def pdscFile = new File("$pegasusDir.path/ATypeRef.pdsc")
    def pdscData = [
            type: 'typeref',
            name: 'ATypeRef',
            ref:  'string',
            doc:  'A type ref data.'
    ]
    pdscFile << JsonOutput.prettyPrint(JsonOutput.toJson(pdscData))

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

    def compiledSchema = new File(tempDir.root, 'build/classes/java/mainGeneratedDataTemplate/ATypeRef.class')
    compiledSchema.exists()

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
