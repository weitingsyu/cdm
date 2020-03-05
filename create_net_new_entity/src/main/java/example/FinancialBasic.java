// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See License.txt in the project root for license information.

package example;

import java.util.concurrent.ExecutionException;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDocumentDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmFolderDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmManifestDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTypeAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.storage.LocalAdapter;

/**
 * ----------------------------------------------------------------------------------------------------------------------------------------
 * This sample is going to simulate the steps a tool would follow in order to
 * create a new manifest document in some user storage folder with two types of
 * entities - a net new entity and an entity extended from some public
 * standards. Note: If we want to create a relationship from a new custom entity
 * to an existing entity which is loaded from some public standards, we need to
 * create an entity extended from the existing entity and add a relationship to
 * the attribute of the new entity. Since we can't modify attributes from an
 * 'abstract' schema defintion in the public standards. This sample also creates
 * a relationship from a net new entity to an existing entity, and a
 * relationship between two net new entities.
 * <p>
 * The steps are: 1. Create a temporary 'manifest' object at the root of the
 * corpus 2. Create two net new entities without extending any existing entity,
 * create a relationship from one to the other, and add them to the manifest 3.
 * Create one entity which extends from the public standards, create a
 * relationship from it to a net new entity, and add the entity to the manifest
 * 4. Make a 'resolved' version of each entity doc in our local folder. Call
 * CreateResolvedManifestAsync on our starting manifest. This will resolve
 * everything and find all of the relationships between entities for us. Please
 * check out the second example 2-create-manifest for more details 5. Save the
 * new document(s)
 * ----------------------------------------------------------------------------------------------------------------------------------------
 */
public class FinancialBasic {

    public static void create(CdmCorpusDefinition cdmCorpus, CdmManifestDefinition manifestAbstract,
            CdmFolderDefinition localRoot, String schemaDocsRoot, String entityName, String fundationJsonPath)
            throws ExecutionException, InterruptedException {

        // Create two entities from scratch, and add some attributes, traits,
        // properties, and relationships in between
        System.out.println("Create " + entityName + " entities");

        // Create the simplest entity - CustomPerson
        // Create the entity definition instance
        CdmEntityDefinition basicInfoEntity = cdmCorpus.makeObject(CdmObjectType.EntityDef, entityName);
        // Add type attributes to the entity instance
        CdmTypeAttributeDefinition bgAttribute = EntityAttributeUtils
                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "bg", "hasA", "string");
        basicInfoEntity.getAttributes().add(bgAttribute);
        CdmTypeAttributeDefinition siteAttribute = EntityAttributeUtils
                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "site", "hasA", "string");
        basicInfoEntity.getAttributes().add(siteAttribute);
        CdmTypeAttributeDefinition productAttribute = EntityAttributeUtils
                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "product", "hasA", "string");
        basicInfoEntity.getAttributes().add(productAttribute);
        CdmTypeAttributeDefinition typeAttribute = EntityAttributeUtils
                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "type", "hasA", "string");
        basicInfoEntity.getAttributes().add(typeAttribute);
        CdmTypeAttributeDefinition reportingMonthAttribute = EntityAttributeUtils
                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "reportingMonth", "hasA", "date");
        basicInfoEntity.getAttributes().add(reportingMonthAttribute);
        // Add properties to the entity instance
        basicInfoEntity.setDisplayName(entityName);
        basicInfoEntity.setVersion("0.0.1");
        basicInfoEntity.setDescription("This is a financial basic entity.");
        // Create the document which contains the entity
        CdmDocumentDefinition basicInfoEntityDoc = cdmCorpus.makeObject(CdmObjectType.DocumentDef,
                entityName + ".cdm.json", false);
        // Add an import to the foundations doc so the traits about partitons will
        // resolve nicely
        basicInfoEntityDoc.getImports().add(fundationJsonPath);
        basicInfoEntityDoc.getDefinitions().add(basicInfoEntity);
        // Add the document to the root of the local documents in the corpus
        localRoot.getDocuments().add(basicInfoEntityDoc);
        // Add the entity to the manifest
        manifestAbstract.getEntities().add(basicInfoEntity);

        // Create the resolved version of everything in the root folder too
        System.out.println(entityName + " -- Resolve the placeholder");

    }

}
