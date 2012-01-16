package org.spout.api.protocol;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.spout.api.entity.Entity;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.player.Player;

public class NetworkSynchronizer {
	
	protected final Player owner;
	protected Entity entity;
	protected final Session session;
	
	public NetworkSynchronizer(Player owner) {
		this(owner, null);
	}
	
	public NetworkSynchronizer(Player owner, Entity entity) {
		this.owner = owner;
		this.entity = entity;
		this.session = owner.getSession();
	}
	
	private final static int TARGET_SIZE = 5 * Chunk.CHUNK_SIZE;
	private final static int CHUNKS_PER_TICK = 200;

	private final int viewDistance = 5;
	private final int blockViewDistance = viewDistance * Chunk.CHUNK_SIZE;

	private Point lastChunkCheck;
	
	// Base points used so as not to load chunks unnecessarily
	private Set<Point> chunkInitQueue = new LinkedHashSet<Point>();
	private Set<Point> priorityChunkSendQueue = new LinkedHashSet<Point>();
	private Set<Point> chunkSendQueue = new LinkedHashSet<Point>();
	private Set<Point> chunkFreeQueue = new LinkedHashSet<Point>();

	private Set<Point> initializedChunks = new LinkedHashSet<Point>();
	private Set<Point> activeChunks = new LinkedHashSet<Point>();
	
	private boolean first = true;
	private volatile boolean teleported = false;
	
	private LinkedHashSet<Chunk> observed = new LinkedHashSet<Chunk>();
	
	public void setEntity(Entity entity) {
		this.entity = entity;
	}
	
	public void onDeath() {
		for (Point p : initializedChunks) {
			freeChunk(p);
			activeChunks.remove(p);
			Chunk c = p.getWorld().getChunk(p, false); 
			if (c != null) {
				removeObserver(c);
			}
		}
		initializedChunks.clear();
		entity = null;
	}
	
	public void preSnapshot() {
		
		if (entity == null) {
			return;
		}
		
		// TODO - teleport smoothing
		
		Transform lastTransform = entity.getTransform();
		Transform liveTransform = entity.getLiveTransform();
		
		if (liveTransform != null) {
			Point currentPosition = liveTransform.getPosition();
			
			if (currentPosition.getMahattanDistance(lastChunkCheck) > (Chunk.CHUNK_SIZE >> 1)) {
				checkChunkUpdates(currentPosition);
				lastChunkCheck = currentPosition;
			}
			
			if (first || lastTransform == null || lastTransform.getPosition().getWorld() != liveTransform.getPosition().getWorld()) {
				worldChanged(liveTransform.getPosition().getWorld());
				teleported = true;
			}
		}
		
		for (Point p : chunkFreeQueue) {
			if (initializedChunks.remove(p)) {
				freeChunk(p);
				activeChunks.remove(p);
				Chunk c = p.getWorld().getChunk(p, false); 
				if (c != null) {
					removeObserver(c);
				}
			}
		}
		
		chunkFreeQueue.clear();
		
		for (Point p : chunkInitQueue) {
			if (initializedChunks.add(p)) {
				Chunk c = p.getWorld().getChunk(p, true); 
				initChunk(p);
				addObserver(c);
			}
		}
		
		chunkInitQueue.clear();
		
		int chunksSent = 0;
		
		Iterator<Point> i;
		
		i = priorityChunkSendQueue.iterator();
		while (i.hasNext() && chunksSent < CHUNKS_PER_TICK) {
			Point p = i.next();
			Chunk c = p.getWorld().getChunk(p, true);
			sendChunk(c);
			activeChunks.add(p);
			i.remove();
			chunksSent++;
		}
		
		i = chunkSendQueue.iterator();
		while (i.hasNext() && chunksSent < CHUNKS_PER_TICK) {
			Point p = i.next();
			Chunk c = p.getWorld().getChunk(p, true);
			sendChunk(c);
			activeChunks.add(p);
			i.remove();
			chunksSent++;
		}
		
		if (teleported) {
			sendPosition(liveTransform);
			first = false;
			teleported = false;
		}
		
	}
	
	private void addObserver(Chunk c) {
		observed.add(c);
		c.addObserver(owner);
	}
	
	private void removeObserver(Chunk c) {
		observed.remove(c);
		c.removeObserver(owner);
	}
	

	
	private void checkChunkUpdates(Point currentPosition) {
			
		// Recalculating these
		priorityChunkSendQueue.clear();
		chunkSendQueue.clear();
		chunkFreeQueue.clear();
		chunkInitQueue.clear();

		World world = currentPosition.getWorld();
		int bx = (int)currentPosition.getX();
		int by = (int)currentPosition.getY();
		int bz = (int)currentPosition.getZ();
		
		Point playerChunkBase = Chunk.pointToBase(currentPosition);
		
		for (Point p : initializedChunks) {
			if (p.getMahattanDistance(playerChunkBase) > blockViewDistance) {
				chunkFreeQueue.add(p);
			}	
		}
		
		int cx = bx >> Chunk.CHUNK_SIZE_BITS;
		int cy = by >> Chunk.CHUNK_SIZE_BITS;
		int cz = bz >> Chunk.CHUNK_SIZE_BITS;
		
		// TODO - circle loading
		for (int x = cx - viewDistance; x < cx + viewDistance; x++) {
			for (int y = cy - viewDistance; y < cy + viewDistance; y++) {
				for (int z = cz - viewDistance; z < cz + viewDistance; z++) {
					Point base = new Point(world, x << Chunk.CHUNK_SIZE_BITS, y << Chunk.CHUNK_SIZE_BITS, z << Chunk.CHUNK_SIZE_BITS);
					double distance = base.getMahattanDistance(playerChunkBase);
					if (distance <= blockViewDistance) {
						if (!activeChunks.contains(base)) {
							if (distance <= TARGET_SIZE) {
								priorityChunkSendQueue.add(base);
							} else {
								chunkSendQueue.add(base);
							}
						}
						if (!initializedChunks.contains(base)) {
							chunkInitQueue.add(base);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Sends a chunk to the client.
	 * 
	 * This method is called during the startSnapshot stage of the tick.
	 * 
	 * This is a MONITOR method, for sending network updates, no changes should be made to the chunk
	 * 
	 * @param c the chunk
	 */
	public void sendChunk(Chunk c){
		//TODO: Implement Spout Protocol
	}
	
	/**
	 * Frees a chunk on the client.
	 * 
	 * This method is called during the startSnapshot stage of the tick.
	 * 
	 * This is a MONITOR method, for sending network updates, no changes should be made to the chunk
	 * 
	 * @param p the base Point for the chunk
	 */
	protected void initChunk(Point p){
		//TODO: Implement Spout Protocol
	}
	
	/**
	 * Frees a chunk on the client.
	 * 
	 * This method is called during the startSnapshot stage of the tick.
	 * 
	 * This is a MONITOR method, for sending network updates, no changes should be made to the chunk
	 * 
	 * @param p the base Point for the chunk
	 */
	protected void freeChunk(Point p){
		//TODO: Inplement Spout Protocol
	}
	
	/**
	 * Sends the player's position to the client
	 * 
	 * This method is called during the startSnapshot stage of the tick.
	 * 
	 * This is a MONITOR method, for sending network updates, no changes should be made to the chunk
	 * 
	 * @param t the transform
	 */
	protected void sendPosition(Transform t){
		//TODO: Implement Spout Protocol
	}
	
	/**
	 * Called when the player's world changes.
	 * 
	 * This method is called during the startSnapshot stage of the tick.
	 * 
	 * This is a MONITOR method, for sending network updates, no changes should be made to the chunk
	 * 
	 * @param t the transform
	 */
	protected void worldChanged(World world){
		//TODO: Implement Spout Protocol
	}
	
	/**
	 * Called when a block in a chunk that the player is observing changes.<br>
	 * <br>
	 * Note: The coordinates of the block are chunk relative and the world field is undefined.
	 * 
	 * @param chunk the chunk
	 * @param block the block
	 */
	public void updateBlock(Chunk chunk, Block block) {
	}
	
}