/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.aws.redshift;

import com.appdynamics.extensions.aws.SingleNamespaceCloudwatchMonitor;
import com.appdynamics.extensions.aws.collectors.NamespaceMetricStatisticsCollector;
import com.appdynamics.extensions.aws.config.Configuration;
import com.appdynamics.extensions.aws.metric.processors.MetricsProcessor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.aws.redshift.util.Constants.DEFAULT_METRIC_PREFIX;
import static com.appdynamics.extensions.aws.redshift.util.Constants.MONITOR_NAME;

/**
 * @author Vishaka Sekar
 */
public class RedshiftMonitor extends SingleNamespaceCloudwatchMonitor<Configuration> {

    private static final Logger LOGGER = Logger.getLogger(RedshiftMonitor.class);

    public RedshiftMonitor(){super(Configuration.class);}

    @Override
    protected int getTaskCount() {
        List list = Lists.newArrayList();
        Map<String, ?> map = Maps.newHashMap();
        list.add(map);
        return 3;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    protected NamespaceMetricStatisticsCollector getNamespaceMetricsCollector(Configuration config) {
        MetricsProcessor metricsProcessor = createMetricsProcessor(config);

        return new NamespaceMetricStatisticsCollector
                .Builder(config.getAccounts(),
                config.getConcurrencyConfig(),
                config.getMetricsConfig(),
                metricsProcessor,
                config.getMetricPrefix())
                .withCredentialsDecryptionConfig(config.getCredentialsDecryptionConfig())
                .withProxyConfig(config.getProxyConfig())
                .build();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    private MetricsProcessor createMetricsProcessor(Configuration config) {
        return new RedshiftMetricsProcessor(
                config.getMetricsConfig().getIncludeMetrics(), config.getDimensions());
    }




}
