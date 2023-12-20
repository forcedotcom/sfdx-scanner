import java.awt.Desktop

plugins {
  java
  application
  jacoco
  id("de.undercouch.download") version "4.0.4"
}

group = "sfdx"
version = "1.0"

val distDir = "$buildDir/../../dist"
val pmdVersion = "6.55.0"
val pmdFile = "pmd-bin-$pmdVersion.zip"
val pmdUrl = "https://github.com/pmd/pmd/releases/download/pmd_releases%2F${pmdVersion}/${pmdFile}"
val skippableJarRegexes = setOf("""^common_[\d\.-]*\.jar""".toRegex(),
  """^fastparse.*\.jar""".toRegex(),
  """^groovy.*\.jar""".toRegex(),
  """^lenses.*\.jar""".toRegex(),
  """^parsers.*\.jar""".toRegex(),
  """^pmd-(cpp|cs|dart|fortran|go|groovy|jsp|kotlin|lua|matlab|modelica|objectivec|perl|php|plsql|python|ruby|scala|swift|ui)[-_\d\.]*\.jar""".toRegex(),
  """^protobuf-java-[\d\.]*\.jar""".toRegex(),
  """^scala.*\.jar""".toRegex(),
  """^sourcecode_[\d\.-]*\.jar""".toRegex(),
  """^trees_[\d\.-]*\.jar""".toRegex()
)

repositories {
  mavenCentral()
  google()
}

jacoco {
  toolVersion = "0.8.7"
}

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadPmd") {
  src(pmdUrl)
  dest(buildDir)
  overwrite(false)
}

tasks.register<Copy>("installPmd") {
  dependsOn("downloadPmd")
  from(zipTree("$buildDir/$pmdFile"))
  exclude { details: FileTreeElement ->
    skippableJarRegexes.any {it.containsMatchIn(details.file.name)}
  }
  into("$distDir/pmd")
  // TODO include("just the *.jars etc. we care about")
  includeEmptyDirs = false
  eachFile {
    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
  }
}

dependencies {
  implementation(project(":cli-messaging"))
  implementation ("com.googlecode.json-simple:json-simple:1.1.1") {
    exclude("junit")
  }
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("com.google.guava:guava:31.1-jre")

  testImplementation("org.mockito:mockito-core:5.2.0")
  testImplementation("org.hamcrest:hamcrest:2.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")

  // Used in unit tests
  testImplementation(files("$buildDir/../../test/test-jars/apex/testjar-categories-and-rulesets-1.jar"))
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

application {
  mainClass.set("sfdc.sfdx.scanner.pmd.Main");
}

// Running the cli locally needs the dist exploded, so just do that
// automatically with build for ease of use.
tasks.named<Sync>("installDist") {
  into("$distDir/pmd-cataloger")
}

tasks.named("assemble") {

  // TODO: These currently do not get cleaned with ./gradlew clean which can cause a lot of confusion.
  dependsOn("installDist")
  dependsOn("installPmd")
}

tasks.test {
  // Use JUnit 5
  useJUnitPlatform()

  testLogging {
    events("passed", "skipped", "failed")
  }

  // Run tests in multiple threads
  maxParallelForks = Runtime.getRuntime().availableProcessors()/2 + 1

  // Report is always generated after test runs
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}

tasks.register("showCoverageReport") {
  group = "verification"
  dependsOn(tasks.jacocoTestReport)
  doLast {
    Desktop.getDesktop().browse(File("$buildDir/reports/jacoco/test/html/index.html").toURI())
  }
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = BigDecimal("0.80")
      }
    }
  }
}
