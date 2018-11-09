/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.aws.redshift;

import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.AccountMetricStatistics;
import com.appdynamics.extensions.aws.metric.MetricStatistic;
import com.appdynamics.extensions.aws.metric.NamespaceMetricStatistics;
import com.appdynamics.extensions.aws.metric.RegionMetricStatistics;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RedshiftMonitorTest {


    private NamespaceMetricStatistics namespaceMetricStatistics = new NamespaceMetricStatistics();
    @Before
    public void init(){

        List<List<com.amazonaws.services.cloudwatch.model.Dimension>> dimensionsList = new ArrayList<>();
        List<com.amazonaws.services.cloudwatch.model.Metric> metricsList = Lists.newArrayList();
        List<IncludeMetric> includeMetric = Lists.newArrayList();
        List<List<IncludeMetric>> includeMetricsList = Lists.newArrayList();
        List<AWSMetric> awsMetricList = Lists.newArrayList();
        List<MetricStatistic> metricStatisticsList = Lists.newArrayList();

        for(int i = 0 ; i < 6; i++){
            dimensionsList.add(new ArrayList<>());
            metricsList.add(new com.amazonaws.services.cloudwatch.model.Metric());
            includeMetric.add(new IncludeMetric());
            includeMetricsList.add(includeMetric);
            awsMetricList.add(new AWSMetric());
            metricStatisticsList.add(new MetricStatistic());
        }

        com.amazonaws.services.cloudwatch.model.Dimension dimension1 =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension1.withName("ClusterIdentifier").withValue("my-cluster");

        com.amazonaws.services.cloudwatch.model.Dimension dimension2 =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension2.withName("NodeID").withValue("Compute-0");

        com.amazonaws.services.cloudwatch.model.Dimension dimension3 =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension3.withName("latency").withValue("short");

        com.amazonaws.services.cloudwatch.model.Dimension dimension4 =
                new com.amazonaws.services.cloudwatch.model.Dimension();
        dimension4.withName("service class").withValue("6");

        dimensionsList.get(0).add(dimension1); //ClusterIdentifier, NodeID , latency
        dimensionsList.get(0).add(dimension2);
        dimensionsList.get(0).add(dimension3);

        dimensionsList.get(1).add(dimension1);//ClusterIdentifier, NodeID , latency, service class
        dimensionsList.get(1).add(dimension2);
        dimensionsList.get(1).add(dimension4);

        dimensionsList.get(2).add(dimension1); //ClusterIdentifier, latency
        dimensionsList.get(2).add(dimension3);

        dimensionsList.get(3).add(dimension1); //Cluster Identifier, service class
        dimensionsList.get(3).add(dimension4);

        dimensionsList.get(4).add(dimension1); //Cluster level metrics

        dimensionsList.get(5).add(dimension1);
        dimensionsList.get(5).add(dimension2);//Node level metrics

        AccountMetricStatistics accountMetricStatistics = new AccountMetricStatistics();
        accountMetricStatistics.setAccountName("AppD");
        namespaceMetricStatistics.setNamespace("AWS/Redshift");
        RegionMetricStatistics regionMetricStatistics = new RegionMetricStatistics();
        regionMetricStatistics.setRegion("us-west-1");

        for(int i = 0 ; i < 6; i++){

            com.amazonaws.services.cloudwatch.model.Metric metric =  metricsList.get(i);
            metric.setDimensions(dimensionsList.get(i));
            List<IncludeMetric> includeMetrics = includeMetricsList.get(i);
            includeMetrics.get(i).setName("TestMetric"+i);
            includeMetricsList.add(includeMetrics);

            awsMetricList.get(i).setIncludeMetric(includeMetrics.get(i));
            awsMetricList.get(i).setMetric(metric);

            metricStatisticsList.get(i).setValue(1.0);
            metricStatisticsList.get(i).setUnit("testunits");
            metricStatisticsList.get(i).setMetricPrefix("Custom Metric|AWS Redshift|");
            metricStatisticsList.get(i).setMetric(awsMetricList.get(i));

            regionMetricStatistics.addMetricStatistic(metricStatisticsList.get(i));
        }

        accountMetricStatistics.add(regionMetricStatistics);
        namespaceMetricStatistics.add(accountMetricStatistics);

    }

    @Test
    public void whenPrintingMetricThenCheckMetricPath(){

        List<com.appdynamics.extensions.aws.config.Dimension> dimensionsFromConfig = Lists.newArrayList();
        com.appdynamics.extensions.aws.config.Dimension dimension1 =
                new com.appdynamics.extensions.aws.config.Dimension();
        dimension1.setName("ClusterIdentifier");
        dimension1.setDisplayName("Cluster Identifier");
        dimensionsFromConfig.add(dimension1);

        com.appdynamics.extensions.aws.config.Dimension dimension2 =
                new com.appdynamics.extensions.aws.config.Dimension();
        dimension2.setName("NodeID");
        dimension2.setDisplayName("Node ID");
        dimensionsFromConfig.add(dimension2);

        com.appdynamics.extensions.aws.config.Dimension dimension3 =
                new com.appdynamics.extensions.aws.config.Dimension();
        dimension3.setName("latency");
        dimension3.setDisplayName("latency");
        dimensionsFromConfig.add(dimension3);

        com.appdynamics.extensions.aws.config.Dimension dimension4 =
                new com.appdynamics.extensions.aws.config.Dimension();
        dimension4.setName("service class");
        dimension4.setDisplayName("service class");
        dimensionsFromConfig.add(dimension4);
        RedshiftMetricsProcessor redshiftMetricsProcessor =
                new RedshiftMetricsProcessor(new ArrayList(), dimensionsFromConfig);

        List<Metric> stats = redshiftMetricsProcessor.createMetricStatsMapForUpload(namespaceMetricStatistics);

        Assert.assertNotNull(stats);
        Assert.assertEquals(stats.get(0).getMetricPath(),
                "Custom Metric|AWS Redshift|AppD|us-west-1|Cluster Identifier|my-cluster|Node ID|Compute-0|latency|short|TestMetric0" );
        Assert.assertEquals(stats.get(1).getMetricPath(),
                "Custom Metric|AWS Redshift|AppD|us-west-1|Cluster Identifier|my-cluster|Node ID|Compute-0|service class|6|TestMetric1" );
        Assert.assertEquals(stats.get(2).getMetricPath(),
                "Custom Metric|AWS Redshift|AppD|us-west-1|Cluster Identifier|my-cluster|latency|short|TestMetric2" );
        Assert.assertEquals(stats.get(3).getMetricPath(),
                "Custom Metric|AWS Redshift|AppD|us-west-1|Cluster Identifier|my-cluster|service class|6|TestMetric3"  );
        Assert.assertEquals(stats.get(4).getMetricPath(),
                "Custom Metric|AWS Redshift|AppD|us-west-1|Cluster Identifier|my-cluster|TestMetric4");
        Assert.assertEquals(stats.get(5).getMetricPath(),
                "Custom Metric|AWS Redshift|AppD|us-west-1|Cluster Identifier|my-cluster|Node ID|Compute-0|TestMetric5");


    }



}
