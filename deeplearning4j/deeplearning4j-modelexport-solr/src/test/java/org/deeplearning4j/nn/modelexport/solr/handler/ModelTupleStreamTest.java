/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */
package org.deeplearning4j.nn.modelexport.solr.handler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.expr.Explanation;
import org.apache.solr.client.solrj.io.stream.expr.StreamExplanation;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionParser;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.SolrDefaultStreamFactory;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.core.util.ModelGuesser;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.util.NetworkUtils;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Model Tuple Stream Test")
@Tag(TagNames.SOLR)
@Tag(TagNames.DIST_SYSTEMS)
class ModelTupleStreamTest {

    static {
        /*
    This is a hack around the backend-dependent nature of secure random implementations
    though we can set the secure random algorithm in our pom.xml files (via maven surefire and test.solr.allowed.securerandom)
    there isn't a mechanism that is completely platform independent.
    By setting it there (for example, to NativePRNG) that makes it pass on some platforms like Linux but fails on some JVMs on Windows
    For testing purposes, we don't need strict guarantees around RNG, hence we don't want to enforce the RNG algorithm
     */
        String algorithm = new SecureRandom().getAlgorithm();
        System.setProperty("test.solr.allowed.securerandom", algorithm);
    }

    protected List<float[]> floatsList(int numFloats) {
        final List<float[]> floatsList = new ArrayList<float[]>();
        final float[] floats0 = new float[numFloats];
        final float[] floats1 = new float[numFloats];
        for (int ii = 0; ii < numFloats; ++ii) {
            floats0[ii] = 0f;
            floats1[ii] = 1f;
        }
        floatsList.add(floats0);
        floatsList.add(floats1);
        return floatsList;
    }

    @Test
    @DisplayName("Test")
    @Disabled("Permissions issues on CI")
    void test() throws Exception {
        int testsCount = 0;
        for (int numInputs = 1; numInputs <= 5; ++numInputs) {
            for (int numOutputs = 1; numOutputs <= 5; ++numOutputs) {
                for (Model model : new Model[] { buildMultiLayerNetworkModel(numInputs, numOutputs), buildComputationGraphModel(numInputs, numOutputs) }) {
                    doTest(model, numInputs, numOutputs);
                    ++testsCount;
                }
            }
        }
        assertEquals(50, testsCount);
    }

    private void doTest(Model originalModel, int numInputs, int numOutputs) throws Exception {
        final Path tempDirPath = Files.createTempDirectory(null);
        final File tempDirFile = tempDirPath.toFile();
        tempDirFile.deleteOnExit();
        final SolrResourceLoader solrResourceLoader = new SolrResourceLoader(tempDirPath);
        final File tempFile = File.createTempFile("prefix", "suffix", tempDirFile);
        tempFile.deleteOnExit();
        final String serializedModelFileName = tempFile.getPath();
        ModelSerializer.writeModel(originalModel, serializedModelFileName, false);
        final Model restoredModel = ModelGuesser.loadModelGuess(serializedModelFileName);
        final StreamContext streamContext = new StreamContext();
        final SolrClientCache solrClientCache = new SolrClientCache();
        streamContext.setSolrClientCache(solrClientCache);
        final String[] inputKeys = new String[numInputs];
        final String inputKeysList = fillArray(inputKeys, "input", ",");
        final String[] outputKeys = new String[numOutputs];
        final String outputKeysList = fillArray(outputKeys, "output", ",");
        for (final float[] floats : floatsList(numInputs)) {
            final String inputValuesList;
            {
                final StringBuilder sb = new StringBuilder();
                for (int ii = 0; ii < inputKeys.length; ++ii) {
                    if (0 < ii)
                        sb.append(',');
                    sb.append(inputKeys[ii]).append('=').append(floats[ii]);
                }
                inputValuesList = sb.toString();
            }
            final StreamFactory streamFactory = new SolrDefaultStreamFactory().withSolrResourceLoader(solrResourceLoader).withFunctionName("model", ModelTupleStream.class);
            final StreamExpression streamExpression = StreamExpressionParser.parse("model(" + "tuple(" + inputValuesList + ")" + ",serializedModelFileName=\"" + serializedModelFileName + "\"" + ",inputKeys=\"" + inputKeysList + "\"" + ",outputKeys=\"" + outputKeysList + "\"" + ")");
            final TupleStream tupleStream = streamFactory.constructStream(streamExpression);
            tupleStream.setStreamContext(streamContext);
            assertTrue(tupleStream instanceof ModelTupleStream);
            final ModelTupleStream modelTupleStream = (ModelTupleStream) tupleStream;
            modelTupleStream.open();
            {
                final Tuple tuple1 = modelTupleStream.read();
                assertNotNull(tuple1);
                assertFalse(tuple1.EOF);
                for (int ii = 0; ii < outputKeys.length; ++ii) {
                    final INDArray inputs = Nd4j.create(new float[][] { floats });
                    final double originalScore = NetworkUtils.output((Model) originalModel, inputs).getDouble(ii);
                    final double restoredScore = NetworkUtils.output((Model) restoredModel, inputs).getDouble(ii);
                    assertEquals(originalScore, restoredScore, 1e-5,originalModel.getClass().getSimpleName() + " (originalScore-restoredScore)=" + (originalScore - restoredScore));
                    final Double outputValue = tuple1.getDouble(outputKeys[ii]);
                    assertNotNull(outputValue);
                    final double tupleScore = outputValue.doubleValue();
                    assertEquals(originalScore, tupleScore, 1e-5,originalModel.getClass().getSimpleName() + " (originalScore-tupleScore[" + ii + "])=" + (originalScore - tupleScore));
                }
                final Tuple tuple2 = modelTupleStream.read();
                assertNotNull(tuple2);
                assertTrue(tuple2.EOF);
            }
            modelTupleStream.close();
            doToExpressionTest(streamExpression, modelTupleStream.toExpression(streamFactory), inputKeys.length);
            doToExplanationTest(modelTupleStream.toExplanation(streamFactory));
        }
    }

    private static void doToExpressionTest(StreamExpression streamExpression, StreamExpressionParameter streamExpressionParameter, int inputKeysLength) {
        assertTrue(streamExpressionParameter instanceof StreamExpression);
        // tuple(input1=1,input2=2) and tuple(input2=2,input1=1) are equivalent
        // but StreamExpression equals does not consider them equal.
        if (inputKeysLength == 1) {
            assertEquals(streamExpression, (StreamExpression) streamExpressionParameter);
        }
    }

    private static void doToExplanationTest(Explanation explanation) {
        final Map<String, Object> explanationMap = new TreeMap<String, Object>();
        explanation.toMap(explanationMap);
        assertTrue(explanation instanceof StreamExplanation);
        assertNotNull(explanationMap.remove("children"));
        assertNotNull(explanationMap.remove("expression"));
        assertNotNull(explanationMap.remove("expressionNodeId"));
        assertEquals(ExpressionType.STREAM_DECORATOR, explanationMap.remove("expressionType"));
        assertEquals(explanationMap.remove("functionName"), "model");
        assertEquals(ModelTupleStream.class.getName(), explanationMap.remove("implementingClass"));
        assertTrue(explanationMap.isEmpty(),explanationMap.toString());
    }

    /**
     * Fills an existing array using prefix and delimiter, e.g.
     * input: arr = [ "", "", "" ] prefix="value" delimiter=","
     * output: arr = [ "value1", "value2", "value3" ]
     * return: "value1,value2,value3"
     */
    private static String fillArray(String[] arr, final String prefix, final String delimiter) {
        final StringBuilder sb = new StringBuilder();
        for (int ii = 0; ii < arr.length; ++ii) {
            arr[ii] = prefix + Integer.toString(ii + 1);
            if (0 < ii)
                sb.append(delimiter);
            sb.append(arr[ii]);
        }
        return sb.toString();
    }

    protected Model buildMultiLayerNetworkModel(int numInputs, int numOutputs) throws Exception {
        final MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().list(new OutputLayer.Builder().nIn(numInputs).nOut(numOutputs).activation(Activation.IDENTITY).lossFunction(LossFunctions.LossFunction.MSE).build()).build();
        final MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        final float[] floats = new float[(numInputs + 1) * numOutputs];
        final float base0 = 0.01f;
        float base = base0;
        for (int ii = 0; ii < floats.length; ++ii) {
            base *= 2;
            if (base > 1 / base0)
                base = base0;
            floats[ii] = base;
        }
        final INDArray params = Nd4j.create(floats);
        model.setParams(params);
        return model;
    }

    protected Model buildComputationGraphModel(int numInputs, int numOutputs) throws Exception {
        final ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder().graphBuilder().addInputs("inputLayer").addLayer("outputLayer", new OutputLayer.Builder().nIn(numInputs).nOut(numOutputs).activation(Activation.IDENTITY).lossFunction(LossFunctions.LossFunction.MSE).build(), "inputLayer").setOutputs("outputLayer").build();
        final ComputationGraph model = new ComputationGraph(conf);
        model.init();
        final float[] floats = new float[(numInputs + 1) * numOutputs];
        final float base0 = 0.01f;
        float base = base0;
        for (int ii = 0; ii < floats.length; ++ii) {
            base *= 2;
            if (base > 1 / base0)
                base = base0;
            floats[ii] = base;
        }
        final INDArray params = Nd4j.create(floats);
        model.setParams(params);
        return model;
    }
}
