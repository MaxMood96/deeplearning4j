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

/*
 * broadcasting.h
 *
 *  Created on: Dec 28, 2015
 *      Author: agibsonccc
 */

#ifndef BROADCASTING_BOOL_H_
#define BROADCASTING_BOOL_H_
#include <helpers/DebugHelper.h>
#include <math/templatemath.h>
#include <ops/ops.h>
#include <system/op_boilerplate.h>

#ifdef __JNI__
#include <jni.h>
#endif

#include <loops/legacy_ops.h>

namespace functions {
namespace broadcast {

/**
 * Broadcast operation
 * for broadcasting a smaller tensor
 * along long a bigger one.
 */
template <typename X, typename Z>
class BroadcastBool {
 public:
#ifdef __CUDACC__

  template <typename OpType>
  static SD_DEVICE void transformCuda(const void *x, const sd::LongType *xShapeInfo, const void *y,
                                      const sd::LongType *yShapeInfo, void *result, const sd::LongType *resultShapeInfo,
                                      void *extraParams,sd::LongType *dimension,sd::LongType dimensionLength,
                                      const sd::LongType *tadOnlyShapeInfo, const sd::LongType *tadOffsets,
                                      const sd::LongType *tadOnlyShapeInfoZ, const sd::LongType *tadOffsetsZ);

  template <typename OpType>
  static SD_DEVICE void transformCuda(const void *x, const sd::LongType *xShapeInfo, const void *y,
                                      const sd::LongType *yShapeInfo, void *z, const sd::LongType *zShapeInfo,
                                      void *extraParams);

  template <typename OpClass>
  static SD_HOST void intermediateBroadcast(dim3 launchDims, cudaStream_t *stream, void const *x,
                                            sd::LongType const *xShapeInfo, void const *y,
                                            sd::LongType const *yShapeInfo, void *result,
                                            sd::LongType const *resultShapeInfo, void *extraParams,
                                            sd::LongType *dimension,sd::LongType dimensionLength, sd::LongType const *tadOnlyShapeInfo,
                                            sd::LongType const *tadOffsets, sd::LongType const *tadOnlyShapeInfoZ,
                                            sd::LongType const *tadOffsetsZ);

  template <typename OpClass>
  static SD_HOST void intermediateBroadcast(dim3 launchDims, cudaStream_t *stream, const void *x,
                                            const sd::LongType *xShapeInfo, const void *y,
                                            const sd::LongType *yShapeInfo, void *z, const sd::LongType *zShapeInfo,
                                            void *extraParams);

  static SD_HOST void execBroadcast(dim3 launchDims, cudaStream_t *stream, int opNum, void const *x,
                                    sd::LongType const *xShapeInfo, void const *y, sd::LongType const *yShapeInfo,
                                    void *result, sd::LongType const *resultShapeInfo, void *extraParams,
                                    sd::LongType *dimension, sd::LongType dimensionLength, sd::LongType const *tadOnlyShapeInfo,
                                    sd::LongType const *tadOffsets, sd::LongType const *tadOnlyShapeInfoZ,
                                    sd::LongType const *tadOffsetsZ);

  static SD_HOST void execBroadcast(dim3 launchDims, cudaStream_t *stream, const int opNum, const void *x,
                                    const sd::LongType *xShapeInfo, const void *y, const sd::LongType *yShapeInfo,
                                    void *z, const sd::LongType *zShapeInfo, void *extraParams);

  template <typename OpType>
  static SD_DEVICE void transformInverseCuda(const void *x, const sd::LongType *xShapeInfo, const void *y,
                                             const sd::LongType *yShapeInfo, void *result,
                                             const sd::LongType *resultShapeInfo, void *extraParams,
                                             sd::LongType *dimension, sd::LongType dimensionLength, const sd::LongType *tadOnlyShapeInfo,
                                             const sd::LongType *tadOffsets, const sd::LongType *tadOnlyShapeInfoZ,
                                             const sd::LongType *tadOffsetsZ);

  template <typename OpClass>
  static SD_HOST void intermediateInverseBroadcast(
      dim3 launchDims, cudaStream_t *stream, const void *x, const sd::LongType *xShapeInfo, const void *y,
      const sd::LongType *yShapeInfo, void *result, const sd::LongType *resultShapeInfo, void *extraParams,
      sd::LongType *dimension, sd::LongType dimensionLength, const sd::LongType *tadOnlyShapeInfo, const sd::LongType *tadOffsets,
      const sd::LongType *tadOnlyShapeInfoZ, const sd::LongType *tadOffsetsZ);

  static SD_HOST void execInverseBroadcast(dim3 launchDims, cudaStream_t *stream, int opNum, const void *x,
                                           const sd::LongType *xShapeInfo, const void *y,
                                           const sd::LongType *yShapeInfo, void *result,
                                           const sd::LongType *resultShapeInfo, void *extraParams,
                                           sd::LongType *dimension, sd::LongType dimensionLength, const sd::LongType *tadOnlyShapeInfo,
                                           const sd::LongType *tadOffsets, const sd::LongType *tadOnlyShapeInfoZ,
                                           const sd::LongType *tadOffsetsZ);

#else

  static void exec(int opNum, const void *x, const sd::LongType *xShapeInfo, const void *y,
                   const sd::LongType *yShapeInfo, void *result, const sd::LongType *resultShapeInfo, void *extraParams,
                   sd::LongType *dimension, sd::LongType dimensionLength, const sd::LongType *tadShapeInfo, const sd::LongType *tadOffset,
                   const sd::LongType *tadShapeInfoZ, const sd::LongType *tadOffsetZ, sd::LongType start, sd::LongType stop);

  static void exec(int opNum, const void *x, const sd::LongType *xShapeInfo, const void *y,
                   const sd::LongType *yShapeInfo, void *z, const sd::LongType *zShapeInfo, void *extraParams);

  static void execInverse(int opNum, const void *x, const sd::LongType *xShapeInfo, const void *y,
                          const sd::LongType *yShapeInfo, void *result, const sd::LongType *resultShapeInfo,
                          void *extraParams, sd::LongType *dimension, sd::LongType dimensionLength, const sd::LongType *tadShapeInfo,
                          const sd::LongType *tadOffset, const sd::LongType *tadShapeInfoZ,
                          const sd::LongType *tadOffsetZ, sd::LongType start, sd::LongType stop);

  /**
   * CPU execution
   * @param x the input
   * @param xShapeInfo the x shape information
   * @param y the y data
   * @param yShapeInfo the y shape information
   * @param result the result
   * @param resultShapeInfo the result shape information
   * @param dimension the dimension to broadcast along long
   * @param dimensionLength the length of the dimension buffer
   */
  template <typename OpType>
  static void exec(const void *x, const sd::LongType *xShapeInfo, const void *y, const sd::LongType *yShapeInfo,
                   void *result, const sd::LongType *resultShapeInfo, void *extraParams, sd::LongType *dimension,
                   sd::LongType dimensionLength, const sd::LongType *tadShapeInfo, const sd::LongType *tadOffset,
                   const sd::LongType *tadShapeInfoZ, const sd::LongType *tadOffsetZ, uint64_t start, uint64_t stop);

  template <typename OpType>
  static void exec(const void *x, const sd::LongType *xShapeInfo, const void *y, const sd::LongType *yShapeInfo,
                   void *z, const sd::LongType *zShapeInfo, void *extraParams);

  template <typename OpType>
  static void execInverse(const void *x, const sd::LongType *xShapeInfo, const void *y, const sd::LongType *yShapeInfo,
                          void *result, const sd::LongType *resultShapeInfo, void *extraParams,
                          sd::LongType *dimension,
                          sd::LongType dimensionLength, const sd::LongType *tadShapeInfo, const sd::LongType *tadOffset,
                          const sd::LongType *tadShapeInfoZ, const sd::LongType *tadOffsetZ, uint64_t start,
                          uint64_t stop);
#endif
};
}  // namespace broadcast
}  // namespace functions

#endif /* BROADCASTING_H_ */
