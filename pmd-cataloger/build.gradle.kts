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

  // I went to https://github.com/pmd/pmd/tree/pmd_releases/6.55.0 and for each of the languages that we support
  // (apex, java, visualforce, xml), I took a look at its pom file (like pmd-apex/src/pom.xml for example).
  // That pom said what other modules it depends on and I listed these dependencies (besides test-scoped and  optional
  // modules). I did this recursively for any dependent pmd-* modules and put it all here.
  // For completeness, I listed the modules and all their dependencies. Duplicates don't matter since we use setOf.
  val pmd6ModulesToInclude = setOf(
    // LANGUAGE MODULE     DEPENDENCIES
    "pmd-apex",            "pmd-core", "antlr-runtime", "pmd-apex-jorje", "commons-lang3",
    "pmd-java",            "pmd-core", "saxon", "asm", "commons-lang3",
    "pmd-visualforce",     "pmd-core", "pmd-apex",
    "pmd-xml",             "pmd-core", "antlr4-runtime", "saxon",
    // DEPENDENT MODULE    DEPENDENCIES
    "pmd-core",            "antlr4-runtime", "jcommander", "saxon", "commons-lang3", "asm", "gson",
    "pmd-apex-jorje",      "cglib", "logback-classic", "logback-core", "jsr305", "gson", "error_prone_annotations", "guava", "j2objc-annotations", "antlr-runtime", "stringtemplate", "common-lang3", "animal-sniffer-annotations", "jol-core", "slf4j-api", "snakeyaml", "aopalliance", "javax.inject", "asm"
  )

  val pmd6JarsToIncludeRegexes = mutableSetOf("""^LICENSE""".toRegex())
  pmd6ModulesToInclude.forEach {
    pmd6JarsToIncludeRegexes.add("""^$it-.*\.jar""".toRegex())
  }

  include { details: FileTreeElement -> pmd6JarsToIncludeRegexes.any { it.containsMatchIn(details.file.name) } }
  into(pmd6DistDir)
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

  // I went to https://github.com/pmd/pmd/tree/pmd_releases/7.0.0-rc4 and for each of the languages that we support
  // (apex, java, visualforce, xml), I took a look at its pom file (like pmd-apex/src/pom.xml for example).
  // That pom said what other modules it depends on and I took these dependencies (besides test-scoped and  optional
  // modules). I did this recursively for any dependent pmd-* modules and put it all here.
  // For completeness, I listed the modules and all their dependencies except for pmd-ui (since it isn't needed) and
  // the dependencies of pmd-languages-deps (since it contains all language modules). I also had to add in pmd-cli.
  // Note that "pkgforce_2.13" was missing from pmd-apex, so I added it in and that unfortunately required me to pull
  // in all of its dependencies listed at https://central.sonatype.com/artifact/com.github.nawforce/pkgforce_2.13/dependencies.
  // Duplicates don't matter since we use setOf.
  val pmd7ModulesToInclude = setOf(
    // LANGUAGE MODULE     DEPENDENCIES
    "pmd-apex",            "pmd-core", "antlr-runtime", "pmd-apex-jorje", "apex-link", "commons-lang3", "guava", "pkgforce_2.13",
    "pmd-java",            "pmd-core", "asm", "commons-lang3", "checker-qual", "Saxon-HE", "pcollections",
    "pmd-visualforce",     "pmd-core", "pmd-apex",
    "pmd-xml",             "pmd-core", "antlr4-runtime",
    // MAIN CLI MODULE     DEPENDENCIES
    "pmd-cli",             "pmd-languages-deps", "slf4j-api", "slf4j-simple", "picocli", "progressbar", "checker-qual",
    // DEPENDENT MODULE    DEPENDENCIES
    "pmd-core",            "slf4j-api", "jul-to-slf4j", "antlr4-runtime", "Saxon-HE", "commons-lang3", "asm", "gson", "checker-qual", "pcollections", "nice-xml-messages",
    "pmd-apex-jorje",      "cglib", "jsr305", "gson", "error_prone_annotations", "guava", "j2objc-annotations", "antlr-runtime", "stringtemplate", "commons-lang3", "animal-sniffer-annotations", "slf4j-api", "aopalliance", "javax.inject", "asm",
    "pkgforce_2.13",       "scala-json-rpc-upickle-json-serializer_2.13", "scala-json-rpc_2.13", "geny_2.13", "ujson_2.13", "upack_2.13", "upickle-core_2.13", "upickle-implicits_2.13", "upickle_2.13", "apex-parser", "antlr4-runtime", "scala-collection-compat_2.13", "scala-xml_2.13", "scala-library", "scala-reflect"
  )
  val pmd7JarsToIncludeRegexes = mutableSetOf("""^LICENSE""".toRegex())
  pmd7ModulesToInclude.forEach {
    pmd7JarsToIncludeRegexes.add("""^$it-.*\.jar""".toRegex())
  }

  include { details: FileTreeElement -> pmd7JarsToIncludeRegexes.any { it.containsMatchIn(details.file.name) } }
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
