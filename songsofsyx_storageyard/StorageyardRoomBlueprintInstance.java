package jackthestripper.songsofsyx_storageyard;


import static settlement.main.SETT.*;

import java.io.IOException;

import init.boostable.BOOSTABLE;
import init.boostable.BOOSTABLES;
import init.resources.RESOURCE;
import settlement.misc.util.RESOURCE_TILE;
import settlement.path.finder.SFinderRoomService;
import settlement.room.main.Room;
import settlement.room.main.RoomBlueprintIns;
import settlement.room.main.job.ROOM_EMPLOY_AUTO;
import settlement.room.main.job.ROOM_RADIUS.ROOM_RADIUSE;
import settlement.room.main.util.RoomInitData;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.misc.CLAMP;
import snake2d.util.sets.LISTE;
import util.statistics.HistoryResource;
import view.sett.ui.room.UIRoomModule;

public final class StorageyardRoomBlueprintInstance extends RoomBlueprintIns<StorageyardRoomInstance> implements ROOM_RADIUSE, ROOM_EMPLOY_AUTO{

	public final static int CRATE_MAX = 0x0FF;
	protected final StorageyardTally tally = new StorageyardTally();
	
	final StorageyardFurnisher constructor;
	
	public final BOOSTABLE bonus;
	final StorageyardCrate crate = new StorageyardCrate(this);
	private static CharSequence bname = "Carry Capacity";
	private static CharSequence bdesc = "Carry Capacity of all logistics workers.";
	

	
	public StorageyardRoomBlueprintInstance(RoomInitData init) throws IOException {
		super(0, init, "STORAGEYARD_STOCKPILE", init.m.CATS.MAIN_INFRA.misc);
		constructor = new StorageyardFurnisher(this, init);
		bonus = BOOSTABLES.ROOMS().pushRoom(this, init.data(), null, bname, bdesc);
	}
	
	void tally(int res, int crates, int amountTot, int amountUnres, int spaceRes, boolean fetch) {
		
		tally.tally(res, crates, amountTot, amountUnres, spaceRes, CRATE_MAX, fetch);
		
	}

	
	@Override
	protected void update(float ds) {
	
		
		
//		tally.needsFetching.clear();
//		for (int i = 0; i < RESOURCE.all().size(); i++) {
//			RESOURCE r = RESOURCE.all().get(i);
//			if (hasSpaceFor(r) && THINGS().resources.hasRandomNext(r)) {
//				tally.needsFetching.add(r);
//			}
//		}
//		tally.fetchI = 0;
		
	}

	
	@Override
	public StorageyardFurnisher constructor() {
		return constructor;
	}

	@Override
	public SFinderRoomService service(int tx, int ty) {
		return null;
	}
	
	public StorageyardTally tally() {
		return tally;
	}
	
	@Override
	protected void saveP(FilePutter saveFile){
		tally.saver.save(saveFile);
	}
	
	@Override
	protected void loadP(FileGetter saveFile) throws IOException{
		tally.saver.load(saveFile);
//		for (COORDINATE c : SETT.TILE_BOUNDS) {
//			StorageCrate cc = crate.get(c.x(), c.y());
//			if (cc != null && (cc.resource() == RESOURCES.BATTLEGEAR()))
//				cc.storageUnreserve(cc.storageReserved());
//		}
		
	}
	
	@Override
	protected void clearP() {
		this.tally.saver.clear();
	}
	
	@Override
	public void appendView(LISTE<UIRoomModule> mm) {
		mm.add(new StorageyardGui(this).make());
	}

	@Override
	public byte radiusRaw(Room t) {
		return ((StorageyardRoomInstance) t).radius;
	}

	@Override
	public void radiusRawSet(Room t, byte r) {
		((StorageyardRoomInstance) t).radius = r;
	}

	@Override
	public boolean autoEmploy(Room r) {
		return ((StorageyardRoomInstance) r).autoE;
	}

	@Override
	public void autoEmploy(Room r, boolean b) {
		((StorageyardRoomInstance) r).autoE = b;
	}
	
	public void removeFromEverywhere(double am, long mask, HistoryResource record) {
		for (COORDINATE c : TILE_BOUNDS) {
			Room r = ROOMS().STOCKPILE.get(c.x(), c.y());
			if (r == null)
				continue;
			RESOURCE_TILE cr = (RESOURCE_TILE) r.storage(c.x(), c.y());
			if (cr != null && cr.resource() != null && (cr.resource().bit & mask) != 0) {
				int a = (int) Math.ceil(am*cr.reservable());
				for (int i = 0; i < a; i++) {
					cr.findableReserve();
					cr.resourcePickup();
				}
				record.inc(cr.resource(), a);
			}
		}
	}
	
	public void remove(RESOURCE res, int total, HistoryResource record) {
		
		double d = tally.amountReservable(res);
		if (d == 0)
			return;
		d = total/d;
		d = CLAMP.d(d, 0, 1);
		for (int ii = 0; ii < instancesSize(); ii++) {
			StorageyardRoomInstance i = getInstance(ii);
			int am = (int) Math.ceil(d*i.amountUnreservedGet(res));
			if (am == 0)
				continue;
			
			for (COORDINATE c : i.body()) {
				if (i.is(c)) {
					RESOURCE_TILE cr = (RESOURCE_TILE) i.storage(c.x(), c.y());
					if (cr != null && cr.resource() == res) {
						
						int a = CLAMP.i(am, 0, cr.reservable());
						for (int k = 0; k < a; k++) {
							cr.findableReserve();
							cr.resourcePickup();
							total --;
							if (total <= 0) {
								record.inc(cr.resource(), k);
								return;
							}
						}
						record.inc(cr.resource(), a);
					}
				}
			}
			
		}
	}
	
}
