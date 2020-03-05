package com.microsoft.commondatamodel.objectmodel.resolvedmodel;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusContext;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmObjectBase;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitDefinition;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveContextScope;
import com.microsoft.commondatamodel.objectmodel.utilities.ResolveOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class ResolveContext implements CdmCorpusContext {

  final protected Map<String, Object> cache;
  String corpusPathRoot;
  int errors;
  private Stack<ResolveContextScope> scopeStack;
  private ResolveContextScope currentScope;
  private CdmCorpusDefinition corpus;
  private String relativePath;

  public ResolveContext(final CdmCorpusDefinition corpus) {
    this.cache = new LinkedHashMap<>();
    this.corpus = corpus;
  }

  @Override
  public CdmCorpusDefinition getCorpus() {
    return this.corpus;
  }

  @Override
  public void setCorpus(final CdmCorpusDefinition value) {
    this.corpus = value;
  }

  public void pushScope(CdmTraitDefinition currentTrait) {
    if (this.scopeStack == null) {
      this.scopeStack = new Stack<>();
    }

    final ResolveContextScope ctxNew = new ResolveContextScope();
    if (currentTrait == null && this.currentScope != null) {
      currentTrait = this.currentScope.getCurrentTrait();
    }
    ctxNew.setCurrentTrait(currentTrait);
    ctxNew.setCurrentParameter(0);

    this.currentScope = ctxNew;
    this.scopeStack.push(ctxNew);
  }

  public void popScope() {
    this.scopeStack.pop();
    this.currentScope = this.scopeStack.size() > 0 ? this.scopeStack.peek() : null;
  }

  public Object fetchCache(final CdmObjectBase forObj, final String kind, final ResolveOptions resOpt) {
    final String key =
        forObj.getId()
            + "_"
            + (resOpt.getWrtDoc() != null ? resOpt.getWrtDoc().getId() : "NULL")
            + "_"
            + kind;
    return this.cache.get(key);
  }

  public Map<String, Object> fetchCache() {
    return this.cache;
  }

  public void updateCache(final CdmObjectBase forObj, final String kind, final Object value, final ResolveOptions resOpt) {
    final String key =
        forObj.getId()
            + "_"
            + (resOpt.getWrtDoc() != null ? resOpt.getWrtDoc().getId() : "NULL")
            + "_"
            + kind;
    if (!this.cache.containsKey(key)) {
      this.cache.put(key, value);
    } else {
      this.cache.replace(key, value);
    }
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public String getRelativePath() {
    return relativePath;
  }

  /**
   *
   * @param relativePath
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public void setRelativePath(final String relativePath) {
    this.relativePath = relativePath;
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public Stack<ResolveContextScope> getScopeStack() {
    return scopeStack;
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public ResolveContextScope getCurrentScope() {
    return currentScope;
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public String getCorpusPathRoot() {
    return corpusPathRoot;
  }

  /**
   *
   * @param corpusPathRoot
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public void setCorpusPathRoot(final String corpusPathRoot) {
    this.corpusPathRoot = corpusPathRoot;
  }

  /**
   *
   * @return
   * @deprecated This function is extremely likely to be removed in the public interface, and not
   * meant to be called externally at all. Please refrain from using it.
   */
  @Deprecated
  public Map<String, Object> getCache() {
    return cache;
  }
}
