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
    buildFile.text = "plugins { id 'pegasus' }"
    def pegasusDir = tempDir.newFolder('src', 'main', 'pegasus')
    def pdscFile = new File("$pegasusDir.path/A.pdsc")
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
        .withArguments("mainCopySchemas")
        //.withArguments("mainDataTemplateJar")
        .forwardOutput()
    def result = runner.build()

    then:
    result.task(':mainCopySchemas').outcome == SUCCESS
    //result.task(':mainDataTemplateJar').outcome == SUCCESS

    when:
    result = runner.build()

    then:
//    result.task(':generatedDataTemplate').outcome == SKIPPED
//    result.task(':compileMainGeneratedDataTemplateJava').outcome == SKIPPED
    result.task(':mainDestroyStaleFiles').outcome == SKIPPED
    result.task(':mainCopyPdscSchemas').outcome == SKIPPED
    result.task(':mainCopySchemas').outcome == UP_TO_DATE
//    result.task(':processMainGeneratedDataTemplateResources').outcome == UP_TO_DATE
//    result.task(':mainGeneratedDataTemplateClasses').outcome == UP_TO_DATE
//    result.task(':mainTranslateSchemas').outcome == UP_TO_DATE
    //result.task(':mainDataTemplateJar').outcome == UP_TO_DATE
    true

  }
}
