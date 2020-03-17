package example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDataPartitionDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDeclarationDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmFolderDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmManifestDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.storage.AdlsAdapter;
import com.microsoft.commondatamodel.objectmodel.storage.LocalAdapter;

public class WistronFactoryCDM {
        private final static String SCHEMA_DOCS_ROOT = "cdm:/core/wistron/fin";
        private final static String FOUNDATION_JSON_PATH = "cdm:/foundations.cdm.json";
        private final static Set<String> entities = new HashSet<String>();

        public static void main(String[] args) throws ExecutionException, InterruptedException {

                System.out.println("configure storage adapters");

                CdmCorpusDefinition cdmCorpus = new CdmCorpusDefinition();

                System.out.println("configure storage adapters");

                // Configure storage adapters to point at the target local manifest location and
                // at the fake public standards
                String pathFromExeToExampleRoot = "./";

                cdmCorpus.getStorage().mount("local", new LocalAdapter(
                                "../" + pathFromExeToExampleRoot + "example-public-standards/core/wistron/factory"));
                final AdlsAdapter adlsAdapter = new AdlsAdapter("jasonstorage3.dfs.core.windows.net", // Hostname.
                                "/powerbi", // Root.
                                "69a44e1e-8420-46e1-b62c-888963ec4c3b", // Tenant ID.
                                "b3b6e653-8e60-4d71-8690-e573c3c5863b", // Client ID.
                                "e8_kP_5a9mYs:RPH?qbDq3YA2uwFC0ds" // Client secret.

                );

                cdmCorpus.getStorage().mount("adls", adlsAdapter);
                cdmCorpus.getStorage().setDefaultNamespace("adls"); // local is our default. so any paths that start
                                                                    // out
                                                                    // navigating
                                                                    // without a device tag will assume local

                // Fake cdm, normaly use the github adapter
                // Mount it as the 'cdm' device, not the default so must use "cdm:/folder" to
                // get there
                cdmCorpus.getStorage().mount("cdm",
                                new LocalAdapter("../" + pathFromExeToExampleRoot + "example-public-standards"));

                CdmManifestDefinition manifestAbstract = cdmCorpus.makeObject(CdmObjectType.ManifestDef,
                                "tempAbstract");
                CdmFolderDefinition localRoot = cdmCorpus.getStorage().fetchRootFolder("local");
                localRoot.getDocuments().add(manifestAbstract);
                manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot, "FactoryInfo",
                                getFactoryInfo(), "", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, null));
                createDefaultCdmJson(cdmCorpus, manifestAbstract);
                createDefaultCdmJson(cdmCorpus, manifestAbstract);

        }

        private static List<EntityInfo> getFactoryInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("bg").purpose("hasA").dataType("string")
                                .description("所屬事業群").build());
                entityInfos.add(EntityInfo.builder().attributeName("factory").purpose("hasA").dataType("string")
                                .description("工廠Site名稱").build());
                entityInfos.add(EntityInfo.builder().attributeName("address").purpose("hasA").dataType("string")
                                .description("工廠地址").build());
                entityInfos.add(EntityInfo.builder().attributeName("county").purpose("hasA").dataType("string")
                                .description("工廠所屬國家").build());

                return entityInfos;

        }

        private static void createDefaultCdmJson(CdmCorpusDefinition cdmCorpus, CdmManifestDefinition manifestAbstract)
                        throws InterruptedException, ExecutionException {

                CdmManifestDefinition manifestResolved = manifestAbstract.createResolvedManifestAsync("factory", null)
                                .get();

                // Add an import to the foundations doc so the traits about partitions will
                // resolve nicely.
                manifestResolved.getImports().add(FOUNDATION_JSON_PATH);

                System.out.println("Save the documents");
                for (CdmEntityDeclarationDefinition eDef : manifestAbstract.getEntities()) {
                        if (!entities.contains(eDef.getEntityName())) {
                                System.out.println(eDef.getEntityName());
                                // Get the entity being pointed at
                                CdmEntityDeclarationDefinition localEDef = eDef;
                                CdmEntityDefinition entDef = cdmCorpus.<CdmEntityDefinition>fetchObjectAsync(
                                                localEDef.getEntityPath(), manifestResolved).get();
                                // Make a fake partition, just to demo that
                                CdmDataPartitionDefinition part = cdmCorpus.makeObject(CdmObjectType.DataPartitionDef,
                                                entDef.getEntityName() + "-data-description");
                                localEDef.getDataPartitions().add(part);
                                part.setExplanation("not real data, just for demo");
                                // We have existing partition files for the custom entities, so we need to make
                                // the partition point to the file location
                                part.setLocation("adls:/" + entDef.getEntityName() + "/partition-data.csv");
                                // Add trait to partition for csv params
                                CdmTraitReference csvTrait = part.getExhibitsTraits().add("is.partition.format.CSV",
                                                false);
                                csvTrait.getArguments().add("columnHeaders", "true");
                                csvTrait.getArguments().add("delimiter", ",");
                        }
                        entities.add(eDef.getEntityName());
                }
                // We can save the documents as manifest.cdm.json format or model.json
                // Save as manifest.cdm.json
                manifestResolved.saveAsAsync(manifestResolved.getManifestName() + ".manifest.cdm.json", true).get();
                // Save as a model.json
                manifestResolved.saveAsAsync("model.json", true).get();

        }
}