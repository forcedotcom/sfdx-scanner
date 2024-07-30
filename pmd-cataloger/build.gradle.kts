import java.awt.Desktop

plugins {
  java
  application
  jacoco
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


// ======== ATTACH TASKS TO ASSEMBLE AND CLEAN  ========================================================================
tasks.assemble {
  dependsOn("installDist")
}

tasks.clean {
  dependsOn("deletePmdCatalogerDist")
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
