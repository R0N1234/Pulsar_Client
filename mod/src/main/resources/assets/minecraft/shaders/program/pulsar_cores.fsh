#version 120

// Cores Vivas do Pulsar Client: contraste, brilho e saturação da imagem do mundo.

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;

uniform float Saturacao;
uniform float Contraste;
uniform float Brilho;

void main() {
    vec3 cor = texture2D(DiffuseSampler, texCoord).rgb;
    cor = (cor - 0.5) * Contraste + 0.5 + Brilho;
    float luz = dot(cor, vec3(0.299, 0.587, 0.114));
    cor = mix(vec3(luz), cor, Saturacao);
    gl_FragColor = vec4(clamp(cor, 0.0, 1.0), 1.0);
}
