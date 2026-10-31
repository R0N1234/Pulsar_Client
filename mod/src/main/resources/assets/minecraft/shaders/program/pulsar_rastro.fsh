#version 120

// Rastro de Cometa do Pulsar Client: mistura a imagem nova com a do quadro anterior (desfoque de movimento).

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;

uniform float Rastro;

void main() {
    vec3 agora = texture2D(DiffuseSampler, texCoord).rgb;
    vec3 antes = texture2D(PrevSampler, texCoord).rgb;
    gl_FragColor = vec4(mix(agora, antes, Rastro), 1.0);
}
