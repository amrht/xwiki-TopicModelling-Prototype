package com.acme.internal;

import javax.inject.Named;


import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Instance;
import cc.mallet.topics.ParallelTopicModel;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import javax.inject.*;
import java.util.Collections;
import java.util.HashMap;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import com.acme.ExampleMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;

/**
 * Example Macro.
 */
@Component
@Named("lda")
@Singleton
public class ExampleMacro extends AbstractMacro<ExampleMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "This implements the LDA on given parametre";
    
    /**
     * Create and initialize the descriptor of the macro.
     */


   @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private DocumentModelBridge documentModelBridge; 


    public ExampleMacro()
    {
        super("This Provides LDA Topic Modelling", DESCRIPTION, ExampleMacroParameters.class);
    }

    @Override
public List<Block> execute(ExampleMacroParameters parameters, String content, MacroTransformationContext context)
    throws MacroExecutionException
{


    DocumentReference documentReference = parameters.getparameters();
    // DocumentReference docRef = resolver.resolve(parameters.getparameters().getDocumentReference(), documentReference, EntityType.DOCUMENT);
    
    

    DocumentModelBridge documentBridge;
        try {
            documentBridge = this.documentAccessBridge.getDocument(documentReference);
        } catch (Exception e) {
            throw new MacroExecutionException(
                "Failed to load Document",
                e);
        }

    String docContent = documentBridge.getContent();
    ArrayList<String> contentList = new ArrayList<String>();
    contentList.add(docContent);
    ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
    pipeList.add(new CharSequenceLowercase());
    pipeList.add(new CharSequence2TokenSequence());
    pipeList.add(new TokenSequenceRemoveStopwords());
    pipeList.add(new TokenSequence2FeatureSequence());


    InstanceList instances = new InstanceList(new SerialPipes(pipeList));


    for (String text : contentList) {
        instances.addThruPipe(new Instance(text, null, null, null));
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
    try {
        model.estimate();
    } catch (IOException e) {
        e.printStackTrace();
    }
    

   String[][] topWords = (String[][]) model.getTopWords(10);
StringBuilder sb = new StringBuilder();
for (int topic = 0; topic < model.getNumTopics(); topic++) {
    sb.append("Topic " + topic + ":\n");
    for (int rank = 0; rank < 10; rank++) {
        sb.append("  " + (rank + 1) + ". " + topWords[topic][rank] + "\n");
    }
    sb.append("\n");
}

// Create a new ParagraphBlock with the content of the StringBuilder
ParagraphBlock paragraph = new ParagraphBlock(
    Collections.singletonList((Block) new WordBlock(sb.toString())),
    new HashMap<String, String>()
);

// Add the paragraph block to the result
List<Block> blocks = new ArrayList<Block>();
blocks.add(paragraph);

return blocks;

}

@Override
public boolean supportsInlineMode() {
    return false;
}

}
