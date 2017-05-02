package com.wing.game.mapeditor.property;

import java.util.ArrayList;

/**
 * Map attribute
 * @author Wing Mei
 */
public class TiledMap {
	private String mapName;
    private int mapWidth,mapHeight;
    private int tileWidth,tileHeight;
    private static TiledMap mapProperty;
    private ArrayList<TileProperty> propertyList = new ArrayList<>();
    
    public static TiledMap getInstance(){
    	if(mapProperty == null){
    		mapProperty = new TiledMap();
    	}
    	return mapProperty;
    }
    
    public void setMapProperty(int tileWidth,int tileHeight,int mapWidth,int mapHeight){
    	setTileWidth(tileWidth);
    	setTileHeight(tileHeight);
    	setMapWidth(mapWidth);
    	setMapHeight(mapHeight);
    }
    
	public int getMapWidth() {
		return mapWidth;
	}
	public void setMapWidth(int mapWidth) {
		this.mapWidth = mapWidth;
	}
	public int getMapHeight() {
		return mapHeight;
	}
	public void setMapHeight(int mapHeight) {
		this.mapHeight = mapHeight;
	}
	public int getTileWidth() {
		return tileWidth;
	}
	public void setTileWidth(int tileWidth) {
		this.tileWidth = tileWidth;
	}
	public int getTileHeight() {
		return tileHeight;
	}
	public void setTileHeight(int tileHeight) {
		this.tileHeight = tileHeight;
	}
	
	public double getRealTileMapWidth(){
		return tileWidth * mapWidth;
	}
	
	public double getRealTileMapHeight(){
		return tileHeight * mapHeight;
	}

	public String getMapName() {
		return mapName;
	}
	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public ArrayList<TileProperty> getPropertyList() {
		return propertyList;
	}
	
	// Gets the properties of the specified row
	public TileProperty getProperty(int col,int row){
		TileProperty nowProperty = null;
		//Finds the properties of the cells of the specified row and column
		for(TileProperty tileProperty : propertyList){
			if(tileProperty.getRow() == row && tileProperty.getCol() == col){
				nowProperty = tileProperty;
				break;
			}else {
				nowProperty = null;
			}
		}
		return nowProperty;
	}
    
}
