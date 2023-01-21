package jackthestripper.songsofsyx_storageyard;


	import static settlement.main.SETT.*;

	import game.GAME;
	import init.boostable.BOOSTABLE;
	import init.resources.RESOURCE;
	import init.resources.RESOURCES;
	import settlement.main.RenderData;
	import settlement.main.SETT;
	import settlement.misc.util.RESOURCE_TILE;
	import settlement.misc.util.TILE_STORAGE;
	import settlement.path.finder.SPath;
	import settlement.room.infra.transport.ROOM_DELIVERY_INSTANCE;
	import settlement.room.main.RoomInstance;
	import settlement.room.main.TmpArea;
	import settlement.room.main.job.ROOM_RADIUS.ROOM_RADIUS_INSTANCE;
	import settlement.room.main.job.StorageCrate;
	import settlement.room.main.util.RoomInit;
	import settlement.room.main.util.RoomState;
	import settlement.room.main.util.RoomState.RoomStateInstance;
	import settlement.thing.ThingsResources.ScatteredResource;
	import snake2d.Renderer;
	import snake2d.util.datatypes.COORDINATE;
	import snake2d.util.misc.CLAMP;
	import snake2d.util.sets.ArrayCooShort;
	import util.rendering.ShadowBatch;

	public final class StorageyardRoomInstance extends RoomInstance implements StorageCrate.STORAGE_CRATE_HASSER, ROOM_RADIUS_INSTANCE, ROOM_DELIVERY_INSTANCE {

		private final static long serialVersionUID = -7063521835843676015l;
		private int usedCrates = 0;
		final short[] allocated = new short[RESOURCES.ALL().size()];
		final int[] amountTotal = new int[RESOURCES.ALL().size()];
		final int[] amountUnreserved = new int[RESOURCES.ALL().size()];
		final int[] spaceReserved = new int[RESOURCES.ALL().size()];
		
		private long fetchMaximums = 0;
		private final ArrayCooShort crates;

		private long fetchMaskBig = 0;
		private long fetchMask = 0;
		private long hasMask = 0;
		private long fetchOther = 0;
		private byte searchStatus = 0;
		byte radius = 100;
		private StorageyardRoomInstance emptyTo;
		private StorageyardRoomBlueprintInstance ins;
		boolean autoE;
		

		StorageyardRoomInstance(StorageyardRoomBlueprintInstance p, TmpArea area, RoomInit init) {
			super(p, area, init);
			ins = p;

			int crateI = 0;
			for (COORDINATE c : body()) {
				if (!is(c))
					continue;
				StorageCrate cr = p.crate.get(c.x(), c.y(), this);
				if (cr != null) {
					crateI++;
				}
			}
			
			crates = new ArrayCooShort(crateI);
		
			crateI = 0;
			for (COORDINATE c : body()) {
				if (!is(c))
					continue;
				if (p.crate.get(c.x(), c.y(), this) != null) {
					crates.set(crateI++).set(c.x(), c.y());
				}
			}
			
			crates.shuffle(crates.size());
			
			crates.set(0);
			
			while(crates.hasNext()) {
				ScatteredResource s = SETT.THINGS().resources.tGet.get(crates.get());
				StorageCrate c = p.crate.get(crates.get().x(), crates.get().y(), this);
				
				if (s != null) {
					
					c.resourceSet(s.resource());
					int am = CLAMP.i(s.amount(), 0, StorageyardRoomBlueprintInstance.CRATE_MAX - c.amount());
					c.amountSet(c.amount() + am);
					while(am-- > 0) {
						if (!s.findableReservedIs())
							s.findableReserve();
						s.resourcePickup();
					}
				}
				crates.next();
			}
			crates.set(0);
			
			employees().maxSet(crates.size()*2);
			activate();
		}
		
		private void updateMasks() {
			fetchMask = 0;
			fetchMaskBig = 0;
			for (RESOURCE r : RESOURCES.ALL()) {
				int am = allocated[r.bIndex()]*StorageyardRoomBlueprintInstance.CRATE_MAX;
				am -= amountTotal[r.bIndex()];
				am -= spaceReserved[r.bIndex()];
				if (am > 0) {
					fetchMask |= r.bit;
				}
				if (am > 64)
					fetchMaskBig |= r.bit;
			}
		}

		@Override
		protected boolean render(Renderer r, ShadowBatch shadowBatch, RenderData.RenderIterator it) {
			super.render(r, shadowBatch, it);
			it.lit();
			return false;
		}
		
		void addCrate(int res, int crates, int amountTot, int amountUnres, int spaceRes) {
			amountTotal[res] += amountTot;
			amountUnreserved[res] += amountUnres;
			spaceReserved[res] += spaceRes;
			usedCrates -= allocated[res];
			allocated[res]+=crates;
			usedCrates += allocated[res];
			
			
			if (amountTotal[res] < 0)
				throw new RuntimeException();
			if (amountUnreserved[res] < 0)
				throw new RuntimeException();
			if (spaceReserved[res] < 0)
				throw new RuntimeException();
			if (allocated[res] < 0)
				throw new RuntimeException();
			ins.tally(res, crates, amountTot, amountUnres, spaceRes, getsMaximum(RESOURCES.ALL().get(res)));
			
			long bit = RESOURCES.ALL().get(res).bit;
			
			fetchMask &= ~bit;
			fetchMaskBig &= fetchMask;
			int am = allocated[res]*StorageyardRoomBlueprintInstance.CRATE_MAX - amountTotal[res] - spaceReserved[res];
			//GAME.Notify(RESOURCES.ALL().get(res).name + " " + am + " " + allocated[res]*ROOM_STOCKPILE.CRATE_MAX + " " + amountTotal[res] + " " + spaceReserved[res] + " " + spaceRes);

			if (am > 5) {
				fetchMask |= RESOURCES.ALL().get(res).bit;
			}
			if (am > 64)
				fetchMaskBig |= RESOURCES.ALL().get(res).bit;
			if (amountUnreserved[res] > 0)
				hasMask |= RESOURCES.ALL().get(res).bit;
			else
				hasMask &= ~RESOURCES.ALL().get(res).bit;
		}

		void allocateCrate(RESOURCE res, int amount){
			
			
			if (amount < allocated[res.bIndex()]) {
				for (COORDINATE c : body()) {
					if (!is(c))
						continue;
					StorageCrate crate = blueprintI().crate.get(c.x(), c.y(), this);
					if (crate == null)
						continue;
					if (crate.resource() != res)
						continue;
					if (crate.amount() != 0)
						continue;
					crate.clear();
					if (allocated[res.bIndex()] == amount)
						break;
				}
				if (amount < allocated[res.bIndex()]) {
					for (COORDINATE c : body()) {
						if (!is(c))
							continue;
						StorageCrate crate = blueprintI().crate.get(c.x(), c.y(), this);
						if (crate == null)
							continue;
						if (crate.resource() != res)
							continue;
						crate.clear();
						
						if (allocated[res.bIndex()] == amount)
							break;
					}
				}
					
				
			}else if(amount > allocated[res.bIndex()]) {
				for (COORDINATE c : body()) {
					if (!is(c))
						continue;
					StorageCrate crate = blueprintI().crate.get(c.x(), c.y(), this);
					
					if (crate == null)
						continue;
					if (crate.resource() != null)
						continue;
					crate.resourceSet(res);
					
					if (allocated[res.bIndex()] == amount)
						break;
				}
			}

		}
		
		int usedCrates() {
			return usedCrates;
		}

		int totalCrates() {
			return crates.size();
		}

		public int amountGet(RESOURCE r) {
			return amountTotal[r.bIndex()];
		}
		
		public int amountUnreservedGet(RESOURCE r) {
			return amountUnreserved[r.bIndex()];
		}

		public int storageGet(RESOURCE r) {
			return allocated[r.bIndex()] * StorageyardRoomBlueprintInstance.CRATE_MAX;
		}
		
		public int cratesGet(RESOURCE r) {
			return allocated[r.bIndex()];
		}
		
		public int storageUnreserved(RESOURCE r) {
			return storageGet(r) - spaceReserved[r.bIndex()];
		}

		void setGetMaximum(RESOURCE r, boolean get) {
			boolean now = (fetchMaximums & r.bit) == r.bit;
			
			
			if (now != get) {
				
				for (int i = 0; i < crates.size(); i++) {
					blueprintI().crate.get(crates.get().x(), crates.get().y(), this).remove();
					crates.inc();
				}
				
				fetchMaximums ^= r.bit;
				
				for (int i = 0; i < crates.size(); i++) {
					blueprintI().crate.get(crates.get().x(), crates.get().y(), this).add();
					crates.inc();
				}
			}
			
			updateMasks();
		}

		@Override
		public boolean getsMaximum(RESOURCE r) {
			return (fetchMaximums & r.bit) == r.bit;
		}
		
		public boolean getsMaximum(int r) {
			return getsMaximum(RESOURCES.ALL().get(r));
		}

		@Override
		protected void updateAction(double ds, boolean day, int daycount) {

			if (!active() || employees().employed() <= 0)
				return;
			searchStatus = 0;
		}

		@Override
		public TILE_STORAGE job(COORDINATE start, SPath path) {
			
			
			if (searchStatus == 2)
				return null;
			
			
			RESOURCE res = null;
			if (fetchOther == 0)
				fetchOther = -1;
			if (searchStatus == 0 && (fetchOther & fetchMask) != 0) {
				res = PATH().finders.resource.find((fetchOther & fetchMask), fetchOther & fetchMask & fetchMaximums, 0, start, path, radius());
				if (res != null)
					fetchOther &= ~res.bit;
				else
					fetchOther = -1;
			}else {
				fetchOther = -1;
			}
			
			if (res == null && searchStatus == 0 && fetchMaskBig != 0) {
				res = PATH().finders.resource.find(fetchMaskBig, fetchMaskBig & fetchMaximums, 0, start, path, radius());
				if (res == null)
					searchStatus = 1;
			}
			
			if (res == null) {
				res = PATH().finders.resource.find(fetchMask, fetchMask & fetchMaximums, 0, start,  path, radius());
			}
			
			if (res == null) {
				updateMasks();
				searchStatus = 2;
				return null;
			}
			
			for (int i = 0; i < crates.size(); i++) {
				crates.inc();
				TILE_STORAGE c = blueprintI().crate.get(crates.get().x(), crates.get().y(), this);
				if (c.resource() == res && c.storageReservable() > 0) {
					return c; 
				}
				
			}
			

			GAME.Notify("weird!");
			
			PATH().finders.resource.unreserve(res, path.destX(), path.destY(), 1);
			updateMasks();
			searchStatus = 2;
			return null;
			
		}
		
		@Override
		public TILE_STORAGE job(int tx, int ty) {
			if (is(tx, ty))
				return blueprintI().crate.get(tx, ty, this);
			return null;
		}
		
		public TILE_STORAGE emptyJob(COORDINATE start, SPath path) {
			
			StorageyardRoomInstance e = emptyTo();
			
			if (e == null)
				return null;
			
			long m = hasMask & e.fetchMaskBig & ~fetchMaximums;
			
			if (m == 0)
				return null;
			
			RESOURCE_TILE c = null;
			for (int i = 0; i < crates.size(); i++) {
				crates.inc();
				RESOURCE_TILE c2 = blueprintI().crate.get(crates.get().x(), crates.get().y(), this);
				if (c2.resource() != null && (c2.resource().bit & m) != 0 && c2.findableReservedCanBe()) {
					c = c2;
					break;
				}
			}
			
			
			
			if (c == null) {
				GAME.Notify("weird!");
				return null;
			}
			
			RESOURCE r = c.resource();
			
			if (!path.request(start, c.x(), c.y()))
				return null;
			
			for (int i = 0; i < e.crates.size(); i++) {
				e.crates.inc();
				TILE_STORAGE s = blueprintI().crate.get(e.crates.get().x(), e.crates.get().y(), this);
				if (s.resource() == r && s.storageReservable() > 0) {
					return s;
				}
			}
			
			GAME.Notify("weird!");
			return null;
			
		}
		
		@Override
		protected void dispose() {
			
			for (int i = 0; i < crates.size(); i++) {
				StorageCrate crate = blueprintI().crate.get(crates.get().x(), crates.get().y(), this);
				crate.dispose();
				crates.inc();
			}
			
			for (RESOURCE res : RESOURCES.ALL()) {
				if (this.amountTotal[res.bIndex()] != 0)
					GAME.Notify(res.name + " "+ this.amountTotal[res.bIndex()]);
				if (this.amountUnreserved[res.bIndex()] != 0)
					GAME.Notify(res.name + " "+ this.amountTotal[res.bIndex()]);
			}
		}
		
		@Override
		public StorageyardRoomBlueprintInstance blueprintI() {
			return (StorageyardRoomBlueprintInstance) blueprint();
		}
		
		@Override
		public RESOURCE_TILE resourceTile(int tx, int ty) {
			return blueprintI().crate.get(tx, ty, this);
		}
		
		@Override
		public TILE_STORAGE storage(int tx, int ty) {
			return blueprintI().crate.get(tx, ty, SETT.ROOMS().STOCKPILE.get(tx, ty));
		}
		
		public double getUsedSpace() {
			double d = 0;
			double c = 0;
			for (int i = 0; i < RESOURCES.ALL().size(); i++) {
				d += (double)amountTotal[i];
				c += allocated[i];
			}
			if (c == 0)
				return 0;
			return d/(c*StorageyardRoomBlueprintInstance.CRATE_MAX);
		}

		@Override
		protected void activateAction() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void deactivateAction() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int radius() {
			return (radius & 0x0FF)*16;
		}

		@Override
		public boolean searching() {
			return searchStatus != 2;
		}
		
		public StorageyardRoomInstance emptyTo() {
			if (emptyTo == null || !emptyTo.exists()) {
				emptyTo = null;
			}
			return emptyTo;
		}
		
		public void emptyTo(StorageyardRoomInstance ins) {
			emptyTo = ins;
		}
		
		public long getMask() {
			return fetchMaskBig;
		}
		
		public long hasMask() {
			return fetchMaskBig;
		}

		@Override
		public int deliverCapacity() {
			return StorageyardRoomBlueprintInstance.CRATE_MAX;
		}

		@Override
		public TILE_STORAGE getDeliveryCrate(long okMask, int minAmount) {
			if ((fetchMask & okMask) == 0)
				return null;
			
			for (int i = 0; i < crates.size(); i++) {
				crates.inc();
				TILE_STORAGE s = blueprintI().crate.get(crates.get().x(), crates.get().y(), this);
				if (s.resource() != null && (s.resource().bit & okMask) != 0 && s.storageReservable() >= minAmount) {
					return s;
				}
			}
			
			
			return null;
		}
		
		@Override
		public RoomState makeState(int tx, int ty) {
			return new State(this);
		}
		
		private static class State extends RoomStateInstance {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private final short[] crates = new short[RESOURCES.ALL().size()];
			private boolean[] fetch = new boolean[RESOURCES.ALL().size()];
			
			public State(StorageyardRoomInstance ins) {
				super(ins);
				for (RESOURCE r : RESOURCES.ALL()) {
					crates[r.index()] = ins.allocated[r.index()];
					fetch[r.index()] = ins.getsMaximum(r);
				}
			}
			
			@Override
			public void applyIns(RoomInstance ins) {
				if (ins instanceof StorageyardRoomInstance) {
					
					StorageyardRoomInstance s = (StorageyardRoomInstance) ins;
					for (int ri = 0; ri < RESOURCES.ALL().size(); ri++) {
						s.allocateCrate(RESOURCES.ALL().get(ri), crates[ri]);
						s.setGetMaximum(RESOURCES.ALL().get(ri), fetch[ri]);
					}
					
					
				}
				
			}
			
			
		}

		@Override
		public BOOSTABLE carryBonus() {
			return blueprintI().bonus;
		}

	}