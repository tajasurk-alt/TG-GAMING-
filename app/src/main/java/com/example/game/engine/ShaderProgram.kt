package com.example.game.engine

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(vertexSrc: String, fragmentSrc: String) {
  val programId: Int

  init {
    val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSrc)
    val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
    val prog = GLES30.glCreateProgram()
    GLES30.glAttachShader(prog, vs)
    GLES30.glAttachShader(prog, fs)
    GLES30.glLinkProgram(prog)
    val linkStatus = IntArray(1)
    GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] == 0) {
      val log = GLES30.glGetProgramInfoLog(prog)
      GLES30.glDeleteProgram(prog)
      Log.e("ShaderProgram", "Link error: $log")
    }
    GLES30.glDeleteShader(vs)
    GLES30.glDeleteShader(fs)
    programId = prog
  }

  fun use() {
    GLES30.glUseProgram(programId)
  }

  fun uniform(name: String): Int = GLES30.glGetUniformLocation(programId, name)

  fun release() {
    if (programId != 0) GLES30.glDeleteProgram(programId)
  }

  private fun compile(type: Int, src: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, src)
    GLES30.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
      val log = GLES30.glGetShaderInfoLog(shader)
      Log.e("ShaderProgram", "Compile error: $log")
      GLES30.glDeleteShader(shader)
      return 0
    }
    return shader
  }
}

object GameShaders {
  val SKY_VERTEX =
    """#version 300 es
    layout(location = 0) in vec3 aPos;
    out vec2 vNdc;
    void main() {
      vNdc = aPos.xy;
      gl_Position = vec4(aPos.xy, 0.9999, 1.0);
    }
    """.trimIndent()

  val SKY_FRAGMENT =
    """#version 300 es
    precision highp float;
    in vec2 vNdc;
    out vec4 fragColor;

    uniform mat4 uInvViewProj;
    uniform vec3 uCamPos;
    uniform vec3 uSunDir;
    uniform vec3 uSunColor;
    uniform vec3 uZenithColor;
    uniform vec3 uHorizonColor;

    void main() {
      vec4 nearP = uInvViewProj * vec4(vNdc, -1.0, 1.0);
      vec4 farP  = uInvViewProj * vec4(vNdc,  1.0, 1.0);
      vec3 dir = normalize(farP.xyz / farP.w - nearP.xyz / nearP.w);

      float h = clamp(dir.y * 1.45 + 0.08, 0.0, 1.0);
      vec3 sky = mix(uHorizonColor, uZenithColor, pow(h, 0.65));

      float sunDot = max(dot(dir, normalize(uSunDir)), 0.0);
      float sunGlow = pow(sunDot, 12.0) * 0.45;
      float sunDisc = smoothstep(0.9975, 0.9992, sunDot) * 1.6;
      sky += uSunColor * (sunGlow + sunDisc);

      fragColor = vec4(sky, 1.0);
    }
    """.trimIndent()

  val WORLD_VERTEX =
    """#version 300 es
    layout(location = 0) in vec3 aPos;
    layout(location = 1) in vec3 aNormal;
    layout(location = 2) in vec3 aColor;
    layout(location = 3) in vec2 aExtra;

    uniform mat4 uMvp;
    uniform mat4 uModel;

    out vec3 vWorldPos;
    out vec3 vNormal;
    out vec3 vColor;
    out vec2 vExtra;

    void main() {
      vec4 wp = uModel * vec4(aPos, 1.0);
      vWorldPos = wp.xyz;
      vNormal = normalize(mat3(uModel) * aNormal);
      vColor = aColor;
      vExtra = aExtra;
      gl_Position = uMvp * vec4(aPos, 1.0);
    }
    """.trimIndent()

  val GROUND_FRAGMENT =
    """#version 300 es
    precision highp float;
    in vec3 vWorldPos;
    in vec3 vNormal;
    in vec3 vColor;
    in vec2 vExtra;

    out vec4 fragColor;

    uniform vec3 uCamPos;
    uniform vec3 uSunDir;
    uniform vec3 uSunColor;
    uniform vec3 uFogColor;
    uniform float uAmbient;
    uniform float uTime;
    uniform vec3 uHeadlightPos;
    uniform vec3 uHeadlightDir;
    uniform float uHeadlightOn;

    float hash21(vec2 p) {
      p = fract(p * vec2(123.34, 456.21));
      p += dot(p, p + 45.32);
      return fract(p.x * p.y);
    }

    bool isAve(float x) {
      return abs(x + 240.0) < 9.5 || abs(x + 120.0) < 9.5 || abs(x) < 9.5 || abs(x - 114.0) < 9.5;
    }

    bool isStreet(float z) {
      return abs(z + 220.0) < 9.5 || abs(z + 110.0) < 9.5 || abs(z) < 9.5 || abs(z - 110.0) < 9.5 || abs(z - 220.0) < 9.5;
    }

    float nearestAveDist(float x) {
      float d = abs(x + 240.0);
      d = min(d, abs(x + 120.0));
      d = min(d, abs(x));
      d = min(d, abs(x - 114.0));
      return d;
    }

    float nearestStreetDist(float z) {
      float d = abs(z + 220.0);
      d = min(d, abs(z + 110.0));
      d = min(d, abs(z));
      d = min(d, abs(z - 110.0));
      d = min(d, abs(z - 220.0));
      return d;
    }

    void main() {
      vec3 pos = vWorldPos;
      float x = pos.x;
      float z = pos.z;

      vec3 N = normalize(vNormal);
      vec3 baseCol;
      float specPow = 16.0;
      float specStrength = 0.12;

      // Water area (east of x = 172)
      if (vExtra.x > 0.5) {
        float w1 = sin(x * 0.28 + uTime * 2.1 + z * 0.14);
        float w2 = cos(z * 0.34 - uTime * 1.6 + x * 0.12);
        N = normalize(vec3((w1 - w2) * 0.14, 1.0, (w1 + w2) * 0.14));
        vec3 deepWater = vec3(0.03, 0.22, 0.38);
        vec3 shallowWater = vec3(0.06, 0.52, 0.64);
        float shoreMix = smoothstep(235.0, 172.0, x);
        baseCol = mix(deepWater, shallowWater, shoreMix);
        // Foam along beach shoreline
        float foam = smoothstep(177.0, 172.0, x) * (0.5 + 0.5 * sin(z * 0.4 + uTime * 3.0));
        baseCol = mix(baseCol, vec3(0.88, 0.94, 0.97), foam * 0.55);
        specPow = 96.0;
        specStrength = 0.85;
      } else {
        bool inAveBounds = abs(z) <= 275.0;
        bool inStBounds = (x >= -275.0 && x <= 124.0);
        float dAve = nearestAveDist(x);
        float dSt = nearestStreetDist(z);
        bool onAve = inAveBounds && dAve <= 9.5;
        bool onSt = inStBounds && dSt <= 9.5;
        bool onSidewalk = (inAveBounds && dAve <= 12.2 && !onAve) || (inStBounds && dSt <= 12.2 && !onSt);

        if (onAve || onSt) {
          // Realistic textured asphalt
          float grain = hash21(floor(pos.xz * 4.5)) * 0.045;
          baseCol = vec3(0.11, 0.12, 0.14) + grain;
          specPow = 32.0;
          specStrength = 0.24;

          // Center yellow double line & dashed lane markers
          if (onAve && !onSt) {
            if (dAve < 0.22 && abs(dAve - 0.14) < 0.06) {
              baseCol = vec3(0.92, 0.72, 0.14);
            } else if (abs(dAve - 4.7) < 0.11 && fract(z * 0.11) < 0.5) {
              baseCol = vec3(0.86, 0.88, 0.90);
            } else if (dAve > 8.95) {
              baseCol = vec3(0.75, 0.76, 0.78);
            }
          } else if (onSt && !onAve) {
            if (dSt < 0.22 && abs(dSt - 0.14) < 0.06) {
              baseCol = vec3(0.92, 0.72, 0.14);
            } else if (abs(dSt - 4.7) < 0.11 && fract(x * 0.11) < 0.5) {
              baseCol = vec3(0.86, 0.88, 0.90);
            } else if (dSt > 8.95) {
              baseCol = vec3(0.75, 0.76, 0.78);
            }
          } else if (onAve && onSt) {
            // Zebra crosswalks near intersection edges
            if ((dAve > 7.6 && dAve < 9.3 && fract(z * 0.55) < 0.5) ||
                (dSt > 7.6 && dSt < 9.3 && fract(x * 0.55) < 0.5)) {
              baseCol = vec3(0.84, 0.85, 0.86);
            }
          }
        } else if (onSidewalk) {
          float tile = hash21(floor(pos.xz * 0.8)) * 0.04;
          baseCol = vec3(0.46, 0.46, 0.48) + tile;
        } else if (x > 130.0) {
          // Golden beach sand
          float sandNoise = hash21(floor(pos.xz * 1.8)) * 0.05;
          baseCol = vec3(0.82, 0.71, 0.52) + sandNoise;
        } else if (x > -104.0 && x < -16.0 && z > 16.0 && z < 94.0) {
          // Central Stunt Plaza concrete & turf
          bool gridLine = fract(x * 0.125) < 0.04 || fract(z * 0.125) < 0.04;
          baseCol = gridLine ? vec3(0.32, 0.34, 0.38) : vec3(0.24, 0.26, 0.30);
        } else {
          // City blocks & park turf
          float grassNoise = hash21(floor(pos.xz * 1.2)) * 0.06;
          baseCol = vec3(0.16, 0.29, 0.17) + grassNoise;
        }
      }

      vec3 L = normalize(uSunDir);
      vec3 V = normalize(uCamPos - pos);
      vec3 H = normalize(L + V);

      float ndl = max(dot(N, L), 0.0);
      float spec = pow(max(dot(N, H), 0.0), specPow) * specStrength;

      // Headlight spotlight on asphalt
      vec3 headGlow = vec3(0.0);
      if (uHeadlightOn > 0.5) {
        vec3 toFrag = pos - uHeadlightPos;
        float distH = length(toFrag);
        if (distH < 52.0 && distH > 0.5) {
          vec3 dirH = toFrag / distH;
          float cone = smoothstep(0.74, 0.94, dot(dirH, normalize(uHeadlightDir)));
          float atten = 1.0 - smoothstep(10.0, 52.0, distH);
          headGlow = vec3(1.0, 0.94, 0.78) * cone * atten * 0.85;
        }
      }

      vec3 lit = baseCol * (uAmbient + ndl * uSunColor * 0.92) + uSunColor * spec + baseCol * headGlow;

      float dist = length(uCamPos - pos);
      float fogFactor = 1.0 - exp(-pow(dist * 0.0036, 1.45));
      vec3 finalCol = mix(lit, uFogColor, clamp(fogFactor, 0.0, 0.94));
      fragColor = vec4(finalCol, 1.0);
    }
    """.trimIndent()

  val LIT_FRAGMENT =
    """#version 300 es
    precision highp float;
    in vec3 vWorldPos;
    in vec3 vNormal;
    in vec3 vColor;
    in vec2 vExtra;

    out vec4 fragColor;

    uniform vec3 uCamPos;
    uniform vec3 uSunDir;
    uniform vec3 uSunColor;
    uniform vec3 uFogColor;
    uniform vec3 uSkyZenith;
    uniform float uAmbient;
    uniform float uWindowGlow;
    uniform vec3 uPaintOverride;
    uniform float uUsePaintOverride;
    uniform vec3 uEmissiveTint;
    uniform float uHeadlightOn;

    void main() {
      float matType = vExtra.x;
      float shine = max(vExtra.y, 4.0);

      vec3 baseCol = vColor;
      // Material 1.0 = Vehicle Metallic Paint (overrideable)
      if (matType > 0.5 && matType < 1.5 && uUsePaintOverride > 0.5) {
        baseCol = uPaintOverride;
      }

      // Material 3.0 = Self-illuminated headlights / taillights / flames / beacons
      if (matType > 2.5) {
        vec3 glowCol = baseCol * (1.15 + uHeadlightOn * 0.45) + uEmissiveTint;
        float dist = length(uCamPos - vWorldPos);
        float fogF = clamp(1.0 - exp(-pow(dist * 0.0036, 1.45)), 0.0, 0.85);
        fragColor = vec4(mix(glowCol, uFogColor, fogF), 1.0);
        return;
      }

      vec3 N = normalize(vNormal);
      vec3 L = normalize(uSunDir);
      vec3 V = normalize(uCamPos - vWorldPos);
      vec3 H = normalize(L + V);

      // Hemisphere ambient (sky top + warm asphalt bounce)
      float hemi = N.y * 0.5 + 0.5;
      vec3 hemiCol = mix(vec3(0.18, 0.16, 0.18), uSkyZenith * 0.55 + vec3(0.32), hemi);

      float ndl = max(dot(N, L), 0.0);
      float spec = pow(max(dot(N, H), 0.0), shine);
      float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0);

      float specWeight = (matType > 0.5 && matType < 1.5) ? 0.95 : 0.35;
      float rimWeight = (matType > 0.5 && matType < 1.5) ? 0.48 : 0.12;

      vec3 lit = baseCol * (hemiCol * uAmbient + ndl * uSunColor * 0.95)
               + uSunColor * spec * specWeight
               + mix(uSunColor, uSkyZenith, 0.4) * fresnel * rimWeight;

      // Material 2.0 = Skyscraper Facade with procedural illuminated glass window grid!
      if (matType > 1.5 && matType < 2.5 && abs(N.y) < 0.35) {
        float wx = fract((vWorldPos.x + vWorldPos.z) * 0.32);
        float wy = fract(vWorldPos.y * 0.28);
        bool isWindow = (wx > 0.18 && wx < 0.82) && (wy > 0.24 && wy < 0.80);
        if (isWindow) {
          float cellHash = fract(sin(dot(floor(vec2(vWorldPos.x + vWorldPos.z, vWorldPos.y) * 0.32), vec2(12.9898, 78.233))) * 43758.5453);
          float litMask = step(0.36, cellHash) * uWindowGlow;
          vec3 glassCol = mix(vec3(0.12, 0.19, 0.28), vec3(1.0, 0.82, 0.48), litMask);
          float glassFresnel = pow(1.0 - max(dot(N, V), 0.0), 2.5);
          lit = mix(lit * 0.45, glassCol * (0.65 + litMask * 1.1) + uSkyZenith * glassFresnel * 0.65, 0.85);
        }
      }

      float dist = length(uCamPos - vWorldPos);
      float fogFactor = 1.0 - exp(-pow(dist * 0.0036, 1.45));
      vec3 finalCol = mix(lit, uFogColor, clamp(fogFactor, 0.0, 0.94));
      fragColor = vec4(finalCol, 1.0);
    }
    """.trimIndent()
}
