// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.esb.webservice.ui;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.datatools.enablement.oda.xml.util.ui.ATreeNode;
import org.eclipse.datatools.enablement.oda.xml.util.ui.XSDPopulationUtil2;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.util.XSDResourceImpl;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.ui.swt.advanced.dataeditor.AbstractDataTableEditorView;
import org.talend.commons.ui.swt.extended.table.ExtendedTableModel;
import org.talend.commons.ui.swt.formtools.LabelledFileField;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreator;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreatorColumn;
import org.talend.commons.ui.utils.PathUtils;
import org.talend.commons.utils.VersionUtils;
import org.talend.commons.utils.data.bean.IBeanPropertyAccessors;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.IESBService;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.metadata.MappingTypeRetriever;
import org.talend.core.model.metadata.MetadataTalendType;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.WSDLParameter;
import org.talend.core.model.metadata.builder.connection.WSDLSchemaConnection;
import org.talend.core.model.metadata.builder.connection.XMLFileNode;
import org.talend.core.model.metadata.builder.connection.XmlFileConnection;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.properties.XmlFileConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.RepositoryManager;
import org.talend.core.model.utils.ContextParameterUtils;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.ui.AbstractWebService;
import org.talend.core.ui.proposal.TalendProposalUtils;
import org.talend.cwm.helper.ConnectionHelper;
import org.talend.cwm.helper.PackageHelper;
import org.talend.designer.core.model.utils.emf.talendfile.ContextType;
import org.talend.designer.esb.webservice.WebServiceComponent;
import org.talend.designer.esb.webservice.WebServiceComponentMain;
import org.talend.designer.esb.webservice.WebServiceComponentPlugin;
import org.talend.designer.esb.webservice.data.ExternalWebServiceUIProperties;
import org.talend.designer.esb.webservice.i18n.Messages;
import org.talend.designer.esb.webservice.managers.WebServiceManager;
import org.talend.designer.esb.webservice.ws.WSDLDiscoveryHelper;
import org.talend.designer.esb.webservice.ws.wsdlinfo.FlowInfo;
import org.talend.designer.esb.webservice.ws.wsdlinfo.Function;
import org.talend.designer.esb.webservice.ws.wsdlinfo.ParameterInfo;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.ERepositoryStatus;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.IRepositoryNode.EProperties;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.ui.dialog.RepositoryReviewDialog;
import org.talend.repository.ui.utils.ConnectionContextHelper;
import orgomg.cwm.resource.record.RecordFactory;
import orgomg.cwm.resource.record.RecordFile;

/**
 * gcui class global comment. Detailled comment
 */
@SuppressWarnings("unchecked")
public class WebServiceUI extends AbstractWebService {

    private static final String METHOD = "METHOD";
    private static final String TRUE = "true";
    private static final String NEED_SSL_TO_TRUSTSERVER = "NEED_SSL_TO_TRUSTSERVER";
    private static final String PORT_NAME = "PORT_NAME";
    private static final String ENDPOINT = "ENDPOINT";

    private static final String ERROR_GETTING_WSDL = "Error getting service description";

    protected int maximumRowsToPreview = CorePlugin.getDefault().getPreferenceStore()
            .getInt(ITalendCorePrefConstants.PREVIEW_LIMIT);

    private WebServiceManager webServiceManager;

    private Composite uiParent;

    private LabelledFileField wsdlField;

    private Label operationLabel;

    private Label portNameLabel;

    private CTabFolder tabFolder;

    private CTabItem wsdlTabItem;

    private Composite wsdlComposite;

    private SashForm allContentForm;

    private AbstractDataTableEditorView<Function> listTableView;

    private AbstractDataTableEditorView<String> portListTableView;

    private Button refreshbut;

	private Button servicebut;

    private Table listTable;

    private Table portListTable;

    private WebServiceComponent connector;

    private static int DEFAULT_INDEX = 0;

    private int selectedColumnIndex = DEFAULT_INDEX;

    private List<Function> functionList = new ArrayList<Function>();

    private List<String> allPortNames = new ArrayList<String>();

    private String URLValue;

    private Function currentFunction;

    private String currentPortName;

    private WSDLSchemaConnection connection = null;

    private List<Function> allFunctions = new ArrayList<Function>();

    private Set<String> portNameList = new HashSet<String>();

    private Button wizardOkButton;
    
    private String parseUrl = "";

    private Button populateCheckbox;
    
    private boolean gotNewData = false;

    public WebServiceUI(Composite uiParent, WebServiceComponentMain webServiceMain) {
        super();
        this.uiParent = uiParent;
        this.webServiceManager = webServiceMain.getWebServiceManager();
        this.connector = webServiceMain.getWebServiceComponent();
        URLValue = new String();
        initWebserviceUI();
    }

    public WebServiceUI(Composite uiParent, WebServiceComponentMain webServiceMain, ConnectionItem connectionItem) {
        super();
        this.uiParent = uiParent;
        this.webServiceManager = webServiceMain.getWebServiceManager();
        this.connector = webServiceMain.getWebServiceComponent();
        this.connection = (WSDLSchemaConnection) connectionItem.getConnection();
        URLValue = new String();
        initWebserviceUI();
    }

    private void initWebserviceUI() {
        IElementParameter METHODPara = connector.getElementParameter(METHOD); //$NON-NLS-1$
        Object obj = METHODPara.getValue();
        if (obj == null) {
            return;
        }
        if (obj != null && obj instanceof String && !"".equals(obj)) {
            String currentURL = (String) connector.getElementParameter(PORT_NAME).getValue(); //$NON-NLS-1$

            allPortNames.clear();
            allPortNames.add(currentURL);

            Function fun = new Function(obj.toString());
            fun.setPortNames(Arrays.asList(new String[]{currentURL}));
            functionList.clear();
            functionList.add(fun);
            allFunctions.add(fun);
            if (fun != null) {
                currentFunction = fun;
            }
            initwebServiceMappingData(currentURL);
        }
    }

    private void initwebServiceMappingData(String currentURL) {
        if (currentURL != null && !currentURL.equals("")) {
            String str = currentURL;
            IElementParameter METHODPara = this.connector.getElementParameter(METHOD);
            Object obj = METHODPara.getValue();
            if (obj == null) {
                return;
            }
            if (obj instanceof String) {
                str = (String) obj;
            }
            Function fun = new Function(str);
            currentFunction = fun;
        }
    }

    private static IStatus[] getStatus(final Throwable e, final String pluginId) {
        List<IStatus> alStatus = new ArrayList<IStatus>();
        alStatus.add(new Status(IStatus.ERROR, pluginId, 0, e.getClass().getName(), e));
        for (int i = 0; i < e.getStackTrace().length; i++) {
            alStatus.add(new Status(IStatus.ERROR, pluginId, 0, e.getStackTrace()[i].toString(), null));
        }
        return alStatus.toArray(new IStatus[alStatus.size()]);
    }

    public final void openErrorDialog(String message, Throwable e) {
        String msg = (message != null) ? message : ((e.getMessage() != null) ? e.getMessage() : e.getClass().getName()); //$NON-NLS-1$
        String pluginId = WebServiceComponentPlugin.PLUGIN_ID;
        final IStatus status = new MultiStatus(pluginId, 0, getStatus(e, pluginId), msg, null);
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                ErrorDialog.openError(uiParent.getShell(), Messages.getString("Error"), null, status);
            }
        });
    }

    /**
     * DOC gcui Comment method "useSSL".
     * 
     * @return
     */
    private void useSSL() {
        String trustStoreFile = "";
        String trustStorePassword = "";
        IElementParameter trustserverFileParameter = webServiceManager.getWebServiceComponent().getElementParameter(
                "SSL_TRUSTSERVER_TRUSTSTORE");
        IElementParameter trustserverPasswordParameter = webServiceManager.getWebServiceComponent().getElementParameter(
                "SSL_TRUSTSERVER_PASSWORD");
        if (trustserverFileParameter.getValue() != null) {
            trustStoreFile = trustserverFileParameter.getValue().toString();
            trustStoreFile = TalendTextUtils.removeQuotes(trustStoreFile);
        }
        if (trustserverPasswordParameter.getValue() != null) {
            trustStorePassword = trustserverPasswordParameter.getValue().toString();
            trustStorePassword = TalendTextUtils.removeQuotes(trustStorePassword);
        }

        // System.clearProperty("javax.net.ssl.trustStore");
        System.setProperty("javax.net.ssl.trustStore", trustStoreFile);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
    }

    public void init() {
        uiParent.setLayout(new GridLayout());

        Composite composite = new Composite(uiParent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FormLayout());

        allContentForm = new SashForm(composite, SWT.NONE);
        FormData formData = new FormData();
        formData.top = new FormAttachment(5, 5);
        formData.left = new FormAttachment(0, 5);
        formData.right = new FormAttachment(100, -5);
        formData.bottom = new FormAttachment(100, -5);
        allContentForm.setLayoutData(formData);
        createViewers(allContentForm);
    }

    private void setOk(boolean enabled) {
        if (null != wizardOkButton) {
            wizardOkButton.setEnabled(enabled && gotNewData);
        }
    }

    protected WebServiceComponent getWebServiceComponent() {
        return getWebServiceManager().getWebServiceComponent();
    }

    protected WebServiceManager getWebServiceManager() {
        return this.webServiceManager;
    }

    private void createViewers(SashForm allContentForm) {
        createHeader(allContentForm);
    }

    private void createHeader(SashForm allContentForm) {
        //
        tabFolder = new CTabFolder(allContentForm, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        wsdlTabItem = new CTabItem(tabFolder, SWT.NONE);
        wsdlTabItem.setText(ExternalWebServiceUIProperties.WSDL_LABEL);
        tabFolder.setSelection(wsdlTabItem);
        tabFolder.setSimple(false);
        wsdlTabItem.setControl(createWSDLStatus());
    }

    @SuppressWarnings("rawtypes")
    private class DataTableEditorView<T> extends AbstractDataTableEditorView<T>{
    
        private IBeanPropertyAccessors accessors;
        private TableViewerCreatorColumn rowColumn;

        public DataTableEditorView(Composite parent, int style, ExtendedTableModel<T> model, boolean b, boolean c, boolean d, IBeanPropertyAccessors accessors) {
            super(parent, style, model, false, true, false, false);
            this.accessors = accessors;
            initGraphicComponents();
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
            gridData.minimumHeight = 150;
            this.getMainComposite().setLayoutData(gridData);
            GridLayout layout = (GridLayout) this.getMainComposite().getLayout();
            layout.marginWidth = 0;
            layout.marginHeight = 0;

        }
        
        protected void setTableViewerCreatorOptions(TableViewerCreator<T> newTableViewerCreator) {
            super.setTableViewerCreatorOptions(newTableViewerCreator);
            newTableViewerCreator.setHeaderVisible(false);
            newTableViewerCreator.setVerticalScroll(true);
            newTableViewerCreator.setReadOnly(true);
        }

        protected void createColumns(TableViewerCreator<T> tableViewerCreator, Table table) {
            rowColumn = new TableViewerCreatorColumn(tableViewerCreator);
            rowColumn.setTitle(Messages.getString("WebServiceUI.COLUMN")); //$NON-NLS-1$
            rowColumn.setBeanPropertyAccessors(accessors);
            rowColumn.setWeight(60);
            rowColumn.setModifiable(true);
            rowColumn.setMinimumWidth(60);
            rowColumn.setCellEditor(new TextCellEditor(tableViewerCreator.getTable()));
        }
    };
    
    
    private Composite createWSDLStatus() {
        wsdlComposite = new Composite(tabFolder, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 20;
        layout.marginHeight = 20;
        wsdlComposite.setLayout(layout);
        wsdlComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // WSDL URL
        Composite wsdlUrlcomposite = new Composite(wsdlComposite, SWT.NONE);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
        layoutData.verticalIndent = 2;
        layoutData.verticalSpan = 1;
        wsdlUrlcomposite.setLayoutData(layoutData);
		layout = new GridLayout(5, false);
        wsdlUrlcomposite.setLayout(layout);

        wsdlField = new LabelledFileField(wsdlUrlcomposite, ExternalWebServiceUIProperties.FILE_LABEL,
                ExternalWebServiceUIProperties.FILE_EXTENSIONS, 1, SWT.BORDER) {

            protected void setFileFieldValue(String result) {
                if (result != null) {
                    getTextControl().setText(TalendTextUtils.addQuotes(PathUtils.getPortablePath(result)));
                    getDataFromNet();
                    if (portListTable.getItemCount() > 1) {
                        portListTable.deselectAll();
                        setOk(false);
                    }
                    if (listTable.getItemCount() == 1) {
                        selectFirstFunction();
                    }
                }
            }

        };
        wsdlField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                URLValue = wsdlField.getText();
                if (connection != null)
                    connection.setWSDL(URLValue);
            }
        });
        // add a listener for ctrl+space.
        TalendProposalUtils.installOn(wsdlField.getTextControl(), connector.getProcess(), connector);
        String wsdlUrl = (String) connector.getElementParameter(ENDPOINT).getValue(); //$NON-NLS-1$
        if (wsdlUrl != null) {
            wsdlField.setText(wsdlUrl);
        }

		// TESB-3590，gliu
		servicebut = new Button(wsdlUrlcomposite, SWT.PUSH | SWT.CENTER);
		servicebut.setText(Messages.getString("WebServiceUI.Services"));

        refreshbut = new Button(wsdlUrlcomposite, SWT.PUSH | SWT.CENTER);
        refreshbut.setImage(ImageProvider.getImage(EImage.REFRESH_ICON));
        GridData butData = new GridData();
        butData.verticalSpan = 1;
        refreshbut.setLayoutData(butData);

        // add port name UI
        Composite wsdlPortOperationComposite = new Composite(wsdlComposite, SWT.NONE);
        GridData portlayoutData = new GridData(GridData.FILL_HORIZONTAL);
        wsdlPortOperationComposite.setLayoutData(portlayoutData);
        layout = new GridLayout(2, false);
        wsdlPortOperationComposite.setLayout(layout);

        portNameLabel = new Label(wsdlPortOperationComposite, SWT.NONE);
        portNameLabel.setText(Messages.getString("WebServiceUI.Port")); //$NON-NLS-1$
        portNameLabel.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));

        ExtendedTableModel<String> portModel = new ExtendedTableModel<String>("PORTNAMELIST", allPortNames); //$NON-NLS-1$
        portListTableView = new DataTableEditorView<String>(
                wsdlPortOperationComposite, 
                SWT.NONE, portModel, false, true, false,
                new IBeanPropertyAccessors<String, String>() {
                    public String get(String bean) {
                        return bean;
                    }

                    public void set(String bean, String value) {
                      //readonly
                    }
                }
        );

        // WSDL Operation
        operationLabel = new Label(wsdlPortOperationComposite, SWT.NONE);
        operationLabel.setText(Messages.getString("WebServiceUI.Operation")); //$NON-NLS-1$
        operationLabel.setLayoutData(new GridData(SWT.NONE, SWT.TOP, false, false));

        ExtendedTableModel<Function> funModel = new ExtendedTableModel<Function>("FUNCTIONLIST", functionList); //$NON-NLS-1$
        listTableView = new DataTableEditorView<Function>(
                wsdlPortOperationComposite, 
                SWT.NONE, funModel, false, true, false,
                new IBeanPropertyAccessors<Function, String>() {
                    public String get(Function bean) {
                        return bean.getName();
                    }

                    public void set(Function bean, String value) {
                        //readonly
                    }
                }
        ); 
        
        addListenerForWSDLCom();
        
        populateCheckbox = new Button(wsdlComposite, SWT.CHECK | SWT.CENTER);
        populateCheckbox.setLayoutData(new GridData());
        populateCheckbox.setText("Populate schema to repository on finish");
        
        return wsdlComposite;
    }

    private void refresh() {
        final Job job = new Job("t") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                // monitor.setCanceled(true);
                monitor.beginTask("Retrieve WSDL parameter from net.", IProgressMonitor.UNKNOWN);
                getDataFromNet();
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
        ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getDisplay()
                .getActiveShell().getShell());
        IRunnableWithProgress runnable = new IRunnableWithProgress() {

            public void run(final IProgressMonitor monitor) {
                monitor.beginTask("Retrieve WSDL parameter from net.", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                boolean f = true;
                while (f) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (monitor.isCanceled()) {
                        job.done(Status.OK_STATUS);
                        job.cancel();
                    }
                    if (job.getResult() != null && job.getResult().isOK()) {
                        monitor.done();
                        f = false;
                    }
                }
            }
        };

        try {
            progressDialog.run(true, true, runnable);
        } catch (InvocationTargetException e1) {
            ExceptionHandler.process(e1);
        } catch (InterruptedException e1) {
            ExceptionHandler.process(e1);
        } catch (WebServiceCancelException e1) {
            return;
        }
        if (portListTable.getItemCount() > 1) {
            portListTable.deselectAll();
            setOk(false);
        }
        if (listTable.getItemCount() == 1) {
            selectFirstFunction();
        }
    }


    
    private void addListenerForWSDLCom() {
        wsdlField.getTextControl().addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent event) {
                switch (event.keyCode) {
                case 13:
                case SWT.KEYPAD_CR:
                    refresh();
                }
                
            }

            public void keyReleased(KeyEvent event) {
            }
        });

		servicebut.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				// TODO
				RepositoryReviewDialog dialog = new RepositoryReviewDialog(
						Display.getCurrent().getActiveShell(),
						ERepositoryObjectType.METADATA, "SERVICES:OPERATION", // see
																				// it
																				// in
																				// RepositoryTypeProcessor.RepositoryTypeProcessor(String)
						new ViewerFilter[] { new OnlyShowServicesFilter() }) {
					@Override
					protected boolean isSelectionValid(
							SelectionChangedEvent event) {
						IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();
						if (selection.size() == 1) {
							return true;
						}
						return false;
					}
				};
				int open = dialog.open();
				if (open == Dialog.OK) {
					RepositoryNode result = dialog.getResult();
					Item item = result.getObject().getProperty().getItem();
					if (GlobalServiceRegister.getDefault().isServiceRegistered(
							IESBService.class)) {
						IESBService service = (IESBService) GlobalServiceRegister
								.getDefault().getService(IESBService.class);
						String wsdlFilePath = service.getWsdlFilePath(item);
						if (wsdlFilePath != null) {
							wsdlField.getTextControl().setText(
									TalendTextUtils.addQuotes(PathUtils
											.getPortablePath(wsdlFilePath)));
							getDataFromNet();
							if (portListTable.getItemCount() > 1) {
								portListTable.deselectAll();
								setOk(false);
							}
							if (listTable.getItemCount() == 1) {
								selectFirstFunction();
							}
						}
					}

				}
			}

		});

        refreshbut.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                refresh();
            }

        });
        listTable = listTableView.getTable();
        portListTable = portListTableView.getTable();

        listTable.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                TableItem[] item = listTable.getSelection();
                currentFunction = (Function) item[0].getData();
                if (currentFunction != null) {
                    setOk(true);
                }
            }
        });

        portListTable.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                TableItem[] item = portListTable.getSelection();
                currentPortName = (String) item[0].getData();
                if (connection != null) {
                    if (!updateConnection()) {
                        connection.setPortName("");
                    }
                }
                List<Function> portFunctions = new ArrayList<Function>();
                for (Function function : allFunctions) {
                    List<String> portNames = function.getPortNames();
                    if ((portNames != null) && (portNames.contains(currentPortName))) {
                        portFunctions.add(function);
                    }
                }
                ExtendedTableModel<Function> listModel = listTableView.getExtendedTableModel();
                listModel.removeAll();
                listModel.addAll(portFunctions);
                selectFirstFunction();
            }
        });
    }

    private void selectFirstFunction() {
        if (listTable.getItemCount() > 0) {
            listTable.setSelection(new int[]{0});
            currentFunction = (Function) listTable.getItem(0).getData();
            setOk(true);
        } else {
            setOk(false);
        }
    }
    
    private void getDataFromNet() {
        allFunctions.clear();
        portNameList.clear();
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                URLValue = wsdlField.getText();
            }
        });

        if (URLValue == null) {
            URLValue = ""; //$NON-NLS-1$
        }
        allFunctions = getFunctionsList(URLValue); 
        gotNewData = true;
        for (Function function : allFunctions) {
            if ((function != null) && (function.getPortNames() != null)) {
                portNameList.addAll(function.getPortNames());
            }
        }
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                ExtendedTableModel<String> portListModel = portListTableView.getExtendedTableModel();
                portListModel.removeAll();
                //NO addAll(Collection) for ExtendedTableModel??? grrr
                for (String portName : portNameList) {
                    portListModel.add(portName);
                }
                //clear functions
                ExtendedTableModel<Function> listModel = listTableView.getExtendedTableModel();
                listModel.removeAll();
                if (portNameList.size() == 1) { //only one porttype
                    listModel.addAll(allFunctions);
                } 
            }

        });

    }

    private List<Function> getFunctionsList(String wsdlUrl) {
        List<Function> funList = new ArrayList<Function>();
        WSDLDiscoveryHelper ws = new WSDLDiscoveryHelper();
        WebServiceComponent webServiceComponent = webServiceManager.getWebServiceComponent();
        IElementParameter parameter = webServiceComponent.getElementParameter(NEED_SSL_TO_TRUSTSERVER);
        boolean isUseSSL = (parameter != null) && TRUE
                .equals(parameter.getValue().toString());

        if (isUseSSL) {
            useSSL();
        }

        try {
            if (wsdlUrl != null && !wsdlUrl.contains("\"")) {
                funList = ws.getFunctionsAvailable(parseContextParameter(wsdlUrl));
            } else {
                funList = ws.getFunctionsAvailable(wsdlUrl);
            }
        } catch (Exception e) {
            openErrorDialog(ERROR_GETTING_WSDL, e);
        }
        return funList;
    }

    private String parseContextParameter(final String contextValue) {
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                String url = "";
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                IContextManager contextManager = null;
                if (connector.getProcess() == null) {
                    // contextManager = contextModeManager.getSelectedContextType().getContextParameter(); //
                    // connection.get
                    // IContextManager contextManager
                    ContextType contextType = ConnectionContextHelper.getContextTypeForContextMode(connection);
                    url = ConnectionContextHelper.getOriginalValue(contextType, contextValue);
                } else {
                    contextManager = connector.getProcess().getContextManager();
                    String currentDefaultName = contextManager.getDefaultContext().getName();
                    List<IContext> contextList = contextManager.getListContext();
                    if ((contextList != null) && (contextList.size() > 1)) {
                        currentDefaultName = ConnectionContextHelper.getContextTypeForJob(shell, contextManager, false);
                    }
                    // ContextSetsSelectionDialog cssd=new ContextSetsSelectionDialog(shell,,false);
                    // ContextType contextType=ConnectionContextHelper.getContextTypeForContextMode(connector);
                    IContext context = contextManager.getContext(currentDefaultName);
                    url = ContextParameterUtils.parseScriptContextCode(contextValue, context);

                }
                parseUrl = url;
            }

        });

        return parseUrl;
    }

    public CTabFolder getTabFolder() {
        return this.tabFolder;
    }

    public Composite getWsdlComposite() {
        return this.wsdlComposite;
    }

    // bug 14067
    public String getURL() {
        return URLValue;
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    public String getCurrentPortName() {
        return currentPortName;
    }

    public List<String> getAllPortNames() {
        return this.allPortNames;
    }

    public int getSelectedColumnIndex() {
        return this.selectedColumnIndex;
    }

    public void saveProperties() {
        getWebServiceManager().savePropertiesToComponent();
        populateSchema();
    }

    public void prepareClosing(int dialogResponse) {

    }

    public Table getTable() {
        return listTable;
    }

    public LabelledFileField getWSDLLabel(Boolean b) {
        refreshbut.setEnabled(!b);
        return wsdlField;
    }

    public void saveInputValue() {
        if (connection != null) {
            if (currentFunction != null) {
                if (currentFunction.getName() != null) {
                    connection.setMethodName(currentFunction.getName());
                }
                if (currentFunction.getServerNameSpace() != null) {
                    connection.setServerNameSpace(currentFunction.getServerNameSpace());
                }
                if (currentFunction.getServerName() != null) {
                    connection.setServerName(currentFunction.getServerName());
                }
                if (currentFunction.getServerNameSpace() != null) {
                    connection.setPortNameSpace(currentFunction.getServerNameSpace());
                }
            }
            updateConnection();

        }
        EList<WSDLParameter> inputValue = connection.getParameterValue();

        IElementParameter INPUT_PARAMSPara = connector.getElementParameter("INPUT_PARAMS");
        List<Map<String, String>> inputparaValue = (List<Map<String, String>>) INPUT_PARAMSPara.getValue();
        if (inputparaValue != null) {
            inputValue.clear();
            if (currentFunction != null) {
                ParameterInfo inputParameter = currentFunction.getInput().getParameterRoot();
                if (inputParameter != null) {
                    boolean mark = true;
                    List<ParameterInfo> ls = new ArrayList<ParameterInfo>();
                    if (inputParameter != null) {
                        WSDLParameter parameter = ConnectionFactory.eINSTANCE.createWSDLParameter();
                        parameter.setParameterInfo(inputParameter.getName());
                        if (inputParameter.getParent() == null) {
                            parameter.setParameterInfoParent("");
                        } else {
                            parameter.setParameterInfoParent(inputParameter.getParent().getName());
                        }
                        inputValue.add(parameter);
                        mark = false;
                        if (!inputParameter.getParameterInfos().isEmpty()) {
                            ls.addAll(new ParameterInfoUtil().getAllChildren(inputParameter));
                        }
                    }
                    if (!mark) {
                        for (ParameterInfo para : ls) {
                            WSDLParameter parameter = ConnectionFactory.eINSTANCE.createWSDLParameter();
                            parameter.setParameterInfo(para.getName());
                            parameter.setParameterInfoParent(para.getParent().getName());
                            inputValue.add(parameter);
                        }
                    }

                }
            }
            String[] src = new String[]{"payload"};
            for (String insource : src) {
                WSDLParameter parameter = ConnectionFactory.eINSTANCE.createWSDLParameter();
                if (insource == null || "".equals(insource)) {
                    continue;
                }
                // Map<String, String> sourceMap = new HashMap<String, String>(1);
                parameter.setSource(insource);
                inputValue.add(parameter);
            }
        }
    }

    private void populateSchema() {
    	if (currentFunction == null || !(populateCheckbox.getSelection())) {
    		return;
    	}  	
    	
    	populateMessage(currentFunction.getInput());
    	populateMessage(currentFunction.getOutput());
    	for (FlowInfo fault : currentFunction.getFaults()) {
    		populateMessage(fault);
    	}
    }
    
    private void populateMessage(FlowInfo message) {
		if (message == null) {
			return;
		}
//    	String componentName = connector.getProcess().getName()+"_"+connector.getUniqueName();
        ParameterInfo parameter = message.getParameterRoot();
        String name = /*componentName + "_"+*/parameter.getName();
		XmlFileConnection connection = ConnectionFactory.eINSTANCE.createXmlFileConnection();
		connection.setName(ERepositoryObjectType.METADATA_FILE_XML.getKey());
		connection.setXmlFilePath(name+".xsd");
		XmlFileConnectionItem connectionItem = PropertiesFactory.eINSTANCE.createXmlFileConnectionItem();
        Property connectionProperty = PropertiesFactory.eINSTANCE.createProperty();
        connectionProperty.setAuthor(((RepositoryContext) CoreRuntimePlugin.getInstance().getContext()
                .getProperty(Context.REPOSITORY_CONTEXT_KEY)).getUser());
		connectionProperty.setLabel(name); 
        connectionProperty.setVersion(VersionUtils.DEFAULT_VERSION);
        connectionProperty.setStatusCode(""); //$NON-NLS-1$
        
		connectionItem.setProperty(connectionProperty);
        connectionItem.setConnection(connection);

		/*
		 * TESB-3689: change from true to false to fix the cannot imported by
		 * tESB* component
		 */
		connection.setInputModel(false);
		//schema
		connection.setFileContent(message.getSchema());

		// GLIU: add for TESB-3668
		fillMetadataInfo(message, name, connection);

        // save
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        String nextId = factory.getNextId();
        connectionProperty.setId(nextId);
        try {
			// http://jira.talendforge.org/browse/TESB-3655 Remove possible
			// schema prefix
			String folderString = currentFunction.getServerNameSpace() + "/"
					+ currentFunction.getPortTypeName();
			try {
				URI uri = new URI(folderString);
				String scheme = uri.getScheme();
				if (scheme != null) {
					folderString = folderString.substring(scheme.length());
				}
			} catch (URISyntaxException e) {

			}
			if (folderString.startsWith(":")) {
				folderString = folderString.substring(1);
			}
			folderString = replaceAllLimited(folderString);
			IPath path = new Path(folderString);
			// if (path.segmentCount() > 0 && path.segment(0).startsWith(":")) {
			// path = path.removeFirstSegments(1);
			// }
			factory.create(connectionItem, path);
			ProxyRepositoryFactory.getInstance().saveProject(ProjectManager.getInstance().getCurrentProject());
			RepositoryManager.refresh(ERepositoryObjectType.METADATA_FILE_XML);
		} catch (PersistenceException e) {
			openErrorDialog("Error populating schema to XML metadata.", e);
		}
	}

	/*
	 * GLIU: TESB-3668 start please refer to
	 * 
	 * @see org.talend.repository.services.action
	 * .PublishMetadataAction#populateMessage2(ParameterInfo, QName) for more
	 * details
	 */
	private int orderId;
	private boolean loopElementFound;

	private void fillMetadataInfo(FlowInfo message, String name,
			XmlFileConnection connection) {
		try {
			String text = wsdlField.getText();
			if (text.startsWith("\"")) {
				text = text.substring(1);
			}
			if (text.endsWith("\"")) {
				text = text.substring(0, text.length() - 1);
			}
			// URI uri = new File(text).toURI();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(
					message.getSchema());
			XSDResourceImpl xsdResourceImpl = new XSDResourceImpl(
					org.eclipse.emf.common.util.URI.createFileURI(text));
			xsdResourceImpl.load(inputStream, Collections.EMPTY_MAP);
			XSDSchema xsdSchema = xsdResourceImpl.getSchema();
			@SuppressWarnings("restriction")
			XSDPopulationUtil2 util = new XSDPopulationUtil2();
			List<ATreeNode> rootNodes = util.getAllRootNodes(xsdSchema);

			ATreeNode node = null;

			// try to find the root element needed from XSD file.
			// note: if there is any prefix, it will get the node with the first
			// correct name, no matter the prefix.
			// once the we can get the correct prefix value from the wsdl, this
			// code should be modified.
			for (ATreeNode curNode : rootNodes) {
				String curName = (String) curNode.getValue();
				if (curName.contains(":")) {
					// if with prefix, don't care about it for now, just compare
					// the name.
					if (curName.split(":")[1].equals(name)) {
						node = curNode;
						break;
					}
				} else if (curName.equals(name)) {
					node = curNode;
					break;
				}
			}
			node = util.getSchemaTree(xsdSchema, node, true);

			orderId = 1;
			loopElementFound = false;

			if (ConnectionHelper.getTables(connection).isEmpty()) {
				MetadataTable table = ConnectionFactory.eINSTANCE
						.createMetadataTable();
				RecordFile record = (RecordFile) ConnectionHelper.getPackage(
						connection.getName(), connection, RecordFile.class);
				if (record != null) {
					PackageHelper.addMetadataTable(table, record);
				} else {
					RecordFile newrecord = RecordFactory.eINSTANCE
							.createRecordFile();
					newrecord.setName(connection.getName());
					ConnectionHelper.addPackage(newrecord, connection);
					PackageHelper.addMetadataTable(table, newrecord);
				}
			}
			fillRootInfo(connection, node, "");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private void fillRootInfo(XmlFileConnection connection, ATreeNode node,
			String path) throws Exception {
		XMLFileNode xmlNode = ConnectionFactory.eINSTANCE.createXMLFileNode();
		xmlNode.setXMLPath(path + "/" + node.getValue());
		xmlNode.setOrder(orderId);
		orderId++;
		MappingTypeRetriever retriever;
		String nameWithoutPrefixForColumn;
		String curName = (String) node.getValue();
		if (curName.contains(":")) {
			nameWithoutPrefixForColumn = curName.split(":")[1];
		} else {
			nameWithoutPrefixForColumn = curName;
		}
		retriever = MetadataTalendType.getMappingTypeRetriever("xsd_id");
		xmlNode.setAttribute("attri");
		xmlNode.setType(retriever.getDefaultSelectedTalendType(node
				.getDataType()));
		MetadataColumn column = null;
		switch (node.getType()) {
		case ATreeNode.ATTRIBUTE_TYPE:
			xmlNode.setRelatedColumn(nameWithoutPrefixForColumn);
			column = ConnectionFactory.eINSTANCE.createMetadataColumn();
			column.setTalendType(xmlNode.getType());
			column.setLabel(nameWithoutPrefixForColumn);
			ConnectionHelper.getTables(connection)
					.toArray(new MetadataTable[0])[0].getColumns().add(column);
			break;
		case ATreeNode.ELEMENT_TYPE:
			boolean haveElementOrAttributes = false;
			for (Object curNode : node.getChildren()) {
				if (((ATreeNode) curNode).getType() != ATreeNode.NAMESPACE_TYPE) {
					haveElementOrAttributes = true;
					break;
				}
			}
			if (!haveElementOrAttributes) {
				xmlNode.setAttribute("branch");
				retriever = MetadataTalendType
						.getMappingTypeRetriever("xsd_id");
				xmlNode.setRelatedColumn(nameWithoutPrefixForColumn);
				xmlNode.setType(retriever.getDefaultSelectedTalendType(node
						.getDataType()));
				column = ConnectionFactory.eINSTANCE.createMetadataColumn();
				column.setTalendType(xmlNode.getType());
				column.setLabel(nameWithoutPrefixForColumn);
				ConnectionHelper.getTables(connection).toArray(
						new MetadataTable[0])[0].getColumns().add(column);
			}
			break;
		case ATreeNode.NAMESPACE_TYPE:
			xmlNode.setAttribute("ns");
			// specific for namespace... no path set, there is only the prefix
			// value.
			// this value is saved now in node.getDataType()
			xmlNode.setXMLPath(node.getDataType());

			xmlNode.setDefaultValue((String) node.getValue());
			break;
		case ATreeNode.OTHER_TYPE:
			break;
		}
		// will try to get the first element (branch or main), and set it as
		// loop.
		if (!loopElementFound && path.split("/").length == 2
				&& node.getType() == ATreeNode.ELEMENT_TYPE) {
			connection.getLoop().add(xmlNode);

			for (XMLFileNode curNode : connection.getRoot()) {
				if (curNode.getXMLPath().startsWith(path)) {
					curNode.setAttribute("main");
				}
			}
			xmlNode.setAttribute("main");
			loopElementFound = true;
		} else {
			connection.getRoot().add(xmlNode);
		}
		if (node.getChildren().length > 0) {
			for (Object curNode : node.getChildren()) {
				fillRootInfo(connection, (ATreeNode) curNode,
						path + "/" + node.getValue());
			}
		}
	}
	// TESB-3668 end

	private boolean updateConnection() {
        if (currentPortName != null) {
            connection.setPortName(currentPortName);
        } else if (currentPortName == null && !allPortNames.isEmpty()) {
            currentPortName = allPortNames.get(0);
            connection.setPortName(currentPortName);
        } else {
            return false;
        }
        return true;
    }

    public void saveOutPutValue() {
        // save output
        EList<WSDLParameter> outPutValue = connection.getOutputParameter();

        List<ParameterInfo> ls = new ArrayList<ParameterInfo>();
        IElementParameter OUTPUT_PARAMSPara = connector.getElementParameter("OUTPUT_PARAMS");
        List<Map<String, String>> outputMap = (List<Map<String, String>>) OUTPUT_PARAMSPara.getValue();
        if (outputMap != null) {
            outPutValue.clear();
            if (currentFunction != null) {
                ParameterInfo outputParameter = currentFunction.getOutput().getParameterRoot();
                if (outputParameter != null) {
                    // for (int i = 0; i < inputParameter.size(); i++) {
                    boolean mark = true;
                    WSDLParameter parameter = ConnectionFactory.eINSTANCE.createWSDLParameter();
                    parameter.setParameterInfo(outputParameter.getName());
                    if (outputParameter.getParent() == null) {
                        parameter.setParameterInfoParent("");
                    } else {
                        parameter.setParameterInfoParent(outputParameter.getParent().getName());
                    }
                    outPutValue.add(parameter);
                    // System.out.println(element.getParent() + " ppp");
                    mark = false;
                    if (!outputParameter.getParameterInfos().isEmpty()) {
                        ls.addAll(new ParameterInfoUtil().getAllChildren(outputParameter));
                    }
                    if (!mark) {
                        for (ParameterInfo para : ls) {
                            WSDLParameter parameter1 = ConnectionFactory.eINSTANCE.createWSDLParameter();
                            parameter1.setParameterInfo(para.getName());
                            parameter1.setParameterInfoParent(para.getParent().getName());
                            outPutValue.add(parameter1);
                        }
                    }
                }
            }
        }
    }

    
    /**
     * @param okButton the wizardOkButton to set
     */
    public void setWizardOkButton(Button okButton) {
        this.wizardOkButton = okButton;
        setOk(false);
    }
    
	class OnlyShowServicesFilter extends ViewerFilter {

		private ERepositoryObjectType SERVICES = (ERepositoryObjectType) ERepositoryObjectType
				.valueOf(ERepositoryObjectType.class, "SERVICES"); // see
																	// ESBRepositoryNodeType.SERVICES

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof RepositoryNode)) {
				return false;
			}
			RepositoryNode node = (RepositoryNode) element;
			if (node.getType() != ENodeType.REPOSITORY_ELEMENT
					|| node.getProperties(EProperties.CONTENT_TYPE) != SERVICES) {
				return false;
			}
			if (node.getObject() != null
					&& ProxyRepositoryFactory.getInstance().getStatus(
							node.getObject()) == ERepositoryStatus.DELETED) {
				return false;
			}
			return true;
		}
	}

	/*
	 * [TESB-3653] more detail please refer to
	 * org.talend.repository.services.utils
	 * .FolderNameUtil.replaceAllLimited(String) but, since it will cause the
	 * github hudson build failed so just copy it here
	 */
	private String replaceAllLimited(String input) {
		if (input == null) {
			return input;
		}
		String[] split = input.split("/");
		if (split.length <= 1) {
			// return input;
			split = new String[] { input };
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < split.length; i++) {
			String replaceAll = split[i].replaceAll("\\p{Punct}", "-");
			sb.append(replaceAll);
			if (i < split.length - 1) {
				sb.append("/");
			}
		}
		return sb.toString();
	}
}
