package example;

import java.util.concurrent.ExecutionException;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDataPartitionDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDeclarationDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmFolderDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmManifestDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.storage.LocalAdapter;

public class WistronCDM {
    private final static String SCHEMA_DOCS_ROOT = "cdm:/core/wistron";

    private final static String FOUNDATION_JSON_PATH = "cdm:/foundations.cdm.json";

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        CdmCorpusDefinition cdmCorpus = new CdmCorpusDefinition();

        System.out.println("configure storage adapters");

        // Configure storage adapters to point at the target local manifest location and
        // at the fake public standards
        String pathFromExeToExampleRoot = "./";

        cdmCorpus.getStorage().mount("local",
                new LocalAdapter("../" + pathFromExeToExampleRoot + "example-public-standards/core/wistron"));
        cdmCorpus.getStorage().setDefaultNamespace("local"); // local is our default. so any paths that start out
                                                             // navigating
                                                             // without a device tag will assume local

        // Fake cdm, normaly use the github adapter
        // Mount it as the 'cdm' device, not the default so must use "cdm:/folder" to
        // get there
        cdmCorpus.getStorage().mount("cdm",
                new LocalAdapter("../" + pathFromExeToExampleRoot + "example-public-standards"));

        CdmManifestDefinition manifestAbstract = cdmCorpus.makeObject(CdmObjectType.ManifestDef, "tempAbstract");
        CdmFolderDefinition localRoot = cdmCorpus.getStorage().fetchRootFolder("local");
        localRoot.getDocuments().add(manifestAbstract);

        FinancialBasic.create(cdmCorpus, manifestAbstract, localRoot, SCHEMA_DOCS_ROOT, "FinancialBasic",
                FOUNDATION_JSON_PATH);
        createDefaultCdmJson(cdmCorpus, manifestAbstract);
        Performance.create(cdmCorpus, manifestAbstract, localRoot, SCHEMA_DOCS_ROOT, "Performance",
                FOUNDATION_JSON_PATH, "FinancialBasic");
        HitRate.create(cdmCorpus, manifestAbstract, localRoot, SCHEMA_DOCS_ROOT, "HitRate", FOUNDATION_JSON_PATH,
                "FinancialBasic");
        createDefaultCdmJson(cdmCorpus, manifestAbstract);

    }

    private static void createDefaultCdmJson(CdmCorpusDefinition cdmCorpus, CdmManifestDefinition manifestAbstract)
            throws InterruptedException, ExecutionException {

        CdmManifestDefinition manifestResolved = manifestAbstract.createResolvedManifestAsync("Financial", null).get();

        // Add an import to the foundations doc so the traits about partitions will
        // resolve nicely.
        manifestResolved.getImports().add(FOUNDATION_JSON_PATH);

        System.out.println("Save the documents");
        for (CdmEntityDeclarationDefinition eDef : manifestAbstract.getEntities()) {
            // Get the entity being pointed at
            CdmEntityDeclarationDefinition localEDef = eDef;
            CdmEntityDefinition entDef = cdmCorpus
                    .<CdmEntityDefinition>fetchObjectAsync(localEDef.getEntityPath(), manifestResolved).get();
            // Make a fake partition, just to demo that
            CdmDataPartitionDefinition part = cdmCorpus.makeObject(CdmObjectType.DataPartitionDef,
                    entDef.getEntityName() + "-data-description");
            localEDef.getDataPartitions().add(part);
            part.setExplanation("not real data, just for demo");
            // We have existing partition files for the custom entities, so we need to make
            // the partition point to the file location
            part.setLocation("local:/" + entDef.getEntityName() + "/partition-data.csv");
            // Add trait to partition for csv params
            CdmTraitReference csvTrait = part.getExhibitsTraits().add("is.partition.format.CSV", false);
            csvTrait.getArguments().add("columnHeaders", "true");
            csvTrait.getArguments().add("delimiter", ",");
        }
        // We can save the documents as manifest.cdm.json format or model.json
        // Save as manifest.cdm.json
        manifestResolved.saveAsAsync(manifestResolved.getManifestName() + ".manifest.cdm.json", true).get();
        // Save as a model.json
        // manifestResolved.saveAsAsync("model.json", true).get();

    }
}