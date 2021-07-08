attribute vec3 a_Position;
attribute vec2 a_texCoord;
uniform mat4 vMatrix;
uniform mat4 vCoordMatrix;
varying vec2 v_texCoord;
void main(){
    gl_Position = vMatrix * vec4(a_Position, 1);
    v_texCoord = (vCoordMatrix*vec4(a_texCoord, 0, 1)).xy;
}