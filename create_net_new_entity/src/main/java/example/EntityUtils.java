package example;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmAttributeResolutionGuidance;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDocumentDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmFolderDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTypeAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;

import org.apache.commons.lang3.StringUtils;

public class EntityUtils {

        public static CdmEntityDefinition createEntity(CdmCorpusDefinition cdmCorpus, CdmFolderDefinition localRoot,
                        String entityName, List<EntityInfo> entityInfos, String fundationJsonPath, boolean depends) {

                System.out.println("Create " + entityName + " entities");
                CdmEntityDefinition entity = null;
                CdmDocumentDefinition entityDoc = cdmCorpus.makeObject(CdmObjectType.DocumentDef,
                                entityName + ".cdm.json", false);
                if (!depends) {
                        entity = cdmCorpus.makeObject(CdmObjectType.EntityDef, entityName, false);

                } else {
                        entity = cdmCorpus.makeObject(CdmObjectType.EntityDef, entityName);
                }

                for (

                EntityInfo entityInfo : entityInfos) {

                        if (!entityInfo.isRelation()) {

                                CdmTypeAttributeDefinition attribute = createEntityAttributeWithPurposeAndDataType(
                                                cdmCorpus, entityInfo.getAttributeName(), //
                                                entityInfo.getPurpose(), //
                                                entityInfo.getDataType()//

                                );
                                attribute.updateDescription(entityInfo.getDescription());
                                entity.getAttributes().add(attribute);
                        } else {
                                System.out.println(entityInfo.getAttributeName());
                                CdmEntityAttributeDefinition attribute = createAttributeForRelationshipBetweenTwoEntities(
                                                cdmCorpus, entityInfo.getRelationEntityName(),
                                                entityInfo.getAttributeName(), entityInfo.getRelationDescription());
                                entity.getAttributes().add(attribute);
                                entityDoc.getImports().add(entityInfo.getRelationEntityName() + ".cdm.json");
                        }
                }
                entity.setDisplayName(entityName);
                entity.setVersion("0.0.1");
                entity.setDescription("This is a " + entityName + " entity.");

                // Add an import to the foundations doc so the traits about partitons will
                // resolve nicely
                entityDoc.getImports().add(fundationJsonPath);
                entityDoc.getDefinitions().add(entity);
                // Add the document to the root of the local documents in the corpus
                localRoot.getDocuments().add(entityDoc);

                return entity;

        }

        public static CdmEntityDefinition create(CdmCorpusDefinition cdmCorpus, CdmFolderDefinition localRoot,
                        String entityName, List<EntityInfo> entityInfos, String extendEntity, String schemaDocsRoot,
                        String fundationJsonPath, List<RelationInfo> relations)
                        throws ExecutionException, InterruptedException {

                // Create two entities from scratch, and add some attributes, traits,
                // properties, and relationships in between
                System.out.println("Create " + entityName + " entities");

                // Create the simplest entity - CustomPerson
                // Create the entity definition instance
                CdmEntityDefinition entity = cdmCorpus.makeObject(CdmObjectType.EntityDef, entityName);
                CdmDocumentDefinition basicInfoEntityDoc = cdmCorpus.makeObject(CdmObjectType.DocumentDef,
                                entityName + ".cdm.json", false);
                if (StringUtils.isNotEmpty(extendEntity)) {
                        entity.setExtendsEntity(cdmCorpus.makeObject(CdmObjectType.EntityRef, extendEntity, true));
                }

                if (relations != null) {
                        for (RelationInfo relationInfo : relations) {
                                entity.getAttributes().add(createAttributeForRelationshipBetweenTwoEntities(cdmCorpus, //
                                                relationInfo.getEntityName(), //
                                                relationInfo.getColumnName(), //
                                                relationInfo.getDescription()));

                                basicInfoEntityDoc.getImports().add(relationInfo.getJsonPath() + ".cdm.json");
                        }
                }

                // Add type attributes to the entity instance
                for (

                EntityInfo entityInfo : entityInfos) {
                        CdmTypeAttributeDefinition attribute = createEntityAttributeWithPurposeAndDataType(cdmCorpus,
                                        entityInfo.getAttributeName(), //
                                        entityInfo.getPurpose(), //
                                        entityInfo.getDataType()//

                        );
                        attribute.updateDescription(entityInfo.getDescription());
                        entity.getAttributes().add(attribute);
                }

                // Add properties to the entity instance
                entity.setDisplayName(entityName);
                entity.setVersion("0.0.1");
                entity.setDescription("This is a " + entityName + " entity.");

                basicInfoEntityDoc.getImports().add(fundationJsonPath);
                if (StringUtils.isNotBlank(extendEntity)) {

                        basicInfoEntityDoc.getImports().add(schemaDocsRoot + "/" + extendEntity + ".cdm.json");

                        // Add the document to the root of the local documents in the corpus

                }

                localRoot.getDocuments().add(basicInfoEntityDoc);
                basicInfoEntityDoc.getDefinitions().add(entity);
                System.out.println(entityName + " -- Resolve the placeholder");
                return entity;

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
        public static CdmTypeAttributeDefinition createEntityAttributeWithPurposeAndDataType(
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
        public static CdmEntityAttributeDefinition createSimpleAttributeForRelationshipBetweenTwoEntities(
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
        public static CdmEntityAttributeDefinition createAttributeForRelationshipBetweenTwoEntities(
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
                String s = associatedEntityName + "/(resolvedAttributes)/" + associatedEntityName;
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
                                foreignKeyName, "hasA", "string");
                attributeResolution.getEntityByReference().setForeignKeyAttribute(entityAttribute);
                entityAttributeDef.setResolutionGuidance(attributeResolution);
                return entityAttributeDef;
        }
}