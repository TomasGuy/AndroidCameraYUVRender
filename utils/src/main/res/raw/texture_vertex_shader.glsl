attribute vec3 a_Position;
attribute vec2 a_texCoord;
uniform mat4 vMatrix;
varying vec2 v_texCoord;
void main(){
    gl_Position = vMatrix * vec4(a_Position, 1);
    v_texCoord = a_texCoord;
}