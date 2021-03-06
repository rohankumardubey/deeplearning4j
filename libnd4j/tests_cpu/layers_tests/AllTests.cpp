/* ******************************************************************************
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

//
// Created by raver119 on 04.08.17.
//
//
#include "testlayers.h"
/*
#include "ConvolutionTests.cpp"
#include "DeclarableOpsTests.cpp"
#include "DenseLayerTests.cpp"
#include "FlatBuffersTests.cpp"
#include "GraphTests.cpp"
#include "HashUtilsTests.cpp"
#include "NDArrayTests.cpp"
#include "SessionLocalTests.cpp"
#include "StashTests.cpp"
#include "TadTests.cpp"
#include "VariableSpaceTests.cpp"
#include "VariableTests.cpp"
#include "WorkspaceTests.cpp"
 */
///////

//#include "CyclicTests.h"
// #include "ProtoBufTests.cpp"

int main(int argc, char **argv) {
  testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}
