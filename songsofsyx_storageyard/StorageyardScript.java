package jackthestripper.songsofsyx_storageyard;


import java.io.IOException;


import script.SCRIPT;
import settlement.room.main.RoomBlueprint;
import settlement.room.main.RoomCreator;
import settlement.room.main.util.RoomInitData;
import util.info.INFO;

public class StorageyardScript implements SCRIPT{

	/*
	 * Name and description to be displayed in the script menu at game creation
	 */
	
	private final INFO info = new INFO("storage yard", "stores building materials");
	
	public StorageyardScript(){
		
	}
	
	@Override
	public CharSequence name() {
		return info.name;
	}

	@Override
	public CharSequence desc() {
		return info.desc;
	}

	/*
	 * Required to actually create the building
	 */
	
	@Override
	public void initBeforeGameCreated() {
		new RoomCreator() {
			
			@Override
			public RoomBlueprint createBlueprint(RoomInitData init) throws IOException {
				return new StorageyardRoomBlueprintInstance(init);
			}
		};
	}
	
	@Override
	public SCRIPT_INSTANCE initAfterGameCreated() {
		return new StorageyardScriptInstance();
	}




}
