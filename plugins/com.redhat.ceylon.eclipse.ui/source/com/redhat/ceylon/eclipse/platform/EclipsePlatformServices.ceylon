import com.redhat.ceylon.ide.common.platform {
    PlatformServices,
    ModelServices,
    IdeUtils,
    VfsServices
}
import com.redhat.ceylon.ide.common.util {
    unsafeCast
}
import com.redhat.ceylon.ide.common.correct {
    ImportProposals
}
import com.redhat.ceylon.eclipse.code.correct {
    eclipseImportProposals
}

object eclipsePlatformServices satisfies PlatformServices {
    
    shared actual ModelServices<NativeProject,NativeResource,NativeFolder,NativeFile>
        model<NativeProject, NativeResource, NativeFolder, NativeFile>() => 
            unsafeCast<ModelServices<NativeProject,NativeResource,NativeFolder,NativeFile>>(eclipseModelServices);
    
    shared actual IdeUtils utils() => eclipsePlatformUtils;
    
    shared actual ImportProposals<IFile,ICompletionProposal,IDocument,InsertEdit,TextEdit,TextChange>
        importProposals<IFile, ICompletionProposal, IDocument, InsertEdit, TextEdit, TextChange>() =>
            unsafeCast<ImportProposals<IFile,ICompletionProposal,IDocument,InsertEdit,TextEdit,TextChange>>(eclipseImportProposals);

    shared actual VfsServices<NativeProject,NativeResource,NativeFolder,NativeFile> vfs<NativeProject, NativeResource, NativeFolder, NativeFile>() => nothing;
}