/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.aws.redshift;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.appdynamics.extensions.aws.config.Dimension;
import com.appdynamics.extensions.aws.config.IncludeMetric;
import com.appdynamics.extensions.aws.dto.AWSMetric;
import com.appdynamics.extensions.aws.metric.*;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessorHelper;
import com.appdynamics.extensions.aws.predicate.MultiDimensionPredicate;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import static com.appdynamics.extensions.aws.redshift.util.Constants.METRIC_SEPARATOR;
import static com.appdynamics.extensions.aws.redshift.util.Constants.NAMESPACE;

/**
 * @author Vishaka Sekar
 */
public class RedshiftMetricsProcessor implements MetricsProcessor {
    private List<IncludeMetric> includeMetrics;
    private List<Dimension> dimensions;
    private static final Logger LOGGER = Logger.getLogger(RedshiftMetricsProcessor.class);

    RedshiftMetricsProcessor(List<IncludeMetric> includeMetrics, List<Dimension> dimensions) {
        this.includeMetrics = includeMetrics;
        this.dimensions = dimensions;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public List<AWSMetric> getMetrics(AmazonCloudWatch awsCloudWatch, String accountName, LongAdder awsRequestsCounter) {
        MultiDimensionPredicate multiDimensionPredicate = new MultiDimensionPredicate(dimensions);
        return MetricsProcessorHelper.getFilteredMetrics(awsCloudWatch, awsRequestsCounter, NAMESPACE,
                includeMetrics, null, multiDimensionPredicate);
    }

    @Override
    public StatisticType getStatisticType(AWSMetric awsMetric) {
        return MetricsProcessorHelper.getStatisticType(awsMetric.getIncludeMetric(), includeMetrics);
    }

    @Override
    public List<Metric> createMetricStatsMapForUpload(NamespaceMetricStatistics namespaceMetricStats) {
        List<Metric> stats = new ArrayList<>();
        Map<String, String> dimensionDisplayNameMap = Maps.newHashMap();
        for (Dimension dimension : dimensions) {
            dimensionDisplayNameMap.put(dimension.getName(), dimension.getDisplayName());
        }
        for (AccountMetricStatistics accountMetricStatistics :
                namespaceMetricStats.getAccountMetricStatisticsList()) {
            String accountPrefix = accountMetricStatistics.getAccountName();
            for (RegionMetricStatistics regionMetricStatistics :
                    accountMetricStatistics.getRegionMetricStatisticsList()) {
                String regionPrefix = regionMetricStatistics.getRegion();
                for (MetricStatistic metricStatistic : regionMetricStatistics.getMetricStatisticsList()) {
                    Map<String, String> dimensionValueMap = Maps.newHashMap();
                    for (com.amazonaws.services.cloudwatch.model.Dimension dimension :
                            metricStatistic.getMetric().getMetric().getDimensions()) {
                        dimensionValueMap.put(dimension.getName(), dimension.getValue());
                    }
                    StringBuilder partialMetricPath = new StringBuilder();
                    buildMetricPath(partialMetricPath, true,
                            accountPrefix, regionPrefix);
                    arrangeMetricPathHierarchy(partialMetricPath, dimensionDisplayNameMap, dimensionValueMap);
                    String awsMetricName = metricStatistic.getMetric().getIncludeMetric().getName();
                    buildMetricPath(partialMetricPath, false, awsMetricName);
                    String fullMetricPath = metricStatistic.getMetricPrefix() + partialMetricPath;
                    if (metricStatistic.getValue() != null) {
                        Map<String, Object> metricProperties = new HashMap<>();
                        IncludeMetric metricWithConfig = metricStatistic.getMetric().getIncludeMetric();
                        metricProperties.put("alias", metricWithConfig.getAlias());
                        metricProperties.put("multiplier", metricWithConfig.getMultiplier());
                        metricProperties.put("aggregationType", metricWithConfig.getAggregationType());
                        metricProperties.put("timeRollUpType", metricWithConfig.getTimeRollUpType());
                        metricProperties.put("clusterRollUpType", metricWithConfig.getClusterRollUpType());
                        metricProperties.put("delta", metricWithConfig.isDelta());
                        Metric metric = new Metric(awsMetricName, Double.toString(metricStatistic.getValue()),
                                fullMetricPath, metricProperties);
                        stats.add(metric);
                    } else {
                        LOGGER.debug(String.format("Ignoring metric [ %s ] which has value null", fullMetricPath));
                    }
                }
            }
        }
        return stats;
    }

    private static void buildMetricPath(StringBuilder partialMetricPath, boolean appendMetricSeparator,
                                        String... elements) {

        for (String element : elements) {
            partialMetricPath.append(element);
            if (appendMetricSeparator) {
                partialMetricPath.append(METRIC_SEPARATOR);
            }
        }
    }

    private void arrangeMetricPathHierarchy(StringBuilder partialMetricPath, Map<String, String> dimensionDisplayNameMap,
                                            Map<String, String> dimensionValueMap) {
        String clusterIDDimension = "ClusterIdentifier";
        String clusterIDDisplayName = dimensionDisplayNameMap.get(clusterIDDimension);

        String nodeIDDimension = "NodeID";
        String nodeIDDisplayName = dimensionDisplayNameMap.get(nodeIDDimension);

        String latencyDimension = "latency";
        String latencyDisplayName = dimensionDisplayNameMap.get(latencyDimension);

        String serviceClassDimension = "service class";
        String serviceClassDimensionName = dimensionDisplayNameMap.get(serviceClassDimension);

        String stageDimension = "stage";
        String stageDimensionDisplayName = dimensionDisplayNameMap.get(stageDimension);

        String wmlidDimension = "wmlid";
        String wmlidDimensionName = dimensionDisplayNameMap.get(wmlidDimension);

        //<Account> | <Region> | Cluster Identifier | <ClusterId> |
        buildMetricPath(partialMetricPath, true , clusterIDDisplayName,
                dimensionValueMap.get(clusterIDDimension) );

        // Adding each node in the cluster
        //<Account> | <Region> | Cluster Identifier | <ClusterId> | Node ID | <NodeId> |
        if(dimensionValueMap.get(nodeIDDimension)!= null){
            buildMetricPath(partialMetricPath, true, nodeIDDisplayName,
                    dimensionValueMap.get(nodeIDDimension));
        }

        // if this is a cluster level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | Latency | <LatencyValue> |
        // else if this is a node-level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | Node ID | <NodeId> | Latency | <LatencyValue> |
        if(dimensionValueMap.get(latencyDimension) != null){
            buildMetricPath(partialMetricPath, true, latencyDisplayName,
                    dimensionValueMap.get(latencyDimension));
        }

        // if this is a cluster-level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | Service Class | <serviceclass> |
        // else if this is a node-level metric,
        //<Account> | <Region> | Cluster Identifier | <ClusterId> | Node ID | <NodeId> | Service Class | <serviceclass> |
        if(dimensionValueMap.get(serviceClassDimension) != null){
            buildMetricPath(partialMetricPath, true, serviceClassDimensionName,
                    dimensionValueMap.get(serviceClassDimension));
        }

        // if this is a cluster-level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | Service Class | <serviceclass> |
        // else if this is a node-level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | Node ID | <NodeId> | Service Class | <serviceclass> |
        if(dimensionValueMap.get(stageDimension) != null){
            buildMetricPath(partialMetricPath, true, stageDimensionDisplayName,
                    dimensionValueMap.get(stageDimension));
        }

        // if this is a cluster-level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | WMLID | <wmlid> |
        // else if this is a node-level metric,
        // <Account> | <Region> | Cluster Identifier | <ClusterId> | Node ID | <NodeId> | WMLID | <wmlid> |
        if(dimensionValueMap.get(wmlidDimension) != null){
            buildMetricPath(partialMetricPath, true, wmlidDimensionName,
                    dimensionValueMap.get(wmlidDimension));
        }
    }
}
