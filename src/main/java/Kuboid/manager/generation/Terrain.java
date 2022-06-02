package Kuboid.manager.generation;

import Kuboid.manager.ObjectLoader;
import Kuboid.manager.entity.Entity;
import Kuboid.manager.model.Model;
import Kuboid.manager.model.Texture;
import Kuboid.manager.utils.PerlinNoise;
import Kuboid.manager.utils.SimplexNoise;
import Kuboid.manager.voxel.Voxel;
import Kuboid.manager.voxel.VoxelType;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.*;
import java.util.random.RandomGenerator;

import static Kuboid.manager.utils.Utils.diamondSquare;

public class Terrain implements Runnable {

    private ObjectLoader loader;
    private Model model, newModel;
    private PerlinNoise generator;
    private SimplexNoise simplexNoise;

    private long size;
    private long index = 0;
    private long chunkSize, chunkDepth;
    private boolean plain;
    private boolean isWireframe;
    private boolean running = true;
    private Vector3f camPos;

    private Map<Model, List<Entity>> entitiesMap = Collections.synchronizedMap(new HashMap<>());
    private List<ChunkMesh> chunks = Collections.synchronizedList(new ArrayList<>());
    private List<Entity> entities = Collections.synchronizedList(new ArrayList<>());
    private List<Vector3f> usedPos = Collections.synchronizedList(new ArrayList<>());
//    private List<Vector3f> usedAbsolutePositions = Collections.synchronizedList(new ArrayList<>());

    private final float[] verticesDirt = new float[]{
            -0.5f, 0.5f, 0.5f, //0
            -0.5f, -0.5f, 0.5f, //1
            0.5f, -0.5f, 0.5f, //2
            0.5f, 0.5f, 0.5f, //3
            //Front
            -0.5f, 0.5f, -0.5f, //4
            -0.5f, -0.5f, -0.5f, //5
            0.5f, -0.5f, -0.5f, //6
            0.5f, 0.5f, -0.5f, //7
            //Back
            -0.5f, 0.5f, -0.5f, //8
            -0.5f, -0.5f, -0.5f, //9
            0.5f, -0.5f, -0.5f, //10
            0.5f, 0.5f, -0.5f, //11
            //Duplicates for proper texture rendering

    };

    private final float[] verticesGrassBlock = new float[]{
            -0.5f, 0.5f, 0.5f, //0
            -0.5f, -0.5f, 0.5f, //1
            0.5f, -0.5f, 0.5f, //2
            0.5f, 0.5f, 0.5f, //3
            //Front
            -0.5f, 0.5f, -0.5f, //4
            -0.5f, -0.5f, -0.5f, //5
            0.5f, -0.5f, -0.5f, //6
            0.5f, 0.5f, -0.5f, //7
            //Back
            -0.5f, 0.5f, -0.5f, //8
            -0.5f, 0.5f, 0.5f, //9
            0.5f, 0.5f, 0.5f, //10
            0.5f, 0.5f, -0.5f, //11
            //Top
            -0.5f, -0.5f, -0.5f, //12
            -0.5f, -0.5f, 0.5f, //13
            0.5f, -0.5f, 0.5f, //14
            0.5f, -0.5f, -0.5f, //15
            //Bottom

    };

    private final float[] textCoordsDirt = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            //
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            //
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    private final float[] textCoordsGrassBlock = new float[]{
            0.0f, 0.25f,
            0.0f, 0.5f,
            0.25f, 0.5f,
            0.25f, 0.25f,
            //ok
            0.25f, 0.25f,
            0.25f, 0.5f,
            0.0f, 0.5f,
            0.0f, 0.25f,
            //ok
            0.5f, 0.5f,
            0.5f, 0.75f,
            0.75f, 0.75f,
            0.75f, 0.5f,
            //Top
            0.0f, 0.5f,
            0.0f, 0.75f,
            0.25f, 0.75f,
            0.25f, 0.5f,
            //Bottom
    };

    private final int[] indicesDirt = new int[]{
            0, 1, 2, 2, 3, 0, //front
            7, 6, 5, 5, 4, 7, //back
            8, 0, 3, 3, 11, 8, //top
            9, 1, 2, 2, 10, 9, // bottom
            4, 5, 1, 1, 0, 4, //left
            3, 2, 6, 6, 7, 3, //right
    };

    private final int[] indicesGrassBlock = new int[]{
            0, 1, 2, 2, 3, 0, //front
            7, 6, 5, 5, 4, 7, //back
            8, 9, 10, 10, 11, 8, //top
            12, 13, 14, 14, 15, 12, // bottom
            4, 5, 1, 1, 0, 4, //left
            3, 2, 6, 6, 7, 3, //right
    };

    public Terrain(long chunksPerAxis, long chunkSize, boolean plain, boolean isWireframe, Vector3f camPos) {
        this.size = chunksPerAxis * chunkSize;
        this.chunkSize = chunkSize;
        this.chunkDepth = 1;
        this.plain = plain;
        this.isWireframe = isWireframe;
        this.camPos = camPos;
        this.loader = new ObjectLoader();
        this.generator = new PerlinNoise();
        this.simplexNoise = new SimplexNoise();

        RandomGenerator random = RandomGenerator.of("Random");
        generator.setSeed(random.nextLong());

        if (isWireframe)
            model = loader.loadModel(verticesDirt, indicesDirt);
        else {
            //model = loader.loadModel(verticesDirt, textCoordsDirt, indicesDirt);
            /*try {
                model.setTexture(new Texture(loader.loadTexture("textures/dirt.png")));
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            /*model = loader.loadModel(verticesGrassBlock, textCoordsGrassBlock, indicesGrassBlock);
            try {
                model.setTexture(new Texture(loader.loadTexture("textures/grassblock.png")));
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }
    }

    public void setWireframe(boolean wireframe) {
        isWireframe = wireframe;
    }

    public void generateTerrain() {
        long x, y = 0, z;

        Vector3f vector;
        Chunk chunk;

        //This could be optimized for sure
        for (x = ((int) camPos.x - (size / 2)) / chunkSize; x < ((int) camPos.x + (size / 2)) / chunkSize; x++) {
            for (z = ((int) camPos.z - (size / 2)) / chunkSize; z < ((int) camPos.z + (size / 2)) / chunkSize; z++) {
                vector = new Vector3f(x * chunkSize, 0, z * chunkSize);

                if (!usedPos.contains(vector)) {

                    List<Voxel> blocks = new ArrayList<>();

                    for (int i = 0; i < chunkSize; i++) {
                        for (int j = 0; j < chunkSize; j++) {
                            //float k = generator.generateHeight((int) (i + (x * chunkSize)), (int) (j + (z * chunkSize)));

                            double nx = (i + (x * chunkSize));
                            double nz = (j + (z * chunkSize));

                            /*
                             * The values multiplying the nx and nz variables are the frequency of the noise generation,
                             * for terrain generation this translates into a flatter world when values are near 0 and more
                             * hills or extreme changes in altitude when does values get further away from 0, to get a better
                             * terrain generation we can combine multiple wavelengths of noise giving us the perfect combination
                             * of flat and hilly terrain.
                             * */

                            double noise = simplexNoise.noise(0.01 * nx, 0.01 * nz);
                            noise += 0.5 * simplexNoise.noise(0.02 * nx, 0.02 * nz);
                            noise += 0.25 * simplexNoise.noise(0.04 * nx, 0.04 * nz);
                            double k = (1 + noise) / 2;

                            //This returns values from -1 to 1, we have to convert it to a 0-16 for example
                            //float k = (float) simplexNoise.noise(nx, ny)
                            //        + 0.5f * (float) simplexNoise.noise( 2 * nx, 2 * ny)
                            //        + 0.25f * (float) simplexNoise.noise( 4 * nx, 4 * ny);

                            //k = (float) simplexNoise.noise(0.25 * nx, 0.25 * ny);

                            //k = (float) Math.pow((k * 3)/(1 + 0.5 + 0.25), 3);
                            //k = (float) Math.pow((k * 16), 1.5);

                            //for (int k = (int) perlinNoiseGenerator.generateHeight((int) (i + (x * chunkSize)), (int) (j + (z * chunkSize))); k > -chunkDepth; k--) {
                            //System.out.println(Math.round(simplexNoise.noise(0.25 * nx, 0.25 * ny)));

                            //Altitude ranges from 0 to 16
                            long h = Math.round(k / 0.0625);
                            //for (long h = Math.round(k / 0.0625); h > -2; h--) {
                            blocks.add(new Voxel(new Vector3f(i, h, j), VoxelType.DIRT));
                            //usedAbsolutePositions.add(new Vector3f((vector.x * chunkSize) + i, vector.y + k, (vector.z * chunkSize) + j));
                            //}
                        }
                    }

                    chunk = new Chunk(blocks, vector, chunkSize);

                    usedPos.add(vector);
                    chunks.add(new ChunkMesh(chunk));
                }
            }
        }
    }

    public float[][] generateTerrainDiamondSquare() {
        int TOP_LEFT = 0;
        int TOP_RIGHT = 1;
        int BOTTOM_RIGHT = 2;
        int BOTTOM_LEFT = 3;

        List<Vector2i> corners = new ArrayList<>();

        //TOP_LEFT
        corners.add(new Vector2i((int) (camPos.x - (size / 2)), (int) (camPos.z - (size / 2))));
        //TOP_RIGHT
        corners.add(new Vector2i((int) (camPos.x + (size / 2)), (int) (camPos.z - (size / 2))));
        //BOTTOM_RIGHT
        corners.add(new Vector2i((int) (camPos.x - (size / 2)), (int) (camPos.z + (size / 2))));
        //BOTTOM_LEFT
        corners.add(new Vector2i((int) (camPos.x + (size / 2)), (int) (camPos.z + (size / 2))));

        System.out.println(corners.toString());
        System.out.println();

        //Matrix to calculate DiamondSquare result
        float[][] m = new float[(int) size + 1][(int) size + 1];

        //PerlinNoise
        m[0][0] = generator.generateHeight(corners.get(TOP_LEFT).x, corners.get(TOP_LEFT).y);
        m[0][m.length - 1] = generator.generateHeight(corners.get(TOP_RIGHT).x, corners.get(TOP_RIGHT).y);
        m[m.length - 1][m.length - 1] = generator.generateHeight(corners.get(BOTTOM_RIGHT).x, corners.get(BOTTOM_RIGHT).y);
        m[m.length - 1][0] = generator.generateHeight(corners.get(BOTTOM_LEFT).x, corners.get(BOTTOM_LEFT).y);

        //System.out.println(generator.generateHeight(corners.get(TOP_LEFT).x, corners.get(TOP_LEFT).y));
        //System.out.println(generator.generateHeight(corners.get(TOP_RIGHT).x, corners.get(TOP_RIGHT).y));
        //System.out.println(generator.generateHeight(corners.get(BOTTOM_RIGHT).x, corners.get(BOTTOM_RIGHT).y));
        //System.out.println(generator.generateHeight(corners.get(BOTTOM_LEFT).x, corners.get(BOTTOM_LEFT).y));
        //System.out.println();

        var result = diamondSquare(m, new Vector2i(0, 0), new Vector2i((int) size, (int) size));

        /*for (float[] row : result) {
            System.out.println(Arrays.toString(row));
        }*/

        return result;

    }

    public void update(Vector3f camPos) {
        this.camPos = camPos;

        if (index < chunks.size()) {
            for (int i = 0; i < chunks.size(); i++) {
                newModel = loader.loadModel(chunks.get(i).positions, chunks.get(i).uvs);

                try {
                    newModel.setTexture(new Texture(loader.loadTexture("textures/dirt.png")));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                entities.add(new Entity(newModel, chunks.get(i).chunk.getOrigin(), new Vector3f(0, 0, 0), 1));

            }

            index++;
        }
    }

    public Map<Model, List<Entity>> getTerrain() {
        entitiesMap = new HashMap<>();
        //activeChunks = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);

            Vector3f pos = entity.getPos();

            float distX = (camPos.x - pos.x);
            float distZ = (camPos.z - pos.z);

            if ((Math.abs(distX) <= (size / 2)) && (Math.abs(distZ) <= (size / 2))) {
                addEntity(entity);

                /*for (int j = 0; j < chunks.size(); j++) {
                    if(pos.equals(chunks.get(j).chunk.getOrigin())) {
                        activeChunks.add(chunks.get(j));
                    }
                }*/
            }
        }

        return entitiesMap;
    }


    public void addEntity(Entity entity) {
        List<Entity> entitiesList = entitiesMap.get(entity.getModel());
        if (entitiesList != null) {
            if (!entitiesList.contains(entity)) {
                entitiesList.add(entity);
                entitiesMap.put(entity.getModel(), entitiesList);
            }
        } else {
            List<Entity> aux = new ArrayList<>();
            aux.add(entity);
            entitiesMap.put(entity.getModel(), aux);
        }
    }

    public void stopLoop() {
        running = false;
    }

    public void run() {
        try {
            generateTerrain();
            while (running) {
                generateTerrain();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
