import java.awt.Desktop

plugins {
  java
  jacoco
}

version = "1.0"
java.sourceCompatibility = JavaVersion.VERSION_11
group = "com.salesforce.messaging"

repositories {
  mavenCentral()
}

dependencies {
  implementation ("com.googlecode.json-simple:json-simple:1.1.1") {
    exclude("junit")
  }
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("com.google.guava:guava:33.2.1-jre")

  testImplementation("org.hamcrest:hamcrest:2.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
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
        minimum = BigDecimal("0.70") // TODO: We should aim to increase this
      }
    }
  }
}
