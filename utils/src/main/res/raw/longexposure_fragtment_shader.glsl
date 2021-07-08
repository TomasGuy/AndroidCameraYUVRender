#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D avg_texture;
uniform sampler2D s_texture;
uniform int size;
void main() {
    if(size==1) {
        vec4 in_color = texture2D(s_texture, v_texCoord);
        gl_FragColor = in_color;
    } else {
        vec4 avg_color = texture2D(avg_texture, v_texCoord);
        vec4 in_color = texture2D(s_texture, v_texCoord);

        gl_FragColor = mix(avg_color, in_color, 1.0/float(size+1));
    }
}