package game;

import maze.Mesh;
import maze.GoalMesh;
import maze.MazeData;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.nio.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private int W=800, H=600;
    private int prog, uMVP, uLightPos, uAmbient, uFogColor, uFogDensity;
    private Mesh maze = new Mesh();
    private GoalMesh goal = new GoalMesh();
    private Camera camera = new Camera();
    private boolean won = false;
    private double lastX=-1, lastY=-1;

    private static final String VERT =
        "#version 330 core\n" +
        "layout(location=0) in vec3 aPos;\n" +
        "layout(location=1) in vec3 aNorm;\n" +
        "layout(location=2) in vec3 aCol;\n" +
        "uniform mat4 uMVP;\n" +
        "out vec3 vNorm,vCol,vPos;\n" +
        "void main(){ gl_Position=uMVP*vec4(aPos,1.0); vNorm=aNorm; vCol=aCol; vPos=aPos; }\n";

    private static final String FRAG =
        "#version 330 core\n" +
        "in vec3 vNorm,vCol,vPos;\n" +
        "uniform vec3 uLightPos; uniform float uAmbient;\n" +
        "uniform vec3 uFogColor; uniform float uFogDensity;\n" +
        "out vec4 fragColor;\n" +
        "void main(){\n" +
        "  vec3 n=normalize(vNorm); vec3 l=normalize(uLightPos-vPos);\n" +
        "  float dist=length(uLightPos-vPos);\n" +
        "  float att=1.0/(1.0+0.03*dist+0.003*dist*dist);\n" +
        "  float lit=uAmbient+(1.0-uAmbient)*max(dot(n,l),0.0)*att;\n" +
        "  vec3 col=vCol*clamp(lit,0.0,1.0);\n" +
        "  float fog=exp(-uFogDensity*dist*dist);\n" +
        "  fragColor=vec4(mix(uFogColor,col,clamp(fog,0.0,1.0)),1.0);\n" +
        "}\n";

    public static void main(String[] args) { new Main().run(); }

    public void run() {
        init(); loop();
        glfwFreeCallbacks(window); glfwDestroyWindow(window);
        glfwTerminate(); glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("glfwInit failed");
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3); glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE); glfwWindowHint(GLFW_SAMPLES,4);

        window=glfwCreateWindow(W,H,"Labyrint",NULL,NULL);
        if (window==NULL) throw new RuntimeException("window failed");
        camera.addYaw((float)(Math.PI/2));

        glfwSetMouseButtonCallback(window,(win,btn,action,mods)->{
            if (btn==GLFW_MOUSE_BUTTON_RIGHT) {
                if (action==GLFW_PRESS){glfwSetInputMode(win,GLFW_CURSOR,GLFW_CURSOR_DISABLED);lastX=-1;lastY=-1;}
                else glfwSetInputMode(win,GLFW_CURSOR,GLFW_CURSOR_NORMAL);
            }
        });
        glfwSetCursorPosCallback(window,(win,xp,yp)->{
            if (lastX<0){lastX=xp;lastY=yp;return;}
            camera.addYaw((float)(xp-lastX)*0.0025f);
            camera.addPitch((float)(yp-lastY)*0.0025f);
            lastX=xp;lastY=yp;
        });
        glfwSetKeyCallback(window,(win,key,sc,action,mods)->{
            if (key==GLFW_KEY_ESCAPE&&action==GLFW_PRESS) glfwSetWindowShouldClose(win,true);
        });

        try (MemoryStack s=stackPush()) {
            IntBuffer pw=s.mallocInt(1),ph=s.mallocInt(1);
            glfwGetFramebufferSize(window,pw,ph); W=pw.get(); H=ph.get();
        }
        glfwMakeContextCurrent(window); glfwSwapInterval(1); glfwShowWindow(window);
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST); glEnable(GL_CULL_FACE); glEnable(GL_MULTISAMPLE);
        glClearColor(0.18f,0.14f,0.10f,1f); glViewport(0,0,W,H);

        int v=compile(GL_VERTEX_SHADER,VERT),f=compile(GL_FRAGMENT_SHADER,FRAG);
        prog=glCreateProgram(); glAttachShader(prog,v); glAttachShader(prog,f);
        glLinkProgram(prog); glDeleteShader(v); glDeleteShader(f);
        uMVP=glGetUniformLocation(prog,"uMVP"); uLightPos=glGetUniformLocation(prog,"uLightPos");
        uAmbient=glGetUniformLocation(prog,"uAmbient"); uFogColor=glGetUniformLocation(prog,"uFogColor");
        uFogDensity=glGetUniformLocation(prog,"uFogDensity");
        maze.build(); goal.build();
    }

    private int compile(int type,String src){int id=glCreateShader(type);glShaderSource(id,src);glCompileShader(id);return id;}

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            float spd=0.10f;
            if (glfwGetKey(window,GLFW_KEY_W)==GLFW_PRESS) camera.moveForward( spd);
            if (glfwGetKey(window,GLFW_KEY_S)==GLFW_PRESS) camera.moveForward(-spd);
            if (glfwGetKey(window,GLFW_KEY_D)==GLFW_PRESS) camera.moveRight(   spd);
            if (glfwGetKey(window,GLFW_KEY_A)==GLFW_PRESS) camera.moveRight(  -spd);

            glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
            glUseProgram(prog);
            glUniformMatrix4fv(uMVP,false,mul(perspective(),camera.viewMatrix()));
            glUniform3f(uLightPos,camera.x,camera.y,camera.z);
            glUniform1f(uAmbient,0.45f);
            glUniform3f(uFogColor,0.18f,0.14f,0.10f);
            glUniform1f(uFogDensity,0.004f);

            glBindVertexArray(maze.vao); glDrawArrays(GL_TRIANGLES,0,maze.count);
            glBindVertexArray(goal.vao); glDrawArrays(GL_TRIANGLES,0,goal.count);
            glBindVertexArray(0);

            float dx=camera.x-GoalMesh.GX, dz=camera.z-GoalMesh.GZ;
            if (!won && dx*dx+dz*dz<4.0f) {
                won=true;
                glfwSetWindowTitle(window,"VYHRÁL JSI! - Labyrint");
            }
            glfwSwapBuffers(window);
        }
    }

    private float[] perspective() {
        float fov=70f,near=0.05f,far=80f,f=(float)(1.0/Math.tan(Math.toRadians(fov/2))),asp=(float)W/H;
        return new float[]{f/asp,0,0,0,0,f,0,0,0,0,-(far+near)/(far-near),-1,0,0,-2*far*near/(far-near),0};
    }

    private static float[] mul(float[] a,float[] b) {
        float[] r=new float[16];
        for(int c=0;c<4;c++)for(int row=0;row<4;row++)for(int k=0;k<4;k++)r[c*4+row]+=a[k*4+row]*b[c*4+k];
        return r;
    }
}
