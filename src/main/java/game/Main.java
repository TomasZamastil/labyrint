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
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private long window;
    private int W=800, H=600;
    private Camera camera = new Camera();
    private Mesh maze = new Mesh();
    private GoalMesh goal = new GoalMesh();
    private int prog, uMVP, uLightPos, uAmbient, uFogColor, uFogDensity;
    private boolean won = false;
    private double prevMouseX = -1, prevMouseY = -1;
    private boolean mouseActive = true;
    private boolean showInfo = false;

    
    private enum State { MENU, PLAYING }
    private State state = State.MENU;
    private int selectedLevel = 0; 

    private static final String VERT =
        "#version 330 core\n" +
        "layout(location=0) in vec3 aPos;\n" +
        "layout(location=1) in vec3 aNorm;\n" +
        "layout(location=2) in vec3 aCol;\n" +
        "uniform mat4 uMVP;\n" +
        "out vec3 vNorm,vCol,vPos;\n" +
        "void main(){\n" +
        "  gl_Position=uMVP*vec4(aPos,1.0);\n" +
        "  vNorm=aNorm; vCol=aCol; vPos=aPos;\n" +
        "}\n";

    private static final String FRAG =
        "#version 330 core\n" +
        "in vec3 vNorm,vCol,vPos;\n" +
        "uniform vec3 uLightPos; uniform float uAmbient;\n" +
        "uniform vec3 uFogColor; uniform float uFogDensity;\n" +
        "out vec4 fragColor;\n" +
        "void main(){\n" +
        "  vec3 n=normalize(vNorm);\n" +
        "  vec3 l=normalize(uLightPos-vPos);\n" +
        "  float dist=length(uLightPos-vPos);\n" +
        "  float diff=max(dot(n,l),0.0);\n" +
        "  float att=1.0/(1.0+0.03*dist+0.003*dist*dist);\n" +
        "  float lit=uAmbient+(1.0-uAmbient)*diff*att;\n" +
        "  vec3 col=vCol*clamp(lit,0.0,1.0);\n" +
        "  float fog=exp(-uFogDensity*dist*dist);\n" +
        "  fragColor=vec4(mix(uFogColor,col,clamp(fog,0.0,1.0)),1.0);\n" +
        "}\n";

    private static final String VERT2D =
        "#version 330 core\n" +
        "layout(location=0) in vec4 aPos;\n" +
        "out vec2 vUV;\n" +
        "void main(){ gl_Position=vec4(aPos.xy,0,1); vUV=aPos.zw; }\n";

    private static final String FRAG2D =
        "#version 330 core\n" +
        "in vec2 vUV;\n" +
        "uniform sampler2D uTex;\n" +
        "out vec4 fragColor;\n" +
        "void main(){ fragColor=texture(uTex,vUV); }\n";

    private int prog2d, uTex2d;
    private int quadVao, quadVbo;
    private int winTex, hintTex, btnTex, infoPanelTex;
    private int menuTex, lvl1Tex, lvl2Tex, lvl3Tex;

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

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode mode = glfwGetVideoMode(monitor);
        W = mode.width(); H = mode.height();
        glfwWindowHint(GLFW_RED_BITS,     mode.redBits());
        glfwWindowHint(GLFW_GREEN_BITS,   mode.greenBits());
        glfwWindowHint(GLFW_BLUE_BITS,    mode.blueBits());
        glfwWindowHint(GLFW_REFRESH_RATE, mode.refreshRate());
        window = glfwCreateWindow(W, H, "Labyrint", monitor, NULL);
        if (window==NULL) throw new RuntimeException("window creation failed");

        glfwSetFramebufferSizeCallback(window, (win,w,h) -> { W=w; H=h; glViewport(0,0,w,h); });

        glfwSetKeyCallback(window, (win,key,sc,action,mods) -> {
            if (key==GLFW_KEY_ESCAPE && action==GLFW_PRESS) {
                if (state == State.PLAYING) {
                    
                    state = State.MENU;
                    won = false;
                    glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    mouseActive = false;
                } else {
                    glfwSetWindowShouldClose(win, true);
                }
            }
            if (state == State.PLAYING && key==GLFW_KEY_I && action==GLFW_PRESS) {
                mouseActive = !mouseActive;
                if (mouseActive) {
                    glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    if (glfwRawMouseMotionSupported())
                        glfwSetInputMode(win, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
                    prevMouseX = -1; prevMouseY = -1;
                } else {
                    glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    showInfo = false;
                }
            }
        });

        glfwSetMouseButtonCallback(window, (win,btn,action,mods) -> {
            if (btn==GLFW_MOUSE_BUTTON_LEFT && action==GLFW_PRESS) {
                try (MemoryStack s = stackPush()) {
                    DoubleBuffer mx = s.mallocDouble(1), my = s.mallocDouble(1);
                    glfwGetCursorPos(win, mx, my);
                    double cx=mx.get(0), cy=my.get(0);

                    if (state == State.MENU) {
                        
                        int btnW=320, btnH=80, gap=40;
                        int totalH = 3*btnH + 2*gap;
                        int startY = (H - totalH) / 2 + 80;
                        for (int lvl=1; lvl<=3; lvl++) {
                            int bx = (W-btnW)/2;
                            int by = startY + (lvl-1)*(btnH+gap);
                            if (cx>=bx && cx<=bx+btnW && cy>=by && cy<=by+btnH) {
                                startLevel(lvl);
                            }
                        }
                    } else if (!mouseActive) {
                        
                        int btnSz=48, margin=12;
                        float bx = W - btnSz - margin + btnSz/2f;
                        float by = H - btnSz - margin + btnSz/2f;
                        double dx=cx-bx, dy=cy-by;
                        if (dx*dx+dy*dy < (btnSz/2.0)*(btnSz/2.0))
                            showInfo = !showInfo;
                        else
                            showInfo = false;
                    }
                }
            }
        });

        try (MemoryStack s=stackPush()) {
            IntBuffer pw=s.mallocInt(1), ph=s.mallocInt(1);
            glfwGetFramebufferSize(window, pw, ph);
            W=pw.get(); H=ph.get();
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_MULTISAMPLE);
        glClearColor(0.40f, 0.55f, 0.80f, 1f);
        glViewport(0,0,W,H);

        int v=compileShader(GL_VERTEX_SHADER,   VERT);
        int f=compileShader(GL_FRAGMENT_SHADER, FRAG);
        prog=glCreateProgram();
        glAttachShader(prog,v); glAttachShader(prog,f);
        glLinkProgram(prog);
        if (glGetProgrami(prog,GL_LINK_STATUS)==0)
            throw new RuntimeException("link: "+glGetProgramInfoLog(prog));
        glDeleteShader(v); glDeleteShader(f);

        uMVP        = glGetUniformLocation(prog,"uMVP");
        uLightPos   = glGetUniformLocation(prog,"uLightPos");
        uAmbient    = glGetUniformLocation(prog,"uAmbient");
        uFogColor   = glGetUniformLocation(prog,"uFogColor");
        uFogDensity = glGetUniformLocation(prog,"uFogDensity");

        int v2=compileShader(GL_VERTEX_SHADER,   VERT2D);
        int f2=compileShader(GL_FRAGMENT_SHADER, FRAG2D);
        prog2d=glCreateProgram();
        glAttachShader(prog2d,v2); glAttachShader(prog2d,f2);
        glLinkProgram(prog2d);
        glDeleteShader(v2); glDeleteShader(f2);
        uTex2d=glGetUniformLocation(prog2d,"uTex");

        quadVao=glGenVertexArrays(); quadVbo=glGenBuffers();
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER,quadVbo);
        glBufferData(GL_ARRAY_BUFFER, 6*4*4, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0,4,GL_FLOAT,false,4*4,0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);

        
        winTex       = buildWinTexture();
        hintTex      = buildSimpleTex("Pro skryt\u00ed my\u0161i stiskni I", 520, 44, 20);
        btnTex       = buildCircleTex(64, 64);
        infoPanelTex = buildInfoPanelTex(480, 56);
        menuTex      = buildMenuTexture();
        lvl1Tex      = buildLevelButton("Lehk\u00e1", "6\u00d76 bludit\u011b", new java.awt.Color(46,139,87));
        lvl2Tex      = buildLevelButton("St\u0159edn\u00ed", "9\u00d79 bludit\u011b", new java.awt.Color(205,133,0));
        lvl3Tex      = buildLevelButton("T\u011b\u017ek\u00e1", "12\u00d712 bludit\u011b", new java.awt.Color(178,34,34));
    }

    private void startLevel(int level) {
        selectedLevel = level;
        MazeData.setLevel(level);
        maze.build();
        goal.build();
        camera.reset();
        won = false;
        showInfo = false;
        state = State.PLAYING;
        mouseActive = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported())
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        prevMouseX = -1; prevMouseY = -1;
    }

    private int compileShader(int type, String src) {
        int id=glCreateShader(type);
        glShaderSource(id,src);
        glCompileShader(id);
        if (glGetShaderi(id,GL_COMPILE_STATUS)==0)
            throw new RuntimeException("compile: "+glGetShaderInfoLog(id));
        return id;
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            if (state == State.MENU) {
                drawMenu();
            } else {
                processKeys();
                drawGame();
            }

            glfwSwapBuffers(window);
        }
    }

    private void drawMenu() {
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);

        
        drawQuad(menuTex, -1,-1, 2, 2);

        
        int btnW=320, btnH=80, gap=40;
        int totalH = 3*btnH + 2*gap;
        int startY = (H - totalH) / 2 + 80;

        int[] texs = {lvl1Tex, lvl2Tex, lvl3Tex};
        for (int i=0; i<3; i++) {
            int bx = (W-btnW)/2;
            int by = startY + i*(btnH+gap);
            drawQuad(texs[i],
                ndcX(bx, btnW), ndcY(by, btnH),
                ndcW(btnW),     ndcH(btnH));
        }
    }

    private void drawGame() {
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        glUseProgram(prog);

        float[] mvp=mul(perspective(), camera.viewMatrix());
        glUniformMatrix4fv(uMVP, false, mvp);
        glUniform3f(uLightPos, camera.x, camera.y, camera.z);
        glUniform1f(uAmbient, 0.45f);
        glUniform3f(uFogColor, 0.18f, 0.14f, 0.10f);
        glUniform1f(uFogDensity, 0.004f);

        glBindVertexArray(maze.vao);
        glDrawArrays(GL_TRIANGLES, 0, maze.count);
        glBindVertexArray(goal.vao);
        glDrawArrays(GL_TRIANGLES, 0, goal.count);
        glBindVertexArray(0);

        float gdx=camera.x-goal.gx(), gdz=camera.z-goal.gz();
        if (!won && gdx*gdx+gdz*gdz < 1.0f) won=true;

        if (won) drawQuad(winTex, -1,-1, 2, 2);

        if (mouseActive) {
            int tw=520, th=44, margin=10;
            drawQuad(hintTex,
                ndcX(margin, tw), ndcY(H-margin-th, th),
                ndcW(tw),         ndcH(th));
        }

        int btnSz=48, margin=12;
        drawQuad(btnTex,
            ndcX(W-btnSz-margin, btnSz), ndcY(H-btnSz-margin, btnSz),
            ndcW(btnSz),                  ndcH(btnSz));

        if (showInfo) {
            int pw=480, ph=56;
            drawQuad(infoPanelTex,
                ndcX(W-pw-btnSz-margin*2, pw), ndcY(H-ph-(btnSz-ph)/2-margin, ph),
                ndcW(pw),                       ndcH(ph));
        }
    }

    private float ndcX(float px, float w) { return px*2f/W - 1f; }
    private float ndcY(float py, float h) { return 1f - (py+h)*2f/H; }
    private float ndcW(float w)           { return w*2f/W; }
    private float ndcH(float h)           { return h*2f/H; }

    private void drawQuad(int tex, float x, float y, float w, float h) {
        float[] q = {
            x,   y,   0,1,   x+w, y,   1,1,   x+w, y+h, 1,0,
            x,   y,   0,1,   x+w, y+h, 1,0,   x,   y+h, 0,0
        };
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(prog2d);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tex);
        glUniform1i(uTex2d, 0);
        glBindVertexArray(quadVao);
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, q);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void processKeys() {
        if (mouseActive) {
            try (MemoryStack s = stackPush()) {
                DoubleBuffer mx=s.mallocDouble(1), my=s.mallocDouble(1);
                glfwGetCursorPos(window, mx, my);
                double nx=mx.get(0), ny=my.get(0);
                if (prevMouseX >= 0) {
                    camera.addYaw  ((float)(nx-prevMouseX)*0.0025f);
                    camera.addPitch((float)(ny-prevMouseY)*0.0025f);
                }
                prevMouseX=nx; prevMouseY=ny;
            }
        }
        boolean sprint = glfwGetKey(window,GLFW_KEY_LEFT_SHIFT)==GLFW_PRESS
                      || glfwGetKey(window,GLFW_KEY_RIGHT_SHIFT)==GLFW_PRESS;
        float spd = sprint ? 0.22f : 0.10f;
        if (glfwGetKey(window,GLFW_KEY_W)==GLFW_PRESS) camera.moveForward( spd);
        if (glfwGetKey(window,GLFW_KEY_S)==GLFW_PRESS) camera.moveForward(-spd);
        if (glfwGetKey(window,GLFW_KEY_D)==GLFW_PRESS) camera.moveRight(   spd);
        if (glfwGetKey(window,GLFW_KEY_A)==GLFW_PRESS) camera.moveRight(  -spd);
    }

    private int buildMenuTexture() {
        int tw=1920, th=1080;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tw,th,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setColor(new java.awt.Color(10, 15, 30, 210));
        g.fillRect(0, 0, tw, th);
        
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 220));
        g.setColor(new java.awt.Color(255, 255, 255));
        String title = "LABYRINT";
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (tw - fm.stringWidth(title))/2, 300);
        
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 72));
        g.setColor(new java.awt.Color(180, 200, 255));
        String sub = "Vyber obtížnost";
        fm = g.getFontMetrics();
        g.drawString(sub, (tw - fm.stringWidth(sub))/2, 430);
        
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 42));
        g.setColor(new java.awt.Color(120, 130, 150));
        String esc = "ESC pro ukončení";
        fm = g.getFontMetrics();
        g.drawString(esc, (tw - fm.stringWidth(esc))/2, 1040);
        g.dispose();
        return uploadTex(img, tw, th);
    }

    private int buildLevelButton(String label, String sub, java.awt.Color color) {
        int tw=1024, th=256;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tw,th,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g.setColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        g.fillRoundRect(0, 0, tw, th, 20, 20);
        
        g.setColor(new java.awt.Color(255,255,255,60));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawRoundRect(1, 1, tw-2, th-2, 20, 20);
        
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 96));
        g.setColor(java.awt.Color.WHITE);
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(label, (tw - fm.stringWidth(label))/2, 120);
        
        g.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 52));
        g.setColor(new java.awt.Color(220, 220, 220));
        fm = g.getFontMetrics();
        g.drawString(sub, (tw - fm.stringWidth(sub))/2, 200);
        g.dispose();
        return uploadTex(img, tw, th);
    }

    private int buildWinTexture() {
        int tw=2048, th=1024;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tw,th,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,         java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(new java.awt.Color(0,0,0,180));
        g.fillRoundRect(20,20,tw-40,th-40,80,80);
        g.setFont(new java.awt.Font("Arial",java.awt.Font.BOLD,220));
        g.setColor(new java.awt.Color(255,220,50));
        String l1="VYHRÁL JSI!";
        java.awt.FontMetrics fm=g.getFontMetrics();
        g.drawString(l1,(tw-fm.stringWidth(l1))/2,480);
        g.setFont(new java.awt.Font("Arial",java.awt.Font.PLAIN,80));
        g.setColor(new java.awt.Color(200,200,200));
        String l2="Stiskni ESC pro návrat do menu";
        fm=g.getFontMetrics();
        g.drawString(l2,(tw-fm.stringWidth(l2))/2,660);
        g.dispose();
        return uploadTex(img,tw,th);
    }

    private int buildSimpleTex(String text, int tw, int th, int fontSize) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tw,th,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0,0,0,140));
        g.fillRoundRect(0,0,tw,th,10,10);
        g.setFont(new java.awt.Font("Arial",java.awt.Font.PLAIN,fontSize));
        g.setColor(new java.awt.Color(255,255,255,230));
        java.awt.FontMetrics fm=g.getFontMetrics();
        g.drawString(text, 10, th/2+fm.getAscent()/2-2);
        g.dispose();
        return uploadTex(img,tw,th);
    }

    private int buildCircleTex(int tw, int th) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tw,th,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0,0,0,0));
        g.fillRect(0,0,tw,th);
        g.setColor(new java.awt.Color(50,100,200,210));
        g.fillOval(2,2,tw-4,th-4);
        g.setColor(new java.awt.Color(180,210,255,255));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.drawOval(2,2,tw-4,th-4);
        g.setFont(new java.awt.Font("Arial",java.awt.Font.BOLD,tw*5/8));
        g.setColor(java.awt.Color.WHITE);
        java.awt.FontMetrics fm=g.getFontMetrics();
        g.drawString("i",(tw-fm.stringWidth("i"))/2, th/2+fm.getAscent()/2-2);
        g.dispose();
        return uploadTex(img,tw,th);
    }

    private int buildInfoPanelTex(int tw, int th) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(tw,th,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0,0,0,190));
        g.fillRoundRect(0,0,tw,th,14,14);
        g.setFont(new java.awt.Font("Arial",java.awt.Font.PLAIN,18));
        g.setColor(java.awt.Color.WHITE);
        java.awt.FontMetrics fm=g.getFontMetrics();
        String s="Pro PGRF2 vytvo\u0159il Tom\u00e1\u0161 Zamastil \u00A9 2026";
        g.drawString(s,(tw-fm.stringWidth(s))/2, th/2+fm.getAscent()/2-2);
        g.dispose();
        return uploadTex(img,tw,th);
    }

    private int uploadTex(java.awt.image.BufferedImage img, int tw, int th) {
        int[] pixels=img.getRGB(0,0,tw,th,null,0,tw);
        java.nio.ByteBuffer buf=org.lwjgl.BufferUtils.createByteBuffer(tw*th*4);
        for(int p:pixels){buf.put((byte)((p>>16)&0xFF));buf.put((byte)((p>>8)&0xFF));buf.put((byte)(p&0xFF));buf.put((byte)((p>>24)&0xFF));}
        buf.flip();
        int tex=glGenTextures();
        glBindTexture(GL_TEXTURE_2D,tex);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,tw,th,0,GL_RGBA,GL_UNSIGNED_BYTE,buf);
        glBindTexture(GL_TEXTURE_2D,0);
        return tex;
    }

    private float[] perspective() {
        float fov=70f,near=0.05f,far=80f;
        float f=(float)(1.0/Math.tan(Math.toRadians(fov/2))),asp=(float)W/H;
        return new float[]{f/asp,0,0,0, 0,f,0,0, 0,0,-(far+near)/(far-near),-1, 0,0,-2*far*near/(far-near),0};
    }

    private static float[] mul(float[] a, float[] b) {
        float[] r=new float[16];
        for(int c=0;c<4;c++) for(int row=0;row<4;row++) for(int k=0;k<4;k++) r[c*4+row]+=a[k*4+row]*b[c*4+k];
        return r;
    }
}
