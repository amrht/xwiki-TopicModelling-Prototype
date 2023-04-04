/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.acme.internal;

import javax.inject.Named;


import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Instance;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;


import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import com.acme.ExampleMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Example Macro.
 */
@Component
@Named("lda")
public class ExampleMacro extends AbstractMacro<ExampleMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "This implements the LDA on given parametre";
    
    /**
     * Create and initialize the descriptor of the macro.
     */
    public ExampleMacro()
    {
        super("This Provides LDA Topic Modelling", DESCRIPTION, ExampleMacroParameters.class);
    }

    @Override
    public List<Block> execute(ExampleMacroParameters docRef, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String result;

        EntityReference includedReference =
            resolve(docRef.getdocRef());

        String content;
        try {
            content = this.documentModelBridge.getContent(includedReference);
        } catch (Exception e) {
            throw new MacroExecutionException(
                "Failed to load Document",
                e);
        }
        ArrayList<String> contentList = new ArrayList<String>();
        contentList.add(content);
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence());
        pipeList.add(new TokenSequenceRemoveStopwords());
        pipeList.add(new TokenSequence2FeatureSequence());


        InstanceList instances = new InstanceList(new SerialPipes(pipeList));


        for (String content : contentList) {
            instances.addThruPipe(new Instance(content, null, null, null));
        }

        // Set up the LDA topic model parameters
        int numTopics = 10;
        int numIterations = 1000;
        int numThreads = 4;

        // Create the LDA topic model
        ParallelTopicModel model = new ParallelTopicModel(numTopics);
        model.addInstances(instances);
        model.setNumThreads(numThreads);
        model.setNumIterations(numIterations);
        model.estimate();


        return result;
    }
}
