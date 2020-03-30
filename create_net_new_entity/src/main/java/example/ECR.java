// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See License.txt in the project root for license information.

package example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.gson.JsonObject;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

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
public class ECR {

    private static JSONObject schema;
    // The names of the custom entities for this sample.
    // private final static String FACTORYINFO_ENTITY_NAME = "Factory";
    // private final static String PRODUCT_ENTITY_NAME = "Product";
    // private final static String PERFORMANCE_ENTITY_NAME = "Performance";
    // private final static String INCOME_STATMENT_ENTITY_NAME = "IncomeStatment";
    // private final static String REPORTING_MONTH_ENTITY_NAME = "ReportingMonth";
    private final static String ECR_COVERPAGE_NAME = "Coverpage";
    private final static String ECR_AFFECTED_ITEM_NAME = "Affecteditem";
    private final static String ECR_APPROVAL_LOG_NAME = "Approvallog";

    private final static String FOUNDATION_JSON_PATH = "cdm:/foundations.cdm.json";
    private final static String pathFromExeToExampleRoot = "./";
    private final static String localPath = "../" + pathFromExeToExampleRoot
            + "example-public-standards/core/wistron/ecr";
    // private final static String rootFolderName = "ecr";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        try {
            String ecr = readFile("src/main/resources/ecr.json");
            // System.out.println(ecr);
            schema = new JSONObject(ecr);
        } catch (JSONException err) {
            System.out.println(err);
        }

        // Make a corpus, the corpus is the collection of all documents and folders
        // created or discovered while navigating objects and paths
        CdmCorpusDefinition cdmCorpus = new CdmCorpusDefinition();

        System.out.println("configure storage adapters");

        cdmCorpus.getStorage().mount("local", new LocalAdapter(localPath));

        cdmCorpus.getStorage().setDefaultNamespace("local");
        final AdlsAdapter adlsAdapter = new AdlsAdapter("wsdcdmstorage.dfs.core.windows.net", // Hostname.
                "/powerbi/" + schema.getString("namespace"), // Root.
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
        CdmManifestDefinition manifestAbstract = cdmCorpus.makeObject(CdmObjectType.ManifestDef, "tempAbstract");

        // Add the temp manifest to the root of the local documents in the corpus
        CdmFolderDefinition localRoot = cdmCorpus.getStorage().fetchRootFolder("local");
        localRoot.getDocuments().add(manifestAbstract);

        // Create two entities from scratch, and add some attributes, traits,
        // properties, and relationships in between

        JSONObject jCoverpage = null;
        JSONObject jApprovallog = null;
        JSONObject jAffecteditem = null;
        JSONArray fields = schema.getJSONArray("fields");
        System.out.println(fields.length());
        for (int i = 0; i < fields.length(); i++) {
            JSONObject j = fields.getJSONObject(i);
            if (j.getString("name").equalsIgnoreCase(ECR_COVERPAGE_NAME)) {
                jCoverpage = j.getJSONArray("type").getJSONObject(1);
                System.out.println(jCoverpage.toString());
            }
            if (j.getString("name").equalsIgnoreCase("AffecteditemList")) {
                jAffecteditem = j.getJSONArray("type").getJSONObject(1).getJSONObject("items");
                System.out.println(jAffecteditem.toString());
            }
            if (j.getString("name").equalsIgnoreCase("ApprovallogList")) {
                jApprovallog = j.getJSONArray("type").getJSONObject(1).getJSONObject("items");
                System.out.println(jApprovallog.toString());
            }

        }

        // Create coverpage entity
        CdmEntityDefinition coverpage = EntityUtils.createEntity(cdmCorpus, localRoot, jCoverpage.getString("name"),
                buildEntity(jCoverpage), FOUNDATION_JSON_PATH, false);
        manifestAbstract.getEntities().add(coverpage);

        // Create coverpage entity
        CdmEntityDefinition affectedItem = EntityUtils.createEntity(cdmCorpus, localRoot,
                jAffecteditem.getString("name"), buildEntity(jAffecteditem), FOUNDATION_JSON_PATH, false);
        manifestAbstract.getEntities().add(affectedItem);

        // Create coverpage entity
        CdmEntityDefinition approvallog = EntityUtils.createEntity(cdmCorpus, localRoot, jApprovallog.getString("name"),
                buildEntity(jApprovallog), FOUNDATION_JSON_PATH, false);
        manifestAbstract.getEntities().add(approvallog);

        CdmManifestDefinition manifestResolved = manifestAbstract
                .createResolvedManifestAsync(schema.getString("name"), null).get();

        // Add an import to the foundations doc so the traits about partitions will
        // resolve nicely.
        manifestResolved.getImports().add(FOUNDATION_JSON_PATH);

        System.out.println("Save the documents");
        for (CdmEntityDeclarationDefinition eDef : manifestResolved.getEntities()) {
            // Get the entity being pointed at
            CdmEntityDeclarationDefinition localEDef = eDef;
            CdmEntityDefinition entDef = cdmCorpus
                    .<CdmEntityDefinition>fetchObjectAsync(localEDef.getEntityPath(), manifestResolved).get();
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
            csvTrait.getArguments().add("columnHeaders", "false");
            csvTrait.getArguments().add("delimiter", ",");
        }
        // We can save the documents as manifest.cdm.json format or model.json
        // Save as manifest.cdm.json
        manifestResolved.saveAsAsync(manifestResolved.getManifestName() + ".manifest.cdm.json", true).get();
        // Save as a model.json
        manifestResolved.saveAsAsync("model.json", true).get();
        // try {
        // DataLakeUtil.saveToDataLake(localPath, rootFolderName);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // try {
        // DataLakeUtil.saveToDataLake(
        // "../" + pathFromExeToExampleRoot + "example-public-standards/pre-data",
        // rootFolderName);
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

    }

    private static EntityInfo buildAttribute(String name, String dataType, String description) {
        return EntityInfo.builder().attributeName(name).purpose("hasA").dataType(dataType).description(description)
                .build();
    }

    private static List<EntityInfo> buildEntity(JSONObject object) {
        // sort csv header

        List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
        JSONArray fields = object.getJSONArray("fields");
        JSONArray sortFileds = sortJsonArray(fields, "name");

        entityInfos.add(buildAttribute("CDM_UUID", "string", "CDM UUID for avro record"));
        for (int i = 0; i < sortFileds.length(); i++) {
            JSONObject f = sortFileds.getJSONObject(i);
            String name = f.getString("name");
            // System.out.println(f.getJSONArray("type").getJSONObject(1).getString("type"));
            String type = "";
            if (f.getJSONArray("type").get(1) instanceof JSONObject) {
                type = AvroToCDMType(f.getJSONArray("type").getJSONObject(1).getString("type"));
            } else if (f.getJSONArray("type").get(1) instanceof String) {
                type = AvroToCDMType(f.getJSONArray("type").getString(1));
            }
            String doc = f.getString("doc");
            ;
            // System.out.println(name + " " + type + " " + doc);
            entityInfos.add(buildAttribute(name, type, doc));
        }
        return entityInfos;
    }

    private static JSONArray sortJsonArray(JSONArray jsonArr, String fieldName) {
        JSONArray sortedJsonArray = new JSONArray();
        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        for (int i = 0; i < jsonArr.length(); i++) {
            jsonValues.add(jsonArr.getJSONObject(i));
        }
        Collections.sort(jsonValues, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = new String();
                String valB = new String();

                try {
                    valA = (String) a.get(fieldName);
                    valB = (String) b.get(fieldName);
                } catch (JSONException e) {
                    // do something
                }

                return valA.compareTo(valB);
                // if you want to change the sort order, simply use the following:
                // return -valA.compareTo(valB);
            }
        });

        for (int i = 0; i < jsonArr.length(); i++) {
            sortedJsonArray.put(jsonValues.get(i));
        }
        return sortedJsonArray;
    }

    private static String AvroToCDMType(String avroType) {
        String cdmType = "";
        if (avroType.equalsIgnoreCase("long")) {
            cdmType = "double";
        } else if (avroType.equalsIgnoreCase("UUID")) {
            cdmType = "string";
        } else {
            cdmType = avroType;
        }
        // System.out.println(avroType + " " + cdmType);
        return cdmType;
    }

    private static String readFile(String path) {
        BufferedReader reader = null;
        String schema = "";
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8")); // 指定讀取文件的編碼格式，以免出現中文亂碼
            String str = null;
            while ((str = reader.readLine()) != null) {
                // System.out.println(str);
                schema += str;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return schema;
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
    private static CdmTypeAttributeDefinition createEntityAttributeWithPurposeAndDataType(CdmCorpusDefinition cdmCorpus,
            String attributeName, String purpose, String dataType) {
        CdmTypeAttributeDefinition entityAttribute = cdmCorpus.makeObject(CdmObjectType.TypeAttributeDef, attributeName,
                false);
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
        CdmEntityReference associatedEntityRef = cdmCorpus.makeRef(CdmObjectType.EntityRef, associatedEntityName,
                false);

        // Creating a "is.identifiedBy" trait for entity reference
        CdmTraitReference traitReference = cdmCorpus.makeObject(CdmObjectType.TraitRef, "is.identifiedBy", false);
        String s = associatedEntityName + "/(resolvedAttributes)/" + associatedEntityName + "Id";
        traitReference.getArguments().add(null, associatedEntityName + "/(resolvedAttributes)/" + foreignKeyName);

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
