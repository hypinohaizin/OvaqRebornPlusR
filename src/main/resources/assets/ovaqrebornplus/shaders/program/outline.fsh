#version 150

#define TWO_PI 6.28318530718f

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

//
uniform int sobel;

//
uniform vec2 texelSize;
uniform vec4 color;
uniform int samples;
uniform int steps;
uniform int dots;
uniform int dotRadius;

uniform int fastOutline;
uniform float radius;
uniform int glow;
uniform float glowRadius;

// Computes the distance from a vec2 to the nearest texture edge
vec4 computeEdgeDistance(vec2 coords)
{
    vec4 color = vec4(0.0f);

    if (fastOutline != 1)
    {
        float closest = radius * 2.0f + 2.0f;
        for (float x = -radius; x <= radius; x++)
        {
            for (float y = -radius; y <= radius; y++)
            {
                vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));
                if (currentColor.a > 0)
                {
                    float currentDist = sqrt(x * x + y * y);
                    if (currentDist < closest)
                    {
                        closest = currentDist;
                        color = currentColor;
                    }
                }
            }
        }

        return vec4(color.rgb, closest);
    }

    float minDist = radius * 2.0f;
    float stepSize = radius / float(steps);
    for (float r = stepSize; r < radius; r += stepSize)
    {
        for (int i = 0; i < samples; ++i)
        {
            float angle = float(i) * TWO_PI / float(samples);
            vec2 offset = vec2(cos(angle), sin(angle)) * r;
            vec2 offsetCoord = coords + offset * texelSize;

            vec4 offsetTex = texture(DiffuseSampler, offsetCoord);
            if (offsetTex.a > 0.0)
            {
                float dist = length(offset);
                minDist = min(minDist, dist);
                color = offsetTex;

                if (minDist <= radius)
                {
                    return vec4(color.rgb, minDist);
                }
            }
        }
    }

    return vec4(color.rgb, minDist);
}

void main()
{
    vec4 centerTex = texture(DiffuseSampler, texCoord);

    vec3 colorFill = color.rgb;
    if (sobel != 0)
    {
        colorFill = centerTex.rgb;
    }
    if (centerTex.a > 0.0)
    {
        vec2 pixelCoord = mod(gl_FragCoord.xy, float(dotRadius));
        if (dots != 0 && pixelCoord.x <= 1.0f && pixelCoord.y <= 1.0f)
        {
            fragColor = vec4(colorFill, 1.0f);
        }
        else
        {
            fragColor = vec4(colorFill, color.a);
        }
    }
    else
    {
        vec4 edgeDist = computeEdgeDistance(texCoord);

        vec3 colorOutline = color.rgb;
        if (sobel != 0)
        {
            colorOutline = edgeDist.rgb;
        }
        float alpha = 1.0f - smoothstep(radius * 0.7, radius, edgeDist.a);

        if (radius > 0.0f && edgeDist.a <= radius)
        {
            if (glow != 0)
            {
                float glowAlpha = exp(-edgeDist.a * edgeDist.a * glowRadius * 0.2);
                fragColor = vec4(colorOutline.rgb, alpha * glowAlpha);
            }
            else
            {
                fragColor = vec4(colorOutline.rgb, alpha);
            }
        }
        else
        {
            fragColor = vec4(0.0f);
        }
    }
}
