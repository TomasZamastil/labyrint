package maze;

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {

    public int vao, vbo, count;

    public void build() {
        List<Float> v = new ArrayList<>();

        float h = MazeData.H, s = MazeData.S;
        float[] w = MazeData.WALLS;

        
        for (int i = 0; i < w.length; i += 4) {
            float x0=w[i], z0=w[i+1], x1=w[i+2], z1=w[i+3];
            float dx=x1-x0, dz=z1-z0;
            float len=(float)Math.sqrt(dx*dx+dz*dz);
            if (len==0) continue;
            float nx=dz/len, nz=-dx/len;

            
            boolean vert = (x0==x1);
            float sh = vert ? 0.80f : 0.95f;
            float r=0.67f*sh, g=0.53f*sh, b=0.40f*sh;
            float rd=r*0.60f, gd=g*0.60f, bd=b*0.60f;

            quad(v, x0,0,z0, x1,0,z1, x1,h,z1, x0,h,z0,  nx,0,nz,  r,g,b);   
            quad(v, x1,0,z1, x0,0,z0, x0,h,z0, x1,h,z1, -nx,0,-nz, rd,gd,bd); 
        }

        
        quad(v, 0,0,0, s,0,0, s,0,s, 0,0,s,  0,1,0, 0.38f,0.32f,0.26f);

        
        quad(v, 0,h,s, s,h,s, s,h,0, 0,h,0,  0,-1,0, 0.10f,0.09f,0.08f);

        
        float[] arr = new float[v.size()];
        for (int i=0;i<arr.length;i++) arr[i]=v.get(i);
        count = arr.length / 9;

        FloatBuffer buf = BufferUtils.createFloatBuffer(arr.length);
        buf.put(arr).flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        int stride = 9*4;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3*4);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 6*4);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    private static void quad(List<Float> v,
            float x0,float y0,float z0, float x1,float y1,float z1,
            float x2,float y2,float z2, float x3,float y3,float z3,
            float nx,float ny,float nz,  float r,float g,float b) {
        vert(v, x0,y0,z0, nx,ny,nz, r,g,b);
        vert(v, x1,y1,z1, nx,ny,nz, r,g,b);
        vert(v, x2,y2,z2, nx,ny,nz, r,g,b);
        vert(v, x0,y0,z0, nx,ny,nz, r,g,b);
        vert(v, x2,y2,z2, nx,ny,nz, r,g,b);
        vert(v, x3,y3,z3, nx,ny,nz, r,g,b);
    }

    private static void vert(List<Float> v,
            float x,float y,float z,
            float nx,float ny,float nz,
            float r,float g,float b) {
        v.add(x); v.add(y); v.add(z);
        v.add(nx); v.add(ny); v.add(nz);
        v.add(r); v.add(g); v.add(b);
    }
}
