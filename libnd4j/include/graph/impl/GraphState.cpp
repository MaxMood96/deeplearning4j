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
// Created by raver119 on 23.01.18.
//
#include <graph/GraphState.h>
#include <graph/Node.h>

namespace sd {
namespace graph {
GraphState::GraphState(sd::LongType id) {
  _id = id;
  _graph = new Graph(nullptr, &_variableSpace);
};

GraphState::~GraphState() {
  // stupid. we should get rid of pointers here i think. no need to bother
  for (const auto& v : _scopes) {
    v.second->forgetNodes();
    delete v.second;
  }

  // we must remove reference to VariableSpace
  _graph->forgetVariableSpace();

  delete _graph;
};

sd::Status GraphState::registerScope(int scopeId) {
  auto scope = new Scope(scopeId);
  _scopes[scopeId] = scope;

  auto scopeWrapper = new Node(::graph::OpType_LOGIC, 10, scopeId);
  _graph->addNode(scopeWrapper);

  return sd::Status::OK;
};

sd::Status GraphState::forgetScope(int scopeId) {
  if (_scopes.count(scopeId) > 0)
    _scopes.erase(scopeId);
  else
    return Logger::logKernelFailureMsg("Non-existent scope requested");

  return sd::Status::OK;
};

#ifndef __JAVACPP_HACK__
sd::Status GraphState::attachOpToScope(int scopeId, int nodeId, ops::DeclarableOp* op, ArgumentsList inputs) {
  if (_scopes.count(scopeId) == 0) return Logger::logKernelFailureMsg("GraphState: can't attach op to unknown scope");

  auto scope = _scopes[scopeId];

  // creating new Node
  auto node = new Node(op, nodeId);
  node->setScopeInfo(scopeId);

  // mapping inputs here
  for (int e = 0; e < inputs.size(); e++) {
    auto p = inputs.at(e);

    // each expected input is Variable in current VariableSpace
    // it should have it's numerical and symbolic ID

    if (!_variableSpace.hasVariable(p.first(), p.second())) {
      auto var = new Variable();
      var->setId(p.first(), p.second());
      _variableSpace.putVariable(p.first(), p.second(), var);
    }

    node->pickInput(p.first(), p.second());
  }

  scope->push_back(node);

  _graph->addNode(node);

  return sd::Status::OK;
};

Graph* GraphState::graph() { return _graph; }

Scope* GraphState::getScope(int scopeId) {
  if (_scopes.count(scopeId) == 0) {
    sd_printf("GraphState: Unknown scope requested %i\n", scopeId);
    return nullptr;
  }

  return _scopes[scopeId];
}
#endif
sd::Status GraphState::defineReturn(int scopeId, int nodeId, ArgumentsList args) {
  if (_scopes.count(scopeId) == 0) return Logger::logKernelFailureMsg("GraphState: can't attach op to unknown scope");

  auto scope = _scopes[scopeId];

  // creating new Node for RETURN
  auto node = new Node(::graph::OpType_LOGIC, 40, nodeId);
  node->setScopeInfo(scopeId);

  // mapping inputs here
  for (int e = 0; e < args.size(); e++) {
    auto p = args.at(e);

    // each expected input is Variable in current VariableSpace
    // it should have it's numerical and symbolic ID

    if (!_variableSpace.hasVariable(p.first(), p.second())) {
      auto var = new Variable();
      var->setId(p.first(), p.second());
      _variableSpace.putVariable(p.first(), p.second(), var);
    }

    node->pickInput(p.first(), p.second());
    node->pickOutput(0, e);
  }

  scope->push_back(node);

  _graph->addNode(node);

  return sd::Status::OK;
}

bool GraphState::hasScope(int scopeId) { return _scopes.count(scopeId) > 0; }

VariableSpace* GraphState::variableSpace() { return &_variableSpace; };

sd::LongType GraphState::id() { return _id; }

sd::Status GraphState::attachOpToScope(int scopeId, sd::LongType opNum, int type, ArgumentsList inputs) {
  // we should use OpRegistrator here, to create Node and push it to specific scope
  return sd::Status::OK;
}
}  // namespace graph
}  // namespace sd
