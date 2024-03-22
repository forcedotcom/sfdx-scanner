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
  implementation("com.google.guava:guava:33.0.0-jre")

  testImplementation("org.mockito:mockito-core:5.10.0")
  testImplementation("org.hamcrest:hamcrest:2.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")

  // Used in unit tests
  testImplementation(files("$buildDir/../../test/test-jars/apex/testjar-categories-and-rulesets-1.jar"))
}


// ======== MODIFY PLUGIN PROPERTIES ===================================================================================
java.sourceCompatibility = JavaVersion.VERSION_11

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


// ======== DEFINE/UPDATE PMD7 DIST RELATED TASKS  =====================================================================
val pmd7DistDir = "$distDir/pmd7"
val pmd7Version = "7.0.0"
val pmd7File = "pmd-dist-$pmd7Version-bin.zip"

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadPmd7") {
  src("https://github.com/pmd/pmd/releases/download/pmd_releases%2F${pmd7Version}/${pmd7File}")
  dest(buildDir)
  overwrite(false)
}

tasks.register<Copy>("installPmd7") {
  dependsOn("downloadPmd7")
  from(zipTree("$buildDir/$pmd7File"))

  // I went to https://github.com/pmd/pmd/tree/pmd_releases/7.0.0 and for each of the languages that we support
  // (apex, java, visualforce, xml), I took a look at its direct and indirect dependencies at
  //     https://central.sonatype.com/artifact/net.sourceforge.pmd/pmd-apex/dependencies
  // by selecting the 7.0.0 dropdown and clicking on "Dependencies" and selecting "All Dependencies".
  // For completeness, I listed the modules and all their compile time dependencies (direct and indirect).
  // Duplicates don't matter since we use setOf.
  val pmd7ModulesToInclude = setOf(
    // LANGUAGE MODULE     DEPENDENCIES (direct and indirect)
    "pmd-apex",            "Saxon-HE", "annotations", "antlr4-runtime", "apex-parser", "apexlink", "asm", "checker-compat-qual", "checker-qual", "checker-qual", "commons-lang3", "error_prone_annotations", "failureaccess", "flogger", "flogger-system-backend", "geny_2.13", "gson", "gson-extras", "guava", "j2objc-annotations", "jsr250-api", "jsr305", "jul-to-slf4j", "kotlin-stdlib", "kotlin-stdlib-common", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "listenablefuture", "nice-xml-messages", "pcollections", "pkgforce_2.13", "pmd-core", "runforce", "scala-collection-compat_2.13", "scala-json-rpc-upickle-json-serializer_2.13", "scala-json-rpc_2.13", "scala-library", "scala-parallel-collections_2.13", "scala-reflect", "scala-xml_2.13", "slf4j-api", "summit-ast", "ujson_2.13", "upack_2.13", "upickle-core_2.13", "upickle-implicits_2.13", "upickle_2.13",
    "pmd-java",            "Saxon-HE", "antlr4-runtime", "asm", "checker-qual", "commons-lang3", "gson", "jul-to-slf4j", "nice-xml-messages", "pcollections", "pmd-core", "slf4j-api",
    "pmd-visualforce",     "Saxon-HE", "antlr4-runtime", "apex-parser", "apexlink", "asm", "checker-compat-qual", "checker-qual", "commons-lang3", "error_prone_annotations", "failureaccess", "flogger", "flogger-system-backend", "geny_2.13", "gson", "gson-extras", "guava", "j2objc-annotations", "jsr250-api", "jsr305", "jul-to-slf4j", "kotlin-stdlib", "kotlin-stdlib-common", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "listenablefuture", "nice-xml-messages", "pcollections", "pkgforce_2.13", "pmd-apex", "pmd-core", "runforce", "scala-collection-compat_2.13", "scala-json-rpc-upickle-json-serializer_2.13", "scala-json-rpc_2.13", "scala-library", "scala-parallel-collections_2.13", "scala-reflect", "scala-xml_2.13", "slf4j-api", "summit-ast", "ujson_2.13", "upack_2.13", "upickle-core_2.13", "upickle-implicits_2.13", "upickle_2.13",
    "pmd-xml",             "Saxon-HE", "antlr4-runtime", "asm", "checker-qual", "commons-lang3", "gson", "jul-to-slf4j", "nice-xml-messages", "pcollections", "pmd-core", "slf4j-api",
    // MAIN CLI MODULE     DEPENDENCIES (direct and indirect)
    "pmd-cli",             "Saxon-HE", "antlr4-runtime", "asm", "checker-qual", "commons-lang3", "gson", "jline", "jul-to-slf4j", "nice-xml-messages", "pcollections", "picocli", "pmd-core", "progressbar", "slf4j-api", "slf4j-simple",
  )
  val pmd7JarsToIncludeRegexes = mutableSetOf("""^LICENSE""".toRegex())
  pmd7ModulesToInclude.forEach {
    pmd7JarsToIncludeRegexes.add("""^$it-.*\.jar""".toRegex())
  }

  include { details: FileTreeElement -> pmd7JarsToIncludeRegexes.any { it.containsMatchIn(details.file.name) } }
  into(pmd7DistDir)
  includeEmptyDirs = false
  eachFile {
    // We drop the parent "pmd-bin-7.0.0" folder and put files directly into our "pmd7" folder
    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
  }
}

tasks.register<Delete>("deletePmd7Dist") {
  delete(pmd7DistDir)
}


// ======== ATTACH TASKS TO ASSEMBLE AND CLEAN  ========================================================================
tasks.assemble {
  dependsOn("installDist")
  dependsOn("installPmd7")
}

tasks.clean {
  dependsOn("deletePmdCatalogerDist")
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
        minimum = BigDecimal("0.78")
      }
    }
  }
}
