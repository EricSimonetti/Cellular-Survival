package com.cellular.survival;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.ai.utils.Location;

import java.util.ArrayList;

public class Player implements Location<Vector2> {
    private Vector2 linearVelocity, position;
    private float orientation, angularVelocity, angularVelocityMax, maxLinearSpeed;
    private Color color;
    private float size;
    private OpenSimplexNoise noise;
    private ShapeRenderer shapeRenderer;
    private int numGrowths;
    private float maxSize = 10;

    public Player(OpenSimplexNoise noise, ShapeRenderer shapeRenderer, Chunk chunk){
        this.linearVelocity = new Vector2();
        this.orientation = 0;
        this.position = new Vector2(0, 0);
        this.angularVelocity = 0;
        this.maxLinearSpeed = 1/size;
        this.angularVelocityMax = .1f;
        this.size = 1;
        this.color = new Color(.85f,.85f, .3f, .1f);
        this.noise = noise;
        this.shapeRenderer = shapeRenderer;
        numGrowths = 0;

        while(!checkCollision(1).isEmpty()){
            position.x+=10;
            position.y+=10;
        }
    }

    private ArrayList<Vector2> checkCollision(float mult){
        int numVec = 16;
        double ang = (2*Math.PI)/numVec;

        ArrayList<Vector2> collisions = new ArrayList<>();

        Vector2[] vecCheck = new Vector2[numVec];
        for(int i = 0; i<numVec; i++){
            vecCheck[i] = new Vector2((position.x+5*size*(float)Math.cos(ang*i)*mult),
                                   (position.y+5*size*(float)Math.sin(ang*i))*mult);
        }

        for(int i = 0; i<numVec; i++){
            Vector2 vec = vecCheck[i];
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.line(vec.x, vec.y, position.x, position.y);
            shapeRenderer.end();


            double value = noise.eval(((int)vec.x/ Chunk.FEATURE_SIZE),
                                      ((int)vec.y/ Chunk.FEATURE_SIZE));
            if(value>.2){
                collisions.add(vec);
            }
        }
        return collisions;
    }

    public void render(){
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(color);
        shapeRenderer.circle(position.x, position.y, 5*size);

        shapeRenderer.setColor(Color.BLACK);

        Vector2 eyeVec = getEyeposition((orientation-.5f),position);
        shapeRenderer.circle(eyeVec.x, eyeVec.y, 2*size);
        eyeVec = getEyeposition((orientation+.5f),position);
        shapeRenderer.circle(eyeVec.x, eyeVec.y, 2*size);

        shapeRenderer.end();
    }
    private Vector2 getEyeposition(float ang, Vector2 position){
        return new Vector2(position.x+4*size*(float)Math.cos(ang), position.y+4*size*(float)Math.sin(ang));
    }

    public void right(){
        linearVelocity.x+=.3f;
    }

    public void left(){
        linearVelocity.x-=.3f;
    }

    public void up(){
        linearVelocity.y+=.3f;
    }

    public void down(){
        linearVelocity.y-=.3f;
    }

    public void act(){
        position.x += linearVelocity.x;
        position.y += linearVelocity.y;
        ArrayList<Vector2> collisions = checkCollision(1f);
        if(!collisions.isEmpty()){
            linearVelocity.scl(-1.25f);
            position.x += linearVelocity.x;
            position.y += linearVelocity.y;
            /* //tried to implement sensible collision ricochet angles, but for some reason they would never always workout
            float average = 0;
            for(Vector2 vec: collisions){
                average += Math.atan(vec.y/vec.x);
                System.out.println(Math.atan2(vec.y,vec.x) + " " + vec.angleRad());
            }
            average/=collisions.size();
            System.out.println("Average: "+average);
            System.out.println("OG: "+linearVelocity.angleRad());
            Vector2 oldLinear = linearVelocity.cpy();

            linearVelocity.setAngleRad(average + (float) Math.PI);

            position.x += linearVelocity.x;
            position.y += linearVelocity.y;

            ArrayList<Vector2> nextCollision = checkCollision(1f);
            if(nextCollision.size()>collisions.size()){ //this was a catch to see if the angle was wrong,
                position.x -= linearVelocity.x;         //it almost worked except for in corners
                position.y -= linearVelocity.y;
                linearVelocity =oldLinear;
                linearVelocity.scl(-1);
                position.x += linearVelocity.x;
                position.y += linearVelocity.y;
            }
             */
        }
        linearVelocity.x/=1.05;
        linearVelocity.y/=1.05;
        if(linearVelocity.len()!=0) {
            orientation = linearVelocity.angleRad();
        }
        //if(linearVelocity.len()-.1<0) setSpeed(0);
        if(linearVelocity.len()>maxLinearSpeed){
            linearVelocity = linearVelocity.limit(maxLinearSpeed);
        }

    }

    public boolean grow(){
        if(!(size>=maxSize)) {
            size += .1;
            numGrowths++;
            return numGrowths % 50 == 0;
        }
        return false;
    }

    public float getSize() {
        return size;
    }

    @Override
    public Vector2 angleToVector(Vector2 outVector, float angle){
        outVector.x = -(float)Math.sin(angle);
        outVector.y = (float)Math.cos(angle);
        return outVector;
    }

    @Override
    public float getOrientation(){
        return orientation;
    }
    @Override
    public Vector2 getPosition(){
        return position;
    }
    @Override
    public Location newLocation(){
        return this;
    }
    @Override
    public void setOrientation(float orientation){
        this.orientation = orientation;
    }
    @Override
    public float vectorToAngle(Vector2 vector){
        return (float)Math.atan(vector.y/vector.x);
    }
    public Vector2 getLinearVelocity(){
        return  linearVelocity;
    }
}
