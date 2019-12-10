/*
 *   Copyright 2013 - 2019 The Original Authors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.elasticsoftware.elasticactors.configuration;

import org.elasticsoftware.elasticactors.cluster.ClusterService;
import org.elasticsoftware.elasticactors.kubernetes.cluster.KubernetesClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public class ClusteringConfiguration {
    @Autowired
    private Environment env;

    @Bean(name= "clusterService")
    public ClusterService createClusterService() {
        String namespace = env.getProperty("ea.cluster.kubernetes.namespace", "default");
        String name = env.getRequiredProperty("ea.cluster.kubernetes.statefulsetName");
        Integer timeoutSeconds = env.getProperty("ea.cluster.kubernetes.timeoutSeconds", Integer.class, 60);
        String nodeId = env.getRequiredProperty("ea.node.id");
        return new KubernetesClusterService(namespace, name, nodeId, timeoutSeconds);
    }
}