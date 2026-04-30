package game;

import maze.MazeData;

public class Camera {

    public float x = 1.5f, y = 1.6f, z = 1.5f;
    public float yaw = 0f, pitch = 0f;

    public void reset() {
        x = 1.5f; y = 1.6f; z = 1.5f;
        yaw = (float)(Math.PI / 2); pitch = 0f;
    }

    public void moveForward(float d) {
        move((float)Math.sin(yaw)*d, (float)Math.cos(yaw)*d);
    }

    public void moveRight(float d) {
        move((float)Math.sin(yaw + Math.PI/2)*d, (float)Math.cos(yaw + Math.PI/2)*d);
    }

    private void move(float dx, float dz) {
        float r = 0.3f;
        if      (!MazeData.collides(x+dx, z+dz, r)) { x+=dx; z+=dz; }
        else if (!MazeData.collides(x+dx, z,    r)) { x+=dx; }
        else if (!MazeData.collides(x,    z+dz, r)) { z+=dz; }
    }

    public void addYaw(float d) {
        yaw += d;
        while (yaw >  Math.PI) yaw -= (float)(2 * Math.PI);
        while (yaw < -Math.PI) yaw += (float)(2 * Math.PI);
    }

    public void addPitch(float d) {
        pitch = Math.max(-1.4f, Math.min(1.4f, pitch + d));
    }

    public float[] viewMatrix() {
        float cy=(float)Math.cos(yaw), sy=(float)Math.sin(yaw);
        float cp=(float)Math.cos(pitch), sp=(float)Math.sin(pitch);
        float rx=cy, ry=0f, rz=-sy;
        float ux=sy*sp, uy=cp, uz=cy*sp;
        float fx=sy*cp, fy=-sp, fz=cy*cp;
        return new float[]{
            rx, ux, -fx, 0f,
            ry, uy, -fy, 0f,
            rz, uz, -fz, 0f,
            -(rx*x+ry*y+rz*z),
            -(ux*x+uy*y+uz*z),
             (fx*x+fy*y+fz*z), 1f
        };
    }
}
