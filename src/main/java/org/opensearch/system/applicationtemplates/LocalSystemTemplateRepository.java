/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.system.applicationtemplates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.cluster.applicationtemplates.SystemTemplate;
import org.opensearch.cluster.applicationtemplates.SystemTemplateMetadata;
import org.opensearch.cluster.applicationtemplates.SystemTemplateRepository;
import org.opensearch.cluster.applicationtemplates.TemplateRepositoryMetadata;
import org.opensearch.common.util.io.Streams;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.core.common.bytes.BytesArray;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.xcontent.DeprecationHandler;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository implementation for this plugin.
 */
public class LocalSystemTemplateRepository implements SystemTemplateRepository {

    private static final Logger logger = LogManager.getLogger(LocalSystemTemplateRepository.class);

    static final String REPOSITORY_ID = "__core__";
    static final long CURRENT_REPO_VERSION = 1L;

    @Override
    public TemplateRepositoryMetadata metadata() {
        return new TemplateRepositoryMetadata(REPOSITORY_ID, CURRENT_REPO_VERSION);
    }

    @Override
    public Iterable<SystemTemplateMetadata> listTemplates() throws IOException {
        List<SystemTemplateMetadata> templateMetadataList = new ArrayList<>();

        try (
            InputStream is = getResourceAsStream("templates.json");
            XContentParser listParser = JsonXContent.jsonXContent.createParser(
                NamedXContentRegistry.EMPTY,
                DeprecationHandler.IGNORE_DEPRECATIONS,
                is
            )
        ) {
            while (listParser.currentToken() != XContentParser.Token.START_ARRAY) {
                listParser.nextToken();
            }
            if (!"templates".equals(listParser.currentName())) {
                throw new IllegalArgumentException("Format of template metadata file does not match expected format");
            }

            while (listParser.nextToken() != XContentParser.Token.END_ARRAY) {
                if (listParser.currentToken() != XContentParser.Token.START_OBJECT) {
                    throw new IllegalArgumentException(
                        "Format of template metadata file does not match expected format" + listParser.currentToken()
                    );
                }
                String templateName = null;
                String templateType = null;
                long templateVersion = 0L;

                String name = null;
                while (listParser.nextToken() != XContentParser.Token.END_OBJECT) {
                    XContentParser.Token currentToken = listParser.currentToken();
                    if (currentToken == XContentParser.Token.FIELD_NAME) {
                        name = listParser.currentName();
                    } else if (currentToken == XContentParser.Token.VALUE_STRING) {
                        if ("name".equals(name)) {
                            templateName = listParser.text();
                        } else if ("type".equals(name)) {
                            templateType = listParser.text();
                        } else {
                            throw new IllegalArgumentException("Unexpected token " + currentToken);
                        }
                    } else if (currentToken == XContentParser.Token.VALUE_NUMBER) {
                        if ("version".equals(name)) {
                            templateVersion = listParser.longValue();
                        } else {
                            throw new IllegalArgumentException("Unexpected token " + currentToken);
                        }
                    } else {
                        throw new IllegalArgumentException("Unexpected token " + currentToken);
                    }
                }
                if (templateName == null || templateType == null || templateVersion == 0L) {
                    throw new IllegalArgumentException(
                        "Could not read template metadata: [name: "
                            + templateName
                            + " , type: "
                            + templateType
                            + " , version: "
                            + templateVersion
                    );
                }
                templateMetadataList.add(new SystemTemplateMetadata(templateVersion, templateType, templateName));
            }
        } catch (Exception ex) {
            throw new IOException("Could not load system templates: ", ex);
        }
        return templateMetadataList;
    }

    @Override
    public SystemTemplate getTemplate(SystemTemplateMetadata templateMetadata) throws IOException {
        final String fileName = buildFileName(templateMetadata);
        logger.debug("Loading {} from file: {}", templateMetadata, fileName);
        try (InputStream is = getResourceAsStream(fileName)) {
            if (is != null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                Streams.copy(is, out);
                final BytesReference templateContent = new BytesArray(out.toByteArray());
                return new SystemTemplate(templateContent, templateMetadata, metadata());
            } else {
                throw new IOException("Unable to read: " + templateMetadata + " from repository: " + metadata());
            }
        }
    }

    static String buildFileName(SystemTemplateMetadata templateMetadata) {
        return "v" + templateMetadata.version() + "/" + templateMetadata.name() + ".json";
    }

    // Visible for testing (if we need UTs with mocked resources)
    protected InputStream getResourceAsStream(String name) throws IOException {
        return LocalSystemTemplateRepository.class.getResourceAsStream(name);
    }

    @Override
    public void close() throws IOException {}
}
