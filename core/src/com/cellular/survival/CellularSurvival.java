package com.cellular.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.Random;


public class CellularSurvival extends ApplicationAdapter {
	private static final float FRAME_RATE = 1/60f;
	private BitmapFont font;
	private int renderDistance;
	private OpenSimplexNoise noise;
	private Vector2 currentChunk;
	private Chunk[][] loadedChunks;
	private Random random;

	private Player player;
	private ArrayList<NPC> NPCs;
	boolean gameOver;

	private OrthographicCamera cam;

	private SpriteBatch batch;
	private ShapeRenderer shapeRenderer;

	
	@Override
	public void create () {
		font = new BitmapFont();
		random = new Random();
		noise = new OpenSimplexNoise(random.nextLong());
		renderDistance = Chunk.RENDER_DISTANCE;
		currentChunk = new Vector2(renderDistance,renderDistance);
		loadedChunks = new Chunk[2*renderDistance+1][2*renderDistance+1];
		for (int j = -renderDistance; j<=renderDistance; j++){
			for(int i = -renderDistance; i<=renderDistance; i++){
				loadedChunks[i+renderDistance][j+renderDistance] = new Chunk(i, j, noise);
			}
		}
		cam = new OrthographicCamera(renderDistance*Chunk.WIDTH*.5f, renderDistance*Chunk.HEIGHT*.5f);
		cam.zoom = .90f;
		cam.position.x = 0;
		cam.position.y = 0;

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		player = new Player(noise, shapeRenderer, loadedChunks[renderDistance][renderDistance]);
		NPCs = new ArrayList<>();
		for (int j = -renderDistance; j<=renderDistance; j++){
			for(int i = -renderDistance; i<=renderDistance; i++){
				if(Math.abs(j)>renderDistance/2||Math.abs(i)>renderDistance/2) {
					newNPCs(i, j);
				}
			}
		}

		gameOver = false;
	}

	@Override
	public void dispose () {
		batch.dispose();
		shapeRenderer.dispose();
		for (int j = -renderDistance; j<=renderDistance; j++){
			for(int i = -renderDistance; i<=renderDistance; i++){
				loadedChunks[i+renderDistance][j+renderDistance].dispose();
			}
		}
	}

	@Override
	public void render () {
		long beginTime = System.currentTimeMillis();
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.setProjectionMatrix(cam.combined);
		shapeRenderer.setProjectionMatrix(cam.combined);
		cam.update();

		batch.begin();
		for (int j = -renderDistance; j<=renderDistance; j++){
			for(int i = -renderDistance; i<=renderDistance; i++){
				batch.draw(loadedChunks[i+renderDistance][j+renderDistance].getImg(),
						loadedChunks[i+renderDistance][j+renderDistance].getLoc().x*Chunk.WIDTH,
						loadedChunks[i+renderDistance][j+renderDistance].getLoc().y*Chunk.HEIGHT);
				}
		}
		batch.end();

		for (NPC npc : NPCs) {
			Vector2 loc = npc.getPosition().cpy();
			Vector3 onCamLoc = cam.project(new Vector3(loc, 0));
			if (onCamLoc.x + 5 * npc.getSize() > 0 && onCamLoc.x - 5 * npc.getSize() < Gdx.graphics.getWidth() &&
				onCamLoc.y + 5 * npc.getSize() > 0 && onCamLoc.y - 5 * npc.getSize() < Gdx.graphics.getHeight()) {
				if(!gameOver) {
					npc.update();
				}
				npc.render();
			}
		}

		if(!gameOver) {
			player.act();
		}
		player.render();
		update();

		if(gameOver) {
			Vector3 center = cam.unproject(new Vector3(768/2f-180, 725/2f+30, 0));
			Vector3 centerG = cam.unproject(new Vector3(768/2f-270, 725/2f-70, 0));
			batch.begin();
			font.setColor(Color.FIREBRICK);
			font.getData().setScale(5);
			font.draw(batch, "Game Over", centerG.x, centerG.y);
			font.getData().setScale(2);
			font.draw(batch, "Press Enter to Reset", center.x, center.y);
			batch.end();
		}
		long timeDiff = System.currentTimeMillis() - beginTime;
		long sleepTime = (long) (FRAME_RATE - timeDiff);
		if (sleepTime > 0) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
			}
		}
	}

	private void update(){
		if(gameOver){
			if (Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
				restart();
			}
		}
		else {
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				player.right();
			}
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				player.left();
			}
			if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
				player.down();
			}
			if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
				player.up();
			}
			ArrayList<NPC> remove = new ArrayList<>();
			for(NPC npc : NPCs){
				if(Math.abs(player.getPosition().x-npc.getPosition().x) < player.getSize()*5+npc.getSize()*5 &&
				   Math.abs(player.getPosition().y-npc.getPosition().y) < player.getSize()*5+npc.getSize()*5){
					if(player.getSize()>npc.getSize()){
						grow();
						remove.add(npc);
					}
					else{
						gameOver = true;
					}
				}
			}
			NPCs.removeAll(remove);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
			devCam();
		}

		cam.position.x = player.getPosition().x;
		cam.position.y = player.getPosition().y;

		renderChunks();
	}

	private void renderChunks(){
		Vector2 playerChunk = getChunkFromPosition(player.getPosition());

		if(playerChunk.x!=currentChunk.x||playerChunk.y!=currentChunk.y){
			int deltaX = ((int)playerChunk.x -(int)currentChunk.x)*renderDistance,
					deltaY = ((int)playerChunk.y -(int)currentChunk.y)*renderDistance;
			currentChunk = playerChunk;

			int count = -renderDistance;
			for (int j = -renderDistance; j<=renderDistance && count<=renderDistance; j++)
				for(int i=-renderDistance; i<=renderDistance && count<=renderDistance; i++){
					Chunk chunk = loadedChunks[i+renderDistance][j+renderDistance];
					if (Math.abs(currentChunk.x - chunk.X()) > renderDistance || Math.abs(currentChunk.y - chunk.Y()) > renderDistance) {
						if (deltaX == 0) {
							int x = (int) playerChunk.x + count, y = (int) playerChunk.y + deltaY;
							removeNPCs(loadedChunks[i+renderDistance][j+renderDistance]);
							loadedChunks[i+renderDistance][j+renderDistance] = new Chunk(x, y, noise);
							newNPCs(x, y);
							count++;
						} else {
							int x = (int) playerChunk.x + deltaX, y = (int) playerChunk.y + count;
							loadedChunks[i+renderDistance][j+renderDistance] = new Chunk(x, y, noise);
							newNPCs(x, y);
							count++;
						}
					}
				}
		}
	}
	private void newNPCs(int x, int y){
		int num = getNumNPCs();
		for(int n = 0; n<num; n++) {
			Vector2 chunk = new Vector2(x, y);
			float size = (float) (player.getSize() + (random.nextDouble() * 2 - 1));
			NPCs.add(new NPC(player, size, chunk, shapeRenderer, noise, NPCs));
		}
	}

	private void removeNPCs(Chunk chunk){
		ArrayList<NPC> remove = new ArrayList<>();
		for(NPC npc: NPCs){
			Vector2 npcChunk = getChunkFromPosition(npc.getPosition());
			if(npcChunk.x == chunk.X() && npcChunk.y == chunk.Y())
				remove.add(npc);
		}
		NPCs.removeAll(remove);
	}

	private Vector2 getChunkFromPosition(Vector2 position){
		Vector2 positionChunk = new Vector2((int)(position.x/Chunk.WIDTH), (int)(position.y/Chunk.HEIGHT));
		if(player.getPosition().x<0){
			positionChunk.x--;
		}
		if(player.getPosition().y<0){
			positionChunk.y--;
		}
		return positionChunk;
	}

	private int getNumNPCs(){
		double rand = random.nextDouble();
		int num;
		if(rand<.75) num = 0;
		else if(rand <.90) num = 1;
		else if(rand <.99) num = 2;
		else num = 3;

		return num;
	}

	private void grow(){
		if(player.grow()){
			cam.viewportWidth /= cam.zoom;
			cam.viewportHeight /= cam.zoom;
		}
	}

	private void devCam(){
		cam.viewportWidth /= cam.zoom;
		cam.viewportHeight /= cam.zoom;
	}

	private void restart(){
		create();
	}
}
