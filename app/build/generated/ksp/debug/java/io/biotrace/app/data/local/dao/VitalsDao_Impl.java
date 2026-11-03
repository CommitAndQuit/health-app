package io.biotrace.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.biotrace.app.data.local.converter.Converters;
import io.biotrace.app.data.local.entity.VitalsRecordEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VitalsDao_Impl implements VitalsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VitalsRecordEntity> __insertionAdapterOfVitalsRecordEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public VitalsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVitalsRecordEntity = new EntityInsertionAdapter<VitalsRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `vitals_records` (`id`,`metric_type`,`value`,`unit`,`timestamp`,`source`,`created_at`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VitalsRecordEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getMetricType());
        statement.bindDouble(3, entity.getValue());
        statement.bindString(4, entity.getUnit());
        final Long _tmp = __converters.fromInstant(entity.getTimestamp());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp);
        }
        statement.bindString(6, entity.getSource());
        final Long _tmp_1 = __converters.fromInstant(entity.getCreatedAt());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, _tmp_1);
        }
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM vitals_records WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM vitals_records";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final VitalsRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVitalsRecordEntity.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<VitalsRecordEntity> records,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVitalsRecordEntity.insert(records);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final Instant cutoff,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        final Long _tmp = __converters.fromInstant(cutoff);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VitalsRecordEntity>> getRecordsFlow(final String metricType,
      final Instant startTime, final Instant endTime) {
    final String _sql = "\n"
            + "        SELECT * FROM vitals_records \n"
            + "        WHERE metric_type = ? \n"
            + "          AND timestamp >= ? \n"
            + "          AND timestamp <= ? \n"
            + "        ORDER BY timestamp ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, metricType);
    _argIndex = 2;
    final Long _tmp = __converters.fromInstant(startTime);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    _argIndex = 3;
    final Long _tmp_1 = __converters.fromInstant(endTime);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp_1);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vitals_records"}, new Callable<List<VitalsRecordEntity>>() {
      @Override
      @NonNull
      public List<VitalsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<VitalsRecordEntity> _result = new ArrayList<VitalsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VitalsRecordEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_3;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_5 = __converters.toInstant(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_5;
            }
            _item = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecordsBetween(final String metricType, final Instant startTime,
      final Instant endTime, final Continuation<? super List<VitalsRecordEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM vitals_records \n"
            + "        WHERE metric_type = ? \n"
            + "          AND timestamp >= ? \n"
            + "          AND timestamp <= ? \n"
            + "        ORDER BY timestamp ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, metricType);
    _argIndex = 2;
    final Long _tmp = __converters.fromInstant(startTime);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    _argIndex = 3;
    final Long _tmp_1 = __converters.fromInstant(endTime);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp_1);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VitalsRecordEntity>>() {
      @Override
      @NonNull
      public List<VitalsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<VitalsRecordEntity> _result = new ArrayList<VitalsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VitalsRecordEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_3;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_5 = __converters.toInstant(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_5;
            }
            _item = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestRecord(final String metricType,
      final Continuation<? super VitalsRecordEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM vitals_records \n"
            + "        WHERE metric_type = ? \n"
            + "        ORDER BY timestamp DESC \n"
            + "        LIMIT 1\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, metricType);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VitalsRecordEntity>() {
      @Override
      @Nullable
      public VitalsRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final VitalsRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_3;
            }
            _result = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<VitalsRecordEntity> getLatestRecordFlow(final String metricType) {
    final String _sql = "\n"
            + "        SELECT * FROM vitals_records \n"
            + "        WHERE metric_type = ? \n"
            + "        ORDER BY timestamp DESC \n"
            + "        LIMIT 1\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, metricType);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vitals_records"}, new Callable<VitalsRecordEntity>() {
      @Override
      @Nullable
      public VitalsRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final VitalsRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_3;
            }
            _result = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllRecords(final Continuation<? super List<VitalsRecordEntity>> $completion) {
    final String _sql = "SELECT * FROM vitals_records ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VitalsRecordEntity>>() {
      @Override
      @NonNull
      public List<VitalsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<VitalsRecordEntity> _result = new ArrayList<VitalsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VitalsRecordEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_3;
            }
            _item = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecentRecords(final String metricType, final int limit,
      final Continuation<? super List<VitalsRecordEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM vitals_records \n"
            + "        WHERE metric_type = ? \n"
            + "        ORDER BY timestamp DESC \n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, metricType);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VitalsRecordEntity>>() {
      @Override
      @NonNull
      public List<VitalsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<VitalsRecordEntity> _result = new ArrayList<VitalsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VitalsRecordEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_3;
            }
            _item = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VitalsRecordEntity>> getRecentRecordsFlow(final String metricType,
      final int limit) {
    final String _sql = "\n"
            + "        SELECT * FROM vitals_records \n"
            + "        WHERE metric_type = ? \n"
            + "        ORDER BY timestamp DESC \n"
            + "        LIMIT ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, metricType);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vitals_records"}, new Callable<List<VitalsRecordEntity>>() {
      @Override
      @NonNull
      public List<VitalsRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metric_type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<VitalsRecordEntity> _result = new ArrayList<VitalsRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VitalsRecordEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpMetricType;
            _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Instant _tmpTimestamp;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfTimestamp);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpTimestamp = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            final Instant _tmpCreatedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_3;
            }
            _item = new VitalsRecordEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpTimestamp,_tmpSource,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
