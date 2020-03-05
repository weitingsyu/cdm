package example;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmAttributeResolutionGuidance;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTypeAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;

public class EntityAttributeUtils {
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
    public static CdmTypeAttributeDefinition createEntityAttributeWithPurposeAndDataType(CdmCorpusDefinition cdmCorpus,
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
        CdmEntityReference associatedEntityRef = cdmCorpus.makeRef(CdmObjectType.EntityRef, associatedEntityName,
                false);

        // Creating a "is.identifiedBy" trait for entity reference
        CdmTraitReference traitReference = cdmCorpus.makeObject(CdmObjectType.TraitRef, "is.identifiedBy", false);
        String s = associatedEntityName + "/(resolvedAttributes)/" + associatedEntityName + "Id";
        traitReference.getArguments().add(null,
                associatedEntityName + "/(resolvedAttributes)/" + associatedEntityName + "Id");

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