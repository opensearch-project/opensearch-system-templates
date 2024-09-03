/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.system.applicationtemplates;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.Request;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.common.Strings;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.engine.EngineConfig;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;

public class CreateIndexTemplateWithContextTemplateIT extends OpenSearchRestTestCase {

    @SuppressWarnings("unchecked")
    public void testCreateIndexWithContextBasedTemplate() throws Exception {

        final String indexTemplate = "my-metrics-template";
        final String index = "my-metrics-1";

        Request request = new Request("PUT", "/_index_template/" + indexTemplate);
        String content = "{\n"
            + "    \"index_patterns\": [\n"
            + "        \"my-metrics-*\"\n"
            + "    ],\n"
            + "    \"context\": {\n"
            + "        \"name\": \"metrics\"\n"
            + "    }\n"
            + "}";

        request.setJsonEntity(content);

        // Create index template
        client().performRequest(request);

        // creating index
        createIndex(index, Settings.builder().put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0).build());

        Map<String, Object> currentIndexSettings = (Map<String, Object>) ((Map<String, Object>) getIndexSettings(index).get(index)).get(
            "settings"
        );
        assertEquals(currentIndexSettings.get(IndexSettings.INDEX_REFRESH_INTERVAL_SETTING.getKey()), "60s");
        assertEquals(currentIndexSettings.get(IndexSettings.INDEX_MERGE_POLICY.getKey()), "log_byte_size");
        assertEquals(currentIndexSettings.get(EngineConfig.INDEX_CODEC_SETTING.getKey()), "zstd_no_dict");

        try {
            ensureGreen(index);
        } finally {
            deleteIndex(index);
        }
    }

    @Override
    protected RestClient buildClient(Settings settings, HttpHost[] hosts) throws IOException {
        RestClientBuilder builder = RestClient.builder(hosts);
        configureHttpOrHttpsClient(builder, settings);
        builder.setStrictDeprecationMode(true);
        return builder.build();
    }

    protected void configureHttpOrHttpsClient(RestClientBuilder builder, Settings settings) throws IOException {
        configureClient(builder, settings);

        if (getProtocol().equalsIgnoreCase("https")) {
            final String username = System.getProperty("user");
            if (Strings.isNullOrEmpty(username)) {
                throw new RuntimeException("user name is missing");
            }

            final String password = System.getProperty("password");
            if (Strings.isNullOrEmpty(password)) {
                throw new RuntimeException("password is missing");
            }

            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

            try {
                final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();

                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                });
            } catch (final NoSuchAlgorithmException | KeyManagementException | KeyStoreException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    protected String getProtocol() {
        return Objects.equals(System.getProperty("https"), "true") ? "https" : "http";
    }

    /**
     * wipeAllIndices won't work since it cannot delete security index
     */
    @Override
    protected boolean preserveIndicesUponCompletion() {
        return true;
    }
}
