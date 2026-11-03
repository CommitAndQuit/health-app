package io.biotrace.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import io.biotrace.app.data.local.converter.Converters;
import io.biotrace.app.data.local.entity.MealEntity;
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
public final class MealDao_Impl implements MealDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MealEntity> __insertionAdapterOfMealEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public MealDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMealEntity = new EntityInsertionAdapter<MealEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `meals` (`id`,`name`,`calories`,`protein_grams`,`carbs_grams`,`fats_grams`,`fiber_grams`,`consumed_at`,`photo_path`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MealEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getCalories());
        statement.bindDouble(4, entity.getProteinGrams());
        statement.bindDouble(5, entity.getCarbsGrams());
        statement.bindDouble(6, entity.getFatsGrams());
        if (entity.getFiberGrams() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getFiberGrams());
        }
        final Long _tmp = __converters.fromInstant(entity.getConsumedAt());
        if (_tmp == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, _tmp);
        }
        if (entity.getPhotoPath() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getPhotoPath());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM meals";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MealEntity meal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMealEntity.insert(meal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<MealEntity> meals,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMealEntity.insert(meals);
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
  public Flow<List<MealEntity>> getMealsBetweenFlow(final Instant startTime,
      final Instant endTime) {
    final String _sql = "\n"
            + "        SELECT * FROM meals \n"
            + "        WHERE consumed_at >= ? AND consumed_at <= ? \n"
            + "        ORDER BY consumed_at DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final Long _tmp = __converters.fromInstant(startTime);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    _argIndex = 2;
    final Long _tmp_1 = __converters.fromInstant(endTime);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp_1);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meals"}, new Callable<List<MealEntity>>() {
      @Override
      @NonNull
      public List<MealEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfProteinGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "protein_grams");
          final int _cursorIndexOfCarbsGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "carbs_grams");
          final int _cursorIndexOfFatsGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "fats_grams");
          final int _cursorIndexOfFiberGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "fiber_grams");
          final int _cursorIndexOfConsumedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "consumed_at");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_path");
          final List<MealEntity> _result = new ArrayList<MealEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MealEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpCalories;
            _tmpCalories = _cursor.getInt(_cursorIndexOfCalories);
            final double _tmpProteinGrams;
            _tmpProteinGrams = _cursor.getDouble(_cursorIndexOfProteinGrams);
            final double _tmpCarbsGrams;
            _tmpCarbsGrams = _cursor.getDouble(_cursorIndexOfCarbsGrams);
            final double _tmpFatsGrams;
            _tmpFatsGrams = _cursor.getDouble(_cursorIndexOfFatsGrams);
            final Double _tmpFiberGrams;
            if (_cursor.isNull(_cursorIndexOfFiberGrams)) {
              _tmpFiberGrams = null;
            } else {
              _tmpFiberGrams = _cursor.getDouble(_cursorIndexOfFiberGrams);
            }
            final Instant _tmpConsumedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfConsumedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfConsumedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpConsumedAt = _tmp_3;
            }
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            _item = new MealEntity(_tmpId,_tmpName,_tmpCalories,_tmpProteinGrams,_tmpCarbsGrams,_tmpFatsGrams,_tmpFiberGrams,_tmpConsumedAt,_tmpPhotoPath);
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
  public Object getMealsBetween(final Instant startTime, final Instant endTime,
      final Continuation<? super List<MealEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM meals \n"
            + "        WHERE consumed_at >= ? AND consumed_at <= ? \n"
            + "        ORDER BY consumed_at DESC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final Long _tmp = __converters.fromInstant(startTime);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    _argIndex = 2;
    final Long _tmp_1 = __converters.fromInstant(endTime);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp_1);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MealEntity>>() {
      @Override
      @NonNull
      public List<MealEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfProteinGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "protein_grams");
          final int _cursorIndexOfCarbsGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "carbs_grams");
          final int _cursorIndexOfFatsGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "fats_grams");
          final int _cursorIndexOfFiberGrams = CursorUtil.getColumnIndexOrThrow(_cursor, "fiber_grams");
          final int _cursorIndexOfConsumedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "consumed_at");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photo_path");
          final List<MealEntity> _result = new ArrayList<MealEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MealEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpCalories;
            _tmpCalories = _cursor.getInt(_cursorIndexOfCalories);
            final double _tmpProteinGrams;
            _tmpProteinGrams = _cursor.getDouble(_cursorIndexOfProteinGrams);
            final double _tmpCarbsGrams;
            _tmpCarbsGrams = _cursor.getDouble(_cursorIndexOfCarbsGrams);
            final double _tmpFatsGrams;
            _tmpFatsGrams = _cursor.getDouble(_cursorIndexOfFatsGrams);
            final Double _tmpFiberGrams;
            if (_cursor.isNull(_cursorIndexOfFiberGrams)) {
              _tmpFiberGrams = null;
            } else {
              _tmpFiberGrams = _cursor.getDouble(_cursorIndexOfFiberGrams);
            }
            final Instant _tmpConsumedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfConsumedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfConsumedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpConsumedAt = _tmp_3;
            }
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            _item = new MealEntity(_tmpId,_tmpName,_tmpCalories,_tmpProteinGrams,_tmpCarbsGrams,_tmpFatsGrams,_tmpFiberGrams,_tmpConsumedAt,_tmpPhotoPath);
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
