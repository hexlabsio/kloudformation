package io.kloudformation.metadata

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value
import io.kloudformation.json

@DslMarker
annotation class CfnDsl

data class CfnConfigSetRef(@JsonProperty("ConfigSet") val configSet: String) : CfnInit.Value<String>
@JsonSerialize(using = CfnInit.Serializer::class)
data class CfnInit(val configSets: Map<String, List<CfnInit.Value<String>>>, val configs: Map<String, CfnInitConfig>) {

    class Serializer : StdSerializer<CfnInit>(CfnInit::class.java) {
        override fun serialize(value: CfnInit, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            if (value.configSets.isNotEmpty()) {
                generator.writeObjectField("configSets", value.configSets)
            }
            value.configs.forEach { config ->
                generator.writeObjectField(config.key, config.value)
            }
            generator.writeEndObject()
        }
    }

    interface Value<T>
    class Builder(
        private val configSets: MutableMap<String, List<CfnInit.Value<String>>> = mutableMapOf(),
        private val configs: MutableMap<String, CfnInitConfig> = mutableMapOf()
    ) {
        fun configSetRef(configSet: String) = CfnConfigSetRef(configSet)
        fun defaultConfigSet(vararg configs: CfnInit.Value<String>) = configSet("default", *configs)
        fun configSet(name: String, vararg configs: CfnInit.Value<String>) {
            configSets[name] = configs.toList()
        }

        fun defaultConfig(builder: CfnInitConfig.Builder.() -> Unit = {}) = config("config", builder)
        fun config(name: String, builder: CfnInitConfig.Builder.() -> Unit = {}) { configs[name] = CfnInitConfig.Builder().apply(builder).build() }
        fun build(): CfnInit = CfnInit(configSets, configs)
    }
}

fun cfnInitMetadata(builder: CfnInit.Builder.() -> Unit) = "AWS::CloudFormation::Init" to cfnInit(builder)
fun cfnInit(builder: CfnInit.Builder.() -> Unit) = CfnInit.Builder().apply(builder).build()

enum class PackageManager(val value: String) {
    apt("apt"), msi("msi"), python("python"), rpm("rpm"), rubygems("rubygems"), yum("yum")
}

data class CfnInitConfig(
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("packages") val packages: Value<JsonNode>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("groups") val groups: Map<String, CfnGroup>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("users") val users: Map<String, CfnUser>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("sources") val sources: Map<String, Value<String>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("files") val files: Map<String, CfnFile>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("commands") val commands: Map<String, CfnCommand>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("services") val services: Map<String, Map<String, CfnService>>? = null
) {
    class Builder(
        private var packages: Value<JsonNode>? = null,
        private var groups: MutableMap<String, CfnGroup>? = null,
        private var users: MutableMap<String, CfnUser>? = null,
        private var sources: MutableMap<String, Value<String>>? = null,
        private var files: MutableMap<String, CfnFile>? = null,
        private var commands: MutableMap<String, CfnCommand>? = null,
        private var services: MutableMap<String, Map<String, CfnService>>? = null
    ) {

        fun packages(builder: PackagesBuilder.() -> Unit) {
            packages = PackagesBuilder().apply(builder).build()
        }

        fun groups(builder: CfnGroup.GroupsBuilder.() -> Unit) {
            groups = CfnGroup.GroupsBuilder().apply(builder).build().toMutableMap()
        }

        fun files(builder: CfnFile.FilesBuilder.() -> Unit = {}) {
            files = CfnFile.FilesBuilder().apply(builder).build().toMutableMap()
        }

        fun services(builder: CfnService.ServicesBuilder.() -> Unit = {}) {
            services = CfnService.ServicesBuilder().apply(builder).build().toMutableMap()
        }

        fun users(builder: CfnUser.UsersBuilder.() -> Unit = {}) {
            users = CfnUser.UsersBuilder().apply(builder).build().toMutableMap()
        }

        fun source(target: String, sourceUrl: Value<String>) {
            if (sources == null) sources = mutableMapOf()
            sources!![target] = sourceUrl
        }

        fun source(target: String, sourceUrl: String) = source(target, Value.Of(sourceUrl))

        fun command(name: String, command: CfnCommand.Value<String>, builder: CfnCommand.Builder.() -> Unit = {}) {
            if (commands == null) commands = mutableMapOf()
            commands!![name] = CfnCommand.Builder(command).apply(builder).build()
        }
        fun command(name: String, commandParts: List<Value<String>>, builder: CfnCommand.Builder.() -> Unit = {}) {
            if (commands == null) commands = mutableMapOf()
            commands!![name] = CfnCommand.Builder(CfnArrayCommand(*commandParts.toTypedArray())).apply(builder).build()
        }

        fun build() = CfnInitConfig(packages, groups, users, sources, files, commands, services)

        @CfnDsl
        class PackagesBuilder(private val packageManagers: MutableList<Pair<String, Any>> = mutableListOf()) {

            operator fun PackageManager.invoke(packages: PackageBuilder.() -> Unit) = this.value(packages)

            operator fun String.invoke(packages: PackageBuilder.() -> Unit) {
                packageManagers.add(this to PackageBuilder().apply(packages).build())
            }

            fun build() = json(packageManagers.toMap())

            @CfnDsl
            class PackageBuilder(private val packages: MutableList<Pair<String, Any>> = mutableListOf()) {
                operator fun String.invoke(url: String) {
                    packages.add(this to url)
                }
                operator fun String.invoke(versions: List<String>) {
                    packages.add(this to versions)
                }
                fun build() = packages.toMap()
            }
        }
    }
}

data class CfnService(
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("ensureRunning") val ensureRunning: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("enabled") val enabled: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("files") val files: List<Value<String>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("sources") val sources: List<Value<String>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("packages") val packages: Map<String, List<Value<String>>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("commands") val commands: List<Value<String>>? = null
) {
    @CfnDsl
    class ServicesBuilder(private val services: MutableMap<String, Map<String, CfnService>> = mutableMapOf()) {
        operator fun String.invoke(serviceBuilder: ServiceManagerBuilder.() -> Unit) {
            services[this] = ServiceManagerBuilder().apply(serviceBuilder).build()
        }
        fun build(): Map<String, Map<String, CfnService>> = services

        @CfnDsl
        class ServiceManagerBuilder(private val serviceManager: MutableMap<String, CfnService> = mutableMapOf()) {
            operator fun String.invoke(serviceBuilder: ServiceBuilder.() -> Unit) {
                serviceManager[this] = ServiceBuilder().apply(serviceBuilder).build()
            }
            fun build(): Map<String, CfnService> = serviceManager

            @CfnDsl
            class ServiceBuilder(
                private var ensureRunning: Value<String>? = null,
                private var enabled: Value<String>? = null,
                private var files: List<Value<String>>? = null,
                private var sources: List<Value<String>>? = null,
                private var packages: Map<String, List<Value<String>>>? = null,
                private var commands: List<Value<String>>? = null
            ) {
                fun ensureRunning(ensureRunning: Boolean) { this.ensureRunning = Value.Of(ensureRunning.toString()) }
                fun ensureRunning(ensureRunning: Value<String>) { this.ensureRunning = ensureRunning }
                fun enabled(enabled: Boolean) { this.enabled = Value.Of(enabled.toString()) }
                fun enabled(enabled: Value<String>) { this.enabled = enabled }
                fun files(files: List<Value<String>>) { this.files = files }
                fun sources(sources: List<Value<String>>) { this.sources = sources }
                fun packages(vararg packages: Pair<String, List<Value<String>>>) { this.packages = packages.toMap() }
                fun commands(commands: List<Value<String>>) { this.commands = commands }
                fun build() = CfnService(
                        ensureRunning = ensureRunning,
                        enabled = enabled,
                        files = files,
                        sources = sources,
                        packages = packages,
                        commands = commands
                )
            }
        }
    }
}
class CfnArrayCommand(vararg items: Value<String>) : ArrayList<Value<String>>(items.toMutableList()), CfnCommand.Value<String>
data class CfnCommand(
    @JsonProperty("command") val command: CfnCommand.Value<String>,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("env") val env: Map<String, io.kloudformation.Value<String>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("cwd") val cwd: io.kloudformation.Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("test") val test: io.kloudformation.Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("ignoreErrors") val ignoreErrors: io.kloudformation.Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("waitAfterCompletion") val waitAfterCompletion: io.kloudformation.Value<String>? = null
) {
    interface Value<T>
    class Builder(
        private val command: CfnCommand.Value<String>,
        private var env: Map<String, io.kloudformation.Value<String>>? = null,
        private var cwd: io.kloudformation.Value<String>? = null,
        private var test: io.kloudformation.Value<String>? = null,
        private var ignoreErrors: io.kloudformation.Value<String>? = null,
        private var waitAfterCompletion: io.kloudformation.Value<String>? = null
    ) {

        fun env(vararg environment: Pair<String, io.kloudformation.Value<String>>) { env = environment.toMap() }
        fun cwd(cwd: String) { this.cwd = io.kloudformation.Value.Of(cwd) }
        fun cwd(cwd: io.kloudformation.Value<String>) { this.cwd = cwd }
        fun test(test: String) { this.test = io.kloudformation.Value.Of(test) }
        fun test(test: io.kloudformation.Value<String>) { this.test = test }
        fun ignoreErrors(ignoreErrors: Boolean) { this.ignoreErrors = io.kloudformation.Value.Of(ignoreErrors.toString()) }
        fun ignoreErrors(ignoreErrors: io.kloudformation.Value<String>) { this.ignoreErrors = ignoreErrors }
        fun waitAfterCompletion(waitAfterCompletion: String) { this.waitAfterCompletion = io.kloudformation.Value.Of(waitAfterCompletion) }
        fun waitAfterCompletion(waitAfterCompletion: io.kloudformation.Value<String>) { this.waitAfterCompletion = waitAfterCompletion }

        fun build() = CfnCommand(command, env, cwd, test, ignoreErrors, waitAfterCompletion)
    }
}

open class CfnFile(
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("encoding") val encoding: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("owner") val owner: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("group") val group: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("mode") val mode: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("authentication") val authentication: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("context") val context: Value<JsonNode>? = null
) {
    @CfnDsl
    class FilesBuilder(private val files: MutableList<Pair<String, CfnFile>> = mutableListOf()) {
        operator fun String.invoke(content: Value<String>, fileBuilder: FileBuilder.() -> Unit = {}) {
            files.add(this to FileBuilder(content = content).apply(fileBuilder).build())
        }
        fun remote(name: String, source: Value<String>, fileBuilder: FileBuilder.() -> Unit = {}) {
            files.add(name to FileBuilder(source = source).apply(fileBuilder).build())
        }
        fun build() = files.toMap()
        @CfnDsl
        class FileBuilder(
            private val content: Value<String>? = null,
            private val source: Value<String>? = null,
            private var encoding: Value<String>? = null,
            private var owner: Value<String>? = null,
            private var group: Value<String>? = null,
            private var mode: Value<String>? = null,
            private var authentication: Value<String>? = null,
            private var context: Value<JsonNode>? = null
        ) {
            fun encoding(encoding: String) { this.encoding = Value.Of(encoding) }
            fun encoding(encoding: Value<String>) { this.encoding = encoding }
            fun owner(owner: String) { this.owner = Value.Of(owner) }
            fun owner(owner: Value<String>) { this.owner = owner }
            fun group(group: String) { this.group = Value.Of(group) }
            fun group(group: Value<String>) { this.group = group }
            fun mode(mode: String) { this.mode = Value.Of(mode) }
            fun mode(mode: Value<String>) { this.mode = mode }
            fun authentication(authentication: String) { this.authentication = Value.Of(authentication) }
            fun authentication(authentication: Value<String>) { this.authentication = authentication }
            fun context(context: Value<JsonNode>) { this.context = context }
            fun build() = if (content != null)
                CfnFileContent(content = content, encoding = encoding, owner = owner, group = group, mode = mode, authentication = authentication, context = context)
            else CfnRemoteFile(source = source!!, encoding = encoding, owner = owner, group = group, mode = mode, authentication = authentication, context = context)
        }
    }
}
class CfnRemoteFile(
    @JsonProperty("source") val source: Value<String>,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("encoding") encoding: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("owner") owner: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("group") group: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("mode") mode: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("authentication") authentication: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("context") context: Value<JsonNode>? = null
) : CfnFile(
        encoding, owner, group, mode, authentication, context
)
class CfnFileContent(
    @JsonProperty("content") val content: Value<String>,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("encoding") encoding: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("owner") owner: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("group") group: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("mode") mode: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("authentication") authentication: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("context") context: Value<JsonNode>? = null
) : CfnFile(
        encoding, owner, group, mode, authentication, context
)

data class CfnUser(
    @JsonProperty("uid") val uid: Value<String>,
    @JsonProperty("groups") val groups: Value<List<Value<String>>>,
    @JsonProperty("homeDir") val homeDir: Value<String>
) {
    @CfnDsl
    class UsersBuilder(private val users: MutableList<Pair<String, CfnUser>> = mutableListOf()) {
        operator fun String.invoke(uid: Value<String>, groups: Value<List<Value<String>>>, homeDir: Value<String>) {
            users.add(this to CfnUser(uid, groups, homeDir))
        }
        fun build() = users.toMap()
    }
}

interface CfnGroup {
    @CfnDsl
    class GroupsBuilder(private val groups: MutableList<Pair<String, CfnGroup>> = mutableListOf()) {
        operator fun String.invoke(groupBuilder: GroupBuilder.() -> Unit) {
            groups.add(this to GroupBuilder().apply(groupBuilder).build())
        }
        fun build() = groups.toMap()
        @CfnDsl
        class GroupBuilder(private var gid: Value<String>? = null) {
            fun gid(gid: String) { this.gid = Value.Of(gid) }
            fun gid(gid: Value<String>) { this.gid = gid }
            fun build() = if (gid != null) CfnGroupWithId(gid!!) else CfnGroupNoId()
        }
    }
}
data class CfnGroupWithId(
    @JsonProperty("gid") val gid: Value<String>
) : CfnGroup
class CfnGroupNoId : CfnGroup