// This build file simply downloads the pmd jar files and their dependencies that we care to bundle with the scanner
// plugin and puts them in the dist/pmd7/libs folder.

plugins {
  java // Minimum needed to be able to download dependencies
}

repositories {
  mavenCentral()
}

// Keep this in sync with src/Constants.ts > PMD7_VERSION
var pmd7Version = "7.9.0"

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

// TEMPORARY - FOR SOME REASON WHEN UPGRADING TO PMD 7.9.0, THE TRANSITIVE DEPENDENCY:
//    io.github.apex-dev-tools:apex-parser
// IS GETTING PULLED IN AS 4.3.1 INSTEAD OF THE LISTED 4.3.0 AND IT SEEMS TO HAVE A BUG: A MISSING DEPENDENCY LISTED.
// SO WE SHOULD FORCE 4.3.0 TO GET PULLED IN INSTEAD UNTIL THIS IS FIXED.
// See https://github.com/pmd/pmd/issues/5456
// TODO: As soon as the pmd folks fix this ^... we should remove this workaround:
configurations.all {
  resolutionStrategy {
    force("io.github.apex-dev-tools:apex-parser:4.3.0")
  }
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
