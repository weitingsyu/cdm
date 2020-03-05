// Copyright (c) Microsoft Corporation.

package com.microsoft.commondatamodel.objectmodel.cdm;

import com.google.common.base.Strings;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ParameterCollection;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ParameterValueSet;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolveContext;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedAttributeSetBuilder;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedTrait;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedTraitSet;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedTraitSetBuilder;
import com.microsoft.commondatamodel.objectmodel.utilities.CdmException;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.SymbolSet;
import com.microsoft.commondatamodel.objectmodel.utilities.VisitCallback;
import java.util.ArrayList;
import java.util.List;

public class CdmTraitDefinition extends CdmObjectDefinitionBase {

  Boolean thisIsKnownToHaveParameters;
  private Boolean baseIsKnownToHaveParameters;
  private String traitName;
  private CdmTraitReference extendsTrait;
  private Boolean elevated;
  private Boolean ugly;
  private List<String> associatedProperties;
  ParameterCollection allParameters;
  private boolean hasSetFlags;
  private CdmCollection<CdmParameterDefinition> parameters;

  public CdmTraitDefinition(final CdmCorpusContext ctx, final String name) {
    this(ctx, name, null);
  }

  public CdmTraitDefinition(final CdmCorpusContext ctx, final String name, final CdmTraitReference extendsTrait) {
    super(ctx);
    this.hasSetFlags = false;
    this.setObjectType(CdmObjectType.TraitDef);
    this.traitName = name;
    this.extendsTrait = extendsTrait;
  }

  @Override
  public String getName() {
    return getTraitName();
  }

  @Override
  public ResolvedTraitSet fetchResolvedTraits(final ResolveOptions resOpt) {
    final String kind = "rtsb";
    final ResolveContext ctx = (ResolveContext) this.getCtx();
    // this may happen 0, 1 or 2 times. so make it fast
    final BaseInfo baseInfo = new BaseInfo(null, null, null);

    // see if one is already cached
    // if this trait has parameters, then the base trait found through the reference might be a different reference
    // because trait references are unique per argument value set. so use the base as a part of the cache tag
    // since it is expensive to figure out the extra tag, cache that too!
    if (this.getBaseIsKnownToHaveParameters() == null) {
      this.updateBaseInfo(baseInfo, resOpt);
      // is a cache tag needed? then make one
      this.setBaseIsKnownToHaveParameters(false);
      if (baseInfo.getValues() != null && baseInfo.getValues().size() > 0) {
        this.setBaseIsKnownToHaveParameters(true);
      }
    }
    String cacheTagExtra = "";
    if (this.getBaseIsKnownToHaveParameters()) {
      cacheTagExtra = String.valueOf(this.getExtendsTrait().getId());
    }

    String cacheTag = ctx.getCorpus()
        .createDefinitionCacheTag(resOpt, this, kind, cacheTagExtra);
    Object rtsResultDynamic = null;
    if (cacheTag != null) {
      rtsResultDynamic = ctx.fetchCache().get(cacheTag);
    }
    ResolvedTraitSet rtsResult = (ResolvedTraitSet) rtsResultDynamic;

    // store the previous reference symbol set, we will need to add it with
    // children found from the constructResolvedTraits call
    SymbolSet currSymbolRefSet = resOpt.getSymbolRefSet();
    if (currSymbolRefSet == null) {
      currSymbolRefSet = new SymbolSet();
    }
    resOpt.setSymbolRefSet(new SymbolSet());

    // if not, then make one and save it
    if (rtsResult == null) {
      this.updateBaseInfo(baseInfo, resOpt);
      if (baseInfo.getTrait() != null) {
        // get the resolution of the base class and use the values as a starting point for this trait's values
        if (!this.hasSetFlags) {
          // inherit these flags
          if (this.getElevated() == null) {
            this.setElevated(baseInfo.getTrait().getElevated());
          }
          if (this.getUgly() == null) {
            this.setUgly(baseInfo.getTrait().getUgly());
          }
          if (this.getAssociatedProperties() == null) {
            this.setAssociatedProperties(baseInfo.getTrait().getAssociatedProperties());
          }
        }
      }
      this.hasSetFlags = true;
      final ParameterCollection pc = this.fetchAllParameters(resOpt);
      final List<Object> av = new ArrayList<>();
      final List<Boolean> wasSet = new ArrayList<>();
      this.thisIsKnownToHaveParameters = pc.getSequence().size() > 0;
      for (int i = 0; i < pc.getSequence().size(); i++) {
        // either use the default value or (higher precidence) the value taken from the base reference
        Object value = pc.getSequence().get(i).getDefaultValue();
        Object baseValue = null;
        if (baseInfo.getValues() != null && i < baseInfo.getValues().size()) {
          baseValue = baseInfo.getValues().get(i);
          if (baseValue != null) {
            value = baseValue;
          }
        }
        av.add(value);
        wasSet.add(false);
      }

      // save it
      final ResolvedTrait resTrait = new ResolvedTrait(this, pc, av, wasSet);
      rtsResult = new ResolvedTraitSet(resOpt);
      rtsResult.merge(resTrait, false);

      // register set of possible symbols
      ctx.getCorpus()
          .registerDefinitionReferenceSymbols(this.fetchObjectDefinition(resOpt), kind,
              resOpt.getSymbolRefSet());
      // get the new cache tag now that we have the list of docs
      cacheTag = ctx.getCorpus()
          .createDefinitionCacheTag(resOpt, this, kind, cacheTagExtra);
      if (!Strings.isNullOrEmpty(cacheTag)) {
        ctx.fetchCache().put(cacheTag, rtsResult);
      }
    } else {
      // cache found
      // get the SymbolSet for this cached object
      final String key = CdmCorpusDefinition.createCacheKeyFromObject(this, kind);
      final SymbolSet tempSymbolRefSet;
      tempSymbolRefSet = ctx.getCorpus().getDefinitionReferenceSymbols()
          .get(key);
      resOpt.setSymbolRefSet(tempSymbolRefSet);
    }
    // merge child document set with current
    currSymbolRefSet.merge(resOpt.getSymbolRefSet());
    resOpt.setSymbolRefSet(currSymbolRefSet);

    return rtsResult;
  }

  @Override
  public boolean isDerivedFrom(final String baseDef, final ResolveOptions resOpt) {
    if (baseDef.equalsIgnoreCase(this.traitName)) {
      return true;
    }

    return this.isDerivedFromDef(resOpt, this.getExtendsTrait(), this.getTraitName(), baseDef);
  }

  @Override
  public boolean visit(final String pathFrom, final VisitCallback preChildren, final VisitCallback postChildren) {
    String path = "";

    if (this.getCtx() != null
        && this.getCtx().getCorpus() != null
        && !this.getCtx().getCorpus().blockDeclaredPathChanges) {
      path = this.getDeclaredPath();

      if (Strings.isNullOrEmpty(path)) {
        path = pathFrom + this.traitName;
        this.setDeclaredPath(path);
      }
    }

    if (preChildren != null && preChildren.invoke(this, path)) {
      return false;
    }
    if (this.getExtendsTrait() != null && this.getExtendsTrait()
        .visit(path + "/extendsTrait/", preChildren, postChildren)) {
      return true;
    }
    if (this.getParameters() != null && this.getParameters()
        .visitList(path + "/hasParameters/", preChildren, postChildren)) {
      return true;
    }
    return postChildren != null && postChildren.invoke(this, path);
  }

  /**
   * Gets or sets the trait associated properties.
   */
  public List<String> getAssociatedProperties() {
    return this.associatedProperties;
  }

  private Boolean getBaseIsKnownToHaveParameters() {
    return baseIsKnownToHaveParameters;
  }

  public void setAssociatedProperties(final List<String> value) {
    this.associatedProperties = value;
  }

  private void setBaseIsKnownToHaveParameters(final Boolean baseIsKnownToHaveParameters) {
    this.baseIsKnownToHaveParameters = baseIsKnownToHaveParameters;
  }

  /**
   * Gets or sets the trait elevated.
   */
  public Boolean getElevated() {
    return this.elevated;
  }

  public void setElevated(final Boolean value) {
    this.elevated = value;
  }

  /**
   * Gets or sets the trait extended by this trait.
   */
  public CdmTraitReference getExtendsTrait() {
    return this.extendsTrait;
  }

  public void setExtendsTrait(final CdmTraitReference value) {
    this.extendsTrait = value;
  }

  /**
   * Gets the trait parameters.
   */
  public CdmCollection<CdmParameterDefinition> getParameters() {
    if (this.parameters == null) {
      this.parameters = new CdmCollection<>(this.getCtx(), this, CdmObjectType.ParameterDef);
    }
    return this.parameters;
  }

  /**
   * Gets or sets the trait name.
   */
  public String getTraitName() {
    return this.traitName;
  }

  public void setTraitName(final String value) {
    this.traitName = value;
  }

  /**
   * Gets or sets if trait is user facing or not.
   */
  public Boolean getUgly() {
    return this.ugly;
  }

  public void setUgly(final Boolean value) {
    this.ugly = value;
  }

  ParameterCollection fetchAllParameters(final ResolveOptions resOpt) {
    if (this.allParameters != null) {
      return this.allParameters;
    }

    // get parameters from base if there is one
    ParameterCollection prior = null;
    if (this.extendsTrait != null) {
      prior = ((CdmTraitDefinition) this.extendsTrait.fetchObjectDefinition(resOpt)).fetchAllParameters(resOpt);
    }
    this.allParameters = new ParameterCollection(prior);
    if (this.parameters != null) {
      for (final CdmParameterDefinition element : this.parameters) {
        try {
          this.allParameters.add(element);
        } catch (final CdmException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return this.allParameters;
  }

  @Override
  public boolean validate() {
    return !Strings.isNullOrEmpty(this.traitName);
  }

  /**
   *
   * @param resOpt
   * @param options
   * @return
   * @deprecated CopyData is deprecated. Please use the Persistence Layer instead. This function is
   * extremely likely to be removed in the public interface, and not meant to be called externally
   * at all. Please refrain from using it.
   */
  @Override
  @Deprecated
  public Object copyData(final ResolveOptions resOpt, final CopyOptions options) {
    return CdmObjectBase.copyData(this, resOpt, options, CdmTraitDefinition.class);
  }

  @Override
  public CdmObject copy(ResolveOptions resOpt, CdmObject host) {
    if (resOpt == null) {
      resOpt = new ResolveOptions(this);
    }

    CdmTraitDefinition copy;

    if (host == null) {
      copy = new CdmTraitDefinition(this.getCtx(), this.traitName, null);
    } else {
      copy = (CdmTraitDefinition) host;
      copy.setCtx(this.getCtx());
      copy.setTraitName(this.getTraitName());
    }

    copy.setExtendsTrait(
            (CdmTraitReference) (this.extendsTrait == null ? null : this.extendsTrait.copy(resOpt)));
    copy.setAllParameters(null);
    copy.setUgly(this.ugly);
    copy.setAssociatedProperties(this.associatedProperties);
    this.copyDef(resOpt, copy);
    return copy;
  }


  @Override
  ResolvedAttributeSetBuilder constructResolvedAttributes(final ResolveOptions resOpt,
                                                          final CdmAttributeContext under) {
    // return null intentionally
    return null;
  }

  @Override
  ResolvedAttributeSetBuilder constructResolvedAttributes(final ResolveOptions resOpt) {
    return constructResolvedAttributes(resOpt, null);
  }

  /**
   * Gets allParameters.
   *
   * @return Value of allParameters.
   */
  public ParameterCollection getAllParameters() {
    return allParameters;
  }

  /**
   * Sets new allParameters.
   *
   * @param allParameters New value of allParameters.
   */
  public void setAllParameters(final ParameterCollection allParameters) {
    this.allParameters = allParameters;
  }

  @Override
  void constructResolvedTraits(final ResolvedTraitSetBuilder rtsb, final ResolveOptions resOpt) {
//    INTENTIONALLY LEFT BLANK
  }

  // TODO-BQ: this class may be refactored.
  private static class BaseInfo {

    CdmTraitDefinition trait;
    ResolvedTraitSet rts;
    List<Object> values;

    public BaseInfo(final CdmTraitDefinition traits,
                    final ResolvedTraitSet rts, final List<Object> values) {
      this.trait = traits;
      this.rts = rts;
      this.values = values;
    }

    public CdmTraitDefinition getTrait() {
      return trait;
    }

    public ResolvedTraitSet getRts() {
      return rts;
    }

    public List<Object> getValues() {
      return values;
    }

    public void setTrait(final CdmTraitDefinition trait) {
      this.trait = trait;
    }

    public void setRts(final ResolvedTraitSet rts) {
      this.rts = rts;
    }

    public void setValues(final List<Object> values) {
      this.values = values;
    }
  }

  // TODO-BQ: this function may be refactored
  private void updateBaseInfo(final BaseInfo baseInfo, final ResolveOptions resOpt) {
    if (this.getExtendsTrait() != null) {
      baseInfo.setTrait(this.getExtendsTrait().fetchObjectDefinition(resOpt));
      if (baseInfo.getTrait() != null) {
        baseInfo.setRts(this.getExtendsTrait().fetchResolvedTraits(resOpt));
        if (baseInfo.getRts() != null && baseInfo.getRts().getSize() == 1) {
          final ParameterValueSet basePv =
              baseInfo.getRts().get(baseInfo.getTrait()) != null ? baseInfo.getRts()
                  .get(baseInfo.getTrait()).getParameterValues() : null;
          if (basePv != null) {
            baseInfo.setValues(basePv.getValues());
          }
        }
      }
    }
  }
}
