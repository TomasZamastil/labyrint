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
        List<Float> v=new ArrayList<>();
        float h=MazeData.H, s=MazeData.S;
        float[] w=MazeData.WALLS;

        for (int i=0;i<w.length;i+=4) {
            float x0=w[i],z0=w[i+1],x1=w[i+2],z1=w[i+3];
            float r=0.72f,g=0.57f,b=0.43f;
            quad(v,x0,0,z0,x1,0,z1,x1,h,z1,x0,h,z0, r,g,b);
            quad(v,x1,0,z1,x0,0,z0,x0,h,z0,x1,h,z1, r*0.6f,g*0.6f,b*0.6f);
        }
        quad(v,0,0,0,s,0,0,s,0,s,0,0,s, 0.40f,0.34f,0.28f);

        float[] arr=new float[v.size()];
        for(int i=0;i<arr.length;i++) arr[i]=v.get(i);
        count=arr.length/6;
        FloatBuffer buf=BufferUtils.createFloatBuffer(arr.length);
        buf.put(arr).flip();
        vao=glGenVertexArrays(); vbo=glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER,vbo);
        glBufferData(GL_ARRAY_BUFFER,buf,GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,6*4,0);   glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,6*4,3*4); glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    private static void quad(List<Float> v,
            float x0,float y0,float z0,float x1,float y1,float z1,
            float x2,float y2,float z2,float x3,float y3,float z3,float r,float g,float b) {
        for (float[] p:new float[][]{{x0,y0,z0},{x1,y1,z1},{x2,y2,z2},{x0,y0,z0},{x2,y2,z2},{x3,y3,z3}})
            {v.add(p[0]);v.add(p[1]);v.add(p[2]);v.add(r);v.add(g);v.add(b);}
    }
}
