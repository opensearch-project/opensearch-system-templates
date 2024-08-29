/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.system.applicationtemplates;

import org.opensearch.cluster.applicationtemplates.SystemTemplate;
import org.opensearch.cluster.applicationtemplates.SystemTemplateMetadata;
import org.opensearch.cluster.applicationtemplates.SystemTemplateRepository;
import org.opensearch.cluster.applicationtemplates.TemplateRepositoryMetadata;
import org.opensearch.test.OpenSearchTestCase;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalSystemTemplateRepositoryTests extends OpenSearchTestCase {

    public void testRepositoryMetadata() throws Exception {
        try (SystemTemplateRepository repository = new LocalSystemTemplateRepository()) {
            TemplateRepositoryMetadata repositoryMetadata = repository.metadata();
            assertNotNull(repositoryMetadata);
            assertEquals(repositoryMetadata.id(), LocalSystemTemplateRepository.REPOSITORY_ID);
            assertEquals(repositoryMetadata.version(), LocalSystemTemplateRepository.CURRENT_REPO_VERSION);
        }
    }

    public void testRepositoryListTemplates() throws Exception {
        try (SystemTemplateRepository repository = new LocalSystemTemplateRepository()) {
            AtomicInteger counter = new AtomicInteger();
            repository.listTemplates().forEach(templateMetadata -> { counter.incrementAndGet(); });

            assertEquals(counter.get(), 8);
        }
    }

    public void testRepositoryGetTemplate() throws Exception {
        try (SystemTemplateRepository repository = new LocalSystemTemplateRepository()) {
            for (SystemTemplateMetadata templateMetadata : repository.listTemplates()) {
                SystemTemplate template = repository.getTemplate(templateMetadata);
                assertEquals(templateMetadata, template.templateMetadata());
                assertNotNull(template.templateContent());

                try (InputStream is = this.getClass().getResourceAsStream(LocalSystemTemplateRepository.buildFileName(templateMetadata))) {
                    String expectedTemplateContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    assertEquals(template.templateContent().utf8ToString(), expectedTemplateContent);
                }
            }
        }
    }
}
