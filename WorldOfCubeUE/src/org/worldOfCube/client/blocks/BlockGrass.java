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
package org.worldOfCube.client.blocks;

import java.awt.Color;

import org.worldOfCube.client.logic.chunks.Chunk;
import org.worldOfCube.client.res.ResLoader;
import org.worldOfCube.client.res.ResLoader.Blocks;

public class BlockGrass extends Block {

	public static final Blocks BLOCK_TEX = ResLoader.Blocks.GRASS;

	private static final Color awtCol = new Color(0x487634);

	public BlockGrass(byte x, byte y, Chunk c, boolean foreground) {
		super(x, y, c, foreground);
	}

	public BlockGrass(boolean foreground) {
		super(foreground);
	}

	@Override
	public void init() {
		if (borderID == ResLoader.TileTypes.FILLED.ordinal()) {
			replaceWithEarth();
		}
	}

	@Override
	public void update() {
		super.update();
		if (borderID == ResLoader.TileTypes.FILLED.ordinal()) {
			replaceWithEarth();
		}
	}

	@Override
	public void render() {
		super.renderIntern(ResLoader.get(BLOCK_TEX, borderID));
	}

	@Override
	public void renderBackground() {
		super.renderBackgroundIntern(ResLoader.get(BLOCK_TEX, borderID));
	}

	@Override
	public boolean isValidNeighbor(int x, int y) {
		Block b = c.getBlock(x, y, foreground);
		if (b != null) {
			return b instanceof BlockTreewood
					|| b instanceof BlockGrass
					|| b instanceof BlockEarth
					|| b instanceof BlockRock
					|| b instanceof BlockWood;
		}
		return false;
	}

	@Override
	public boolean containsAlpha() {
		return borderID != ResLoader.TileTypes.FILLED.ordinal();
	}

	@Override
	public void destroy() {
	}

	public void replaceWithEarth() {
		BlockEarth eb = new BlockEarth(x, y, c, foreground);
		c.setLocalBlock(x, y, eb, foreground);
		eb.update();
	}

	@Override
	public Color getAWTBackgroundColor() {
		return awtCol;
	}

}
