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

>> Note: All resources are generated and are up to date with Amazon's specifications. If a resource is not listed below it is because this textual list is generated manually.

| CloudFormation | KloudFormation |
|---|---|
|**AWS AmazonMQ**||
| AWS::AmazonMQ::Broker | io.kloudformation.resource.aws.amazonmq.Broker |
| AWS::AmazonMQ::Configuration | io.kloudformation.resource.aws.amazonmq.Configuration |
| AWS::AmazonMQ::ConfigurationAssociation | io.kloudformation.resource.aws.amazonmq.ConfigurationAssociation |
|**AWS ApiGateway**||
| AWS::ApiGateway::Account | io.kloudformation.resource.aws.apigateway.Account |
| AWS::ApiGateway::ApiKey | io.kloudformation.resource.aws.apigateway.ApiKey |
| AWS::ApiGateway::Authorizer | io.kloudformation.resource.aws.apigateway.Authorizer |
| AWS::ApiGateway::BasePathMapping | io.kloudformation.resource.aws.apigateway.BasePathMapping |
| AWS::ApiGateway::ClientCertificate | io.kloudformation.resource.aws.apigateway.ClientCertificate |
| AWS::ApiGateway::Deployment | io.kloudformation.resource.aws.apigateway.Deployment |
| AWS::ApiGateway::DocumentationPart | io.kloudformation.resource.aws.apigateway.DocumentationPart |
| AWS::ApiGateway::DocumentationVersion | io.kloudformation.resource.aws.apigateway.DocumentationVersion |
| AWS::ApiGateway::DomainName | io.kloudformation.resource.aws.apigateway.DomainName |
| AWS::ApiGateway::GatewayResponse | io.kloudformation.resource.aws.apigateway.GatewayResponse |
| AWS::ApiGateway::Method | io.kloudformation.resource.aws.apigateway.Method |
| AWS::ApiGateway::Model | io.kloudformation.resource.aws.apigateway.Model |
| AWS::ApiGateway::RequestValidator | io.kloudformation.resource.aws.apigateway.RequestValidator |
| AWS::ApiGateway::Resource | io.kloudformation.resource.aws.apigateway.Resource |
| AWS::ApiGateway::RestApi | io.kloudformation.resource.aws.apigateway.RestApi |
| AWS::ApiGateway::Stage | io.kloudformation.resource.aws.apigateway.Stage |
| AWS::ApiGateway::UsagePlan | io.kloudformation.resource.aws.apigateway.UsagePlan |
| AWS::ApiGateway::UsagePlanKey | io.kloudformation.resource.aws.apigateway.UsagePlanKey |
| AWS::ApiGateway::VpcLink | io.kloudformation.resource.aws.apigateway.VpcLink |
|**AWS ApiGatewayV2**||
| AWS::ApiGatewayV2::Api | io.kloudformation.resource.aws.apigatewayv2.Api |
| AWS::ApiGatewayV2::Authorizer | io.kloudformation.resource.aws.apigatewayv2.Authorizer |
| AWS::ApiGatewayV2::Deployment | io.kloudformation.resource.aws.apigatewayv2.Deployment |
| AWS::ApiGatewayV2::Integration | io.kloudformation.resource.aws.apigatewayv2.Integration |
| AWS::ApiGatewayV2::IntegrationResponse | io.kloudformation.resource.aws.apigatewayv2.IntegrationResponse |
| AWS::ApiGatewayV2::Model | io.kloudformation.resource.aws.apigatewayv2.Model |
| AWS::ApiGatewayV2::Route | io.kloudformation.resource.aws.apigatewayv2.Route |
| AWS::ApiGatewayV2::RouteResponse | io.kloudformation.resource.aws.apigatewayv2.RouteResponse |
| AWS::ApiGatewayV2::Stage | io.kloudformation.resource.aws.apigatewayv2.Stage |
|**AWS AppStream**||
| AWS::AppStream::DirectoryConfig | io.kloudformation.resource.aws.appstream.DirectoryConfig |
| AWS::AppStream::Fleet | io.kloudformation.resource.aws.appstream.Fleet |
| AWS::AppStream::ImageBuilder | io.kloudformation.resource.aws.appstream.ImageBuilder |
| AWS::AppStream::Stack | io.kloudformation.resource.aws.appstream.Stack |
| AWS::AppStream::StackFleetAssociation | io.kloudformation.resource.aws.appstream.StackFleetAssociation |
| AWS::AppStream::StackUserAssociation | io.kloudformation.resource.aws.appstream.StackUserAssociation |
| AWS::AppStream::User | io.kloudformation.resource.aws.appstream.User |
|**AWS AppSync**||
| AWS::AppSync::ApiKey | io.kloudformation.resource.aws.appsync.ApiKey |
| AWS::AppSync::DataSource | io.kloudformation.resource.aws.appsync.DataSource |
| AWS::AppSync::FunctionConfiguration | io.kloudformation.resource.aws.appsync.FunctionConfiguration |
| AWS::AppSync::GraphQLApi | io.kloudformation.resource.aws.appsync.GraphQLApi |
| AWS::AppSync::GraphQLSchema | io.kloudformation.resource.aws.appsync.GraphQLSchema |
| AWS::AppSync::Resolver | io.kloudformation.resource.aws.appsync.Resolver |
|**AWS ApplicationAutoScaling**||
| AWS::ApplicationAutoScaling::ScalableTarget | io.kloudformation.resource.aws.applicationautoscaling.ScalableTarget |
| AWS::ApplicationAutoScaling::ScalingPolicy | io.kloudformation.resource.aws.applicationautoscaling.ScalingPolicy |
|**AWS Athena**||
| AWS::Athena::NamedQuery | io.kloudformation.resource.aws.athena.NamedQuery |
|**AWS AutoScaling**||
| AWS::AutoScaling::AutoScalingGroup | io.kloudformation.resource.aws.autoscaling.AutoScalingGroup |
| AWS::AutoScaling::LaunchConfiguration | io.kloudformation.resource.aws.autoscaling.LaunchConfiguration |
| AWS::AutoScaling::LifecycleHook | io.kloudformation.resource.aws.autoscaling.LifecycleHook |
| AWS::AutoScaling::ScalingPolicy | io.kloudformation.resource.aws.autoscaling.ScalingPolicy |
| AWS::AutoScaling::ScheduledAction | io.kloudformation.resource.aws.autoscaling.ScheduledAction |
|**AWS AutoScalingPlans**||
| AWS::AutoScalingPlans::ScalingPlan | io.kloudformation.resource.aws.autoscalingplans.ScalingPlan |
|**AWS Batch**||
| AWS::Batch::ComputeEnvironment | io.kloudformation.resource.aws.batch.ComputeEnvironment |
| AWS::Batch::JobDefinition | io.kloudformation.resource.aws.batch.JobDefinition |
| AWS::Batch::JobQueue | io.kloudformation.resource.aws.batch.JobQueue |
|**AWS Budgets**||
| AWS::Budgets::Budget | io.kloudformation.resource.aws.budgets.Budget |
|**AWS CertificateManager**||
| AWS::CertificateManager::Certificate | io.kloudformation.resource.aws.certificatemanager.Certificate |
|**AWS Cloud9**||
| AWS::Cloud9::EnvironmentEC2 | io.kloudformation.resource.aws.cloud9.EnvironmentEC2 |
|**AWS CloudFormation**||
| AWS::CloudFormation::CustomResource | io.kloudformation.resource.aws.cloudformation.CustomResource |
| AWS::CloudFormation::Macro | io.kloudformation.resource.aws.cloudformation.Macro |
| AWS::CloudFormation::Stack | io.kloudformation.resource.aws.cloudformation.Stack |
| AWS::CloudFormation::WaitCondition | io.kloudformation.resource.aws.cloudformation.WaitCondition |
| AWS::CloudFormation::WaitConditionHandle | io.kloudformation.resource.aws.cloudformation.WaitConditionHandle |
|**AWS CloudFront**||
| AWS::CloudFront::CloudFrontOriginAccessIdentity | io.kloudformation.resource.aws.cloudfront.CloudFrontOriginAccessIdentity |
| AWS::CloudFront::Distribution | io.kloudformation.resource.aws.cloudfront.Distribution |
| AWS::CloudFront::StreamingDistribution | io.kloudformation.resource.aws.cloudfront.StreamingDistribution |
|**AWS CloudTrail**||
| AWS::CloudTrail::Trail | io.kloudformation.resource.aws.cloudtrail.Trail |
|**AWS CloudWatch**||
| AWS::CloudWatch::Alarm | io.kloudformation.resource.aws.cloudwatch.Alarm |
| AWS::CloudWatch::Dashboard | io.kloudformation.resource.aws.cloudwatch.Dashboard |
|**AWS CodeBuild**||
| AWS::CodeBuild::Project | io.kloudformation.resource.aws.codebuild.Project |
|**AWS CodeCommit**||
| AWS::CodeCommit::Repository | io.kloudformation.resource.aws.codecommit.Repository |
|**AWS CodeDeploy**||
| AWS::CodeDeploy::Application | io.kloudformation.resource.aws.codedeploy.Application |
| AWS::CodeDeploy::DeploymentConfig | io.kloudformation.resource.aws.codedeploy.DeploymentConfig |
| AWS::CodeDeploy::DeploymentGroup | io.kloudformation.resource.aws.codedeploy.DeploymentGroup |
|**AWS CodePipeline**||
| AWS::CodePipeline::CustomActionType | io.kloudformation.resource.aws.codepipeline.CustomActionType |
| AWS::CodePipeline::Pipeline | io.kloudformation.resource.aws.codepipeline.Pipeline |
| AWS::CodePipeline::Webhook | io.kloudformation.resource.aws.codepipeline.Webhook |
|**AWS Cognito**||
| AWS::Cognito::IdentityPool | io.kloudformation.resource.aws.cognito.IdentityPool |
| AWS::Cognito::IdentityPoolRoleAttachment | io.kloudformation.resource.aws.cognito.IdentityPoolRoleAttachment |
| AWS::Cognito::UserPool | io.kloudformation.resource.aws.cognito.UserPool |
| AWS::Cognito::UserPoolClient | io.kloudformation.resource.aws.cognito.UserPoolClient |
| AWS::Cognito::UserPoolGroup | io.kloudformation.resource.aws.cognito.UserPoolGroup |
| AWS::Cognito::UserPoolUser | io.kloudformation.resource.aws.cognito.UserPoolUser |
| AWS::Cognito::UserPoolUserToGroupAttachment | io.kloudformation.resource.aws.cognito.UserPoolUserToGroupAttachment |
|**AWS Config**||
| AWS::Config::AggregationAuthorization | io.kloudformation.resource.aws.config.AggregationAuthorization |
| AWS::Config::ConfigRule | io.kloudformation.resource.aws.config.ConfigRule |
| AWS::Config::ConfigurationAggregator | io.kloudformation.resource.aws.config.ConfigurationAggregator |
| AWS::Config::ConfigurationRecorder | io.kloudformation.resource.aws.config.ConfigurationRecorder |
| AWS::Config::DeliveryChannel | io.kloudformation.resource.aws.config.DeliveryChannel |
|**AWS DAX**||
| AWS::DAX::Cluster | io.kloudformation.resource.aws.dax.Cluster |
| AWS::DAX::ParameterGroup | io.kloudformation.resource.aws.dax.ParameterGroup |
| AWS::DAX::SubnetGroup | io.kloudformation.resource.aws.dax.SubnetGroup |
|**AWS DLM**||
| AWS::DLM::LifecyclePolicy | io.kloudformation.resource.aws.dlm.LifecyclePolicy |
|**AWS DMS**||
| AWS::DMS::Certificate | io.kloudformation.resource.aws.dms.Certificate |
| AWS::DMS::Endpoint | io.kloudformation.resource.aws.dms.Endpoint |
| AWS::DMS::EventSubscription | io.kloudformation.resource.aws.dms.EventSubscription |
| AWS::DMS::ReplicationInstance | io.kloudformation.resource.aws.dms.ReplicationInstance |
| AWS::DMS::ReplicationSubnetGroup | io.kloudformation.resource.aws.dms.ReplicationSubnetGroup |
| AWS::DMS::ReplicationTask | io.kloudformation.resource.aws.dms.ReplicationTask |
|**AWS DataPipeline**||
| AWS::DataPipeline::Pipeline | io.kloudformation.resource.aws.datapipeline.Pipeline |
|**AWS DirectoryService**||
| AWS::DirectoryService::MicrosoftAD | io.kloudformation.resource.aws.directoryservice.MicrosoftAD |
| AWS::DirectoryService::SimpleAD | io.kloudformation.resource.aws.directoryservice.SimpleAD |
|**AWS DocDB**||
| AWS::DocDB::DBCluster | io.kloudformation.resource.aws.docdb.DBCluster |
| AWS::DocDB::DBClusterParameterGroup | io.kloudformation.resource.aws.docdb.DBClusterParameterGroup |
| AWS::DocDB::DBInstance | io.kloudformation.resource.aws.docdb.DBInstance |
| AWS::DocDB::DBSubnetGroup | io.kloudformation.resource.aws.docdb.DBSubnetGroup |
|**AWS DynamoDB**||
| AWS::DynamoDB::Table | io.kloudformation.resource.aws.dynamodb.Table |
|**AWS EC2**||
| AWS::EC2::CustomerGateway | io.kloudformation.resource.aws.ec2.CustomerGateway |
| AWS::EC2::DHCPOptions | io.kloudformation.resource.aws.ec2.DHCPOptions |
| AWS::EC2::EC2Fleet | io.kloudformation.resource.aws.ec2.EC2Fleet |
| AWS::EC2::EIP | io.kloudformation.resource.aws.ec2.EIP |
| AWS::EC2::EIPAssociation | io.kloudformation.resource.aws.ec2.EIPAssociation |
| AWS::EC2::EgressOnlyInternetGateway | io.kloudformation.resource.aws.ec2.EgressOnlyInternetGateway |
| AWS::EC2::FlowLog | io.kloudformation.resource.aws.ec2.FlowLog |
| AWS::EC2::Host | io.kloudformation.resource.aws.ec2.Host |
| AWS::EC2::Instance | io.kloudformation.resource.aws.ec2.Instance |
| AWS::EC2::InternetGateway | io.kloudformation.resource.aws.ec2.InternetGateway |
| AWS::EC2::LaunchTemplate | io.kloudformation.resource.aws.ec2.LaunchTemplate |
| AWS::EC2::NatGateway | io.kloudformation.resource.aws.ec2.NatGateway |
| AWS::EC2::NetworkAcl | io.kloudformation.resource.aws.ec2.NetworkAcl |
| AWS::EC2::NetworkAclEntry | io.kloudformation.resource.aws.ec2.NetworkAclEntry |
| AWS::EC2::NetworkInterface | io.kloudformation.resource.aws.ec2.NetworkInterface |
| AWS::EC2::NetworkInterfaceAttachment | io.kloudformation.resource.aws.ec2.NetworkInterfaceAttachment |
| AWS::EC2::NetworkInterfacePermission | io.kloudformation.resource.aws.ec2.NetworkInterfacePermission |
| AWS::EC2::PlacementGroup | io.kloudformation.resource.aws.ec2.PlacementGroup |
| AWS::EC2::Route | io.kloudformation.resource.aws.ec2.Route |
| AWS::EC2::RouteTable | io.kloudformation.resource.aws.ec2.RouteTable |
| AWS::EC2::SecurityGroup | io.kloudformation.resource.aws.ec2.SecurityGroup |
| AWS::EC2::SecurityGroupEgress | io.kloudformation.resource.aws.ec2.SecurityGroupEgress |
| AWS::EC2::SecurityGroupIngress | io.kloudformation.resource.aws.ec2.SecurityGroupIngress |
| AWS::EC2::SpotFleet | io.kloudformation.resource.aws.ec2.SpotFleet |
| AWS::EC2::Subnet | io.kloudformation.resource.aws.ec2.Subnet |
| AWS::EC2::SubnetCidrBlock | io.kloudformation.resource.aws.ec2.SubnetCidrBlock |
| AWS::EC2::SubnetNetworkAclAssociation | io.kloudformation.resource.aws.ec2.SubnetNetworkAclAssociation |
| AWS::EC2::SubnetRouteTableAssociation | io.kloudformation.resource.aws.ec2.SubnetRouteTableAssociation |
| AWS::EC2::TransitGateway | io.kloudformation.resource.aws.ec2.TransitGateway |
| AWS::EC2::TransitGatewayAttachment | io.kloudformation.resource.aws.ec2.TransitGatewayAttachment |
| AWS::EC2::TransitGatewayRoute | io.kloudformation.resource.aws.ec2.TransitGatewayRoute |
| AWS::EC2::TransitGatewayRouteTable | io.kloudformation.resource.aws.ec2.TransitGatewayRouteTable |
| AWS::EC2::TransitGatewayRouteTableAssociation | io.kloudformation.resource.aws.ec2.TransitGatewayRouteTableAssociation |
| AWS::EC2::TransitGatewayRouteTablePropagation | io.kloudformation.resource.aws.ec2.TransitGatewayRouteTablePropagation |
| AWS::EC2::TrunkInterfaceAssociation | io.kloudformation.resource.aws.ec2.TrunkInterfaceAssociation |
| AWS::EC2::VPC | io.kloudformation.resource.aws.ec2.VPC |
| AWS::EC2::VPCCidrBlock | io.kloudformation.resource.aws.ec2.VPCCidrBlock |
| AWS::EC2::VPCDHCPOptionsAssociation | io.kloudformation.resource.aws.ec2.VPCDHCPOptionsAssociation |
| AWS::EC2::VPCEndpoint | io.kloudformation.resource.aws.ec2.VPCEndpoint |
| AWS::EC2::VPCEndpointConnectionNotification | io.kloudformation.resource.aws.ec2.VPCEndpointConnectionNotification |
| AWS::EC2::VPCEndpointService | io.kloudformation.resource.aws.ec2.VPCEndpointService |
| AWS::EC2::VPCEndpointServicePermissions | io.kloudformation.resource.aws.ec2.VPCEndpointServicePermissions |
| AWS::EC2::VPCGatewayAttachment | io.kloudformation.resource.aws.ec2.VPCGatewayAttachment |
| AWS::EC2::VPCPeeringConnection | io.kloudformation.resource.aws.ec2.VPCPeeringConnection |
| AWS::EC2::VPNConnection | io.kloudformation.resource.aws.ec2.VPNConnection |
| AWS::EC2::VPNConnectionRoute | io.kloudformation.resource.aws.ec2.VPNConnectionRoute |
| AWS::EC2::VPNGateway | io.kloudformation.resource.aws.ec2.VPNGateway |
| AWS::EC2::VPNGatewayRoutePropagation | io.kloudformation.resource.aws.ec2.VPNGatewayRoutePropagation |
| AWS::EC2::Volume | io.kloudformation.resource.aws.ec2.Volume |
| AWS::EC2::VolumeAttachment | io.kloudformation.resource.aws.ec2.VolumeAttachment |
|**AWS ECR**||
| AWS::ECR::Repository | io.kloudformation.resource.aws.ecr.Repository |
|**AWS ECS**||
| AWS::ECS::Cluster | io.kloudformation.resource.aws.ecs.Cluster |
| AWS::ECS::Service | io.kloudformation.resource.aws.ecs.Service |
| AWS::ECS::TaskDefinition | io.kloudformation.resource.aws.ecs.TaskDefinition |
|**AWS EFS**||
| AWS::EFS::FileSystem | io.kloudformation.resource.aws.efs.FileSystem |
| AWS::EFS::MountTarget | io.kloudformation.resource.aws.efs.MountTarget |
|**AWS EKS**||
| AWS::EKS::Cluster | io.kloudformation.resource.aws.eks.Cluster |
|**AWS EMR**||
| AWS::EMR::Cluster | io.kloudformation.resource.aws.emr.Cluster |
| AWS::EMR::InstanceFleetConfig | io.kloudformation.resource.aws.emr.InstanceFleetConfig |
| AWS::EMR::InstanceGroupConfig | io.kloudformation.resource.aws.emr.InstanceGroupConfig |
| AWS::EMR::SecurityConfiguration | io.kloudformation.resource.aws.emr.SecurityConfiguration |
| AWS::EMR::Step | io.kloudformation.resource.aws.emr.Step |
|**AWS ElastiCache**||
| AWS::ElastiCache::CacheCluster | io.kloudformation.resource.aws.elasticache.CacheCluster |
| AWS::ElastiCache::ParameterGroup | io.kloudformation.resource.aws.elasticache.ParameterGroup |
| AWS::ElastiCache::ReplicationGroup | io.kloudformation.resource.aws.elasticache.ReplicationGroup |
| AWS::ElastiCache::SecurityGroup | io.kloudformation.resource.aws.elasticache.SecurityGroup |
| AWS::ElastiCache::SecurityGroupIngress | io.kloudformation.resource.aws.elasticache.SecurityGroupIngress |
| AWS::ElastiCache::SubnetGroup | io.kloudformation.resource.aws.elasticache.SubnetGroup |
|**AWS ElasticBeanstalk**||
| AWS::ElasticBeanstalk::Application | io.kloudformation.resource.aws.elasticbeanstalk.Application |
| AWS::ElasticBeanstalk::ApplicationVersion | io.kloudformation.resource.aws.elasticbeanstalk.ApplicationVersion |
| AWS::ElasticBeanstalk::ConfigurationTemplate | io.kloudformation.resource.aws.elasticbeanstalk.ConfigurationTemplate |
| AWS::ElasticBeanstalk::Environment | io.kloudformation.resource.aws.elasticbeanstalk.Environment |
|**AWS ElasticLoadBalancing**||
| AWS::ElasticLoadBalancing::LoadBalancer | io.kloudformation.resource.aws.elasticloadbalancing.LoadBalancer |
|**AWS ElasticLoadBalancingV2**||
| AWS::ElasticLoadBalancingV2::Listener | io.kloudformation.resource.aws.elasticloadbalancingv2.Listener |
| AWS::ElasticLoadBalancingV2::ListenerCertificate | io.kloudformation.resource.aws.elasticloadbalancingv2.ListenerCertificate |
| AWS::ElasticLoadBalancingV2::ListenerRule | io.kloudformation.resource.aws.elasticloadbalancingv2.ListenerRule |
| AWS::ElasticLoadBalancingV2::LoadBalancer | io.kloudformation.resource.aws.elasticloadbalancingv2.LoadBalancer |
| AWS::ElasticLoadBalancingV2::TargetGroup | io.kloudformation.resource.aws.elasticloadbalancingv2.TargetGroup |
|**AWS Elasticsearch**||
| AWS::Elasticsearch::Domain | io.kloudformation.resource.aws.elasticsearch.Domain |
|**AWS Events**||
| AWS::Events::EventBusPolicy | io.kloudformation.resource.aws.events.EventBusPolicy |
| AWS::Events::Rule | io.kloudformation.resource.aws.events.Rule |
|**AWS FSx**||
| AWS::FSx::FileSystem | io.kloudformation.resource.aws.fsx.FileSystem |
|**AWS GameLift**||
| AWS::GameLift::Alias | io.kloudformation.resource.aws.gamelift.Alias |
| AWS::GameLift::Build | io.kloudformation.resource.aws.gamelift.Build |
| AWS::GameLift::Fleet | io.kloudformation.resource.aws.gamelift.Fleet |
|**AWS Glue**||
| AWS::Glue::Classifier | io.kloudformation.resource.aws.glue.Classifier |
| AWS::Glue::Connection | io.kloudformation.resource.aws.glue.Connection |
| AWS::Glue::Crawler | io.kloudformation.resource.aws.glue.Crawler |
| AWS::Glue::Database | io.kloudformation.resource.aws.glue.Database |
| AWS::Glue::DevEndpoint | io.kloudformation.resource.aws.glue.DevEndpoint |
| AWS::Glue::Job | io.kloudformation.resource.aws.glue.Job |
| AWS::Glue::Partition | io.kloudformation.resource.aws.glue.Partition |
| AWS::Glue::Table | io.kloudformation.resource.aws.glue.Table |
| AWS::Glue::Trigger | io.kloudformation.resource.aws.glue.Trigger |
|**AWS GuardDuty**||
| AWS::GuardDuty::Detector | io.kloudformation.resource.aws.guardduty.Detector |
| AWS::GuardDuty::Filter | io.kloudformation.resource.aws.guardduty.Filter |
| AWS::GuardDuty::IPSet | io.kloudformation.resource.aws.guardduty.IPSet |
| AWS::GuardDuty::Master | io.kloudformation.resource.aws.guardduty.Master |
| AWS::GuardDuty::Member | io.kloudformation.resource.aws.guardduty.Member |
| AWS::GuardDuty::ThreatIntelSet | io.kloudformation.resource.aws.guardduty.ThreatIntelSet |
|**AWS IAM**||
| AWS::IAM::AccessKey | io.kloudformation.resource.aws.iam.AccessKey |
| AWS::IAM::Group | io.kloudformation.resource.aws.iam.Group |
| AWS::IAM::InstanceProfile | io.kloudformation.resource.aws.iam.InstanceProfile |
| AWS::IAM::ManagedPolicy | io.kloudformation.resource.aws.iam.ManagedPolicy |
| AWS::IAM::Policy | io.kloudformation.resource.aws.iam.Policy |
| AWS::IAM::Role | io.kloudformation.resource.aws.iam.Role |
| AWS::IAM::ServiceLinkedRole | io.kloudformation.resource.aws.iam.ServiceLinkedRole |
| AWS::IAM::User | io.kloudformation.resource.aws.iam.User |
| AWS::IAM::UserToGroupAddition | io.kloudformation.resource.aws.iam.UserToGroupAddition |
|**AWS Inspector**||
| AWS::Inspector::AssessmentTarget | io.kloudformation.resource.aws.inspector.AssessmentTarget |
| AWS::Inspector::AssessmentTemplate | io.kloudformation.resource.aws.inspector.AssessmentTemplate |
| AWS::Inspector::ResourceGroup | io.kloudformation.resource.aws.inspector.ResourceGroup |
|**AWS IoT**||
| AWS::IoT::Certificate | io.kloudformation.resource.aws.iot.Certificate |
| AWS::IoT::Policy | io.kloudformation.resource.aws.iot.Policy |
| AWS::IoT::PolicyPrincipalAttachment | io.kloudformation.resource.aws.iot.PolicyPrincipalAttachment |
| AWS::IoT::Thing | io.kloudformation.resource.aws.iot.Thing |
| AWS::IoT::ThingPrincipalAttachment | io.kloudformation.resource.aws.iot.ThingPrincipalAttachment |
| AWS::IoT::TopicRule | io.kloudformation.resource.aws.iot.TopicRule |
|**AWS IoT1Click**||
| AWS::IoT1Click::Device | io.kloudformation.resource.aws.iot1click.Device |
| AWS::IoT1Click::Placement | io.kloudformation.resource.aws.iot1click.Placement |
| AWS::IoT1Click::Project | io.kloudformation.resource.aws.iot1click.Project |
|**AWS IoTAnalytics**||
| AWS::IoTAnalytics::Channel | io.kloudformation.resource.aws.iotanalytics.Channel |
| AWS::IoTAnalytics::Dataset | io.kloudformation.resource.aws.iotanalytics.Dataset |
| AWS::IoTAnalytics::Datastore | io.kloudformation.resource.aws.iotanalytics.Datastore |
| AWS::IoTAnalytics::Pipeline | io.kloudformation.resource.aws.iotanalytics.Pipeline |
|**AWS KMS**||
| AWS::KMS::Alias | io.kloudformation.resource.aws.kms.Alias |
| AWS::KMS::Key | io.kloudformation.resource.aws.kms.Key |
|**AWS Kinesis**||
| AWS::Kinesis::Stream | io.kloudformation.resource.aws.kinesis.Stream |
| AWS::Kinesis::StreamConsumer | io.kloudformation.resource.aws.kinesis.StreamConsumer |
|**AWS KinesisAnalytics**||
| AWS::KinesisAnalytics::Application | io.kloudformation.resource.aws.kinesisanalytics.Application |
| AWS::KinesisAnalytics::ApplicationOutput | io.kloudformation.resource.aws.kinesisanalytics.ApplicationOutput |
| AWS::KinesisAnalytics::ApplicationReferenceDataSource | io.kloudformation.resource.aws.kinesisanalytics.ApplicationReferenceDataSource |
|**AWS KinesisAnalyticsV2**||
| AWS::KinesisAnalyticsV2::Application | io.kloudformation.resource.aws.kinesisanalyticsv2.Application |
| AWS::KinesisAnalyticsV2::ApplicationCloudWatchLoggingOption | io.kloudformation.resource.aws.kinesisanalyticsv2.ApplicationCloudWatchLoggingOption |
| AWS::KinesisAnalyticsV2::ApplicationOutput | io.kloudformation.resource.aws.kinesisanalyticsv2.ApplicationOutput |
| AWS::KinesisAnalyticsV2::ApplicationReferenceDataSource | io.kloudformation.resource.aws.kinesisanalyticsv2.ApplicationReferenceDataSource |
|**AWS KinesisFirehose**||
| AWS::KinesisFirehose::DeliveryStream | io.kloudformation.resource.aws.kinesisfirehose.DeliveryStream |
|**AWS Lambda**||
| AWS::Lambda::Alias | io.kloudformation.resource.aws.lambda.Alias |
| AWS::Lambda::EventSourceMapping | io.kloudformation.resource.aws.lambda.EventSourceMapping |
| AWS::Lambda::Function | io.kloudformation.resource.aws.lambda.Function |
| AWS::Lambda::LayerVersion | io.kloudformation.resource.aws.lambda.LayerVersion |
| AWS::Lambda::LayerVersionPermission | io.kloudformation.resource.aws.lambda.LayerVersionPermission |
| AWS::Lambda::Permission | io.kloudformation.resource.aws.lambda.Permission |
| AWS::Lambda::Version | io.kloudformation.resource.aws.lambda.Version |
|**AWS Logs**||
| AWS::Logs::Destination | io.kloudformation.resource.aws.logs.Destination |
| AWS::Logs::LogGroup | io.kloudformation.resource.aws.logs.LogGroup |
| AWS::Logs::LogStream | io.kloudformation.resource.aws.logs.LogStream |
| AWS::Logs::MetricFilter | io.kloudformation.resource.aws.logs.MetricFilter |
| AWS::Logs::SubscriptionFilter | io.kloudformation.resource.aws.logs.SubscriptionFilter |
|**AWS Neptune**||
| AWS::Neptune::DBCluster | io.kloudformation.resource.aws.neptune.DBCluster |
| AWS::Neptune::DBClusterParameterGroup | io.kloudformation.resource.aws.neptune.DBClusterParameterGroup |
| AWS::Neptune::DBInstance | io.kloudformation.resource.aws.neptune.DBInstance |
| AWS::Neptune::DBParameterGroup | io.kloudformation.resource.aws.neptune.DBParameterGroup |
| AWS::Neptune::DBSubnetGroup | io.kloudformation.resource.aws.neptune.DBSubnetGroup |
|**AWS OpsWorks**||
| AWS::OpsWorks::App | io.kloudformation.resource.aws.opsworks.App |
| AWS::OpsWorks::ElasticLoadBalancerAttachment | io.kloudformation.resource.aws.opsworks.ElasticLoadBalancerAttachment |
| AWS::OpsWorks::Instance | io.kloudformation.resource.aws.opsworks.Instance |
| AWS::OpsWorks::Layer | io.kloudformation.resource.aws.opsworks.Layer |
| AWS::OpsWorks::Stack | io.kloudformation.resource.aws.opsworks.Stack |
| AWS::OpsWorks::UserProfile | io.kloudformation.resource.aws.opsworks.UserProfile |
| AWS::OpsWorks::Volume | io.kloudformation.resource.aws.opsworks.Volume |
|**AWS OpsWorksCM**||
| AWS::OpsWorksCM::Server | io.kloudformation.resource.aws.opsworkscm.Server |
|**AWS RAM**||
| AWS::RAM::ResourceShare | io.kloudformation.resource.aws.ram.ResourceShare |
|**AWS RDS**||
| AWS::RDS::DBCluster | io.kloudformation.resource.aws.rds.DBCluster |
| AWS::RDS::DBClusterParameterGroup | io.kloudformation.resource.aws.rds.DBClusterParameterGroup |
| AWS::RDS::DBInstance | io.kloudformation.resource.aws.rds.DBInstance |
| AWS::RDS::DBParameterGroup | io.kloudformation.resource.aws.rds.DBParameterGroup |
| AWS::RDS::DBSecurityGroup | io.kloudformation.resource.aws.rds.DBSecurityGroup |
| AWS::RDS::DBSecurityGroupIngress | io.kloudformation.resource.aws.rds.DBSecurityGroupIngress |
| AWS::RDS::DBSubnetGroup | io.kloudformation.resource.aws.rds.DBSubnetGroup |
| AWS::RDS::EventSubscription | io.kloudformation.resource.aws.rds.EventSubscription |
| AWS::RDS::OptionGroup | io.kloudformation.resource.aws.rds.OptionGroup |
|**AWS Redshift**||
| AWS::Redshift::Cluster | io.kloudformation.resource.aws.redshift.Cluster |
| AWS::Redshift::ClusterParameterGroup | io.kloudformation.resource.aws.redshift.ClusterParameterGroup |
| AWS::Redshift::ClusterSecurityGroup | io.kloudformation.resource.aws.redshift.ClusterSecurityGroup |
| AWS::Redshift::ClusterSecurityGroupIngress | io.kloudformation.resource.aws.redshift.ClusterSecurityGroupIngress |
| AWS::Redshift::ClusterSubnetGroup | io.kloudformation.resource.aws.redshift.ClusterSubnetGroup |
|**AWS RoboMaker**||
| AWS::RoboMaker::Fleet | io.kloudformation.resource.aws.robomaker.Fleet |
| AWS::RoboMaker::Robot | io.kloudformation.resource.aws.robomaker.Robot |
| AWS::RoboMaker::RobotApplication | io.kloudformation.resource.aws.robomaker.RobotApplication |
| AWS::RoboMaker::RobotApplicationVersion | io.kloudformation.resource.aws.robomaker.RobotApplicationVersion |
| AWS::RoboMaker::SimulationApplication | io.kloudformation.resource.aws.robomaker.SimulationApplication |
| AWS::RoboMaker::SimulationApplicationVersion | io.kloudformation.resource.aws.robomaker.SimulationApplicationVersion |
|**AWS Route53**||
| AWS::Route53::HealthCheck | io.kloudformation.resource.aws.route53.HealthCheck |
| AWS::Route53::HostedZone | io.kloudformation.resource.aws.route53.HostedZone |
| AWS::Route53::RecordSet | io.kloudformation.resource.aws.route53.RecordSet |
| AWS::Route53::RecordSetGroup | io.kloudformation.resource.aws.route53.RecordSetGroup |
|**AWS Route53Resolver**||
| AWS::Route53Resolver::ResolverEndpoint | io.kloudformation.resource.aws.route53resolver.ResolverEndpoint |
| AWS::Route53Resolver::ResolverRule | io.kloudformation.resource.aws.route53resolver.ResolverRule |
| AWS::Route53Resolver::ResolverRuleAssociation | io.kloudformation.resource.aws.route53resolver.ResolverRuleAssociation |
|**AWS S3**||
| AWS::S3::Bucket | io.kloudformation.resource.aws.s3.Bucket |
| AWS::S3::BucketPolicy | io.kloudformation.resource.aws.s3.BucketPolicy |
|**AWS SDB**||
| AWS::SDB::Domain | io.kloudformation.resource.aws.sdb.Domain |
|**AWS SES**||
| AWS::SES::ConfigurationSet | io.kloudformation.resource.aws.ses.ConfigurationSet |
| AWS::SES::ConfigurationSetEventDestination | io.kloudformation.resource.aws.ses.ConfigurationSetEventDestination |
| AWS::SES::ReceiptFilter | io.kloudformation.resource.aws.ses.ReceiptFilter |
| AWS::SES::ReceiptRule | io.kloudformation.resource.aws.ses.ReceiptRule |
| AWS::SES::ReceiptRuleSet | io.kloudformation.resource.aws.ses.ReceiptRuleSet |
| AWS::SES::Template | io.kloudformation.resource.aws.ses.Template |
|**AWS SNS**||
| AWS::SNS::Subscription | io.kloudformation.resource.aws.sns.Subscription |
| AWS::SNS::Topic | io.kloudformation.resource.aws.sns.Topic |
| AWS::SNS::TopicPolicy | io.kloudformation.resource.aws.sns.TopicPolicy |
|**AWS SQS**||
| AWS::SQS::Queue | io.kloudformation.resource.aws.sqs.Queue |
| AWS::SQS::QueuePolicy | io.kloudformation.resource.aws.sqs.QueuePolicy |
|**AWS SSM**||
| AWS::SSM::Association | io.kloudformation.resource.aws.ssm.Association |
| AWS::SSM::Document | io.kloudformation.resource.aws.ssm.Document |
| AWS::SSM::MaintenanceWindow | io.kloudformation.resource.aws.ssm.MaintenanceWindow |
| AWS::SSM::MaintenanceWindowTarget | io.kloudformation.resource.aws.ssm.MaintenanceWindowTarget |
| AWS::SSM::MaintenanceWindowTask | io.kloudformation.resource.aws.ssm.MaintenanceWindowTask |
| AWS::SSM::Parameter | io.kloudformation.resource.aws.ssm.Parameter |
| AWS::SSM::PatchBaseline | io.kloudformation.resource.aws.ssm.PatchBaseline |
| AWS::SSM::ResourceDataSync | io.kloudformation.resource.aws.ssm.ResourceDataSync |
|**AWS SageMaker**||
| AWS::SageMaker::Endpoint | io.kloudformation.resource.aws.sagemaker.Endpoint |
| AWS::SageMaker::EndpointConfig | io.kloudformation.resource.aws.sagemaker.EndpointConfig |
| AWS::SageMaker::Model | io.kloudformation.resource.aws.sagemaker.Model |
| AWS::SageMaker::NotebookInstance | io.kloudformation.resource.aws.sagemaker.NotebookInstance |
| AWS::SageMaker::NotebookInstanceLifecycleConfig | io.kloudformation.resource.aws.sagemaker.NotebookInstanceLifecycleConfig |
|**AWS SecretsManager**||
| AWS::SecretsManager::ResourcePolicy | io.kloudformation.resource.aws.secretsmanager.ResourcePolicy |
| AWS::SecretsManager::RotationSchedule | io.kloudformation.resource.aws.secretsmanager.RotationSchedule |
| AWS::SecretsManager::Secret | io.kloudformation.resource.aws.secretsmanager.Secret |
| AWS::SecretsManager::SecretTargetAttachment | io.kloudformation.resource.aws.secretsmanager.SecretTargetAttachment |
|**AWS ServiceCatalog**||
| AWS::ServiceCatalog::AcceptedPortfolioShare | io.kloudformation.resource.aws.servicecatalog.AcceptedPortfolioShare |
| AWS::ServiceCatalog::CloudFormationProduct | io.kloudformation.resource.aws.servicecatalog.CloudFormationProduct |
| AWS::ServiceCatalog::CloudFormationProvisionedProduct | io.kloudformation.resource.aws.servicecatalog.CloudFormationProvisionedProduct |
| AWS::ServiceCatalog::LaunchNotificationConstraint | io.kloudformation.resource.aws.servicecatalog.LaunchNotificationConstraint |
| AWS::ServiceCatalog::LaunchRoleConstraint | io.kloudformation.resource.aws.servicecatalog.LaunchRoleConstraint |
| AWS::ServiceCatalog::LaunchTemplateConstraint | io.kloudformation.resource.aws.servicecatalog.LaunchTemplateConstraint |
| AWS::ServiceCatalog::Portfolio | io.kloudformation.resource.aws.servicecatalog.Portfolio |
| AWS::ServiceCatalog::PortfolioPrincipalAssociation | io.kloudformation.resource.aws.servicecatalog.PortfolioPrincipalAssociation |
| AWS::ServiceCatalog::PortfolioProductAssociation | io.kloudformation.resource.aws.servicecatalog.PortfolioProductAssociation |
| AWS::ServiceCatalog::PortfolioShare | io.kloudformation.resource.aws.servicecatalog.PortfolioShare |
| AWS::ServiceCatalog::TagOption | io.kloudformation.resource.aws.servicecatalog.TagOption |
| AWS::ServiceCatalog::TagOptionAssociation | io.kloudformation.resource.aws.servicecatalog.TagOptionAssociation |
|**AWS ServiceDiscovery**||
| AWS::ServiceDiscovery::HttpNamespace | io.kloudformation.resource.aws.servicediscovery.HttpNamespace |
| AWS::ServiceDiscovery::Instance | io.kloudformation.resource.aws.servicediscovery.Instance |
| AWS::ServiceDiscovery::PrivateDnsNamespace | io.kloudformation.resource.aws.servicediscovery.PrivateDnsNamespace |
| AWS::ServiceDiscovery::PublicDnsNamespace | io.kloudformation.resource.aws.servicediscovery.PublicDnsNamespace |
| AWS::ServiceDiscovery::Service | io.kloudformation.resource.aws.servicediscovery.Service |
|**AWS StepFunctions**||
| AWS::StepFunctions::Activity | io.kloudformation.resource.aws.stepfunctions.Activity |
| AWS::StepFunctions::StateMachine | io.kloudformation.resource.aws.stepfunctions.StateMachine |
|**AWS WAF**||
| AWS::WAF::ByteMatchSet | io.kloudformation.resource.aws.waf.ByteMatchSet |
| AWS::WAF::IPSet | io.kloudformation.resource.aws.waf.IPSet |
| AWS::WAF::Rule | io.kloudformation.resource.aws.waf.Rule |
| AWS::WAF::SizeConstraintSet | io.kloudformation.resource.aws.waf.SizeConstraintSet |
| AWS::WAF::SqlInjectionMatchSet | io.kloudformation.resource.aws.waf.SqlInjectionMatchSet |
| AWS::WAF::WebACL | io.kloudformation.resource.aws.waf.WebACL |
| AWS::WAF::XssMatchSet | io.kloudformation.resource.aws.waf.XssMatchSet |
|**AWS WAFRegional**||
| AWS::WAFRegional::ByteMatchSet | io.kloudformation.resource.aws.wafregional.ByteMatchSet |
| AWS::WAFRegional::IPSet | io.kloudformation.resource.aws.wafregional.IPSet |
| AWS::WAFRegional::Rule | io.kloudformation.resource.aws.wafregional.Rule |
| AWS::WAFRegional::SizeConstraintSet | io.kloudformation.resource.aws.wafregional.SizeConstraintSet |
| AWS::WAFRegional::SqlInjectionMatchSet | io.kloudformation.resource.aws.wafregional.SqlInjectionMatchSet |
| AWS::WAFRegional::WebACL | io.kloudformation.resource.aws.wafregional.WebACL |
| AWS::WAFRegional::WebACLAssociation | io.kloudformation.resource.aws.wafregional.WebACLAssociation |
| AWS::WAFRegional::XssMatchSet | io.kloudformation.resource.aws.wafregional.XssMatchSet |
|**AWS WorkSpaces**||
| AWS::WorkSpaces::Workspace | io.kloudformation.resource.aws.workspaces.Workspace |
|**Alexa ASK**||
| Alexa::ASK::Skill | io.kloudformation.resource.alexa.ask.Skill |