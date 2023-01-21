package jackthestripper.songsofsyx_storageyard;


import init.D;
import init.resources.Minable;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.sprite.ICON;
import init.sprite.SPRITES;
import init.sprite.UI.UI;
import settlement.main.SETT;
import settlement.room.main.RoomInstance;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.color.OPACITY;
import snake2d.util.datatypes.DIR;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.LinkedList;
import snake2d.util.sprite.SPRITE;
import snake2d.util.sprite.text.Str;
import util.colors.GCOLOR;
import util.data.GETTER;
import util.data.INT;
import util.data.INT.INTE;
import util.dic.DicRes;
import util.gui.misc.*;
import util.gui.misc.GMeter.GGaugeColor;
import util.gui.slider.GSliderInt;
import util.gui.table.GScrollRows;
import util.info.GFORMAT;
import view.interrupter.ISidePanel;
import view.main.VIEW;
import view.sett.ui.room.UIRoomModule.UIRoomModuleImp;
import view.tool.PLACABLE;
import view.tool.PlacableSingle;

class StorageyardGui extends UIRoomModuleImp<StorageyardRoomInstance, StorageyardRoomBlueprintInstance> {
	
	private static CharSequence ¤¤Fetch = "Fetch";
	private static CharSequence ¤¤FetchD = "If fetch is toggled, deliverymen will fetch this resource from other stockpiles that have not enabled fetch for this resource.";
	private static CharSequence ¤¤emptyTo = "Empty To:";
	private static CharSequence ¤¤Choose = "Choose a different Warehouse that is not currently emptying to this one:";
	private static CharSequence ¤¤emptyToDesc = "If there is nothing to collect within the radius, the workers will start delivering non-fetching resources to this other stockpile.";
	private static CharSequence ¤¤capacityD = "Used Capacity";
	private static CharSequence ¤¤Crates = "Crates";
	private static CharSequence ¤¤allocatedCrates = "Allocated Crates/Total crates";
	
	private static ArrayList<RESOURCE> resList;
	
	
	static {
		D.ts(StorageyardGui.class);
	}
	
	StorageyardGui(StorageyardRoomBlueprintInstance s) {
		super(s); 
		resList = generateResourceList();
	}


	@Override
	protected void appendPanel(GuiSection section, GGrid grid, GETTER<StorageyardRoomInstance> g, int x1, int y1) {
		
		RENDEROBJ r = null;
		
		r = new GStat() {

			@Override
			public void update(GText text) {
				GFORMAT.percInv(text, g.get().getUsedSpace());
				
			}
		}.hh(DicRes.¤¤Capacity).hoverInfoSet(¤¤capacityD);
		grid.add(r);
		
		
		r = new GStat() {

			@Override
			public void update(GText text) {
				GFORMAT.iofk(text, g.get().usedCrates(), g.get().totalCrates());
				
			}
		}.hh(¤¤Crates).hoverInfoSet(¤¤allocatedCrates);
		grid.add(r);
		
		GuiSection s = new GuiSection();
		s.body().setWidth(260);
		s.hoverInfoSet(¤¤emptyToDesc);
		s.add(new GHeader(¤¤emptyTo));
		
		PLACABLE p = new PlacableSingle(¤¤emptyTo) {
			
			@Override
			public void placeFirst(int tx, int ty) {
				g.get().emptyTo(blueprint.get(tx, ty));
				VIEW.s().tools.place(null);
			}
			
			@Override
			public CharSequence isPlacable(int tx, int ty) {
				if (blueprint.is(tx, ty) && blueprint.get(tx, ty) != g.get() && blueprint.get(tx, ty).emptyTo() != g.get())
					return null;
				return ¤¤Choose;
			}
			
			@Override
			public boolean expandsTo(int fromX, int fromY, int toX, int toY) {
				if (isPlacable(fromX,fromY) == null && SETT.ROOMS().map.get(fromX, fromY) == SETT.ROOMS().map.get(toX, toY))
					return true;
				return false;
			}
		};
		
		CLICKABLE pick = new GButt.ButtPanel(SPRITES.icons().m.crossair) {
			@Override
			protected void clickA() {
				if (g.get().emptyTo() == null)
					VIEW.s().tools.place(p);
				else
					g.get().emptyTo(null);
			}
			
			@Override
			protected void renAction() {
				if (g.get().emptyTo() == null) {
					replaceLabel(SPRITES.icons().m.crossair, DIR.C);
				}else
					replaceLabel(SPRITES.icons().m.cancel, DIR.C);
			}
			
		};

		
		s.addRightC(8, pick);
		
		CLICKABLE name = new CLICKABLE.ClickableAbs(200, UI.FONT().M.height()) {
			
			@Override
			protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected, boolean isHovered) {
				if (g.get().emptyTo() != null) {
					Str.TMP.clear().add(g.get().emptyTo().name());
					UI.FONT().S.render(r, Str.TMP, body().x1(), body().y1());
				}
			}
			
			@Override
			protected void clickA() {
				if (g.get().emptyTo() != null)
					VIEW.s().getWindow().centererTile.set(g.get().emptyTo().body().cX(), g.get().emptyTo().body().cY());
			}
		};
		
		s.addRightC(8, name);
		
		section.addRelBody(4, DIR.S, s);
		
		
		makeTable2(g, section, section.getLastY2()+20);
		
	}

	
	private void makeTable2(GETTER<StorageyardRoomInstance> g, GuiSection section, int y1) {
		
		LinkedList<RENDEROBJ> rows = new LinkedList<>();
		int rr = 2;
		int r = 0;
		
		GuiSection ss = new GuiSection();
		rows.add(ss);
		int ci = RESOURCES.ALL().get(0).category;
		for (RESOURCE res: resList){
			if (res.category != ci) {
				rows.add(new RENDEROBJ.RenderDummy(10, 26));
				ci = res.category;
				r = 0;
				ss = new GuiSection();
				rows.add(ss);
				
			}
			if (r >= rr) {
				r = 0;
				ss = new GuiSection();
				rows.add(ss);
			}
			ss.addRightC(0, new Res2(res, g));
			r++;
		}
		
		section.addRelBody(6, DIR.S, new GScrollRows(rows, ISidePanel.HEIGHT-y1-8).view());
	}
	
	@Override
	protected void appendTableButt(GuiSection s, GETTER<RoomInstance> ins) {

		s.add(new SPRITE.Imp(s.body().width(), ICON.MEDIUM.SIZE) {

			@Override
			public void render(SPRITE_RENDERER r, int X1, int X2, int Y1, int Y2) {
				StorageyardRoomInstance in = (StorageyardRoomInstance) ins.get();
				
				double t = in.getUsedSpace();
				GMeter.render(r, GMeter.C_BLUE, t, X1, X2, Y1, Y2);
				
				int x = X1+2;
				
				for (RESOURCE res : resList) {
					if (in.cratesGet(res) > 0) {
						res.icon().small.render(r, x, Y1+4);
						x += res.icon().small.size();
						if (x >= X2-res.icon().small.size())
							break;
						
					}
				}
				
			}
		}, 0, s.body().y2());

	}
	
	@Override
	protected void hover(GBox box, StorageyardRoomInstance i) {
		super.hover(box, i);
		box.NL(4);
		int m = 0;
		for (RESOURCE r : resList) {
			
			if (i.cratesGet(r) > 0) {
				
				box.tab((m%3)*5);
				
				box.add(r.icon());
				box.add(GFORMAT.iofkInv(box.text(), i.amountGet(r), i.storageGet(r)));
				if (i.fetchesFromEveryone(r))
					box.add(SPRITES.icons().s.crossheir);
				if (m % 3 == 2) {
					box.NL();
				}
				m++;
				
			}
			
			
			
			
		}
		
	}
	
	protected ArrayList<RESOURCE> generateResourceList() {
		ArrayList<RESOURCE> tmpList = new ArrayList<RESOURCE>(100) {{
			add(RESOURCES.WOOD());
			add(RESOURCES.STONE());
			for (Minable r : RESOURCES.minables().all()) {
				add(r.resource);
			};
		}};	
		/*tmpList.add(RESOURCES.WOOD());
		tmpList.add(RESOURCES.STONE());
		for (Minable r : RESOURCES.minables().all()) {
			tmpList.add(r.resource);
		}*/
		return tmpList;
	}

	private static class Res2 extends GuiSection {
		
		private final RESOURCE res;
		private final INTE crates;
		private final INT stored;
		private final GETTER<StorageyardRoomInstance> g;
		
		Res2(RESOURCE res, GETTER<StorageyardRoomInstance> g){
			add(res.icon(),0 ,0);
			this.g = g;
			stored = new INT() {
				
				@Override
				public int min() {
					return 0;
				}
				
				@Override
				public int max() {
					return crates.get()*StorageyardRoomBlueprintInstance.CRATE_MAX;
				}
				
				@Override
				public int get() {
					return g.get().amountTotal[res.index()];
				}
			};
			
			crates = new INTE() {
				
				@Override
				public int get() {
					return g.get().allocated[res.index()];
				}

				@Override
				public int min() {
					return 0;
				}

				@Override
				public int max() {
					return g.get().totalCrates();
					
				}

				@Override
				public void set(int t) {
					int m = 0;
					for (int i = 0; i < resList.size(); i++) {
						if (i == res.index())
							continue;
						m+= g.get().allocated[i];
						
					}
					if (m + t > g.get().totalCrates()) {
						t = g.get().totalCrates() - m;
					}
					g.get().allocateCrate(res, t);
				}
			};
			
			GSliderInt gg = new GSliderInt(crates, 160, 24, true) {
				
				@Override
				protected void renderMidColor(SPRITE_RENDERER r, int x1, int width, int widthFull, int y1, int y2) {
					double a = g.get().amountTotal[res.index()];
					double c = crates.get()*StorageyardRoomBlueprintInstance.CRATE_MAX;
					double d = 0;
					if (c > 0)
						d = a/c;
					GGaugeColor col = GMeter.C_INACTIVE;
					if (d > 0.9)
						col= GMeter.C_REDPURPLE;
					else if (c > 0)
						col= GMeter.C_REDGREEN;
					else
						GMeter.render(r, GMeter.C_INACTIVE, d, body());
					
					col.bg.render(r, x1, x1+width, y1, y2);
					
					col.dark.render(r, x1, (int) (x1+width*d), y1, y2);
					col.bright.render(r, x1, (int) (x1+width*d), y1+1, y2-1);
					
				}
				
				@Override
				public void hoverInfoGet(GUI_BOX text) {
					
				}
				
			};
			addRightC(4, gg);
			
			GStat s = new GStat() {
				@Override
				public void update(GText text) {
					GFORMAT.i(text, g.get().amountTotal[res.index()]);
				}
				
				@Override
				public void render(SPRITE_RENDERER r, int X1, int X2, int Y1, int Y2) {
					if (hoveredIs() || crates.get() == 0)
						return;
					OPACITY.O50.bind();
					COLOR.BLACK.render(r, X1-2, X2+2, Y1-1, Y2+2);
					OPACITY.unbind();
					super.render(r, X1, X2, Y1, Y2);
				}
			};
			
			addCentredY(s, getLastX1()+38);
			
			CLICKABLE f = new GButt.ButtPanel(SPRITES.icons().s.speed) {
				
				@Override
				protected void renAction() {
					selectedSet(g.get().getsMaximum(res));
				}
				
				@Override
				protected void clickA() {
					g.get().setGetMaximum(res, !g.get().getsMaximum(res));
				}
			}.hoverTitleSet(¤¤Fetch).hoverInfoSet(¤¤FetchD);
			
			addRelBody(6, DIR.E, f);
			
			pad(6, 2);
			
			this.res = res;
			
		}

		@Override
		public void render(SPRITE_RENDERER r, float ds) {
			GCOLOR.UI().border().render(r, body(), -2);
			boolean hov = hoveredIs();
			super.render(r, ds);
			if (crates.get() == 0 && !hov) {
				OPACITY.O50.bind();
				COLOR.BLACK.render(r, body());
				OPACITY.unbind();
			}
		}
		
		@Override
		public void hoverInfoGet(GUI_BOX text) {
			super.hoverInfoGet(text);
			if (text.emptyIs()) {
				text.title(res.name);
				text.text(res.desc);
				GBox b = (GBox) text;
				b.NL(8);
				b.textL(¤¤Crates);
				b.tab(5);
				b.add(GFORMAT.iofk(b.text(), crates.get(), g.get().totalCrates()));
				b.NL(8);
				b.textL(DicRes.¤¤Stored);
				b.tab(5);
				b.add(GFORMAT.iofk(b.text(), stored.get(), crates.get()* StorageyardRoomBlueprintInstance.CRATE_MAX));
			}
		}
		
	}
	

}
