// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.camel.designer.build;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.talend.camel.designer.ui.editor.RouteProcess;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.CommonUIPlugin;
import org.talend.commons.utils.MojoType;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.process.JobInfo;
import org.talend.core.model.process.ProcessUtils;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.runtime.maven.MavenConstants;
import org.talend.core.runtime.process.LastGenerationInfo;
import org.talend.core.runtime.process.TalendProcessArgumentConstant;
import org.talend.core.runtime.projectsetting.IProjectSettingPreferenceConstants;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.designer.core.IDesignerCoreService;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.tools.AggregatorPomsHelper;
import org.talend.designer.maven.tools.creator.CreateMavenJobPom;
import org.talend.designer.maven.utils.PomIdsHelper;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.designer.runprocess.ItemCacheManager;
import org.talend.repository.ProjectManager;
import org.talend.utils.io.FilesUtils;

/**
 * Route pom creator
 */
public class CreateMavenBundlePom extends CreateMavenJobPom {
    
    private static final String PATH_FEATURE = "${basedir}/src/main/bundle-resources/feature.xml";
    
    private static final String PROJECT_VERSION = "${project.version}";

    private static final String JOB_FINAL_NAME = "talend.job.finalName";

    private static final String PATH_ROUTES = "resources/templates/karaf/routes/";
    
    private static final String MAVEN_CORE_VERSION = "3.8.8";

    private Model bundleModel;

    /**
     * sunchaoqun CreateMavenCamelPom constructor comment.
     *
     * @param jobProcessor
     * @param pomFile
     */
    public CreateMavenBundlePom(IProcessor jobProcessor, IFile pomFile) {
        super(jobProcessor, pomFile);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.talend.designer.maven.tools.creator.CreateMavenBundleTemplatePom#create(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    public void create(IProgressMonitor monitor) throws Exception {

        IFile curPomFile = getPomFile();

        if (curPomFile == null) {
            return;
        }

        bundleModel = createModel();
        // patch for TESB-23953: find "tdm-lib-di-" and remove in route, only keep 'tdm-camel'
        boolean containsTdmCamelDependency = false;
        Dependency tdmDIDependency = null;
        Dependency logbackClassicDependency = null;
        Dependency logbackCoreDependency = null;
        List<Dependency> dependencies = bundleModel.getDependencies();
        for (int i = 0; i < dependencies.size(); i++) {
            String artifactId = dependencies.get(i).getArtifactId();
            if (artifactId.startsWith("tdm-lib-di-")) {
                tdmDIDependency = dependencies.get(i);
            }
            if (artifactId.startsWith("tdm-camel-")) {
                containsTdmCamelDependency = true;
            }
            if(artifactId.equals("logback-classic")) {
            	logbackClassicDependency = dependencies.get(i);
            }
            if(artifactId.equals("logback-core")) {
            	logbackCoreDependency = dependencies.get(i);
            }
        }
        if (containsTdmCamelDependency && tdmDIDependency != null) {
            bundleModel.getDependencies().remove(tdmDIDependency);
        }
        if (logbackClassicDependency != null) {
            bundleModel.getDependencies().remove(logbackClassicDependency);
        }
        if (logbackCoreDependency != null) {
        	bundleModel.getDependencies().remove(logbackCoreDependency);
        }

        IContainer parent = curPomFile.getParent();

        Model pom = new Model();

        boolean isRoute = "CAMEL".equals(getJobProcessor().getProcess().getComponentsType())
                && ERepositoryObjectType.getType(getJobProcessor().getProperty()).equals(ERepositoryObjectType.PROCESS_ROUTE);
        
        boolean isRoutelet = "CAMEL".equals(getJobProcessor().getProcess().getComponentsType())
                && ERepositoryObjectType.getType(getJobProcessor().getProperty()).equals(ERepositoryObjectType.PROCESS_ROUTELET);
        

        Parent parentPom = new Parent();
        parentPom.setGroupId(bundleModel.getGroupId());
        parentPom.setArtifactId(bundleModel.getArtifactId() + "-Kar");
        parentPom.setVersion(bundleModel.getVersion());
        parentPom.setRelativePath("/");


        
        if (isRoute) {

            RouteProcess routeProcess = (RouteProcess) getJobProcessor().getProcess();

            boolean publishAsSnapshot = BooleanUtils
                    .toBoolean((String) routeProcess.getAdditionalProperties().get(MavenConstants.NAME_PUBLISH_AS_SNAPSHOT));

            File featurePom = new File(parent.getLocation().toOSString() + File.separator + "pom-feature.xml");

            Model featureModel = new Model();

            featureModel.setModelVersion("4.0.0");
            featureModel.setParent(parentPom);
            featureModel.setGroupId(bundleModel.getGroupId());
            featureModel.setArtifactId(bundleModel.getArtifactId() + "-feature");

            featureModel.setName(bundleModel.getName() + " Feature");

            featureModel.setVersion(bundleModel.getVersion());
            featureModel.setPackaging("pom");

            featureModel.setProperties(bundleModel.getProperties());
            featureModel.addProperty("cloud.publisher.skip", "false");
            Build featureModelBuild = new Build();


            List<Profile> profiles = new ArrayList<Profile>();

            Profile profile = new Profile();
            profile.setId("kar-publisher");

            Activation activation = new Activation();
            activation.setActiveByDefault(false);

            ActivationProperty property = new ActivationProperty();
            property.setName("!altDeploymentRepository");
            activation.setProperty(property);
            profile.setActivation(activation);

            BuildBase buildBase = new BuildBase();
            buildBase.addPlugin(addFeaturesMavenPlugin(bundleModel.getProperties().getProperty("talend.job.finalName")));
            profile.setBuild(buildBase);

            profiles.add(profile);

            featureModel.setProfiles(profiles);
            featureModelBuild.addPlugin(addOsgiHelperMavenPlugin(true, CommonUIPlugin.isFullyHeadless(), false));
            
            // featureModelBuild.addPlugin(addDeployFeatureMavenPlugin(featureModel.getArtifactId(), featureModel.getVersion(), publishAsSnapshot));
//            featureModelBuild.addPlugin(addSkipDeployFeatureMavenPlugin());
            featureModelBuild.addPlugin(addSkipMavenCleanPlugin());
            featureModelBuild.addPlugin(addSkipDockerMavenPlugin());
            featureModel.setBuild(featureModelBuild);
//            featureModel.addProfile(addProfileForNexus(publishAsSnapshot, featureModel));
            PomUtil.savePom(monitor, featureModel, featurePom);
        }

        pom.setModelVersion("4.0.0");
        pom.setParent(bundleModel.getParent());
        pom.setGroupId(bundleModel.getGroupId());
        pom.setArtifactId(bundleModel.getArtifactId() + "-Kar");
        pom.setName(bundleModel.getName() + " Kar");
        pom.setVersion(bundleModel.getVersion());
        pom.setPackaging("pom");

        pom.addModule("pom-bundle.xml");
        if (isRoute) {
            pom.addModule("pom-feature.xml");
        }
        pom.setDependencies(bundleModel.getDependencies());

        if (pom.getBuild() == null) {
            pom.setBuild(new Build());
        }
        pom.addProfile(addProfileForCloud());

        File pomBundle = new File(parent.getLocation().toOSString() + File.separator + "pom-bundle.xml");
        bundleModel.addProperty(JOB_FINAL_NAME, "${talend.job.name}-${project.version}");

        bundleModel.addProperty("cloud.publisher.skip", "true");
        bundleModel.setParent(parentPom);
        bundleModel.setName(bundleModel.getName() + " Bundle");

        if (bundleModel.getBuild() == null) {
            bundleModel.setBuild(new Build());
        }

        bundleModel.getBuild().addPlugin(addSkipDockerMavenPlugin());
        if (isRoutelet && CommonUIPlugin.isFullyHeadless()) {
        	bundleModel.getBuild().addPlugin(addOsgiHelperMavenPlugin(false, false, true));
        }
      
        updateBundleMainfest(bundleModel);

        PomUtil.savePom(monitor, bundleModel, pomBundle);

        PomUtil.savePom(monitor, pom, curPomFile);

        parent.refreshLocal(IResource.DEPTH_ONE, monitor);

        afterCreate(monitor);
    }

    protected void updateBundleMainfest(Model bundleModel) {
        // do nothing for route
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addChildrenDependencies(final List<Dependency> dependencies) {
        String parentId = getJobProcessor().getProperty().getId();
        String parentBuildType = (String) getJobProcessor().getProperty().getAdditionalProperties().get(TalendProcessArgumentConstant.ARG_BUILD_TYPE);
        final Set<JobInfo> clonedChildrenJobInfors = getJobProcessor().getBuildFirstChildrenJobs();
        for (JobInfo jobInfo : clonedChildrenJobInfors) {
            if (jobInfo.getFatherJobInfo() != null && jobInfo.getFatherJobInfo().getJobId().equals(parentId)) {
                if (!validChildrenJob(jobInfo)) {
                    continue;
                }
                Property property;
                String groupId;
                String artifactId;
                String version;
                String type = null;
                String buildType = null;
                if (!jobInfo.isJoblet()) {
                    property = jobInfo.getProcessItem().getProperty();
                    if ((isJob(jobInfo) && ProcessUtils.isChildRouteProcess(getProcessor(jobInfo).getProcess())) || (isRoutelet(jobInfo) && CommonUIPlugin.isFullyHeadless()))  {
                        String parentRouteID = PomIdsHelper.getJobArtifactId(getJobProcessor().getProperty());
                    	String parentRouteVersion = getJobProcessor().getProperty().getVersion().replace(".", "_");
                    	String prefix = CommonUIPlugin.isFullyHeadless()? parentRouteID + "_" + parentRouteVersion + "_" : "";
                        artifactId = prefix + PomIdsHelper.getJobArtifactId(jobInfo);
                        groupId = PomIdsHelper.getJobGroupId(getJobProcessor().getProperty());     
                        version = PomIdsHelper.getJobVersion(getJobProcessor().getProperty());
                    }else {
                        artifactId = PomIdsHelper.getJobArtifactId(jobInfo);
                        groupId = PomIdsHelper.getJobGroupId(property);     
                        version = PomIdsHelper.getJobVersion(property);                    	
                    }
                    // try to get the pom version of children job and load from the pom file.
                    String childPomFileName = PomUtil.getPomFileName(jobInfo.getJobName(), jobInfo.getJobVersion());
                    IProject codeProject = getJobProcessor().getCodeProject();
                    if (codeProject != null) {
                        try {
                            codeProject.refreshLocal(IResource.DEPTH_ONE, null); // is it ok or needed here ???
                        } catch (CoreException e) {
                            ExceptionHandler.process(e);
                        }
                        IFile childPomFile = codeProject.getFile(new Path(childPomFileName));
                        if (childPomFile.exists()) {
                            try {
                                Model childModel = MODEL_MANAGER.readMavenModel(childPomFile);
                                // try to get the real groupId, artifactId, version.
                                groupId = childModel.getGroupId();
                                artifactId = childModel.getArtifactId();
                                version = childModel.getVersion();
                            } catch (CoreException e) {
                                ExceptionHandler.process(e);
                            }
                        }
                    }
                } else {
                    property = jobInfo.getJobletProperty();
                    groupId = PomIdsHelper.getJobletGroupId(property);
                    artifactId = PomIdsHelper.getJobletArtifactId(property);
                    version = PomIdsHelper.getJobletVersion(property);
                    type = MavenConstants.PACKAGING_POM;
                }
                if(property != null) {
                    buildType = (String) property.getAdditionalProperties().get(TalendProcessArgumentConstant.ARG_BUILD_TYPE);
                    
                    if (buildType== null && "ROUTE".equalsIgnoreCase(parentBuildType)){
                    	property.getAdditionalProperties().put(TalendProcessArgumentConstant.ARG_BUILD_TYPE, "OSGI");
                    }
                }

                JobInfo mainJobInfo = LastGenerationInfo.getInstance().getLastMainJob();

                boolean needOSGIProcessor = true;

                if (mainJobInfo != null && mainJobInfo.getJobId().equals(property.getId())) {
                    needOSGIProcessor = false;
                }

				Dependency d = PomUtil.createDependency(groupId, artifactId, version, type);
                dependencies.add(d);
            }
        }
    }

    protected void generateAssemblyFile(IProgressMonitor monitor, final Set<JobInfo> clonedChildrenJobInfors) throws Exception {
        IFile assemblyFile = this.getAssemblyFile();
        if (assemblyFile != null) {
            // read template from project setting
            try {
                File templateFile = PomUtil.getTemplateFile(getObjectTypeFolder(), getItemRelativePath(),
                        TalendMavenConstants.ASSEMBLY_FILE_NAME);
                if (!FilesUtils.allInSameFolder(templateFile, TalendMavenConstants.POM_FILE_NAME)) {
                    templateFile = null; // force to set null, in order to use the template from other places.
                }

                final Map<String, Object> templateParameters = PomUtil.getTemplateParameters(getJobProcessor());
                String content = MavenTemplateManager.getTemplateContent(templateFile,
                        IProjectSettingPreferenceConstants.TEMPLATE_ROUTE_ASSEMBLY, JOB_TEMPLATE_BUNDLE,
                        IProjectSettingTemplateConstants.PATH_OSGI_BUNDLE + '/'
                                + IProjectSettingTemplateConstants.ASSEMBLY_ROUTE_TEMPLATE_FILE_NAME,
                        templateParameters);
                if (content != null) {
                    ByteArrayInputStream source = new ByteArrayInputStream(content.getBytes());
                    if (assemblyFile.exists()) {
                        assemblyFile.setContents(source, true, false, monitor);
                    } else {
                        assemblyFile.create(source, true, monitor);
                    }
                    updateDependencySet(assemblyFile);
                }
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
        }
    }

    /**
     * skip depoly phase in publich to cloud in parent pom, enable in nexus.
     */
    private Profile addProfileForCloud() {
        Profile deployCloudProfile = new Profile();
        deployCloudProfile.setId("deploy-cloud");
        Activation deployCloudActivation = new Activation();
        ActivationProperty activationProperty = new ActivationProperty();
        activationProperty.setName("!altDeploymentRepository");
        deployCloudActivation.setProperty(activationProperty);
        deployCloudProfile.setActivation(deployCloudActivation);
        deployCloudProfile.setBuild(new Build());
        deployCloudProfile.getBuild().addPlugin(addSkipDeployFeatureMavenPlugin());
        return deployCloudProfile;
    }

    private Plugin addFeaturesMavenPlugin(String finalNameValue) {
        Plugin plugin = new Plugin();

        plugin.setGroupId("org.apache.karaf.tooling");
        plugin.setArtifactId("karaf-maven-plugin");
        plugin.setVersion("4.2.10");

        Xpp3Dom configuration = new Xpp3Dom("configuration");

        Xpp3Dom finalName = new Xpp3Dom("finalName");

        finalName.setValue(finalNameValue);// "${talend.job.finalName}"

        Xpp3Dom resourcesDir = new Xpp3Dom("resourcesDir");
        resourcesDir.setValue("${project.build.directory}/bin");

        Xpp3Dom featuresFile = new Xpp3Dom("featuresFile");
        featuresFile.setValue(PATH_FEATURE);

        configuration.addChild(finalName);
        configuration.addChild(resourcesDir);
        configuration.addChild(featuresFile);

        List<PluginExecution> pluginExecutions = new ArrayList<PluginExecution>();
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("create-kar");
        pluginExecution.addGoal("kar");
        pluginExecution.setConfiguration(configuration);

        pluginExecutions.add(pluginExecution);
        plugin.setExecutions(pluginExecutions);

        List<Dependency> dependencies = new ArrayList<Dependency>();
        Dependency mavensharedDep = new Dependency();
        mavensharedDep.setGroupId("org.apache.maven.shared");
        mavensharedDep.setArtifactId("maven-shared-utils");
        mavensharedDep.setVersion("3.3.3");

        Dependency commonsioDep = new Dependency();
        commonsioDep.setGroupId("commons-io");
        commonsioDep.setArtifactId("commons-io");
        commonsioDep.setVersion("2.8.0");

        Dependency httpclientDep = new Dependency();
        httpclientDep.setGroupId("org.apache.httpcomponents");
        httpclientDep.setArtifactId("httpclient");
        httpclientDep.setVersion("4.5.13");

        Dependency httpcoreDep = new Dependency();
        httpcoreDep.setGroupId("org.apache.httpcomponents");
        httpcoreDep.setArtifactId("httpcore");
        httpcoreDep.setVersion("4.4.13");

        Dependency istackDep = new Dependency();
        istackDep.setGroupId("com.sun.istack");
        istackDep.setArtifactId("istack-commons-runtime");
        istackDep.setVersion("3.0.10");

        Dependency xzDep = new Dependency();
        xzDep.setGroupId("org.tukaani");
        xzDep.setArtifactId("xz");
        xzDep.setVersion("1.8");

        Dependency junitDep = new Dependency();
        junitDep.setGroupId("junit");
        junitDep.setArtifactId("junit");
        junitDep.setVersion("4.13.2");
        
        Dependency mavenCoreDep = new Dependency();
        mavenCoreDep.setGroupId("org.apache.maven");
        mavenCoreDep.setArtifactId("maven-core");
        mavenCoreDep.setVersion(MAVEN_CORE_VERSION);

        Dependency mavenCompatDep = new Dependency();
        mavenCompatDep.setGroupId("org.apache.maven");
        mavenCompatDep.setArtifactId("maven-compat");
        mavenCompatDep.setVersion(MAVEN_CORE_VERSION);

        Dependency mavenSettingsDep = new Dependency();
        mavenSettingsDep.setGroupId("org.apache.maven");
        mavenSettingsDep.setArtifactId("maven-settings");
        mavenSettingsDep.setVersion(MAVEN_CORE_VERSION);

        Dependency mavenSettingsBdDep = new Dependency();
        mavenSettingsBdDep.setGroupId("org.apache.maven");
        mavenSettingsBdDep.setArtifactId("maven-settings-builder");
        mavenSettingsBdDep.setVersion(MAVEN_CORE_VERSION);

        Dependency plexusArchiverDep = new Dependency();
        plexusArchiverDep.setGroupId("org.codehaus.plexus");
        plexusArchiverDep.setArtifactId("plexus-archiver");
        plexusArchiverDep.setVersion("4.8.0");

        Dependency commonsCompressDep = new Dependency();
        commonsCompressDep.setGroupId("org.apache.commons");
        commonsCompressDep.setArtifactId("commons-compress");
        commonsCompressDep.setVersion("1.21");

        Dependency jsoupDep = new Dependency();
        jsoupDep.setGroupId("org.jsoup");
        jsoupDep.setArtifactId("jsoup");
        jsoupDep.setVersion("1.15.3");

        Dependency mavenModelDep = new Dependency();
        mavenModelDep.setGroupId("org.apache.maven");
        mavenModelDep.setArtifactId("maven-model");
        mavenModelDep.setVersion(MAVEN_CORE_VERSION);

        Dependency commonsCodecDep = new Dependency();
        commonsCodecDep.setGroupId("commons-codec");
        commonsCodecDep.setArtifactId("commons-codec");
        commonsCodecDep.setVersion("1.15");
        
        Dependency guavaDep = new Dependency();
        guavaDep.setGroupId("com.google.guava");
        guavaDep.setArtifactId("guava");
        guavaDep.setVersion("32.0.1-jre");

        Dependency slf4jDep = new Dependency();
        slf4jDep.setGroupId("org.slf4j");
        slf4jDep.setArtifactId("slf4j-jdk14");
        slf4jDep.setVersion("1.7.34");      
        
        Dependency slf4jJclDep = new Dependency();
        slf4jJclDep.setGroupId("org.slf4j");
        slf4jJclDep.setArtifactId("jcl-over-slf4j");
        slf4jJclDep.setVersion("1.7.34");  
        
        Dependency slf4jApiDep = new Dependency();
        slf4jApiDep.setGroupId("org.slf4j");
        slf4jApiDep.setArtifactId("slf4j-api");
        slf4jApiDep.setVersion("1.7.34");  

        Dependency doxiaDep = new Dependency();
        doxiaDep.setGroupId("org.apache.maven.doxia");
        doxiaDep.setArtifactId("doxia-site-renderer");
        doxiaDep.setVersion("1.0");
        List<Exclusion> exclusionList = new ArrayList<Exclusion>();
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("org.apache.velocity");
        exclusion.setArtifactId("velocity");
        exclusionList.add(exclusion);
        doxiaDep.setExclusions(exclusionList);

        Dependency velocityDep = new Dependency();
        velocityDep.setGroupId("org.apache.velocity");
        velocityDep.setArtifactId("velocity-engine-core");
        velocityDep.setVersion("2.3");
        
        //org.apache.karaf.shell.console---to remove dependency to sshd-osgi, then spring-framwork-bom
        Dependency karafShellConsoleDep = new Dependency();
        karafShellConsoleDep.setGroupId("org.apache.karaf.shell");
        karafShellConsoleDep.setArtifactId("org.apache.karaf.shell.console");
        karafShellConsoleDep.setVersion("4.2.10");
        List<Exclusion> karafShellConsoleExclusionList = new ArrayList<Exclusion>();
        Exclusion sshdExclusion1 = new Exclusion();
        sshdExclusion1.setGroupId("org.apache.sshd");
        sshdExclusion1.setArtifactId("sshd-osgi");
        karafShellConsoleExclusionList.add(sshdExclusion1);
        karafShellConsoleDep.setExclusions(karafShellConsoleExclusionList);
        dependencies.add(karafShellConsoleDep);
        
        //org.apache.karaf.shell.core---to remove dependency to sshd-osgi, then spring-framwork-bom
        Dependency karafShellCoreDep = new Dependency();
        karafShellCoreDep.setGroupId("org.apache.karaf.shell");
        karafShellCoreDep.setArtifactId("org.apache.karaf.shell.core");
        karafShellCoreDep.setVersion("4.2.10");
        List<Exclusion> karafShellCoreExclusionList = new ArrayList<Exclusion>();
        Exclusion sshdExclusion = new Exclusion();
        sshdExclusion.setGroupId("org.apache.sshd");
        sshdExclusion.setArtifactId("sshd-osgi");
        karafShellCoreExclusionList.add(sshdExclusion);
        karafShellCoreDep.setExclusions(karafShellCoreExclusionList);
        dependencies.add(karafShellCoreDep);
        
        dependencies.add(mavensharedDep);
        dependencies.add(commonsioDep);
        dependencies.add(httpclientDep);
        dependencies.add(httpcoreDep);
        dependencies.add(istackDep);
        dependencies.add(xzDep);
        dependencies.add(junitDep);
        dependencies.add(mavenCoreDep);
        dependencies.add(mavenCompatDep);
        dependencies.add(mavenSettingsDep);
        dependencies.add(mavenSettingsBdDep);
        dependencies.add(plexusArchiverDep);
        dependencies.add(commonsCompressDep);
        dependencies.add(jsoupDep);
        dependencies.add(mavenModelDep);
        dependencies.add(commonsCodecDep);
        dependencies.add(guavaDep);
        dependencies.add(slf4jDep);
        dependencies.add(slf4jJclDep);
        dependencies.add(slf4jApiDep);
        dependencies.add(doxiaDep);
        dependencies.add(velocityDep);
        plugin.setDependencies(dependencies);

        return plugin;
    }
    
    private Plugin addOsgiHelperMavenPlugin(boolean setFeatureFile, boolean setSkipChilds, boolean setInstallProject) {
        Plugin plugin = new Plugin();


        plugin.setGroupId(TalendMavenConstants.DEFAULT_CI_GROUP_ID);
        plugin.setArtifactId(MojoType.OSGI_HELPER.getArtifactId());
        plugin.setVersion(VersionUtils.getMojoVersion(MojoType.OSGI_HELPER));

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        
        if (setFeatureFile) {
	        Xpp3Dom featuresFile = new Xpp3Dom("featuresFile");
	        featuresFile.setValue(PATH_FEATURE);
	        configuration.addChild(featuresFile);
        }
        
        if (setSkipChilds) {
            Xpp3Dom skipChilds = new Xpp3Dom("skipChilds");
            skipChilds.setValue("true");
            configuration.addChild(skipChilds);
        }
    
        if (setInstallProject) {
            Xpp3Dom installProject = new Xpp3Dom("installProject");
            installProject.setValue("true");
            configuration.addChild(installProject);
        }
        
        List<PluginExecution> pluginExecutions = new ArrayList<PluginExecution>();
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setId("feature-helper");
        pluginExecution.setPhase("generate-sources");
        pluginExecution.addGoal("generate");
        pluginExecution.setConfiguration(configuration);
        pluginExecutions.add(pluginExecution);
        plugin.setExecutions(pluginExecutions);

        return plugin;
    }



    private Plugin addSkipDeployFeatureMavenPlugin() {

        Plugin plugin = new Plugin();

        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-deploy-plugin");
        plugin.setVersion("2.7");

        Xpp3Dom configuration = new Xpp3Dom("configuration");

        Xpp3Dom skip = new Xpp3Dom("skip");
        skip.setValue("true");
        configuration.addChild(skip);
        plugin.setConfiguration(configuration);

        return plugin;

    }

    private Xpp3Dom createInstallFileElement(JobInfo job) {
        Xpp3Dom file = null;
        if (getJobProcessor() != null && getProcessor(job) != null) {
            IPath currentProjectRootDir = getTalendJobJavaProject(getJobProcessor()).getProject().getLocation();
            IPath targetDir = getTalendJobJavaProject(getProcessor(job)).getTargetFolder().getLocation();
            String relativeTargetDir = targetDir.makeRelativeTo(currentProjectRootDir).toString();

            if (!ProjectManager.getInstance().isInCurrentMainProject(job.getProcessItem().getProperty())) {
                // this job/routelet is from a reference project
                currentProjectRootDir = new Path(currentProjectRootDir.getDevice(),
                        currentProjectRootDir.toString().replaceAll("/\\d+/", "/"));
                targetDir = new Path(targetDir.getDevice(), targetDir.toString().replaceAll("/\\d+/", "/"));
                relativeTargetDir = targetDir.makeRelativeTo(currentProjectRootDir).toString();
            }

            IFile jobPom = AggregatorPomsHelper.getItemPomFolder(job.getProcessItem().getProperty())
                    .getFile(TalendMavenConstants.POM_FILE_NAME);
            if (jobPom.exists()) {
                try {
                    Model jobModel = MODEL_MANAGER.readMavenModel(jobPom);
                    String resolvedFinalName = null;
                    if (jobModel.getProperties().getProperty(JOB_FINAL_NAME) != null) {
                        resolvedFinalName = resolveJobFinalName(jobModel.getProperties().getProperty(JOB_FINAL_NAME), jobModel);
                    } else {
                        for (String modelName : jobModel.getModules()) {
                            IFile subPom = AggregatorPomsHelper.getItemPomFolder(job.getProcessItem().getProperty())
                                    .getFile(modelName);
                            if (subPom.exists()) {
                                Model subModel = MODEL_MANAGER.readMavenModel(subPom);
                                if (subModel.getProperties().getProperty(JOB_FINAL_NAME) != null) {
                                    resolvedFinalName = resolveJobFinalName(subModel.getProperties().getProperty(JOB_FINAL_NAME),
                                            subModel);
                                    break;
                                }
                            }
                        }
                    }
                    if (resolvedFinalName == null) {
                        resolvedFinalName = job.getJobName().toLowerCase() + "_"
                                + PomIdsHelper.getJobVersion(job).replaceAll("\\.", "_");
                    }

                    String pathToJar = relativeTargetDir + Path.SEPARATOR + resolvedFinalName + ".jar";
                    file = new Xpp3Dom("file");
                    file.setValue(pathToJar);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    private String resolveJobFinalName(String finalNameWithToken, Model jobModel) {
        if (finalNameWithToken != null) {
            Pattern p = Pattern.compile("\\$\\{([^\\}]+)\\}");
            Matcher m = p.matcher(finalNameWithToken);
            while (m.find()) {
                if (PROJECT_VERSION.equals(m.group(0))) {
                    finalNameWithToken = finalNameWithToken.replace(PROJECT_VERSION, jobModel.getVersion());
                } else {
                    String propertyValue = jobModel.getProperties().getProperty(m.group(1));
                    if (propertyValue != null) {
                        finalNameWithToken = finalNameWithToken.replace(m.group(0), propertyValue);
                    }
                }
            }
        }
        return finalNameWithToken;
    }

    boolean isRoutelet(JobInfo job) {
        if (job != null && job.getProcessItem() != null) {
            Property p = job.getProcessItem().getProperty();
            if (p != null) {
                return ERepositoryObjectType.getType(p).equals(ERepositoryObjectType.PROCESS_ROUTELET);
            }
        }
        return false;
    }

    boolean isJob(JobInfo job) {
        if (job != null && job.getProcessItem() != null) {
            Property p = job.getProcessItem().getProperty();
            if (p != null) {
                return ERepositoryObjectType.getType(p).equals(ERepositoryObjectType.PROCESS);
            }
        }
        return false;
    }

    public static IProcessor getProcessor(JobInfo jobInfo) {

        if (jobInfo.getProcessor() != null) {
            return jobInfo.getProcessor();
        }

        IProcess process = null;
        ProcessItem processItem;

        processItem = jobInfo.getProcessItem();

        if (processItem == null && jobInfo.getJobVersion() == null) {
            processItem = ItemCacheManager.getProcessItem(jobInfo.getJobId());
        }

        if (processItem == null && jobInfo.getJobVersion() != null) {
            processItem = ItemCacheManager.getProcessItem(jobInfo.getJobId(), jobInfo.getJobVersion());
        }

        if (processItem == null && jobInfo.getProcess() == null) {
            return null;
        }

        if (jobInfo.getProcess() == null) {
            if (processItem != null) {
                IDesignerCoreService service = CorePlugin.getDefault().getDesignerCoreService();
                process = service.getProcessFromProcessItem(processItem);
                if (process instanceof IProcess2) {
                    ((IProcess2) process).setProperty(processItem.getProperty());
                }
            }
            if (process == null) {
                return null;
            }
        } else {
            process = jobInfo.getProcess();
        }

        Property curProperty = processItem.getProperty();
        if (processItem.getProperty() == null && process instanceof IProcess2) {
            curProperty = ((IProcess2) process).getProperty();
        }

        IRunProcessService service = CorePlugin.getDefault().getRunProcessService();
        IProcessor processor = service.createCodeProcessor(process, curProperty,
                ((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY)).getProject()
                        .getLanguage(),
                true);

        jobInfo.setProcessor(processor);

        return processor;
    }

    /**
     * Skip clean control-bundle file in target folde, in case of using mvn clean + package goal
     *
     * @return plugin
     */
    private Plugin addSkipMavenCleanPlugin() {
        Plugin plugin = new Plugin();

        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-clean-plugin");
        plugin.setVersion("3.0.0");

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom skipClean = new Xpp3Dom("skip");
        skipClean.setValue("true");
        configuration.addChild(skipClean);
        plugin.setConfiguration(configuration);

        return plugin;
    }

    @Override
    protected InputStream getTemplateStream() throws IOException {
        File templateFile = PomUtil.getTemplateFile(getObjectTypeFolder(), getItemRelativePath(),
                TalendMavenConstants.POM_FILE_NAME);
        if (!FilesUtils.allInSameFolder(templateFile, TalendMavenConstants.ASSEMBLY_FILE_NAME)) {
            templateFile = null; // force to set null, in order to use the template from other places.
        }
        try {
            final Map<String, Object> templateParameters = PomUtil.getTemplateParameters(getJobProcessor());
            return MavenTemplateManager.getTemplateStream(templateFile,
                    IProjectSettingPreferenceConstants.TEMPLATE_ROUTES_KARAF_BUNDLE, "org.talend.resources.export.route",
                    getBundleTemplatePath(), templateParameters);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    protected String getBundleTemplatePath() {
        return PATH_ROUTES + IProjectSettingTemplateConstants.MAVEN_KARAF_BUILD_BUNDLE_FILE_NAME;
    }

}
