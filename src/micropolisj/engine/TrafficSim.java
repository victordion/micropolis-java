package micropolisj.engine;

import java.math.*;
import java.util.HashMap;

/**
 * 
 * @author schwtony
 * Simluates traffic stuff
 */
public class TrafficSim {
	Micropolis engine;
	HashMap<Integer,SpecifiedTile> unready;
	HashMap<CityLocation,SpecifiedTile> ready;
	
	
	
	
	public TrafficSim(){
		this(new Micropolis());
	}
	
	public TrafficSim(Micropolis city){
		engine = city;
	}
	
	public void findEnd(CityLocation startpos){
		// determines the end of a way starting at a given field
		
	}
	
	
	public int findWay(CityLocation startpos, CityLocation endpos){
		// finds way from A to B, if exists
		// in general, does A*-algorithm
		return 0;
	}
	
	
	
	public void findPeriphereRoad(CityLocation pos){
		// finds out if there are streets next to our field
		// and writes them into the HashMap "ready"
		char tiletype;
		tiletype=engine.getTile(pos.x, pos.y);
		int dimension; //height (and so width) of the tile
		if(tiletype==716){       //if tiletype==AIRPORT -> see TileConstants
			dimension=4;
		}else{
			dimension=3;
		}
		if (dimension==3){
			if (engine.getTile(pos.x-2, pos.y-1)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x-2,pos.y-1),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x-2, pos.y)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x-2,pos.y),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x-2, pos.y+1)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x-2,pos.y+1),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x-1, pos.y-2)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x-1,pos.y-2),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x-1, pos.y+2)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x-1,pos.y+2),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x, pos.y-2)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x,pos.y-2),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x, pos.y+2)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x,pos.y+2),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x+1, pos.y-2)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x+1,pos.y-2),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x+1, pos.y+2)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x+1,pos.y+2),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x+2, pos.y-1)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x+2,pos.y-1),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x+2, pos.y)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x+2,pos.y),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
			if (engine.getTile(pos.x+2, pos.y+1)==1){  //1 muss ersetzt werden durch alle möglichen Straßen- und Schienenteile!!!
				ready.put(new CityLocation(pos.x+2,pos.y+1),new SpecifiedTile(new CityLocation(pos.x-2,pos.y-2),true));
			}
		}
		if(dimension==4){
			// Abfrage von oben mit einbauen!
		}
	}
	
	public void makeTraffic(CityLocation pos, int value){
		// increases the traffic on a given tile by value
	}
	
	public static int evalfunc(CityLocation start, CityLocation finish){
		return Math.abs(start.x-finish.x)+Math.abs(start.y-finish.y)+1;
	}
	
	
	
	
	
}