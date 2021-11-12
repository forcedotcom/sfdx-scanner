plugins {
  java
  application
  jacoco
  id("de.undercouch.download") version "4.0.4"
}

group = "sfdx"
version = "1.0"

val distDir = "$buildDir/../../dist"
val pmdVersion = "6.38.0"
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
  implementation ("com.googlecode.json-simple:json-simple:1.1.1")
  implementation("com.google.code.gson:gson:2.3")
  implementation("com.google.guava:guava:28.0-jre")
  testImplementation("org.mockito:mockito-core:1.+")
  testImplementation("junit", "junit", "4.12")
  testImplementation("org.hamcrest:hamcrest:2.1")
  // Used in unit tests
  testImplementation(files("$buildDir/../../test/test-jars/apex/testjar-categories-and-rulesets-1.jar"))
}

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
  mainClassName = "sfdc.sfdx.scanner.pmd.Main"
}

// Running the cli locally needs the dist exploded, so just do that
// automatically with build for ease of use.
tasks.named<Sync>("installDist") {
  into("$distDir/pmd-cataloger")
}

tasks.named("assemble") {
  dependsOn("installDist")
  dependsOn("installPmd")
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport) // Report is always generated after test runs.
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = "0.80".toBigDecimal()
      }
    }
  }
}
