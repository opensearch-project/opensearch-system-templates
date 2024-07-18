/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.system.applicationtemplates;

import org.opensearch.cluster.applicationtemplates.SystemTemplateMetadata;
import org.opensearch.cluster.applicationtemplates.SystemTemplateRepository;
import org.opensearch.cluster.applicationtemplates.TemplateRepositoryMetadata;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalSystemTemplateRepositoryTests extends OpenSearchTestCase {

    private List<SystemTemplateMetadata> templateMetadataList = List.of(
        SystemTemplateMetadata.fromComponentTemplateInfo("logs-general", 1L),
        SystemTemplateMetadata.fromComponentTemplateInfo("metrics-general", 1L)
    );

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
            repository.listTemplates().forEach(templateMetadata -> {
                counter.incrementAndGet();

            });

            assertEquals(counter.get(), 2);
        }
    }

    public void testRepositoryGetTemplate() throws Exception {
        try (SystemTemplateRepository repository = new LocalSystemTemplateRepository()) {
            AtomicInteger counter = new AtomicInteger();
            repository.listTemplates().forEach(templateMetadata -> counter.incrementAndGet());

            assertEquals(counter.get(), 2);
        }
    }
}
