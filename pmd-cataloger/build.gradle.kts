plugins {
  `java-library`
  application
}

group = "sfdx"
version = "1.0"

repositories {
  mavenCentral()
  google()
}

dependencies {
  implementation("com.googlecode.json-simple:json-simple:1.1.1")
  testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
  mainClassName = "sfdc.sfdx.scanner.pmd.Main"
}

// Running the cli locally needs the dist exploded, so just do that
// automatically with build for ease of use.
tasks.named("build") { dependsOn("installDist") }
