/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ytkab0bp.slicebeam.utils;

import androidx.annotation.NonNull;

/**
 * Double alternative to android.opengl.Matrix
 * 
 * Matrix math utilities. These methods operate on OpenGL ES format
 * matrices and vectors stored in double arrays.
 * <p>
 * Matrices are 4 x 4 column-vector matrices stored in column-major
 * order:
 * <pre>
 *  m[offset +  0] m[offset +  4] m[offset +  8] m[offset + 12]
 *  m[offset +  1] m[offset +  5] m[offset +  9] m[offset + 13]
 *  m[offset +  2] m[offset +  6] m[offset + 10] m[offset + 14]
 *  m[offset +  3] m[offset +  7] m[offset + 11] m[offset + 15]</pre>
 *
 * Vectors are 4 x 1 column vectors stored in order:
 * <pre>
 * v[offset + 0]
 * v[offset + 1]
 * v[offset + 2]
 * v[offset + 3]</pre>
 */
public class DoubleMatrix {

    /** Temporary memory for operations that need temporary matrix data. */
    private static final ThreadLocal<double[]> ThreadTmp = new ThreadLocal() {
        @Override protected double[] initialValue() {
            return new double[32];
        }
    };

    private static boolean overlap(
            double[] a, int aStart, int aLength, double[] b, int bStart, int bLength) {
        if (a != b) {
            return false;
        }

        if (aStart == bStart) {
            return true;
        }

        int aEnd = aStart + aLength;
        int bEnd = bStart + bLength;

        if (aEnd == bEnd) {
            return true;
        }

        if (aStart < bStart && bStart < aEnd) {
            return true;
        }
        if (aStart < bEnd   && bEnd   < aEnd) {
            return true;
        }

        if (bStart < aStart && aStart < bEnd) {
            return true;
        }
        return bStart < aEnd && aEnd < bEnd;
    }

    /**
     * Multiplies two 4x4 matrices together and stores the result in a third 4x4
     * matrix. In matrix notation: result = lhs x rhs. Due to the way
     * matrix multiplication works, the result matrix will have the same
     * effect as first multiplying by the rhs matrix, then multiplying by
     * the lhs matrix. This is the opposite of what you might expect.
     * <p>
     * The same double array may be passed for result, lhs, and/or rhs. This
     * operation is expected to do the correct thing if the result elements
     * overlap with either of the lhs or rhs elements.
     *
     * @param result The double array that holds the result.
     * @param resultOffset The offset into the result array where the result is
     *        stored.
     * @param lhs The double array that holds the left-hand-side matrix.
     * @param lhsOffset The offset into the lhs array where the lhs is stored
     * @param rhs The double array that holds the right-hand-side matrix.
     * @param rhsOffset The offset into the rhs array where the rhs is stored.
     *
     * @throws IllegalArgumentException under any of the following conditions:
     * result, lhs, or rhs are null;
     * resultOffset + 16 > result.length
     * or lhsOffset + 16 > lhs.length
     * or rhsOffset + 16 > rhs.length;
     * resultOffset < 0 or lhsOffset < 0 or rhsOffset < 0
     */
    public static void multiplyMM(double[] result, int resultOffset,
                                  double[] lhs, int lhsOffset, double[] rhs, int rhsOffset) {
        // error checking
        if (result == null) {
            throw new IllegalArgumentException("result == null");
        }
        if (lhs == null) {
            throw new IllegalArgumentException("lhs == null");
        }
        if (rhs == null) {
            throw new IllegalArgumentException("rhs == null");
        }
        if (resultOffset < 0) {
            throw new IllegalArgumentException("resultOffset < 0");
        }
        if (lhsOffset < 0) {
            throw new IllegalArgumentException("lhsOffset < 0");
        }
        if (rhsOffset < 0) {
            throw new IllegalArgumentException("rhsOffset < 0");
        }
        if (result.length < resultOffset + 16) {
            throw new IllegalArgumentException("result.length < resultOffset + 16");
        }
        if (lhs.length < lhsOffset + 16) {
            throw new IllegalArgumentException("lhs.length < lhsOffset + 16");
        }
        if (rhs.length < rhsOffset + 16) {
            throw new IllegalArgumentException("rhs.length < rhsOffset + 16");
        }

        // Check for overlap between rhs and result or lhs and result
        if ( overlap(result, resultOffset, 16, lhs, lhsOffset, 16)
                || overlap(result, resultOffset, 16, rhs, rhsOffset, 16) ) {
            double[] tmp = ThreadTmp.get();
            for (int i=0; i<4; i++) {
                final double rhs_i0 = rhs[4 * i + rhsOffset];
                double ri0 = lhs[lhsOffset] * rhs_i0;
                double ri1 = lhs[ 1 + lhsOffset ] * rhs_i0;
                double ri2 = lhs[ 2 + lhsOffset ] * rhs_i0;
                double ri3 = lhs[ 3 + lhsOffset ] * rhs_i0;
                for (int j=1; j<4; j++) {
                    final double rhs_ij = rhs[ 4*i + j + rhsOffset];
                    ri0 += lhs[4 * j + lhsOffset] * rhs_ij;
                    ri1 += lhs[ 4*j + 1 + lhsOffset ] * rhs_ij;
                    ri2 += lhs[ 4*j + 2 + lhsOffset ] * rhs_ij;
                    ri3 += lhs[ 4*j + 3 + lhsOffset ] * rhs_ij;
                }
                tmp[4 * i] = ri0;
                tmp[ 4*i + 1 ] = ri1;
                tmp[ 4*i + 2 ] = ri2;
                tmp[ 4*i + 3 ] = ri3;
            }

            // copy from tmp to result
            System.arraycopy(tmp, 0, result, 0 + resultOffset, 16);

        } else {
            for (int i=0; i<4; i++) {
                final double rhs_i0 = rhs[4 * i + rhsOffset];
                double ri0 = lhs[lhsOffset] * rhs_i0;
                double ri1 = lhs[ 1 + lhsOffset ] * rhs_i0;
                double ri2 = lhs[ 2 + lhsOffset ] * rhs_i0;
                double ri3 = lhs[ 3 + lhsOffset ] * rhs_i0;
                for (int j=1; j<4; j++) {
                    final double rhs_ij = rhs[ 4*i + j + rhsOffset];
                    ri0 += lhs[4 * j + lhsOffset] * rhs_ij;
                    ri1 += lhs[ 4*j + 1 + lhsOffset ] * rhs_ij;
                    ri2 += lhs[ 4*j + 2 + lhsOffset ] * rhs_ij;
                    ri3 += lhs[ 4*j + 3 + lhsOffset ] * rhs_ij;
                }
                result[4 * i + resultOffset] = ri0;
                result[ 4*i + 1 + resultOffset ] = ri1;
                result[ 4*i + 2 + resultOffset ] = ri2;
                result[ 4*i + 3 + resultOffset ] = ri3;
            }
        }
    }

    /**
     * Multiplies a 4 element vector by a 4x4 matrix and stores the result in a
     * 4-element column vector. In matrix notation: result = lhs x rhs
     * <p>
     * The same double array may be passed for resultVec, lhsMat, and/or rhsVec.
     * This operation is expected to do the correct thing if the result elements
     * overlap with either of the lhs or rhs elements.
     *
     * @param resultVec The double array that holds the result vector.
     * @param resultVecOffset The offset into the result array where the result
     *        vector is stored.
     * @param lhsMat The double array that holds the left-hand-side matrix.
     * @param lhsMatOffset The offset into the lhs array where the lhs is stored
     * @param rhsVec The double array that holds the right-hand-side vector.
     * @param rhsVecOffset The offset into the rhs vector where the rhs vector
     *        is stored.
     *
     * @throws IllegalArgumentException under any of the following conditions:
     * resultVec, lhsMat, or rhsVec are null;
     * resultVecOffset + 4  > resultVec.length
     * or lhsMatOffset + 16 > lhsMat.length
     * or rhsVecOffset + 4  > rhsVec.length;
     * resultVecOffset < 0 or lhsMatOffset < 0 or rhsVecOffset < 0
     */
    public static void multiplyMV(double[] resultVec,
                                  int resultVecOffset, double[] lhsMat, int lhsMatOffset,
                                  double[] rhsVec, int rhsVecOffset) {
        // error checking
        if (resultVec == null) {
            throw new IllegalArgumentException("resultVec == null");
        }
        if (lhsMat == null) {
            throw new IllegalArgumentException("lhsMat == null");
        }
        if (rhsVec == null) {
            throw new IllegalArgumentException("rhsVec == null");
        }
        if (resultVecOffset < 0) {
            throw new IllegalArgumentException("resultVecOffset < 0");
        }
        if (lhsMatOffset < 0) {
            throw new IllegalArgumentException("lhsMatOffset < 0");
        }
        if (rhsVecOffset < 0) {
            throw new IllegalArgumentException("rhsVecOffset < 0");
        }
        if (resultVec.length < resultVecOffset + 4) {
            throw new IllegalArgumentException("resultVec.length < resultVecOffset + 4");
        }
        if (lhsMat.length < lhsMatOffset + 16) {
            throw new IllegalArgumentException("lhsMat.length < lhsMatOffset + 16");
        }
        if (rhsVec.length < rhsVecOffset + 4) {
            throw new IllegalArgumentException("rhsVec.length < rhsVecOffset + 4");
        }

        double tmp0 = lhsMat[lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset];
        double tmp1 = lhsMat[1 + lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[1 + 4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[1 + 4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[1 + 4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset];
        double tmp2 = lhsMat[2 + lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[2 + 4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[2 + 4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[2 + 4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset];
        double tmp3 = lhsMat[3 + lhsMatOffset] * rhsVec[rhsVecOffset] +
                lhsMat[3 + 4 + lhsMatOffset] * rhsVec[1 + rhsVecOffset] +
                lhsMat[3 + 4 * 2 + lhsMatOffset] * rhsVec[2 + rhsVecOffset] +
                lhsMat[3 + 4 * 3 + lhsMatOffset] * rhsVec[3 + rhsVecOffset];

        resultVec[resultVecOffset] = tmp0;
        resultVec[ 1 + resultVecOffset ] = tmp1;
        resultVec[ 2 + resultVecOffset ] = tmp2;
        resultVec[ 3 + resultVecOffset ] = tmp3;
    }

    /**
     * Transposes a 4 x 4 matrix.
     * <p>
     * mTrans and m must not overlap.
     *
     * @param mTrans the array that holds the output transposed matrix
     * @param mTransOffset an offset into mTrans where the transposed matrix is
     *        stored.
     * @param m the input array
     * @param mOffset an offset into m where the input matrix is stored.
     */
    public static void transposeM(double[] mTrans, int mTransOffset, double[] m,
                                  int mOffset) {
        for (int i = 0; i < 4; i++) {
            int mBase = i * 4 + mOffset;
            mTrans[i + mTransOffset] = m[mBase];
            mTrans[i + 4 + mTransOffset] = m[mBase + 1];
            mTrans[i + 8 + mTransOffset] = m[mBase + 2];
            mTrans[i + 12 + mTransOffset] = m[mBase + 3];
        }
    }

    /**
     * Inverts a 4 x 4 matrix.
     * <p>
     * mInv and m must not overlap.
     *
     * @param mInv the array that holds the output inverted matrix
     * @param mInvOffset an offset into mInv where the inverted matrix is
     *        stored.
     * @param m the input array
     * @param mOffset an offset into m where the input matrix is stored.
     * @return true if the matrix could be inverted, false if it could not.
     */
    public static boolean invertM(double[] mInv, int mInvOffset, double[] m,
                                  int mOffset) {
        // Invert a 4 x 4 matrix using Cramer's Rule

        // transpose matrix
        final double src0  = m[mOffset];
        final double src4  = m[mOffset +  1];
        final double src8  = m[mOffset +  2];
        final double src12 = m[mOffset +  3];

        final double src1  = m[mOffset +  4];
        final double src5  = m[mOffset +  5];
        final double src9  = m[mOffset +  6];
        final double src13 = m[mOffset +  7];

        final double src2  = m[mOffset +  8];
        final double src6  = m[mOffset +  9];
        final double src10 = m[mOffset + 10];
        final double src14 = m[mOffset + 11];

        final double src3  = m[mOffset + 12];
        final double src7  = m[mOffset + 13];
        final double src11 = m[mOffset + 14];
        final double src15 = m[mOffset + 15];

        // calculate pairs for first 8 elements (cofactors)
        final double atmp0  = src10 * src15;
        final double atmp1  = src11 * src14;
        final double atmp2  = src9  * src15;
        final double atmp3  = src11 * src13;
        final double atmp4  = src9  * src14;
        final double atmp5  = src10 * src13;
        final double atmp6  = src8  * src15;
        final double atmp7  = src11 * src12;
        final double atmp8  = src8  * src14;
        final double atmp9  = src10 * src12;
        final double atmp10 = src8  * src13;
        final double atmp11 = src9  * src12;

        // calculate first 8 elements (cofactors)
        final double dst0  = (atmp0 * src5 + atmp3 * src6 + atmp4  * src7)
                - (atmp1 * src5 + atmp2 * src6 + atmp5  * src7);
        final double dst1  = (atmp1 * src4 + atmp6 * src6 + atmp9  * src7)
                - (atmp0 * src4 + atmp7 * src6 + atmp8  * src7);
        final double dst2  = (atmp2 * src4 + atmp7 * src5 + atmp10 * src7)
                - (atmp3 * src4 + atmp6 * src5 + atmp11 * src7);
        final double dst3  = (atmp5 * src4 + atmp8 * src5 + atmp11 * src6)
                - (atmp4 * src4 + atmp9 * src5 + atmp10 * src6);
        final double dst4  = (atmp1 * src1 + atmp2 * src2 + atmp5  * src3)
                - (atmp0 * src1 + atmp3 * src2 + atmp4  * src3);
        final double dst5  = (atmp0 * src0 + atmp7 * src2 + atmp8  * src3)
                - (atmp1 * src0 + atmp6 * src2 + atmp9  * src3);
        final double dst6  = (atmp3 * src0 + atmp6 * src1 + atmp11 * src3)
                - (atmp2 * src0 + atmp7 * src1 + atmp10 * src3);
        final double dst7  = (atmp4 * src0 + atmp9 * src1 + atmp10 * src2)
                - (atmp5 * src0 + atmp8 * src1 + atmp11 * src2);

        // calculate pairs for second 8 elements (cofactors)
        final double btmp0  = src2 * src7;
        final double btmp1  = src3 * src6;
        final double btmp2  = src1 * src7;
        final double btmp3  = src3 * src5;
        final double btmp4  = src1 * src6;
        final double btmp5  = src2 * src5;
        final double btmp6  = src0 * src7;
        final double btmp7  = src3 * src4;
        final double btmp8  = src0 * src6;
        final double btmp9  = src2 * src4;
        final double btmp10 = src0 * src5;
        final double btmp11 = src1 * src4;

        // calculate second 8 elements (cofactors)
        final double dst8  = (btmp0  * src13 + btmp3  * src14 + btmp4  * src15)
                - (btmp1  * src13 + btmp2  * src14 + btmp5  * src15);
        final double dst9  = (btmp1  * src12 + btmp6  * src14 + btmp9  * src15)
                - (btmp0  * src12 + btmp7  * src14 + btmp8  * src15);
        final double dst10 = (btmp2  * src12 + btmp7  * src13 + btmp10 * src15)
                - (btmp3  * src12 + btmp6  * src13 + btmp11 * src15);
        final double dst11 = (btmp5  * src12 + btmp8  * src13 + btmp11 * src14)
                - (btmp4  * src12 + btmp9  * src13 + btmp10 * src14);
        final double dst12 = (btmp2  * src10 + btmp5  * src11 + btmp1  * src9 )
                - (btmp4  * src11 + btmp0  * src9  + btmp3  * src10);
        final double dst13 = (btmp8  * src11 + btmp0  * src8  + btmp7  * src10)
                - (btmp6  * src10 + btmp9  * src11 + btmp1  * src8 );
        final double dst14 = (btmp6  * src9  + btmp11 * src11 + btmp3  * src8 )
                - (btmp10 * src11 + btmp2  * src8  + btmp7  * src9 );
        final double dst15 = (btmp10 * src10 + btmp4  * src8  + btmp9  * src9 )
                - (btmp8  * src9  + btmp11 * src10 + btmp5  * src8 );

        // calculate determinant
        final double det =
                src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3;

        if (det == 0.0f) {
            return false;
        }

        // calculate matrix inverse
        final double invdet = 1.0f / det;
        mInv[     mInvOffset] = dst0  * invdet;
        mInv[ 1 + mInvOffset] = dst1  * invdet;
        mInv[ 2 + mInvOffset] = dst2  * invdet;
        mInv[ 3 + mInvOffset] = dst3  * invdet;

        mInv[ 4 + mInvOffset] = dst4  * invdet;
        mInv[ 5 + mInvOffset] = dst5  * invdet;
        mInv[ 6 + mInvOffset] = dst6  * invdet;
        mInv[ 7 + mInvOffset] = dst7  * invdet;

        mInv[ 8 + mInvOffset] = dst8  * invdet;
        mInv[ 9 + mInvOffset] = dst9  * invdet;
        mInv[10 + mInvOffset] = dst10 * invdet;
        mInv[11 + mInvOffset] = dst11 * invdet;

        mInv[12 + mInvOffset] = dst12 * invdet;
        mInv[13 + mInvOffset] = dst13 * invdet;
        mInv[14 + mInvOffset] = dst14 * invdet;
        mInv[15 + mInvOffset] = dst15 * invdet;

        return true;
    }

    /**
     * Computes an orthographic projection matrix.
     *
     * @param m returns the result
     * @param mOffset
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     */
    public static void orthoM(double[] m, int mOffset,
                              double left, double right, double bottom, double top,
                              double near, double far) {
        if (left == right) {
            throw new IllegalArgumentException("left == right");
        }
        if (bottom == top) {
            throw new IllegalArgumentException("bottom == top");
        }
        if (near == far) {
            throw new IllegalArgumentException("near == far");
        }

        final double r_width  = 1.0f / (right - left);
        final double r_height = 1.0f / (top - bottom);
        final double r_depth  = 1.0f / (far - near);
        final double x =  2.0f * (r_width);
        final double y =  2.0f * (r_height);
        final double z = -2.0f * (r_depth);
        final double tx = -(right + left) * r_width;
        final double ty = -(top + bottom) * r_height;
        final double tz = -(far + near) * r_depth;
        m[mOffset] = x;
        m[mOffset + 5] = y;
        m[mOffset +10] = z;
        m[mOffset +12] = tx;
        m[mOffset +13] = ty;
        m[mOffset +14] = tz;
        m[mOffset +15] = 1.0f;
        m[mOffset + 1] = 0.0f;
        m[mOffset + 2] = 0.0f;
        m[mOffset + 3] = 0.0f;
        m[mOffset + 4] = 0.0f;
        m[mOffset + 6] = 0.0f;
        m[mOffset + 7] = 0.0f;
        m[mOffset + 8] = 0.0f;
        m[mOffset + 9] = 0.0f;
        m[mOffset + 11] = 0.0f;
    }


    /**
     * Defines a projection matrix in terms of six clip planes.
     *
     * @param m the double array that holds the output perspective matrix
     * @param offset the offset into double array m where the perspective
     *        matrix data is written
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     */
    public static void frustumM(double[] m, int offset,
                                double left, double right, double bottom, double top,
                                double near, double far) {
        if (left == right) {
            throw new IllegalArgumentException("left == right");
        }
        if (top == bottom) {
            throw new IllegalArgumentException("top == bottom");
        }
        if (near == far) {
            throw new IllegalArgumentException("near == far");
        }
        if (near <= 0.0f) {
            throw new IllegalArgumentException("near <= 0.0f");
        }
        if (far <= 0.0f) {
            throw new IllegalArgumentException("far <= 0.0f");
        }
        final double r_width  = 1.0f / (right - left);
        final double r_height = 1.0f / (top - bottom);
        final double r_depth  = 1.0f / (near - far);
        final double x = 2.0f * (near * r_width);
        final double y = 2.0f * (near * r_height);
        final double A = (right + left) * r_width;
        final double B = (top + bottom) * r_height;
        final double C = (far + near) * r_depth;
        final double D = 2.0f * (far * near * r_depth);
        m[offset] = x;
        m[offset + 5] = y;
        m[offset + 8] = A;
        m[offset +  9] = B;
        m[offset + 10] = C;
        m[offset + 14] = D;
        m[offset + 11] = -1.0f;
        m[offset +  1] = 0.0f;
        m[offset +  2] = 0.0f;
        m[offset +  3] = 0.0f;
        m[offset +  4] = 0.0f;
        m[offset +  6] = 0.0f;
        m[offset +  7] = 0.0f;
        m[offset + 12] = 0.0f;
        m[offset + 13] = 0.0f;
        m[offset + 15] = 0.0f;
    }

    /**
     * Defines a projection matrix in terms of a field of view angle, an
     * aspect ratio, and z clip planes.
     *
     * @param m the double array that holds the perspective matrix
     * @param offset the offset into double array m where the perspective
     *        matrix data is written
     * @param fovy field of view in y direction, in degrees
     * @param aspect width to height aspect ratio of the viewport
     * @param zNear
     * @param zFar
     */
    public static void perspectiveM(double[] m, int offset,
                                    double fovy, double aspect, double zNear, double zFar) {
        double f = 1.0f / Math.tan(fovy * (Math.PI / 360.0));
        double rangeReciprocal = 1.0f / (zNear - zFar);

        m[offset] = f / aspect;
        m[offset + 1] = 0.0f;
        m[offset + 2] = 0.0f;
        m[offset + 3] = 0.0f;

        m[offset + 4] = 0.0f;
        m[offset + 5] = f;
        m[offset + 6] = 0.0f;
        m[offset + 7] = 0.0f;

        m[offset + 8] = 0.0f;
        m[offset + 9] = 0.0f;
        m[offset + 10] = (zFar + zNear) * rangeReciprocal;
        m[offset + 11] = -1.0f;

        m[offset + 12] = 0.0f;
        m[offset + 13] = 0.0f;
        m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
        m[offset + 15] = 0.0f;
    }

    /**
     * Computes the length of a vector.
     *
     * @param x x coordinate of a vector
     * @param y y coordinate of a vector
     * @param z z coordinate of a vector
     * @return the length of a vector
     */
    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Sets matrix m to the identity matrix.
     *
     * @param sm returns the result
     * @param smOffset index into sm where the result matrix starts
     */
    public static void setIdentityM(double[] sm, int smOffset) {
        for (int i=0 ; i<16 ; i++) {
            sm[smOffset + i] = 0;
        }
        for(int i = 0; i < 16; i += 5) {
            sm[smOffset + i] = 1.0f;
        }
    }

    /**
     * Scales matrix m by x, y, and z, putting the result in sm.
     * <p>
     * m and sm must not overlap.
     *
     * @param sm returns the result
     * @param smOffset index into sm where the result matrix starts
     * @param m source matrix
     * @param mOffset index into m where the source matrix starts
     * @param x scale factor x
     * @param y scale factor y
     * @param z scale factor z
     */
    public static void scaleM(double[] sm, int smOffset,
                              double[] m, int mOffset,
                              double x, double y, double z) {
        for (int i=0 ; i<4 ; i++) {
            int smi = smOffset + i;
            int mi = mOffset + i;
            sm[     smi] = m[     mi] * x;
            sm[ 4 + smi] = m[ 4 + mi] * y;
            sm[ 8 + smi] = m[ 8 + mi] * z;
            sm[12 + smi] = m[12 + mi];
        }
    }

    /**
     * Scales matrix m in place by sx, sy, and sz.
     *
     * @param m matrix to scale
     * @param mOffset index into m where the matrix starts
     * @param x scale factor x
     * @param y scale factor y
     * @param z scale factor z
     */
    public static void scaleM(double[] m, int mOffset,
                              double x, double y, double z) {
        for (int i=0 ; i<4 ; i++) {
            int mi = mOffset + i;
            m[     mi] *= x;
            m[ 4 + mi] *= y;
            m[ 8 + mi] *= z;
        }
    }

    /**
     * Translates matrix m by x, y, and z, putting the result in tm.
     * <p>
     * m and tm must not overlap.
     *
     * @param tm returns the result
     * @param tmOffset index into sm where the result matrix starts
     * @param m source matrix
     * @param mOffset index into m where the source matrix starts
     * @param x translation factor x
     * @param y translation factor y
     * @param z translation factor z
     */
    public static void translateM(double[] tm, int tmOffset,
                                  double[] m, int mOffset,
                                  double x, double y, double z) {
        System.arraycopy(m, mOffset + 0, tm, tmOffset + 0, 12);
        for (int i=0 ; i<4 ; i++) {
            int tmi = tmOffset + i;
            int mi = mOffset + i;
            tm[12 + tmi] = m[mi] * x + m[4 + mi] * y + m[8 + mi] * z +
                    m[12 + mi];
        }
    }

    /**
     * Translates matrix m by x, y, and z in place.
     *
     * @param m matrix
     * @param mOffset index into m where the matrix starts
     * @param x translation factor x
     * @param y translation factor y
     * @param z translation factor z
     */
    public static void translateM(
            double[] m, int mOffset,
            double x, double y, double z) {
        for (int i=0 ; i<4 ; i++) {
            int mi = mOffset + i;
            m[12 + mi] += m[mi] * x + m[4 + mi] * y + m[8 + mi] * z;
        }
    }

    /**
     * Rotates matrix m by angle a (in degrees) around the axis (x, y, z).
     * <p>
     * m and rm must not overlap.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param m source matrix
     * @param mOffset index into m where the source matrix starts
     * @param a angle to rotate in degrees
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    public static void rotateM(double[] rm, int rmOffset,
                               double[] m, int mOffset,
                               double a, double x, double y, double z) {
        double[] tmp = ThreadTmp.get();
        setRotateM(tmp, 16, a, x, y, z);
        multiplyMM(rm, rmOffset, m, mOffset, tmp, 16);
    }

    /**
     * Rotates matrix m in place by angle a (in degrees)
     * around the axis (x, y, z).
     *
     * @param m source matrix
     * @param mOffset index into m where the matrix starts
     * @param a angle to rotate in degrees
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    public static void rotateM(double[] m, int mOffset,
                               double a, double x, double y, double z) {
        rotateM(m, mOffset, m, mOffset, a, x, y, z);
    }

    /**
     * Creates a matrix for rotation by angle a (in degrees)
     * around the axis (x, y, z).
     * <p>
     * An optimized path will be used for rotation about a major axis
     * (e.g. x=1.0f y=0.0f z=0.0f).
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param a angle to rotate in degrees
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    public static void setRotateM(double[] rm, int rmOffset,
                                  double a, double x, double y, double z) {
        rm[rmOffset + 3] = 0;
        rm[rmOffset + 7] = 0;
        rm[rmOffset + 11]= 0;
        rm[rmOffset + 12]= 0;
        rm[rmOffset + 13]= 0;
        rm[rmOffset + 14]= 0;
        rm[rmOffset + 15]= 1;
        a *= Math.PI / 180.0f;
        double s = Math.sin(a);
        double c = Math.cos(a);
        if (1.0f == x && 0.0f == y && 0.0f == z) {
            rm[rmOffset + 5] = c;   rm[rmOffset + 10]= c;
            rm[rmOffset + 6] = s;   rm[rmOffset + 9] = -s;
            rm[rmOffset + 1] = 0;   rm[rmOffset + 2] = 0;
            rm[rmOffset + 4] = 0;   rm[rmOffset + 8] = 0;
            rm[rmOffset] = 1;
        } else if (0.0f == x && 1.0f == y && 0.0f == z) {
            rm[rmOffset] = c;   rm[rmOffset + 10]= c;
            rm[rmOffset + 8] = s;   rm[rmOffset + 2] = -s;
            rm[rmOffset + 1] = 0;   rm[rmOffset + 4] = 0;
            rm[rmOffset + 6] = 0;   rm[rmOffset + 9] = 0;
            rm[rmOffset + 5] = 1;
        } else if (0.0f == x && 0.0f == y && 1.0f == z) {
            rm[rmOffset] = c;   rm[rmOffset + 5] = c;
            rm[rmOffset + 1] = s;   rm[rmOffset + 4] = -s;
            rm[rmOffset + 2] = 0;   rm[rmOffset + 6] = 0;
            rm[rmOffset + 8] = 0;   rm[rmOffset + 9] = 0;
            rm[rmOffset + 10]= 1;
        } else {
            double len = length(x, y, z);
            if (1.0f != len) {
                double recipLen = 1.0f / len;
                x *= recipLen;
                y *= recipLen;
                z *= recipLen;
            }
            double nc = 1.0f - c;
            double xy = x * y;
            double yz = y * z;
            double zx = z * x;
            double xs = x * s;
            double ys = y * s;
            double zs = z * s;
            rm[rmOffset] = x*x*nc +  c;
            rm[rmOffset +  4] =  xy*nc - zs;
            rm[rmOffset +  8] =  zx*nc + ys;
            rm[rmOffset +  1] =  xy*nc + zs;
            rm[rmOffset +  5] = y*y*nc +  c;
            rm[rmOffset +  9] =  yz*nc - xs;
            rm[rmOffset +  2] =  zx*nc - ys;
            rm[rmOffset +  6] =  yz*nc + xs;
            rm[rmOffset + 10] = z*z*nc +  c;
        }
    }

    /**
     * Converts Euler angles to a rotation matrix.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param x angle of rotation, in degrees
     * @param y is broken, do not use
     * @param z angle of rotation, in degrees
     *
     * @deprecated This method is incorrect around the y axis. This method is
     *             deprecated and replaced (below) by setRotateEulerM2 which
     *             behaves correctly
     */
    @Deprecated
    public static void setRotateEulerM(double[] rm, int rmOffset,
                                       double x, double y, double z) {
        x *= Math.PI / 180.0f;
        y *= Math.PI / 180.0f;
        z *= Math.PI / 180.0f;
        double cx = Math.cos(x);
        double sx = Math.sin(x);
        double cy = Math.cos(y);
        double sy = Math.sin(y);
        double cz = Math.cos(z);
        double sz = Math.sin(z);
        double cxsy = cx * sy;
        double sxsy = sx * sy;

        rm[rmOffset]  =   cy * cz;
        rm[rmOffset + 1]  =  -cy * sz;
        rm[rmOffset + 2]  =   sy;
        rm[rmOffset + 3]  =  0.0f;

        rm[rmOffset + 4]  =  cxsy * cz + cx * sz;
        rm[rmOffset + 5]  = -cxsy * sz + cx * cz;
        rm[rmOffset + 6]  =  -sx * cy;
        rm[rmOffset + 7]  =  0.0f;

        rm[rmOffset + 8]  = -sxsy * cz + sx * sz;
        rm[rmOffset + 9]  =  sxsy * sz + sx * cz;
        rm[rmOffset + 10] =  cx * cy;
        rm[rmOffset + 11] =  0.0f;

        rm[rmOffset + 12] =  0.0f;
        rm[rmOffset + 13] =  0.0f;
        rm[rmOffset + 14] =  0.0f;
        rm[rmOffset + 15] =  1.0f;
    }

    /**
     * Converts Euler angles to a rotation matrix.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param x angle of rotation, in degrees
     * @param y angle of rotation, in degrees
     * @param z angle of rotation, in degrees
     *
     * @throws IllegalArgumentException if rm is null;
     * or if rmOffset + 16 > rm.length;
     * rmOffset < 0
     */
    public static void setRotateEulerM2(@NonNull double[] rm, int rmOffset,
                                        double x, double y, double z) {
        if (rm == null) {
            throw new IllegalArgumentException("rm == null");
        }
        if (rmOffset < 0) {
            throw new IllegalArgumentException("rmOffset < 0");
        }
        if (rm.length < rmOffset + 16) {
            throw new IllegalArgumentException("rm.length < rmOffset + 16");
        }

        x *= Math.PI / 180.0f;
        y *= Math.PI / 180.0f;
        z *= Math.PI / 180.0f;
        double cx = Math.cos(x);
        double sx = Math.sin(x);
        double cy = Math.cos(y);
        double sy = Math.sin(y);
        double cz = Math.cos(z);
        double sz = Math.sin(z);
        double cxsy = cx * sy;
        double sxsy = sx * sy;

        rm[rmOffset]  =  cy * cz;
        rm[rmOffset + 1]  = -cy * sz;
        rm[rmOffset + 2]  =  sy;
        rm[rmOffset + 3]  =  0.0f;

        rm[rmOffset + 4]  =  sxsy * cz + cx * sz;
        rm[rmOffset + 5]  = -sxsy * sz + cx * cz;
        rm[rmOffset + 6]  = -sx * cy;
        rm[rmOffset + 7]  =  0.0f;

        rm[rmOffset + 8]  = -cxsy * cz + sx * sz;
        rm[rmOffset + 9]  =  cxsy * sz + sx * cz;
        rm[rmOffset + 10] =  cx * cy;
        rm[rmOffset + 11] =  0.0f;

        rm[rmOffset + 12] =  0.0f;
        rm[rmOffset + 13] =  0.0f;
        rm[rmOffset + 14] =  0.0f;
        rm[rmOffset + 15] =  1.0f;
    }

    /**
     * Defines a viewing transformation in terms of an eye point, a center of
     * view, and an up vector.
     *
     * @param rm returns the result
     * @param rmOffset index into rm where the result matrix starts
     * @param eyeX eye point X
     * @param eyeY eye point Y
     * @param eyeZ eye point Z
     * @param centerX center of view X
     * @param centerY center of view Y
     * @param centerZ center of view Z
     * @param upX up vector X
     * @param upY up vector Y
     * @param upZ up vector Z
     */
    public static void setLookAtM(double[] rm, int rmOffset,
                                  double eyeX, double eyeY, double eyeZ,
                                  double centerX, double centerY, double centerZ, double upX, double upY,
                                  double upZ) {

        // See the OpenGL GLUT documentation for gluLookAt for a description
        // of the algorithm. We implement it in a straightforward way:

        double fx = centerX - eyeX;
        double fy = centerY - eyeY;
        double fz = centerZ - eyeZ;

        // Normalize f
        double rlf = 1.0f / DoubleMatrix.length(fx, fy, fz);
        fx *= rlf;
        fy *= rlf;
        fz *= rlf;

        // compute s = f x up (x means "cross product")
        double sx = fy * upZ - fz * upY;
        double sy = fz * upX - fx * upZ;
        double sz = fx * upY - fy * upX;

        // and normalize s
        double rls = 1.0f / DoubleMatrix.length(sx, sy, sz);
        sx *= rls;
        sy *= rls;
        sz *= rls;

        // compute u = s x f
        double ux = sy * fz - sz * fy;
        double uy = sz * fx - sx * fz;
        double uz = sx * fy - sy * fx;

        rm[rmOffset] = sx;
        rm[rmOffset + 1] = ux;
        rm[rmOffset + 2] = -fx;
        rm[rmOffset + 3] = 0.0f;

        rm[rmOffset + 4] = sy;
        rm[rmOffset + 5] = uy;
        rm[rmOffset + 6] = -fy;
        rm[rmOffset + 7] = 0.0f;

        rm[rmOffset + 8] = sz;
        rm[rmOffset + 9] = uz;
        rm[rmOffset + 10] = -fz;
        rm[rmOffset + 11] = 0.0f;

        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;

        translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ);
    }
}
