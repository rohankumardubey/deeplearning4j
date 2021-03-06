/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  * See the NOTICE file distributed with this work for additional
 *  * information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.deeplearning4j.cuda.lstm;

import org.deeplearning4j.BaseDL4JTest;
import org.deeplearning4j.cuda.dropout.CudnnDropoutHelper;
import org.junit.jupiter.api.Test;
import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.reduce.longer.MatchCondition;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.conditions.Conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NativeTag
public class ValidateCudnnDropout extends BaseDL4JTest {

    @Override
    public long getTimeoutMilliseconds() {
        return 180000L;
    }

    @Test
    public void testCudnnDropoutSimple() {
        for (int[] shape : new int[][]{{10, 10}, {5, 2, 5, 2}}) {

            Nd4j.getRandom().setSeed(12345);
            INDArray in = Nd4j.ones(shape);
            double pRetain = 0.25;
            double valueIfKept = 1.0 / pRetain;

            CudnnDropoutHelper d = new CudnnDropoutHelper(DataType.DOUBLE);

            INDArray out = Nd4j.createUninitialized(shape);
            d.applyDropout(in, out, pRetain);

            int countZero = Nd4j.getExecutioner().execAndReturn(new MatchCondition(out, Conditions.equals(0.0))).z().getInt(0);
            int countNonDropped = Nd4j.getExecutioner().execAndReturn(new MatchCondition(out, Conditions.equals(valueIfKept))).z().getInt(0);
//            System.out.println(countZero);
//            System.out.println(countNonDropped);

            assertTrue(countZero >= 5 && countZero <= 90, String.valueOf(countZero));
            assertTrue(countNonDropped >= 5 && countNonDropped <= 95, String.valueOf(countNonDropped));
            assertEquals(100, countZero + countNonDropped);

            //Test repeatability:
            for (int i = 0; i < 10; i++) {
                Nd4j.getRandom().setSeed(12345);
                d.setRngStates(null);
                d.setMask(null);

                INDArray outNew = Nd4j.createUninitialized(shape);
                d.applyDropout(in, outNew, pRetain);

                assertEquals(out, outNew);
            }

            //Test backprop:
            INDArray gradAtOut = Nd4j.ones(shape);
            INDArray gradAtInput = Nd4j.createUninitialized(shape);
            d.backprop(gradAtOut, gradAtInput);
            Nd4j.getExecutioner().commit();

            //If dropped: expect 0. Otherwise: expect 1/pRetain, i.e., output for 1s input
            assertEquals(out, gradAtInput);
        }
    }

}
