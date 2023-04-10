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
import javax.inject.*;


import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.DocumentReference;
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
    private DocumentModelBridge documentModelBridge;


    public ExampleMacro()
    {
        super("This Provides LDA Topic Modelling", DESCRIPTION, ExampleMacroParameters.class);
    }

    @Override
public List<Block> execute(ExampleMacroParameters parameters, String content, MacroTransformationContext context)
    throws MacroExecutionException
{
    String documentReference = parameters.getparameters().getDocumentReference();
    DocumentReference docRef = new DocumentReference(context.getWikiReference(), documentReference);
    XWikiDocument doc = context.getWiki().getDocument(docRef, context);
    String docContent = doc.getContent();

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
    model.estimate();

    List<Block> result = new ArrayList<Block>();

    // Create a new paragraph block to contain the results
    ParagraphBlock paragraph = new ParagraphBlock();

    // Add a word block containing the number of topics
    WordBlock numTopicsBlock = new WordBlock("Number of topics: " + model.getNumTopics());
    paragraph.add(numTopicsBlock);

    // Add a word block containing the number of iterations
    WordBlock numIterationsBlock = new WordBlock("Number of iterations: " + model.getNumIterations());
    paragraph.add(numIterationsBlock);

    // Add a word block containing the number of threads
    WordBlock numThreadsBlock = new WordBlock("Number of threads: " + model.getNumThreads());
    paragraph.add(numThreadsBlock);

    // Add a word block containing the top words for each topic
    for (int topic = 0; topic < model.getNumTopics(); topic++) {
        WordBlock topicBlock = new WordBlock("Topic " + topic + ": ");
        paragraph.add(topicBlock);
        int[] topicWords = model.getSortedWordsPerTopic(topic);
        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                topicBlock.add(new WordBlock(", "));
            }
            topicBlock.add(new WordBlock(instances.getDataAlphabet().lookupObject(topicWords[i]).toString()));
        }
    }

    // Add the paragraph block to the result
    result.add(paragraph);

    return result;
}

@Override
public boolean supportsInlineMode() {
    return false;
}

}
