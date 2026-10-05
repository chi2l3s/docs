plugins {
    kotlin("jvm") version "2.4.20"
    id("io.github.thirtyeighttwentysix.volan") version "1.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
    implementation(platform("io.github.thirtyeighttwentysix:volan-bom:1.0.0"))
    implementation("io.github.thirtyeighttwentysix:volan-dialect-h2")
    implementation("io.github.thirtyeighttwentysix:volan-migrate")
    runtimeOnly("com.h2database:h2:2.5.252")
    // The contract suite also exercises SQLite:
    testImplementation("io.github.thirtyeighttwentysix:volan-dialect-sqlite")
    runtimeOnly("org.xerial:sqlite-jdbc:3.53.4.0")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.20")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin { jvmToolchain(25) }
tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
tasks.register<JavaExec>("runKotlinExample") {
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.example.MainKt")
}
tasks.register<JavaExec>("runJavaExample") {
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("Main")
}
