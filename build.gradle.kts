plugins {
	java
	id("com.github.hierynomus.license") version "0.15.0"
}

allprojects {
  task<Wrapper>("allWrappers") {
    gradleVersion = "7.2"
  }
}

subprojects {
	version = "1.0"
}
