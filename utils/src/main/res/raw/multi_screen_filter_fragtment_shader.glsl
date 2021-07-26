#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texCoord;
uniform samplerExternalOES s_texture;
void main() {
    float newX, newY;

    if(v_texCoord.x<1.0/3.0) {
        newX = v_texCoord.x * 3.0;
    } else if(v_texCoord.x>=1.0/3.0 && v_texCoord.x<2.0/3.0) {
        newX = (v_texCoord.x-1.0/3.0) * 3.0;
    } else if(v_texCoord.x>=2.0/3.0) {
        newX = (v_texCoord.x-2.0/3.0) * 3.0;
    }

    if(v_texCoord.y<1.0/3.0) {
        newY = v_texCoord.y * 3.0;
    } else if(v_texCoord.y>=1.0/3.0 && v_texCoord.y<2.0/3.0) {
        newY = (v_texCoord.y-1.0/3.0) * 3.0;
    } else if(v_texCoord.y>=2.0/3.0) {
        newY = (v_texCoord.y-2.0/3.0) * 3.0;
    }

    vec4 in_color = texture2D(s_texture, vec2(newX, newY));

    gl_FragColor = in_color;
}