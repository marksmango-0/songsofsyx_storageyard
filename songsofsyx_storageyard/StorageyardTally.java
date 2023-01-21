package jackthestripper.songsofsyx_storageyard;



import java.io.IOException;
import java.util.Arrays;

import game.time.TIME;
import init.D;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import settlement.main.SETT;
import settlement.stats.STATS;
import snake2d.util.file.*;
import util.data.DOUBLE_O;
import util.info.INFO;
import util.statistics.HistoryResource;

public final class StorageyardTally{
	
	{
		//D.gInit(this);
	}
	
	private final HistoryResource amounts = new HistoryResource(64, TIME.seasons(), true) {
		
		private final INFO info = new INFO(
			D.g("Stored"),
			D.g("StoredD", "How much is stored in your warehouses")
				);
		
		@Override
		public INFO info() {
			return info;
		};
	};
	private final HistoryResource amountDay = new HistoryResource(STATS.DAYS_SAVED, TIME.days(), true) {
		
		@Override
		public INFO info() {
			return amounts.info();
		};
	};
	private final int[] crateDesignations = new int[RESOURCES.ALL().size()];
	private final int[] amountTotal = new int[RESOURCES.ALL().size()];
	private final int[] amountReservable = new int[RESOURCES.ALL().size()];
	private final int[] amountFetch = new int[RESOURCES.ALL().size()];
//	private final int[] spaceTotal = new int[RESOURCES.ALL().size()];
//	private final int[] spaceReservable = new int[RESOURCES.ALL().size()];
	private final int[] spaceReserved = new int[RESOURCES.ALL().size()];
	private long spaceMask = 0;
	private long totalAmount = 0;
	private long totalSpace = 0;
	
	public static final DOUBLE_O<RESOURCE> usage = new DOUBLE_O<RESOURCE>() {

		@Override
		public double getD(RESOURCE t) {
			int space = (int) SETT.ROOMS().STOCKPILE.tally().spaceTotal(t);
			if (space == 0)
				return 1.0;
			double used = (int) SETT.ROOMS().STOCKPILE.tally().amountReservable(t);
			
			return used/space;
		}		
	};
	
	final SAVABLE saver = new SAVABLE() {
		
		@Override
		public void save(FilePutter file) {
			amounts.save(file);
			amountDay.save(file);
			file.is(crateDesignations);
			file.is(amountTotal);
			file.is(amountReservable);
			file.is(amountFetch);
			file.is(spaceReserved);
			file.l(spaceMask);
			file.l(totalAmount);
			file.l(totalSpace);
		}
		
		@Override
		public void load(FileGetter file) throws IOException {
			amounts.load(file);
			amountDay.load(file);
			file.is(crateDesignations);
			file.is(amountTotal);
			file.is(amountReservable);
			file.is(amountFetch);
			file.is(spaceReserved);
			spaceMask = file.l();
			totalAmount = file.l();
			totalSpace = file.l();
		}
		
		@Override
		public void clear() {
			amounts.clear();
			amountDay.clear();
			Arrays.fill(crateDesignations, 0);
			Arrays.fill(amountTotal, 0);
			Arrays.fill(amountReservable, 0);
			Arrays.fill(amountFetch, 0);
			Arrays.fill(spaceReserved, 0);
			spaceMask = 0;
			totalAmount = 0;
			totalSpace = 0;
		}
	};
	
	
	public StorageyardTally() {
		// TODO Auto-generated constructor stub
	}
	
	void tally(int res, int crates, int amountTot, int amountUnres, int spaceRes, int crateSize, boolean fetch) {
		
		crateDesignations[res] += crates;
		amountTotal[res] += amountTot;
		totalAmount += amountTot;
		amountReservable[res] += amountUnres;
		spaceReserved[res] += spaceRes;
		if (fetch) {
			amountFetch[res] += amountTot;
		}
		
		if (spaceReservable(res) == 0) {
			spaceMask &= ~RESOURCES.ALL().get(res).bit;
		}else if(spaceReservable(res) > 0) {
			spaceMask |= RESOURCES.ALL().get(res).bit;
		}else {
			debug(res);
			throw new RuntimeException("IF THIS CAN BE REPRODUCED, PLEASE SEND THE SAVE TO: info@songsofsyx.com");
		}
		
		if (amountReservable[res] == 0) {
		
		}else if(amountReservable[res] > 0) {
			
		}else {
			debug(res);
			throw new RuntimeException("" +amountReservable[res]);
		}
		
		amounts.set(RESOURCES.ALL().get(res), amountTotal(res));
		amountDay.set(RESOURCES.ALL().get(res), amountTotal(res));
		//debug(res);
		
	}
	
	void debug(int res) {
		System.err.println(RESOURCES.ALL().get(res));
		System.err.println(crateDesignations[res]);
		System.err.println(amountTotal[res]);
		System.err.println(amountReservable[res]);
		System.err.println(amountFetch[res]);
		System.err.println(spaceTotal(res));
		System.err.println(spaceReservable(res));
		System.err.println(spaceReserved[res]);
	}
	
	private long get(int[] array, int ri) {
		int am = 0;
		if (ri == -1) {
			for (int i = 0; i < RESOURCES.ALL().size(); i++) {
				am += array[i];
			}
		}else {
			am += array[ri];
		}
		return am;
	}
	
	private long get(int[] array, RESOURCE ri) {
		return get(array, ri == null ? -1 : ri.index());
	}
	
	public HistoryResource amountsSeason() {
		return amounts;
	}
	
	public HistoryResource amountsDay() {
		return amountDay;
	}
	
	public double load(RESOURCE res) {
		if (spaceTotal(res) == 0)
			return 1;
		return (double)amountTotal(res)/spaceTotal(res);
	}
	
	long crateDesignations(int res) {
		return get(crateDesignations, res);
	}

	public long crateDesignations(RESOURCE res) {
		return get(crateDesignations, res);
	}
	
	int amountTotal(int res) {
		return (int) get(amountTotal, res);
	}
	
	public int amountTotal(RESOURCE res) {
		return (int) get(amountTotal, res);
	}
	
	long amountReservable(int res) {
		return get(amountReservable, res);
	}
	
	/**
	 * Amount stored that is not set to fetch maximum
	 * @param res
	 * @return
	 */
	public long amountNGReservable(RESOURCE res) {
		return get(amountReservable, res) - get(amountFetch, res);
	}
	
	public long amountReservable(RESOURCE res) {
		return get(amountReservable, res);
	}
	
	long spaceTotal(int res) {
		return get(crateDesignations, res)*StorageyardRoomBlueprintInstance.CRATE_MAX;
	}
	
	public long spaceTotal(RESOURCE res) {
		return get(crateDesignations, res)*StorageyardRoomBlueprintInstance.CRATE_MAX;
	}
	
	long spaceReservable(int res) {
		return spaceTotal(res)-amountTotal(res)-spaceReserved(res);
	}
	
	public long spaceReservable(RESOURCE res) {
		return spaceReservable(res.bIndex());
	}
	
	public boolean spaceReservable(long mask) {
		return (spaceMask & mask) > 0;
	}
	
	long spaceReserved(int res) {
		return get(spaceReserved, res);
	}
	
	public long spaceReserved(RESOURCE res) {
		return get(spaceReserved, res);
	}
	
	public long totalSpace() {
		return totalSpace;
	}
	
	public long totalAmount() {
		return totalAmount;
	}
	
}
