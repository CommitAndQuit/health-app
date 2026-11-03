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
import io.biotrace.app.data.local.entity.LabBiomarkerEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalStateException;
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
public final class LabBiomarkerDao_Impl implements LabBiomarkerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LabBiomarkerEntity> __insertionAdapterOfLabBiomarkerEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public LabBiomarkerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLabBiomarkerEntity = new EntityInsertionAdapter<LabBiomarkerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `lab_biomarkers` (`id`,`biomarker_name`,`raw_name`,`value`,`unit`,`reference_min`,`reference_max`,`flag`,`recorded_at`,`source`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LabBiomarkerEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getBiomarkerName());
        statement.bindString(3, entity.getRawName());
        statement.bindDouble(4, entity.getValue());
        statement.bindString(5, entity.getUnit());
        if (entity.getReferenceMin() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getReferenceMin());
        }
        if (entity.getReferenceMax() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getReferenceMax());
        }
        if (entity.getFlag() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getFlag());
        }
        final Long _tmp = __converters.fromInstant(entity.getRecordedAt());
        if (_tmp == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, _tmp);
        }
        statement.bindString(10, entity.getSource());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM lab_biomarkers";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final LabBiomarkerEntity biomarker,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLabBiomarkerEntity.insert(biomarker);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<LabBiomarkerEntity> biomarkers,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLabBiomarkerEntity.insert(biomarkers);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
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
  public Flow<List<LabBiomarkerEntity>> getAllFlow() {
    final String _sql = "SELECT * FROM lab_biomarkers ORDER BY recorded_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lab_biomarkers"}, new Callable<List<LabBiomarkerEntity>>() {
      @Override
      @NonNull
      public List<LabBiomarkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBiomarkerName = CursorUtil.getColumnIndexOrThrow(_cursor, "biomarker_name");
          final int _cursorIndexOfRawName = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_name");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfReferenceMin = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_min");
          final int _cursorIndexOfReferenceMax = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_max");
          final int _cursorIndexOfFlag = CursorUtil.getColumnIndexOrThrow(_cursor, "flag");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recorded_at");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final List<LabBiomarkerEntity> _result = new ArrayList<LabBiomarkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LabBiomarkerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBiomarkerName;
            _tmpBiomarkerName = _cursor.getString(_cursorIndexOfBiomarkerName);
            final String _tmpRawName;
            _tmpRawName = _cursor.getString(_cursorIndexOfRawName);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpReferenceMin;
            if (_cursor.isNull(_cursorIndexOfReferenceMin)) {
              _tmpReferenceMin = null;
            } else {
              _tmpReferenceMin = _cursor.getDouble(_cursorIndexOfReferenceMin);
            }
            final Double _tmpReferenceMax;
            if (_cursor.isNull(_cursorIndexOfReferenceMax)) {
              _tmpReferenceMax = null;
            } else {
              _tmpReferenceMax = _cursor.getDouble(_cursorIndexOfReferenceMax);
            }
            final String _tmpFlag;
            if (_cursor.isNull(_cursorIndexOfFlag)) {
              _tmpFlag = null;
            } else {
              _tmpFlag = _cursor.getString(_cursorIndexOfFlag);
            }
            final Instant _tmpRecordedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfRecordedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfRecordedAt);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpRecordedAt = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _item = new LabBiomarkerEntity(_tmpId,_tmpBiomarkerName,_tmpRawName,_tmpValue,_tmpUnit,_tmpReferenceMin,_tmpReferenceMax,_tmpFlag,_tmpRecordedAt,_tmpSource);
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
  public Flow<List<LabBiomarkerEntity>> getByNameFlow(final String biomarkerName) {
    final String _sql = "SELECT * FROM lab_biomarkers WHERE biomarker_name = ? ORDER BY recorded_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, biomarkerName);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lab_biomarkers"}, new Callable<List<LabBiomarkerEntity>>() {
      @Override
      @NonNull
      public List<LabBiomarkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBiomarkerName = CursorUtil.getColumnIndexOrThrow(_cursor, "biomarker_name");
          final int _cursorIndexOfRawName = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_name");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfReferenceMin = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_min");
          final int _cursorIndexOfReferenceMax = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_max");
          final int _cursorIndexOfFlag = CursorUtil.getColumnIndexOrThrow(_cursor, "flag");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recorded_at");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final List<LabBiomarkerEntity> _result = new ArrayList<LabBiomarkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LabBiomarkerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBiomarkerName;
            _tmpBiomarkerName = _cursor.getString(_cursorIndexOfBiomarkerName);
            final String _tmpRawName;
            _tmpRawName = _cursor.getString(_cursorIndexOfRawName);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpReferenceMin;
            if (_cursor.isNull(_cursorIndexOfReferenceMin)) {
              _tmpReferenceMin = null;
            } else {
              _tmpReferenceMin = _cursor.getDouble(_cursorIndexOfReferenceMin);
            }
            final Double _tmpReferenceMax;
            if (_cursor.isNull(_cursorIndexOfReferenceMax)) {
              _tmpReferenceMax = null;
            } else {
              _tmpReferenceMax = _cursor.getDouble(_cursorIndexOfReferenceMax);
            }
            final String _tmpFlag;
            if (_cursor.isNull(_cursorIndexOfFlag)) {
              _tmpFlag = null;
            } else {
              _tmpFlag = _cursor.getString(_cursorIndexOfFlag);
            }
            final Instant _tmpRecordedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfRecordedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfRecordedAt);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpRecordedAt = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _item = new LabBiomarkerEntity(_tmpId,_tmpBiomarkerName,_tmpRawName,_tmpValue,_tmpUnit,_tmpReferenceMin,_tmpReferenceMax,_tmpFlag,_tmpRecordedAt,_tmpSource);
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
  public Object getLatestByName(final String biomarkerName,
      final Continuation<? super LabBiomarkerEntity> $completion) {
    final String _sql = "SELECT * FROM lab_biomarkers WHERE biomarker_name = ? ORDER BY recorded_at DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, biomarkerName);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LabBiomarkerEntity>() {
      @Override
      @Nullable
      public LabBiomarkerEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBiomarkerName = CursorUtil.getColumnIndexOrThrow(_cursor, "biomarker_name");
          final int _cursorIndexOfRawName = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_name");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfReferenceMin = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_min");
          final int _cursorIndexOfReferenceMax = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_max");
          final int _cursorIndexOfFlag = CursorUtil.getColumnIndexOrThrow(_cursor, "flag");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recorded_at");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final LabBiomarkerEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBiomarkerName;
            _tmpBiomarkerName = _cursor.getString(_cursorIndexOfBiomarkerName);
            final String _tmpRawName;
            _tmpRawName = _cursor.getString(_cursorIndexOfRawName);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpReferenceMin;
            if (_cursor.isNull(_cursorIndexOfReferenceMin)) {
              _tmpReferenceMin = null;
            } else {
              _tmpReferenceMin = _cursor.getDouble(_cursorIndexOfReferenceMin);
            }
            final Double _tmpReferenceMax;
            if (_cursor.isNull(_cursorIndexOfReferenceMax)) {
              _tmpReferenceMax = null;
            } else {
              _tmpReferenceMax = _cursor.getDouble(_cursorIndexOfReferenceMax);
            }
            final String _tmpFlag;
            if (_cursor.isNull(_cursorIndexOfFlag)) {
              _tmpFlag = null;
            } else {
              _tmpFlag = _cursor.getString(_cursorIndexOfFlag);
            }
            final Instant _tmpRecordedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfRecordedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfRecordedAt);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpRecordedAt = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _result = new LabBiomarkerEntity(_tmpId,_tmpBiomarkerName,_tmpRawName,_tmpValue,_tmpUnit,_tmpReferenceMin,_tmpReferenceMax,_tmpFlag,_tmpRecordedAt,_tmpSource);
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
  public Object getAnomalies(final Continuation<? super List<LabBiomarkerEntity>> $completion) {
    final String _sql = "SELECT * FROM lab_biomarkers WHERE flag IN ('HIGH', 'LOW') ORDER BY recorded_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LabBiomarkerEntity>>() {
      @Override
      @NonNull
      public List<LabBiomarkerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBiomarkerName = CursorUtil.getColumnIndexOrThrow(_cursor, "biomarker_name");
          final int _cursorIndexOfRawName = CursorUtil.getColumnIndexOrThrow(_cursor, "raw_name");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfReferenceMin = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_min");
          final int _cursorIndexOfReferenceMax = CursorUtil.getColumnIndexOrThrow(_cursor, "reference_max");
          final int _cursorIndexOfFlag = CursorUtil.getColumnIndexOrThrow(_cursor, "flag");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recorded_at");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final List<LabBiomarkerEntity> _result = new ArrayList<LabBiomarkerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LabBiomarkerEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBiomarkerName;
            _tmpBiomarkerName = _cursor.getString(_cursorIndexOfBiomarkerName);
            final String _tmpRawName;
            _tmpRawName = _cursor.getString(_cursorIndexOfRawName);
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final Double _tmpReferenceMin;
            if (_cursor.isNull(_cursorIndexOfReferenceMin)) {
              _tmpReferenceMin = null;
            } else {
              _tmpReferenceMin = _cursor.getDouble(_cursorIndexOfReferenceMin);
            }
            final Double _tmpReferenceMax;
            if (_cursor.isNull(_cursorIndexOfReferenceMax)) {
              _tmpReferenceMax = null;
            } else {
              _tmpReferenceMax = _cursor.getDouble(_cursorIndexOfReferenceMax);
            }
            final String _tmpFlag;
            if (_cursor.isNull(_cursorIndexOfFlag)) {
              _tmpFlag = null;
            } else {
              _tmpFlag = _cursor.getString(_cursorIndexOfFlag);
            }
            final Instant _tmpRecordedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfRecordedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfRecordedAt);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpRecordedAt = _tmp_1;
            }
            final String _tmpSource;
            _tmpSource = _cursor.getString(_cursorIndexOfSource);
            _item = new LabBiomarkerEntity(_tmpId,_tmpBiomarkerName,_tmpRawName,_tmpValue,_tmpUnit,_tmpReferenceMin,_tmpReferenceMax,_tmpFlag,_tmpRecordedAt,_tmpSource);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
