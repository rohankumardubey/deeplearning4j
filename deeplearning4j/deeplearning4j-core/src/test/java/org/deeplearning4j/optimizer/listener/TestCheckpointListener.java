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

package org.deeplearning4j.optimizer.listener;

import org.deeplearning4j.BaseDL4JTest;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.Checkpoint;
import org.deeplearning4j.optimize.listeners.CheckpointListener;
import org.deeplearning4j.util.ModelSerializer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.common.primitives.Pair;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
@NativeTag
@Tag(TagNames.DL4J_OLD_API)
public class TestCheckpointListener extends BaseDL4JTest {

    @Override
    public long getTimeoutMilliseconds() {
        return 90000L;
    }


    private static Pair<MultiLayerNetwork,DataSetIterator> getNetAndData(){
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .list()
                .layer(new OutputLayer.Builder().nIn(4).nOut(3).activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        DataSetIterator iter = new IrisDataSetIterator(25,50);

        return new Pair<>(net, iter);
    }

    @Test
    public void testCheckpointListenerEvery2Epochs(@TempDir Path tempDir) throws Exception {
        File f = tempDir.toFile();
        Pair<MultiLayerNetwork, DataSetIterator> p = getNetAndData();
        MultiLayerNetwork net = p.getFirst();
        DataSetIterator iter = p.getSecond();


        CheckpointListener l = new CheckpointListener.Builder(f)
                .keepAll()
                .saveEveryNEpochs(2)
                .build();
        net.setListeners(l);

        for(int i=0; i<10; i++ ){
            net.fit(iter);

            if(i > 0 && i % 2 == 0){
                assertEquals(1 + i/2, f.list().length);
            }
        }

        //Expect models saved at end of epochs: 1, 3, 5, 7, 9... (i.e., after 2, 4, 6 etc epochs)
        File[] files = f.listFiles();
        int count = 0;
        for(File f2 : files){
            if(!f2.getPath().endsWith(".zip")){
                continue;
            }

            int prefixLength = "checkpoint_".length();
            int num = Integer.parseInt(f2.getName().substring(prefixLength, prefixLength+1));

            MultiLayerNetwork n = ModelSerializer.restoreMultiLayerNetwork(f2, true);
            int expEpoch = 2 * (num + 1) - 1;   //Saved at the end of the previous epoch
            int expIter = (expEpoch+1) * 2;     //+1 due to epochs being zero indexed

            assertEquals(expEpoch, n.getEpochCount());
            assertEquals(expIter, n.getIterationCount());
            count++;
        }

        assertEquals(5, count);
        assertEquals(5, l.availableCheckpoints().size());

        List<Checkpoint> listStatic = CheckpointListener.availableCheckpoints(f);
        assertEquals(5, listStatic.size());
    }

    @Test
    public void testCheckpointListenerEvery5Iter(@TempDir Path tempDir) throws Exception {
        File f = tempDir.toFile();
        Pair<MultiLayerNetwork, DataSetIterator> p = getNetAndData();
        MultiLayerNetwork net = p.getFirst();
        DataSetIterator iter = p.getSecond();


        CheckpointListener l = new CheckpointListener.Builder(f)
                .keepLast(3)
                .saveEveryNIterations(5)
                .build();
        net.setListeners(l);

        for(int i=0; i<20; i++ ){   //40 iterations total
            net.fit(iter);
        }

        //Expect models saved at iterations: 5, 10, 15, 20, 25, 30, 35  (training does 0 to 39 here)
        //But: keep only 25, 30, 35
        File[] files = f.listFiles();
        int count = 0;
        Set<Integer> ns = new HashSet<>();
        for(File f2 : files){
            if(!f2.getPath().endsWith(".zip")){
                continue;
            }
            count++;
            int prefixLength = "checkpoint_".length();
            int num = Integer.parseInt(f2.getName().substring(prefixLength, prefixLength+1));

            MultiLayerNetwork n = ModelSerializer.restoreMultiLayerNetwork(f2, true);
            int expIter = 5 * (num+1);
            assertEquals(expIter, n.getIterationCount());

            ns.add(n.getIterationCount());
            count++;
        }

        assertEquals( 3, ns.size(),ns.toString());
        assertTrue(ns.contains(25));
        assertTrue(ns.contains(30));
        assertTrue(ns.contains(35));

        assertEquals(3, l.availableCheckpoints().size());

        List<Checkpoint> listStatic = CheckpointListener.availableCheckpoints(f);
        assertEquals(3, listStatic.size());

        MultiLayerNetwork netStatic = CheckpointListener.loadCheckpointMLN(f, 6);
        assertEquals(35, netStatic.getIterationCount());

        MultiLayerNetwork netStatic2 = CheckpointListener.loadLastCheckpointMLN(f);
        assertEquals(35, netStatic2.getIterationCount());
        assertEquals(netStatic.params(), netStatic2.params());
    }

    @Test
    public void testCheckpointListenerEveryTimeUnit(@TempDir Path tempDir) throws Exception {
        File f = tempDir.toFile();
        Pair<MultiLayerNetwork, DataSetIterator> p = getNetAndData();
        MultiLayerNetwork net = p.getFirst();
        DataSetIterator iter = p.getSecond();


        CheckpointListener l = new CheckpointListener.Builder(f)
                .keepLast(3)
                .saveEvery(4900, TimeUnit.MILLISECONDS)
                .build();
        net.setListeners(l);

        for(int i=0; i<3; i++ ){   //10 iterations total
            net.fit(iter);
            Thread.sleep(5000);
        }

        //Expect models saved at iterations: 2, 4, 6, 8 (iterations 0 and 1 shoud happen before first 3 seconds is up)
        //But: keep only 5, 7, 9
        File[] files = f.listFiles();
        Set<Integer> ns = new HashSet<>();
        for(File f2 : files){
            if(!f2.getPath().endsWith(".zip")){
                continue;
            }

            int prefixLength = "checkpoint_".length();
            int num = Integer.parseInt(f2.getName().substring(prefixLength, prefixLength+1));

            MultiLayerNetwork n = ModelSerializer.restoreMultiLayerNetwork(f2, true);
            int expIter = 2 * (num + 1);
            assertEquals(expIter, n.getIterationCount());

            ns.add(n.getIterationCount());
        }

        assertEquals(2, l.availableCheckpoints().size());
        assertEquals(2, ns.size(),ns.toString());
        System.out.println(ns);
        assertTrue(ns.containsAll(Arrays.asList(2,4)));
    }

    @Test
    public void testCheckpointListenerKeepLast3AndEvery3(@TempDir Path tempDir) throws Exception {
        File f = tempDir.toFile();
        Pair<MultiLayerNetwork, DataSetIterator> p = getNetAndData();
        MultiLayerNetwork net = p.getFirst();
        DataSetIterator iter = p.getSecond();


        CheckpointListener l = new CheckpointListener.Builder(f)
                .keepLastAndEvery(3, 3)
                .saveEveryNEpochs(2)
                .build();
        net.setListeners(l);

        for(int i=0; i<20; i++ ){   //40 iterations total
            net.fit(iter);
        }

        //Expect models saved at end of epochs: 1, 3, 5, 7, 9, 11, 13, 15, 17, 19
        //But: keep only 5, 11, 15, 17, 19
        File[] files = f.listFiles();
        int count = 0;
        Set<Integer> ns = new HashSet<>();
        for(File f2 : files){
            if(!f2.getPath().endsWith(".zip")){
                continue;
            }
            count++;
            int prefixLength = "checkpoint_".length();
            int end = f2.getName().lastIndexOf("_");
            int num = Integer.parseInt(f2.getName().substring(prefixLength, end));

            MultiLayerNetwork n = ModelSerializer.restoreMultiLayerNetwork(f2, true);
            int expEpoch = 2 * (num+1) - 1;
            assertEquals(expEpoch, n.getEpochCount());

            ns.add(n.getEpochCount());
            count++;
        }

        assertEquals(5, ns.size(),ns.toString());
        assertTrue(ns.containsAll(Arrays.asList(5, 11, 15, 17, 19)),ns.toString());

        assertEquals(5, l.availableCheckpoints().size());
    }

    @Test
    public void testDeleteExisting(@TempDir Path tempDir) throws Exception {
        File f = tempDir.toFile();
        Pair<MultiLayerNetwork, DataSetIterator> p = getNetAndData();
        MultiLayerNetwork net = p.getFirst();
        DataSetIterator iter = p.getSecond();


        CheckpointListener l = new CheckpointListener.Builder(f)
                .keepAll()
                .saveEveryNEpochs(1)
                .build();
        net.setListeners(l);

        for(int i=0; i<3; i++ ){
            net.fit(iter);
        }

        //Now, create new listener:
        try{
            l = new CheckpointListener.Builder(f)
                    .keepAll()
                    .saveEveryNEpochs(1)
                    .build();
            fail("Expected exception");
        } catch (IllegalStateException e){
            assertTrue(e.getMessage().contains("Use deleteExisting(true)"));
        }

        l = new CheckpointListener.Builder(f)
                .keepAll()
                .saveEveryNEpochs(1)
                .deleteExisting(true)
                .build();
        net.setListeners(l);

        net.fit(iter);

        File[] fList = f.listFiles();   //checkpoint meta file + 1 checkpoint
        assertNotNull(fList);
        assertEquals(2, fList.length);
    }
}
