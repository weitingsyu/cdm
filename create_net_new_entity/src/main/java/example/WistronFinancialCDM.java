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

import org.apache.commons.lang3.StringUtils;

public class WistronFinancialCDM {
        private final static String SCHEMA_DOCS_ROOT = "cdm:/core/wistron/financial";

        private final static String FOUNDATION_JSON_PATH = "cdm:/foundations.cdm.json";
        private final static String FACTORY_JSON_PATH = "cdm:/core/wistron/financial/FactoryInfo";
        private final static Set<String> entities = new HashSet<String>();

        public static void main(String[] args) throws ExecutionException, InterruptedException {

                System.out.println("configure storage adapters");

                CdmCorpusDefinition cdmCorpus = new CdmCorpusDefinition();

                System.out.println("configure storage adapters");

                // Configure storage adapters to point at the target local manifest location and
                // at the fake public standards
                String pathFromExeToExampleRoot = "./";

                cdmCorpus.getStorage().mount("financial", new LocalAdapter(
                                "../" + pathFromExeToExampleRoot + "example-public-standards/core/wistron/financial"));

                final AdlsAdapter adlsAdapter = new AdlsAdapter("wsdcdmstorage.dfs.core.windows.net", // Hostname.
                                "/powerbi/Financial", // Root.
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
                CdmFolderDefinition localRoot = cdmCorpus.getStorage().fetchRootFolder("financial");
                localRoot.getDocuments().add(manifestAbstract);
                manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot, "FactoryInfo",
                                getFactoryInfo(), "", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, null));
                createDefaultCdmJson(cdmCorpus, manifestAbstract);
                // basic cloumn : bg, type, site, model, reportingMonth
                List<RelationInfo> relations = new ArrayList<RelationInfo>();
                // relations.add(RelationInfo.builder().entityName("FactoryInfo").columnName("bg")
                //                 .jsonPath(FACTORY_JSON_PATH).description("BG relation").build());
                relations.add(RelationInfo.builder().entityName("FactoryInfo").columnName("site")
                                .jsonPath(FACTORY_JSON_PATH).description("Site relation").build());

                // manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot,
                // "FinancialBasic",
                // getFinancialBasicEntityInfo(), "", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH,
                // relations));
                // createDefaultCdmJson(cdmCorpus, manifestAbstract);

                // income statment
                manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot, "IncomeStatment",
                                getIncomeStatmentEntityInfo(), "", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, relations));

                // // revenue ratio
                // manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot,
                // "RevenueRatio",
                // getRevenueRatio(), "", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, null));

                // // Gap
                // manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot,
                // "Gap", getGapEntityInfo(),
                // "FinancialBasic", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, null));

                // income statement Hit Rate
                // manifestAbstract.getEntities()
                // .add(EntityUtils.create(cdmCorpus, localRoot, "IncomeStatmentHitRate",
                // getIncomeStatmentHitRateEntityInfo(), "FinancialBasic",
                // SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, null));

                // Performance
                manifestAbstract.getEntities().add(EntityUtils.create(cdmCorpus, localRoot, "Performance",
                                getPerformanceEntityInfo(), "", SCHEMA_DOCS_ROOT, FOUNDATION_JSON_PATH, relations));

                // Performance Hit Rate
                // manifestAbstract.getEntities()
                // .add(EntityUtils.create(cdmCorpus, localRoot, "PerformanceHitRate",
                // getPerformanceHitRateEntityInfo(), "FinancialBasic", SCHEMA_DOCS_ROOT,
                // FOUNDATION_JSON_PATH, null));
                createDefaultCdmJson(cdmCorpus, manifestAbstract);
                createDefaultCdmJson(cdmCorpus, manifestAbstract);

        }

        private static List<EntityInfo> getIncomeStatmentHitRateEntityInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("revenueHitRate").purpose("hasA").dataType("float")
                                .description("營收達成率 (對比年度預估值) or 營收達成率 (對比滾動預估值)").build());
                entityInfos.add(EntityInfo.builder().attributeName("operationIncomeHitRate").purpose("hasA")
                                .dataType("float").description("營收達成率 (對比年度預估值) or 營收達成率 (對比滾動預估值)").build());

                entityInfos.add(EntityInfo.builder().attributeName("mvaHitRate").purpose("hasA").dataType("float")
                                .description("營收達成率 (對比年度預估值) or 營收達成率 (對比滾動預估值)").build());

                entityInfos.add(EntityInfo.builder().attributeName("mohHitRate").purpose("hasA").dataType("float")
                                .description("營收達成率 (對比年度預估值) or 營收達成率 (對比滾動預估值)").build());

                entityInfos.add(EntityInfo.builder().attributeName("invoiceQtyHitRate").purpose("hasA")
                                .description("營收達成率 (對比年度預估值) or 營收達成率 (對比滾動預估值)").dataType("float").build());

                entityInfos.add(EntityInfo.builder().attributeName("productionQtyHitRate").purpose("hasA")
                                .dataType("float").description("營收達成率 (對比年度預估值) or 營收達成率 (對比滾動預估值)").build());

                return entityInfos;

        }

        private static List<EntityInfo> getFactoryInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("bg").purpose("identifiedBy").dataType("string")
                                .description("所屬事業群").build());
                entityInfos.add(EntityInfo.builder().attributeName("site").purpose("identifiedBy").dataType("string")
                                .description("工廠Site名稱").build());
                entityInfos.add(EntityInfo.builder().attributeName("address").purpose("hasA").dataType("string")
                                .description("工廠地址").build());
                entityInfos.add(EntityInfo.builder().attributeName("county").purpose("hasA").dataType("string")
                                .description("工廠所屬國家").build());

                return entityInfos;

        }

        private static List<EntityInfo> getPerformanceHitRateEntityInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("adsHitRate").purpose("hasA").dataType("float")
                                .description("庫存指標達成率").build());
                entityInfos.add(EntityInfo.builder().attributeName("yieldRateHitRate").purpose("hasA").dataType("float")
                                .description("良率指標達成率").build());

                entityInfos.add(EntityInfo.builder().attributeName("scrapRateHitRate").purpose("hasA").dataType("float")
                                .description("報廢指標達成率").build());

                return entityInfos;

        }

        private static List<EntityInfo> getGapEntityInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();

                entityInfos.add(EntityInfo.builder().attributeName("operationIncomeGap").purpose("hasA")
                                .dataType("float").description("落差值 (對比年度預估值) or 落差值 (對比滾動預估值)").build());
                entityInfos.add(EntityInfo.builder().attributeName("mohGap").purpose("hasA").dataType("float")
                                .description("落差值 (對比年度預估值) or 落差值 (對比滾動預估值) ").build());
                entityInfos.add(EntityInfo.builder().attributeName("metrialCostGap").purpose("hasA").dataType("float")
                                .description("落差值 (對比年度預估值) or 落差值 (對比滾動預估值) ").build());
                entityInfos.add(EntityInfo.builder().attributeName("otherGostGap").purpose("hasA").dataType("float")
                                .description("落差值 (對比年度預估值) or 落差值 (對比滾動預估值) ").build());
                entityInfos.add(EntityInfo.builder().attributeName("opexGap").purpose("hasA").dataType("float")
                                .description("落差值 (對比年度預估值) or 落差值 (對比滾動預估值) ").build());

                return entityInfos;

        }

        private static List<EntityInfo> getPerformanceEntityInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("product").purpose("hasA").dataType("string")
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("type").purpose("hasA").dataType("string").build());
                entityInfos.add(EntityInfo.builder().attributeName("reportingMonth").purpose("hasA").dataType("date")
                                .build());

                entityInfos.add(EntityInfo.builder().attributeName("ads").purpose("hasA").dataType("float")
                                .description("產品庫存量(天數)").build());
                entityInfos.add(EntityInfo.builder().attributeName("yieldRate").purpose("hasA").dataType("float")
                                .description("生產良率").build());
                entityInfos.add(EntityInfo.builder().attributeName("scrapRate").purpose("hasA").dataType("float")
                                .description("生產報廢率").build());

                return entityInfos;

        }

        private static List<EntityInfo> getRevenueRatio() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();

                entityInfos.add(EntityInfo.builder().attributeName("operationIncomeRatio").purpose("hasA")
                                .dataType("float").description("單月營業利潤佔營收比率").build());
                entityInfos.add(EntityInfo.builder().attributeName("mohRatio").purpose("hasA").dataType("float")
                                .description("單月生產成本佔營收比率").build());
                entityInfos.add(EntityInfo.builder().attributeName("materialCostRatio").purpose("hasA")
                                .dataType("float").description("單月材料成本佔營收比率").build());
                entityInfos.add(EntityInfo.builder().attributeName("otherCostRatio").purpose("hasA").dataType("float")
                                .description("單月其他成本佔營收比率").build());
                entityInfos.add(EntityInfo.builder().attributeName("opexRatio").purpose("hasA").dataType("float")
                                .description("單月管銷費用佔營收比率").build());
                entityInfos.add(EntityInfo.builder().attributeName("grossMarginRatio").purpose("hasA").dataType("float")
                                .description("單月毛利率").build());
                return entityInfos;

        }

        private static List<EntityInfo> getIncomeStatmentEntityInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();
                entityInfos.add(EntityInfo.builder().attributeName("product").purpose("hasA").dataType("string")
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("type").purpose("hasA").dataType("string").build());
                entityInfos.add(EntityInfo.builder().attributeName("reportingMonth").purpose("hasA").dataType("date")
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("revenue").purpose("hasA").dataType("float")
                                .description("單月營收").build());
                entityInfos.add(EntityInfo.builder().attributeName("operationIncome").purpose("hasA").dataType("float")
                                .description("單月營業利潤").build());
                entityInfos.add(EntityInfo.builder().attributeName("mva").purpose("hasA").dataType("float")
                                .description("單月代工費用").build());
                entityInfos.add(EntityInfo.builder().attributeName("moh").purpose("hasA").dataType("float")
                                .description("單月生產成本").build());
                entityInfos.add(EntityInfo.builder().attributeName("materialCost").purpose("hasA").dataType("float")
                                .description("單月材料成本").build());
                entityInfos.add(EntityInfo.builder().attributeName("otherCost").purpose("hasA").dataType("float")
                                .description("單月其他成本").build());
                entityInfos.add(EntityInfo.builder().attributeName("opex").purpose("hasA").dataType("float")
                                .description("單月管銷費用").build());
                entityInfos.add(EntityInfo.builder().attributeName("invoiceQty").purpose("hasA").dataType("float")
                                .description("單月出貨數量").build());
                entityInfos.add(EntityInfo.builder().attributeName("productionQty").purpose("hasA").dataType("float")
                                .description("單月生產數量").build());

                return entityInfos;

        }

        private static List<EntityInfo> getFinancialBasicEntityInfo() {
                List<EntityInfo> entityInfos = new ArrayList<EntityInfo>();

                entityInfos.add(EntityInfo.builder().attributeName("product").purpose("hasA").dataType("string")
                                .build());
                entityInfos.add(EntityInfo.builder().attributeName("type").purpose("hasA").dataType("string").build());
                entityInfos.add(EntityInfo.builder().attributeName("reportingMonth").purpose("hasA").dataType("date")
                                .build());
                return entityInfos;

        }

        private static void createDefaultCdmJson(CdmCorpusDefinition cdmCorpus, CdmManifestDefinition manifestAbstract)
                        throws InterruptedException, ExecutionException {

                CdmManifestDefinition manifestResolved = manifestAbstract.createResolvedManifestAsync("financial", null)
                                .get();

                // Add an import to the foundations doc so the traits about partitions will
                // resolve nicely.
                manifestResolved.getImports().add(FOUNDATION_JSON_PATH);
                // manifestResolved.getImports().add(FACTORY_JSON_PATH + ".cdm.json");

                System.out.println("Save the documents");
                for (CdmEntityDeclarationDefinition eDef : manifestAbstract.getEntities()) {
                        if (!entities.contains(eDef.getEntityName())
                                        && !StringUtils.equals(eDef.getEntityName(), "FinancialBasic")) {
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