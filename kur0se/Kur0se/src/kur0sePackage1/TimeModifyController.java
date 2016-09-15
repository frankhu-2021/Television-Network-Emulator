package kur0sePackage1;

import java.io.File;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import DataStructures.FileBlock;
import DataStructures.TimeBlock;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import uk.co.caprica.vlcj.component.DirectMediaPlayerComponent;

public class TimeModifyController {

	@FXML
	private Button BrowseButton;

	@FXML
	private TableView<TimeBlock> TimeBlocks;
	@FXML
	private TextField FolderPath;
	@FXML
	private CheckBox RandomBox;
	@FXML
	private TableView<FileBlock> FileTable;
	@FXML
	private TableColumn<FileBlock, String> fileCol;
	@FXML
	private TableColumn<FileBlock, String> pathCol;
	@FXML
	private TableColumn<TimeBlock, String> TimeBlockName;
	@FXML
	private Label day;
	@FXML
	private Label time;
	@FXML
	private Label currentTime;
	@FXML
	private BorderPane videoPane;

	private int trackNum = 0;
	private MediaView mediaView;
	private TimeBlock currentTimeBlockView;
	private ObservableList<FileBlock> fileList = FXCollections.observableArrayList();
	private ObservableList<TimeBlock> timeList = FXCollections.observableArrayList();

	private TimeBlock currentPlayingTimeblock;
	private HashMap<String, TimeBlock> timeBlockMap = new HashMap<>();

	private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
	Stage stage;

	public void initialize() {
		// TimeTable init
		initializeTimeList();
		initMediaPlayer();
		initFileTable();
		initFileList();
		initTimeBlocks();
		initTimer();
	}

	private void initTimeBlocks() {
		TimeBlockName.setCellValueFactory(new PropertyValueFactory<>("timeBlockName"));
		TimeBlocks.setItems(timeList);
		TimeBlocks.sortPolicyProperty().set(new Callback<TableView<TimeBlock>, Boolean>() {
			@Override
			public Boolean call(TableView<TimeBlock> param) {
				Comparator<TimeBlock> comparator = new Comparator<TimeBlock>() {
					@Override
					public int compare(TimeBlock r1, TimeBlock r2) {
						return 0;
					}
				};
				FXCollections.sort(TimeBlocks.getItems(), comparator);
				return true;
			}
		});

		TimeBlocks.setRowFactory(tv -> {
			TableRow<TimeBlock> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					TimeBlock rowData = row.getItem();
					System.out.println(rowData.getDay().toString());
					this.day.setText(rowData.getDay().toString());
					this.time.setText(rowData.getTimeInString());
					this.currentTimeBlockView = rowData;
					this.fileList.clear();
					this.fileList.addAll(this.currentTimeBlockView.getFileList());
				}
			});
			return row;
		});
	}

	private void initFileList() {

		TimeBlock rowData = timeList.get(0);
		System.out.println(rowData.getDay().toString());
		this.day.setText(rowData.getDay().toString());
		this.time.setText(rowData.getTimeInString());
		this.currentTimeBlockView = rowData;
		this.fileList.clear();
		this.fileList.addAll(this.currentTimeBlockView.getFileList());

	}

	private void initTimer() {
		Timer timer = new Timer();
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.UK)
				.withZone(ZoneId.systemDefault());
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Instant instant = Instant.now();
				String output = formatter.format(instant);
				Platform.runLater(new Runnable() {
					public void run() {
						currentTime.setText(output);
						
					}
				});
				System.out.println("asdf");
			}
		}, 0, 500);
	}

	private void nextTimeBlock() {
		int nextHour = currentPlayingTimeblock.getHour() + 1;
		int nextMinute = currentPlayingTimeblock.getMinute() + 30;
		DayOfWeek nextDay = currentPlayingTimeblock.getDay().plus(1);
		String timeInString = nextHour + ":" + nextMinute;
		currentPlayingTimeblock = timeBlockMap.get(nextDay.toString() + " | " + timeInString);
	}

	private void updateTimeBlockHashmap() {
		for (TimeBlock tb : timeList) {
			timeBlockMap.put(tb.getTimeBlockName(), tb);
		}
	}

	private void initFileTable() {
		// FileTable Init
		fileCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
		pathCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
		FileTable.setItems(fileList);
		FileTable.setEditable(true);

		FileTable.setRowFactory(tv -> {
			TableRow<FileBlock> row = new TableRow<>();
			row.setOnDragDetected(event -> {
				if (!row.isEmpty()) {
					Integer index = row.getIndex();
					Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
					db.setDragView(row.snapshot(null, null));
					ClipboardContent cc = new ClipboardContent();
					cc.put(SERIALIZED_MIME_TYPE, index);
					db.setContent(cc);
					event.consume();
				}
			});

			row.setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasContent(SERIALIZED_MIME_TYPE)) {
					if (row.getIndex() != ((Integer) db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
						event.consume();
					}
				}
			});

			row.setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasContent(SERIALIZED_MIME_TYPE)) {
					int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
					FileBlock draggedPerson = FileTable.getItems().remove(draggedIndex);

					int dropIndex;

					if (row.isEmpty()) {
						dropIndex = FileTable.getItems().size();
					} else {
						dropIndex = row.getIndex();
					}

					FileTable.getItems().add(dropIndex, draggedPerson);
					event.setDropCompleted(true);
					FileTable.getSelectionModel().select(dropIndex);
					System.out.println(fileList.get(0).getFileName());
					event.consume();

				}
			});

			return row;
		}); // FileTable Init
		fileCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
		pathCol.setCellValueFactory(new PropertyValueFactory<>("filePath"));
		FileTable.setItems(fileList);
		FileTable.setEditable(true);

		FileTable.setRowFactory(tv -> {
			TableRow<FileBlock> row = new TableRow<>();
			row.setOnDragDetected(event -> {
				if (!row.isEmpty()) {
					Integer index = row.getIndex();
					Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
					db.setDragView(row.snapshot(null, null));
					ClipboardContent cc = new ClipboardContent();
					cc.put(SERIALIZED_MIME_TYPE, index);
					db.setContent(cc);
					event.consume();
				}
			});

			row.setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasContent(SERIALIZED_MIME_TYPE)) {
					if (row.getIndex() != ((Integer) db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
						event.consume();
					}
				}
			});

			row.setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasContent(SERIALIZED_MIME_TYPE)) {
					int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
					FileBlock draggedPerson = FileTable.getItems().remove(draggedIndex);

					int dropIndex;

					if (row.isEmpty()) {
						dropIndex = FileTable.getItems().size();
					} else {
						dropIndex = row.getIndex();
					}

					FileTable.getItems().add(dropIndex, draggedPerson);
					event.setDropCompleted(true);
					FileTable.getSelectionModel().select(dropIndex);
					System.out.println(fileList.get(0).getFileName());
					event.consume();

				}
			});

			return row;
		});
	}

	@FXML
	private void handleButtonAction(ActionEvent event) {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("JavaFX Projects");
		File defaultDirectory = new File("c:/");
		chooser.setInitialDirectory(defaultDirectory);
		File selectedDirectory = chooser.showDialog(stage);
		FolderPath.setText(selectedDirectory.getAbsolutePath());
		FileTable.getItems().clear();
		for (final File fileEntry : selectedDirectory.listFiles()) {
			FileBlock block = new FileBlock(fileEntry.getName(), fileEntry.toURI().toString());
			System.out.println(block.getFileName());
			fileList.add(block);
		}
		this.currentTimeBlockView.setFileList(FXCollections.observableArrayList(fileList));
		this.updateTimeBlockHashmap();
	}

	@FXML
	private void randomizeList() {

	}

	private DirectMediaPlayerComponent mp;

	public void initMediaPlayer() {
		mediaView = new MediaView();
		videoPane.setCenter(mediaView);
		videoPane.setBottom(addToolBar());
		videoPane.setStyle("-fx-background-color: Black");
		DropShadow dropshadow = new DropShadow();
		dropshadow.setOffsetY(5.0);
		dropshadow.setOffsetX(5.0);
		dropshadow.setColor(Color.WHITE);
		mediaView.setEffect(dropshadow);
		DoubleProperty mvw = mediaView.fitWidthProperty();
		DoubleProperty mvh = mediaView.fitHeightProperty();
		mvw.bind(Bindings.selectDouble(mediaView.sceneProperty(), "width"));
		mvh.bind(Bindings.selectDouble(mediaView.sceneProperty(), "height"));
		mediaView.setPreserveRatio(true);

	}

	@FXML
	private void start() {
		Media media = new Media(currentTimeBlockView.getFileList().get(trackNum++).getFilePath());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setAutoPlay(true);
		mediaView.setMediaPlayer(mediaPlayer);
		mediaPlayer.setOnEndOfMedia(new Runnable() {
			@Override
			public void run() {
				Media media = new Media(currentTimeBlockView.getFileList().get(trackNum++).getFilePath());
				MediaPlayer mediaPlayer = new MediaPlayer(media);
				mediaPlayer.setAutoPlay(true);
				mediaView.setMediaPlayer(mediaPlayer);
			}
		});
	}

	private HBox addToolBar() {
		HBox toolBar = new HBox();
		toolBar.setPadding(new Insets(20));
		toolBar.setAlignment(Pos.CENTER);
		toolBar.alignmentProperty().isBound();
		toolBar.setSpacing(5);
		toolBar.setStyle("-fx-background-color: Black");

		return toolBar;
	}

	public void setStageAndSetupListeners(Stage primaryStage) {
		stage = primaryStage;
	}

	public void initializeTimeList() {
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.MONDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.TUESDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.WEDNESDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.THURSDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.FRIDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.SATURDAY, 23, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 0, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 0, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 1, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 1, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 2, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 2, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 3, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 3, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 4, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 4, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 5, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 5, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 6, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 6, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 7, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 7, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 8, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 8, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 9, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 9, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 10, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 10, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 11, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 11, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 12, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 12, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 13, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 13, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 14, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 14, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 15, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 15, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 16, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 16, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 17, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 17, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 18, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 18, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 19, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 19, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 20, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 20, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 21, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 21, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 22, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 22, 30));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 23, 0));
		timeList.add(new TimeBlock(DayOfWeek.SUNDAY, 23, 30));
	}
}