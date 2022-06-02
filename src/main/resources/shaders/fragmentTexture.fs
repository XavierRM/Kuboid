#version 400 core

in vec2 fragTextureCoord;

out vec4 fragColour;

uniform sampler2D textureSampler;

void main() {
    fragColour = ((fragTextureCoord.x/5) * (fragTextureCoord.y/3)) + texture(textureSampler, fragTextureCoord);
}