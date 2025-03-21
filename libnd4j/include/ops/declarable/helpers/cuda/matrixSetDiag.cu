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
// @author Yurii Shyrma (iuriish@yahoo.com)
//
#include <array/ResultSet.h>
#include <helpers/PointersManager.h>
#include <ops/declarable/helpers/matrixSetDiag.h>

#include "execution/cuda/LaunchDims.h"


namespace sd {
namespace ops {
namespace helpers {

///////////////////////////////////////////////////////////////////
template <typename T>
SD_KERNEL static void matrixSetDiagCuda(const void* vx, const LongType* xShapeInfo, const void* vy,
                                        const LongType* yShapeInfo, void* vz, const LongType* zShapeInfo,
                                        const bool zeroPad) {
  // x - input,    shape [A,B,C]
  // y - diagonal, shape [A,B]
  // z - output,   shape [A,B,C]

  const auto x = reinterpret_cast<const T*>(vx);
  const auto y = reinterpret_cast<const T*>(vy);
  auto z = reinterpret_cast<T*>(vz);

  __shared__ int xRank;
  __shared__ LongType xLen;
  __shared__ const LongType *shapeX, *strideX, *strideY, *strideZ;
  __shared__ bool areSameOffsets;

  if (threadIdx.x == 0) {
    xRank = shape::rank(xShapeInfo);
    xLen = shape::length(xShapeInfo);
    shapeX = shape::shapeOf(xShapeInfo);
    strideX = shape::stride(xShapeInfo);
    strideY = shape::stride(yShapeInfo);
    strideZ = shape::stride(zShapeInfo);
    areSameOffsets = shape::haveSameShapeAndStrides(xShapeInfo, zShapeInfo);
  }
  __syncthreads();

  LongType coords[SD_MAX_RANK];
  const auto tid = blockIdx.x * blockDim.x + threadIdx.x;
  const auto step = gridDim.x * blockDim.x;

  for (LongType i = tid; i < xLen; i += step) {
    // Compute coordinates and offsets
    INDEX2COORDS(i, xRank, shapeX, coords);
    LongType xOffset, zOffset, yOffset;

    COORDS2INDEX(xRank, strideX, coords, xOffset);
    zOffset = areSameOffsets ? xOffset : 0;
    if (!areSameOffsets) {
      COORDS2INDEX(xRank, strideZ, coords, zOffset);
    }

    // Check if on the diagonal
    if (coords[xRank - 2] == coords[xRank - 1]) {
      COORDS2INDEX(xRank - 1, strideY, coords, yOffset);
      z[zOffset] = y[yOffset];
    } else {
      z[zOffset] = zeroPad ? static_cast<T>(0) : x[xOffset];
    }
  }
}

///////////////////////////////////////////////////////////////////
template <typename T>
static void matrixSetDiagCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const int sharedMem,
                                      const cudaStream_t* stream, const void* vx, const LongType* xShapeInfo,
                                      const void* vy, const LongType* yShapeInfo, void* vz,
                                      const LongType* zShapeInfo, const bool zeroPad) {
  matrixSetDiagCuda<T>
      <<<blocksPerGrid, threadsPerBlock, sharedMem, *stream>>>(vx, xShapeInfo, vy, yShapeInfo, vz, zShapeInfo, zeroPad);
  sd::DebugHelper::checkErrorCode(const_cast<cudaStream_t *>(stream), "matrixSetDiagCuda failed");

}

///////////////////////////////////////////////////////////////////
void matrixSetDiag(LaunchContext* context, NDArray& input, NDArray& diagonal, NDArray& output,
                   const bool zeroPad) {
  const int threadsPerBlock = SD_MAX_NUM_THREADS / 2;
  const int blocksPerGrid = (input.lengthOf() + threadsPerBlock - 1) / threadsPerBlock;
  const int sharedMem = threadsPerBlock * sizeof(LongType) * input.rankOf() + 128;

  dim3 launchDims = matrixSetDiagDims(input.lengthOf(),input.rankOf());
  PointersManager manager(context, "matrixSetDiag");

  NDArray::prepareSpecialUse({&output}, {&input, &diagonal});
  BUILD_SINGLE_SELECTOR(input.dataType(), matrixSetDiagCudaLauncher,
                        (launchDims.y, launchDims.x, launchDims.z, context->getCudaStream(), input.specialBuffer(),
                         input.specialShapeInfo(), diagonal.specialBuffer(), diagonal.specialShapeInfo(),
                         output.specialBuffer(), output.specialShapeInfo(), zeroPad),
                        SD_COMMON_TYPES);
  NDArray::registerSpecialUse({&output}, {&input, &diagonal});

  manager.synchronize();
}

}  // namespace helpers
}  // namespace ops
}  // namespace sd
