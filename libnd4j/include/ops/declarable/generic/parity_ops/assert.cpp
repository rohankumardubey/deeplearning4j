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
// @author raver119@gmail.com
//

#include <system/op_boilerplate.h>
#if NOT_EXCLUDED(OP_Assert)

#include <ops/declarable/CustomOperations.h>
#include <ops/declarable/helpers/axis.h>

namespace sd {
namespace ops {
OP_IMPL(Assert, 1, 1, false) {
  auto x = INPUT_VARIABLE(0);

  if (!x->e<bool>(0)) {
    REQUIRE_TRUE(false, 0, "Assertion failed for node [%i]\n", block.getNodeId());
  }

  return sd::Status::OK;
}
DECLARE_TYPES(Assert) { getOpDescriptor()->setAllowedInputTypes(DataType::ANY)->setSameMode(true); }
}  // namespace ops
}  // namespace sd

#endif
