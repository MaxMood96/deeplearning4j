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

//
// @author Yurii Shyrma (iuriish@yahoo.com)
//
#include <helpers/PointersManager.h>
#include <ops/declarable/helpers/convolutions.h>

#include "execution/cuda/LaunchDims.h"
#include "helpers/DebugHelper.h"


namespace sd {
namespace ops {

//////////////////////////////////////////////////////////////////////////
template <typename T>
SD_KERNEL static void upsampling2dBPCuda(const void* vx, const LongType* xShapeInfo, void* vz,
                                         const LongType* zShapeInfo, const bool isNCHW) {
  const T* x = reinterpret_cast<const T*>(vx);
  T* z = reinterpret_cast<T*>(vz);

  __shared__ LongType rank, dimIH;
  __shared__ LongType factorH, factorW;
  __shared__ LongType zLen, *sharedMem;
  __shared__ LongType* xShape;
  __shared__ LongType* zShape;
  __shared__ LongType* xStride;
  __shared__ LongType* zStride;

  if (threadIdx.x == 0) {
    extern __shared__ unsigned char shmem[];
    sharedMem = reinterpret_cast<LongType*>(shmem);

    dimIH = isNCHW ? 2 : 1;
    zLen = shape::length(zShapeInfo);
    rank = 4;

    factorH = xShapeInfo[dimIH + 1] / zShapeInfo[dimIH + 1];
    factorW = xShapeInfo[dimIH + 2] / zShapeInfo[dimIH + 2];

    // Cache shape information
    xShape = shape::shapeOf(xShapeInfo);
    zShape = shape::shapeOf(zShapeInfo);
    xStride = shape::stride(xShapeInfo);
    zStride = shape::stride(zShapeInfo);
  }
  __syncthreads();

  const auto zInd = threadIdx.x + blockIdx.x * blockDim.x;

  if (zInd >= zLen) return;

  auto coords = sharedMem + threadIdx.x * rank;

  INDEX2COORDS(zInd, rank, zShape, coords);

  LongType zOffset;
  COORDS2INDEX(rank, zStride, coords, zOffset);

  z[zOffset] = 0;

  const LongType zCoord2 = coords[dimIH] * factorH;
  const LongType zCoord3 = coords[dimIH + 1] * factorW;

  for (coords[dimIH] = zCoord2; coords[dimIH] < zCoord2 + factorH; ++coords[dimIH])
    for (coords[dimIH + 1] = zCoord3; coords[dimIH + 1] < zCoord3 + factorW; ++coords[dimIH + 1]) {
      LongType xOffset;
      COORDS2INDEX(rank, xStride, coords, xOffset);
      z[zOffset] += x[xOffset];
    }
}
//////////////////////////////////////////////////////////////////////////
template <typename T>
static void upsampling2dBPCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const int sharedMem,
                                       const cudaStream_t* stream, const void* vx, const LongType* xShapeInfo,
                                       void* vz, const LongType* zShapeInfo, const bool isNCHW) {
  upsampling2dBPCuda<T><<<blocksPerGrid, threadsPerBlock, sharedMem, *stream>>>(vx, xShapeInfo, vz, zShapeInfo, isNCHW);
  DebugHelper::checkErrorCode(const_cast<cudaStream_t*>(stream),"upsampling2dBPCuda failed");

}

//////////////////////////////////////////////////////////////////////////
void ConvolutionUtils::upsampling2dBP(graph::Context& block, NDArray& gradO, NDArray& gradI,
                                      const bool isNCHW) {
  PointersManager manager(block.launchContext(), "upsampling2d_bp");

  dim3 getUpSampling = getUpsamplingDims(gradI.lengthOf(),gradI.rankOf());

  NDArray::prepareSpecialUse({&gradI}, {&gradO});
  BUILD_SINGLE_SELECTOR(
      gradI.dataType(), upsampling2dBPCudaLauncher,
      (getUpSampling.x, getUpSampling.y, getUpSampling.z, block.launchContext()->getCudaStream(), gradO.specialBuffer(),
       gradO.specialShapeInfo(), gradI.specialBuffer(), gradI.specialShapeInfo(), isNCHW),
      SD_FLOAT_TYPES);
  NDArray::registerSpecialUse({&gradI}, {&gradO});

  manager.synchronize();
}

}  // namespace ops
}  // namespace sd
