package io.biotrace.app.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import io.biotrace.app.data.local.dao.ChatHistoryDao;
import io.biotrace.app.data.local.dao.ChatHistoryDao_Impl;
import io.biotrace.app.data.local.dao.LabBiomarkerDao;
import io.biotrace.app.data.local.dao.LabBiomarkerDao_Impl;
import io.biotrace.app.data.local.dao.MealDao;
import io.biotrace.app.data.local.dao.MealDao_Impl;
import io.biotrace.app.data.local.dao.VitalsDao;
import io.biotrace.app.data.local.dao.VitalsDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BioTraceDatabase_Impl extends BioTraceDatabase {
  private volatile VitalsDao _vitalsDao;

  private volatile LabBiomarkerDao _labBiomarkerDao;

  private volatile MealDao _mealDao;

  private volatile ChatHistoryDao _chatHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `vitals_records` (`id` TEXT NOT NULL, `metric_type` TEXT NOT NULL, `value` REAL NOT NULL, `unit` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vitals_records_metric_type` ON `vitals_records` (`metric_type`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vitals_records_timestamp` ON `vitals_records` (`timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_vitals_records_metric_type_timestamp` ON `vitals_records` (`metric_type`, `timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `lab_biomarkers` (`id` TEXT NOT NULL, `biomarker_name` TEXT NOT NULL, `raw_name` TEXT NOT NULL, `value` REAL NOT NULL, `unit` TEXT NOT NULL, `reference_min` REAL, `reference_max` REAL, `flag` TEXT, `recorded_at` INTEGER NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lab_biomarkers_biomarker_name` ON `lab_biomarkers` (`biomarker_name`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lab_biomarkers_recorded_at` ON `lab_biomarkers` (`recorded_at`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `meals` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `calories` INTEGER NOT NULL, `protein_grams` REAL NOT NULL, `carbs_grams` REAL NOT NULL, `fats_grams` REAL NOT NULL, `fiber_grams` REAL, `consumed_at` INTEGER NOT NULL, `photo_path` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meals_consumed_at` ON `meals` (`consumed_at`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_history` (`id` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `tool_calls` TEXT, `citations` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_history_timestamp` ON `chat_history` (`timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'c5c72cae93bd7ff789bbbb630746ed71')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `vitals_records`");
        db.execSQL("DROP TABLE IF EXISTS `lab_biomarkers`");
        db.execSQL("DROP TABLE IF EXISTS `meals`");
        db.execSQL("DROP TABLE IF EXISTS `chat_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsVitalsRecords = new HashMap<String, TableInfo.Column>(7);
        _columnsVitalsRecords.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVitalsRecords.put("metric_type", new TableInfo.Column("metric_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVitalsRecords.put("value", new TableInfo.Column("value", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVitalsRecords.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVitalsRecords.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVitalsRecords.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVitalsRecords.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVitalsRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVitalsRecords = new HashSet<TableInfo.Index>(3);
        _indicesVitalsRecords.add(new TableInfo.Index("index_vitals_records_metric_type", false, Arrays.asList("metric_type"), Arrays.asList("ASC")));
        _indicesVitalsRecords.add(new TableInfo.Index("index_vitals_records_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        _indicesVitalsRecords.add(new TableInfo.Index("index_vitals_records_metric_type_timestamp", false, Arrays.asList("metric_type", "timestamp"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoVitalsRecords = new TableInfo("vitals_records", _columnsVitalsRecords, _foreignKeysVitalsRecords, _indicesVitalsRecords);
        final TableInfo _existingVitalsRecords = TableInfo.read(db, "vitals_records");
        if (!_infoVitalsRecords.equals(_existingVitalsRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "vitals_records(io.biotrace.app.data.local.entity.VitalsRecordEntity).\n"
                  + " Expected:\n" + _infoVitalsRecords + "\n"
                  + " Found:\n" + _existingVitalsRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsLabBiomarkers = new HashMap<String, TableInfo.Column>(10);
        _columnsLabBiomarkers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("biomarker_name", new TableInfo.Column("biomarker_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("raw_name", new TableInfo.Column("raw_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("value", new TableInfo.Column("value", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("reference_min", new TableInfo.Column("reference_min", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("reference_max", new TableInfo.Column("reference_max", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("flag", new TableInfo.Column("flag", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("recorded_at", new TableInfo.Column("recorded_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLabBiomarkers.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLabBiomarkers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLabBiomarkers = new HashSet<TableInfo.Index>(2);
        _indicesLabBiomarkers.add(new TableInfo.Index("index_lab_biomarkers_biomarker_name", false, Arrays.asList("biomarker_name"), Arrays.asList("ASC")));
        _indicesLabBiomarkers.add(new TableInfo.Index("index_lab_biomarkers_recorded_at", false, Arrays.asList("recorded_at"), Arrays.asList("ASC")));
        final TableInfo _infoLabBiomarkers = new TableInfo("lab_biomarkers", _columnsLabBiomarkers, _foreignKeysLabBiomarkers, _indicesLabBiomarkers);
        final TableInfo _existingLabBiomarkers = TableInfo.read(db, "lab_biomarkers");
        if (!_infoLabBiomarkers.equals(_existingLabBiomarkers)) {
          return new RoomOpenHelper.ValidationResult(false, "lab_biomarkers(io.biotrace.app.data.local.entity.LabBiomarkerEntity).\n"
                  + " Expected:\n" + _infoLabBiomarkers + "\n"
                  + " Found:\n" + _existingLabBiomarkers);
        }
        final HashMap<String, TableInfo.Column> _columnsMeals = new HashMap<String, TableInfo.Column>(9);
        _columnsMeals.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("calories", new TableInfo.Column("calories", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("protein_grams", new TableInfo.Column("protein_grams", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("carbs_grams", new TableInfo.Column("carbs_grams", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("fats_grams", new TableInfo.Column("fats_grams", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("fiber_grams", new TableInfo.Column("fiber_grams", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("consumed_at", new TableInfo.Column("consumed_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeals.put("photo_path", new TableInfo.Column("photo_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMeals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMeals = new HashSet<TableInfo.Index>(1);
        _indicesMeals.add(new TableInfo.Index("index_meals_consumed_at", false, Arrays.asList("consumed_at"), Arrays.asList("ASC")));
        final TableInfo _infoMeals = new TableInfo("meals", _columnsMeals, _foreignKeysMeals, _indicesMeals);
        final TableInfo _existingMeals = TableInfo.read(db, "meals");
        if (!_infoMeals.equals(_existingMeals)) {
          return new RoomOpenHelper.ValidationResult(false, "meals(io.biotrace.app.data.local.entity.MealEntity).\n"
                  + " Expected:\n" + _infoMeals + "\n"
                  + " Found:\n" + _existingMeals);
        }
        final HashMap<String, TableInfo.Column> _columnsChatHistory = new HashMap<String, TableInfo.Column>(6);
        _columnsChatHistory.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatHistory.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatHistory.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatHistory.put("tool_calls", new TableInfo.Column("tool_calls", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatHistory.put("citations", new TableInfo.Column("citations", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatHistory = new HashSet<TableInfo.Index>(1);
        _indicesChatHistory.add(new TableInfo.Index("index_chat_history_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoChatHistory = new TableInfo("chat_history", _columnsChatHistory, _foreignKeysChatHistory, _indicesChatHistory);
        final TableInfo _existingChatHistory = TableInfo.read(db, "chat_history");
        if (!_infoChatHistory.equals(_existingChatHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_history(io.biotrace.app.data.local.entity.ChatMessageEntity).\n"
                  + " Expected:\n" + _infoChatHistory + "\n"
                  + " Found:\n" + _existingChatHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "c5c72cae93bd7ff789bbbb630746ed71", "dee15e09e979e0e4182d4ff4f5bfb556");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "vitals_records","lab_biomarkers","meals","chat_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `vitals_records`");
      _db.execSQL("DELETE FROM `lab_biomarkers`");
      _db.execSQL("DELETE FROM `meals`");
      _db.execSQL("DELETE FROM `chat_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(VitalsDao.class, VitalsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LabBiomarkerDao.class, LabBiomarkerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MealDao.class, MealDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChatHistoryDao.class, ChatHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public VitalsDao vitalsDao() {
    if (_vitalsDao != null) {
      return _vitalsDao;
    } else {
      synchronized(this) {
        if(_vitalsDao == null) {
          _vitalsDao = new VitalsDao_Impl(this);
        }
        return _vitalsDao;
      }
    }
  }

  @Override
  public LabBiomarkerDao labBiomarkerDao() {
    if (_labBiomarkerDao != null) {
      return _labBiomarkerDao;
    } else {
      synchronized(this) {
        if(_labBiomarkerDao == null) {
          _labBiomarkerDao = new LabBiomarkerDao_Impl(this);
        }
        return _labBiomarkerDao;
      }
    }
  }

  @Override
  public MealDao mealDao() {
    if (_mealDao != null) {
      return _mealDao;
    } else {
      synchronized(this) {
        if(_mealDao == null) {
          _mealDao = new MealDao_Impl(this);
        }
        return _mealDao;
      }
    }
  }

  @Override
  public ChatHistoryDao chatHistoryDao() {
    if (_chatHistoryDao != null) {
      return _chatHistoryDao;
    } else {
      synchronized(this) {
        if(_chatHistoryDao == null) {
          _chatHistoryDao = new ChatHistoryDao_Impl(this);
        }
        return _chatHistoryDao;
      }
    }
  }
}
