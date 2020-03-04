package com.microsoft.commondatamodel.objectmodel.utilities;

import com.microsoft.commondatamodel.objectmodel.resolvedmodel.ResolvedAttribute;
import java.util.function.Consumer;

/**
 * @deprecated This class is extremely likely to be removed in the public interface, and not meant
 * to be called externally at all. Please refrain from using it.
 */
@Deprecated
public class ApplierState {
    boolean flexRemove;
    ResolvedAttribute arrayTemplate;
    Integer flexCurrentOrdinal;
    Integer arrayFinalOrdinal;
    Integer arrayInitialOrdinal;
    Consumer<ApplierContext> array_specializedContext;

    public ApplierState copy() {
        final ApplierState copy = new ApplierState();

        copy.flexRemove = flexRemove;
        copy.arrayTemplate = arrayTemplate;
        copy.flexCurrentOrdinal = flexCurrentOrdinal;
        copy.arrayFinalOrdinal = arrayFinalOrdinal;
        copy.arrayInitialOrdinal = arrayInitialOrdinal;
        copy.array_specializedContext = array_specializedContext;

        return copy;
    }
}
