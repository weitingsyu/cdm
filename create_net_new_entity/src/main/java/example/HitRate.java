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

import org.apache.commons.lang3.StringUtils;

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
public class HitRate {

        public static void create(CdmCorpusDefinition cdmCorpus, CdmManifestDefinition manifestAbstract,
                        CdmFolderDefinition localRoot, String schemaDocsRoot, String entityName,
                        String foundationJsonPath, String extendEntity)
                        throws ExecutionException, InterruptedException {

                System.out.println("Create " + entityName + " entities");
                // Create an entity which extends "Account" from the standard, it contains
                // everything that "Account" has
                CdmEntityDefinition entity = cdmCorpus.makeObject(CdmObjectType.EntityDef, entityName, false);
                // This function with 'true' will make a simple reference to the base
                if (StringUtils.isNotEmpty(extendEntity)) {
                        entity.setExtendsEntity(cdmCorpus.makeObject(CdmObjectType.EntityRef, extendEntity, true));
                }
                String attrExplanation = "This is a simple custom performance for this sample.";
                // Add a relationship from it to the CustomAccount entity, and name the foreign
                // key to SimpleCustomAccount
                // You can all CreateSimpleAttributeForRelationshipBetweenTwoEntities() instead,
                // but CreateAttributeForRelationshipBetweenTwoEntities() can show
                // more details of how to use resolution guidance to customize your data
                CdmTypeAttributeDefinition revenueHitRate = EntityAttributeUtils
                                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "revenueHitRate", "hasA",
                                                "float");
                entity.getAttributes().add(revenueHitRate);
                CdmTypeAttributeDefinition operationIncomeHitRate = EntityAttributeUtils
                                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "operationIncomeHitRate",
                                                "hasA", "float");
                entity.getAttributes().add(operationIncomeHitRate);
                CdmTypeAttributeDefinition mvaHitRate = EntityAttributeUtils
                                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "mvaHitRate", "hasA", "float");
                entity.getAttributes().add(mvaHitRate);
                CdmTypeAttributeDefinition mohHitRate = EntityAttributeUtils
                                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "mohHitRate", "hasA", "float");
                entity.getAttributes().add(mohHitRate);
                CdmTypeAttributeDefinition invoiceQtyHitRate = EntityAttributeUtils
                                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "invoiceQtyHitRate", "hasA",
                                                "float");
                entity.getAttributes().add(invoiceQtyHitRate);
                CdmTypeAttributeDefinition productionQtyHitRate = EntityAttributeUtils
                                .createEntityAttributeWithPurposeAndDataType(cdmCorpus, "productionQtyHitRate", "hasA",
                                                "float");
                entity.getAttributes().add(productionQtyHitRate);

                entity.setDisplayName(entityName);
                entity.setVersion("0.0.1");
                entity.setDescription("This is a hit rate entity.");

                CdmDocumentDefinition extendedStandardAccountEntityDoc = cdmCorpus.makeObject(CdmObjectType.DocumentDef,
                                entityName + ".cdm.json", false);
                // Add an import to the foundations doc so the traits about partitons will
                // resolve nicely
                extendedStandardAccountEntityDoc.getImports().add(foundationJsonPath);
                // The ExtendedAccount entity extends from the "Account" entity from standards,
                // the import to the entity Account's doc is required
                // it also has a relationship with the CustomAccount entity, the relationship
                // defined from its from its attribute with traits, the import to the entity
                // reference CustomAccount's doc is required
                extendedStandardAccountEntityDoc.getImports().add(schemaDocsRoot + "/FinancialBasic.cdm.json");

                // Add the document to the root of the local documents in the corpus
                localRoot.getDocuments().add(extendedStandardAccountEntityDoc);
                extendedStandardAccountEntityDoc.getDefinitions().add(entity);
                // Add the entity to the manifest
                manifestAbstract.getEntities().add(entity);

                // Create the resolved version of everything in the root folder too
                System.out.println(entityName + " -- Resolve the placeholder");
        }

}
