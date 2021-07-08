#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D s_texture;
void main() {
    vec4 in_color = texture2D(s_texture, v_texCoord);
    gl_FragColor = in_color;
}