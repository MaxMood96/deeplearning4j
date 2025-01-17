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
// @author yurii@skymind.io
//
#include <graph/Intervals.h>

namespace sd {

// default constructor
Intervals::Intervals() : _content({{}}) {}

// constructor
Intervals::Intervals(const std::initializer_list<std::vector<LongType>>& content) : _content(content) {}
Intervals::Intervals(const std::vector<std::vector<LongType>>& content) : _content(content) {}

//////////////////////////////////////////////////////////////////////////
// accessing operator
std::vector<LongType> Intervals::operator[](const LongType i) const { return *(_content.begin() + i); }

//////////////////////////////////////////////////////////////////////////
// returns size of _content
int Intervals::size() const { return _content.size(); }

//////////////////////////////////////////////////////////////////////////


}  // namespace sd
