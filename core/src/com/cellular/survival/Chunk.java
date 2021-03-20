package com.cellular.survival;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import java.util.ArrayList;

public class Chunk {
    static final int RENDER_DISTANCE = 8;
    static final int RENDER_LENGTH = 2*RENDER_DISTANCE + 1;
    static final int WIDTH = 3*768/RENDER_LENGTH, HEIGHT = 3*725/RENDER_LENGTH;
    static final double FEATURE_SIZE = 200;
    private int pix;
    private Vector2 loc;
    private Pixmap pixmap;
    OpenSimplexNoise noise;
    private double zoom;
    private Texture img;

    private ArrayList<PolygonShape> polygons;

    public Chunk(int x, int y, OpenSimplexNoise noise){
        pix = 1;
        loc = new Vector2(x, y);
        pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGB888);
        this.noise = noise;
        polygons = new ArrayList<>();
        img = getTexture(x, y);
    }

    public void dispose(){
        pixmap.dispose();
        img.dispose();
        for(PolygonShape poly: polygons)
            poly.dispose();
    }

    private Texture getTexture(int chunkX, int chunkY){
        for (int y = 0; y < HEIGHT; y+=pix) {
            for (int x = 0; x < WIDTH; x+=pix) {
                Color color;
                double scaledX =  x + WIDTH*chunkX, scaledY = y + HEIGHT*chunkY;
                double value = noise.eval((scaledX/ FEATURE_SIZE),
                                          (scaledY/ FEATURE_SIZE));

                if(value>.2){
                    color = new Color(Color.valueOf("#FFFF99"));
                }
                else if(value<0){
                    color = new Color(Color.valueOf("#5D5D89"));
                }
                else{
                    color = new Color(Color.valueOf("#A2A290"));
                }

                 //*/
                /*
                if(value>.2&&value<.2001){
                    color = new Color(Color.valueOf("#FFFF99"));
                }
                else{
                    color = new Color(Color.valueOf("#5D5D89"));
                }

                 //*/
                pixmap.setColor(color);
                pixmap.drawPixel(x, HEIGHT-1-y);
            }
        }
        return new Texture(pixmap);
    }

    public Texture getImg(){
        return img;
    }
    public Vector2 getLoc() {
        return loc;
    }
    public int X(){
        return (int)loc.x;
    }
    public int Y(){
        return (int)loc.y;
    }
    public Pixmap getPixmap() {
        return pixmap;
    }

    public OpenSimplexNoise getNoise() {
        return noise;
    }
}
