#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D sampler_y;
uniform sampler2D sampler_u;
uniform sampler2D sampler_v;

vec4 YuvToRgb(vec2 uv) {
    float y, u, v, r, g, b;
    y = texture2D(sampler_y, uv).r;
    u = texture2D(sampler_u, uv).r;
    v = texture2D(sampler_v, uv).r;
    u = u - 0.5;
    v = v - 0.5;
    r = y + 1.403 * v;
    g = y - 0.344 * u - 0.714 * v;
    b = y + 1.770 * u;
    return vec4(r, g, b, 1.0);
}

void main() {
        gl_FragColor=YuvToRgb(v_texCoord);
}