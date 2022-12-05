package me.minetsh.imaging.core;

/**
 * Created by felix on 2017/11/16 下午5:49.
 */

public interface IMGViewPortrait {

    int getWidth();

    int getHeight();

    float getScaleX();

    void setScaleX(float scaleX);

    float getScaleY();

    void setScaleY(float scaleY);

    float getRotation();

    void setRotation(float rotate);

    float getPivotX();

    float getPivotY();

    float getX();

    void setX(float x);

    float getY();

    void setY(float y);

    float getScale();

    void setScale(float scale);

    void addScale(float scale);
}
