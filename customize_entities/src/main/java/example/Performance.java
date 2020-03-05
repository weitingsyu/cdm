package example;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmCorpusDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmDocumentDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmEntityDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmManifestDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmParameterDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitDefinition;
import com.microsoft.commondatamodel.objectmodel.cdm.CdmTraitReference;

import com.microsoft.commondatamodel.objectmodel.cdm.CdmTypeAttributeDefinition;
import com.microsoft.commondatamodel.objectmodel.enums.CdmObjectType;
import com.microsoft.commondatamodel.objectmodel.storage.LocalAdapter;

/*
 * -------------------------------------------------------------------------------------------------
 * This sample demonstrates how an existing entity loaded from some public standards can be
 * customized.
 * The steps are:
 *      1. Load a manifest from local file-system
 *      2. Create a new entity named 'MobileCareTeam' which extends from a standard entity called
 *         'CareTeam', and add an attribute named 'currentCity'
 *      3. Resolve and flatten this new local abstract description of 'CareTeam' entity, then add
 *         this customized version of 'CareTeam' entity to the manifest
 *      4. Save the new documents
 * -------------------------------------------------------------------------------------------------
 */
public class Performance {
    public static void main(String[] args) {
        // Make a corpus, the corpus is the collection of all documents and folders
        // created or
        // discovered while navigating objects and paths.
        final CdmCorpusDefinition cdmCorpus = new CdmCorpusDefinition();

        System.out.println("Configure storage adapters.");

        // Configure storage adapters to point at the target local manifest location and
        // at the fake
        // public standards.
        String pathFromExeToExampleRoot = "./";

        // Mount is as a local device.
        cdmCorpus.getStorage().mount("local", new LocalAdapter(pathFromExeToExampleRoot + "3-customize-entities"));
        cdmCorpus.getStorage().setDefaultNamespace("local");
        // local is our default. So any paths that start out navigating without a device
        // tag will assume local.

        // Mount it as the 'cdm' device, not the default so must use "cdm:/folder" to
        // get there.
        cdmCorpus.getStorage().mount("cdm",
                new LocalAdapter("../" + pathFromExeToExampleRoot + "example-public-standards"));

        // Example how to mount to the ADLS.
        // final AdlsAdapter adlsAdapter = new AdlsAdapter(
        // "<ACCOUNT_NAME>.dfs.core.windows.net", // Hostname.
        // "/<FILESYSTEM-NAME>", // Root.
        // "72f988bf-86f1-41af-91ab-2d7cd011db47", // Tenant ID.
        // "<CLIENT_ID>", // Client ID.
        // "<CLIENT_SECRET>" // Client secret.
        // );
        // cdmCorpus.getStorage().mount("adls", adlsAdapter);

        // Open the default manifest at the root, used later when done.
        // This method turns relative corpus paths into absolute ones in case we are in
        // some
        // sub-folders and don't know it.
        final CdmManifestDefinition manifest = cdmCorpus
                .<CdmManifestDefinition>fetchObjectAsync("wistron.manifest.cdm.json").join();

        System.out.println("Define new extension.");

        // First we will make a new document right in the same folder as the manifest.
        final CdmDocumentDefinition docAbs = cdmCorpus.makeObject(CdmObjectType.DocumentDef, "Performance.cdm.json");

        // Import the cdm description of the original so the symbols will resolve.
        docAbs.getImports().add("cdm:/wistron/BasicInfo.cdm.json");

        // We will make a new traitReference to identify things that are known to be
        // temporary, used later.
        // In theory this would be defined somewhere central so it can be shared.
        // CdmTraitDefinition traitTemp = (CdmTraitDefinition)
        // docAbs.getDefinitions().add(CdmObjectType.TraitDef,
        // "means.temporary");
        // // Extends the standard 'means' base traitReference.
        // traitTemp.setExtendsTrait(cdmCorpus.makeObject(CdmObjectType.TraitRef,
        // "means", true));
        // // Add a parameter for the expected duration in days.
        // CdmParameterDefinition param =
        // cdmCorpus.makeObject(CdmObjectType.ParameterDef, "estimatedDays");
        // param.setDataTypeRef(cdmCorpus.makeObject(CdmObjectType.DataTypeRef,
        // "integer"));
        // // By not using "true" on the last arg, this becomes an real reference object
        // in
        // // the json.
        // // Go look at the difference from "means" when this is done.
        // param.setDefaultValue("30");
        // traitTemp.getParameters().add(param);

        // Make an entity definition and add it to the list of definitions in the
        // document.
        CdmEntityDefinition entAbs = (CdmEntityDefinition) docAbs.getDefinitions().add(CdmObjectType.EntityDef,
                "Performance");
        // This entity extends the standard.
        // This function with 'true' will make a simple reference to the base.
        // entAbs.setExtendsEntity(cdmCorpus.makeObject(CdmObjectType.EntityRef,
        // "CareTeam", true));

        // And we will add an attribute.
        CdmTypeAttributeDefinition attNew1 = cdmCorpus.makeObject(CdmObjectType.TypeAttributeDef, "ads");
        // The attribute is a type is 'City" this is one of the predefined semantic
        // types in meanings.cdm.json.
        attNew1.setDataType(cdmCorpus.makeObject(CdmObjectType.DataTypeRef, "integer", true));
        attNew1.updateDescription("產品庫存量(天數)");

        // Also apply our fancy new 'temporary' traitReference. They stay in a city for
        // 90 days on average.
        // CdmTraitReference traitReference =
        // cdmCorpus.makeObject(CdmObjectType.TraitRef, "means.temporary");
        // traitReference.getArguments().add("estimatedDays", "90");
        // attNew1.getAppliedTraits().add(traitReference);

        // Add attribute to the entity.
        entAbs.getAttributes().add(attNew1);

        CdmTypeAttributeDefinition attNew2 = cdmCorpus.makeObject(CdmObjectType.TypeAttributeDef, "yield_rate");
        // The attribute is a type is 'City" this is one of the predefined semantic
        // types in meanings.cdm.json.
        attNew2.setDataType(cdmCorpus.makeObject(CdmObjectType.DataTypeRef, "float", true));
        attNew2.updateDescription("生產良率 ");
        entAbs.getAttributes().add(attNew2);
        // Also apply our fancy new 'temporary' traitReference. They stay in a city for
        // 90 days on average.

        // The entity abstract definition is done, add the document to the corpus in the
        // root folder and then save that doc.
        cdmCorpus.getStorage().fetchRootFolder("local").getDocuments().add(docAbs);

        // Next step is to remove all of the guesswork out of decoding the entity shape
        // by 'resolving' it to a relational by reference shape.
        System.out.println("Make a local 'resolved' copy.");

        // Now resolve it.
        // Made the entity and document have a different name to avoid conflicts in this
        // folder.
        CdmEntityDefinition entFlat = entAbs.createResolvedEntityAsync("LocalPerformance").join();

        // Now just add the pointer into our manifest.
        System.out.println("Add to manifest.");
        manifest.getEntities().add(entFlat);

        // This function will update all of the fileStatus times in the manifest.
        // manifest.refreshAsync().join();

        // And save the manifest along with linked definition files.
        manifest.saveAsAsync("wistron-resolved.manifest.cdm.json", true).join();
    }
}
