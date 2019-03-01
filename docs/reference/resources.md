---
layout: default
title: Resources
parent: Reference
nav_order: 5
---
<script src="https://unpkg.com/kotlin-playground@1" data-selector=".kotlin"></script>
<style>
blockquote{
    color: #666;
    margin: 0;
    padding-left: 3em;
    border-left: 0.5em #f2c152 solid;
}
blockquote>blockquote{
    border: 0;
    padding-left: 0;
    color: #a5a5a5;
    font-size: 0.8em;
}
</style>

## Table of contents
{: .no_toc .text-delta }

* TOC
{:toc}

# Resources

The Resources section defines the AWS Resources that you wish to create using this template.
All of the AWS Resources that are present in CloudFormation also appear in KloudFormation.

> Info on Resources from AWS can be found  [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html)

Within the curly braces of the `KloudFormation.create()` function you have access to all of the resource types.
To see a list in IntelliJ you can press the hotkey for code completion (Ctrl + Space on Mac).

Each resource has a function of the same name. That function has a receiver of type `KloudFormation` meaning that the function is available inside the `{ }` braces

The following will create an S3 Bucket:

<pre class="kotlin" data-highlight-only>
override fun KloudFormation.create() {
    bucket()
}
</pre>

This produces the following template:

```yaml
Resources:
  Bucket:
    Type: "AWS::S3::Bucket"
```

## Resource Parameters

Each resource takes the following parameters:
 
 * Required Resource Properties
 * `logicalName`
 * `builder`
 * `dependsOn`
 * `resourceProperties`
 
### Required Resource Properties

<pre class="kotlin" data-highlight-only>
class Stack: StackBuilder {
    override fun KloudFormation.create() {
        topic() // No required properties
        vPC(cidrBlock = +"0.0.0.0/0") // cidrBlock is required
    }
}
</pre>
 
### Logical Name

The Logical Name parameter determines what the resource will be named inside your stack. This will appear in the CloudFormation console.

Logical names are allocated for you if you do not set one and will appear as the capitalised name of the Resource followed by a number that increments.

Here is an example with three S3 Buckets:

<pre class="kotlin" data-highlight-only>
bucket()
bucket()
bucket(logicalName = "MyBucket")
</pre>

Produces

```yaml
Bucket:
  Type: "AWS::S3::Bucket"
Bucket2:
  Type: "AWS::S3::Bucket"
MyBucket:
  Type: "AWS::S3::Bucket"
```

### Optional Resource Properties

The builder is always the last parameter and is a function so can be passed as curly braces.

Any Properties that are optional appear as functions within the resource Builder which is passed to that function.

For example `bucketName` is optional so appears within the `{ }` braces

<pre class="kotlin" data-highlight-only>
bucket { 
    bucketName("MyBucket")
}
</pre>

### Resource Dependencies

You can pass a list of logical names to the `dependsOn` parameter so that CloudFormation creates the resources in the correct order.

<pre class="kotlin" data-highlight-only>
val topic = topic()
val q = queue(dependsOn = listOf(topic.logicalName))
</pre>

This Produces

```yaml
Resources:
  Topic:
    Type: "AWS::SNS::Topic"
  Queue:
    Type: "AWS::SQS::Queue"
    DependsOn:
    - "Topic"
```

This can also be achieved by chaining with the `then()` function.

<pre class="kotlin" data-highlight-only>
topic().then{ queue() }
</pre>

Or, for multiple dependencies:

<pre class="kotlin" data-highlight-only>
(topic() and topic()).then{ queue()  }
</pre>

### Resource Properties

Other metadata can be passed to the `resourceProperties` parameter.

ResourceProperties takes the following optional parameters:

* condition
* metadata
* updatePolicy
* creationPolicy
* deletionPolicy
* otherProperties

##### Condition
Condition has its own docs [here](./conditions.html)

##### Metadata

The metadata section allows you to pass JSON. It takes the `Value<JsonNode>` type described [here](./fundamentals.html#the-valuejsonnode-type)

> For information on other Metadata elements like `AWS::CloudFormation::Init`, see [Metadata](./metadata.html)

<pre class="kotlin" data-highlight-only>
topic(resourceProperties = ResourceProperties(
    metadata = json(mapOf(
            "Key1" to "Value1",
            "Key2" to listOf("Value2", "Value3")
    ))
))
</pre>

##### Update Policy

The update policy attribute tells CloudFormation what to do when this resource updates.

<pre class="kotlin" data-highlight-only>
ResourceProperties(
    updatePolicy = UpdatePolicy(
            autoScalingReplacingUpdate = AutoScalingReplacingUpdate(willReplace = +true)
    )
)
</pre>

##### Creation Policy

The creation policy attribute tells CloudFormation what to do when this resource is created.

<pre class="kotlin" data-highlight-only>
ResourceProperties(
        creationPolicy = CreationPolicy(
                resourceSignal = CreationPolicy.ResourceSignal(
                        count = Value.Of(3),
                        timeout = +"PT15M"
                )
        )
)
</pre>

##### Deletion Policy

The deletion policy attribute tells CloudFormation what to do when this resource is deleted.

<pre class="kotlin" data-highlight-only>
ResourceProperties(
        deletionPolicy = DeletionPolicy.RETAIN.policy
)
</pre>

##### Other Properties

If KloudFormation does not have the property you need to set, you can add any extra properties by passing them to the otherProperties attribute.

<pre class="kotlin" data-highlight-only>
topic(resourceProperties = ResourceProperties(
        otherProperties = mapOf("MyProperty" to "Value")
))
</pre>

```yaml
Resources:
  Topic:
    Type: "AWS::SNS::Topic"
    Properties:
      MyProperty: "Value"
```

## Custom Resources

There are two types of custom resources:
 
* `AWS::CloudFormation::CustomResource`
* `Custom::<some string>`

Both types can be made with the `customResource()` function provided. In order to change the type to `Custom::<some string>` invoke the `asCustomResource` function after building. Shown Below:

<pre class="kotlin" data-highlight-only>
val standardCustomResource = customResource(
        logicalName = "DatabaseInitializer",
        serviceToken = +"arn:aws::xxxx:xxx"
).asCustomResource(properties = mapOf(
        "A" to "B",
        "C" to "D"
))
val customNameCustomResource = customResource(
        logicalName = "DatabaseInitializer2",
        serviceToken = +"arn:aws::xxxx:xxx"
).asCustomResource("Custom::DBInit")
</pre>

```yaml
Resources:
  DatabaseInitializer:
    Type: "AWS::CloudFormation::CustomResource"
    Properties:
      ServiceToken: "arn:aws::xxxx:xxx"
      A: "B"
      C: "D"
  DatabaseInitializer2:
    Type: "Custom::DBInit"
    Properties:
      ServiceToken: "arn:aws::xxxx:xxx"
```

# Full Resource List

>> Note: All resources are generated and are up to date with Amazon's specifications. If a resource is not listed below it is because this textual list is not generated.

```yaml
Alexa ASK Resources: Skill
AWS ApiGateway Resources: Account, ApiKey, Authorizer, BasePathMapping, ClientCertificate, Deployment, DocumentationPart, DocumentationVersion, DomainName, GatewayResponse, Method, Model, RequestValidator, Resource, RestApi, Stage, UsagePlan, UsagePlanKey, VpcLink
AWS ApiGatewayV2 Resources: Api, Authorizer, Deployment, Integration, IntegrationResponse, Model, Route, RouteResponse, Stage
AWS AppStream Resources: DirectoryConfig, Fleet, ImageBuilder, Stack, StackFleetAssociation, StackUserAssociation, User
AWS AppSync Resources: ApiKey, DataSource, FunctionConfiguration, GraphQLApi, GraphQLSchema, Resolver
AWS ApplicationAutoScaling Resources: ScalableTarget, ScalingPolicy
AWS Athena Resources: NamedQuery
AWS AutoScaling Resources: AutoScalingGroup, LaunchConfiguration, LifecycleHook, ScalingPolicy, ScheduledAction
AWS AutoScalingPlans Resources: ScalingPlan
AWS Batch Resources: ComputeEnvironment, JobDefinition, JobQueue
AWS Budgets Resources: Budget
AWS CertificateManager Resources: Certificate
AWS Cloud9 Resources: EnvironmentEC2
AWS CloudFormation Resources: CustomResource, Macro, Stack, WaitCondition, WaitConditionHandle
AWS CloudFront Resources: CloudFrontOriginAccessIdentity, Distribution, StreamingDistribution
AWS CloudTrail Resources: Trail
AWS CloudWatch Resources: Alarm, Dashboard
AWS CodeBuild Resources: Project
AWS CodeCommit Resources: Repository
AWS CodeDeploy Resources: Application, DeploymentConfig, DeploymentGroup
AWS CodePipeline Resources: CustomActionType, Pipeline, Webhook
AWS Cognito Resources: IdentityPool, IdentityPoolRoleAttachment, UserPool, UserPoolClient, UserPoolGroup, UserPoolUser, UserPoolUserToGroupAttachment
AWS Config Resources: AggregationAuthorization, ConfigRule, ConfigurationAggregator, ConfigurationRecorder, DeliveryChannel
AWS DAX Resources: Cluster, ParameterGroup, SubnetGroup
AWS DLM Resources: LifecyclePolicy
AWS DMS Resources: Certificate, Endpoint, EventSubscription, ReplicationInstance, ReplicationSubnetGroup, ReplicationTask
AWS DataPipeline Resources: Pipeline
AWS DirectoryService Resources: MicrosoftAD, SimpleAD
AWS DocDB Resources: DBCluster, DBClusterParameterGroup, DBInstance, DBSubnetGroup
AWS DynamoDB Resources: Table
AWS EC2 Resources: CustomerGateway, DHCPOptions, EC2Fleet, EIP, EIPAssociation, EgressOnlyInternetGateway, FlowLog, Host, Instance, InternetGateway, LaunchTemplate, NatGateway, NetworkAcl, NetworkAclEntry, NetworkInterface, NetworkInterfaceAttachment, NetworkInterfacePermission, PlacementGroup, Route, RouteTable, SecurityGroup, SecurityGroupEgress, SecurityGroupIngress, SpotFleet, Subnet, SubnetCidrBlock, SubnetNetworkAclAssociation, SubnetRouteTableAssociation, TransitGateway, TransitGatewayAttachment, TransitGatewayRoute, TransitGatewayRouteTable, TransitGatewayRouteTableAssociation, TransitGatewayRouteTablePropagation, TrunkInterfaceAssociation, VPC, VPCCidrBlock, VPCDHCPOptionsAssociation, VPCEndpoint, VPCEndpointConnectionNotification, VPCEndpointService, VPCEndpointServicePermissions, VPCGatewayAttachment, VPCPeeringConnection, VPNConnection, VPNConnectionRoute, VPNGateway, VPNGatewayRoutePropagation, Volume, VolumeAttachment
AWS ECR Resources: Repository
AWS ECS Resources: Cluster, Service, TaskDefinition
AWS EFS Resources: FileSystem, MountTarget
AWS EKS Resources: Cluster
AWS EMR Resources: Cluster, InstanceFleetConfig, InstanceGroupConfig, SecurityConfiguration, Step
AWS ElastiCache Resources: CacheCluster, ParameterGroup, ReplicationGroup, SecurityGroup, SecurityGroupIngress, SubnetGroup
AWS ElasticBeanstalk Resources: Application, ApplicationVersion, ConfigurationTemplate, Environment
AWS ElasticLoadBalancing Resources: LoadBalancer
AWS ElasticLoadBalancingV2 Resources: Listener, ListenerCertificate, ListenerRule, LoadBalancer, TargetGroup
AWS Elasticsearch Resources: Domain
AWS Events Resources: EventBusPolicy, Rule
AWS FSx Resources: FileSystem
AWS GameLift Resources: Alias, Build, Fleet
AWS Glue Resources: Classifier, Connection, Crawler, Database, DevEndpoint, Job, Partition, Table, Trigger
AWS GuardDuty Resources: Detector, Filter, IPSet, Master, Member, ThreatIntelSet
AWS IAM Resources: AccessKey, Group, InstanceProfile, ManagedPolicy, Policy, Role, ServiceLinkedRole, User, UserToGroupAddition
AWS Inspector Resources: AssessmentTarget, AssessmentTemplate, ResourceGroup
AWS IoT Resources: Certificate, Policy, PolicyPrincipalAttachment, Thing, ThingPrincipalAttachment, TopicRule
AWS IoT1Click Resources: Device, Placement, Project
AWS IoTAnalytics Resources: Channel, Dataset, Datastore, Pipeline
AWS KMS Resources: Alias, Key
AWS Kinesis Resources: Stream, StreamConsumer
AWS KinesisAnalytics Resources: Application, ApplicationOutput, ApplicationReferenceDataSource
AWS KinesisAnalyticsV2 Resources: Application, ApplicationCloudWatchLoggingOption, ApplicationOutput, ApplicationReferenceDataSource
AWS KinesisFirehose Resources: DeliveryStream
AWS Lambda Resources: Alias, EventSourceMapping, Function, LayerVersion, LayerVersionPermission, Permission, Version
AWS Logs Resources: Destination, LogGroup, LogStream, MetricFilter, SubscriptionFilter
AWS Neptune Resources: DBCluster, DBClusterParameterGroup, DBInstance, DBParameterGroup, DBSubnetGroup
AWS OpsWorks Resources: App, ElasticLoadBalancerAttachment, Instance, Layer, Stack, UserProfile, Volume
AWS OpsWorksCM Resources: Server
AWS RAM Resources: ResourceShare
AWS RDS Resources: DBCluster, DBClusterParameterGroup, DBInstance, DBParameterGroup, DBSecurityGroup, DBSecurityGroupIngress, DBSubnetGroup, EventSubscription, OptionGroup
AWS Redshift Resources: Cluster, ClusterParameterGroup, ClusterSecurityGroup, ClusterSecurityGroupIngress, ClusterSubnetGroup
AWS RoboMaker Resources: Fleet, Robot, RobotApplication, RobotApplicationVersion, SimulationApplication, SimulationApplicationVersion
AWS Route53 Resources: HealthCheck, HostedZone, RecordSet, RecordSetGroup
AWS Route53Resolver Resources: ResolverEndpoint, ResolverRule, ResolverRuleAssociation
AWS S3 Resources: Bucket, BucketPolicy
AWS SDB Resources: Domain
AWS SES Resources: ConfigurationSet, ConfigurationSetEventDestination, ReceiptFilter, ReceiptRule, ReceiptRuleSet, Template
AWS SNS Resources: Subscription, Topic, TopicPolicy
AWS SQS Resources: Queue, QueuePolicy
AWS SSM Resources: Association, Document, MaintenanceWindow, MaintenanceWindowTarget, MaintenanceWindowTask, Parameter, PatchBaseline, ResourceDataSync
AWS SageMaker Resources: Endpoint, EndpointConfig, Model, NotebookInstance, NotebookInstanceLifecycleConfig
AWS SecretsManager Resources: ResourcePolicy, RotationSchedule, Secret, SecretTargetAttachment
AWS ServiceCatalog Resources: AcceptedPortfolioShare, CloudFormationProduct, CloudFormationProvisionedProduct, LaunchNotificationConstraint, LaunchRoleConstraint, LaunchTemplateConstraint, Portfolio, PortfolioPrincipalAssociation, PortfolioProductAssociation, PortfolioShare, TagOption, TagOptionAssociation
AWS ServiceDiscovery Resources: HttpNamespace, Instance, PrivateDnsNamespace, PublicDnsNamespace, Service
AWS StepFunctions Resources: Activity, StateMachine
AWS WAF Resources: ByteMatchSet, IPSet, Rule, SizeConstraintSet, SqlInjectionMatchSet, WebACL, XssMatchSet
AWS WAFRegional Resources: ByteMatchSet, IPSet, Rule, SizeConstraintSet, SqlInjectionMatchSet, WebACL, WebACLAssociation, XssMatchSet
AWS WorkSpaces Resources: Workspace
```