plugins {
    kotlin("jvm") version "2.4.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val generator = sourceSets.create("generator")
val generated = layout.buildDirectory.dir("generated/volan")

dependencies {
    implementation(platform("io.github.thirtyeighttwentysix:volan-bom:0.1.0-alpha.2"))
    implementation("io.github.thirtyeighttwentysix:volan-runtime")
    implementation("io.github.thirtyeighttwentysix:volan-dialect-sqlite")
    runtimeOnly("org.xerial:sqlite-jdbc:3.53.4.0")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.20")

    add(generator.implementationConfigurationName,
        "io.github.thirtyeighttwentysix:volan-codegen:0.1.0-alpha.2")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
    sourceSets.main {
        kotlin.srcDir(generated)
    }
}

val generateClient = tasks.register<JavaExec>("generateClient") {
    classpath = generator.runtimeClasspath
    mainClass.set("org.example.GenerateClient")
    inputs.file("schema.volan")
    outputs.dir(generated)
    args(
        layout.projectDirectory.file("schema.volan").asFile.absolutePath,
        generated.get().asFile.absolutePath
    )
}

tasks.named("compileKotlin") {
    dependsOn(generateClient)
}

tasks.test {
    useJUnitPlatform()
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
