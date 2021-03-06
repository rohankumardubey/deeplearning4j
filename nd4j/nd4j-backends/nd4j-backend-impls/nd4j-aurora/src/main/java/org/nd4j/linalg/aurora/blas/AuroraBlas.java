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


package org.nd4j.linalg.aurora.blas;

import lombok.extern.slf4j.Slf4j;
import org.nd4j.nativeblas.NativeOpsHolder;
import org.nd4j.nativeblas.Nd4jBlas;

/**
 * Implementation of Nd4jBlas with OpenBLAS/MKL
 *
 * @author saudet
 */
@Slf4j
public class AuroraBlas extends Nd4jBlas {

    // values from /opt/nec/ve/nlc/2.0.0/include/cblas.h
    static final int CblasRowMajor=101, CblasColMajor=102,
                     CblasNoTrans=111, CblasTrans=112, CblasConjTrans=113,
                     CblasUpper=121, CblasLower=122,
                     CblasNonUnit=131, CblasUnit=132,
                     CblasLeft=141, CblasRight=142;

    /**
     * Converts a character
     * to its proper enum
     * for row (c) or column (f) ordering
     * default is row major
     */
    static int convertOrder(int from) {
        switch (from) {
            case 'c':
            case 'C':
                return CblasRowMajor;
            case 'f':
            case 'F':
                return CblasColMajor;
            default:
                return CblasColMajor;
        }
    }

    /**
     * Converts a character to its proper enum
     * t -> transpose
     * n -> no transpose
     * c -> conj
     */
    static int convertTranspose(int from) {
        switch (from) {
            case 't':
            case 'T':
                return CblasTrans;
            case 'n':
            case 'N':
                return CblasNoTrans;
            case 'c':
            case 'C':
                return CblasConjTrans;
            default:
                return CblasNoTrans;
        }
    }

    /**
     * Upper or lower
     * U/u -> upper
     * L/l -> lower
     *
     * Default is upper
     */
    static int convertUplo(int from) {
        switch (from) {
            case 'u':
            case 'U':
                return CblasUpper;
            case 'l':
            case 'L':
                return CblasLower;
            default:
                return CblasUpper;
        }
    }


    /**
     * For diagonals:
     * u/U -> unit
     * n/N -> non unit
     *
     * Default: unit
     */
    static int convertDiag(int from) {
        switch (from) {
            case 'u':
            case 'U':
                return CblasUnit;
            case 'n':
            case 'N':
                return CblasNonUnit;
            default:
                return CblasUnit;
        }
    }

    /**
     * Side of a matrix, left or right
     * l /L -> left
     * r/R -> right
     * default: left
     */
    static int convertSide(int from) {
        switch (from) {
            case 'l':
            case 'L':
                return CblasLeft;
            case 'r':
            case 'R':
                return CblasRight;
            default:
                return CblasLeft;
        }
    }

    @Override
    public void setMaxThreads(int num) {
        // blas_set_num_threads(num);
    }

    @Override
    public int getMaxThreads() {
        return NativeOpsHolder.getInstance().getDeviceNativeOps().ompGetMaxThreads(); // blas_get_num_threads();
    }

    @Override
    public int getBlasVendorId() {
        return -1; // blas_get_vendor();
    }
}
