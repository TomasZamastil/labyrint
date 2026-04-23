package maze;

public class MazeData {

    public static final float[] WALLS = {
        0,0,36,0,    0,3,21,3,    30,3,33,3,   3,6,6,6,    24,6,30,6,
        0,9,3,9,     6,9,15,9,    21,9,33,9,   3,12,6,12,  15,12,18,12,
        21,12,24,12, 27,12,36,12, 3,15,9,15,   15,15,21,15, 30,15,33,15,
        3,18,6,18,   9,18,15,18,  18,18,24,18, 33,18,36,18,
        15,21,18,21, 21,21,27,21, 0,24,3,24,   12,24,21,24, 27,24,33,24,
        15,27,21,27, 27,27,30,27, 33,27,36,27, 15,30,27,30, 30,30,33,30,
        3,33,21,33,  27,33,30,33, 0,36,36,36,  0,0,0,36,
        3,6,3,9,     3,12,3,15,   3,18,3,21,   3,24,3,30,
        6,9,6,12,    6,18,6,33,   9,6,9,9,     9,12,9,30,
        12,3,12,6,   12,9,12,15,  12,21,12,33, 15,6,15,9,  15,12,15,15,
        15,18,15,21, 18,3,18,12,  18,18,18,21, 21,3,21,9,
        21,21,21,27, 21,30,21,33, 24,0,24,6,   24,9,24,18,
        24,24,24,30, 24,33,24,36, 27,0,27,3,   27,12,27,21,
        27,24,27,27, 27,30,27,33, 30,3,30,6,   30,15,30,24,
        30,27,30,30, 33,6,33,9,   33,18,33,21, 33,30,33,36, 36,0,36,36,
    };

    public static final float H = 3.0f;   
    public static final float S = 36.0f;  

    public static boolean collides(float px, float pz, float radius) {
        for (int i = 0; i < WALLS.length; i += 4) {
            float x0=WALLS[i], z0=WALLS[i+1], x1=WALLS[i+2], z1=WALLS[i+3];
            if (x0 == x1) {
                float lo=Math.min(z0,z1), hi=Math.max(z0,z1);
                if (pz>=lo-radius && pz<=hi+radius && Math.abs(px-x0)<radius) return true;
            } else {
                float lo=Math.min(x0,x1), hi=Math.max(x0,x1);
                if (px>=lo-radius && px<=hi+radius && Math.abs(pz-z0)<radius) return true;
            }
        }
        return false;
    }
}
