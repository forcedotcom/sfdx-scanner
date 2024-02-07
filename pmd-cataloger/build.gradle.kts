import java.awt.Desktop

plugins {
  java
  application
  jacoco
  id("de.undercouch.download") version "4.0.4"
}

group = "sfdx"
version = "1.0"

repositories {
  mavenCentral()
  google()
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


// ======== MODIFY PLUGIN PROPERTIES ===================================================================================
java.sourceCompatibility = JavaVersion.VERSION_1_8

application {
  mainClass.set("sfdc.sfdx.scanner.pmd.Main");
}

jacoco {
  toolVersion = "0.8.7"
}

val distDir = "$buildDir/../../dist"


// ======== DEFINE/UPDATE PMD-CATALOGER DIST RELATED TASKS =============================================================
val pmdCatalogerDistDir = "$distDir/pmd-cataloger"

tasks.named<Sync>("installDist") {
  // The installDist task comes with the distribution plugin which comes with the applciation plugin. We modify it here:
  into(pmdCatalogerDistDir)
}

tasks.register<Delete>("deletePmdCatalogerDist") {
	delete(pmdCatalogerDistDir)
}

// ======== DEFINE/UPDATE PMD6 DIST RELATED TASKS  =====================================================================
val pmd6DistDir = "$distDir/pmd"
val pmd6Version = "6.55.0"
val pmd6File = "pmd-bin-$pmd6Version.zip"

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadPmd6") {
  src("https://github.com/pmd/pmd/releases/download/pmd_releases%2F${pmd6Version}/${pmd6File}")
  dest(buildDir)
  overwrite(false)
}

tasks.register<Copy>("installPmd6") {
  dependsOn("downloadPmd6")
  from(zipTree("$buildDir/$pmd6File"))

  val skippablePmd6JarRegexes = setOf("""^common_[\d\.-]*\.jar""".toRegex(),
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
  exclude { details: FileTreeElement ->
    skippablePmd6JarRegexes.any {it.containsMatchIn(details.file.name)}
  }

  into(pmd6DistDir)
  // TODO include("just the *.jars etc. we care about")
  includeEmptyDirs = false
  eachFile {
    // We drop the parent "pmd-bin-6.55.0" folder and put files directly into our "pmd" folder
    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
  }
}

tasks.register<Delete>("deletePmd6Dist") {
  delete(pmd6DistDir)
}


// ======== DEFINE/UPDATE PMD7 DIST RELATED TASKS  =====================================================================
val pmd7DistDir = "$distDir/pmd7"
val pmd7Version = "7.0.0-rc4"
val pmd7File = "pmd-dist-$pmd7Version-bin.zip"

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadPmd7") {
  src("https://github.com/pmd/pmd/releases/download/pmd_releases%2F${pmd7Version}/${pmd7File}")
  dest(buildDir)
  overwrite(false)
}

tasks.register<Copy>("installPmd7") {
  dependsOn("downloadPmd7")
  from(zipTree("$buildDir/$pmd7File"))
  // TODO: We will soon optimize this with W-14980337 by reducing the dist down to only the jars we care about.
  into(pmd7DistDir)
  includeEmptyDirs = false
  eachFile {
    // We drop the parent "pmd-bin-7.0.0-rc4" folder and put files directly into our "pmd7" folder
    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
  }
}

tasks.register<Delete>("deletePmd7Dist") {
  delete(pmd7DistDir)
}


// ======== ATTACH TASKS TO ASSEMBLE AND CLEAN  ========================================================================
tasks.assemble {
  dependsOn("installDist")
  dependsOn("installPmd6")
  dependsOn("installPmd7")
}

tasks.clean {
	dependsOn("deletePmdCatalogerDist")
  dependsOn("deletePmd6Dist")
  dependsOn("deletePmd7Dist")
}


// ======== TEST RELATED TASKS =========================================================================================
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
