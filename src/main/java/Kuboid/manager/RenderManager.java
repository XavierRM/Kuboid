package Kuboid.manager;

import Kuboid.manager.entity.Entity;
import Kuboid.manager.entity.Model;
import Kuboid.manager.utils.Transformation;
import Kuboid.manager.utils.Utils;
import test.Launcher;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class RenderManager {

    private final WindowManager window;
    private ShaderManager shader;
    private final Camera camera;

    private boolean isWireframe;

    public RenderManager(Camera camera, boolean isWireframe) {
        window = Launcher.getWindow();
        this.camera = camera;
        this.isWireframe = isWireframe;
    }

    public void init() throws Exception {
        shader = new ShaderManager();
        if (isWireframe) {
            shader.createVertexShader(Utils.loadResource("/shaders/vertexWireframe.vs"));
            shader.createFragmentShader(Utils.loadResource("/shaders/fragmentWireframe.fs"));
        } else {
            shader.createVertexShader(Utils.loadResource("/shaders/vertexTexture.vs"));
            shader.createFragmentShader(Utils.loadResource("/shaders/fragmentTexture.fs"));
        }
        shader.link();

        if (!isWireframe)
            shader.createUniform("textureSampler");

        shader.createUniform("transformationMatrix");
        shader.createUniform("projectionMatrix");
        shader.createUniform("viewMatrix");

    }

    public void render(Entity entity) {
        Model model = entity.getModel();

        shader.bind();

        if (!isWireframe)
            shader.setUniform("textureSampler", 0);

        shader.setUniform("transformationMatrix", Transformation.createTransformationMatrix(entity));
        shader.setUniform("projectionMatrix", window.getProjectionMatrix());
        shader.setUniform("viewMatrix", Transformation.getViewMatrix(this.camera));

        glBindVertexArray(model.getId());
        glEnableVertexAttribArray(0);

        if (!isWireframe) {
            glEnableVertexAttribArray(1);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, model.getTexture().getId());
        }

        glDrawElements(GL_TRIANGLES, model.getVertexCount(), GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);

        if (!isWireframe)
            glDisableVertexAttribArray(1);

        glBindVertexArray(0);
        shader.unbind();
    }

    public void setWireframe(boolean wireframe) {
        isWireframe = wireframe;
    }

    public void switchRenderer() throws Exception {
        clear();
        cleanup();
        init();
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void cleanup() {
        shader.cleanup();
    }
}
