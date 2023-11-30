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
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("com.google.guava:guava:31.1-jre")
  testImplementation("org.hamcrest:hamcrest:2.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
}

tasks.getByName<Test>("test") {
  useJUnitPlatform()
}
