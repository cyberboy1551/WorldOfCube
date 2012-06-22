package org.worldOfCube.client.logic.inventory;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;

import org.lwjgl.input.Mouse;
import org.worldOfCube.client.blocks.BlockID;
import org.worldOfCube.client.input.WrappedMouse;
import org.worldOfCube.client.res.ResLoader;
import org.worldOfCube.client.res.Sprite;
import org.worldOfCube.client.util.Distance;

public class Item {
	
	public static final int DRAW_SIZE = 16;
	public static final float MAX_SCALE = 0.5f; 
	public static final float SCALE_DISTANCE = 96f;

	//TODO: Blocks: Add as Item.
	public static final int EARTH = 0;
	public static final int GRASS = 1;
	public static final int ROCK = 2;
	public static final int LIGHTSTONE = 3;
	public static final int TREEWOOD = 4;
	public static final int LEAVES = 5;
	public static final int WOOD = 6;
	
	private int id;
	
	public Item(int id) {
		this.id = id;
	}
	
	public int getID() {
		return id;
	}
	
	public void render(float x, float y) {
		float distance = Distance.get(Mouse.getX(), WrappedMouse.getY(), x, y)/SCALE_DISTANCE;
		float scale = 1f+MAX_SCALE-Math.max(0f, Math.min(MAX_SCALE, distance));
		Sprite s = BlockID.itemToSprite(id);
		s.bind();
		glColor3f(1f, 1f, 1f);
		glTranslatef(x, y, -1f);
		glScalef(scale, scale, 0f);
		glBegin(GL_QUADS);
		{
			s.glTexCoord(0);
			glVertex2f(-ResLoader.BLOCK_SIZE/2, -ResLoader.BLOCK_SIZE/2);
			s.glTexCoord(1);
			glVertex2f(+ResLoader.BLOCK_SIZE/2, -ResLoader.BLOCK_SIZE/2);
			s.glTexCoord(2);
			glVertex2f(+ResLoader.BLOCK_SIZE/2, +ResLoader.BLOCK_SIZE/2);
			s.glTexCoord(3);
			glVertex2f(-ResLoader.BLOCK_SIZE/2, +ResLoader.BLOCK_SIZE/2);
		}
		glEnd();
	}
	
	public static int getStackable(int id) {
		switch (id) {
		default: return 500;
		}
	}

}
