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

package org.nd4j.linalg.api.ops;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import onnx.Onnx;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.listeners.At;
import org.nd4j.autodiff.listeners.Listener;
import org.nd4j.autodiff.samediff.SDVariable;
import org.nd4j.autodiff.samediff.SameDiff;
import org.nd4j.autodiff.samediff.internal.SameDiffOp;
import org.nd4j.common.base.Preconditions;
import org.nd4j.graph.OpType;
import org.nd4j.imports.NoOpNameFoundException;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import org.tensorflow.framework.AttrValue;
import org.tensorflow.framework.GraphDef;
import org.tensorflow.framework.NodeDef;

import java.nio.Buffer;
import java.util.Arrays;
import java.util.Map;

@Data
public abstract class BaseOp extends DifferentialFunction implements Op {

    protected INDArray x, y, z;

    @Getter @Setter
    protected String xVertexId,yVertexId,zVertexId;
    // cached instance, for dataType checks
    protected DataBuffer extraArgz;

    protected INDArray dimensionz;

    public BaseOp() {
    }

    public BaseOp(SameDiff sameDiff, boolean inPlace, Object[] extraArgs) {
        super(sameDiff, inPlace, extraArgs);
    }

    public BaseOp(SameDiff sameDiff, Object[] extraArgs) {
        super(sameDiff, extraArgs);
    }

    /**
     * Specify an alternative result array
     *
     * @param x the input
     * @param z the output array
     */
    public BaseOp(INDArray x, INDArray z) {
        this(x, null, z);
    }


    public BaseOp(INDArray x, INDArray y, INDArray z) {
        super(false);
        this.x = x;
        this.y = y;
        this.z = z;
    }


    /**
     * An op for one ndarray
     *
     * @param x the ndarray
     */
    public BaseOp(INDArray x) {
        this(x, null, x);
    }

    public static Type getOpType(Op op) {
        Type type = null;

        if (op instanceof CustomOp) {
            return Type.CUSTOM;
        } else if (op instanceof TransformOp) {
            if (op.y() == null) {
                type = Type.TRANSFORM_FLOAT;
            } else {
                type = Type.PAIRWISE;
            }
        } else if (op instanceof ReduceOp) {
            if (op.y() == null)
                type = ((ReduceOp) op).getOpType();
            else
                type = Type.REDUCE3;
        } else if (op instanceof ScalarOp) {
            type = Type.SCALAR;
        } else if (op instanceof BroadcastOp) {
            type = Type.BROADCAST;
        } else if (op instanceof IndexAccumulation) {
            type = Type.INDEXREDUCE;
        } else if (op instanceof MetaOp) {
            type = Type.META;
        } else if (op instanceof GridOp) {
            type = Type.GRID;
        }

        return type;
    }



    @Override
    public void initFromTensorFlow(NodeDef nodeDef, SameDiff initWith, Map<String, AttrValue> attributesForNode, GraphDef graph) {

    }

    @Override
    public void initFromOnnx(Onnx.NodeProto node, SameDiff initWith, Map<String, Onnx.AttributeProto> attributesForNode, Onnx.GraphProto graph) {
    }

    @Override
    public DataBuffer extraArgsDataBuff(DataType dtype) {
        if (extraArgz != null)
            return extraArgz;

        if (extraArgs != null) {
            if (Shape.isZ(dtype) || Shape.isB(dtype)) {
                long extraz[] = new long[extraArgs.length];
                for (int i = 0; i < extraArgs.length; i++) {
                    if (extraArgs[i] instanceof Number) {
                        Number arg = (Number) extraArgs[i];
                        long val = arg.longValue();
                        extraz[i] = val;
                    }
                }
                extraArgz = Nd4j.getConstantHandler().getConstantBuffer(extraz, dtype);
                return extraArgz;
            } else if (Shape.isR(dtype)) {
                double extraz[] = new double[extraArgs.length];
                for (int i = 0; i < extraArgs.length; i++) {
                    if (!(extraArgs[i] instanceof Number))
                        continue;
                    Number arg = (Number) extraArgs[i];
                    if (arg == null)
                        arg = 0.0;
                    double val = arg.doubleValue();
                    extraz[i] = val;
                }
                extraArgz = Nd4j.getConstantHandler().getConstantBuffer(extraz, dtype);
                return extraArgz;
            }
        }

        return null;
    }

    @Override
    public Buffer extraArgsBuff() {
        if (extraArgs != null) {
            DataBuffer retBuff;
            if (x.data().dataType() == DataType.FLOAT) {
                retBuff = Nd4j.createBuffer(new float[extraArgs.length]);
                for (int i = 0; i < extraArgs.length; i++) {
                    Number val = (Number) extraArgs[i];
                    retBuff.put(i, val.floatValue());
                }
                return retBuff.asNioFloat();
            } else {
                retBuff = Nd4j.createBuffer(new double[extraArgs.length]);
                for (int i = 0; i < extraArgs.length; i++) {
                    Number val = (Number) extraArgs[i];
                    retBuff.put(i, val.doubleValue());
                }
                return retBuff.asNioDouble();
            }


        }
        return null;
    }

    @Override
    public void setX(INDArray x) {
        this.x = x;
    }

    @Override
    public void setZ(INDArray z) {
        this.z = z;
    }

    @Override
    public void setY(INDArray y) {
        this.y = y;
    }

    @Override
    public Object[] extraArgs() {
        return extraArgs;
    }

    @Override
    public INDArray x() {
        return x;
    }

    @Override
    public INDArray y() {
        return y;
    }


    @Override
    public INDArray z() {
        return z;
    }

    @Override
    public INDArray getInputArgument(int index){
        Preconditions.checkState(index >= 0 && index < 2, "Input argument index must be 0 or 1, got %s", index);
        return index == 0 ? x : y;
    }

    @Override
    public SDVariable[] outputVariables(String baseName) {
        if(zVertexId == null)  {
            val outputNames = sameDiff.getOutputsForOp(this);
            //no need to dynamically create if already exists
            if(outputNames != null) {
                zVertexId = sameDiff.getVariable(outputNames[0]).name();
                SDVariable[] ret =  new SDVariable[]{sameDiff.getVariable(outputNames[0])};
                return ret;

            }

            if(isInPlace()) {
                val newVars = sameDiff.generateOutputVariableForOp(this,null,false);
                val inputArr = x();
                //in place op
                if(inputArr == null) {
                    computeVariables(newVars);
                    return newVars;
                }

                sameDiff.setArrayForVariable(newVars[0].name(),inputArr);
                z = inputArr;
                if(sameDiff.getOutputsForOp(this) == null)
                    sameDiff.addOutgoingFor(newVars,this);
                computeVariables(newVars);

                return newVars;
            }

            SDVariable[] newVars = sameDiff.generateOutputVariableForOp(this, baseName, false);
            computeVariables(newVars);
            if (sameDiff.getOutputsForOp(this) == null)
                sameDiff.addOutgoingFor(newVars, this);
            return newVars;
        }

        return new SDVariable[]{sameDiff.getVariable(zVertexId)};
    }

    /**
     * Compute the output vars using this op
     * and store them in the samediff instance.
     * @param newVars the new variables to compute arrays for
     */
    public void computeVariables(SDVariable[] newVars) {
        if(sameDiff.isEagerMode()) {
            SDVariable[] args = args();
            if(args.length == 1) {
                x = args[0].getArr();
            } else if(args.length > 1) {
                x = args[0].getArr();
                if(this.opType()  == Type.REDUCE3 ||
                        this.opType() == Type.PAIRWISE_BOOL
                        || this.opType() == Type.TRANSFORM_SAME)
                    y = args[1].getArr();
                else if((opType() == Type.REDUCE_FLOAT || opType() == Type.REDUCE_LONG || opType() == Type.REDUCE_BOOL  || opType() == Type.REDUCE_BOOL || opType() == Type.REDUCE_SAME) && args.length > 1) {
                    this.dimensionz = args[1].getArr();
                    if(!args[1].getArr().isEmpty())
                        this.dimensions = args[1].getArr().toLongVector();
                    else
                        this.dimensions = new long[0];
                }
            }

            if(x == null) {
                throw new IllegalArgumentException("No variable found for the given input variables of " +  args[0].name() + " At least one input required.");
            }

            //ensure data types are correct
            if(args.length > 0 && args[0].dataType() != null) {
                x = x.castTo(args[0].dataType());
            }

            //can be reduce float op or something similar where dimensions were specified
            //as an input
            if(args.length > 1 && args[1].dataType() != null && y != null) {
                y = y.castTo(args[1].dataType());
            }

            if(z == null) {
                if(!(this instanceof ReduceOp)) {
                    if(x.isEmpty()) {
                        setZ(Nd4j.emptyWithShape(x.shape(),x.dataType()));
                    }
                    else {
                        setZ(Nd4j.zeros(x.shape()).castTo(newVars[0].dataType()).detach());
                    }
                }  else {
                    if(this instanceof BaseReduceOp) {
                        if(dimensions == null && dimensionz != null)
                            dimensions = dimensionz.ravel().toLongVector();
                        BaseReduceOp baseReduceOp = (BaseReduceOp) this;
                        setZ(Nd4j.create(Shape.reductionShape(x,dimensions,true,baseReduceOp.keepDims)).castTo(newVars[0].dataType()).detach());
                    } else {
                        setZ(Nd4j.create(Shape.reductionShape(x,dimensions,true,false)).castTo(newVars[0].dataType()).detach());

                    }
                }
            }

            if(this instanceof BaseScalarOp) {
                BaseScalarOp baseScalarOp = (BaseScalarOp) this;
                if(baseScalarOp.scalar() != null) {
                    if(baseScalarOp.scalar().dataType() != baseScalarOp.x().dataType()) {
                        baseScalarOp.setScalar(baseScalarOp.scalar().castTo(x().dataType()));
                    }
                }
            }


            try(OpContext ctx = Nd4j.getExecutioner().buildContext()) {
                if(y == null)
                    ctx.setInputArrays(x);
                else if(y != null) {
                    ctx.setInputArrays(x,y);
                }

                ctx.setOutputArrays(z);

                SameDiffOp op2 = sameDiff.getOps().get(getOwnName());
                for(Listener l : sameDiff.getListeners()) {
                    l.preOpExecution(sameDiff, At.defaultAt(),op2,ctx);
                }

                INDArray exec = Nd4j.getExecutioner().exec(this,ctx);
                for(Listener  l : sameDiff.getListeners()) {
                    l.opExecution(sameDiff, At.defaultAt(),null,op2,ctx,new INDArray[]{exec});
                }

                for(Listener  l : sameDiff.getListeners()) {
                    l.preUpdate(sameDiff,At.defaultAt(),sameDiff.getVariables().get(outputVariable().name()),z);

                }


            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            INDArray exec = Nd4j.getExecutioner().exec(this);
            for (int i = 0; i < newVars.length; i++) {
                newVars[i].setShape(exec.shape());
                sameDiff.setEagerArrForVarName(newVars[i].name(),exec);
            }
        }
    }


    @Override
    public String toString() {
        return opName();
    }


    @Override
    public CustomOp toCustomOp() {
        DynamicCustomOp.DynamicCustomOpsBuilder customOpBuilder = DynamicCustomOp.builder(opName());
        customOpBuilder.callInplace(x() == z());

        if (y() != null)
            customOpBuilder.addInputs(x(), y());
        else
            customOpBuilder.addInputs(x());

        customOpBuilder.addOutputs(z());
        if (extraArgs != null) {
            for (int i = 0; i < extraArgs.length; i++) {
                if (extraArgs[i] instanceof Integer) {
                    customOpBuilder.addIntegerArguments((Integer) extraArgs[i]);
                } else if (extraArgs[i] instanceof Double || extraArgs[i] instanceof Float) {
                    Double num = (Double) extraArgs[i];
                    customOpBuilder.addFloatingPointArguments(num);
                }
            }
        }

        return customOpBuilder.build();

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseOp baseOp = (BaseOp) o;

        if (x != null ? !x.equals(baseOp.x) : baseOp.x != null) return false;
        if (y != null ? !y.equals(baseOp.y) : baseOp.y != null) return false;
        if (z != null ? !z.equals(baseOp.z) : baseOp.z != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(extraArgs, baseOp.extraArgs)) return false;
        return extraArgz != null ? extraArgz.equals(baseOp.extraArgz) : baseOp.extraArgz == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        result = 31 * result + (z != null ? z.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(extraArgs);
        result = 31 * result + (extraArgz != null ? extraArgz.hashCode() : 0);
        return result;
    }

    protected void defineDimensions(long... dimensions) {
        if (dimensions != null && dimensions.length > 0) {
            if(x != null) {
                dimensions = Shape.normalizeAxis(x.rank(), dimensions);
            }
        }

        if (dimensions == null || dimensions.length == 0)
            dimensions = new long[]{Integer.MAX_VALUE};

        this.dimensionz = Shape.ndArrayDimFromLong(dimensions).detach();

    }

    public long[] dimensionsArr() {
        return dimensions;
    }
    public INDArray dimensions() {
        return dimensionz;
    }

    public Number getFinalResult() {
        if (this.z == null)
            throw new ND4JIllegalStateException("Op.Z is null. Op wasn't executed yet?");

        if (z.isEmpty())
            throw new ND4JIllegalStateException("Can't get number from empty array");

        if (!z.isScalar())
            throw new ND4JIllegalStateException("Can't get final result scalar out of N-dim tensor");

        if (z.isR())
            return new Double(z.getDouble(0));
        else if (z.isZ())
            return new Long(z.getInt(0));
        else if (z.isB())
            return new Integer(z.getInt(0));

        throw new ND4JIllegalStateException("???");
    }

    @Override
    public int getNumOutputs(){
        //Always 1 for legacy/base ops
        return 1;
    }

    @Override
    public void clearArrays(){
        x = null;
        y = null;
        z = null;
    }

    @Override
    public String onnxName() {
        throw new NoOpNameFoundException("No onnx op opName found for " + opName());
    }

    @Override
    public String tensorflowName() {
        throw new NoOpNameFoundException("No tensorflow op opName found for " + opName());
    }

}
