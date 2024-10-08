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

package org.deeplearning4j.nn.layers.wrapper;

import lombok.Data;
import lombok.NonNull;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.MaskState;
import org.deeplearning4j.nn.api.TrainingConfig;
import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.deeplearning4j.optimize.api.ConvexOptimizer;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.common.primitives.Pair;

import java.util.Collection;
import java.util.Map;

@Data
public abstract class BaseWrapperLayer implements Layer {

    protected Layer underlying;

    public BaseWrapperLayer(@NonNull Layer underlying){
        this.underlying = underlying;
    }

    @Override
    public void setCacheMode(CacheMode mode) {
        underlying.setCacheMode(mode);
    }

    @Override
    public double calcRegularizationScore(boolean backpropParamsOnly){
        return underlying.calcRegularizationScore(backpropParamsOnly);
    }

    @Override
    public Type type() {
        return underlying.type();
    }

    @Override
    public Pair<Gradient, INDArray> backpropGradient(INDArray epsilon, LayerWorkspaceMgr workspaceMgr) {
        return underlying.backpropGradient(epsilon, workspaceMgr);
    }

    @Override
    public INDArray activate(boolean training, LayerWorkspaceMgr workspaceMgr) {
        return underlying.activate(training, workspaceMgr);
    }

    @Override
    public INDArray activate(INDArray input, boolean training, LayerWorkspaceMgr workspaceMgr) {
        return underlying.activate(input, training, workspaceMgr);
    }

    @Override
    public Collection<TrainingListener> getListeners() {
        return underlying.getListeners();
    }

    @Override
    public void setListeners(TrainingListener... listeners) {
        underlying.setListeners(listeners);
    }

    @Override
    public void addListeners(TrainingListener... listener) {
        underlying.addListeners(listener);
    }

    @Override
    public void fit() {
        underlying.fit();
    }

    @Override
    public void update(Gradient gradient) {
        underlying.update(gradient);
    }

    @Override
    public void update(INDArray gradient, String paramType) {
        underlying.update(gradient, paramType);
    }

    @Override
    public double score() {
        return underlying.score();
    }

    @Override
    public void computeGradientAndScore(LayerWorkspaceMgr workspaceMgr) {
        underlying.computeGradientAndScore(workspaceMgr);
    }

    @Override
    public INDArray params() {
        return underlying.params();
    }

    @Override
    public long numParams() {
        return underlying.numParams();
    }

    @Override
    public long numParams(boolean backwards) {
        return underlying.numParams();
    }

    @Override
    public void setParams(INDArray params) {
        underlying.setParams(params);
    }

    @Override
    public void setParamsViewArray(INDArray params) {
        underlying.setParamsViewArray(params);
    }

    @Override
    public INDArray getGradientsViewArray() {
        return underlying.getGradientsViewArray();
    }

    @Override
    public void setBackpropGradientsViewArray(INDArray gradients) {
        underlying.setBackpropGradientsViewArray(gradients);
    }

    @Override
    public void fit(INDArray data, LayerWorkspaceMgr workspaceMgr) {
        underlying.fit(data, workspaceMgr);
    }

    @Override
    public Gradient gradient() {
        return underlying.gradient();
    }

    @Override
    public Pair<Gradient, Double> gradientAndScore() {
        return underlying.gradientAndScore();
    }

    @Override
    public int batchSize() {
        return underlying.batchSize();
    }

    @Override
    public NeuralNetConfiguration conf() {
        return underlying.conf();
    }

    @Override
    public void setConf(NeuralNetConfiguration conf) {
        underlying.setConf(conf);
    }

    @Override
    public INDArray input() {
        return underlying.input();
    }

    @Override
    public ConvexOptimizer getOptimizer() {
        return underlying.getOptimizer();
    }

    @Override
    public INDArray getParam(String param) {
        return underlying.getParam(param);
    }

    @Override
    public Map<String, INDArray> paramTable() {
        return underlying.paramTable();
    }

    @Override
    public Map<String, INDArray> paramTable(boolean backpropParamsOnly) {
        return underlying.paramTable(backpropParamsOnly);
    }

    @Override
    public void setParamTable(Map<String, INDArray> paramTable) {
        underlying.setParamTable(paramTable);
    }

    @Override
    public void setParam(String key, INDArray val) {
        underlying.setParam(key, val);
    }

    @Override
    public void clear() {
        underlying.clear();
    }

    @Override
    public void applyConstraints(int iteration, int epoch) {
        underlying.applyConstraints(iteration, epoch);
    }

    @Override
    public void init() {
        underlying.init();
    }

    @Override
    public void setListeners(Collection<TrainingListener> listeners) {
        underlying.setListeners(listeners);
    }

    @Override
    public void setIndex(int index) {
        underlying.setIndex(index);
    }

    @Override
    public int getIndex() {
        return underlying.getIndex();
    }

    @Override
    public int getIterationCount() {
        return underlying.getIterationCount();
    }

    @Override
    public int getEpochCount() {
        return underlying.getEpochCount();
    }

    @Override
    public void setIterationCount(int iterationCount) {
        underlying.setIterationCount(iterationCount);
    }

    @Override
    public void setEpochCount(int epochCount) {
        underlying.setEpochCount(epochCount);
    }

    @Override
    public void setInput(INDArray input, LayerWorkspaceMgr workspaceMgr) {
        underlying.setInput(input, workspaceMgr);
    }

    @Override
    public void setInputMiniBatchSize(int size) {
        underlying.setInputMiniBatchSize(size);
    }

    @Override
    public int getInputMiniBatchSize() {
        return underlying.getInputMiniBatchSize();
    }

    @Override
    public void setMaskArray(INDArray maskArray) {
        underlying.setMaskArray(maskArray);
    }

    @Override
    public INDArray getMaskArray() {
        return underlying.getMaskArray();
    }

    @Override
    public boolean isPretrainLayer() {
        return underlying.isPretrainLayer();
    }

    @Override
    public void clearNoiseWeightParams() {
        underlying.clearNoiseWeightParams();
    }

    @Override
    public Pair<INDArray, MaskState> feedForwardMaskArray(INDArray maskArray, MaskState currentMaskState, int minibatchSize) {
        return underlying.feedForwardMaskArray(maskArray, currentMaskState, minibatchSize);
    }

    @Override
    public void allowInputModification(boolean allow) {
        underlying.allowInputModification(allow);
    }


    @Override
    public TrainingConfig getConfig() {
        return underlying.getConfig();
    }

    @Override
    public boolean updaterDivideByMinibatch(String paramName) {
        return underlying.updaterDivideByMinibatch(paramName);
    }

    @Override
    public void close(){
        //No-op for individual layers
    }
}
