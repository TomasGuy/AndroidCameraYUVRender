#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D sampler_y;
uniform sampler2D sampler_uv;

void main() {
    //yuv420->rgb
    float y, u, v;
    y = texture2D(sampler_y, v_texCoord).r;
    u = texture2D(sampler_uv, v_texCoord).r- 0.5;
    v = texture2D(sampler_uv, v_texCoord).a- 0.5;
    vec3 rgb;
    rgb.r = y + 1.370705 * v;
    rgb.g = y - 0.337633 * u - 0.698001 * v;
    rgb.b = y + 1.732446 * u;

    gl_FragColor=vec4(rgb, 1);
}