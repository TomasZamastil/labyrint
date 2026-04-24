package maze;

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class GoalMesh {

    public static final float GX=34.5f, GY=1.5f, GZ=34.5f;
    public int vao, vbo, count;

    public void build() {
        List<Float> v = new ArrayList<>();
        int stacks=16, slices=24;
        float r=0.65f;

        for (int i=0;i<stacks;i++) {
            double a0=Math.PI*i/stacks - Math.PI/2;
            double a1=Math.PI*(i+1)/stacks - Math.PI/2;
            for (int j=0;j<slices;j++) {
                double b0=2*Math.PI*j/slices;
                double b1=2*Math.PI*(j+1)/slices;
                float[] p0=sv(a0,b0,r); float[] p1=sv(a0,b1,r);
                float[] p2=sv(a1,b1,r); float[] p3=sv(a1,b0,r);
                tri(v,p0,p1,p2); tri(v,p0,p2,p3);
            }
        }

        float[] arr=new float[v.size()];
        for(int i=0;i<arr.length;i++) arr[i]=v.get(i);
        count=arr.length/9;

        FloatBuffer buf=BufferUtils.createFloatBuffer(arr.length);
        buf.put(arr).flip();

        vao=glGenVertexArrays(); vbo=glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER,vbo);
        glBufferData(GL_ARRAY_BUFFER,buf,GL_STATIC_DRAW);
        int stride=9*4;
        glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0);   glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,stride,3*4); glEnableVertexAttribArray(1);
        glVertexAttribPointer(2,3,GL_FLOAT,false,stride,6*4); glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    private static float[] sv(double lat,double lon,float r){
        float nx=(float)(Math.cos(lat)*Math.cos(lon));
        float ny=(float)Math.sin(lat);
        float nz=(float)(Math.cos(lat)*Math.sin(lon));
        return new float[]{GX+nx*r, GY+ny*r, GZ+nz*r, nx,ny,nz, 1.0f,0.08f,0.04f};
    }

    private static void tri(List<Float> v,float[] a,float[] b,float[] c){
        for(float[] p:new float[][]{a,b,c}) for(float f:p) v.add(f);
    }
}
