/*
 * Copyright (c) 2012 matheusdev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.worldOfCube.client.logic.chunks;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.magicwerk.brownies.collections.GapList;
import org.magicwerk.brownies.collections.MaxList;
import org.matheusdev.util.Dist;
import org.worldOfCube.client.blocks.Block;
import org.worldOfCube.client.input.InputListener;
import org.worldOfCube.client.logic.chunks.generation.Generator;
import org.worldOfCube.client.logic.chunks.light.LightUpdater;
import org.worldOfCube.client.logic.collision.Rectangle;
import org.worldOfCube.client.logic.entity.Entity;
import org.worldOfCube.client.logic.entity.EntityDrop;
import org.worldOfCube.client.logic.entity.EntityPlayer;
import org.worldOfCube.client.res.ResLoader;
import org.worldOfCube.client.util.debug.PerfMonitor;
import org.worldOfCube.client.util.list.ImmutableWrappingList;
import org.worldOfCube.client.util.vecmath.Vec;

/**
 * @author matheusdev
 *
 */
public abstract class World implements InputListener {

	public static final double GRAVITY = 2*64*9.81;
	public static final int MAX_DROPS = 2000;

	public final int totalPix;
	public final int totalBlocks;
	public final Random rand;

	protected final Rectangle bounds;
	protected final Rectangle viewport;
	protected final String name;
	protected ChunkManager cManager;
	protected LightUpdater light;

	protected GapList<Entity> entitys = new GapList<Entity>();
	protected MaxList<EntityDrop> drops = new MaxList<EntityDrop>(MAX_DROPS);
	protected GapList<EntityPlayer> players = new GapList<EntityPlayer>();
	protected HashMap<String, EntityPlayer> playerNames = new HashMap<String, EntityPlayer>();

	/**
	 * <p>Calls {@link #World(int, int, long, String)} with
	 * <tt>seed = System.nanoTime() ^ (System.currentTimeMillis() >>> 10)</tt></p>
	 * @see #World(int, int, long, String)
	 */
	public World(int numChunks, int chunkSize, String name) {
		this(numChunks, chunkSize, System.nanoTime() ^ (System.currentTimeMillis() >>> 10), name);
	}

	/**
	 * <p>Creates a new World instance, which will have a <strong>newly generated ChunkManager</strong>,
	 * which means, that the world will have not previously defined blocks on the Chunks in ChunkManager.</p>
	 * @param numChunks the number of chunks in both x and y axis.
	 * @param chunkSize the number of blocks in the chunk on both the x and y axis.
	 * @param seed the seed to use for the random number generator.
	 * @param name the name of the world.
	 */
	public World(int numChunks, int chunkSize, long seed, String name) {
		// Initialize constructor stuff:
		this.name = name;
		// Calculate the helper-Constants:
		//  - totalBlocks: The total size of blocks along the whole world
		//  - totalPix: The total number of pixels along the whole world
		totalBlocks = numChunks*chunkSize;
		totalPix = totalBlocks*ResLoader.BLOCK_SIZE;
		// Initialize the bounds of the world and the viewport. viewport = bounds by default.
		bounds = new Rectangle(0, 0, totalPix, totalPix);
		viewport = new Rectangle(bounds);
		// Create ChunkManager with the sizes set in constructor:
		cManager = new ChunkManager(numChunks, chunkSize);
		// New Random for getting random numbers.
		rand = new Random();
		// Generate the world on the new ChunkManager:
		generateWorld();
	}

	/**
	 * <p>Creates a new World instance, which will have a <strong>previously generated {@link ChunkManager} on it</strong></p>
	 * <p>You can choose the ChunkManager in the constructor's arguments. The world will
	 * exactly have the same ChunkManager as the given one. The world will not change the
	 * blocks on the ChunkManager on creation</p>
	 * @param cManager the ChunkManager with all definitions of Blocks.
	 * @param name the name of the world.
	 * @throws NullPointerException if "cManager" is null.
	 */
	public World(ChunkManager cManager, String name) {
		if (cManager == null) throw new NullPointerException("cManager == null");
		// Initialize stuff given in the constructor arguments:
		// Especially the ChunkManager, which is already given here
		this.cManager = cManager;
		this.name = name;
		// Calculate the helper-Constants (see other constructor code's comments for more info)
		totalBlocks = cManager.getSize() * cManager.getChunkSize();
		totalPix = totalBlocks * ResLoader.BLOCK_SIZE;
		// Initialize bounds and viewport, which equals bounds by default.
		bounds = new Rectangle(0, 0, totalPix, totalPix);
		viewport = new Rectangle(bounds);
		// Initialize Random instance:
		rand = new Random(System.nanoTime() ^ (System.currentTimeMillis() >>> 10));
	}

	/**
	 * <p>This method generates the world. Should be called AFTER the {@link #cManager}
	 * was created.</p>
	 * <p>This will first initialize an instance of {@link Generator}, and then
	 * call {@link ChunkManager#create(Generator)} on {@link #cManager}.</p>
	 */
	public void generateWorld() {
		Generator g = new Generator(0.6f, 4f, rand, this);
		cManager.create(g); // Creates the blocks according to the information from the "Generator".
		cManager.updateAll();
		g.generateTrees();
	}

	/**
	 * The update-step, called every frame. This does only update things,
	 * but does not render.
	 * @param d the delta time, passed since the last update. Usually gotten from {@link org.worldOfCube.client.screens.Screen#getDelta()}
	 */
	protected void tick(double d) {
		PerfMonitor.startProfile("ENTITY TICK");
		for (Entity e : entitys) {
			e.tick(d, this);
		}
		for (int i = 0; i < drops.size(); i++) {
			EntityDrop drop = drops.get(i);
			EntityPlayer nearestPlayer = nearestPlayer(drop.midx(), drop.midy());
			double dist = Dist.squared(drop.midx(), drop.midy(), nearestPlayer.midx(), nearestPlayer.midy());

			if (dist < 64*64) {
				if (dist < 8*8) {
					if (nearestPlayer.collect(drop)) {
						removeEntity(drop);
					}
				} else {
					Vec delta = new Vec(drop.midx(), drop.midy(), nearestPlayer.midx(), nearestPlayer.midy());
					delta.normalize();
					delta.mul(6f);

					drop.move(delta.x, delta.y, this);
				}
			}
		}
		PerfMonitor.stopProfile("ENTITY TICK");
	}

	public abstract void render();

	/**
	 * <p>Converts the given <tt>xposition</tt> from window-space to
	 * a world-space position.</p>
	 * <p>With this you can, for example, convert a mouse click from
	 * a click position on the window to the actual world position in the
	 * world according to the current viewport.</p>
	 * @param xposition the x position to convert.
	 * @return the converted x position (world-space).
	 */
	public double convertXToWorld(double xposition) {
		return viewport.x + xposition;
	}

	/**
	 * <p>Converts the given <tt>yposition</tt> from window-space to
	 * a world-space position.</p>
	 * <p>With this you can, for example, convert a mouse click from
	 * a click position on the window to the actual world position in the
	 * world according to the current viewport.</p>
	 * @param xposition the y position to convert.
	 * @return the converted y position (world-space).
	 */
	public double convertYToWorld(double yposition) {
		return viewport.y + yposition;
	}

	public Rectangle getViewport() {
		return viewport;
	}

	public Rectangle getBounds() {
		return bounds;
	}

	public List<Entity> getEntitys() {
		return new ImmutableWrappingList<Entity>(entitys);
	}

	public EntityPlayer getPlayer(String name) {
		return playerNames.get(name);
	}

	public String getName() {
		return name;
	}

	public void destroy() {
	}

	/**
	 * <p>Removes the given {@link Entity} form the inner-used lists of <tt>Entity</tt>s.</p>
	 * <p>World caches <tt>Entity</tt>s in multiple Lists and Data Structures. If you call
	 * this method to remove an <tt>Entity</tt> you can be sure, this Entity will be
	 * removed entirely. So if no reference to the <tt>Entity</tt> is hold outside, the
	 * <tt>Entity</tt> will be GCed.</p>
	 * @param e the {@link Entity} to be removed.
	 */
	public void removeEntity(Entity e) {
		if (e instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer) e;

			players.remove(ep);
			playerNames.remove(ep.getName());
		} else if (e instanceof EntityDrop) {
			drops.remove(e);
		}
		entitys.remove(e);
	}

	/**
	 * <p>Adds the given {@link Entity} to the inner-used lists of <tt>Entity</tt>s</p>
	 * <p>If the <tt>Entity</tt> is an instance of special <tt>Entity</tt>s, for
	 * example {@link EntityPlayer} or {@link EntityDrop}, then a reference to the
	 * <tt>Entity</tt> will also be added to these Lists.</p>
	 * <p>To remove an <tt>Entity</tt>, see {@link #removeEntity(Entity)}</p>
	 * @param e the {@link Entity} to be added.
	 */
	public void addEntity(Entity e) {
		if (e == null) throw new NullPointerException("e == null");
		if (e instanceof EntityPlayer) {
			EntityPlayer ep = (EntityPlayer) e;
			playerNames.put(ep.getName(), ep);
			players.add(ep);
		} else if (e instanceof EntityDrop) {
			EntityDrop ed = (EntityDrop) e;
			drops.add(ed);
		}
		entitys.add(e);
	}

	public EntityPlayer nearestPlayer(double x, double y) {
		double lastDist = Double.MAX_VALUE;
		EntityPlayer nearest = null;
		for (int i = 0; i < players.size(); i++) {
			double dist = Dist.squared(x, y, players.get(i).midx(), players.get(i).midy());
			if (dist < lastDist) {
				lastDist = dist;
				nearest = players.get(i);
			}
		}
		return nearest;
	}

	/**
	 * <p>Returns, whether the {@link Entity} <tt>e</tt> exists in any of the used Data Structures for
	 * <tt>Entity</tt>s.</p>
	 * @param e whether the {@link Entity} <tt>e</tt> exists in any of the used Data Structures for
	 * <tt>Entity</tt>s.
	 * @throws NullPointerException if <tt>e == null</tt>
	 */
	public boolean isEntityExisting(Entity e) {
		if (e == null) throw new NullPointerException("e == null");
		return entitys.contains(e);
	}

	public ChunkManager getChunkManager() {
		return cManager;
	}

	@Override
	public void handleKeyEvent(int keyCode, char keyChar, boolean down) {
		for (Entity e : entitys) {
			e.handleKeyEvent(keyCode, keyChar, down, this);
		}
	}

	@Override
	public void handleMouseEvent(int mousex, int mousey, int button, boolean down) {
		for (Entity e : entitys) {
			e.handleMouseEvent(mousex, mousey, button, down, this);
		}
	}

	@Override
	public void handleMousePosition(int mousex, int mousey) {
		for (Entity e : entitys) {
			e.handleMousePosition(mousex, mousey, this);
		}
	}

	/**
	 * @param rect the rect to check the collision with.
	 * @return whether the rect is intersecting any block in the whole world.
	 */
	public boolean rectCollidesBlocks(Rectangle rect) {
		int beginx = (int) (rect.x / ResLoader.BLOCK_SIZE);
		int beginy = (int) (rect.y / ResLoader.BLOCK_SIZE);
		int endx = (int) ((rect.x + rect.w) / ResLoader.BLOCK_SIZE) + 1;
		int endy = (int) ((rect.y + rect.h) / ResLoader.BLOCK_SIZE) + 1;
		Block b;
		for (int x = beginx; x < endx; x++) {
			for (int y = beginy; y < endy; y++) {
				b = cManager.getBlock(x, y, true);
				if (b != null) {
					Rectangle[] rects = b.getCollisionRects();
					for (int i = 0; i < rects.length; i++) {
						if (rect.intersects(rects[i])) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

}
