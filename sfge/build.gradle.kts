import java.awt.Desktop

plugins {
	java
	application
	jacoco
	id("com.diffplug.spotless") version "6.11.0"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":cli-messaging"))
	implementation("commons-cli:commons-cli:1.4")
	implementation("org.apache.commons:commons-collections4:4.4")
	implementation("org.apache.tinkerpop:tinkergraph-gremlin:3.5.8")
	implementation("org.apache.tinkerpop:gremlin-driver:3.5.8")
	implementation("org.antlr:antlr-runtime:3.5.2")
	implementation("org.apache.logging.log4j:log4j-api:2.17.1")
	implementation("org.apache.logging.log4j:log4j-core:2.17.1")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.google.guava:guava:33.2.1-jre")
	implementation("com.google.code.findbugs:jsr305:3.0.2")
	implementation ("com.googlecode.json-simple:json-simple:1.1.1") {
		exclude("junit")
	}
	implementation("org.reflections:reflections:0.9.12")
	implementation("org.ow2.asm:asm:9.2")
	implementation(files("lib/apex-jorje-lsp-sfge.jar"))

	testImplementation("org.hamcrest:hamcrest:2.2")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
	testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
	testImplementation("org.mockito:mockito-core:5.2.0")
	testImplementation("org.mockito:mockito-junit-jupiter:5.2.0")
}

group = "com.salesforce.apex"

// Keep this in sync with src/Constants.ts > SFGE_VERSION
version = "1.0.1-pilot"

description = "Salesforce Graph Engine"
java.sourceCompatibility = JavaVersion.VERSION_11


// TODO: This directory should point to dist directory of CLI Scanner
val distDir = "../dist"


application {
	mainClass.set("com.salesforce.Main")
}

spotless {
	java {
		googleJavaFormat().aosp().groupArtifact("com.google.googlejavaformat:google-java-format")
		toggleOffOn()
	}
}

// Running the cli locally needs the dist exploded, so just do that
// automatically with build for ease of use.
tasks.named<Sync>("installDist") {
	into("$distDir/sfge")
	includeEmptyDirs = false
}

tasks.named("assemble") {
	dependsOn("installDist")
}
tasks.build {
	dependsOn("spotlessApply")
}

tasks.test {
	// Use JUnit 5
	useJUnitPlatform()

	// Enables SfgeTestExtension
	systemProperty("junit.jupiter.extensions.autodetection.enabled", true)

	testLogging {
		events("passed", "skipped", "failed")
		// Show log4j output during tests, unless env-var to disable them is set.
		showStandardStreams = (System.getenv("SFGE_LOGGING") != "false")
		// Show extra expected info when there is a failure
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
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

tasks.getByName<Zip>("distZip").enabled = false
tasks.getByName<Tar>("distTar").enabled = false

tasks.jar {
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
