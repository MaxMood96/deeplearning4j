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

package org.deeplearning4j.frameworkimport.keras.keras.layers.advanced.activations;

import org.deeplearning4j.frameworkimport.keras.keras.KerasLayer;
import org.deeplearning4j.frameworkimport.keras.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.frameworkimport.keras.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.nd4j.linalg.activations.impl.ActivationSoftmax;

import java.util.Map;

public class KerasSoftmax extends KerasLayer {

    /**
     * Constructor from parsed Keras layer configuration dictionary.
     *
     * @param layerConfig dictionary containing Keras layer configuration
     * @throws InvalidKerasConfigurationException Invalid Keras config
     * @throws UnsupportedKerasConfigurationException Unsupported Invalid Keras config
     */
    public KerasSoftmax(Map<String, Object> layerConfig)
            throws InvalidKerasConfigurationException, UnsupportedKerasConfigurationException {
        this(layerConfig, true);
    }

    /**
     * Constructor from parsed Keras layer configuration dictionary.
     *
     * @param layerConfig           dictionary containing Keras layer configuration
     * @param enforceTrainingConfig whether to enforce training-related configuration options
     * @throws InvalidKerasConfigurationException Invalid Keras config
     * @throws UnsupportedKerasConfigurationException Invalid Keras config
     */
    public KerasSoftmax(Map<String, Object> layerConfig, boolean enforceTrainingConfig)
            throws InvalidKerasConfigurationException, UnsupportedKerasConfigurationException {
        super(layerConfig, enforceTrainingConfig);

        this.layer = new ActivationLayer.Builder().name(this.layerName).activation(new ActivationSoftmax()).build();
    }

    /**
     * Get layer output type.
     *
     * @param inputType Array of InputTypes
     * @return output type as InputType
     * @throws InvalidKerasConfigurationException Invalid Keras config
     */
    public InputType getOutputType(InputType... inputType) throws InvalidKerasConfigurationException {
        if (inputType.length > 1)
            throw new InvalidKerasConfigurationException(
                    "Keras Activation layer accepts only one input (received " + inputType.length + ")");
        return this.getActivationLayer().getOutputType(-1, inputType[0]);
    }

    /**
     * Get DL4J ActivationLayer.
     *
     * @return ActivationLayer
     */
    public ActivationLayer getActivationLayer() {
        return (ActivationLayer) this.layer;
    }

}