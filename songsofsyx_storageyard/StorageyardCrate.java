package jackthestripper.songsofsyx_storageyard;





import settlement.room.main.job.StorageCrate;

final class StorageyardCrate extends StorageCrate{

	protected final StorageyardRoomBlueprintInstance b;
	StorageyardRoomInstance ins;
	
	StorageyardCrate(StorageyardRoomBlueprintInstance b) {
		super(StorageyardRoomBlueprintInstance.CRATE_MAX);
		this.b = b;
	}

	@Override
	protected boolean is(int tx, int ty) {
		if (b.is(tx, ty)) {
			ins = b.getter.get(tx, ty);
			if (b.constructor.isCrate(tx, ty)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void count(int res, int crates, int amountTot, int amountUnres, int spaceRes) {
		ins.addCrate(res, crates, amountTot, amountUnres, spaceRes);
	}
	
	@Override
	public boolean isfetching() {
		return resource() != null && ins.getsMaximum(resource());
	}
	
	@Override
	public double spoilRate() {
		return 0.25;
	}
	

}
