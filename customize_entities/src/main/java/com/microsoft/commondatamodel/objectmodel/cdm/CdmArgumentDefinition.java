package com.microsoft.commondatamodel.objectmodel.cdm;

import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.persistence.PersistenceLayer;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.StringUtils;
import com.microsoft.commondatamodel.objectmodel.utilities.VisitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdmArgumentDefinition extends CdmObjectSimple {
  private static final Logger LOGGER = LoggerFactory.getLogger(CdmArgumentDefinition.class);

  private CdmParameterDefinition resolvedParameter;
  private String explanation;
  private String name;
  private Object value;
  private Object unResolvedValue;
  private String declaredPath;

  public CdmArgumentDefinition(final CdmCorpusContext ctx, final String name) {
    super(ctx);
    this.setObjectType(CdmObjectType.ArgumentDef);
    this.name = name;
  }

  public String getExplanation() {
    return this.explanation;
  }

  public void setExplanation(final String value) {
    this.explanation = value;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String value) {
    this.name = value;
  }

  public Object getValue() {
    return this.value;
  }

  public void setValue(final Object value) {
    this.value = value;
  }

  void setUnResolvedValue(final Object unResolvedValue) {
    this.unResolvedValue = unResolvedValue;
  }

  Object getUnResolvedValue() {
    return unResolvedValue;
  }

  void setResolvedParameter(final CdmParameterDefinition resolvedParameter) {
    this.resolvedParameter = resolvedParameter;
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public CdmParameterDefinition getParameterDef() {
    return this.resolvedParameter;
  }

  /**
   *
   * @param resOpt
   * @param options
   * @return
   * @deprecated CopyData is deprecated. Please use the Persistence Layer instead. This function is
   * extremely likely to be removed in the public interface, Please refrain from using it.
   */
  @Deprecated
  @Override
  public Object copyData(final ResolveOptions resOpt, final CopyOptions options) {
    return PersistenceLayer.toData(this, resOpt, options, "CdmFolder", CdmArgumentDefinition.class);
  }

  @Override
  public boolean visit(final String pathFrom, final VisitCallback preChildren, final VisitCallback postChildren) {
    String path = "";

    if (this.getCtx() != null
        && this.getCtx().getCorpus() != null
        && !this.getCtx().getCorpus().blockDeclaredPathChanges) {
      path = this.declaredPath;

      if (StringUtils.isNullOrTrimEmpty(path)) {
        path = pathFrom + (this.getValue() != null ? "value/" : "");
        this.declaredPath = path;
      }
    }

    if (preChildren != null && preChildren.invoke(this, path)) {
      return false;
    }
    if (this.getValue() instanceof CdmObject) {
      final CdmObject value = (CdmObject) this.getValue();
      if (value.visit(path, preChildren, postChildren)) {
        return true;
      }
    }
    if (postChildren != null && postChildren.invoke(this, path)) {
      return true;
    }

    return postChildren != null && postChildren.invoke(this, path);
  }

  @Override
  public CdmObject copy(ResolveOptions resOpt, CdmObject host) {
    if (resOpt == null) {
      resOpt = new ResolveOptions(this);
    }

    CdmArgumentDefinition copy;
    if (host == null) {
      copy = new CdmArgumentDefinition(this.getCtx(), this.name);
    } else {
      copy = (CdmArgumentDefinition) host;
      copy.setCtx(this.getCtx());
      copy.setName(this.getName());
    }

    if (this.getValue() != null) {
      if (this.getValue() instanceof CdmObject) {
        copy.setValue(((CdmObject) this.getValue()).copy(resOpt));
      } else if (this.getValue() instanceof String){
        copy.setValue(this.getValue());
      } else {
        LOGGER.error("Failed to copy CdmArgumentDefinition.getValue(), not recognized type");
        throw new RuntimeException("Failed to copy CdmArgumentDefinition.getValue(), not recognized type");
      }
    }
    copy.setResolvedParameter(this.resolvedParameter);
    copy.setExplanation(this.getExplanation());
    return copy;
  }

  @Override
  public boolean validate() {
    return this.getValue() != null;
  }
}
