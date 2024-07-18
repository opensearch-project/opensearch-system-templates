/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.system.applicationtemplates;

import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.get.GetIndexRequest;
import org.opensearch.action.admin.indices.template.get.GetComponentTemplateAction;
import org.opensearch.action.admin.indices.template.put.PutComposableIndexTemplateAction;
import org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.opensearch.cluster.metadata.Context;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.plugins.Plugin;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST)
public class CreateIndexTemplateWithContextIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(ApplicationBasedConfigurationSystemTemplatesPlugin.class);
    }

    public void testCreateIndexTemplateWithContext() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(1);

        Thread.sleep(10000);

        client().admin()
            .indices()
            .execute(GetComponentTemplateAction.INSTANCE, new GetComponentTemplateAction.Request())
            .actionGet()
            .getComponentTemplates()
            .forEach((k, v) -> {
                System.out.println("COMPONENT TEMPLATE::: " + k);
            });
        // Add context to an index template
        client().admin()
            .indices()
            .execute(
                PutComposableIndexTemplateAction.INSTANCE,
                new PutComposableIndexTemplateAction.Request("my-logs").indexTemplate(
                    new ComposableIndexTemplate(
                        List.of("my-logs-*"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new Context("logs-general", "_latest", Map.of())
                    )
                )
            )
            .actionGet(new TimeValue(30000));

        String indexName = "my-logs-1";
        client().admin().indices().create(new CreateIndexRequest(indexName)).actionGet(new TimeValue(30000));

        Map<String, Settings> allSettings = client().admin()
            .indices()
            .getIndex(new GetIndexRequest().indices(indexName))
            .actionGet(new TimeValue(30000))
            .settings();

        assertEquals("best_compression", allSettings.get(indexName).get("index.codec"));
        assertEquals("60s", allSettings.get(indexName).get("index.refresh_interval"));
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings baseSettings = super.nodeSettings(nodeOrdinal);
        return Settings.builder()
            .put(baseSettings)
            .put("cluster.application_templates.enabled", true)
            .put("logger.org.opensearch", "DEBUG")
            .build();
    }
}
