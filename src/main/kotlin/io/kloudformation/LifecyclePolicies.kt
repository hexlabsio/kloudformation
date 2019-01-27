package io.kloudformation

import com.fasterxml.jackson.annotation.JsonInclude

data class CreationPolicy(
    @JsonInclude(JsonInclude.Include.NON_NULL) val autoScalingCreationPolicy: AutoScalingCreationPolicy? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val resourceSignal: ResourceSignal? = null
) {
    data class AutoScalingCreationPolicy(
        @JsonInclude(JsonInclude.Include.NON_NULL) val minSuccessfulInstancesPercent: Value<Int>? = null
    )
    data class ResourceSignal(
        @JsonInclude(JsonInclude.Include.NON_NULL) val count: Value<Int>? = null,
        @JsonInclude(JsonInclude.Include.NON_NULL) val timeout: Value<String>? = null
    )
}

enum class DeletionPolicy(val policy: String) { DELETE("Delete"), RETAIN("Retain"), SNAPSHOT("Snapshot") }

data class UpdatePolicy(
    @JsonInclude(JsonInclude.Include.NON_NULL) val autoScalingRollingUpdate: AutoScalingRollingUpdate? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val autoScalingReplacingUpdate: AutoScalingReplacingUpdate? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val autoScalingScheduledAction: AutoScalingScheduledAction ? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val codeDeployLambdaAliasUpdate: CodeDeployLambdaAliasUpdate? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val useOnlineResharding: Value<Boolean>? = null
)

data class AutoScalingReplacingUpdate(
    @JsonInclude(JsonInclude.Include.NON_NULL) val willReplace: Value<Boolean>? = null
)

data class AutoScalingRollingUpdate(
    @JsonInclude(JsonInclude.Include.NON_NULL) val maxBatchSize: Value<Int>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val minInstancesInService: Value<Int>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val minSuccessfulInstancesPercent: Value<Int>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val pauseTime: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val suspendProcesses: Value<List<String>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val waitOnResourceSignals: Value<Boolean>? = null
)

data class AutoScalingScheduledAction(
    @JsonInclude(JsonInclude.Include.NON_NULL) val IgnoreUnmodifiedGroupSizeProperties: Value<Boolean>? = null
)

data class CodeDeployLambdaAliasUpdate(
    val applicationName: Value<String>,
    val deploymentGroupName: Value<String>,
    @JsonInclude(JsonInclude.Include.NON_NULL) val afterAllowTrafficHook: Value<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val beforeAllowTrafficHook: Value<String>? = null
)