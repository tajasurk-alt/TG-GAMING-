package com.example.game.engine

import android.opengl.Matrix

object MatrixUtils {
  fun setIdentity(m: FloatArray) {
    Matrix.setIdentityM(m, 0)
  }

  fun copy(src: FloatArray, dst: FloatArray) {
    System.arraycopy(src, 0, dst, 0, 16)
  }

  fun translate(m: FloatArray, x: Float, y: Float, z: Float) {
    Matrix.translateM(m, 0, x, y, z)
  }

  fun rotateYDeg(m: FloatArray, deg: Float) {
    Matrix.rotateM(m, 0, deg, 0f, 1f, 0f)
  }

  fun rotateXDeg(m: FloatArray, deg: Float) {
    Matrix.rotateM(m, 0, deg, 1f, 0f, 0f)
  }

  fun rotateZDeg(m: FloatArray, deg: Float) {
    Matrix.rotateM(m, 0, deg, 0f, 0f, 1f)
  }

  fun scale(m: FloatArray, sx: Float, sy: Float, sz: Float) {
    Matrix.scaleM(m, 0, sx, sy, sz)
  }

  fun radToDeg(rad: Float): Float = (rad * 180.0 / Math.PI).toFloat()
}
