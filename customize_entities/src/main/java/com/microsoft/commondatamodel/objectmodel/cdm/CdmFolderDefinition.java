package com.microsoft.commondatamodel.objectmodel.cdm;

import com.google.common.base.Strings;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.persistence.PersistenceLayer;
import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedTraitSet;
import com.microsoft.commondatamodel.objectmodel.storage.StorageAdapter;
import com.microsoft.commondatamodel.objectmodel.utilities.CopyOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import com.microsoft.commondatamodel.objectmodel.utilities.StringUtils;
import com.microsoft.commondatamodel.objectmodel.utilities.VisitCallback;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdmFolderDefinition extends CdmObjectDefinitionBase implements CdmContainerDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger(CdmFolderDefinition.class);
  private final Map<String, CdmDocumentDefinition> documentLookup = new LinkedHashMap<>();
  private final CdmFolderCollection childFolders = new CdmFolderCollection(this.getCtx(), this);
  private final CdmDocumentCollection documents = new CdmDocumentCollection(this.getCtx(), this);

  private CdmCorpusDefinition corpus;
  private String name;
  private String folderPath;
  private String namespace;

  public CdmFolderDefinition(final CdmCorpusContext ctx, final String name) {
    super(ctx);

    this.name = name;
    this.folderPath = name + "/";
    setObjectType(CdmObjectType.FolderDef);
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public CdmCorpusDefinition getCorpus() {
    return corpus;
  }

  /**
   *
   * @param corpus
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public void setCorpus(final CdmCorpusDefinition corpus) {
    this.corpus = corpus;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(final String folderPath) {
    this.folderPath = folderPath;
  }

  public CdmFolderCollection getChildFolders() {
    return childFolders;
  }

  public CdmDocumentCollection getDocuments() {
    return documents;
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public String getNamespace() {
    return namespace;
  }

  /**
   *
   * @param namespace
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public void setNamespace(final String namespace) {
    this.namespace = namespace;
  }

  CompletableFuture<CdmDocumentDefinition> fetchDocumentFromFolderPathAsync(
          final String path,
          final StorageAdapter adapter) {
    return this.fetchDocumentFromFolderPathAsync(path, adapter, false);
  }

  /**
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public Map<String, CdmDocumentDefinition> getDocumentLookup() {
    return documentLookup;
  }

  CompletableFuture<CdmDocumentDefinition> fetchDocumentFromFolderPathAsync(
      final String objectPath,
      final StorageAdapter adapter,
      final boolean forceReload) {
    final String docName;
    final int first = objectPath.indexOf("/");
    if (first < 0) {
      docName = objectPath;
    } else {
      docName = objectPath.substring(0, first);
    }

    final CdmDocumentDefinition doc = this.documentLookup.get(docName);
    // got that doc?
    if (this.documentLookup.containsKey(docName)) {
      if (!forceReload) {
        return CompletableFuture.completedFuture(doc);
      }
      if (doc.isDirty()) {
        LOGGER.warn("discarding changes in document: {}", doc.getName());
      }
      this.documents.remove(docName);
    }

    return CompletableFuture.supplyAsync(() -> {
      final CdmDocumentDefinition documentDefinition = this.corpus.getPersistence().loadDocumentFromPathAsync(this, docName, doc).join();
      return documentDefinition;
    });
  }

  @Override
  public <T extends CdmObjectDefinition> T fetchObjectDefinition(final ResolveOptions resOpt) {
    return null;
  }

  @Override
  public ResolvedTraitSet fetchResolvedTraits(final ResolveOptions resOpt) {
    // Intended to return null;
    return null;
  }

  CompletableFuture<CdmFolderDefinition> fetchChildFolderFromPathAsync(final String path) {
    return fetchChildFolderFromPathAsync(path, false);
  }

  public CompletableFuture<CdmFolderDefinition> fetchChildFolderFromPathAsync(
      final String path,
      final boolean makeFolder) {
    return CompletableFuture.supplyAsync(() -> {
      String name;
      final String remainingPath;
      int first = path.indexOf('/');
      if (first < 0) {
        name = path;
        remainingPath = "";
      } else {
        name = StringUtils.slice(path, 0, first);
        remainingPath = StringUtils.slice(path, first + 1);
      }

      if (name.equalsIgnoreCase(this.getName())) {
        // the end?
        if (remainingPath.length() == 0) {
          return this;
        }
        // check children folders
        CdmFolderDefinition result = null;
        if (this.getChildFolders() != null) {
          for (int i = 0; i < this.getChildFolders().size(); i++) {
            result =
                    this.getChildFolders()
                            .get(i)
                            .fetchChildFolderFromPathAsync(remainingPath, makeFolder)
                            .join();
            if (result != null) {
              break;
            }
          }
        }
        if (result != null) {
          return result;
        }

        // get the next folder
        first = remainingPath.indexOf("/");
        name = first > 0 ? remainingPath.substring(0, first) : remainingPath;

        if (first != -1) {
          final CdmFolderDefinition childPath = this.getChildFolders().add(name).fetchChildFolderFromPathAsync(remainingPath, makeFolder).join();
          if (childPath == null) {
            LOGGER.error("Invalid path '{}'.", path);
          }
          return childPath;
        }

        if (makeFolder) {
          // huh, well need to make the fold here
          return this.getChildFolders().add(name).fetchChildFolderFromPathAsync(remainingPath, makeFolder).join();
        } else {
          // good enough, return where we got to
          return this;
        }
      }

      return null;
    });
  }

  @Override
  public boolean visit(
      final String pathRoot,
      final VisitCallback preChildren,
      final VisitCallback postChildren) {
    // Intended to return false;
    return false;
  }

  @Override
  public boolean isDerivedFrom(final String baseDef, final ResolveOptions resOpt) {
    return false;
  }

  @Override
  public String getAtCorpusPath() {
    if (this.namespace == null) {
      // We're not under any adapter (not in a corpus), so return special indicator.
      return "NULL:" + this.folderPath;
    } else {
      return this.namespace + ":" + this.folderPath;
    }
  }

  @Override
  public boolean validate() {
    return !Strings.isNullOrEmpty(this.name);
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
    return CdmObjectBase.copyData(this, resOpt, options, CdmFolderDefinition.class);
  }

  @Override
  public CdmObject copy(final ResolveOptions resOpt, final CdmObject host) {
    // Intended to return null;
    return null;
  }
}
