// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See License.txt in the project root for license information.

package example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmAttributeResolutionGuidance;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDataPartitionDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDeclarationDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmFolderDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmManifestDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTypeAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.storage.AdlsAdapter;
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
public class Wistron {

        // The names of the custom entities for this sample.
        private final static String FACTORYINFO_ENTITY_NAME = "Factory";
        private final static String PRODUCT_ENTITY_NAME = "Product";
        private final static String PERFORMANCE_ENTITY_NAME = "Performance";
        private final static String INCOME_STATMENT_ENTITY_NAME = "IncomeStatment";
        private final static String REPORTING_MONTH_ENTITY_NAME = "ReportingMonth";

        private final static String FOUNDATION_JSON_PATH = "cdm:/foundations.cdm.json";
        private final static String pathFromExeToExampleRoot = "./";
        private final static String localPath = "../" + pathFromExeToExampleRoot
                        + "example-public-standards/core/wistron/financial";
        private final static String rootFolderName = "Financial";

        public static void main(String[] args) throws ExecutionException, InterruptedException {
                // Make a corpus, the corpus is the collection of all documents and folders
                // created or discovered while navigating objects and paths
                CdmCorpusDefinition cdmCorpus = new CdmCorpusDefinition();

                System.out.println("configure storage adapters");

                cdmCorpus.getStorage().mount("local", new LocalAdapter(localPath));

                cdmCorpus.getStorage().setDefaultNamespace("local");
                final AdlsAdapter adlsAdapter = new AdlsAdapter("wsdcdmstorageprd.dfs.core.windows.net", // Hostname.
                                "/powerbi/Financial", // Root.
                                "69a44e1e-8420-46e1-b62c-888963ec4c3b", // Tenant ID.
                                "b3b6e653-8e60-4d71-8690-e573c3c5863b", // Client ID.
                                "e8_kP_5a9mYs:RPH?qbDq3YA2uwFC0ds" // Client secret.

                );

                cdmCorpus.getStorage().mount("adls", adlsAdapter);
                cdmCorpus.getStorage().setDefaultNamespace("adls"); // local is our default. so any paths that start
                                                                    // out
                                                                    // navigating
                                                                    // without a device tag will assume local

                System.out.println("Make placeholder manifest");
                // Make the temp manifest and add it to the root of the local documents in the
                // corpus
                CdmManifestDefinition manifestAbstract = cdmCorpus.makeObject(CdmObjectType.ManifestDef,
                                "tempAbstract");

                // Add the temp manifest to the root of the local documents in the corpus
                CdmFolderDefinition localRoot = cdmCorpus.getStorage().fetchRootFolder("local");
                localRoot.getDocuments().add(manifestAbstract);

                // Create two entities from scratch, and add some attributes, traits,
                // properties, and relationships in between

                // Create Factory entity
                CdmEntityDefinition factory = EntityUtils.createEntity(cdmCorpus, localRoot, FACTORYINFO_ENTITY_NAME,
                                getFactoryEntity(), FOUNDATION_JSON_PATH, false);
                manifestAbstract.getEntities().add(factory);

                // Create Product entity
                CdmEntityDefinition product = EntityUtils.createEntity(cdmCorpus, localRoot, PRODUCT_ENTITY_NAME,
                                getProductEntity(), FOUNDATION_JSON_PATH, false);
                manifestAbstract.getEntities().add(product);

                // Create Reporting Month entity
                CdmEntityDefinition reportingMonth = EntityUtils.createEntity(cdmCorpus, localRoot,
                                REPORTING_MONTH_ENTITY_NAME, getReportingMonthEntity(), FOUNDATION_JSON_PATH, false);
                manifestAbstract.getEntities().add(reportingMonth);
                // Create an entity - CustomAccount which has a relationship with the entity
                // CustomPerson
                // Create the entity definition instance
                // List<RelationInfo> relations = new ArrayList<RelationInfo>();

                // relations.add(RelationInfo.builder()//
                // .entityName(FACTORYINFO_ENTITY_NAME)//
                // .columnName("bg")//
                // .description("BG relation").build());
                // relations.add(RelationInfo.builder()//
                // .entityName(FACTORYINFO_ENTITY_NAME)//
                // .columnName("site")//
                // .description("Site relation").build());
                // relations.add(RelationInfo.builder()//
                // .entityName(PRODUCT_ENTITY_NAME)//
                // .columnName("product")//
                // .description("Product Relation").build());

                CdmEntityDefinition performance = EntityUtils.createEntity(cdmCorpus, localRoot,
                                PERFORMANCE_ENTITY_NAME, getPerformanceEntity(), FOUNDATION_JSON_PATH, true);
                manifestAbstract.getEntities().add(performance);

                CdmEntityDefinition incomeStatment = EntityUtils.createEntity(cdmCorpus, localRoot,
                                INCOME_STATMENT_ENTITY_NAME, getIncomeStatmentEntity(), FOUNDATION_JSON_PATH, true);
                manifestAbstract.getEntities().add(incomeStatment);

                CdmManifestDefinition manifestResolved = manifestAbstract.createResolvedManifestAsync("financial", null)
                                .get();

                // Add an import to the foundations doc so the traits about partitions will
                // resolve nicely.
                manifestResolved.getImports().add(FOUNDATION_JSON_PATH);

                System.out.println("Save the documents");
                for (CdmEntityDeclarationDefinition eDef : manifestResolved.getEntities()) {
                        // Get the entity being pointed at
                        CdmEntityDeclarationDefinition localEDef = eDef;
                        CdmEntityDefinition entDef = cdmCorpus.<CdmEntityDefinition>fetchObjectAsync(
                                        localEDef.getEntityPath(), manifestResolved).get();
                        // Make a fake partition, just to demo that
                        CdmDataPartitionDefinition part = cdmCorpus.makeObject(CdmObjectType.DataPartitionDef,
                                        entDef.getEntityName() + "-data-description");
                        localEDef.getDataPartitions().add(part);
                        part.setExplanation("not real data, just for demo");
                        // We have existing partition files for the custom entities, so we need to make
                        // the partition point to the file location
                        part.setLocation("adls:/" + entDef.getEntityName() + "/partition-data.csv");
                        // Add trait to partition for csv params
                        CdmTraitReference csvTrait = part.getExhibitsTraits().add("is.partition.format.CSV", false);
                        csvTrait.getArguments().add("columnHeaders", "true");
                        csvTrait.getArguments().add("delimiter", ",");
                }
                // We can save the documents as manifest.cdm.json format or model.json
                // Save as manifest.cdm.json
                manifestResolved.saveAsAsync(manifestResolved.getManifestName() + ".manifest.cdm.json", true).get();
                // Save as a model.json
                manifestResolved.saveAsAsync("model.json", true).get();
                try {
                        DataLakeUtil.saveToDataLake(localPath, rootFolderName);
                } catch (Exception e) {
                        e.printStackTrace();
                }
                try {
                        DataLakeUtil.saveToDataLake(
                                        "../" + pathFromExeToExampleRoot + "example-public-standards/pre-data",
                                        rootFolderName);
                } catch (Exception e) {
                        e.printStackTrace();
                }

        }

        private static List<EntityInfo> getIncomeStatmentEntity() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();

                entityInfos.add(EntityInfo.builder()//
                                .attributeName("bg")//
                                .relationEntityName(FACTORYINFO_ENTITY_NAME)//
                                .relationDescription("BG relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder()//
                                .attributeName("site")//
                                .relationEntityName(FACTORYINFO_ENTITY_NAME)//
                                .relationDescription("Site relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder()//
                                .attributeName("product")//
                                .relationEntityName(FACTORYINFO_ENTITY_NAME)//
                                .relationDescription("Product relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("type").purpose("hasA").dataType("string").build());

                entityInfos.add(EntityInfo.builder()//
                                .attributeName("reportingMonth")//
                                .relationEntityName(REPORTING_MONTH_ENTITY_NAME)//
                                .relationDescription("reporting month relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("revenue").purpose("hasA").dataType("float")
                                .description("單月營收").build());
                entityInfos.add(EntityInfo.builder().attributeName("operationIncome").purpose("hasA").dataType("float")
                                .description("單月營業利潤").build());
                entityInfos.add(EntityInfo.builder().attributeName("mva").purpose("hasA").dataType("float")
                                .description("單月代工費用").build());
                entityInfos.add(EntityInfo.builder().attributeName("moh").purpose("hasA").dataType("float")
                                .description("單月生產成本").build());
                entityInfos.add(EntityInfo.builder().attributeName("materialCost").purpose("hasA").dataType("float")
                                .description("單月材料成本").build());
                entityInfos.add(EntityInfo.builder().attributeName("otherCost").purpose("hasA").dataType("float")
                                .description("單月其他成本").build());
                entityInfos.add(EntityInfo.builder().attributeName("opex").purpose("hasA").dataType("float")
                                .description("單月管銷費用").build());
                entityInfos.add(EntityInfo.builder().attributeName("invoiceQty").purpose("hasA").dataType("float")
                                .description("單月出貨數量").build());
                entityInfos.add(EntityInfo.builder().attributeName("productionQty").purpose("hasA").dataType("float")
                                .description("單月生產數量").build());

                return entityInfos;

        }

        private static List<EntityInfo> getFactoryEntity() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("bg").purpose("hasA").dataType("string")
                                .description("所屬事業群").build());
                entityInfos.add(EntityInfo.builder().attributeName("site").purpose("hasA").dataType("string")
                                .description("工廠Site名稱").build());
                entityInfos.add(EntityInfo.builder().attributeName("address").purpose("hasA").dataType("string")
                                .description("工廠地址").build());
                entityInfos.add(EntityInfo.builder().attributeName("county").purpose("hasA").dataType("string")
                                .description("工廠所屬國家").build());

                return entityInfos;

        }

        private static List<EntityInfo> getReportingMonthEntity() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("reportingMonth").purpose("hasA").dataType("string")
                                .description("報表日期").build());

                return entityInfos;

        }

        private static List<EntityInfo> getProductEntity() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("product").purpose("hasA").dataType("string")
                                .description("產品名稱").build());
                entityInfos.add(EntityInfo.builder().attributeName("custom").purpose("hasA").dataType("string")
                                .description("客戶名稱").build());

                return entityInfos;

        }

        private static List<EntityInfo> getPerformanceEntity() {

                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();

                entityInfos.add(EntityInfo.builder()//
                                .attributeName("bg")//
                                .relationEntityName(FACTORYINFO_ENTITY_NAME)//
                                .relationDescription("BG relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder()//
                                .attributeName("site")//
                                .relationEntityName(FACTORYINFO_ENTITY_NAME)//
                                .relationDescription("Site relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder()//
                                .attributeName("product")//
                                .relationEntityName(FACTORYINFO_ENTITY_NAME)//
                                .relationDescription("Product relation")//
                                .relation(true)//
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("type").purpose("hasA").dataType("string").build());

                entityInfos.add(EntityInfo.builder()//
                                .attributeName("reportingMonth")//
                                .relationEntityName(REPORTING_MONTH_ENTITY_NAME)//
                                .relationDescription("reporting month relation")//
                                .relation(true)//
                                .build());

                entityInfos.add(EntityInfo.builder().attributeName("ads").purpose("hasA").dataType("float")
                                .description("產品庫存量(天數)").build());
                entityInfos.add(EntityInfo.builder().attributeName("yieldRate").purpose("hasA").dataType("float")
                                .description("生產良率").build());
                entityInfos.add(EntityInfo.builder().attributeName("scrapRate").purpose("hasA").dataType("float")
                                .description("生產報廢率").build());

                return entityInfos;

        }

        /**
         * Create an type attribute definition instance.
         *
         * @param cdmCorpus     The CDM corpus.
         * @param attributeName The directives to use while getting the resolved
         *                      entities.
         * @param purpose       The manifest to be resolved.
         * @param dataType      The data type.
         * @return The instance of type attribute definition.
         */
        private static CdmTypeAttributeDefinition createEntityAttributeWithPurposeAndDataType(
                        CdmCorpusDefinition cdmCorpus, String attributeName, String purpose, String dataType) {
                CdmTypeAttributeDefinition entityAttribute = cdmCorpus.makeObject(CdmObjectType.TypeAttributeDef,
                                attributeName, false);
                entityAttribute.setPurpose(cdmCorpus.makeRef(CdmObjectType.PurposeRef, purpose, true));
                entityAttribute.setDataType(cdmCorpus.makeRef(CdmObjectType.DataTypeRef, dataType, true));
                return entityAttribute;
        }

        /**
         * Create a relationship linking with an attribute an eneity attribute
         * definition instance without a trait.
         *
         * @param cdmCorpus            The CDM corpus.
         * @param associatedEntityName The name of .
         * @param foreignKeyName       The name of the foreign key.
         * @param attributeExplanation The explanation of the attribute.
         * @return The instatnce of entity attribute definition.
         */
        private static CdmEntityAttributeDefinition createSimpleAttributeForRelationshipBetweenTwoEntities(
                        CdmCorpusDefinition cdmCorpus, String associatedEntityName, String foreignKeyName,
                        String attributeExplanation) {
                // Define a relationship by creating an entity attribute
                CdmEntityAttributeDefinition entityAttributeDef = cdmCorpus.makeObject(CdmObjectType.EntityAttributeDef,
                                foreignKeyName);
                entityAttributeDef.setExplanation(attributeExplanation);

                // Creating an entity reference for the associated entity - simple name
                // reference
                entityAttributeDef.setEntity(cdmCorpus.makeRef(CdmObjectType.EntityRef, associatedEntityName, true));

                // Add resolution guidance - enable reference
                CdmAttributeResolutionGuidance attributeResolution = cdmCorpus
                                .makeObject(CdmObjectType.AttributeResolutionGuidanceDef);
                attributeResolution.setEntityByReference(attributeResolution.makeEntityByReference());
                attributeResolution.getEntityByReference().setAllowReference(true);
                entityAttributeDef.setResolutionGuidance(attributeResolution);
                return entityAttributeDef;
        }

        /**
         * Create a relationship linking by creating an eneity attribute definition
         * instance with a trait. This allows you to add a resolution guidance to
         * customize your data.
         *
         * @param cdmCorpus            The CDM corpus.
         * @param associatedEntityName The name of the associated entity.
         * @param foreignKeyName       The name of the foreign key.
         * @param attributeExplanation The explanation of the attribute.
         * @return The instatnce of entity attribute definition.
         */
        private static CdmEntityAttributeDefinition createAttributeForRelationshipBetweenTwoEntities(
                        CdmCorpusDefinition cdmCorpus, String associatedEntityName, String foreignKeyName,
                        String attributeExplanation) {
                // Define a relationship by creating an entity attribute
                CdmEntityAttributeDefinition entityAttributeDef = cdmCorpus.makeObject(CdmObjectType.EntityAttributeDef,
                                foreignKeyName);
                entityAttributeDef.setExplanation(attributeExplanation);
                // Creating an entity reference for the associated entity
                CdmEntityReference associatedEntityRef = cdmCorpus.makeRef(CdmObjectType.EntityRef,
                                associatedEntityName, false);

                // Creating a "is.identifiedBy" trait for entity reference
                CdmTraitReference traitReference = cdmCorpus.makeObject(CdmObjectType.TraitRef, "is.identifiedBy",
                                false);
                String s = associatedEntityName + "/(resolvedAttributes)/" + associatedEntityName + "Id";
                traitReference.getArguments().add(null,
                                associatedEntityName + "/(resolvedAttributes)/" + foreignKeyName);

                // Add the trait to the attribute's entity reference
                associatedEntityRef.getAppliedTraits().add(traitReference);
                entityAttributeDef.setEntity(associatedEntityRef);

                // Add resolution guidance
                CdmAttributeResolutionGuidance attributeResolution = cdmCorpus
                                .makeObject(CdmObjectType.AttributeResolutionGuidanceDef);
                attributeResolution.setEntityByReference(attributeResolution.makeEntityByReference());
                attributeResolution.getEntityByReference().setAllowReference(true);
                attributeResolution.setRenameFormat("{m}");
                CdmTypeAttributeDefinition entityAttribute = createEntityAttributeWithPurposeAndDataType(cdmCorpus,
                                foreignKeyName + "Id", "identifiedBy", "entityId");
                attributeResolution.getEntityByReference().setForeignKeyAttribute(entityAttribute);
                entityAttributeDef.setResolutionGuidance(attributeResolution);
                return entityAttributeDef;
        }
}
