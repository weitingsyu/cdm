package com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusContext;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityReference;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import java.util.List;

public class EntityReferencePersistence {

  public static CdmEntityReference fromData(final CdmCorpusContext ctx, final JsonNode obj) {
    if (obj == null) {
      return null;
    }

    final Object entity;
    boolean simpleReference = true;
    List<CdmTraitReference> appliedTraits = null;

    if (obj.isValueNode()) {
        entity = obj.asText();
    } else {
      entity = getEntityReference(ctx, obj);
      simpleReference = false;
    }

    final CdmEntityReference entityReference = ctx.getCorpus().makeRef(CdmObjectType.EntityRef, entity, simpleReference);

    if (!(obj.isValueNode())) {
      appliedTraits = Utils.createTraitReferenceList(ctx, obj.get("appliedTraits"));
    }

    Utils.addListToCdmCollection(entityReference.getAppliedTraits(), appliedTraits);
    return entityReference;
  }

  public static Object toData(final CdmEntityReference instance, final ResolveOptions resOpt, final CopyOptions options) {
    return CdmObjectRefPersistence.toData(instance, resOpt, options);
  }

  private static Object getEntityReference(final CdmCorpusContext ctx, final JsonNode obj) {
    Object entity = null;
    if (obj.get("entityReference").isValueNode()) {
      entity = obj.get("entityReference");
    } else if (obj.get("entityReference") != null && obj.get("entityReference").get("entityShape") != null) {
        entity = ConstantEntityPersistence.fromData(ctx, obj.get("entityReference"));
    } else {
      entity = EntityPersistence.fromData(ctx, obj.get("entityReference"));
    }

    return entity;
  }
}