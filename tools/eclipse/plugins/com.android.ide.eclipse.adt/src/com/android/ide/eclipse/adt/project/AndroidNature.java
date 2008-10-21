/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.project;

import com.android.ide.eclipse.adt.build.ApkBuilder;
import com.android.ide.eclipse.adt.build.PreCompilerBuilder;
import com.android.ide.eclipse.adt.build.ResourceManagerBuilder;
import com.android.ide.eclipse.common.AndroidConstants;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

/**
 * Project nature for the Android Projects.
 */
public class AndroidNature implements IProjectNature {

    /** the project this nature object is associated with */
    private IProject mProject;

    /**
     * Configures this nature for its project. This is called by the workspace
     * when natures are added to the project using
     * <code>IProject.setDescription</code> and should not be called directly
     * by clients. The nature extension id is added to the list of natures
     * before this method is called, and need not be added here.
     *
     * Exceptions thrown by this method will be propagated back to the caller of
     * <code>IProject.setDescription</code>, but the nature will remain in
     * the project description.
     *
     * The Android nature adds the pre-builder and the APK builder if necessary.
     *
     * @see org.eclipse.core.resources.IProjectNature#configure()
     * @throws CoreException if configuration fails.
     */
    public void configure() throws CoreException {
        configureResourceManagerBuilder(mProject);
        configurePreBuilder(mProject);
        configureApkBuilder(mProject);
    }

    /**
     * De-configures this nature for its project. This is called by the
     * workspace when natures are removed from the project using
     * <code>IProject.setDescription</code> and should not be called directly
     * by clients. The nature extension id is removed from the list of natures
     * before this method is called, and need not be removed here.
     *
     * Exceptions thrown by this method will be propagated back to the caller of
     * <code>IProject.setDescription</code>, but the nature will still be
     * removed from the project description.
     *
     * The Android nature removes the custom pre builder and APK builder.
     *
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     * @throws CoreException if configuration fails.
     */
    public void deconfigure() throws CoreException {
        // remove the android builders
        removeBuilder(mProject, ResourceManagerBuilder.ID);
        removeBuilder(mProject, PreCompilerBuilder.ID);
        removeBuilder(mProject, ApkBuilder.ID);
    }

    /**
     * Returns the project to which this project nature applies.
     *
     * @return the project handle
     * @see org.eclipse.core.resources.IProjectNature#getProject()
     */
    public IProject getProject() {
        return mProject;
    }

    /**
     * Sets the project to which this nature applies. Used when instantiating
     * this project nature runtime. This is called by
     * <code>IProject.create()</code> or
     * <code>IProject.setDescription()</code> and should not be called
     * directly by clients.
     *
     * @param project the project to which this nature applies
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    public void setProject(IProject project) {
        mProject = project;
    }

    /**
     * Adds the Android Nature and the Java Nature to the project if it doesn't
     * already have them.
     *
     * @param project An existing or new project to update
     * @param monitor An optional progress monitor. Can be null.
     * @throws CoreException if fails to change the nature.
     */
    public static synchronized void setupProjectNatures(IProject project,
            IProgressMonitor monitor) throws CoreException {
        if (project == null || !project.isOpen()) return;
        if (monitor == null) monitor = new NullProgressMonitor();

        // Add the natures. We need to add the Java nature first, so it adds its builder to the
        // project first. This way, when the android nature is added, we can control where to put
        // the android builders in relation to the java builder.
        // Adding the java nature after the android one, would place the java builder before the
        // android builders.
        addNatureToProjectDescription(project, JavaCore.NATURE_ID, monitor);
        addNatureToProjectDescription(project, AndroidConstants.NATURE, monitor);
    }

    /**
     * Add the specified nature to the specified project. The nature is only
     * added if not already present.
     * <p/>
     * Android Natures are always inserted at the beginning of the list of natures in order to
     * have the jdt views/dialogs display the proper icon.
     *
     * @param project The project to modify.
     * @param natureId The Id of the nature to add.
     * @param monitor An existing progress monitor.
     * @throws CoreException if fails to change the nature.
     */
    private static void addNatureToProjectDescription(IProject project,
            String natureId, IProgressMonitor monitor) throws CoreException {
        if (!project.hasNature(natureId)) {

            IProjectDescription description = project.getDescription();
            String[] natures = description.getNatureIds();
            String[] newNatures = new String[natures.length + 1];
            
            // Android natures always come first.
            if (natureId.equals(AndroidConstants.NATURE)) {
                System.arraycopy(natures, 0, newNatures, 1, natures.length);
                newNatures[0] = natureId;
            } else {
                System.arraycopy(natures, 0, newNatures, 0, natures.length);
                newNatures[natures.length] = natureId;
            }
            
            description.setNatureIds(newNatures);
            project.setDescription(description, new SubProgressMonitor(monitor, 10));
        }
    }

    /**
     * Adds the ResourceManagerBuilder, if its not already there. It'll insert
     * itself as the first builder.
     * @throws CoreException
     *
     */
    public static void configureResourceManagerBuilder(IProject project)
            throws CoreException {
        // get the builder list
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        // look for the builder in case it's already there.
        for (int i = 0; i < commands.length; ++i) {
            if (ResourceManagerBuilder.ID.equals(commands[i].getBuilderName())) {
                return;
            }
        }

        // it's not there, lets add it at the beginning of the builders
        ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 1, commands.length);
        ICommand command = desc.newCommand();
        command.setBuilderName(ResourceManagerBuilder.ID);
        newCommands[0] = command;
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }

    /**
     * Adds the PreCompilerBuilder if its not already there. It'll check for
     * presence of the ResourceManager and insert itself right after.
     * @param project
     * @throws CoreException
     */
    public static void configurePreBuilder(IProject project)
            throws CoreException {
        // get the builder list
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        // look for the builder in case it's already there.
        for (int i = 0; i < commands.length; ++i) {
            if (PreCompilerBuilder.ID.equals(commands[i].getBuilderName())) {
                return;
            }
        }

        // we need to add it after the resource manager builder.
        // Let's look for it
        int index = -1;
        for (int i = 0; i < commands.length; ++i) {
            if (ResourceManagerBuilder.ID.equals(commands[i].getBuilderName())) {
                index = i;
                break;
            }
        }

        // we're inserting after
        index++;

        // do the insertion

        // copy the builders before.
        ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, index);

        // insert the new builder
        ICommand command = desc.newCommand();
        command.setBuilderName(PreCompilerBuilder.ID);
        newCommands[index] = command;

        // copy the builder after
        System.arraycopy(commands, index, newCommands, index + 1, commands.length-index);

        // set the new builders in the project
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }

    public static void configureApkBuilder(IProject project)
            throws CoreException {
        // Add the .apk builder at the end if it's not already there
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        for (int i = 0; i < commands.length; ++i) {
            if (ApkBuilder.ID.equals(commands[i].getBuilderName())) {
                return;
            }
        }

        ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        ICommand command = desc.newCommand();
        command.setBuilderName(ApkBuilder.ID);
        newCommands[commands.length] = command;
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }

    /**
     * Removes a builder from the project.
     * @param project The project to remove the builder from.
     * @param id The String ID of the builder to remove.
     * @return true if the builder was found and removed.
     * @throws CoreException
     */
    public static boolean removeBuilder(IProject project, String id) throws CoreException {
        IProjectDescription description = project.getDescription();
        ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i) {
            if (id.equals(commands[i].getBuilderName())) {
                ICommand[] newCommands = new ICommand[commands.length - 1];
                System.arraycopy(commands, 0, newCommands, 0, i);
                System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                description.setBuildSpec(newCommands);
                project.setDescription(description, null);
                return true;
            }
        }

        return false;
    }
}
