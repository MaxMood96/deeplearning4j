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
// @author Abdelrauf
//
#include <helpers/LoopsCoordsHelper.h>

#include <type_traits>

#include "testlayers.h"
using namespace sd;

class LoopCoordsHelper : public NDArrayTests {
 public:
};

template <size_t Rank, size_t rankIndex = 0, bool Last_Index_Faster = true>
SD_INLINE typename std::enable_if<(Rank - 1 == rankIndex), bool>::type eq_strides(CoordsState<Rank - 1>& cbs,
                                                                                  const LongType* strides) {
  return STRIDE(cbs, rankIndex) == strides[rankIndex];
}

template <size_t Rank, size_t rankIndex = 0>
SD_INLINE typename std::enable_if<(Rank - 1 != rankIndex), bool>::type eq_strides(CoordsState<Rank - 1>& cbs,
                                                                                  const LongType* strides) {
  return STRIDE(cbs, rankIndex) == strides[rankIndex] && eq_strides<Rank, rankIndex + 1>(cbs, strides);
}

template <size_t Rank, size_t rankIndex = 0, bool Last_Index_Faster = true>
SD_INLINE typename std::enable_if<(Rank - 1 == rankIndex), bool>::type eq_zip_strides(ZipCoordsState<Rank - 1>& cbs,
                                                                                      const LongType* strides1,
                                                                                      const LongType* strides2) {
  return ZIP_STRIDE1(cbs, rankIndex) == strides1[rankIndex] && ZIP_STRIDE2(cbs, rankIndex) == strides2[rankIndex];
}

template <size_t Rank, size_t rankIndex = 0>
SD_INLINE typename std::enable_if<(Rank - 1 != rankIndex), bool>::type eq_zip_strides(ZipCoordsState<Rank - 1>& cbs,
                                                                                      const LongType* strides1,
                                                                                      const LongType* strides2) {
  return ZIP_STRIDE1(cbs, rankIndex) == strides1[rankIndex] && ZIP_STRIDE2(cbs, rankIndex) == strides2[rankIndex] &&
         eq_zip_strides<Rank, rankIndex + 1>(cbs, strides1, strides2);
}

TEST_F(LoopCoordsHelper, Init_Tests) {
  constexpr size_t test_Index = 131;
  constexpr size_t Rank = 5;

  LongType shape[Rank] = {3, 5, 7, 8, 9};
  LongType multiply_st[] = {2, 3, 3, 5, 6, 7, 9, 3};
  LongType strides_c[Rank];
  LongType strides_f[Rank];

  LongType coords[Rank];
  LongType coords_f[Rank];

  strides_f[0] = multiply_st[0] * shape[0];
  strides_c[Rank - 1] = multiply_st[Rank - 1] * shape[Rank - 1];

  for (int i = 1; i < Rank; i++) {
    strides_f[i] = strides_f[i - 1] * multiply_st[i] * shape[i];
  }

  for (int i = Rank - 2; i >= 0; i--) {
    strides_c[i] = strides_c[i + 1] * multiply_st[i] * shape[i];
  }

  // init our base coords
  INDEX2COORDS(test_Index, Rank, shape, coords);
  INDEX2COORDS(test_Index, Rank, shape, coords_f);

  size_t offset_calc;
  COORDS2INDEX(Rank, strides_c, coords, offset_calc);
  size_t offset_calc_f;
  COORDS2INDEX(Rank, strides_f, coords_f, offset_calc_f);

  CoordsState<Rank - 1> cts;
  CoordsState<Rank - 1> cts_f;

  ZipCoordsState<Rank - 1> zcts;
  ZipCoordsState<Rank - 1> zcts_f;

  size_t offset = init_coords<Rank>(cts, test_Index, shape, strides_c);
  size_t offset_f = init_coords<Rank, 0, false>(cts_f, test_Index, shape, strides_f);

  zip_size_t zoffset = init_coords<Rank>(zcts, test_Index, shape, strides_c, strides_c);
  zip_size_t zoffset_f = init_coords<Rank, 0, false>(zcts_f, test_Index, shape, strides_f, strides_f);

  ASSERT_TRUE(eq_coords<Rank>(cts, coords));
  ASSERT_TRUE(eq_coords<Rank>(cts_f, coords_f));

  ASSERT_TRUE(eq_zip_coords<Rank>(zcts, coords));
  ASSERT_TRUE(eq_zip_coords<Rank>(zcts_f, coords_f));

  ASSERT_TRUE(eq_strides<Rank>(cts, strides_c));
  ASSERT_TRUE(eq_strides<Rank>(cts_f, strides_f));

  ASSERT_TRUE(eq_zip_strides<Rank>(zcts, strides_c, strides_c));
  ASSERT_TRUE(eq_zip_strides<Rank>(zcts_f, strides_f, strides_f));

  ASSERT_EQ(offset, offset_calc);
  ASSERT_EQ(zoffset.first, offset_calc);
  ASSERT_EQ(zoffset.second, offset_calc);
  ASSERT_EQ(offset_f, offset_calc_f);
  ASSERT_EQ(zoffset_f.first, offset_calc_f);
  ASSERT_EQ(zoffset_f.second, offset_calc_f);
}

TEST_F(LoopCoordsHelper, Increment_Use_Tests) {
  constexpr size_t Rank = 4;

  LongType shape[Rank] = {3, 5, 7, 8};
  LongType multiply_st[] = {2, 3, 3, 5, 6, 7, 9, 3};
  LongType strides_c[Rank];
  LongType strides_f[Rank];

  LongType coords[Rank] = {};
  LongType coords_f[Rank] = {};
  LongType coords2[Rank] = {};
  LongType coords2_f[Rank] = {};
  LongType zcoords2[Rank] = {};
  LongType zcoords2_f[Rank] = {};

  strides_f[0] = multiply_st[0] * shape[0];
  strides_c[Rank - 1] = multiply_st[Rank - 1] * shape[Rank - 1];

  for (int i = 1; i < Rank; i++) {
    strides_f[i] = strides_f[i - 1] * multiply_st[i] * shape[i];
  }

  for (int i = Rank - 2; i >= 0; i--) {
    strides_c[i] = strides_c[i + 1] * multiply_st[i] * shape[i];
  }

  int total = 1;
  for (int i = 0; i < Rank; i++) {
    total *= shape[i];
  }

  CoordsState<Rank - 1> cts;
  CoordsState<Rank - 1> cts_f;

  ZipCoordsState<Rank - 1> zcts;
  ZipCoordsState<Rank - 1> zcts_f;

  size_t offset = init_coords<Rank>(cts, 0, shape, strides_c);
  size_t offset_f = init_coords<Rank, 0, false>(cts_f, 0, shape, strides_f);

  zip_size_t zoffset = init_coords<Rank>(zcts, 0, shape, strides_c, strides_c);
  zip_size_t zoffset_f = init_coords<Rank, 0, false>(zcts_f, 0, shape, strides_f, strides_f);

  size_t offset2 = 0;
  size_t offset2_f = 0;
  zip_size_t zoffset2 = {};
  zip_size_t zoffset2_f = {};

  for (int j = 0; j < total; j++) {
    INDEX2COORDS(j, Rank, shape, coords);
    INDEX2COORDS(j, Rank, shape, coords_f);

    size_t offset_calc;
    COORDS2INDEX(Rank, strides_c, coords, offset_calc);
    size_t offset_calc_f;
    COORDS2INDEX(Rank, strides_f, coords_f, offset_calc_f);

    ASSERT_TRUE(eq_coords<Rank>(cts, coords));
    ASSERT_TRUE(eq_coords<Rank>(cts_f, coords_f));

    ASSERT_TRUE(eq_zip_coords<Rank>(zcts, coords));
    ASSERT_TRUE(eq_zip_coords<Rank>(zcts_f, coords_f));

    ASSERT_EQ(offset, offset_calc);
    ASSERT_EQ(zoffset.first, offset_calc);
    ASSERT_EQ(zoffset.second, offset_calc);
    ASSERT_EQ(offset_f, offset_calc_f);
    ASSERT_EQ(zoffset_f.first, offset_calc_f);
    ASSERT_EQ(zoffset_f.second, offset_calc_f);

    ASSERT_EQ(offset2, offset_calc);
    ASSERT_EQ(zoffset2.first, offset_calc);
    ASSERT_EQ(zoffset2.second, offset_calc);
    ASSERT_EQ(offset2_f, offset_calc_f);
    ASSERT_EQ(zoffset2_f.first, offset_calc_f);
    ASSERT_EQ(zoffset2_f.second, offset_calc_f);

    offset = inc_coords<Rank>(cts, offset);
    offset_f = inc_coords<Rank, 0, false>(cts_f, offset_f);
    zoffset = inc_coords<Rank>(zcts, zoffset);
    zoffset_f = inc_coords<Rank, 0, false>(zcts_f, zoffset_f);

    offset2 = inc_coords(shape, strides_c, coords2, offset2, Rank);
    offset2_f = inc_coords<false>(shape, strides_f, coords2_f, offset2_f, Rank);
    zoffset2 = inc_coords(shape, strides_c, strides_c, zcoords2, zoffset2, Rank);
    zoffset2_f = inc_coords<false>(shape, strides_f, strides_f, zcoords2_f, zoffset2_f, Rank);
  }
}