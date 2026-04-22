package maze;

public class MazeData {

    public static final float[] WALLS = {
        0,0,18,0,   0,0,0,18,   0,18,18,18,   18,0,18,18,
        3,0,3,6,    6,3,12,3,   9,0,9,6,
        0,6,6,6,    6,6,6,12,   3,9,9,9,
        9,6,12,6,   12,3,12,12, 6,12,15,12,
        0,12,3,12,  15,9,15,15, 9,12,9,18,
        3,15,9,15,  12,15,18,15,
    };

    public static final float H=3.0f, S=18.0f;

    public static boolean collides(float px,float pz,float radius) {
        for (int i=0;i<WALLS.length;i+=4) {
            float x0=WALLS[i],z0=WALLS[i+1],x1=WALLS[i+2],z1=WALLS[i+3];
            if (x0==x1) {
                float lo=Math.min(z0,z1),hi=Math.max(z0,z1);
                if (pz>=lo-radius&&pz<=hi+radius&&Math.abs(px-x0)<radius) return true;
            } else {
                float lo=Math.min(x0,x1),hi=Math.max(x0,x1);
                if (px>=lo-radius&&px<=hi+radius&&Math.abs(pz-z0)<radius) return true;
            }
        }
        return false;
    }
}
