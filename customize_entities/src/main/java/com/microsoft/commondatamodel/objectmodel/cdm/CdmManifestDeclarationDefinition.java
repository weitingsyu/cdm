package com.microsoft.commondatamodel.objectmodel.cdm;

import com.google.common.base.Strings;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedAttributeSetBuilder;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedTraitSetBuilder;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.TimeUtils;
import com.microsoft.commondatamodel.objectmodel.utilities.VisitCallback;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

public class CdmManifestDeclarationDefinition extends CdmObjectDefinitionBase implements CdmFileStatus {

  private String manifestName;
  private OffsetDateTime lastFileStatusCheckTime;
  private OffsetDateTime lastFileModifiedTime;
  private OffsetDateTime lastChildModifiedTime;
  private String definition;

  public CdmManifestDeclarationDefinition(final CdmCorpusContext ctx, final String name) {
    super(ctx);
    this.setObjectType(CdmObjectType.ManifestDeclarationDef);
    this.manifestName = name;
  }

  public String getDefinition() {
    return this.definition;
  }

  public void setDefinition(final String value) {
    this.definition = value;
  }

  public String getManifestName() {
    return this.manifestName;
  }

  public void setManifestName(final String value) {
    this.manifestName = value;
  }

  @Override
  public OffsetDateTime getLastFileStatusCheckTime() {
    return this.lastFileStatusCheckTime;
  }

  @Override
  public void setLastFileStatusCheckTime(final OffsetDateTime value) {
    this.lastFileStatusCheckTime = value;
  }

  @Override
  public OffsetDateTime getLastFileModifiedTime() {
    return this.lastFileModifiedTime;
  }

  @Override
  public void setLastFileModifiedTime(final OffsetDateTime value) {
    this.lastFileModifiedTime = value;
  }

  @Override
  public OffsetDateTime getLastChildFileModifiedTime() {
    return lastChildModifiedTime;
  }

  @Override
  public void setLastChildFileModifiedTime(final OffsetDateTime time) {
    this.lastChildModifiedTime = time;
  }

  @Override
  public CompletableFuture<Void> fileStatusCheckAsync() {

    final String fullPath =
        this.getCtx()
            .getCorpus()
            .getStorage()
            .createAbsoluteCorpusPath(this.getDefinition(), this.getInDocument());
    return getCtx().getCorpus().computeLastModifiedTimeAsync(fullPath, this)
      .thenCompose((modifiedTime) -> {
        // update modified times
        setLastFileStatusCheckTime(OffsetDateTime.now(ZoneOffset.UTC));
        setLastFileModifiedTime(TimeUtils.maxTime(modifiedTime, getLastFileModifiedTime()));

        return reportMostRecentTimeAsync(getLastFileModifiedTime());
      });
  }

  @Override
  public CompletableFuture<Void> reportMostRecentTimeAsync(final OffsetDateTime childTime) {
    if (this.getOwner() instanceof CdmFileStatus && childTime != null) {
      return ((CdmFileStatus) this.getOwner()).reportMostRecentTimeAsync(childTime);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public String getName() {
    return this.manifestName;
  }

  @Override
  public boolean isDerivedFrom(final String baseDef, final ResolveOptions resOpt) {
    return false;
  }

  @Override
  public boolean visit(
      final String pathFrom,
      final VisitCallback preChildren,
      final VisitCallback postChildren) {
    String path = "";
    if (!this.getCtx().getCorpus().blockDeclaredPathChanges) {
      path = this.getDeclaredPath();
      if (path == null) {
        path = pathFrom + this.getName();
        this.setDeclaredPath(path);
      }
    }

    if (preChildren != null && preChildren.invoke(this, path)) {
      return false;
    }

    if (this.visitDef(path, preChildren, postChildren)) {
      return true;
    }

    if (postChildren != null && postChildren.invoke(this, path)) {
      return false;
    }

    return false;
  }

  @Override
  public boolean validate() {
    return !Strings.isNullOrEmpty(this.getManifestName()) && !Strings.isNullOrEmpty(this.getDefinition());
  }

  @Override
  public Object copyData(final ResolveOptions resOpt, final CopyOptions options) {
    return CdmObjectBase.copyData(this, resOpt, options, CdmManifestDeclarationDefinition.class);
  }

  /**
   *
   * @param resOpt
   * @return
   * @deprecated CopyData is deprecated. Please use the Persistence Layer instead. This function is
   * extremely likely to be removed in the public interface, and not meant to be called externally
   * at all. Please refrain from using it.
   */
  @Override
  @Deprecated
  public CdmObject copy(ResolveOptions resOpt, CdmObject host) {
    if (resOpt == null) {
      resOpt = new ResolveOptions(this);
    }

    CdmManifestDeclarationDefinition copy;
    if (host == null) {
      copy = new CdmManifestDeclarationDefinition(this.getCtx(), this.getManifestName());
    } else {
      copy = (CdmManifestDeclarationDefinition) host;
      copy.setCtx(this.getCtx());
      copy.setManifestName(this.getManifestName());
    }

    copy.setDefinition(this.getDefinition());
    copy.setLastFileStatusCheckTime(this.getLastFileStatusCheckTime());
    copy.setLastFileModifiedTime(this.getLastFileModifiedTime());

    this.copyDef(resOpt, copy);

    return copy;
  }


  @Override
  ResolvedAttributeSetBuilder constructResolvedAttributes(final ResolveOptions resOpt) {
    return constructResolvedAttributes(resOpt, null);
  }

  @Override
  ResolvedAttributeSetBuilder constructResolvedAttributes(final ResolveOptions resOpt,
                                                          final CdmAttributeContext under) {
    // return null intentionally
    return null;
  }

  @Override
  void constructResolvedTraits(final ResolvedTraitSetBuilder rtsb, final ResolveOptions resOpt) {
//    INTENTIONALLY LEFT BLANK
  }
}
