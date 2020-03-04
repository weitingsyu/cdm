package com.microsoft.commondatamodel.objectmodel.resolvedmodel;

import java.util.Map;

class TraitParamSpec {

  String traitBaseName;
  Map<String, String> parameters;

  String getTraitBaseName() {
    return traitBaseName;
  }

  void setTraitBaseName(final String traitBaseName) {
    this.traitBaseName = traitBaseName;
  }

  Map<String, String> getParameters() {
    return parameters;
  }

  void setParameters(final Map<String, String> parameters) {
    this.parameters = parameters;
  }
}
