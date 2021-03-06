package Kuboid.manager;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderManager {

    private final int programId;
    private int vertexShaderId, fragmentShaderId;

    private final Map<String, Integer> uniforms;

    public ShaderManager() throws Exception {
        programId = glCreateProgram();

        if(programId == 0)
            throw new Exception("Could not create shader");

        uniforms = new HashMap<>();

    }

    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programId, uniformName);

        if(uniformLocation < 0)
            throw new Exception("Could not find uniform " + uniformName);

        uniforms.put(uniformName, uniformLocation);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(uniforms.get(uniformName), false, value.get(stack.mallocFloat(16)));
        }
    }

    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }

    public void setUniform(String uniformName, Vector4f value) {
        glUniform4f(uniforms.get(uniformName), value.x, value.y, value.z, value.w);
    }

    public void setUniform(String uniformName, boolean value) {
        float res = 0;
        if(value)
            res = 1;

        glUniform1f(uniforms.get(uniformName), res);
    }

    public void setUniform(String uniformName, int value) {
        glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, float value) {
        glUniform1f(uniforms.get(uniformName), value);
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    public int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);

        if(shaderId == 0)
            throw new Exception("Error creating shader. Type: " + shaderType);

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if(glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0)
            throw new Exception("Error compiling shader code. TYPE: " + shaderType + "Info: " + glGetShaderInfoLog(shaderId, 1024));

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() throws Exception{
        glLinkProgram(programId);

        if(glGetProgrami(programId, GL_LINK_STATUS) == 0)
            throw new Exception("Error linking shader code. Info: " + glGetProgramInfoLog(programId, 1024));

        if(vertexShaderId != 0)
            glDetachShader(programId, vertexShaderId);

        if(fragmentShaderId != 0)
            glDetachShader(programId, fragmentShaderId);

        glValidateProgram(programId);

        if(glGetProgrami(programId, GL_VALIDATE_STATUS) == 0)
            throw new Exception("Unable to validate shader code. Info: " + glGetProgramInfoLog(programId, 1024));
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();

        if(programId != 0)
            glDeleteProgram(programId);
    }
}
