/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.system.applicationtemplates;

import org.opensearch.transport.client.Client;
import org.opensearch.cluster.applicationtemplates.ClusterStateSystemTemplateLoader;
import org.opensearch.cluster.applicationtemplates.SystemTemplateLoader;
import org.opensearch.cluster.applicationtemplates.SystemTemplateMetadata;
import org.opensearch.cluster.applicationtemplates.SystemTemplateRepository;
import org.opensearch.cluster.applicationtemplates.SystemTemplatesPlugin;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.plugins.Plugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.opensearch.cluster.applicationtemplates.SystemTemplateMetadata.COMPONENT_TEMPLATE_TYPE;

public class ApplicationBasedConfigurationSystemTemplatesPlugin extends Plugin implements SystemTemplatesPlugin {

    private ClusterService clusterService;
    private Client client;

    private final Map<String, SystemTemplateLoader> loaders = new HashMap<>();

    public ApplicationBasedConfigurationSystemTemplatesPlugin() {}

    @Override
    public Collection<Object> createComponents(
        Client client,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        this.clusterService = clusterService;
        this.client = client;
        return super.createComponents(
            client,
            clusterService,
            threadPool,
            resourceWatcherService,
            scriptService,
            xContentRegistry,
            environment,
            nodeEnvironment,
            namedWriteableRegistry,
            indexNameExpressionResolver,
            repositoriesServiceSupplier
        );
    }

    @Override
    public SystemTemplateRepository loadRepository() throws IOException {
        return new LocalSystemTemplateRepository();
    }

    @Override
    public SystemTemplateLoader loaderFor(SystemTemplateMetadata templateMetadata) {
        return loaders.computeIfAbsent(templateMetadata.type(), k -> {
            if (COMPONENT_TEMPLATE_TYPE.equals(templateMetadata.type())) {
                return new ClusterStateSystemTemplateLoader(client, () -> clusterService.state());
            }
            return null;
        });
    }
}
