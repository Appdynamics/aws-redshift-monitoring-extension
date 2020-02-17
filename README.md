# AWS Redshift Monitoring Extension

## Use Case
Captures AWS Redshift statistics from Amazon CloudWatch and displays them in the AppDynamics Metric Browser.

## Prerequisites
1. Please give the following permissions to the account being used to with the extension.
    ```
    cloudwatch:ListMetrics
    cloudwatch:GetMetricStatistics
    ```
2. In order to use this extension, you do need a [Standalone JAVA Machine Agent](https://docs.appdynamics.com/display/PRO44/Standalone+Machine+Agents) or [SIM Agent](https://docs.appdynamics.com/display/PRO44/Server+Visibility).  For more details on downloading these products, please  visit [here](https://download.appdynamics.com/).
3. The extension needs to be able to connect to AWS CloudWatch in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product using access key and secret key, or have an agent running on EC2 instance, which you can use with instance profile.
4. **Agent Compatibility:**
   **Note** : This extension is compatible with Machine Agent version 4.5.13 or later.
   If you are seeing warning messages while starting the Machine Agent, update the http-client and http-core JARs in `{MACHINE_AGENT_HOME}/monitorsLibs` to httpclient-4.5.9 and httpcore-4.4.12 to make this warning go away. 
   To make this extension work on Machine Agent < 4.5.13: The http-client and http-core JARs in `{MACHINE_AGENT_HOME}/monitorsLibs` has to be manually be updated to httpclient-4.5.9 and httpcore-4.4.12


## Installation
1. Run 'mvn clean install' from `aws-redshift-monitoring-extension`
2. Copy and unzip `AWSRedshiftMonitor-\<version\>.zip` from `target` directory into ` \<machine_agent_dir\>/monitors/`.<br/>Please place the extension in the <b>"monitors"</b> directory of your Machine Agent installation directory. Do not place the extension in the <b>"extensions"</b> directory of your Machine Agent installation directory.
3. Edit config.yml file in AWSRedshiftMonitor/conf and provide the required configuration (see Configuration section)
4. Restart the Machine Agent.


## Configuration
In order to use the extension, you need to update the config.yml file that is present in the extension folder. The following is a step-by-step explanation of the configurable fields that are present in the `config.yml` file.

1. If SIM is enabled, then use the following metricPrefix -

   `metricPrefix: "Custom Metrics|AWS Redshift|"`

   Else, configure the "**COMPONENT_ID**" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in
   `metricPrefix: "Server|Component:<COMPONENT_ID>|Custom Metrics|AWS Redshift|"`.
   For example,

    `metricPrefix: "Server|Component:100|Custom Metrics|AWS Redshift|"`

2. Provide **accessKey**(required) and **secretKey**(required) of your account(s), also provide **displayAccountName**(any name that represents your account) and
   **regions**(required). If you are running this extension inside an EC2 instance which has **IAM profile** configured then you don't have to configure **accessKey** and  **secretKey** values, extension will use **IAM profile** to authenticate. You can provide multiple accounts and regions as below -
   ~~~
   accounts:
     - awsAccessKey: "XXXXXXXX1"
       awsSecretKey: "XXXXXXXXXX1"
       displayAccountName: "TestAccount_1"
       regions: ["us-east-1","us-west-1","us-west-2"]

     - awsAccessKey: "XXXXXXXX2"
       awsSecretKey: "XXXXXXXXXX2"
       displayAccountName: "TestAccount_2"
       regions: ["eu-central-1","eu-west-1"]
   ~~~
3. If you want to encrypt the **awsAccessKey** and **awsSecretKey** then follow the "Credentials Encryption" section and provide the encrypted values in **awsAccessKey** and **awsSecretKey**. Configure `enableDecryption` of `credentialsDecryptionConfig` to `true` and provide the encryption key in `encryptionKey`.
   For example,
   ```
   #Encryption key for Encrypted password.
   credentialsDecryptionConfig:
       enableDecryption: "true"
       encryptionKey: "XXXXXXXX"
   ```

4. Provide all valid proxy information if you use it. If not, leave this section as is.

       proxyConfig:
         host:
         port:
         username:
         password:


5. To report metrics only from specific dimension values, configure the `dimension` section as below -

    ```
    dimensions:
       - name: "latency"
         displayName: "Latency"
         values: ["short", "long", "medium"]
       - name: "NodeID"
         displayName: "Node ID"
         values: ["Leader", "Compute-0", "Compute-1"]
       - name: "ClusterIdentifier"
         displayName: "Cluster ID"
         values: ["my-redshift"]
       - name: "service class"
         displayName: "Service Class"
         values: ["6","5","15","14"]
       - name: "Stage"
         displayName: "Query Execution Stage"
         values: [""]
       - name: "wmlid"
         displayName: "Workload Mgmt Queue"
         values: [""]

    ```

   If these fields are left empty `[]`, the metrics which require that dimension will not be reported.
   In order to monitor everything under a dimension, you can simply use ".*" to pull everything from your AWS Environment.

6.  Configure the metrics section.
    For configuring the metrics, the following properties can be used:

    |     Property      |   Default value |         Possible values         |                                              Description                                                                                                |
    | :---------------- | :-------------- | :------------------------------ | :------------------------------------------------------------------------------------------------------------- |
    | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
    | statType          | "ave"           | "AVERAGE", "SUM", "MIN", "MAX"  | AWS configured values as returned by API                                                                       |
    | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)    |
    | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)   |
    | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/PRO44/Build+a+Monitoring+Extension+Using+Java)|
    | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
    | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:0, DOWN:1  |
    | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |

   For example,
   ```
         - name: "CPUUtilization"
           alias: "CPU Utilization (Unit - %; StatType - ave)"
           statType: "ave"
           delta: false
           multiplier: 1
           aggregationType: "AVERAGE"
           timeRollUpType: "AVERAGE"
           clusterRollUpType: "INDIVIDUAL"
   ```

   **All these metric properties are optional, and the default value shown in the table is applied to the metric(if a property has not been specified) by default.**


7. CloudWatch metrics are delivered on a best-effort basis. This means that the delivery of metrics is not guaranteed to be on-time.
  There may be a case where the metric is updated in CloudWatch much later than when it was processed, with an associated delay.
  For Redshift, the delay can be 4 - 5 minutes. There is a possibility that the extension does not capture the metric, which is the reason there is a time window. The time window allows
  the metric to be updated in CloudWatch before the extension collects it.

    ```
    metricsTimeRange:
      startTimeInMinsBeforeNow: 9
      endTimeInMinsBeforeNow: 4
    ```
8. This field is set as per the defaults suggested by AWS. You can change this if your limit is different.
    ```
    getMetricStatisticsRateLimit: 400
    ```
9. The maximum number of retry attempts for failed requests that can be re-tried.
    ```
    maxErrorRetrySize: 3
    ```
### config.yml

 Please avoid using tab (\t) when editing yaml files. Please copy all the contents of the config.yml file and go to [Yaml Validator](http://www.yamllint.com/) . On reaching the website, paste the contents and press the “Go” button on the bottom left.
 If you get a valid output, that means your formatting is correct and you may move on to the next step.

## Metrics
* The AWS Redshift Extension provides AWS Redshift performance metrics as listed
[here](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/rs-metricscollected.html),
The entire list of metrics and dimensions that are fetched by this extension is listed in [Monitoing AWS Redshift Performance](https://docs.aws.amazon.com/redshift/latest/mgmt/metrics.html) .
* The Query/Load Performance data listed [here](https://docs.aws.amazon.com/redshift/latest/mgmt/metrics-listing.html) is not published as CloudWatch Metrics and hence not monitored as part of this extension. they can be viewed in the Amazon Redshift console.
* AppDynamics AWS Redshift Extension provides node level and cluster level metrics to monitor the performance and health of the clusters.

## Credentials Encryption
Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench

Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130)

## Troubleshooting

Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) to contact the support team.

## Support Tickets

If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) you have not been able to get your extension working, please file a ticket and add the following information.

Please provide the following in order for us to assist you better.

1. Stop the running machine agent.
2. Delete all existing logs under `<MachineAgent>/logs`.
3. Please enable debug logging by editing the file `<MachineAgent>/conf/logging/log4j.xml`. Change the level value of the following `<logger>` elements to debug.
    ```
   <logger name="com.singularity">
   <logger name="com.appdynamics">
   ```

4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory `<MachineAgent>/logs/*`.
5. Attach the zipped `<MachineAgent>/conf/*` directory here.
6. Attach the zipped `<MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith` directory here.

For any support related questions, you can also contact [help@appdynamics.com](mailto:help@appdynamics.com).

## Contributing

Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/aws-redshift-monitoring-extension).

## Version
   |Name|Version|
   |--------------------------|------------|
   |Extension Version         |2.0.2      |
   |Controller Compatibility  |4.4 or Later|
   |Agent Compatibility| Machine Agent version 4.5.13 or later|
   |Last Update               |Feb 04, 2020 |
   |List of Changes           |[Change Log](https://github.com/Appdynamics/aws-redshift-monitoring-extension/blob/master/CHANGELOG.md)|
