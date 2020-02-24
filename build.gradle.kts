subprojects {
    version = "1.0"
}

plugins {
  id("com.moowork.node") version "1.3.1"
}

tasks.register("test") {
  dependsOn("yarn_test")
}
