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

package org.nd4j.linalg.api.ops.impl.shape;

import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.DynamicCustomOp;
import org.nd4j.shade.guava.primitives.Ints;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.util.*;

/**
 *
 */
public class Squeeze extends DynamicCustomOp {

    private int[] squeezeDims;

    public Squeeze() {
    }

    public Squeeze(SameDiff sameDiff, SDVariable arg, int squeezeDims) {
        this(sameDiff, arg, new int[] {squeezeDims});
    }

    public Squeeze(SameDiff sameDiff, SDVariable arg, int[] squeezeDims) {
        super(null, sameDiff, new SDVariable[]{arg});
        this.squeezeDims = squeezeDims;
        addIArgument(squeezeDims);
    }

    public Squeeze(INDArray x, int axis) {
        addInputArgument(x);
        addIArgument(axis);
    }

    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {
        nodeDef.getAttrMap().get("squeeze_dims");
        List<Long> dimList = attributesForNode.get("squeeze_dims").getList().getIList();
        squeezeDims = new int[dimList.size()];
        for( int i = 0; i<dimList.size(); i++ )
            squeezeDims[i] = dimList.get(i).intValue();
        addIArgument(squeezeDims);
    }

    @Override
    public String opName() {
        return "squeeze";
    }

    @Override
    public String tensorflowName() {
        return "Squeeze";
    }


    @Override
    public void configureFromArguments() {
        if(!iArguments.isEmpty()) {
            this.squeezeDims = Ints.toArray(iArguments);
        }
    }

    @Override
    public void setPropertiesForFunction(Map<String, Object> properties) {
        if(properties.containsKey("squeezeDims")) {
            Long squeezeProp = getLongValueFromProperty("squeezeDims",properties);
            this.squeezeDims = new int[] {squeezeProp.intValue()};
        }
    }

    @Override
    public List<SDVariable> doDiff(List<SDVariable> i_v) {
        if (squeezeDims == null) {
            //TODO Strictly speaking this *is* possible by inspecting the input array
            throw new IllegalStateException("Cannot do Squeeze backprop with no dimensions");
        }
        SDVariable ret = i_v.get(0);
        for (int d : squeezeDims) {
            ret = sameDiff.expandDims(ret, d);
        }

        return Arrays.asList(ret);
    }

    @Override
    public List<org.nd4j.linalg.api.buffer.DataType> calculateOutputDataTypes(List<org.nd4j.linalg.api.buffer.DataType> dataTypes){
        Preconditions.checkState(!dataTypes.isEmpty(), "Expected list with at least 1 datatype for %s, got %s", getClass(), dataTypes);
        //Output type is same as input type
        return Arrays.asList(dataTypes.get(0));
    }
}
