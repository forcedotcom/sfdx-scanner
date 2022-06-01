plugins {
  java
}

version = "1.0"
java.sourceCompatibility = JavaVersion.VERSION_1_8
group = "com.salesforce.messaging"

repositories {
  mavenCentral()
}

dependencies {
  implementation ("com.googlecode.json-simple:json-simple:1.1.1") {
    exclude("junit")
  }
  implementation("com.google.code.gson:gson:2.3")
  testImplementation("junit", "junit", "4.12")
  implementation("com.google.guava:guava:28.0-jre")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
  useJUnitPlatform()
}
