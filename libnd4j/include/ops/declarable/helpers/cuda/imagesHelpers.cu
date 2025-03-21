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
// @author Oleh Semeniv (oleg.semeniv@gmail.com)
//
#include <helpers/ConstantTadHelper.h>
#include <helpers/PointersManager.h>
#include <ops/declarable/helpers/adjust_hue.h>
#include <ops/declarable/helpers/imagesHelpers.h>
#include <system/op_boilerplate.h>

#include "execution/cuda/LaunchDims.h"
#include "helpers/DebugHelper.h"


namespace sd {
namespace ops {
namespace helpers {

///////////////////////////////////////////////////////////////////
template <typename T>
SD_KERNEL void rgbToYuvCuda(const void* vx, const LongType* xShapeInfo, const LongType* xTadOffsets, void* vz,
                            const LongType* zShapeInfo, const LongType* zTadOffsets,
                            const LongType numOfTads, const int dimC) {
  const T* x = reinterpret_cast<const T*>(vx);
  T* z = reinterpret_cast<T*>(vz);

  __shared__ int rank;
  __shared__ LongType xDimCstride, zDimCstride;

  if (threadIdx.x == 0) {
    rank = shape::rank(xShapeInfo);
    xDimCstride = shape::stride(xShapeInfo)[dimC];
    zDimCstride = shape::stride(zShapeInfo)[dimC];
  }
  __syncthreads();

  const auto tid = blockIdx.x * blockDim.x + threadIdx.x;

  for (LongType i = tid; i < numOfTads; i += gridDim.x * blockDim.x) {
    const T* xTad = x + xTadOffsets[i];
    T* zTad = z + zTadOffsets[i];

    rgbYuv<T>(xTad[0], xTad[xDimCstride], xTad[2 * xDimCstride], zTad[0], zTad[zDimCstride], zTad[2 * zDimCstride]);
  }
}

///////////////////////////////////////////////////////////////////
template <typename T>
void rgbToYuvCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const cudaStream_t* stream,
                          const void* vx, const LongType* xShapeInfo, const LongType* xTadOffsets, void* vz,
                          const LongType* zShapeInfo, const LongType* zTadOffsets, const LongType numOfTads,
                          const int dimC) {
  rgbToYuvCuda<T><<<blocksPerGrid, threadsPerBlock, 256, *stream>>>(vx, xShapeInfo, xTadOffsets, vz, zShapeInfo,
                                                                    zTadOffsets, numOfTads, dimC);
  sd::DebugHelper::checkErrorCode(const_cast<cudaStream_t *>(stream), "rgbToYuvCudaLauncher failed");

}

///////////////////////////////////////////////////////////////////
void transformRgbYuv(LaunchContext* context, NDArray& input, NDArray& output, const int dimC) {
  auto packX = ConstantTadHelper::getInstance().tadForDimensions(input.shapeInfo(), {dimC});
  auto packZ = ConstantTadHelper::getInstance().tadForDimensions(output.shapeInfo(), {dimC});

  const LongType numOfTads = packX->numberOfTads();

  const int threadsPerBlock = SD_MAX_NUM_THREADS / 2;
  const int blocksPerGrid = (numOfTads + threadsPerBlock - 1) / threadsPerBlock;

  PointersManager manager(context, "yuv_to_rgb");

  NDArray::prepareSpecialUse({&output}, {&input});
  BUILD_SINGLE_SELECTOR(input.dataType(), rgbToYuvCudaLauncher,
                        (blocksPerGrid, threadsPerBlock, context->getCudaStream(), input.specialBuffer(),
                         input.specialShapeInfo(), packX->platformOffsets(), output.specialBuffer(),
                         output.specialShapeInfo(), packZ->platformOffsets(), numOfTads, dimC),
                        SD_FLOAT_TYPES);
  NDArray::registerSpecialUse({&output}, {&input});

  manager.synchronize();
}

///////////////////////////////////////////////////////////////////
template <typename T>
SD_KERNEL void yuvToRgbCuda(const void* vx, const LongType* xShapeInfo, const LongType* xTadOffsets, void* vz,
                            const LongType* zShapeInfo, const LongType* zTadOffsets,
                            const LongType numOfTads, const int dimC) {
  const T* x = reinterpret_cast<const T*>(vx);
  T* z = reinterpret_cast<T*>(vz);

  __shared__ int rank;
  __shared__ LongType xDimCstride, zDimCstride;

  if (threadIdx.x == 0) {
    rank = shape::rank(xShapeInfo);
    xDimCstride = shape::stride(xShapeInfo)[dimC];
    zDimCstride = shape::stride(zShapeInfo)[dimC];
  }
  __syncthreads();

  const auto tid = blockIdx.x * blockDim.x + threadIdx.x;

  for (LongType i = tid; i < numOfTads; i += gridDim.x * blockDim.x) {
    const T* xTad = x + xTadOffsets[i];
    T* zTad = z + zTadOffsets[i];

    yuvRgb<T>(xTad[0], xTad[xDimCstride], xTad[2 * xDimCstride], zTad[0], zTad[zDimCstride], zTad[2 * zDimCstride]);
  }
}

///////////////////////////////////////////////////////////////////
template <typename T>
void yuvToRgbCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const cudaStream_t* stream,
                          const void* vx, const LongType* xShapeInfo, const LongType* xTadOffsets, void* vz,
                          const LongType* zShapeInfo, const LongType* zTadOffsets, const LongType numOfTads,
                          const int dimC) {
  yuvToRgbCuda<T><<<blocksPerGrid, threadsPerBlock, 256, *stream>>>(vx, xShapeInfo, xTadOffsets, vz, zShapeInfo,
                                                                    zTadOffsets, numOfTads, dimC);
  sd::DebugHelper::checkErrorCode(const_cast<cudaStream_t *>(stream), "yuvToRgbCuda failed");

}

///////////////////////////////////////////////////////////////////
void transformYuvRgb(LaunchContext* context, NDArray& input, NDArray& output, const int dimC) {
  auto packX = ConstantTadHelper::getInstance().tadForDimensions(input.shapeInfo(), {dimC});
  auto packZ = ConstantTadHelper::getInstance().tadForDimensions(output.shapeInfo(), {dimC});

  const LongType numOfTads = packX->numberOfTads();

  const int threadsPerBlock = SD_MAX_NUM_THREADS / 2;
  const int blocksPerGrid = (numOfTads + threadsPerBlock - 1) / threadsPerBlock;

  PointersManager manager(context, "yuv_to_rgb");

  NDArray::prepareSpecialUse({&output}, {&input});
  BUILD_SINGLE_SELECTOR(input.dataType(), yuvToRgbCudaLauncher,
                        (blocksPerGrid, threadsPerBlock, context->getCudaStream(), input.specialBuffer(),
                         input.specialShapeInfo(), packX->platformOffsets(), output.specialBuffer(),
                         output.specialShapeInfo(), packZ->platformOffsets(), numOfTads, dimC),
                        SD_FLOAT_TYPES);
  NDArray::registerSpecialUse({&output}, {&input});

  manager.synchronize();
}

///////////////////////////////////////////////////////////////////
// for example xShapeInfo = {2,3,4}, zShapeInfo = {2,1,4}
template <typename T>
SD_KERNEL void rgbToGrsCuda(const void* vx, const LongType* xShapeInfo, void* vz, const LongType* zShapeInfo,
                            const int dimC) {
  const auto x = reinterpret_cast<const T*>(vx);
  auto z = reinterpret_cast<T*>(vz);

  __shared__ LongType zLen;
  __shared__ LongType rank;
  __shared__ const LongType* xShapePtr;
  __shared__ const LongType* zShapePtr;
  __shared__ const LongType* xStridePtr;

  if (threadIdx.x == 0) {
    zLen = shape::length(zShapeInfo);
    rank = shape::rank(zShapeInfo);
    xShapePtr = shape::shapeOf(xShapeInfo);
    zShapePtr = shape::shapeOf(zShapeInfo);
    xStridePtr = shape::stride(xShapeInfo);
  }
  __syncthreads();

  extern __shared__ unsigned char shmem[];
  auto coords = reinterpret_cast<LongType*>(shmem) + threadIdx.x * rank;

  for (LongType i = blockIdx.x * blockDim.x + threadIdx.x; i < zLen; i += gridDim.x * blockDim.x) {
    // Compute coordinates for the current index
    INDEX2COORDS(i, rank, zShapePtr, coords);

    // Compute z offset
    LongType zOffset;
    COORDS2INDEX(rank, zShapePtr, coords, zOffset);

    // Compute x offsets for R, G, B channels
    LongType xOffset0;
    COORDS2INDEX(rank, xShapePtr, coords, xOffset0);
    const auto xOffset1 = xOffset0 + xStridePtr[dimC];
    const auto xOffset2 = xOffset1 + xStridePtr[dimC];

    // Convert RGB to grayscale
    z[zOffset] = 0.2989f * x[xOffset0] + 0.5870f * x[xOffset1] + 0.1140f * x[xOffset2];
  }
}

///////////////////////////////////////////////////////////////////
template <typename T>
void rgbToGrsCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const int sharedMem,
                          const cudaStream_t* stream, const void* vx, const LongType* xShapeInfo, void* vz,
                          const LongType* zShapeInfo, const int dimC) {
  rgbToGrsCuda<T><<<blocksPerGrid, threadsPerBlock, sharedMem, *stream>>>(vx, xShapeInfo, vz, zShapeInfo, dimC);
  sd::DebugHelper::checkErrorCode(const_cast<cudaStream_t *>(stream), "rgbToGrsCuda failed");

}

///////////////////////////////////////////////////////////////////
void transformRgbGrs(LaunchContext* context, NDArray& input, NDArray& output, const int dimC) {
  PointersManager manager(context, "rgbToGrs");

  const int threadsPerBlock = SD_MAX_NUM_THREADS / 4;
  const int blocksPerGrid = (input.lengthOf() + threadsPerBlock - 1) / threadsPerBlock;
  const int sharedMem = input.rankOf() * sizeof(LongType) * threadsPerBlock + 128;

  NDArray::prepareSpecialUse({&output}, {&input});
  BUILD_SINGLE_SELECTOR(input.dataType(), rgbToGrsCudaLauncher,
                        (blocksPerGrid, threadsPerBlock, sharedMem, context->getCudaStream(), input.specialBuffer(),
                         input.specialShapeInfo(), output.specialBuffer(), output.specialShapeInfo(), dimC),
                        SD_NUMERIC_TYPES);
  NDArray::registerSpecialUse({&output}, {&input});

  manager.synchronize();
}

///////////////////////////////////////////////////////////////////
template <typename T>
static void SD_KERNEL rgbToHsvCuda(const void* vx, const LongType* xShapeInfo, const LongType* xTadOffsets,
                                   void* vz, const LongType* zShapeInfo, const LongType* zTadOffsets,
                                   const LongType numOfTads, const int dimC) {
  const T* x = reinterpret_cast<const T*>(vx);
  T* z = reinterpret_cast<T*>(vz);

  __shared__ int rank;
  __shared__ LongType xDimCstride, zDimCstride;

  if (threadIdx.x == 0) {
    rank = shape::rank(xShapeInfo);
    xDimCstride = shape::stride(xShapeInfo)[dimC];
    zDimCstride = shape::stride(zShapeInfo)[dimC];
  }
  __syncthreads();

  const auto tid = blockIdx.x * blockDim.x + threadIdx.x;

  for (LongType i = tid; i < numOfTads; i += gridDim.x * blockDim.x) {
    const T* xTad = x + xTadOffsets[i];
    T* zTad = z + zTadOffsets[i];

    rgbToHsv<T>(xTad[0], xTad[xDimCstride], xTad[2 * xDimCstride], zTad[0], zTad[zDimCstride], zTad[2 * zDimCstride]);
  }
}

///////////////////////////////////////////////////////////////////
template <typename T>
static void SD_KERNEL hsvToRgbCuda(const void* vx, const LongType* xShapeInfo, const LongType* xTadOffsets,
                                   void* vz, const LongType* zShapeInfo, const LongType* zTadOffsets,
                                   const LongType numOfTads, const int dimC) {
  const T* x = reinterpret_cast<const T*>(vx);
  T* z = reinterpret_cast<T*>(vz);

  __shared__ int rank;
  __shared__ LongType xDimCstride, zDimCstride;

  if (threadIdx.x == 0) {
    rank = shape::rank(xShapeInfo);
    xDimCstride = shape::stride(xShapeInfo)[dimC];
    zDimCstride = shape::stride(zShapeInfo)[dimC];
  }
  __syncthreads();

  const auto tid = blockIdx.x * blockDim.x + threadIdx.x;

  for (LongType i = tid; i < numOfTads; i += gridDim.x * blockDim.x) {
    const T* xTad = x + xTadOffsets[i];
    T* zTad = z + zTadOffsets[i];

    hsvToRgb<T>(xTad[0], xTad[xDimCstride], xTad[2 * xDimCstride], zTad[0], zTad[zDimCstride], zTad[2 * zDimCstride]);
  }
}

///////////////////////////////////////////////////////////////////
template <typename T>
static SD_HOST void hsvToRgbCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const int sharedMem,
                                         const cudaStream_t* stream, const void* vx, const LongType* xShapeInfo,
                                         const LongType* xTadOffsets, void* vz, const LongType* zShapeInfo,
                                         const LongType* zTadOffsets, const LongType numOfTads,
                                         const int dimC) {
  hsvToRgbCuda<T><<<blocksPerGrid, threadsPerBlock, sharedMem, *stream>>>(vx, xShapeInfo, xTadOffsets, vz, zShapeInfo,
                                                                    zTadOffsets, numOfTads, dimC);
  sd::DebugHelper::checkErrorCode(const_cast<cudaStream_t *>(stream), "hsvToRgbCuda failed");

}

template <typename T>
static SD_HOST void rgbToHsvCudaLauncher(const int blocksPerGrid, const int threadsPerBlock, const int sharedMemory,
                                         const cudaStream_t* stream, const void* vx, const LongType* xShapeInfo,
                                         const LongType* xTadOffsets, void* vz, const LongType* zShapeInfo,
                                         const LongType* zTadOffsets, const LongType numOfTads,
                                         const int dimC) {
  rgbToHsvCuda<T><<<blocksPerGrid, threadsPerBlock, sharedMemory, *stream>>>(vx, xShapeInfo, xTadOffsets, vz, zShapeInfo,
                                                                    zTadOffsets, numOfTads, dimC);
  sd::DebugHelper::checkErrorCode(const_cast<cudaStream_t *>(stream), "rgbToHsvCuda failed");

}

///////////////////////////////////////////////////////////////////
void transformHsvRgb(LaunchContext* context, NDArray* input, NDArray* output, const int dimC) {
  auto packX = ConstantTadHelper::getInstance().tadForDimensions(input->shapeInfo(), {dimC});
  auto packZ = ConstantTadHelper::getInstance().tadForDimensions(output->shapeInfo(), {dimC});

  const LongType numOfTads = packX->numberOfTads();

  dim3 launchDims = imageHelper(numOfTads);
  PointersManager manager(context, "hsv_to_rgb");

  NDArray::prepareSpecialUse({output}, {input});
  BUILD_SINGLE_SELECTOR(input->dataType(), hsvToRgbCudaLauncher,
                        (launchDims.y, launchDims.x, launchDims.z,context->getCudaStream(), input->specialBuffer(),
                         input->specialShapeInfo(), packX->platformOffsets(), output->specialBuffer(),
                         output->specialShapeInfo(), packZ->platformOffsets(), numOfTads, dimC),
                        SD_FLOAT_TYPES);
  NDArray::registerSpecialUse({output}, {input});

  manager.synchronize();
}

///////////////////////////////////////////////////////////////////
void transformRgbHsv(LaunchContext* context, NDArray* input, NDArray* output, const int dimC) {
  auto packX = ConstantTadHelper::getInstance().tadForDimensions(input->shapeInfo(), {dimC});
  auto packZ = ConstantTadHelper::getInstance().tadForDimensions(output->shapeInfo(), {dimC});

  const LongType numOfTads = packX->numberOfTads();

  dim3 launchDims = imageHelper(numOfTads);

  PointersManager manager(context, "rgb_to_hsv");

  NDArray::prepareSpecialUse({output}, {input});
  BUILD_SINGLE_SELECTOR(input->dataType(), rgbToHsvCudaLauncher,
                        (launchDims.y, launchDims.x,launchDims.z, context->getCudaStream(), input->specialBuffer(),
                         input->specialShapeInfo(), packX->platformOffsets(), output->specialBuffer(),
                         output->specialShapeInfo(), packZ->platformOffsets(), numOfTads, dimC),
                        SD_FLOAT_TYPES);
  NDArray::registerSpecialUse({output}, {input});

  manager.synchronize();
}

template <typename T>
static SD_KERNEL void tripleTransformerCuda(const void* vx, const LongType* xShapeInfo,
                                            const LongType* xTadShapeInfo, const LongType* xOffsets, void* vz,
                                            const LongType* zShapeInfo, const LongType* zTadShapeInfo,
                                            const LongType* zOffsets, const int dimC, int mode, uint64_t numTads) {
  const auto x = reinterpret_cast<const T*>(vx);
  auto z = reinterpret_cast<T*>(vz);

  __shared__ LongType zLen, *sharedMem;
  __shared__ int rank;  // xRank == zRank

  float yiqarr[3][3] = {
      {0.299f, 0.59590059f, 0.2115f}, {0.587f, -0.27455667f, -0.52273617f}, {0.114f, -0.32134392f, 0.31119955f}};

  float rgbarr[3][3] = {
      {1.f, 1.f, 1.f}, {0.95598634f, -0.27201283f, -1.10674021f}, {0.6208248f, -0.64720424f, 1.70423049f}};

  auto tr = mode == 1 ? yiqarr : rgbarr;

  if (threadIdx.x == 0) {
    extern __shared__ unsigned char shmem[];
    sharedMem = reinterpret_cast<LongType*>(shmem);

    zLen = shape::length(zShapeInfo);
    rank = shape::rank(zShapeInfo);
  }
  __syncthreads();

  LongType* coords = sharedMem + threadIdx.x * rank;

  if (dimC == (rank - 1) && 'c' == shape::order(xShapeInfo) && 1 == shape::elementWiseStride(xShapeInfo) &&
      'c' == shape::order(zShapeInfo) && 1 == shape::elementWiseStride(zShapeInfo)) {
    for (uint64_t f = blockIdx.x * blockDim.x + threadIdx.x; f < zLen / 3; f += gridDim.x * blockDim.x) {
      auto i = f * 3;

      auto xi0 = x[i];
      auto xi1 = x[i + 1];
      auto xi2 = x[i + 2];

      for (int e = 0; e < 3; e++) z[i + e] = xi0 * tr[0][e] + xi1 * tr[1][e] + xi2 * tr[2][e];
    }
  } else {
    // TAD based case
    const LongType xDimCstride = shape::stride(xShapeInfo)[dimC];
    const LongType zDimCstride = shape::stride(zShapeInfo)[dimC];

    for (uint64_t i = blockIdx.x * blockDim.x + threadIdx.x; i < numTads; i += blockDim.x * gridDim.x) {
      const T* xTad = x + xOffsets[i];
      T* zTad = z + zOffsets[i];

      auto xi0 = xTad[0];
      auto xi1 = xTad[xDimCstride];
      auto xi2 = xTad[xDimCstride * 2];

      for (int e = 0; e < 3; e++) zTad[zDimCstride * e] = xi0 * tr[0][e] + xi1 * tr[1][e] + xi2 * tr[2][e];
    }
  }
}

template <typename T>
static void rgbYiq(LaunchContext* context, NDArray* input, NDArray* output, const int dimC) {
  auto packX = ConstantTadHelper::getInstance().tadForDimensions(input->shapeInfo(), dimC);
  auto packZ = ConstantTadHelper::getInstance().tadForDimensions(output->shapeInfo(), dimC);

  NDArray::prepareSpecialUse({output}, {input});
  dim3 launchDims = getLaunchDims("image_helpers_triple");
   tripleTransformerCuda<T><<<launchDims.x,launchDims.y, launchDims.z, *context->getCudaStream()>>>(
      input->specialBuffer(), input->specialShapeInfo(), packX->platformShapeInfo(), packX->platformOffsets(),
      output->specialBuffer(), output->specialShapeInfo(), packZ->platformShapeInfo(), packZ->platformOffsets(), dimC, 1,
      packZ->numberOfTads());
  sd::DebugHelper::checkErrorCode(context->getCudaStream(), "tripleTransformerCuda failed");

  NDArray::registerSpecialUse({output}, {input});
}

template <typename T>
SD_INLINE static void yiqRgb(LaunchContext* context, NDArray* input, NDArray* output, const int dimC) {
  auto packX = ConstantTadHelper::getInstance().tadForDimensions(input->shapeInfo(), dimC);
  auto packZ = ConstantTadHelper::getInstance().tadForDimensions(output->shapeInfo(), dimC);

  dim3 launchDims = getLaunchDims("image_helpers_triple");
  NDArray::prepareSpecialUse({output}, {input});
   tripleTransformerCuda<T><<<launchDims.x, launchDims.y,launchDims.z, *context->getCudaStream()>>>(
      input->specialBuffer(), input->specialShapeInfo(), packX->platformShapeInfo(), packX->platformOffsets(),
      output->specialBuffer(), output->specialShapeInfo(), packZ->platformShapeInfo(), packZ->platformOffsets(), dimC, 2,
      packZ->numberOfTads());
  sd::DebugHelper::checkErrorCode(context->getCudaStream(), "tripleTransformerCuda failed");

  NDArray::registerSpecialUse({output}, {input});
}

void transformYiqRgb(LaunchContext* context, NDArray* input, NDArray* output, const int dimC) {
  BUILD_SINGLE_SELECTOR(input->dataType(), yiqRgb, (context, input, output, dimC), SD_FLOAT_TYPES);
}

void transformRgbYiq(LaunchContext* context, NDArray* input, NDArray* output, const int dimC) {
  BUILD_SINGLE_SELECTOR(input->dataType(), rgbYiq, (context, input, output, dimC), SD_FLOAT_TYPES);
}

}  // namespace helpers
}  // namespace ops
}  // namespace sd
