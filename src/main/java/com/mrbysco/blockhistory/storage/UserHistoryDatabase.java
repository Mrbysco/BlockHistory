package com.mrbysco.blockhistory.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mrbysco.blockhistory.BlockHistory;
import com.mrbysco.blockhistory.config.HistoryConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UserHistoryDatabase {
	private static SqlJetDb database;
	private static ISqlJetTable storageTable;
	private static final Long2ObjectOpenHashMap<ArrayList<String>> storage = new Long2ObjectOpenHashMap<>();

	public static void init() throws SqlJetException {
		File file = new File(BlockHistory.personalFolder, "/UserHistory.sqlite");
		boolean exists = file.exists();
		database = SqlJetDb.open(file, true);

		BlockHistory.LOGGER.info("Initializing Block History database");
		if (!exists) {
			BlockHistory.LOGGER.info("Creating database as it doesn't yet exist");
			database.getOptions().setAutovacuum(true);

			String query = "CREATE TABLE `storage` ( "
					+ " `blockpos` INTEGER NOT NULL PRIMARY KEY , "
					+ " `data` TEXT NOT NULL ) ";

			database.createTable(query);
			storageTable = database.getTable("storage");
		}

		storageTable = database.getTable("storage");

		BlockHistory.LOGGER.info("Initializing internal map from database");
		database.beginTransaction(SqlJetTransactionMode.READ_ONLY);
		ISqlJetCursor cursor;
		cursor = storageTable.open();
		while (!cursor.eof()) {
			Gson gson = new Gson();
			Type type = new TypeToken<ArrayList<String>>() {
			}.getType();
			String data = cursor.getString("data");
			ArrayList<String> changes = new ArrayList<>();
			if (!data.isEmpty()) {
				changes = gson.fromJson(data, type);
			}
			storage.put(cursor.getInteger("blockpos"), changes);
			cursor.next();
		}
		cursor.close();
		database.commit();
	}

	public static boolean historyStored(long position) {
		return storage.containsKey(position);
	}

	public static void addHistory(long position, ChangeStorage changes) {
		try {
//            BlockHistory.LOGGER.info(String.format("Block at position %s was %s by %s", BlockPos.fromLong(position), changes.change, changes.username));
			Gson gson = new Gson();
			//Check if the position is already in the database and add it if it isn't
			if (!historyStored(position)) {
				String changeData = gson.toJson(changes);
				ArrayList<String> changeList = new ArrayList<>(Collections.singletonList(changeData));
				storage.put(position, changeList);
				storageTable.insert(position, gson.toJson(changeList));
			} else {
				//If it is in the database, add the change to the existing list
				database.beginTransaction(SqlJetTransactionMode.WRITE);
				ISqlJetCursor updateCursor = storageTable.lookup(storageTable.getPrimaryKeyIndexName(), position);
				while (!updateCursor.eof()) {
					long foundPosition = updateCursor.getInteger("blockpos");
					if (foundPosition == position) {
						ArrayList<String> rawChangeStorage = new ArrayList<>(getRawHistory(position));
						String changeData = gson.toJson(changes);
						if (!changeData.isEmpty() && !rawChangeStorage.contains(changeData)) {
							int maxStorage = HistoryConfig.SERVER.maxHistoryPerBlock.get();
							if (rawChangeStorage.size() == maxStorage) {
								rawChangeStorage = new ArrayList<>(rawChangeStorage.subList(rawChangeStorage.size() - (maxStorage - 1), rawChangeStorage.size()));
							}
							rawChangeStorage.add(changeData);
						}
						storage.put(position, rawChangeStorage);
						updateCursor.update(position, gson.toJson(rawChangeStorage));
						break;
					}
					updateCursor.next();
				}
				updateCursor.close();
				database.commit();
			}
		} catch (SqlJetException e) {
			BlockHistory.LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Adds multiple changes to the database at once to prevent multiple transactions
	 *
	 * @param changeMap A map of positions and changes to add
	 */
	public static void bulkAddHistory(Map<Long, ChangeStorage> changeMap) {
		try {
			Gson gson = new Gson();
			database.beginTransaction(SqlJetTransactionMode.WRITE);
			for (Map.Entry<Long, ChangeStorage> entry : changeMap.entrySet()) {
				long position = entry.getKey();
				ChangeStorage changes = entry.getValue();
//				BlockHistory.LOGGER.info(String.format("Block at position %s was %s by %s", BlockPos.of(position), changes.change, changes.username));
				//Check if the position is already in the database and add it if it isn't
				if (!historyStored(position)) {
					String changeData = gson.toJson(changes);
					ArrayList<String> changeList = new ArrayList<>(Collections.singletonList(changeData));
					storage.put(position, changeList);
					storageTable.insert(position, gson.toJson(changeList));
				} else {
					//If it is in the database, add the change to the existing list
					ISqlJetCursor updateCursor = storageTable.lookup(storageTable.getPrimaryKeyIndexName(), position);
					while (!updateCursor.eof()) {
						long foundPosition = updateCursor.getInteger("blockpos");
						if (foundPosition == position) {
							ArrayList<String> rawChangeStorage = new ArrayList<>(getRawHistory(position));
							String changeData = gson.toJson(changes);
							if (!changeData.isEmpty() && !rawChangeStorage.contains(changeData)) {
								int maxStorage = HistoryConfig.SERVER.maxHistoryPerBlock.get();
								if (rawChangeStorage.size() == maxStorage) {
									rawChangeStorage = new ArrayList<>(rawChangeStorage.subList(rawChangeStorage.size() - (maxStorage - 1), rawChangeStorage.size()));
								}
								rawChangeStorage.add(changeData);
							}
							storage.put(position, rawChangeStorage);
							updateCursor.update(position, gson.toJson(rawChangeStorage));
							break;
						}
						updateCursor.next();
					}
					updateCursor.close();
				}
			}
			database.commit();
		} catch (SqlJetException e) {
			BlockHistory.LOGGER.error(e.getMessage());
		}
	}

	public static List<ChangeStorage> getHistory(long position) {
		List<ChangeStorage> changeDataList = new ArrayList<>();
		if (historyStored(position)) {
			List<String> rawChangeData = getRawHistory(position);
			Gson gson = new Gson();
			if (!rawChangeData.isEmpty()) {
				for (String rawChangeDatum : rawChangeData) {
					changeDataList.add(gson.fromJson(rawChangeDatum, ChangeStorage.class));
				}
				changeDataList.sort(Comparator.comparing(d -> d.date));
			}
		}
		return changeDataList;
	}

	public static ArrayList<String> getRawHistory(long position) {
		ArrayList<String> rawChangeData = new ArrayList<>();
		if (historyStored(position)) {
			rawChangeData = new ArrayList<>(storage.get(position));
		}
		return rawChangeData;
	}

	public static void removeHistory(int days) {
		if (days > 0) {
			try {
				final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

				//Iterate over the database and remove all entries that are older than the set amount of days
				database.beginTransaction(SqlJetTransactionMode.WRITE);
				ISqlJetCursor cursor = storageTable.open();
				while (!cursor.eof()) {
					long position = cursor.getInteger("blockpos");
					List<String> rawChangeData = getRawHistory(position);
					if (!rawChangeData.isEmpty()) {
						List<String> removeList = new ArrayList<>();
						for (String rawChangeDatum : rawChangeData) {
							Gson gson = new Gson();
							ChangeStorage storage = gson.fromJson(rawChangeDatum, ChangeStorage.class);
							try {
								Date changeDate = dateFormat.parse(storage.date);
								Date date = Calendar.getInstance().getTime();
								//Check if the changeDate is older than 7 days
								if (date.getTime() - changeDate.getTime() > (long) days * 24 * 60 * 60 * 1000) {
									BlockHistory.LOGGER.error("Removing data for block at position " + position + " as it's older than " + days + " days");
									removeList.add(rawChangeDatum);
								}
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						if (!removeList.isEmpty()) {
							rawChangeData.removeAll(removeList);
							if (rawChangeData.isEmpty()) {
								storage.remove(position);
								cursor.delete();
							} else {
								storage.put(position, new ArrayList<>(rawChangeData));
								storageTable.insert(position, new Gson().toJson(rawChangeData));
							}
						}
					}
					cursor.next();
				}
				cursor.close();
				database.commit();
			} catch (SqlJetException e) {
				e.printStackTrace();
			}
		}
	}
}