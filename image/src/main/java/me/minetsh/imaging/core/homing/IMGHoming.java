package me.minetsh.imaging.core.homing;

import androidx.annotation.NonNull;

/**
 * Created by felix on 2017/11/28 下午4:14.
 */

public class IMGHoming {

    public float x, y;

    public float scale;

    public float rotate;

    public IMGHoming(float x, float y, float scale, float rotate) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.rotate = rotate;
    }

    public static boolean isRotate(IMGHoming sHoming, IMGHoming eHoming) {
        return Float.compare(sHoming.rotate, eHoming.rotate) != 0;
    }

    public void set(float x, float y, float scale, float rotate) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.rotate = rotate;
    }

    public void concat(IMGHoming homing) {
        scale *= homing.scale;
        x += homing.x;
        y += homing.y;
    }

    public void rConcat(IMGHoming homing) {
        scale *= homing.scale;
        x -= homing.x;
        y -= homing.y;
    }

    @NonNull
    @Override
    public String toString() {
        return "IMGHoming{" +
                "x=" + x +
                ", y=" + y +
                ", scale=" + scale +
                ", rotate=" + rotate +
                '}';
    }
}
