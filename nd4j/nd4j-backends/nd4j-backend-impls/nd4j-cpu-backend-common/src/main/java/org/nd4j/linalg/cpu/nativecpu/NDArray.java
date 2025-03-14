/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.nd4j.linalg.cpu.nativecpu;


import com.google.flatbuffers.FlatBufferBuilder;
import lombok.val;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.common.base.Preconditions;
import org.nd4j.graph.FlatArray;
import org.nd4j.linalg.api.buffer.*;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.BaseNDArray;
import org.nd4j.linalg.api.ndarray.BaseNDArrayProxy;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ndarray.JvmShapeInfo;
import org.nd4j.linalg.api.ops.performance.PerformanceTracker;
import org.nd4j.linalg.api.shape.LongShapeDescriptor;
import org.nd4j.linalg.cpu.nativecpu.buffer.DoubleBuffer;
import org.nd4j.linalg.cpu.nativecpu.buffer.FloatBuffer;
import org.nd4j.linalg.cpu.nativecpu.buffer.LongBuffer;
import org.nd4j.linalg.cpu.nativecpu.buffer.Utf8Buffer;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.memory.MemcpyDirection;
import org.nd4j.linalg.workspace.WorkspaceUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;


public class NDArray extends BaseNDArray {
    static {
        //invoke the override
        Nd4j.getBlasWrapper();
    }

    public NDArray() {
        super();
    }

    public NDArray(LongShapeDescriptor descriptor) {
        super(descriptor);
    }

    public NDArray(DataBuffer buffer, LongBuffer shapeInfo, long[] javaShapeInfo) {
        this.jvmShapeInfo = new JvmShapeInfo(javaShapeInfo);
        this.data = buffer;
    }

    public NDArray(DataBuffer buffer) {
        super(buffer);
    }

    public NDArray(DataBuffer buffer, int[] shape, int[] stride, long offset, char ordering) {
        super(buffer, shape, stride, offset, ordering);
    }

    public NDArray(DataBuffer buffer, long[] shape, long[] stride, long offset, char ordering) {
        super(buffer, shape, stride, offset, ordering);
    }


    public NDArray(DataBuffer buffer, long[] shape, long[] stride, long offset, long ews, char ordering,boolean isView) {
        super(buffer, shape, stride, offset, ews, ordering,isView);
    }
    public NDArray(DataBuffer buffer, long[] shape, long[] stride, long offset, long ews, char ordering) {
        super(buffer, shape, stride, offset, ews, ordering);
    }

    public NDArray(DataBuffer buffer, long[] shape, long[] stride, long offset, char ordering, DataType dataType) {
        super(buffer, shape, stride, offset, ordering, dataType);
    }

    public NDArray(double[][] data) {
        super(data);
    }

    public NDArray(double[][] data, char ordering) {
        super(data, ordering);
    }

    /**
     * Create this ndarray with the given data and shape and 0 offset
     *
     * @param data     the data to use
     * @param shape    the shape of the ndarray
     * @param ordering
     */
    public NDArray(float[] data, int[] shape, char ordering) {
        super(data, shape, ordering);
    }

    /**
     * @param data     the data to use
     * @param shape    the shape of the ndarray
     * @param offset   the desired offset
     * @param ordering the ordering of the ndarray
     */
    public NDArray(float[] data, int[] shape, long offset, char ordering) {
        super(data, shape, offset, ordering);
    }

    public NDArray(float[] data, long[] shape, long offset, char ordering) {
        super(data, shape, offset, ordering);
    }

    public NDArray(float[] data, long[] shape, long[] stride, long offset, char ordering) {
        super(data, shape, stride, offset, ordering);
    }

    public NDArray(double[] data, long[] shape, long[] stride, long offset, char ordering) {
        super(data, shape, stride, offset, ordering);
    }

    public NDArray(DataBuffer data, long[] shape, long[] stride, char ordering, DataType type) {
        super(data, shape, stride, ordering, type);
    }

    public NDArray(DataBuffer data, long[] shape, long[] stride, char ordering, DataType type, MemoryWorkspace workspace) {
        super(data, shape, stride, ordering, type, workspace);
    }

    public NDArray(double[] data, long[] shape, long offset, char ordering) {
        super(data, shape, offset, ordering);
    }

    /**
     * Construct an ndarray of the specified shape
     * with an empty data array
     *
     * @param shape    the shape of the ndarray
     * @param stride   the stride of the ndarray
     * @param offset   the desired offset
     * @param ordering the ordering of the ndarray
     */
    public NDArray(int[] shape, int[] stride, long offset, char ordering) {
        super(shape, stride, offset, ordering);
    }

    public NDArray(long[] shape, long[] stride, long offset, char ordering) {
        super(shape, stride, offset, ordering);
    }

    public NDArray(DataBuffer buffer, LongShapeDescriptor longShapeDescriptor) {
        super(buffer, longShapeDescriptor);
    }

    /**
     * Construct an ndarray of the specified shape, with optional initialization
     *
     * @param shape    the shape of the ndarray
     * @param stride   the stride of the ndarray
     * @param offset   the desired offset
     * @param ordering the ordering of the ndarray
     * @param initialize Whether to initialize the INDArray. If true: initialize. If false: don't.
     */
    public NDArray(int[] shape, int[] stride, long offset, char ordering, boolean initialize) {
        super(shape, stride, offset, ordering, initialize);
    }

    public NDArray(long[] shape, long[] stride, long offset, char ordering, boolean initialize) {
        super(shape, stride, offset, ordering, initialize);
    }

    public NDArray(DataType type, long[] shape, long[] stride, long offset, char ordering) {
        super(type, shape, stride, offset, ordering, true);
    }

    public NDArray(DataType type, long[] shape, long[] stride, long offset, char ordering, MemoryWorkspace workspace) {
        super(type, shape, stride, offset, ordering, true, workspace);
    }

    public NDArray(DataType type, long[] shape, long[] stride, long offset, char ordering, boolean initialize) {
        super(type, shape, stride, offset, ordering, initialize);
    }

    public NDArray(DataType type, long[] shape, long[] stride, long offset, char ordering, boolean initialize, MemoryWorkspace workspace) {
        super(type, shape, stride, offset, ordering, initialize, workspace);
    }

    /**
     * Create the ndarray with
     * the specified shape and stride and an offset of 0
     *
     * @param shape    the shape of the ndarray
     * @param stride   the stride of the ndarray
     * @param ordering the ordering of the ndarray
     */
    public NDArray(int[] shape, int[] stride, char ordering) {
        super(shape, stride, ordering);
    }

    public NDArray(int[] shape, long offset, char ordering) {
        super(shape, offset, ordering);
    }

    public NDArray(int[] shape) {
        super(shape);
    }

    /**
     * Creates a new <i>n</i> times <i>m</i> <tt>DoubleMatrix</tt>.
     *
     * @param newRows    the number of rows (<i>n</i>) of the new matrix.
     * @param newColumns the number of columns (<i>m</i>) of the new matrix.
     * @param ordering
     */
    public NDArray(int newRows, int newColumns, char ordering) {
        super(newRows, newColumns, ordering);
    }

    public NDArray(long newRows, long newColumns, char ordering) {
        super(newRows, newColumns, ordering);
    }

    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices   the slices to merge
     * @param shape    the shape of the ndarray
     * @param ordering
     */
    public NDArray(List<INDArray> slices, int[] shape, char ordering) {
        super(slices, shape, ordering);
    }

    public NDArray(List<INDArray> slices, long[] shape, char ordering) {
        super(slices, shape, ordering);
    }

    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices   the slices to merge
     * @param shape    the shape of the ndarray
     * @param stride
     * @param ordering
     */
    public NDArray(List<INDArray> slices, int[] shape, int[] stride, char ordering) {
        super(slices, shape, stride, ordering);
    }

    public NDArray(float[] data, int[] shape, int[] stride, char ordering) {
        super(data, shape, stride, ordering);
    }

    public NDArray(float[] data, int[] shape, int[] stride, long offset, char ordering) {
        super(data, shape, stride, offset, ordering);
    }

    /**
     * Create this ndarray with the given data and shape and 0 offset
     *
     * @param data  the data to use
     * @param shape the shape of the ndarray
     */
    public NDArray(float[] data, int[] shape) {
        super(data, shape);
    }

    public NDArray(float[] data, int[] shape, long offset) {
        super(data, shape, offset);
    }

    /**
     * Construct an ndarray of the specified shape
     * with an empty data array
     *
     * @param shape  the shape of the ndarray
     * @param stride the stride of the ndarray
     * @param offset the desired offset
     */
    public NDArray(int[] shape, int[] stride, long offset) {
        super(shape, stride, offset);
    }

    public NDArray(long[] shape, long[] stride, long offset) {
        super(shape, stride, offset);
    }

    /**
     * Create the ndarray with
     * the specified shape and stride and an offset of 0
     *
     * @param shape  the shape of the ndarray
     * @param stride the stride of the ndarray
     */
    public NDArray(int[] shape, int[] stride) {
        super(shape, stride);
    }

    public NDArray(int[] shape, long offset) {
        super(shape, offset);
    }

    public NDArray(int[] shape, char ordering) {
        super(shape, ordering);
    }

    /**
     * Creates a new <i>n</i> times <i>m</i> <tt>DoubleMatrix</tt>.
     *
     * @param newRows    the number of rows (<i>n</i>) of the new matrix.
     * @param newColumns the number of columns (<i>m</i>) of the new matrix.
     */
    public NDArray(int newRows, int newColumns) {
        super(newRows, newColumns);
    }

    public NDArray(long newRows, long newColumns) {
        super(newRows, newColumns);
    }

    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices the slices to merge
     * @param shape  the shape of the ndarray
     */
    public NDArray(List<INDArray> slices, int[] shape) {
        super(slices, shape);
    }

    public NDArray(List<INDArray> slices, long[] shape) {
        super(slices, shape);
    }

    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices the slices to merge
     * @param shape  the shape of the ndarray
     * @param stride
     */
    public NDArray(List<INDArray> slices, int[] shape, int[] stride) {
        super(slices, shape, stride);
    }

    public NDArray(List<INDArray> slices, long[] shape, long[] stride) {
        super(slices, shape, stride);
    }

    public NDArray(float[] data, int[] shape, int[] stride) {
        super(data, shape, stride);
    }


    public NDArray(float[] data, int[] shape, int[] stride, long offset) {
        super(data, shape, stride, offset);
    }

    public NDArray(float[] data) {
        super(data);
    }



    public NDArray(double[] data, int[] shape, int[] stride, long offset) {
        super(data, shape, stride, offset);
    }

    public NDArray(float[][] floats) {
        super(floats);
    }

    public NDArray(float[][] data, char ordering) {
        super(data, ordering);
    }

    public NDArray(DataBuffer data, int[] shape, int[] stride, long offset) {
        super(data, shape, stride, offset);

    }

    public NDArray(int[] data, int[] shape, int[] strides) {
        super(data, shape, strides);
    }

    public NDArray(DataBuffer data, int[] shape) {
        super(data, shape);
    }

    public NDArray(DataBuffer data, long[] shape) {
        super(data, shape);
    }

    public NDArray(DataBuffer buffer, int[] shape, long offset) {
        super(buffer, shape, offset);
    }

    public NDArray(DataBuffer buffer, int[] shape, char ordering) {
        super(buffer, shape, ordering);
    }

    public NDArray(double[] data, int[] shape, char ordering) {
        super(data, shape, ordering);
    }

    public NDArray(double[] data, long[] shape, char ordering) {
        super(data, shape, ordering);
    }

    public NDArray(double[] data, int[] shape, int[] stride, long offset, char ordering) {
        super(data, shape, stride, offset, ordering);
    }

    public NDArray(float[] data, char order) {
        super(data, order);
    }

    public NDArray(FloatBuffer floatBuffer, char order) {
        super(floatBuffer, order);
    }

    public NDArray(DataBuffer buffer, int[] shape, int[] strides) {
        super(buffer, shape, strides);
    }

    public NDArray(DoubleBuffer buffer, int[] shape, char ordering) {
        super(buffer, shape, 0, ordering);
    }

    public NDArray(DoubleBuffer buffer, int[] shape, long offset) {
        super(buffer, shape, offset);
    }

    public NDArray(int[] shape, DataBuffer buffer) {
        super(shape, buffer);
    }

    public NDArray(DataType dataType, long[] shape, long[] paddings, long[] paddingOffsets, char ordering,
            MemoryWorkspace workspace){
        super(dataType, shape, paddings, paddingOffsets, ordering, workspace);
    }

    public NDArray(DataType dataType, long[] shape, long[] strides, MemoryWorkspace currentWorkspace) {
        super(dataType, shape, strides, currentWorkspace);
    }

    private Object writeReplace() throws java.io.ObjectStreamException {
        return new BaseNDArrayProxy(this);
    }

    @Override
    public INDArray unsafeDuplication() {
        WorkspaceUtils.assertValidArray(this, "Cannot duplicate array");
        if (isView())
            return this.dup(this.ordering());

        DataBuffer rb = Nd4j.getMemoryManager().getCurrentWorkspace() == null ? Nd4j.getDataBufferFactory().createSame(this.data, false) : Nd4j.getDataBufferFactory().createSame(this.data, false, Nd4j.getMemoryManager().getCurrentWorkspace());

        INDArray ret = Nd4j.createArrayFromShapeBuffer(rb, this.shapeInfoDataBuffer());

        val perfD = PerformanceTracker.getInstance().helperStartTransaction();

        Pointer.memcpy(ret.data().addressPointer(), this.data().addressPointer(), this.data().length() * this.data().getElementSize());

        PerformanceTracker.getInstance().helperRegisterTransaction(0, perfD, this.data().length() * this.data().getElementSize(), MemcpyDirection.HOST_TO_HOST);

        return ret;
    }

    @Override
    public INDArray unsafeDuplication(boolean blocking) {
        return unsafeDuplication();
    }


    @Override
    public LongShapeDescriptor shapeDescriptor() {
        return LongShapeDescriptor.fromShape(shape(), stride(), -1, ordering(), dataType(), isEmpty());
    }

    protected int stringBuffer(FlatBufferBuilder builder, DataBuffer buffer) {
        Preconditions.checkArgument(buffer.dataType() == DataType.UTF8, "This method can be called on UTF8 buffers only");
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            val numWords = this.length();
            val ub = (Utf8Buffer) buffer;
            // writing length first
            val t = length();
            val ptr = (BytePointer) ub.pointer();

            // now write all strings as bytes
            for (int i = 0; i < ub.length(); i++) {
                dos.writeByte(ptr.get(i));
            }

            val bytes = bos.toByteArray();
            return FlatArray.createBufferVector(builder, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(long index) {
        if (!isS())
            throw new UnsupportedOperationException("This method is usable only on String dataType, but got [" + this.dataType() + "]");

        return ((Utf8Buffer) data).getString(index);
    }
}
