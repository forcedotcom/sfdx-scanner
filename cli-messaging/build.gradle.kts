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
    implementation("com.google.code.gson:gson:2.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
