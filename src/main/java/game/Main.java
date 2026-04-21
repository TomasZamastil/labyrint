package game;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private int W=800, H=600;
    private int prog, uMVP;
    private int vao, vbo;

    private float camX=2f, camY=1.6f, camZ=2f;
    private float yaw=0f, pitch=0f;
    private double lastX=-1, lastY=-1;

    private static final String VERT =
        "#version 330 core\n" +
        "layout(location=0) in vec3 aPos;\n" +
        "layout(location=1) in vec3 aCol;\n" +
        "uniform mat4 uMVP;\n" +
        "out vec3 vCol;\n" +
        "void main(){ gl_Position=uMVP*vec4(aPos,1.0); vCol=aCol; }\n";

    private static final String FRAG =
        "#version 330 core\n" +
        "in vec3 vCol;\n" +
        "out vec4 fragColor;\n" +
        "void main(){ fragColor=vec4(vCol,1.0); }\n";

    public static void main(String[] args) { new Main().run(); }

    public void run() {
        init(); loop();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("glfwInit failed");
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT,GLFW_TRUE);

        window=glfwCreateWindow(W,H,"Labyrint",NULL,NULL);
        if (window==NULL) throw new RuntimeException("window failed");

        
        glfwSetMouseButtonCallback(window,(win,btn,action,mods)->{
            if (btn==GLFW_MOUSE_BUTTON_LEFT&&action==GLFW_PRESS)
                glfwSetInputMode(win,GLFW_CURSOR,GLFW_CURSOR_DISABLED);
        });
        glfwSetCursorPosCallback(window,(win,xp,yp)->{
            if (lastX<0){lastX=xp;lastY=yp;return;}
            yaw+=(float)(xp-lastX)*0.003f;
            pitch=Math.max(-1.4f,Math.min(1.4f,pitch+(float)(yp-lastY)*0.003f));
            lastX=xp; lastY=yp;
        });
        glfwSetKeyCallback(window,(win,key,sc,action,mods)->{
            if (key==GLFW_KEY_ESCAPE&&action==GLFW_PRESS) glfwSetWindowShouldClose(win,true);
        });

        glfwMakeContextCurrent(window); glfwSwapInterval(1); glfwShowWindow(window);
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glClearColor(0f,0f,0f,1f);
        glViewport(0,0,W,H);

        int v=compile(GL_VERTEX_SHADER,VERT), f=compile(GL_FRAGMENT_SHADER,FRAG);
        prog=glCreateProgram(); glAttachShader(prog,v); glAttachShader(prog,f);
        glLinkProgram(prog); glDeleteShader(v); glDeleteShader(f);
        uMVP=glGetUniformLocation(prog,"uMVP");

        
        float[] verts={
            0,0,5, 0.8f,0.6f,0.4f,   3,0,5, 0.8f,0.6f,0.4f,   3,3,5, 0.8f,0.6f,0.4f,
            0,0,5, 0.8f,0.6f,0.4f,   3,3,5, 0.8f,0.6f,0.4f,   0,3,5, 0.8f,0.6f,0.4f,
        };
        vao=glGenVertexArrays(); vbo=glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER,vbo);
        glBufferData(GL_ARRAY_BUFFER,verts,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,6*4,0);   glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,6*4,3*4); glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    private int compile(int type,String src){
        int id=glCreateShader(type); glShaderSource(id,src); glCompileShader(id); return id;
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            
            float spd=0.08f;
            float fx=(float)Math.sin(yaw), fz=(float)Math.cos(yaw);
            if (glfwGetKey(window,GLFW_KEY_W)==GLFW_PRESS){ camX+=fx*spd; camZ+=fz*spd; }
            if (glfwGetKey(window,GLFW_KEY_S)==GLFW_PRESS){ camX-=fx*spd; camZ-=fz*spd; }
            if (glfwGetKey(window,GLFW_KEY_D)==GLFW_PRESS){ camX+=fz*spd; camZ-=fx*spd; }
            if (glfwGetKey(window,GLFW_KEY_A)==GLFW_PRESS){ camX-=fz*spd; camZ+=fx*spd; }

            glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
            glUseProgram(prog);
            glUniformMatrix4fv(uMVP,false,mul(perspective(),view()));
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES,0,6);
            glBindVertexArray(0);
            glfwSwapBuffers(window);
        }
    }

    private float[] view() {
        float cy=(float)Math.cos(yaw),sy=(float)Math.sin(yaw);
        float cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);
        float rx=cy,ry=0,rz=-sy, ux=sy*sp,uy=cp,uz=cy*sp, fx=sy*cp,fy=-sp,fz=cy*cp;
        return new float[]{rx,ux,-fx,0, ry,uy,-fy,0, rz,uz,-fz,0,
            -(rx*camX+ry*camY+rz*camZ),-(ux*camX+uy*camY+uz*camZ),(fx*camX+fy*camY+fz*camZ),1};
    }

    private float[] perspective() {
        
        float f=(float)(1.0/Math.tan(Math.toRadians(35f))), asp=(float)W/H;
        return new float[]{f/asp,0,0,0, 0,f,0,0, 0,0,-1.001f,-1, 0,0,-0.1f,0};
    }

    private static float[] mul(float[] a,float[] b) {
        float[] r=new float[16];
        for(int c=0;c<4;c++) for(int row=0;row<4;row++) for(int k=0;k<4;k++) r[c*4+row]+=a[k*4+row]*b[c*4+k];
        return r;
    }
}
