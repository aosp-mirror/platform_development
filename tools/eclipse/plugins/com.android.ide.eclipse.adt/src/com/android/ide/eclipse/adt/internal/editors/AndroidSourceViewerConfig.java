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

package com.android.ide.eclipse.adt.internal.editors;


import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.IInputProvider;
import org.eclipse.wst.sse.core.text.IStructuredPartitions;
import org.eclipse.wst.xml.core.text.IXMLPartitions;
import org.eclipse.wst.xml.ui.StructuredTextViewerConfigurationXML;

import java.util.ArrayList;

/**
 * Base Source Viewer Configuration for Android resources.
 */
public class AndroidSourceViewerConfig extends StructuredTextViewerConfigurationXML {

    /** Content Assist Processor to use for all handled partitions. */
    private IContentAssistProcessor mProcessor;

    public AndroidSourceViewerConfig(IContentAssistProcessor processor) {
        super();
        mProcessor = processor;
    }
    
    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        return super.getContentAssistant(sourceViewer);
    }

    /**
     * Returns the content assist processors that will be used for content
     * assist in the given source viewer and for the given partition type.
     * 
     * @param sourceViewer the source viewer to be configured by this
     *        configuration
     * @param partitionType the partition type for which the content assist
     *        processors are applicable
     * @return IContentAssistProcessors or null if should not be supported
     */
    @Override
    protected IContentAssistProcessor[] getContentAssistProcessors(
            ISourceViewer sourceViewer, String partitionType) {
        ArrayList<IContentAssistProcessor> processors = new ArrayList<IContentAssistProcessor>();
        if (partitionType == IStructuredPartitions.UNKNOWN_PARTITION ||
            partitionType == IStructuredPartitions.DEFAULT_PARTITION ||
            partitionType == IXMLPartitions.XML_DEFAULT) {
            if (sourceViewer instanceof IInputProvider) {
                IInputProvider input = (IInputProvider) sourceViewer;
                Object a = input.getInput();
                if (a != null)
                    a.toString();
            }

            IDocument doc = sourceViewer.getDocument();
            if (doc != null)
                doc.toString();
            
            processors.add(mProcessor);
        }

        IContentAssistProcessor[] others = super.getContentAssistProcessors(sourceViewer,
                partitionType);
        if (others != null && others.length > 0) {
            for (IContentAssistProcessor p : others) {
                processors.add(p);
            }
        }
        
        if (processors.size() > 0) {
            return processors.toArray(new IContentAssistProcessor[processors.size()]);
        } else {
            return null;
        }
    }
    
    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        // TODO text hover for android xml
        return super.getTextHover(sourceViewer, contentType);
    }

    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(
            ISourceViewer sourceViewer, String contentType) {
        // TODO auto edit strategies for android xml
        return super.getAutoEditStrategies(sourceViewer, contentType);
    }
    
    @Override
    public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
        // TODO content formatter for android xml
        return super.getContentFormatter(sourceViewer);
    }
}
