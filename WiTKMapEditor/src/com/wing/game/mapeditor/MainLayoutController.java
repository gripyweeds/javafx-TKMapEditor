package com.wing.game.mapeditor;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ListView.EditEvent;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.wing.game.mapeditor.dialog.AboutDialog;
import com.wing.game.mapeditor.dialog.AlertDialog;
import com.wing.game.mapeditor.dialog.NewMapDialog;
import com.wing.game.mapeditor.dialog.NewMapDialog.OnNewMapDialogActionListener;
import com.wing.game.mapeditor.draw.AltasCanvas;
import com.wing.game.mapeditor.draw.MapCanvas;
import com.wing.game.mapeditor.io.Config;
import com.wing.game.mapeditor.io.XMLElements;
import com.wing.game.mapeditor.layer.TiledMapLayer;
import com.wing.game.mapeditor.property.AltasResourceManager;
import com.wing.game.mapeditor.property.TileProperty;
import com.wing.game.mapeditor.property.AltasResourceManager.AltasResource;
import com.wing.game.mapeditor.property.TiledMap;

/**
 * 主界面事件处理
 * 
 * @author Wing Mei
 */
public class MainLayoutController implements Initializable {
	@FXML
	private TextField importImagePathTf;
	@FXML
	private TextField importImageWidthTf, importImageHeightTf, importImageSizeTf;
	@FXML
	private ListView<String> layerListView;
	private FileChooser fileChooser;
	@FXML
	private Menu mRecentMenu;
	@FXML
	private Button browserImportBtn, addToImageBtn;
	@FXML
	private ToolBar layerToolbar;
	@FXML
	private Slider layerAlphaSlider;
	@FXML
	private Slider scaleSlider;
	@FXML
	private Label mScaleLabel;
	@FXML
	private CheckBox layerShowCheck, layerColliderCheck;
	@FXML
	private Label mapSizeLabel, nowMousePositionLabel;

	@FXML
	private ScrollPane altasCanvasScrollPane;
	@FXML
	private ScrollPane mapScrollPane;
	@FXML
	private ListView<String> altasListView;
	@FXML
	private RadioMenuItem normalBrushItem, paintPailItem, eraserItem, rectItem;
	@FXML
	private CheckMenuItem showMapGridItem, showAltasGridItem, showPropertyGridItem;
	// private int altasOffsetX = 0;
	// private int altasOffsetY = 0;

	private ObservableList<String> layerList = FXCollections.observableArrayList();
	private ObservableList<String> imagePathList = FXCollections.observableArrayList();
	private Image nowBrowserImage;
	private List<TiledMapLayer> tiledMapLayerList = new ArrayList<>();

	private AltasCanvas altasCanvas;
	private MapCanvas mapCanvas;
	private SimpleStringProperty nowSelectAltasIdProperty = new SimpleStringProperty();
	private SimpleIntegerProperty brushTypeProperty = new SimpleIntegerProperty();

	private FileChooser openMapChooser;
	private FileChooser saveAsFileChooser;
	private FileChooser exportFileChooser;

	private NewMapDialog newMapDialog;

	private SAXReader saxReader = new SAXReader();
	// Open the map is wrong
	private boolean isReadError = false;
	// Whether to open or create a new map
	private boolean isNewOrOpenMap = false;
	// Read the information returned by the map
	private List<String> readMsgs = new ArrayList<>();
	// Draw thread sleep time
	private long threadSleep = 50;

	private File openMapFile;

	private Thread drawThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (isRunning) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						if (altasCanvas != null) {
							altasCanvas.draw();
						}
						if (mapCanvas != null) {
							mapCanvas.draw();
							if (isNewOrOpenMap && layerList.size() > 0)
								nowMousePositionLabel.setText(mapCanvas.getMouseCols() + " , "
										+ mapCanvas.getMouseRows());
						}
					}
				});
				try {
					Thread.sleep(threadSleep);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});
	public static boolean isRunning = true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		TiledMap.getInstance().setMapProperty(64, 64, 13, 7);

		// file selector
		fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new ExtensionFilter("图片文件", "*.jpg", "*.png", "*.bmp"));
		altasCanvas = new AltasCanvas(altasCanvasScrollPane.getWidth(), altasCanvasScrollPane.getHeight());
		altasCanvas.BrushTypeProperty().bind(brushTypeProperty);
		// Open the map
		openMapChooser = new FileChooser();
		openMapChooser.getExtensionFilters().add(new ExtensionFilter("地图文件", "*.xml"));

		saveAsFileChooser = new FileChooser();
		saveAsFileChooser.getExtensionFilters().add(new ExtensionFilter("XML文件", "*.xml"));

		exportFileChooser = new FileChooser();
		exportFileChooser.getExtensionFilters().add(new ExtensionFilter("图片文件", "*.png"));

		// Artwork drawing
		altasCanvas.widthProperty().bind(altasCanvasScrollPane.widthProperty());
		altasCanvas.heightProperty().bind(altasCanvasScrollPane.heightProperty());
		altasCanvasScrollPane.setContent(altasCanvas);

		// Map drawing
		mapCanvas = new MapCanvas(TiledMap.getInstance().getMapWidth(), TiledMap.getInstance().getMapHeight());
		mapCanvas.NowSelectLayerProperty().bind(layerListView.getSelectionModel().selectedIndexProperty());
		mapCanvas.BrushTypeProperty().bind(brushTypeProperty);
		mapCanvas.setMapLayerList(tiledMapLayerList);
		mapCanvas.NowChooseProperty().bind(altasCanvas.NowChooseProperty());
		// mapCanvas.ScaleProperty().bind(scaleSlider.valueProperty());
		scaleSlider.valueProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				String value = newValue.toString().subSequence(0, 3).toString();
				mScaleLabel.setText(value);
				mapCanvas.setScale(newValue.doubleValue());
				double width = TiledMap.getInstance().getRealTileMapWidth() * mapCanvas.getScale();
				double height = TiledMap.getInstance().getRealTileMapHeight() * mapCanvas.getScale();
				mapCanvas.setWidth(width);
				mapCanvas.setHeight(height);
			}
		});
		mapScrollPane.setContent(mapCanvas);
		drawThread.start();

		// Layer list
		layerListView.setItems(layerList);
		layerListView.setEditable(true);
		layerListView.setCellFactory(TextFieldListCell.forListView());
		layerListView.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {

			@Override
			public void handle(EditEvent<String> event) {
				layerList.set(event.getIndex(), event.getNewValue());
				tiledMapLayerList.get(event.getIndex()).setLayerName(event.getNewValue());
			}
		});
		layerListView.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				int index = newValue.intValue();
				if (index >= 0 && index < tiledMapLayerList.size()) {
					TiledMapLayer mapLayer = tiledMapLayerList.get(index);
					layerAlphaSlider.setValue(mapLayer.getAlpha());
					layerShowCheck.setSelected(mapLayer.isVisible());
					layerColliderCheck.setSelected(mapLayer.isCollider());
				}
			}
		});
		// Modify the alpha value of the layer
		layerAlphaSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				int index = layerListView.getSelectionModel().selectedIndexProperty().get();
				if (index >= 0 && index < tiledMapLayerList.size()) {
					TiledMapLayer mapLayer = tiledMapLayerList.get(index);
					mapLayer.setAlpha(newValue.doubleValue());
				}
			}
		});

		layerShowCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				int index = layerListView.getSelectionModel().selectedIndexProperty().get();
				if (index >= 0) {
					TiledMapLayer mapLayer = tiledMapLayerList.get(index);
					mapLayer.setVisible(newValue);
				}
			}
		});

		layerColliderCheck.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				int index = layerListView.getSelectionModel().selectedIndexProperty().get();
				if (index >= 0) {
					TiledMapLayer mapLayer = tiledMapLayerList.get(index);
					mapLayer.setCollider(newValue);
				}
			}
		});

		// The list of stickers
		altasListView.setItems(imagePathList);
		altasListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> param) {
				return new ImageCell();
			}
		});
		altasListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				AltasResource altasResource = AltasResourceManager.getInstance().getResourceById(newValue);
				if (altasResource != null && altasResource.getImage() != null) {
					Image image = altasResource.getImage();
					altasCanvas.setImage(image);
					mapCanvas.setNowAltasResource(altasResource);
				} else {
					altasCanvas.setImage(null);
					mapCanvas.setNowAltasResource(null);
				}
			}
		});
		nowSelectAltasIdProperty.bind(altasListView.getSelectionModel().selectedItemProperty());

		// Dialog box
		newMapDialog = new NewMapDialog();
		newMapDialog.setOnNewMapDialogActionListener(new OnNewMapDialogActionListener() {

			@Override
			public void onNewMapOkAction() {
				clearAll();
				newOrOpenMap();
				openMapFile = null;
				// Set the map canvas size
				mapCanvas.setWidth(TiledMap.getInstance().getRealTileMapWidth());
				mapCanvas.setHeight(TiledMap.getInstance().getRealTileMapHeight());
				mapSizeLabel.setText(TiledMap.getInstance().getMapWidth() + " x "
						+ TiledMap.getInstance().getMapHeight());
			}

			@Override
			public void onNewMapCancelAction() {

			}
		});

		// Menu
		ToggleGroup tGroup = new ToggleGroup();
		normalBrushItem.setToggleGroup(tGroup);
		paintPailItem.setToggleGroup(tGroup);
		eraserItem.setToggleGroup(tGroup);
		rectItem.setToggleGroup(tGroup);
		normalBrushItem.setSelected(true);
		mapCanvas.ShowGridProperty().bind(showMapGridItem.selectedProperty());
		altasCanvas.ShowGridProperty().bind(showAltasGridItem.selectedProperty());
		mapCanvas.ShowProProperty().bind(showPropertyGridItem.selectedProperty());
		// Read the most recently opened file
		initRecentFiles();
	}

	private void initRecentFiles() {
		Config.getInstance().readConfig();
		ArrayList<String> paths = Config.getInstance().getFilePaths();
		for (String path : paths) {
			MenuItem item = new MenuItem(path);
			item.setOnAction(e -> {
			    File file = new File(path);
			    if(file.exists()){
				readMapWithAlert(new File(path));
			    } else {
			    	AlertDialog.showAlertDialog("The map file does not exist");
			    	mRecentMenu.getItems().remove(this);
			    	removeRecentFile(file);
			    }
			});
			mRecentMenu.getItems().add(item);
		}
	}

	private void addRecentFile(File file) {
		if (!Config.getInstance().getFilePaths().contains(file.getAbsolutePath())) {
			Config.getInstance().getFilePaths().add(file.getAbsolutePath());
			MenuItem item = new MenuItem(file.getAbsolutePath());
			item.setOnAction(e -> {
				readMapWithAlert(file);
			});
			mRecentMenu.getItems().add(item);
		}
	}
	
	private void removeRecentFile(File file) {
		if (Config.getInstance().getFilePaths().contains(file.getAbsolutePath())) {
			Config.getInstance().getFilePaths().remove(file.getAbsolutePath());
		}
	}

	/**
	 * Empty the map
	 */
	private void clearAll() {
		tiledMapLayerList.clear();
		AltasResourceManager.getInstance().removeAll();
		imagePathList.clear();
		altasCanvas.setImage(null);
		layerList.clear();
		readMsgs.clear();
		// Clear the property list when reading the map
		TiledMap.getInstance().getPropertyList().clear();
	}

	/**
	 * Create or open a map UI changes
	 */
	private void newOrOpenMap() {
		isNewOrOpenMap = true;
		browserImportBtn.setDisable(false);
		addToImageBtn.setDisable(false);
		layerToolbar.setDisable(false);
	}

	/**
	 * Read the map
	 * 
	 * @param file
	 *            Map file
	 */
	private void readMap(File file) {
		// Clear all resources
		clearAll();
		isReadError = false;
		try {
			Document document = saxReader.read(file);
			Element rootElement = document.getRootElement();
			for (Iterator<Element> i = rootElement.elementIterator(); i.hasNext();) {
				Element e = i.next();
				if (e.getName().equals(XMLElements.ELEMENT_MAP_SETTING)) {
					// Read the map information
					int mapWidth = Integer.parseInt(e.elementText(XMLElements.ELEMENT_MAP_WIDTH));
					int mapHeight = Integer.parseInt(e.elementText(XMLElements.ELEMENT_MAP_HEIGHT));
					int tileWidth = Integer.parseInt(e.elementText(XMLElements.ELEMENT_TILE_WIDTH));
					int tileHeight = Integer.parseInt(e.elementText(XMLElements.ELEMENT_TILE_HEIGHT));
					TiledMap.getInstance().setMapProperty(tileWidth, tileHeight, mapWidth, mapHeight);
					// Set the map canvas size
					mapCanvas.setWidth(tileWidth * mapWidth);
					mapCanvas.setHeight(tileHeight * mapHeight);
					mapSizeLabel.setText(TiledMap.getInstance().getMapWidth() + " x "
							+ TiledMap.getInstance().getMapHeight());
					readMsgs.add("Read map settings are successful");
				} else if (e.getName().equals(XMLElements.ELEMENT_MAP_RESOURCE)) {
					// Read the map resource,And added to the resource management
					AltasResourceManager.getInstance().removeAll();
					for (Iterator<Element> j = e.elementIterator(); j.hasNext();) {
						Element ej = j.next();
						String altasID = ej.elementText(XMLElements.ELEMENT_ALTAS_ID);
						String altasPath = ej.elementText(XMLElements.ELEMENT_ALTAS_PATH);
						String fileName = altasPath.substring(altasPath.lastIndexOf("\\") + 1);
						try {
							addImageAtlas(altasID, altasPath);
							readMsgs.add("Read the map " + fileName + " Success");
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
							isReadError = true;
							readMsgs.add("Map " + fileName + " Not found");
						}
					}
					// Add to the resource list
					List<AltasResource> alResources = AltasResourceManager.getInstance().getResources();
					for (AltasResource resource : alResources) {
						imagePathList.add(resource.getAltasId());
					}

				} else if (e.getName().equals(XMLElements.ELEMENT_MAP_DATA)) {
					// Read the layer data，And converted into map data
					for (Iterator<Element> j = e.elementIterator(); j.hasNext();) {
						TiledMapLayer tiledMapLayer = new TiledMapLayer();
						Element ej = j.next();
						String layerName = ej.attributeValue(XMLElements.ATTRIBUTE_NAME);
						String visibleStr = ej.attributeValue(XMLElements.ATTRIBUTE_VISIBLE);
						String alphaStr = ej.attributeValue(XMLElements.ATTRIBUTE_ALPHA);
						String colliderStr = ej.attributeValue(XMLElements.ATTRIBUTE_COLLIDER);
						String mapData = ej.getText();
						tiledMapLayer.setLayerName(layerName);
						if (visibleStr != null)
							tiledMapLayer.setVisible(Boolean.parseBoolean(visibleStr));
						if (alphaStr != null)
							tiledMapLayer.setAlpha(Double.parseDouble(alphaStr));
						if (colliderStr != null)
							tiledMapLayer.setCollider(Boolean.parseBoolean(colliderStr));
						tiledMapLayer.ConvertFromString(mapData);
						layerList.add(layerName);
						// Read the layers added to the list
						tiledMapLayerList.add(tiledMapLayer);
						readMsgs.add("Read the layer\"" + layerName + "\"Success");
					}
				} else if (e.getName().equals(XMLElements.ELEMENT_MAP_PROPERTY)) {
					// Read the layer data，And converted into map data
					for (Iterator<Element> j = e.elementIterator(); j.hasNext();) {
						TileProperty tileProperty = new TileProperty();
						Element ej = j.next();
						String col = ej.attributeValue(XMLElements.ATTRIBUTE_COL);
						String row = ej.attributeValue(XMLElements.ATTRIBUTE_ROW);
						tileProperty.setCol(Integer.parseInt(col));
						tileProperty.setRow(Integer.parseInt(row));

						for (Iterator<Element> oj = ej.elementIterator(); oj.hasNext();) {
							Element property = oj.next();
							String key = property.attributeValue(XMLElements.ATTRIBUTE_KEY);
							String value = property.attributeValue(XMLElements.ATTRIBUTE_VALUE);
							tileProperty.insertValue(key, value);
						}
						TiledMap.getInstance().getPropertyList().add(tileProperty);
					}
					readMsgs.add("Read the map property list successfully");
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
			isReadError = true;
			readMsgs.add("The map file is reading incorrectly " + e.getMessage());
		}
		addRecentFile(file);
	}
	
	private void readMapWithAlert(File mapFile){
		readMap(mapFile);
		String str = "";
		if (!isReadError) {
			str = "Read the map is complete:";
		} else {
			str = "An error occurred while reading the map:";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(str + System.getProperty("line.separator"));
		for (String s : readMsgs) {
			sb.append(s + System.getProperty("line.separator"));
		}
		newOrOpenMap();
		openMapFile = mapFile;
		AlertDialog.showAlertDialog(sb.toString());
	}

	@FXML
	public void onNewMapAction(ActionEvent e) {
		newMapDialog.showAlertDialog();
	}

	@FXML
	public void onOpenMapAction(ActionEvent e) {
		File mapFile = openMapChooser.showOpenDialog(null);
		if (mapFile != null) {
            readMapWithAlert(mapFile);
		}
	}

	@FXML
	public void onSaveMapAction(ActionEvent e) {
		if (openMapFile == null) {
			File file = saveAsFileChooser.showSaveDialog(null);
			openMapFile = file;
			if (file != null) {
				saveMapToFile(file);
			}
		} else {
			saveMapToFile(openMapFile);
		}
	}

	@FXML
	public void onSaveAsMapAction(ActionEvent e) {
		File file = saveAsFileChooser.showSaveDialog(null);
		addRecentFile(file);
		openMapFile = file;
		if (file != null) {
			saveMapToFile(file);
		}
	}

	@FXML
	public void onExportToImageAction(ActionEvent e) {
		File file = exportFileChooser.showSaveDialog(null);
		if (file != null) {
			WritableImage image = mapCanvas.snapshot(new SnapshotParameters(), null);
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
				AlertDialog.showAlertDialog("Saved successfully!");
			} catch (IOException ex) {
				AlertDialog.showAlertDialog("Save failed:" + ex.getMessage());
			}
		}
	}

	/**
	 * Save the map to a file
	 * 
	 * @param file
	 *            Map file
	 */
	private void saveMapToFile(File file) {
		//OutputFormat format = OutputFormat.createPrettyPrint();
		//format.setEncoding("UTF-8");
		Document map = createSaveDocument();
		XMLWriter writer;
		try {
			writer = new XMLWriter(new FileOutputStream(file));//new FileWriter(file), format);
			writer.write(map);
			writer.close();
			AlertDialog.showAlertDialog("Save the map to complete!");
		} catch (IOException e1) {
			e1.printStackTrace();
			AlertDialog.showAlertDialog("Save map error:" + e1.getMessage());
		}
	}

	@FXML
	public void onAboutAction(ActionEvent e) {
		AboutDialog.showAboutDialog();
	}

	/*
	 * Create saved map data
	 */
	private Document createSaveDocument() {
		Document document = DocumentHelper.createDocument();
		Element map = document.addElement(XMLElements.ELEMENT_MAP);

		Element mapSetting = map.addElement(XMLElements.ELEMENT_MAP_SETTING);

		Element mapWidth = mapSetting.addElement(XMLElements.ELEMENT_MAP_WIDTH);
		mapWidth.setText(TiledMap.getInstance().getMapWidth() + "");

		Element mapHeight = mapSetting.addElement(XMLElements.ELEMENT_MAP_HEIGHT);
		mapHeight.setText(TiledMap.getInstance().getMapHeight() + "");

		Element tileWidth = mapSetting.addElement(XMLElements.ELEMENT_TILE_WIDTH);
		tileWidth.setText(TiledMap.getInstance().getTileWidth() + "");

		Element tileHeight = mapSetting.addElement(XMLElements.ELEMENT_TILE_HEIGHT);
		tileHeight.setText(TiledMap.getInstance().getTileHeight() + "");

		// Write a list of resources
		Element mapResource = map.addElement(XMLElements.ELEMENT_MAP_RESOURCE);
		List<AltasResource> resources = AltasResourceManager.getInstance().getResources();
		for (int i = 0; i < resources.size(); i++) {
			AltasResource altasResource = resources.get(i);
			Element resource = mapResource.addElement(XMLElements.ELEMENT_RESOURCE);
			Element resourceId = resource.addElement(XMLElements.ELEMENT_ALTAS_ID);
			resourceId.setText(altasResource.getAltasId());
			Element resourcePath = resource.addElement(XMLElements.ELEMENT_ALTAS_PATH);
			resourcePath.setText(altasResource.getPathStr());
		}

		Element mapData = map.addElement(XMLElements.ELEMENT_MAP_DATA);
		for (int i = 0; i < tiledMapLayerList.size(); i++) {
			TiledMapLayer mapLayer = tiledMapLayerList.get(i);
			Element layer = mapData.addElement(XMLElements.ELEMENT_MAP_LAYER);
			layer.addAttribute(XMLElements.ATTRIBUTE_NAME, mapLayer.getLayerName());
			layer.addAttribute(XMLElements.ATTRIBUTE_VISIBLE, String.valueOf(mapLayer.isVisible()));
			layer.addAttribute(XMLElements.ATTRIBUTE_ALPHA, mapLayer.getAlpha() + "");
			layer.addAttribute(XMLElements.ATTRIBUTE_COLLIDER, String.valueOf(mapLayer.isCollider()));
			layer.setText(mapLayer.toString());
		}

		Element tilePropertyElement = map.addElement(XMLElements.ELEMENT_MAP_PROPERTY);
		ArrayList<TileProperty> tileProperties = TiledMap.getInstance().getPropertyList();
		for (TileProperty tileProperty : tileProperties) {
			Element data = tilePropertyElement.addElement(XMLElements.ELEMENT_PROPERTY_DATA);
			data.addAttribute(XMLElements.ATTRIBUTE_COL, String.valueOf(tileProperty.getCol()));
			data.addAttribute(XMLElements.ATTRIBUTE_ROW, String.valueOf(tileProperty.getRow()));

			HashMap<String, String> valuesMap = tileProperty.getValueMap();
			Iterator<String> keys = valuesMap.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				String value = valuesMap.get(key);
				Element propertyElement = data.addElement(XMLElements.ELEMENT_PROPERTY);
				propertyElement.addAttribute(XMLElements.ATTRIBUTE_KEY, key);
				propertyElement.addAttribute(XMLElements.ATTRIBUTE_VALUE, value);
			}
		}

		return document;
	}

	@FXML
	public void onBrowserImportImageAction(ActionEvent e) {
		File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			importImagePathTf.setText(file.getAbsolutePath());
			try {
				nowBrowserImage = new Image(new FileInputStream(file));
				importImageWidthTf.setText(nowBrowserImage.getWidth() + "");
				importImageHeightTf.setText(nowBrowserImage.getHeight() + "");
				importImageSizeTf.setText(file.length() / 1024 + "kb");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
	}

	@FXML
	public void onAddToImageAtlasAction(ActionEvent e) {
		if (!importImagePathTf.getText().equals("")) {
			String id = AltasResourceManager.createAltasId();
			String path = importImagePathTf.getText();
			try {
				addImageAtlas(id, path);
				imagePathList.add(id);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void addImageAtlas(String id, String path) throws FileNotFoundException {
		Image image = new Image(new FileInputStream(path));
		AltasResourceManager.getInstance().addResource(id, path, image);
	}

	@FXML
	public void onAddNewLayerAction(ActionEvent e) {
		String defaultName = "Create a new layer";
		layerList.add(defaultName);
		TiledMapLayer tiledMapLayer = new TiledMapLayer();
		tiledMapLayer.setLayerName(defaultName);
		tiledMapLayerList.add(tiledMapLayer);
	}

	@FXML
	public void onDeleteLayerAction(ActionEvent e) {
		int index = layerListView.getSelectionModel().getSelectedIndex();
		if (index >= 0) {
			layerList.remove(index);
			tiledMapLayerList.remove(index);
		}
	}

	@FXML
	public void onLayerUpAction(ActionEvent e) {
		int index = layerListView.getSelectionModel().getSelectedIndex();
		if (index > 0) {
			String layerStr = layerList.remove(index - 1);
			TiledMapLayer layer = tiledMapLayerList.remove(index - 1);
			layerList.add(index, layerStr);
			tiledMapLayerList.add(index, layer);
		}
	}

	@FXML
	public void onLayerDownAction(ActionEvent e) {
		int index = layerListView.getSelectionModel().getSelectedIndex();
		if (index < layerList.size() - 1) {
			String layerStr = layerList.remove(index);
			TiledMapLayer layer = tiledMapLayerList.remove(index);
			layerList.add(index + 1, layerStr);
			tiledMapLayerList.add(index + 1, layer);
		}
	}

	@FXML
	public void onNormalBrushItemAction(ActionEvent e) {
		brushTypeProperty.set(0);
	}

	@FXML
	public void onPaintPailItemAction(ActionEvent e) {
		brushTypeProperty.set(1);
	}

	@FXML
	public void onEraserItemAction(ActionEvent e) {
		brushTypeProperty.set(2);
		if (mapCanvas != null) {
			mapCanvas.NowChooseProperty().clear();
		}
	}

	@FXML
	public void onRectItemAction(ActionEvent e) {
		brushTypeProperty.set(3);
		if (mapCanvas != null) {
			mapCanvas.NowChooseProperty().clear();
		}
	}

	@FXML
	public void onDeleteResourceAction(ActionEvent e) {
		int index = altasListView.getSelectionModel().getSelectedIndex();
		imagePathList.remove(index);
		AltasResourceManager.getInstance().removeResource(index);
	}

	@FXML
	public void onAppExit(ActionEvent e) {
		Config.getInstance().saveConfig();
		System.exit(0);
		isRunning = false;
	}

	class ImageCell extends ListCell<String> {
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (item != null && !empty) {
				ImageView iView;
				Image image = AltasResourceManager.getInstance().getResourceById(item).getImage();
				iView = new ImageView(image);
				iView.setFitWidth(50);
				iView.setFitHeight(50);
				setGraphic(iView);
			} else {
				Rectangle rectangle = new Rectangle(altasListView.getWidth(), altasListView.getHeight());
				rectangle.setFill(Color.WHITE);
				setGraphic(rectangle);
			}
		}
	}
}
