/*
 * This file is part of SpoutAPI (http://www.getspout.org/).
 *
 * SpoutAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SpoutAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.spout.api.geo;

import org.spout.api.entity.BlockController;
import org.spout.api.material.BlockMaterial;
import org.spout.api.util.thread.LiveRead;
import org.spout.api.util.thread.SnapshotRead;

public interface AreaBlockSource {
	/**
	 * Gets the material for the block at (x, y, z)
	 *
	 * @param x coordinate of the block
	 * @param y coordinate of the block
	 * @param z coordinate of the block
	 * @return the block's material from the snapshot
	 */
	@LiveRead
	public BlockMaterial getBlockMaterial(int x, int y, int z);

	/**
	 * Gets the data for the block at (x, y, z)
	 *
	 * @param x coordinate of the block
	 * @param y coordinate of the block
	 * @param z coordinate of the block
	 * @return the block's data from the snapshot
	 */
	@LiveRead
	public short getBlockData(int x, int y, int z);

	/**
	 * Gets the block light value for the block at (x, y, z)
	 *
	 * @param x coordinate of the block
	 * @param y coordinate of the block
	 * @param z coordinate of the block
	 * @return the block's block light value
	 */
	@LiveRead
	public byte getBlockLight(int x, int y, int z);

	/**
	 * Gets the sky light value for the block at (x, y, z)
	 *
	 * @param x coordinate of the block
	 * @param y coordinate of the block
	 * @param z coordinate of the block
	 * @return the block's sky light value
	 */
	@LiveRead
	public byte getBlockSkyLight(int x, int y, int z);

	/**
	 * Gets the {@link BlockController} for the block at (x, y, z)
	 *
	 * @param x coordinate of the block
	 * @param y coordinate of the block
	 * @param z coordinate of the block
	 * @return the block controller
	 */
	@SnapshotRead
	public BlockController getBlockController(int x, int y, int z);
}