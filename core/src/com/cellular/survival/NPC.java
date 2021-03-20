package com.cellular.survival;

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.*;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Random;

public class NPC implements Steerable<Vector2> {
    private Vector2 linearVelocity, position;
    private float orientation, angularVelocity, maxLinearSpeed,
            maxLinearAcceleration, maxAngularSpeed, maxAngularAcceleration;
    private float goalOrientation;
    private ShapeRenderer shapeRenderer;
    private OpenSimplexNoise noise;
    Random random;

    private Color color;
    private int state = 0;
    private float size;
    private boolean seePlayer;
    private Player player;
    ArrayList<NPC> NPCs;

    private static final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<Vector2>(new Vector2());
    SteeringBehavior<Vector2> steeringBehavior;
    Seek<Vector2> seek;
    Flee<Vector2> flee;
    float separationRadius;




    public NPC(Player player, float size, Vector2 chunk, ShapeRenderer shapeRenderer, OpenSimplexNoise noise, ArrayList<NPC> NPCs){
        this.position = new Vector2(chunk.x*Chunk.WIDTH, chunk.y*Chunk.HEIGHT);
        this.linearVelocity = new Vector2();
        this.maxLinearSpeed = 1/size+size/2;
        this.maxLinearAcceleration = 2;
        this.orientation = (float)(Math.PI);
        this.angularVelocity = 0;
        this.maxAngularSpeed = 1f;
        this.maxAngularAcceleration = .1f;
        this.state = 0;
        this.shapeRenderer = shapeRenderer;
        this.noise = noise;
        this.size = size>5?5:size;
        this.random = new Random();

        this.color = Color.valueOf("#74745D");
        this.seePlayer = false;
        this.player = player;
        this.NPCs = NPCs;
        this.seek = new Seek<>(this, player);
        this.flee = new Flee<>(this, player);

        while(!checkCollision(1).isEmpty()){
            position.x+=10;
            position.y+=10;
        }

        separationRadius = 12;
    }

    private ArrayList<Vector2> checkCollision(float mult){
        int numVec = 8;
        double ang = (2*Math.PI)/numVec;

        ArrayList<Vector2> collisions = new ArrayList<>();

        Vector2[] vecCheck = new Vector2[numVec];
        for(int i = 0; i<numVec; i++){
            vecCheck[i] = new Vector2((position.x+7*size*(float)Math.cos(ang*i)*mult),
                    (position.y+7*size*(float)Math.sin(ang*i))*mult);
        }

        for(int i = 0; i<numVec; i++){
            Vector2 vec = vecCheck[i];
            double value = noise.eval(((int)vec.x/ Chunk.FEATURE_SIZE),
                    ((int)vec.y/ Chunk.FEATURE_SIZE));
            if(value>.2){
                collisions.add(vec);
            }
        }
        return collisions;
    }


    public void render() {
        shapeRenderer.setColor(new Color(1, 1, 0, 0.1f));
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
        return new Vector2(position.x+7*(float)Math.cos(ang), position.y+7*(float)Math.sin(ang));
    }

    public void update(){
        seePlayer = calcVision();
        if (steeringBehavior != null) {
            steeringBehavior.calculateSteering(steeringOutput);
            applySteering(steeringOutput, .1f);
        }
        tick();
        act();
    }

    public void act(){
        GdxAI.getTimepiece().update(1/60f);
        switch (state) {
            case 0: // wandering
                myWander();
                steeringBehavior = null;
                if(seePlayer && Math.abs(player.getPosition().x-position.x)<100
                             && Math.abs(player.getPosition().y-position.y)<100){
                    //maxLinearSpeed = 1/size+size/2;
                    state = 1;
                }
                break;
            case 1: // seeking/fleeing player
                steeringBehavior = player.getSize()<size?seek:flee;
                if(player.getSize()>size && size > 2){
                    maxLinearSpeed = 5;
                }
                if(player.getPosition().x-position.x>100 && player.getPosition().y-position.y>100) {
                    steeringBehavior = null;
                    state = 0;
                }
                break;
        }
    }

    private void applySteering (SteeringAcceleration<Vector2> steering, float time) {
        linearVelocity.mulAdd(steering.linear, time).limit(this.getMaxLinearSpeed());
        if(linearVelocity.len()-.1>0) {
            goalOrientation = calculateOrientationFromLinearVelocity();
            if (goalOrientation != orientation) {
                float diff = goalOrientation - orientation;
                if(diff>Math.PI)
                    diff-=2*Math.PI;
                if(diff<-Math.PI)
                    diff+=2*Math.PI;

                angularVelocity = (diff) * time;
                if(angularVelocity>maxAngularSpeed)
                    angularVelocity = maxAngularSpeed;
                if(angularVelocity<-maxAngularSpeed)
                    angularVelocity = -maxAngularSpeed;
            }
        }
    }

    public float calculateOrientationFromLinearVelocity(){
        return (float)Math.atan2(linearVelocity.y, linearVelocity.x);
    }

    public void tick(){
        if(linearVelocity.len()>maxLinearSpeed){
            linearVelocity = linearVelocity.limit(maxLinearSpeed);
        }
        position.x += linearVelocity.x;
        position.y += linearVelocity.y;

        ArrayList<Vector2> collisions = checkCollision(1f);
        if(!collisions.isEmpty()) {
            orientation += (float)Math.PI;
            linearVelocity.scl(-6f);
            if(linearVelocity.len()<maxLinearSpeed/1.5){
                linearVelocity.scl(2);
            }
            position.x += linearVelocity.x;
            position.y += linearVelocity.y;
        }

        for(NPC npc: NPCs){
            if(npc!=this){
                Vector2 separationVelocity = new Vector2();
                int count = 0;
                if(Math.abs(npc.position.x-position.x)<size*separationRadius &&
                   Math.abs(npc.position.y-position.y)<size*separationRadius){
                        separationVelocity.x += Math.abs(npc.position.x-position.x);
                        separationVelocity.y += Math.abs(npc.position.y-position.y);
                        count++;
                }
                if(count!=0)
                    separationVelocity.scl(1f/count);
                separationVelocity.scl(-1).nor();
                linearVelocity.add(separationVelocity);
            }
        }

        if(Math.abs(angularVelocity)<.01){
            angularVelocity = 0;
        }
        orientation += angularVelocity;
    }

    private void myWander(){
        linearVelocity.x += Math.cos(orientation)*maxLinearAcceleration;
        linearVelocity.y += Math.sin(orientation)*maxLinearAcceleration;

        double rand = random.nextDouble();

        if(rand<.05){
            if(angularVelocity<=0) {
                angularVelocity += .05;
            }
        }
        else if(rand < .1){
            if(angularVelocity>=0) {
                angularVelocity -= .05;
            }
        }
    }

    public boolean calcVision(){
        return calcVisionPos()||calcVisionNeg();
    }

    public boolean calcVisionPos(){
        Vector2 orientationNorm = new Vector2((float)Math.cos(getOrientation()), (float)Math.sin(getOrientation()));
        Vector2 perp = PerpendicularClockwise(orientationNorm).scl(10);
        Vector2 edge = perp.add(player.getPosition());

        return pointInVision(edge);
    }
    public boolean calcVisionNeg(){
        Vector2 orientationNorm = new Vector2((float)Math.cos(getOrientation()), (float)Math.sin(getOrientation()));
        Vector2 perp = PerpendicularCounterClockwise(orientationNorm).scl(10);
        Vector2 edge = perp.add(player.getPosition());

        return pointInVision(edge);
    }

    public boolean pointInVision(Vector2 point){
        Vector2 dist = new Vector2(position.x-(point.x), position.y-(point.y)).nor();
        Vector2 orientationNorm = new Vector2((float)Math.cos(orientation), (float)Math.sin(orientation));
        float dot = dist.dot(orientationNorm);
        return dot<=0;
    }

    public static Vector2 PerpendicularClockwise(Vector2 vector2)
    {
        return new Vector2(vector2.y, -(vector2.x));
    }

    public static Vector2 PerpendicularCounterClockwise(Vector2 vector2)
    {
        return new Vector2(-vector2.y, vector2.x);
    }

    public float getSize() {
        return size;
    }

    @Override
    public float vectorToAngle (Vector2 vector) {
        return (float)Math.atan2(vector.x, vector.y);
    }

    @Override
    public Vector2 getLinearVelocity(){
        return linearVelocity;
    }

    @Override
    public float getAngularVelocity(){
        return angularVelocity;
    }

    @Override
    public float getBoundingRadius(){
        return 10;
    }

    @Override
    public boolean isTagged(){
        return false;
    }

    @Override
    public void setTagged(boolean tagged){ }

    @Override
    public Vector2 getPosition(){
        return position;
    }

    @Override
    public float getOrientation(){
        return orientation;
    }

    @Override
    public Location<Vector2> newLocation(){
        return null;
    }

    @Override
    public void setOrientation(float orientation){
        this.orientation = orientation;
    }

    @Override
    public float getMaxLinearSpeed () {
        return maxLinearSpeed;
    }

    @Override
    public void setMaxLinearSpeed (float maxLinearSpeed) {
        this.maxLinearSpeed = maxLinearSpeed;
    }

    @Override
    public float getMaxLinearAcceleration () {
        return maxLinearAcceleration;
    }

    @Override
    public void setMaxLinearAcceleration (float maxLinearAcceleration) {
        this.maxLinearAcceleration = maxLinearAcceleration;
    }

    @Override
    public float getMaxAngularSpeed () {
        return maxAngularSpeed;
    }

    @Override
    public void setMaxAngularSpeed (float maxAngularSpeed) {
        this.maxAngularSpeed = maxAngularSpeed;
    }

    @Override
    public float getMaxAngularAcceleration () {
        return maxAngularAcceleration;
    }

    @Override
    public void setMaxAngularAcceleration (float maxAngularAcceleration) {
        this.maxLinearAcceleration = maxAngularAcceleration;
    }

    @Override
    public float getZeroLinearSpeedThreshold () {
        return 0.001f;
    }

    @Override
    public void setZeroLinearSpeedThreshold (float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector2 angleToVector (Vector2 outVector, float angle) {
        outVector.x = -(float)Math.sin(angle);
        outVector.y = (float)Math.cos(angle);
        return outVector;
    }
}
