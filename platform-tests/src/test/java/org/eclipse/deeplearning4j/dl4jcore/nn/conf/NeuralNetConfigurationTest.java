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
package org.eclipse.deeplearning4j.dl4jcore.nn.conf;

import org.deeplearning4j.BaseDL4JTest;
import org.eclipse.deeplearning4j.dl4jcore.TestUtils;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.BaseLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.variational.VariationalAutoencoder;
import org.deeplearning4j.nn.conf.stepfunctions.DefaultStepFunction;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.weights.*;
import org.deeplearning4j.optimize.api.ConvexOptimizer;
import org.deeplearning4j.optimize.solvers.StochasticGradientDescent;
import org.deeplearning4j.optimize.stepfunctions.NegativeDefaultStepFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nd4j.common.tests.tags.NativeTag;
import org.nd4j.common.tests.tags.TagNames;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.scalar.LeakyReLU;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.learning.regularization.Regularization;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Neural Net Configuration Test")
@NativeTag
@Tag(TagNames.DL4J_OLD_API)
class NeuralNetConfigurationTest extends BaseDL4JTest {

    final DataSet trainingSet = createData();

    public DataSet createData() {
        int numFeatures = 40;
        // have to be at least two or else output layer gradient is a scalar and cause exception
        INDArray input = Nd4j.create(2, numFeatures);
        INDArray labels = Nd4j.create(2, 2);
        INDArray row0 = Nd4j.create(1, numFeatures);
        row0.assign(0.1);
        input.putRow(0, row0);
        // set the 4th column
        labels.put(0, 1, 1);
        INDArray row1 = Nd4j.create(1, numFeatures);
        row1.assign(0.2);
        input.putRow(1, row1);
        // set the 2nd column
        labels.put(1, 0, 1);
        return new DataSet(input, labels);
    }

    @Test
    @DisplayName("Test Json")
    void testJson() {
        NeuralNetConfiguration conf = getConfig(1, 1, new WeightInitXavier(), true);
        String json = conf.toJson();
        NeuralNetConfiguration read = NeuralNetConfiguration.fromJson(json);
        assertEquals(conf, read);
    }

    @Test
    @DisplayName("Test Yaml")
    void testYaml() {
        NeuralNetConfiguration conf = getConfig(1, 1, new WeightInitXavier(), true);
        String json = conf.toYaml();
        NeuralNetConfiguration read = NeuralNetConfiguration.fromYaml(json);
        assertEquals(conf, read);
    }

    @Test
    @DisplayName("Test Clone")
    void testClone() {
        NeuralNetConfiguration conf = getConfig(1, 1, new WeightInitUniform(), true);
        BaseLayer bl = (BaseLayer) conf.getLayer();
        conf.setStepFunction(new DefaultStepFunction());
        NeuralNetConfiguration conf2 = conf.clone();
        assertEquals(conf, conf2);
        assertNotSame(conf, conf2);
        assertNotSame(conf.getLayer(), conf2.getLayer());
        assertNotSame(conf.getStepFunction(), conf2.getStepFunction());
    }

    @Test
    @DisplayName("Test RNG")
    void testRNG() {
        DenseLayer layer = new DenseLayer.Builder().nIn(trainingSet.numInputs()).nOut(trainingSet.numOutcomes())
                .weightInit(WeightInit.UNIFORM).activation(Activation.TANH).build();
        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().seed(123)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).layer(layer).build();
        long numParams = conf.getLayer().initializer().numParams(conf);
        INDArray params = Nd4j.create(1, numParams);
        Layer model = conf.getLayer().instantiate(conf, null, 0, params, true, params.dataType());
        INDArray modelWeights = model.getParam(DefaultParamInitializer.WEIGHT_KEY);
        DenseLayer layer2 = new DenseLayer.Builder().nIn(trainingSet.numInputs()).nOut(trainingSet.numOutcomes())
                .weightInit(WeightInit.UNIFORM).activation(Activation.TANH).build();
        NeuralNetConfiguration conf2 = new NeuralNetConfiguration.Builder().seed(123)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).layer(layer2).build();
        long numParams2 = conf2.getLayer().initializer().numParams(conf);
        INDArray params2 = Nd4j.create(1, numParams);
        Layer model2 = conf2.getLayer().instantiate(conf2, null, 0, params2, true, params.dataType());
        INDArray modelWeights2 = model2.getParam(DefaultParamInitializer.WEIGHT_KEY);
        assertEquals(modelWeights, modelWeights2);
    }

    @Test
    @DisplayName("Test Set Seed Size")
    void testSetSeedSize() {
        Nd4j.getRandom().setSeed(123);
        Layer model = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitXavier(), true);
        INDArray modelWeights = model.getParam(DefaultParamInitializer.WEIGHT_KEY);
        Nd4j.getRandom().setSeed(123);
        Layer model2 = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitXavier(), true);
        INDArray modelWeights2 = model2.getParam(DefaultParamInitializer.WEIGHT_KEY);
        assertEquals(modelWeights, modelWeights2);
    }

    @Test
    @DisplayName("Test Set Seed Normalized")
    void testSetSeedNormalized() {
        Nd4j.getRandom().setSeed(123);
        Layer model = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitXavier(), true);
        INDArray modelWeights = model.getParam(DefaultParamInitializer.WEIGHT_KEY);
        Nd4j.getRandom().setSeed(123);
        Layer model2 = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitXavier(), true);
        INDArray modelWeights2 = model2.getParam(DefaultParamInitializer.WEIGHT_KEY);
        assertEquals(modelWeights, modelWeights2);
    }

    @Test
    @DisplayName("Test Set Seed Xavier")
    void testSetSeedXavier() {
        Nd4j.getRandom().setSeed(123);
        Layer model = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitUniform(), true);
        INDArray modelWeights = model.getParam(DefaultParamInitializer.WEIGHT_KEY);
        Nd4j.getRandom().setSeed(123);
        Layer model2 = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitUniform(), true);
        INDArray modelWeights2 = model2.getParam(DefaultParamInitializer.WEIGHT_KEY);
        assertEquals(modelWeights, modelWeights2);
    }

    @Test
    @DisplayName("Test Set Seed Distribution")
    void testSetSeedDistribution() {
        Nd4j.getRandom().setSeed(123);
        Layer model = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitDistribution(new NormalDistribution(1, 1)), true);
        INDArray modelWeights = model.getParam(DefaultParamInitializer.WEIGHT_KEY);
        Nd4j.getRandom().setSeed(123);
        Layer model2 = getLayer(trainingSet.numInputs(), trainingSet.numOutcomes(), new WeightInitDistribution(new NormalDistribution(1, 1)), true);
        INDArray modelWeights2 = model2.getParam(DefaultParamInitializer.WEIGHT_KEY);
        assertEquals(modelWeights, modelWeights2);
    }

    private static NeuralNetConfiguration getConfig(int nIn, int nOut, IWeightInit weightInit, boolean pretrain) {
        DenseLayer layer = new DenseLayer.Builder().nIn(nIn).nOut(nOut).weightInit(weightInit).activation(Activation.TANH).build();
        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).layer(layer).build();
        return conf;
    }

    private static Layer getLayer(int nIn, int nOut, IWeightInit weightInit, boolean preTrain) {
        NeuralNetConfiguration conf = getConfig(nIn, nOut, weightInit, preTrain);
        long numParams = conf.getLayer().initializer().numParams(conf);
        INDArray params = Nd4j.create(1, numParams);
        return conf.getLayer().instantiate(conf, null, 0, params, true, params.dataType());
    }

    @Test
    @DisplayName("Test Learning Rate By Param")
    void testLearningRateByParam() {
        double lr = 0.01;
        double biasLr = 0.02;
        int[] nIns = { 4, 3, 3 };
        int[] nOuts = { 3, 3, 3 };
        int oldScore = 1;
        int newScore = 1;
        int iteration = 3;
        INDArray gradientW = Nd4j.ones(nIns[0], nOuts[0]);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().updater(new Sgd(0.3)).list().layer(0, new DenseLayer.Builder().nIn(nIns[0]).nOut(nOuts[0]).updater(new Sgd(lr)).biasUpdater(new Sgd(biasLr)).build()).layer(1, new BatchNormalization.Builder().nIn(nIns[1]).nOut(nOuts[1]).updater(new Sgd(0.7)).build()).layer(2, new OutputLayer.Builder().nIn(nIns[2]).nOut(nOuts[2]).lossFunction(LossFunctions.LossFunction.MSE).build()).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        ConvexOptimizer opt = new StochasticGradientDescent(net.getDefaultConfiguration(), new NegativeDefaultStepFunction(), null, net);
        assertEquals(lr, ((Sgd) net.getLayer(0).conf().getLayer().getUpdaterByParam("W")).getLearningRate(), 1e-4);
        assertEquals(biasLr, ((Sgd) net.getLayer(0).conf().getLayer().getUpdaterByParam("b")).getLearningRate(), 1e-4);
        assertEquals(0.7, ((Sgd) net.getLayer(1).conf().getLayer().getUpdaterByParam("gamma")).getLearningRate(), 1e-4);
        // From global LR
        assertEquals(0.3, ((Sgd) net.getLayer(2).conf().getLayer().getUpdaterByParam("W")).getLearningRate(), 1e-4);
        // From global LR
        assertEquals(0.3, ((Sgd) net.getLayer(2).conf().getLayer().getUpdaterByParam("W")).getLearningRate(), 1e-4);
    }

    @Test
    @DisplayName("Test Leakyrelu Alpha")
    void testLeakyreluAlpha() {
        // FIXME: Make more generic to use neuralnetconfs
        int sizeX = 4;
        int scaleX = 10;
        System.out.println("Here is a leaky vector..");
        INDArray leakyVector = Nd4j.linspace(-1, 1, sizeX, Nd4j.dataType());
        leakyVector = leakyVector.mul(scaleX);
        System.out.println(leakyVector);
        double myAlpha = 0.5;
        System.out.println("======================");
        System.out.println("Exec and Return: Leaky Relu transformation with alpha = 0.5 ..");
        System.out.println("======================");
        INDArray outDef = Nd4j.getExecutioner().exec(new LeakyReLU(leakyVector.dup(), myAlpha));
        System.out.println(outDef);
        String confActivation = "leakyrelu";
        Object[] confExtra = { myAlpha };
        INDArray outMine = Nd4j.getExecutioner().exec(new LeakyReLU(leakyVector.dup(), myAlpha));
        System.out.println("======================");
        System.out.println("Exec and Return: Leaky Relu transformation with a value via getOpFactory");
        System.out.println("======================");
        System.out.println(outMine);
        // Test equality for ndarray elementwise
        // assertArrayEquals(..)
    }

    @Test
    @DisplayName("Test L 1 L 2 By Param")
    void testL1L2ByParam() {
        double l1 = 0.01;
        double l2 = 0.07;
        int[] nIns = { 4, 3, 3 };
        int[] nOuts = { 3, 3, 3 };
        int oldScore = 1;
        int newScore = 1;
        int iteration = 3;
        INDArray gradientW = Nd4j.ones(nIns[0], nOuts[0]);
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().l1(l1).l2(l2).list().layer(0, new DenseLayer.Builder().nIn(nIns[0]).nOut(nOuts[0]).build()).layer(1, new BatchNormalization.Builder().nIn(nIns[1]).nOut(nOuts[1]).l2(0.5).build()).layer(2, new OutputLayer.Builder().nIn(nIns[2]).nOut(nOuts[2]).lossFunction(LossFunctions.LossFunction.MSE).build()).build();
        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        ConvexOptimizer opt = new StochasticGradientDescent(net.getDefaultConfiguration(), new NegativeDefaultStepFunction(), null, net);
        Assertions.assertEquals(l1, TestUtils.getL1(net.getLayer(0).conf().getLayer().getRegularizationByParam("W")), 1e-4);
        List<Regularization> r = net.getLayer(0).conf().getLayer().getRegularizationByParam("b");
        assertEquals(0, r.size());
        r = net.getLayer(1).conf().getLayer().getRegularizationByParam("beta");
        assertTrue(r == null || r.isEmpty());
        r = net.getLayer(1).conf().getLayer().getRegularizationByParam("gamma");
        assertTrue(r == null || r.isEmpty());
        r = net.getLayer(1).conf().getLayer().getRegularizationByParam("mean");
        assertTrue(r == null || r.isEmpty());
        r = net.getLayer(1).conf().getLayer().getRegularizationByParam("var");
        assertTrue(r == null || r.isEmpty());
        assertEquals(l2, TestUtils.getL2(net.getLayer(2).conf().getLayer().getRegularizationByParam("W")), 1e-4);
        r = net.getLayer(2).conf().getLayer().getRegularizationByParam("b");
        assertTrue(r == null || r.isEmpty());
    }

    @Test
    @DisplayName("Test Layer Pretrain Config")
    void testLayerPretrainConfig() {
        boolean pretrain = true;
        VariationalAutoencoder layer = new VariationalAutoencoder.Builder().nIn(10).nOut(5).updater(new Sgd(1e-1)).lossFunction(LossFunctions.LossFunction.KL_DIVERGENCE).build();
        NeuralNetConfiguration conf = new NeuralNetConfiguration.Builder().seed(42).layer(layer).build();
    }
}
