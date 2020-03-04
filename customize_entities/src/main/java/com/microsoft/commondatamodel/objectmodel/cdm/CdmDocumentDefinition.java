package com.microsoft.commondatamodel.objectmodel.cdm;

import com.google.common.base.Strings;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedAttributeSetBuilder;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedTraitSetBuilder;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.StringUtils;
import com.microsoft.commondatamodel.objectmodel.utilities.VisitCallback;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdmDocumentDefinition extends CdmObjectSimple implements CdmContainerDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger(CdmDocumentDefinition.class);

  protected Map<String, CdmObjectDefinitionBase> internalDeclarations;
  protected boolean isDirty = true;
  private ImportPriorities importPriorities;
  private boolean needsIndexing;
  private CdmDefinitionCollection definitions;
  private CdmImportCollection imports;
  private CdmFolderDefinition folder;
  private String folderPath;
  private String namespace;
  private boolean importsIndexed;
  private boolean currentlyIndexing;
  private String name;
  private String schema;
  private String jsonSchemaSemanticVersion;
  private OffsetDateTime _fileSystemModifiedTime;

  public CdmDocumentDefinition() {
  }

  public CdmDocumentDefinition(final CdmCorpusContext ctx, final String name) {
    super(ctx);
    this.setInDocument(this);
    this.setObjectType(CdmObjectType.DocumentDef);
    this.name = name;
    this.jsonSchemaSemanticVersion = "0.9.0";
    this.needsIndexing = true;
    this.isDirty = true;
    this.importsIndexed = false;
    this.currentlyIndexing = false;

    this.clearCaches();

    this.imports = new CdmImportCollection(this.getCtx(), this);
    this.definitions = new CdmDefinitionCollection(this.getCtx(), this);
  }

  public boolean getNeedsIndexing() {
    return this.needsIndexing;
  }

  public void setNeedsIndexing(final boolean value) {
    this.needsIndexing = value;
  }

  /**
   * @return
   * @deprecated This function is extremely likely to be removed in
   * the public interface, and no meant to be called externally at all. Please refrain from using it.
   */
  @Override
  @Deprecated
  public String getFolderPath() {
    return this.folderPath;
  }

  /**
   * @param folderPath
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Override
  @Deprecated
  public void setFolderPath(final String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Override
  @Deprecated
  public String getNamespace() {
    return this.namespace;
  }

  /**
   * @param namespace
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Override
  @Deprecated
  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  boolean isImportsIndexed() {
    return importsIndexed;
  }

  void setImportsIndexed(final boolean importsIndexed) {
    this.importsIndexed = importsIndexed;
  }

  boolean isCurrentlyIndexing() {
    return currentlyIndexing;
  }

  void setCurrentlyIndexing(final boolean currentlyIndexing) {
    this.currentlyIndexing = currentlyIndexing;
  }

  public CdmDefinitionCollection getDefinitions() {
    return this.definitions;
  }

  /**
   * @return
   * @deprecated User the Owner Property instead. This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public CdmFolderDefinition getFolder() {
    return this.folder;
  }

  /**
   * @param folder
   * @deprecated User the Owner Property instead. This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public void setFolder(final CdmFolderDefinition folder) {
    this.folder = folder;
  }

  public CdmImportCollection getImports() {
    return this.imports;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String value) {
    this.name = value;
  }

  public String getJsonSchemaSemanticVersion() {
    return this.jsonSchemaSemanticVersion;
  }

  public void setJsonSchemaSemanticVersion(final String jsonSchemaSemanticVersion) {
    this.jsonSchemaSemanticVersion = jsonSchemaSemanticVersion;
  }

  void clearCaches() {
    this.internalDeclarations = new LinkedHashMap<>();

    // Remove all of the cached paths and resolved pointers.
    this.visit("", null, (iObject, path) -> {
      ((CdmObjectBase) iObject).setDeclaredPath(null);
      return false;
    });
  }

  /**
   * Finds any relative corpus paths that are held within this document and makes them relative to
   * the new folder instead.
   */
  boolean localizeCorpusPaths(CdmFolderDefinition newFolder) {
    final AtomicBoolean allWentWell = new AtomicBoolean(true);
    boolean wasBlocking = this.getCtx().getCorpus().blockDeclaredPathChanges;
    this.getCtx().getCorpus().blockDeclaredPathChanges = true;

    // shout into the void
    LOGGER.info("Localizing corpus paths in document '{}'.", this.getName());

    // find anything in the document that is a corpus path
    this.visit("", (iObject, path) -> {
      // i don't like that document needs to know a little about these objects
      // in theory, we could create a virtual function on cdmObject that localizes properties
      // but then every object would need to know about the documents and paths and such ...
      // also, i already wrote this code.
      switch (iObject.getObjectType()) {
        case Import: {
          final CdmImport typeObj = (CdmImport) iObject;
          final String localizeCorpusPath =
              localizeCorpusPath(typeObj.getCorpusPath(), newFolder, allWentWell);
          typeObj.setCorpusPath(
              localizeCorpusPath == null
                  ? typeObj.getCorpusPath()
                  : localizeCorpusPath);
          break;
        }
        case LocalEntityDeclarationDef:
        case ReferencedEntityDeclarationDef: {
          final CdmEntityDeclarationDefinition typeObj = (CdmEntityDeclarationDefinition) iObject;
          final String localizeCorpusPath =
              localizeCorpusPath(typeObj.getEntityPath(), newFolder, allWentWell);
          typeObj.setEntityPath(
              localizeCorpusPath == null
                  ? typeObj.getEntityPath()
                  : localizeCorpusPath);
          break;
        }
        case DataPartitionDef: {
          final CdmDataPartitionDefinition typeObj = (CdmDataPartitionDefinition) iObject;
          String localizeCorpusPath =
              localizeCorpusPath(typeObj.getLocation(), newFolder, allWentWell);
          typeObj.setLocation(
              localizeCorpusPath == null
                  ? typeObj.getLocation()
                  : localizeCorpusPath);

          localizeCorpusPath =
              localizeCorpusPath(typeObj.getSpecializedSchema(), newFolder, allWentWell);
          typeObj.setSpecializedSchema(
              localizeCorpusPath == null
                  ? typeObj.getSpecializedSchema()
                  : localizeCorpusPath);
          break;
        }
        case DataPartitionPatternDef: {
          final CdmDataPartitionPatternDefinition typeObj = (CdmDataPartitionPatternDefinition) iObject;
          String localizeCorpusPath =
              localizeCorpusPath(typeObj.getRootLocation(), newFolder, allWentWell);
          typeObj.setRootLocation(
              localizeCorpusPath == null
                  ? typeObj.getRootLocation()
                  : localizeCorpusPath);

          localizeCorpusPath =
              localizeCorpusPath(typeObj.getSpecializedSchema(), newFolder, allWentWell);
          typeObj.setSpecializedSchema(
              localizeCorpusPath == null
                  ? typeObj.getSpecializedSchema()
                  : localizeCorpusPath);
          break;
        }
        case E2ERelationshipDef: {
          final CdmE2ERelationship typeObj = (CdmE2ERelationship) iObject;
          String localizeCorpusPath =
              localizeCorpusPath(typeObj.getToEntity(), newFolder, allWentWell);
          typeObj.setToEntity(
              localizeCorpusPath == null
                  ? typeObj.getToEntity()
                  : localizeCorpusPath);

          localizeCorpusPath =
              localizeCorpusPath(typeObj.getFromEntity(), newFolder, allWentWell);
          typeObj.setFromEntity(
              localizeCorpusPath == null
                  ? typeObj.getFromEntity()
                  : localizeCorpusPath);
          break;
        }
        case ManifestDeclarationDef: {
          final CdmManifestDeclarationDefinition typeObj =
              (CdmManifestDeclarationDefinition) iObject;
          String localizeCorpusPath =
              localizeCorpusPath(typeObj.getDefinition(), newFolder, allWentWell);
          typeObj.setDefinition(
              localizeCorpusPath == null
                  ? typeObj.getDefinition()
                  : localizeCorpusPath);
          break;
        }
      }
      return false;
    }, null);

    this.getCtx().getCorpus().blockDeclaredPathChanges = wasBlocking;

    return allWentWell.get();
  }

  /**
   * Changes a relative corpus path to be relative to the new folder.
   */
  private String localizeCorpusPath(
      final String corpusPath,
      final CdmFolderDefinition newFolder,
      final AtomicBoolean allWentWell) {
    // If this isn't a local path, then don't do anything to it.
    if (StringUtils.isNullOrTrimEmpty(corpusPath)) {
      return corpusPath;
    }

    // First, if there was no previous folder (odd) then just localize as best we can.
    CdmFolderDefinition oldFolder = (CdmFolderDefinition) this.getOwner();
    String newPath;
    if (oldFolder == null) {
      newPath =
          this.getCtx()
              .getCorpus()
              .getStorage()
              .createRelativeCorpusPath(corpusPath, newFolder);
    } else {
      // If the current value != the absolute path, then assume it is a relative path.
      String absPath =
          this.getCtx()
              .getCorpus()
              .getStorage()
              .createAbsoluteCorpusPath(corpusPath, oldFolder);
      if (Objects.equals(absPath, corpusPath)) {
        newPath = absPath; // Leave it alone.
      } else {
        // Make it relative to the new folder then.
        newPath =
            this.getCtx()
                .getCorpus()
                .getStorage()
                .createRelativeCorpusPath(absPath, newFolder);
      }
    }

    if (newPath == null) {
      allWentWell.set(false);
    }

    return newPath;
  }

  public CompletableFuture<Boolean> refreshAsync() {
    return this.refreshAsync(new ResolveOptions(this));
  }

  public CompletableFuture<Boolean> refreshAsync(ResolveOptions resOpt) {
    if (resOpt == null) {
      resOpt = new ResolveOptions(this);
    }

    this.needsIndexing = true;
    return this.indexIfNeededAsync(resOpt);
  }

  public CompletableFuture<Boolean> indexIfNeededAsync(final ResolveOptions resOpt) {

    return CompletableFuture.supplyAsync(() -> {
      if (this.getNeedsIndexing()) {
        final CdmCorpusDefinition corpus = this.getFolder().getCorpus();

        corpus.resolveImportsAsync(this).join();
        corpus.docsNotIndexed.put(this, (short) 1);
        return corpus.indexDocuments(resOpt, this);
      }

      return true;
    });
  }

  public CompletableFuture<Boolean> saveAsAsync(final String newName) {
    return saveAsAsync(newName, false);
  }

  public CompletableFuture<Boolean> saveAsAsync(
      final String newName,
      final boolean saveReferenced) {
    return saveAsAsync(newName, saveReferenced, new CopyOptions());
  }

  public CompletableFuture<Boolean> saveAsAsync(
      final String newName,
      final boolean saveReferenced,
      CopyOptions options) {
    if (options == null) {
      options = new CopyOptions();
    }

    final ResolveOptions resOpt = new ResolveOptions(this);
    if (!this.indexIfNeededAsync(resOpt).join()) {
      LOGGER.error("Failed to index document prior to save '{}'", this.getName());
      return CompletableFuture.completedFuture(false);
    }

    if (newName.equals(this.getName())) {
      this.isDirty = false;
    }
    return this.getCtx().getCorpus().getPersistence().saveDocumentAsAsync(this, newName, saveReferenced, options);
  }

  CdmObject fetchObjectFromDocumentPath(final String objectPath) {
    // in current document?
    if (this.internalDeclarations.containsKey(objectPath)) {
      return this.internalDeclarations.get(objectPath);
    }
    return null;
  }

  @Override
  public String getAtCorpusPath() {
    if (this.folder == null) {
      return "NULL:/" + this.name;
    } else {
      return this.folder.getAtCorpusPath() + this.name;
    }
  }

  @Override
  public boolean visit(final String pathFrom, final VisitCallback preChildren, final VisitCallback postChildren) {
    if (preChildren != null && preChildren.invoke(this, pathFrom)) {
      return false;
    }
    if (this.getDefinitions() != null && this.getDefinitions()
        .visitList(pathFrom, preChildren, postChildren)) {
      return true;
    }
    return postChildren != null && postChildren.invoke(this, pathFrom);
  }

  @Override
  public boolean validate() {
    return !Strings.isNullOrEmpty(this.getName());
  }

  /**
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
    return CdmObjectBase.copyData(this, resOpt, options, CdmDocumentDefinition.class);
  }

  @Override
  public CdmObject copy(ResolveOptions resOpt, CdmObject host) {
    if (resOpt == null) {
      resOpt = new ResolveOptions(this);
    }

    CdmDocumentDefinition copy;
    if (host == null) {
      copy = new CdmDocumentDefinition(this.getCtx(), this.getName());
    } else {
      copy = (CdmDocumentDefinition) host;
      copy.setCtx(this.getCtx());
      copy.setName(this.getName());
      copy.getDefinitions().clear();
      copy.getImports().clear();
    }

    copy.setInDocument(copy);
    copy.setDirty(true);
    copy.setFolderPath(this.getFolderPath());
    copy.setSchema(this.getSchema());
    copy.setJsonSchemaSemanticVersion(this.getJsonSchemaSemanticVersion());

    for (final CdmObjectDefinition definition : this.getDefinitions()) {
      copy.getDefinitions().add(definition);
    }

    for (final CdmImport anImport : this.getImports()) {
      copy.getImports().add(anImport);
    }

    return copy;
  }

  @Override
  ResolvedAttributeSetBuilder constructResolvedAttributes(final ResolveOptions resOpt) {
    return constructResolvedAttributes(resOpt, null);
  }

  @Override
  ResolvedAttributeSetBuilder constructResolvedAttributes(final ResolveOptions resOpt, final CdmAttributeContext under) {
    // return null intentionally
    return null;
  }

  @Override
  void constructResolvedTraits(final ResolvedTraitSetBuilder rtsb, final ResolveOptions resOpt) {
    // intentionally NOP
    return;
  }

  OffsetDateTime getFileSystemModifiedTime() {
    return _fileSystemModifiedTime;
  }

  /**
   * @param _fileSystemModifiedTime
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public void setFileSystemModifiedTime(final OffsetDateTime _fileSystemModifiedTime) {
    this._fileSystemModifiedTime = _fileSystemModifiedTime;
  }

  private int prioritizeImports(final LinkedHashSet<CdmDocumentDefinition> processedSet, final Map<CdmDocumentDefinition, Integer> priorityMap,
                                final int sequence, final Map<String, CdmDocumentDefinition> monikerMap) {
    return prioritizeImports(processedSet, priorityMap, sequence, monikerMap, false);
  }

  private int prioritizeImports(final LinkedHashSet<CdmDocumentDefinition> processedSet, final Map<CdmDocumentDefinition, Integer> priorityMap,
                                int sequence, final Map<String, CdmDocumentDefinition> monikerMap, final boolean skipMonikered) {
    // goal is to make a map from the reverse order of imports (breadth first) to the first (aka last) sequence number in that list.
    // This gives the semantic that the 'last/shallowest' definition for a duplicate symbol wins,
    // the lower in this list a document shows up, the higher priority its definitions are for resolving conflicts.
    // for 'moniker' imports, keep track of the 'last/shallowest' use of each moniker tag.

    // if already in list, don't do this again
    if (processedSet.contains(this)) {
      return sequence;
    }
    processedSet.add(this);

    if (this.getImports() != null) {
      // first add the imports done at this level only
      final int l = this.getImports().getCount();
      // reverse order
      for (int i = l - 1; i >= 0; i--) {
        final CdmImport imp = this.getImports().getAllItems().get(i);
        final CdmDocumentDefinition impDoc = imp.getResolvedDocument();
        // don't add the moniker imports to the priority list
        final boolean isMoniker = !StringUtils.isNullOrTrimEmpty(imp.getMoniker());
        if (imp.getResolvedDocument() != null && !isMoniker) {
          if (!priorityMap.containsKey(impDoc)) {
            // add doc
            priorityMap.put(impDoc, sequence);
            sequence++;
          }
        }
      }

      // now add the imports of the imports
      for (int i = l - 1; i >= 0; i--) {
        final CdmImport imp = this.getImports().getAllItems().get(i);
        final CdmDocumentDefinition impDoc = imp.getResolvedDocument();
        // don't add the moniker imports to the priority list
        final boolean isMoniker = !StringUtils.isNullOrTrimEmpty(imp.getMoniker());
        if (impDoc != null && impDoc.importPriorities != null) {
          // lucky, already done so avoid recursion and copy
          final ImportPriorities impPriSub = impDoc.getImportPriorities();
          impPriSub.getImportPriority().remove(impDoc); // because already added above
          for (final Map.Entry<CdmDocumentDefinition, Integer> ip : impPriSub.getImportPriority().entrySet()
              .stream().sorted(
                  Comparator.comparing(entry -> entry.getKey().getName()))
              .collect(Collectors.toList())) {
            if (priorityMap.containsKey(ip.getKey()) == false) {
              // add doc
              priorityMap.put(ip.getKey(), sequence);
              sequence++;
            }
          }

          if (!isMoniker) {
            impPriSub.getMonikerPriorityMap().entrySet().forEach(mp -> {
              monikerMap.put(mp.getKey(), mp.getValue());
            });
          }
        } else if (impDoc != null) {
          // skip the monikered imports from here if this is a monikered import itself and we are only collecting the dependencies
          sequence = impDoc.prioritizeImports(processedSet, priorityMap, sequence, monikerMap, isMoniker);
        }
      }
      // skip the monikered imports from here if this is a monikered import itself and we are only collecting the dependencies
      if (!skipMonikered) {
        // moniker imports are prioritized by the 'closest' use of the moniker to the starting doc. so last one found in this recursion
        for (int i = 0; i < l; i++) {
          final CdmImport imp = this.getImports().getAllItems().get(i);
          if (imp.getResolvedDocument() != null && imp.getMoniker() != null) {
            monikerMap.put(imp.getMoniker(), imp.getResolvedDocument());
          }
        }
      }
    }
    return sequence;
  }

  /**
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public CompletableFuture<Boolean> saveLinkedDocumentsAsync() {
    return saveLinkedDocumentsAsync(null);
  }

  /**
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public CompletableFuture<Boolean> saveLinkedDocumentsAsync(final CopyOptions options) {
    // the only linked documents would be the imports
    return CompletableFuture.supplyAsync(() -> {
      if (this.getImports() != null) {
        for (final CdmImport anImport : this.getImports()) {
          final CdmImport imp = anImport;
          // get the document object from the import
          final CdmDocumentDefinition docImp = ((CdmDocumentDefinition) this.getCtx().getCorpus()
              .fetchObjectAsync(imp.getCorpusPath(), this).join());
          if (docImp != null && docImp.isDirty) {
            // save it with the same name
            if (!docImp.saveAsAsync(docImp.getName(), true, options).join()) {
              LOGGER.error("Failed to save import '{}'", docImp.getName());
              return false;
            }
          }
        }
      }
      return true;
    });
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(final String schema) {
    this.schema = schema;
  }

  public ImportPriorities getImportPriorities() {
    if (this.importPriorities == null) {
      this.importPriorities = new ImportPriorities();
      this.importPriorities.getImportPriority().put(this, 0);
      this.prioritizeImports(new LinkedHashSet<>(), this.importPriorities.getImportPriority(), 1,
          this.importPriorities.getMonikerPriorityMap(), false);
    }
    // make a copy so the caller doesn't mess these up
    return this.importPriorities.copy();
  }

  public void setImportPriorities(final ImportPriorities importPriorities) {
    this.importPriorities = importPriorities;
  }

  /**
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public boolean isDirty() {
    return isDirty;
  }

  public void setDirty(final boolean dirty) {
    isDirty = dirty;
  }

  public CompletableFuture<Void> reloadAsync() {
    return getCtx().getCorpus().fetchObjectAsync(getAtCorpusPath(), null, true)
        .thenAccept((v) -> {
        });
  }
}
