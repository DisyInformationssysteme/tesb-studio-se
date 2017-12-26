// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.camel.designer.ui.bean;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.talend.camel.core.model.camelProperties.BeanItem;
import org.talend.commons.exception.SystemException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.repository.ui.editor.RepositoryEditorInput;
import org.talend.core.runtime.process.ITalendProcessJavaProject;
import org.talend.core.ui.component.ComponentsFactoryProvider;
import org.talend.designer.codegen.ICodeGeneratorService;
import org.talend.designer.codegen.ITalendSynchronizer;
import org.talend.designer.core.model.utils.emf.component.impl.ComponentFactoryImpl;
import org.talend.designer.core.model.utils.emf.component.impl.IMPORTTypeImpl;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.actions.AContextualAction;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public abstract class AbstractBeanAction extends AContextualAction {

    private Pattern CAMEL_CORE_PATTERN = Pattern.compile("camel-core-\\d+(.\\d+)*(\\S+)*(\\.jar)$");

    // protected RepositoryNode repositoryNode;

    /*
     * (non-Jsdoc)
     * 
     * @see org.talend.commons.ui.swt.actions.ITreeContextualAction#init(org.eclipse.jface.viewers.TreeViewer,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(TreeViewer viewer, IStructuredSelection selection) {
        setEnabled(false);
        Object o = selection.getFirstElement();
        if (selection.isEmpty() || selection.size() != 1 || !(o instanceof RepositoryNode)) {
            return;
        }
        repositoryNode = (RepositoryNode) o;
    }

    public IEditorPart openBeanEditor(BeanItem beanItem, boolean readOnly) throws SystemException, PartInitException {
        if (beanItem == null) {
            return null;
        }
        ICodeGeneratorService service = (ICodeGeneratorService) GlobalServiceRegister.getDefault().getService(
                ICodeGeneratorService.class);

        ECodeLanguage lang = ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getProject().getLanguage();
        ITalendSynchronizer routineSynchronizer = service.createRoutineSynchronizer();

        // check if the related editor is open.
        IWorkbenchPage page = getActivePage();

        IEditorReference[] editorParts = page.getEditorReferences();
        String talendEditorID = "org.talend.designer.core.ui.editor.StandAloneTalend" + lang.getCaseName() + "Editor"; //$NON-NLS-1$ //$NON-NLS-2$
        boolean found = false;
        IEditorPart talendEditor = null;
        for (IEditorReference reference : editorParts) {
            IEditorPart editor = reference.getEditor(false);
            if (talendEditorID.equals(editor.getSite().getId())) {
                // TextEditor talendEditor = (TextEditor) editor;
                RepositoryEditorInput editorInput = (RepositoryEditorInput) editor.getEditorInput();
                if (editorInput.getItem().equals(beanItem)) {
                    page.bringToTop(editor);
                    found = true;
                    talendEditor = editor;
                    break;
                }
            }
        }

        if (!found) {
            routineSynchronizer.syncRoutine(beanItem, true);
            IFile file = routineSynchronizer.getFile(beanItem);
            if (file == null) {
                return null;
            }
            RepositoryEditorInput input = new BeanEditorInput(file, beanItem);
            input.setReadOnly(readOnly);
            talendEditor = page.openEditor(input, talendEditorID); //$NON-NLS-1$            
        }

        return talendEditor;

    }

    /*
     * (non-Jsdoc)
     * 
     * @see org.talend.repository.ui.actions.AContextualAction#doRun()
     */
    @Override
    protected void doRun() {
        // TODO Auto-generated method stub

    }

    protected void addCamelDependency(BeanItem beanItem) {

        if (beanItem.getImports().size() == 0) {
            ModuleNeeded mn = addDefaultDependency(beanItem);
            deployLibrary(mn);
        } else {

            boolean needAddCamelCore = true;

            for (int i = 0; i < beanItem.getImports().size(); i++) {
                Object o = beanItem.getImports().get(i);

                if (o instanceof IMPORTTypeImpl) {
                    IMPORTTypeImpl importType = (IMPORTTypeImpl) o;
                    if (CAMEL_CORE_PATTERN.matcher(importType.getMODULE()).matches()) {
                        deployLibrary(getModuleNeeded("camel-core"));
                        needAddCamelCore = false;
                        continue;
                    }
                }
            }

            if (needAddCamelCore) {
                ModuleNeeded mn = addDefaultDependency(beanItem);
                deployLibrary(mn);
            }
        }
    }

    private ModuleNeeded getModuleNeeded(String aname) {
        IComponent component = ComponentsFactoryProvider.getInstance().get("cTimer", "CAMEL");
        List<ModuleNeeded> mns = component.getModulesNeeded();

        ModuleNeeded cmn = null;

        for (ModuleNeeded mn : mns) {
            if (mn.getId().equals(aname)) {
                cmn = mn;
                break;
            }
        }

        return cmn;
    }

    private ModuleNeeded addDefaultDependency(BeanItem beanItem) {

        IMPORTTypeImpl camelImport = (IMPORTTypeImpl) ComponentFactoryImpl.eINSTANCE.createIMPORTType();

        ModuleNeeded cmn = getModuleNeeded("camel-core");

        camelImport.setMODULE(cmn.getModuleName());
        camelImport.setMVN(cmn.getMavenUri());
        camelImport.setREQUIRED(true);
        beanItem.getImports().add(camelImport);

        return cmn;
    }

    public void deployLibrary(ModuleNeeded mn) {

        IFile pomFile = null;

        ITalendProcessJavaProject talendJavaProject = null;
        Model readMavenModel = null;

        if (GlobalServiceRegister.getDefault().isServiceRegistered(IRunProcessService.class)) {
            IRunProcessService service = (IRunProcessService) GlobalServiceRegister.getDefault()
                    .getService(IRunProcessService.class);
            talendJavaProject = service.getTalendProcessJavaProject();

            IFile beansPomFile = talendJavaProject.getProject()
                    .getFile(PomUtil.getPomFileName(TalendMavenConstants.DEFAULT_BEANS_ARTIFACT_ID));

            if (!beansPomFile.exists()) {
                try {
                    talendJavaProject.cleanMavenFiles(new NullProgressMonitor());
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }

            if (talendJavaProject != null) {
                pomFile = talendJavaProject.getProject().getFile(TalendMavenConstants.POM_FILE_NAME);
                try {
                    readMavenModel = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }

        try {

            boolean needReDeployLibrary = true;

            for (Dependency dependency : readMavenModel.getDependencies()) {
                if (mn.getModuleName().startsWith(dependency.getArtifactId())) {
                    needReDeployLibrary = false;
                    break;
                }
            }

            if (needReDeployLibrary) {
                CorePlugin.getDefault().getLibrariesService()
                        .deployLibrary(FileLocator.toFileURL(new URL(mn.getModuleLocaion())));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
