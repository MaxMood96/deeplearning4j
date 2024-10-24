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
// Created by raver119 on 16/11/17.
//

#ifndef LIBND4J_NODESTATE_H
#define LIBND4J_NODESTATE_H

#include <system/common.h>

namespace sd {
namespace graph {
class SD_LIB_EXPORT NodeState {
 private:
  // inner time spent on specific node
  LongType _inner = 0;

  // outer time spent on specific node
  LongType _outer = 0;

  // flag that shows if node is active or disabled (i.e. after Switch op)
  bool _active = true;

  bool _executed = false;

  // active divergence branch
  int _branch = 0;

  int _id = 0;

 public:
  NodeState(int id = 0);
  ~NodeState() = default;

  void setInnerTime(LongType time);
  void setOuterTime(LongType time);

  LongType innerTime();
  LongType outerTime();

  void markActive(bool isActive);
  bool isActive();

  int branch();
  void markBranch(int index);

  bool wasExecuted();
  void markExecuted(bool wasExecuted);
};
}  // namespace graph
}  // namespace sd

#endif  // LIBND4J_NODESTATE_H
