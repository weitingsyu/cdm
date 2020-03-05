package com.microsoft.commondatamodel.objectmodel.persistence.cdmfolder.types;

/**
 * The referenced entity declaration for CDM folders format.
 */
public class ReferencedEntityDeclaration extends EntityDeclaration {
    private String entityPath;
    private String lastFileStatusCheckTime;
    private String lastFileModifiedTime;

    /**
     * Gets the corpus path pointing to the external document.
     * @return
     */
    public String getEntityPath() {
        return entityPath;
    }

    /**
     * Sets the corpus path pointing to the external document.
     * @param entityPath
     */
    public void setEntityPath(final String entityPath) {
        this.entityPath = entityPath;
    }

    /**
     * Sets the corpus path pointing to the external document.
     * @param entityDeclaration
     */
    public void setEntityDeclaration(final String entityDeclaration) {
        this.entityPath = entityDeclaration;
    }

    public String getLastFileStatusCheckTime() {
        return lastFileStatusCheckTime;
    }

    public void setLastFileStatusCheckTime(final String lastFileStatusCheckTime) {
        this.lastFileStatusCheckTime = lastFileStatusCheckTime;
    }

    public String getLastFileModifiedTime() {
        return lastFileModifiedTime;
    }

    public void setLastFileModifiedTime(final String lastFileModifiedTime) {
        this.lastFileModifiedTime = lastFileModifiedTime;
    }
}