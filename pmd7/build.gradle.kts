// This build file simply downloads the pmd jar files and their dependencies that we care to bundle with the scanner
// plugin and puts them in the dist/pmd7/libs folder.

plugins {
  java // Minimum needed to be able to download dependencies
}

repositories {
  mavenCentral()
}

// Keep this in sync with src/Constants.ts > PMD7_VERSION
var pmd7Version = "7.11.0"

val pmdDist7Dir = "$buildDir/../../dist/pmd7"

dependencies {
  implementation("net.sourceforge.pmd:pmd-apex:$pmd7Version")
  implementation("net.sourceforge.pmd:pmd-cli:$pmd7Version") {
    exclude(group = "net.sourceforge.pmd", module = "pmd-designer")
  }
  implementation("net.sourceforge.pmd:pmd-html:$pmd7Version")
  implementation("net.sourceforge.pmd:pmd-java:$pmd7Version")
  implementation("net.sourceforge.pmd:pmd-javascript:$pmd7Version")
  implementation("net.sourceforge.pmd:pmd-visualforce:$pmd7Version")
  implementation("net.sourceforge.pmd:pmd-xml:$pmd7Version")
}

tasks.register<Copy>("copyDependencies") {
  from(configurations.runtimeClasspath)
  into("$pmdDist7Dir/lib")
}
tasks.assemble {
  dependsOn(tasks.named("copyDependencies"))
}


tasks.register<Delete>("deletePmd7Dist") {
  delete(pmdDist7Dir)
}
tasks.clean {
  dependsOn("deletePmd7Dist")
}
